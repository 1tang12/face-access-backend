# Face Access System Backend

This repository contains the backend part of the face access control system:

- `demo/`: Spring Boot backend service.
- `facenet-pytorch-main/`: Python FaceNet/MobileNet recognition bridge and model files.
- `database/`: MySQL initialization scripts and database design files.

## Runtime Notes

Configure local secrets with environment variables before running:

- `MYSQL_PASSWORD`
- `JWT_SECRET`
- `FILE_UPLOAD_PATH`
- `FACE_MODEL_DIR`

The default backend context path is `/api`.
