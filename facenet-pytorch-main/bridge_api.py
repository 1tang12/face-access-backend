import argparse
import json
import os
import sys
from typing import List, Tuple

import numpy as np
from PIL import Image, ImageOps

from facenet import Facenet

try:
    import cv2
except Exception:
    cv2 = None

JSON_PREFIX = "JSON_RESULT:"
MIN_BLUR_SCORE = 150.0
MIN_FACE_RATIO = 0.10
MAX_FACE_RATIO = 0.45
MIN_BRIGHTNESS = 70.0
MAX_BRIGHTNESS = 190.0
MAX_EYE_ANGLE = 12.0
MIN_QUALITY_SCORE = 0.68


def emit_json(payload: dict) -> None:
    print(JSON_PREFIX + json.dumps(payload, ensure_ascii=False))


def normalize_feature(feature: np.ndarray) -> np.ndarray:
    norm = np.linalg.norm(feature)
    if norm <= 1e-12:
        return feature
    return feature / norm


def clamp(value: float, low: float = 0.0, high: float = 1.0) -> float:
    return max(low, min(high, value))


def detect_face_metrics(image: Image.Image) -> dict:
    if cv2 is None:
        return {
            "face_count": None,
            "blur_score": None,
            "face_ratio": None,
            "brightness": None,
            "eye_angle": None,
            "passed": True,
            "score": 0.80,
            "message": "当前环境未启用质量检测，已跳过质量约束。"
        }

    np_image = np.array(image)
    bgr = cv2.cvtColor(np_image, cv2.COLOR_RGB2BGR)
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)

    face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
    eye_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_eye.xml")

    faces = face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=4, minSize=(60, 60))
    face_count = int(len(faces))
    if face_count != 1:
        return {
            "face_count": face_count,
            "blur_score": None,
            "face_ratio": None,
            "brightness": None,
            "eye_angle": None,
            "passed": False,
            "score": 0.0,
            "message": "请确保画面中只有一张清晰人脸。"
        }

    x, y, w, h = max(faces, key=lambda item: item[2] * item[3])
    roi_gray = gray[y:y + h, x:x + w]
    eyes = eye_cascade.detectMultiScale(roi_gray, scaleFactor=1.1, minNeighbors=4, minSize=(12, 12))

    blur_score = float(cv2.Laplacian(roi_gray, cv2.CV_64F).var())
    face_ratio = float((w * h) / float(gray.shape[0] * gray.shape[1]))
    brightness = float(np.mean(roi_gray))
    eye_angle = None
    if len(eyes) >= 2:
        eyes = sorted(eyes, key=lambda item: item[0])[:2]
        left_eye = eyes[0]
        right_eye = eyes[1]
        left_center = (left_eye[0] + left_eye[2] / 2, left_eye[1] + left_eye[3] / 2)
        right_center = (right_eye[0] + right_eye[2] / 2, right_eye[1] + right_eye[3] / 2)
        dy = right_center[1] - left_center[1]
        dx = right_center[0] - left_center[0]
        eye_angle = float(abs(np.degrees(np.arctan2(dy, dx))))

    sharp_score = clamp((blur_score - 100.0) / 180.0)
    ratio_score = clamp(1.0 - abs(face_ratio - 0.22) / 0.18)
    brightness_score = clamp(1.0 - abs(brightness - 135.0) / 65.0)
    pose_score = 0.40 if eye_angle is None else clamp(1.0 - eye_angle / MAX_EYE_ANGLE)
    quality_score = (
        0.50 * sharp_score +
        0.20 * ratio_score +
        0.10 * brightness_score +
        0.20 * pose_score
    )

    failures = []
    if blur_score < MIN_BLUR_SCORE:
        failures.append("清晰度不足")
    if face_ratio < MIN_FACE_RATIO:
        failures.append("人脸距离过远")
    if face_ratio > MAX_FACE_RATIO:
        failures.append("人脸距离过近")
    if brightness < MIN_BRIGHTNESS or brightness > MAX_BRIGHTNESS:
        failures.append("光照不理想")
    if eye_angle is not None and eye_angle > MAX_EYE_ANGLE:
        failures.append("角度偏斜较大")

    passed = len(failures) == 0 and quality_score >= MIN_QUALITY_SCORE
    message = "质量检测通过，建议继续补录至 2~5 张高质量模板。" if passed else "、".join(failures) + "，请重新采集。"

    return {
        "face_count": face_count,
        "blur_score": round(blur_score, 2),
        "face_ratio": round(face_ratio, 4),
        "brightness": round(brightness, 2),
        "eye_angle": None if eye_angle is None else round(eye_angle, 2),
        "passed": passed,
        "score": round(float(quality_score), 4),
        "message": message
    }


def build_tta_views(image: Image.Image) -> List[Image.Image]:
    base = image
    mirrored = ImageOps.mirror(base)
    auto = ImageOps.autocontrast(base, cutoff=1)
    auto_mirrored = ImageOps.mirror(auto)
    return [base, mirrored, auto, auto_mirrored]


def load_rgb_image(image_path: str) -> Image.Image:
    image = Image.open(image_path).convert("RGB")
    return preprocess_face_image(image)


def extract_robust_feature(model: Facenet, image: Image.Image) -> np.ndarray:
    views = build_tta_views(image)
    features = []
    for view in views:
        feature = np.array(model.get_feature(view), dtype=np.float64)
        features.append(normalize_feature(feature))
    merged = np.mean(features, axis=0)
    return normalize_feature(merged)


def preprocess_face_image(image: Image.Image) -> Image.Image:
    if cv2 is None:
        return image

    try:
        np_image = np.array(image)
        bgr = cv2.cvtColor(np_image, cv2.COLOR_RGB2BGR)
        gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)

        face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")
        eye_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_eye.xml")

        faces = face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=4, minSize=(60, 60))
        if len(faces) == 0:
            return image

        x, y, w, h = max(faces, key=lambda item: item[2] * item[3])
        roi_gray = gray[y:y + h, x:x + w]
        eyes = eye_cascade.detectMultiScale(roi_gray, scaleFactor=1.1, minNeighbors=4, minSize=(12, 12))

        aligned_bgr = bgr
        if len(eyes) >= 2:
            eyes = sorted(eyes, key=lambda item: item[0])[:2]
            left_eye = eyes[0]
            right_eye = eyes[1]
            left_center = (x + left_eye[0] + left_eye[2] / 2, y + left_eye[1] + left_eye[3] / 2)
            right_center = (x + right_eye[0] + right_eye[2] / 2, y + right_eye[1] + right_eye[3] / 2)
            dy = right_center[1] - left_center[1]
            dx = right_center[0] - left_center[0]
            angle = np.degrees(np.arctan2(dy, dx))
            center = ((left_center[0] + right_center[0]) / 2, (left_center[1] + right_center[1]) / 2)
            matrix = cv2.getRotationMatrix2D(center, angle, 1.0)
            aligned_bgr = cv2.warpAffine(
                bgr,
                matrix,
                (bgr.shape[1], bgr.shape[0]),
                flags=cv2.INTER_LINEAR,
                borderMode=cv2.BORDER_REPLICATE
            )
            gray = cv2.cvtColor(aligned_bgr, cv2.COLOR_BGR2GRAY)
            faces = face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=4, minSize=(60, 60))
            if len(faces) > 0:
                x, y, w, h = max(faces, key=lambda item: item[2] * item[3])

        pad = int(max(w, h) * 0.18)
        x1 = max(0, x - pad)
        y1 = max(0, y - pad)
        x2 = min(aligned_bgr.shape[1], x + w + pad)
        y2 = min(aligned_bgr.shape[0], y + h + pad)
        cropped = aligned_bgr[y1:y2, x1:x2]
        if cropped.size == 0:
            return image
        cropped_rgb = cv2.cvtColor(cropped, cv2.COLOR_BGR2RGB)
        return Image.fromarray(cropped_rgb)
    except Exception:
        return image


def parse_candidates_file(file_path: str) -> List[Tuple[int, str]]:
    candidates: List[Tuple[int, str]] = []
    with open(file_path, "r", encoding="utf-8") as f:
        for raw_line in f:
            line = raw_line.strip()
            if not line:
                continue
            parts = line.split("\t", 1)
            if len(parts) != 2:
                continue
            user_id = int(parts[0])
            image_path = parts[1]
            candidates.append((user_id, image_path))
    return candidates


def handle_extract(model: Facenet, image_path: str) -> None:
    raw_image = Image.open(image_path).convert("RGB")
    quality = detect_face_metrics(raw_image)
    feature = extract_robust_feature(model, preprocess_face_image(raw_image))
    feature_list = [round(float(x), 8) for x in feature.tolist()]
    emit_json({
        "ok": True,
        "feature": feature_list,
        "quality": quality
    })


def handle_quality(image_path: str) -> None:
    raw_image = Image.open(image_path).convert("RGB")
    quality = detect_face_metrics(raw_image)
    emit_json({
        "ok": True,
        "quality": quality
    })


def handle_verify(model: Facenet, query_path: str, candidates_file: str, threshold: float) -> None:
    candidates = parse_candidates_file(candidates_file)
    if not candidates:
        emit_json({
            "ok": True,
            "recognized": False,
            "matched_user_id": None,
            "best_distance": None,
            "matched_image": None,
            "checked_count": 0
        })
        return

    query_feature = extract_robust_feature(model, load_rgb_image(query_path))

    best_user_id = None
    best_distance = None
    best_image = None
    checked_count = 0

    for user_id, image_path in candidates:
        if not os.path.isfile(image_path):
            continue
        try:
            db_feature = extract_robust_feature(model, load_rgb_image(image_path))
            distance = float(model.compare_feature(query_feature, db_feature))
        except Exception:
            continue

        checked_count += 1
        if best_distance is None or distance < best_distance:
            best_distance = distance
            best_user_id = user_id
            best_image = image_path

    if best_distance is None:
        emit_json({
            "ok": True,
            "recognized": False,
            "matched_user_id": None,
            "best_distance": None,
            "matched_image": None,
            "checked_count": 0
        })
        return

    emit_json({
        "ok": True,
        "recognized": best_distance < threshold,
        "matched_user_id": best_user_id,
        "best_distance": round(best_distance, 6),
        "matched_image": best_image,
        "checked_count": checked_count
    })


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="FaceNet bridge script for backend integration")
    subparsers = parser.add_subparsers(dest="command", required=True)

    extract_parser = subparsers.add_parser("extract", help="Extract feature vector from one image")
    extract_parser.add_argument("--image", required=True, help="Path to one face image")

    quality_parser = subparsers.add_parser("quality", help="Check image quality before registration")
    quality_parser.add_argument("--image", required=True, help="Path to one face image")

    verify_parser = subparsers.add_parser("verify", help="Find best match for one query image")
    verify_parser.add_argument("--query", required=True, help="Path to query image")
    verify_parser.add_argument("--candidates-file", required=True, help="TSV file: userId\\timagePath")
    verify_parser.add_argument("--threshold", required=True, type=float, help="Distance threshold")

    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    try:
        model = Facenet()

        if args.command == "extract":
            handle_extract(model, args.image)
            return 0

        if args.command == "quality":
            handle_quality(args.image)
            return 0

        if args.command == "verify":
            handle_verify(model, args.query, args.candidates_file, args.threshold)
            return 0

        emit_json({"ok": False, "error": f"Unknown command: {args.command}"})
        return 1
    except Exception as e:
        emit_json({"ok": False, "error": str(e)})
        return 1


if __name__ == "__main__":
    sys.exit(main())
