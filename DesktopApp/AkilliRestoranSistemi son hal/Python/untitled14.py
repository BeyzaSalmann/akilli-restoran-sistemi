# -*- coding: utf-8 -*-
"""SSD tabanlı yüz tespiti (OpenCV DNN)."""

import cv2
import numpy as np

prototxt_yolu = "deploy.prototxt.txt"
model_yolu = "res10_300x300_ssd_iter_140000.caffemodel"
guven_esigi = 0.5
gorunt_yolu = "image_e7c5db.jpg"

print("[BİLGİ] Yüz tespiti modeli yükleniyor...")
net = cv2.dnn.readNetFromCaffe(prototxt_yolu, model_yolu)

image = cv2.imread(gorunt_yolu)
(h, w) = image.shape[:2]

blob = cv2.dnn.blobFromImage(cv2.resize(image, (300, 300)), 1.0,
    (300, 300), (104.0, 177.0, 123.0))

print("[BİLGİ] Yüz tespiti yapılıyor...")
net.setInput(blob)
detections = net.forward()

for i in range(0, detections.shape[2]):
    confidence = detections[0, 0, i, 2]
    if confidence > guven_esigi:
        box = detections[0, 0, i, 3:7] * np.array([w, h, w, h])
        (startX, startY, endX, endY) = box.astype("int")
        cv2.rectangle(image, (startX, startY), (endX, endY), (0, 255, 0), 2)
        text = "{:.2f}%".format(confidence * 100)
        y = startY - 10 if startY - 10 > 10 else startY + 10
        cv2.putText(image, text, (startX, y),
            cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 255, 0), 2)

print("[BİLGİ] Sonuç gösteriliyor. Kapatmak için bir tuşa basın.")
cv2.imshow("Yuz Tespiti Sonucu", image)
cv2.waitKey(0)
cv2.destroyAllWindows()
