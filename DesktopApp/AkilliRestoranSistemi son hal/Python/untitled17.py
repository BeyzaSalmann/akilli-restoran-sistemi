# -*- coding: utf-8 -*-
"""Görüntü üzerinde yüz tespiti ve mutluluk oranı tahmini."""

import tensorflow as tf
import cv2
import numpy as np

prototxt_yolu = "deploy.prototxt.txt"
model_yolu = "res10_300x300_ssd_iter_140000.caffemodel"
face_net = cv2.dnn.readNetFromCaffe(prototxt_yolu, model_yolu)
guven_esigi = 0.5

duygu_model_yolu = "duygu_tanima_model_final.h5"
emotion_model = tf.keras.models.load_model(duygu_model_yolu)
IMAGE_SIZE = (96, 96)
CLASS_NAMES = ["angry", "disgust", "fear", "happy", "neutral", "sad", "surprise"]

try:
    mutlu_index = CLASS_NAMES.index("happy")
except ValueError:
    try:
        mutlu_index = CLASS_NAMES.index("mutlu")
    except ValueError:
        print("HATA: CLASS_NAMES listesinde 'happy' veya 'mutlu' bulunamadı.")
        raise SystemExit(1)

print(f"Sınıflar: {CLASS_NAMES}")
print(f"'Mutlu' sınıfının indeksi: {mutlu_index}")

gorunt_yolu = "image_e7c5db.jpg"
image = cv2.imread(gorunt_yolu)
(h, w) = image.shape[:2]

blob = cv2.dnn.blobFromImage(cv2.resize(image, (300, 300)), 1.0,
    (300, 300), (104.0, 177.0, 123.0))
face_net.setInput(blob)
detections = face_net.forward()
print(f"[BİLGİ] {detections.shape[2]} adet potansiyel yüz bulundu.")

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
        mutluluk_yuzdesi = emotion_probabilities[mutlu_index] * 100

        cv2.rectangle(image, (startX, startY), (endX, endY), (0, 255, 0), 2)
        text = f"Mutluluk: {mutluluk_yuzdesi:.1f}%"
        y = startY - 10 if startY - 10 > 10 else startY + 10
        cv2.putText(image, text, (startX, y),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

print("[BİLGİ] Analiz tamamlandı. Sonuç gösteriliyor...")
max_h = 800
if h > max_h:
    oran = max_h / float(h)
    dim = (int(w * oran), max_h)
    image_resized = cv2.resize(image, dim)
else:
    image_resized = image

cv2.imshow("Restoran Mutluluk Analizi Sonucu", image_resized)
cv2.waitKey(0)
cv2.destroyAllWindows()
