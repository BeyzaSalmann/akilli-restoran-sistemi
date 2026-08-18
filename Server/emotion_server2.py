import cv2
import json
import os
import socket
import struct
import numpy as np
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing.image import img_to_array
from camera_device import open_preferred_camera


def load_dotenv(path):
    if not os.path.isfile(path):
        return
    with open(path, encoding="utf-8") as env_file:
        for raw in env_file:
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            key = key.strip()
            value = value.strip().strip('"').strip("'")
            os.environ.setdefault(key, value)


_BASE_DIR = os.path.dirname(os.path.abspath(__file__))
load_dotenv(os.path.join(_BASE_DIR, ".env"))
load_dotenv(os.path.join(_BASE_DIR, "..", ".env"))

HOST = os.environ.get("EMOTION_HOST", "0.0.0.0")
PORT = int(os.environ.get("EMOTION_SERVER_PORT", "9999"))
MODEL_PATH = os.environ.get("EMOTION_MODEL_PATH", "final_stable_model.h5")

print("Duygu modeli yükleniyor...")
model = load_model(MODEL_PATH)
# Eğitim sırası: angry, notr, disgust, fear, happy, sadness, surprise
class_labels = ["Kizgin", "Notr", "Tiksinme", "Korku", "Mutlu", "Uzgun", "Saskin"]
face_classifier = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server_socket.bind((HOST, PORT))
server_socket.listen(1)
print(f"Duygu analiz sunucusu hazır ({HOST}:{PORT}). Java bekleniyor...")

conn, addr = server_socket.accept()
print(f"Java bağlandı: {addr}")

cap = open_preferred_camera()
if cap is None:
    raise RuntimeError("Kamera açılamadı.")

try:
    while True:
        ret, frame = cap.read()
        if not ret:
            break

        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = face_classifier.detectMultiScale(gray, 1.3, 5)

        detected_emotions = []
        primary_emotion = "Nötr"
        max_conf_global = 0

        for (x, y, w, h) in faces:
            cv2.rectangle(frame, (x, y), (x + w, y + h), (255, 0, 0), 2)

            x_new = x + int(w * 0.1)
            y_new = y + int(h * 0.1)
            w_new = int(w * 0.8)
            h_new = int(h * 0.8)
            roi_gray = gray[y_new:y_new + h_new, x_new:x_new + w_new]

            try:
                roi_gray = cv2.resize(roi_gray, (48, 48), interpolation=cv2.INTER_AREA)
                # Model rescaling katmanı içerdiği için 0-255 aralığı korunur.
                roi = roi_gray.astype("float")
                roi = img_to_array(roi)
                roi = np.expand_dims(roi, axis=0)

                prediction = model.predict(roi, verbose=0)[0]
                max_index = prediction.argmax()
                label = class_labels[max_index]
                confidence = float(prediction[max_index] * 100)

                color = (0, 255, 0) if confidence > 50 else (0, 0, 255)
                cv2.putText(frame, f"{label} %{confidence:.1f}", (x, y - 10),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.8, color, 2)

                detected_emotions.append({
                    "emotion": label,
                    "confidence": confidence,
                    "box": [int(x), int(y), int(w), int(h)],
                })

                if confidence > max_conf_global:
                    max_conf_global = confidence
                    primary_emotion = label

            except Exception as e:
                print(f"Analiz hatası: {e}")

        metadata = {
            "count": len(faces),
            "primary_emotion": primary_emotion,
            "details": detected_emotions,
        }
        json_data = json.dumps(metadata).encode("utf-8")
        _, img_encoded = cv2.imencode(".jpg", frame)
        img_data = img_encoded.tobytes()

        conn.sendall(struct.pack(">I", len(json_data)) + json_data + struct.pack(">I", len(img_data)) + img_data)

except Exception as e:
    print(f"Hata: {e}")
finally:
    cap.release()
    conn.close()
    server_socket.close()
