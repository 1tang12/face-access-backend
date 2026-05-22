import json
import re
import time
from pathlib import Path
from typing import Dict, List, Tuple

import numpy as np
from PIL import Image, ImageOps

from facenet import Facenet
from bridge_api import normalize_feature, preprocess_face_image


ROOT = Path(__file__).resolve().parent
UPLOADS = ROOT.parent / "uploads" / "face"
OUT = ROOT / "model_data" / "lfw_subset_ablation.json"
THRESHOLD = 0.80


def parse_lfwq_files() -> Dict[int, List[Tuple[int, Path]]]:
    pattern = re.compile(r"^face_(\d+)_lfwq_(\d+)_.+_\d+\.jpg$")
    groups: Dict[int, List[Tuple[int, Path]]] = {}
    for path in UPLOADS.glob("*lfwq*.jpg"):
        match = pattern.match(path.name)
        if not match:
            continue
        user_id = int(match.group(1))
        template_index = int(match.group(2))
        groups.setdefault(user_id, []).append((template_index, path))
    return {uid: sorted(items, key=lambda item: item[0]) for uid, items in groups.items()}


def tta_views(image: Image.Image) -> List[Image.Image]:
    auto = ImageOps.autocontrast(image, cutoff=1)
    return [image, ImageOps.mirror(image), auto, ImageOps.mirror(auto)]


def extract_feature(model: Facenet, image_path: Path, *, align: bool, tta: bool) -> np.ndarray:
    image = Image.open(image_path).convert("RGB")
    if align:
        image = preprocess_face_image(image)
    views = tta_views(image) if tta else [image]
    features = []
    for view in views:
        feature = np.array(model.get_feature(view), dtype=np.float64)
        features.append(normalize_feature(feature))
    merged = np.mean(features, axis=0)
    return normalize_feature(merged)


def distance(left: np.ndarray, right: np.ndarray) -> float:
    return float(np.linalg.norm(left - right))


def evaluate_variant(model: Facenet, name: str, *, templates_per_user: int, align: bool, tta: bool,
                     enrolled: Dict[int, List[Tuple[int, Path]]],
                     negative_paths: List[Path]) -> dict:
    feature_cache: Dict[Path, np.ndarray] = {}

    def get_feature(path: Path) -> np.ndarray:
        if path not in feature_cache:
            feature_cache[path] = extract_feature(model, path, align=align, tta=tta)
        return feature_cache[path]

    gallery: Dict[int, List[np.ndarray]] = {}
    positive: List[Tuple[int, Path]] = []
    for user_id, items in enrolled.items():
        gallery_items = items[:templates_per_user]
        probe_items = items[2:]
        gallery[user_id] = [get_feature(path) for _, path in gallery_items]
        positive.extend((user_id, path) for _, path in probe_items)

    positive_distances: List[float] = []
    positive_accept = 0
    for user_id, path in positive:
        query = get_feature(path)
        best = min(distance(query, template) for template in gallery[user_id])
        positive_distances.append(best)
        if best < THRESHOLD:
            positive_accept += 1

    negative_distances: List[float] = []
    negative_reject = 0
    for path in negative_paths:
        query = get_feature(path)
        best = min(
            distance(query, template)
            for templates in gallery.values()
            for template in templates
        )
        negative_distances.append(best)
        if best >= THRESHOLD:
            negative_reject += 1

    positive_total = len(positive_distances)
    negative_total = len(negative_distances)
    positive_accept_rate = positive_accept / positive_total if positive_total else 0.0
    negative_reject_rate = negative_reject / negative_total if negative_total else 0.0

    return {
        "name": name,
        "templates_per_user": templates_per_user,
        "align": align,
        "tta": tta,
        "gallery_templates": sum(len(v) for v in gallery.values()),
        "positive_probe": positive_total,
        "negative_probe": negative_total,
        "positive_accept_rate": round(positive_accept_rate * 100, 2),
        "negative_reject_rate": round(negative_reject_rate * 100, 2),
        "frr": round((1 - positive_accept_rate) * 100, 2),
        "far": round((1 - negative_reject_rate) * 100, 2),
        "positive_distance_mean": round(float(np.mean(positive_distances)), 4),
        "positive_distance_p95": round(float(np.percentile(positive_distances, 95)), 4),
        "negative_best_distance_mean": round(float(np.mean(negative_distances)), 4),
        "negative_best_distance_p05": round(float(np.percentile(negative_distances, 5)), 4),
        "feature_count": len(feature_cache),
    }


def main() -> None:
    groups = parse_lfwq_files()
    enrolled = {uid: items for uid, items in groups.items() if len(items) >= 3}
    negative_paths = [
        path
        for uid, items in groups.items()
        if uid not in enrolled
        for _, path in items
    ]
    model = Facenet()
    variants = [
        ("单模板+无对齐+无TTA", 1, False, False),
        ("单模板+对齐+无TTA", 1, True, False),
        ("多模板+对齐+无TTA", 2, True, False),
        ("多模板+对齐+TTA", 2, True, True),
    ]

    started = time.perf_counter()
    results = []
    for name, templates_per_user, align, tta in variants:
        print(f"Running {name} ...", flush=True)
        item_started = time.perf_counter()
        result = evaluate_variant(
            model,
            name,
            templates_per_user=templates_per_user,
            align=align,
            tta=tta,
            enrolled=enrolled,
            negative_paths=negative_paths,
        )
        result["elapsed_seconds"] = round(time.perf_counter() - item_started, 2)
        results.append(result)
        print(result, flush=True)

    payload = {
        "threshold": THRESHOLD,
        "dataset": "quality-filtered LFW subset from uploads/face",
        "enrolled_identities": len(enrolled),
        "negative_identities": len(groups) - len(enrolled),
        "protocol": "For enrolled identities with at least three images, the first image is used by single-template variants, the first two images are used by multi-template variants, and images from the third onward are used as positive probes. Images from identities with fewer than three images are used as negative probes.",
        "results": results,
        "elapsed_seconds": round(time.perf_counter() - started, 2),
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {OUT}", flush=True)


if __name__ == "__main__":
    main()
