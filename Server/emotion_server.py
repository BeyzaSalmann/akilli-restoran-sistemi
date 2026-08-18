import cv2
import json
import os
import socket
import struct
import sys
import time
import numpy as np
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing.image import img_to_array
from camera_device import open_preferred_camera as open_camera

sys.stdout.reconfigure(line_buffering=True)


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


def placeholder_frame(message):
    frame = np.zeros((480, 640, 3), dtype=np.uint8)
    cv2.putText(frame, message, (40, 240), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (200, 200, 200), 2)
    return frame


def send_packet(conn, frame, primary_emotion, detected_emotions, face_count):
    metadata = {
        "count": face_count,
        "primary_emotion": primary_emotion,
        "details": detected_emotions,
    }
    json_data = json.dumps(metadata).encode("utf-8")
    _, img_encoded = cv2.imencode(".jpg", frame)
    img_data = img_encoded.tobytes()
    conn.sendall(struct.pack(">I", len(json_data)) + json_data + struct.pack(">I", len(img_data)) + img_data)


_BASE_DIR = os.path.dirname(os.path.abspath(__file__))
load_dotenv(os.path.join(_BASE_DIR, ".env"))
load_dotenv(os.path.join(_BASE_DIR, "..", ".env"))

HOST = os.environ.get("EMOTION_HOST", "0.0.0.0")
PORT = int(os.environ.get("EMOTION_SERVER_PORT", "9999"))
MODEL_PATH = os.environ.get("EMOTION_MODEL_PATH", "final_stable_model.h5")
if not os.path.isabs(MODEL_PATH):
    MODEL_PATH = os.path.join(_BASE_DIR, MODEL_PATH)

print("Duygu modeli yükleniyor...")
model = load_model(MODEL_PATH)
class_labels = ["Kizgin", "Kucumseme", "Tiksinme", "Korku", "Mutlu", "Uzgun", "Saskin"]
face_classifier = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")

print("Kamera açılıyor...")
cap = open_camera()
if cap is None:
    print("Kamera açılamadı; yer tutucu görüntü gönderilecek.")

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server_socket.bind((HOST, PORT))
server_socket.listen(1)
print(f"Duygu analiz sunucusu hazır ({HOST}:{PORT}). Java bekleniyor...")

try:
    while True:
        conn, addr = server_socket.accept()
        print(f"Java bağlandı: {addr}")
        try:
            while True:
                if cap is not None:
                    ret, frame = cap.read()
                    if not ret:
                        frame = placeholder_frame("Kamera bekleniyor...")
                else:
                    frame = placeholder_frame("Kamera bulunamadi")
                    time.sleep(0.05)

                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                faces = face_classifier.detectMultiScale(gray, 1.3, 5) if cap is not None else []

                detected_emotions = []
                primary_emotion = "Nötr"
                max_conf_global = 0

                for (x, y, w, h) in faces:
                    cv2.rectangle(frame, (x, y), (x + w, y + h), (255, 0, 0), 2)
                    roi_gray = gray[y:y + h, x:x + w]
                    try:
                        roi_gray = cv2.resize(roi_gray, (48, 48), interpolation=cv2.INTER_AREA)
                        roi = roi_gray.astype("float") / 255.0
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
                        print(f"Analiz hatası: {e}", flush=True)

                send_packet(conn, frame, primary_emotion, detected_emotions, len(faces))
        except (BrokenPipeError, ConnectionResetError, OSError) as e:
            print(f"İstemci bağlantısı kapandı: {e}", flush=True)
        finally:
            try:
                conn.close()
            except Exception:
                pass
            print("Yeni Java bağlantısı bekleniyor...", flush=True)
finally:
    if cap is not None:
        cap.release()
    server_socket.close()
