# -*- coding: utf-8 -*-
"""Yüz tespiti ve mutluluk oranı tahmini için Flask API."""

import os
from flask import Flask, request, jsonify
import tensorflow as tf
import cv2
import numpy as np
import base64


def load_dotenv(path):
    if not os.path.isfile(path):
        return
    with open(path, encoding="utf-8") as env_file:
        for raw in env_file:
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


_BASE_DIR = os.path.dirname(os.path.abspath(__file__))
load_dotenv(os.path.join(_BASE_DIR, ".env"))
load_dotenv(os.path.join(_BASE_DIR, "..", "..", "..", ".env"))

print("[BİLGİ] Modeller yükleniyor...")
prototxt_yolu = os.environ.get("FACE_PROTOTXT", "deploy.prototxt.txt")
model_yolu = os.environ.get("FACE_CAFFE_MODEL", "res10_300x300_ssd_iter_140000.caffemodel")
face_net = cv2.dnn.readNetFromCaffe(prototxt_yolu, model_yolu)
guven_esigi = 0.5

duygu_model_yolu = os.environ.get("EMOTION_MODEL_PATH", "duygu_tanima_model_final.h5")
emotion_model = tf.keras.models.load_model(duygu_model_yolu)

IMAGE_SIZE = (96, 96)
CLASS_NAMES = ["angry", "disgust", "fear", "happy", "neutral", "sad", "surprise"]
try:
    mutlu_index = CLASS_NAMES.index("happy")
except ValueError:
    mutlu_index = CLASS_NAMES.index("mutlu")

print("[BİLGİ] Modeller yüklendi, sunucu hazır.")

app = Flask(__name__)


def analyze_image(image_bytes):
    nparr = np.frombuffer(image_bytes, np.uint8)
    image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    (h, w) = image.shape[:2]
    if h == 0 or w == 0:
        return []

    blob = cv2.dnn.blobFromImage(cv2.resize(image, (300, 300)), 1.0,
        (300, 300), (104.0, 177.0, 123.0))
    face_net.setInput(blob)
    detections = face_net.forward()

    results = []
    for i in range(0, detections.shape[2]):
        confidence = detections[0, 0, i, 2]
        if confidence > guven_esigi:
            box = detections[0, 0, i, 3:7] * np.array([w, h, w, h])
            (startX, startY, endX, endY) = box.astype("int")
            (startX, startY) = (max(0, startX), max(0, startY))
            (endX, endY) = (min(w - 1, endX), min(h - 1, endY))

            face_roi = image[startY:endY, startX:endX]
            if face_roi.size == 0:
                continue

            face_rgb = cv2.cvtColor(face_roi, cv2.COLOR_BGR2RGB)
            face_resized = cv2.resize(face_rgb, IMAGE_SIZE)
            face_expanded = np.expand_dims(face_resized, axis=0)
            face_processed = tf.keras.applications.mobilenet_v2.preprocess_input(face_expanded)

            emotion_probabilities = emotion_model.predict(face_processed)[0]
            mutluluk_yuzdesi = float(emotion_probabilities[mutlu_index] * 100)

            results.append({
                "box": [int(startX), int(startY), int(endX), int(endY)],
                "happiness_percent": round(mutluluk_yuzdesi, 2),
            })

    return results


@app.route("/predict", methods=["POST"])
def predict():
    if "image" not in request.json:
        return jsonify({"error": "Resim bulunamadi (image anahtari eksik)"}), 400

    try:
        image_bytes = base64.b64decode(request.json["image"])
        faces = analyze_image(image_bytes)
        return jsonify({"status": "success", "faces": faces})
    except Exception as e:
        print(f"Hata oluştu: {e}")
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    host = os.environ.get("FLASK_HOST", "127.0.0.1")
    port = int(os.environ.get("FLASK_PORT", "5000"))
    app.run(host=host, port=port, debug=False)
