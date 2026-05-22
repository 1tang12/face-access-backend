import os
from PIL import Image

from facenet import Facenet

# 人脸库文件夹
DATABASE_DIR = "img"

# 阈值，可根据实际效果调整
THRESHOLD = 0.8


if __name__ == "__main__":
    model = Facenet()

    while True:
        query_path = input("Input current image filename (or 'exit'): ").strip()
        if query_path.lower() == "exit":
            break

        try:
            query_image = Image.open(query_path).convert("RGB")
        except Exception as e:
            print("Query image open error! Try again!")
            print("error:", e)
            continue

        if not os.path.exists(DATABASE_DIR):
            print(f"Database directory '{DATABASE_DIR}' does not exist.\n")
            continue

        # 提取待识别图片特征
        query_feature = model.get_feature(query_image)

        best_name = None
        best_dist = float("inf")

        # 遍历人脸库
        for file_name in os.listdir(DATABASE_DIR):
            img_path = os.path.join(DATABASE_DIR, file_name)

            if not os.path.isfile(img_path):
                continue

            try:
                db_image = Image.open(img_path).convert("RGB")
                db_feature = model.get_feature(db_image)
                dist = model.compare_feature(query_feature, db_feature)
                print(f"{file_name} -> distance: {dist:.4f}")
            except Exception as e:
                print(f"Compare failed: {img_path}, error: {e}")
                continue

            if dist < best_dist:
                best_dist = dist
                best_name = os.path.splitext(file_name)[0]

        if best_name is None:
            print("No valid face image found in database.\n")
            continue

        if best_dist < THRESHOLD:
            print(f"Recognition success: {best_name}, distance = {best_dist:.4f}\n")
        else:
            print(f"Unknown person, best match = {best_name}, distance = {best_dist:.4f}\n")