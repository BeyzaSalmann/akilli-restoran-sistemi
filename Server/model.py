import cv2
import numpy as np
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing.image import img_to_array
import os

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
load_dotenv(os.path.join(_BASE_DIR, "..", ".env"))

MODEL_PATH = os.environ.get("EMOTION_MODEL_PATH", "final_stable_model.h5")
model = load_model(MODEL_PATH)
class_labels = ["angry", "contempt", "disgust", "fear", "happy", "sadness", "surprise"]
face_classifier = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_frontalface_default.xml")

cap = cv2.VideoCapture(0)

while True:
    ret, frame = cap.read()
    if not ret:
        break

    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    faces = face_classifier.detectMultiScale(gray, 1.3, 5)

    for (x, y, w, h) in faces:
        cv2.rectangle(frame, (x, y), (x + w, y + h), (255, 0, 0), 2)

        x_new = x + int(w * 0.1)
        y_new = y + int(h * 0.1)
        w_new = int(w * 0.8)
        h_new = int(h * 0.8)
        roi_gray = gray[y_new:y_new + h_new, x_new:x_new + w_new]

        try:
            roi_gray = cv2.resize(roi_gray, (48, 48), interpolation=cv2.INTER_AREA)
            roi = roi_gray.astype("float")
            roi = img_to_array(roi)
            roi = np.expand_dims(roi, axis=0)

            prediction = model.predict(roi, verbose=0)[0]
            max_index = prediction.argmax()
            label = class_labels[max_index]
            confidence = prediction[max_index] * 100

            color = (0, 255, 0) if confidence > 50 else (0, 0, 255)
            cv2.putText(frame, f"{label} ({confidence:.1f}%)", (x, y - 10),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.8, color, 2)
        except Exception as e:
            print(e)
            continue

    cv2.imshow("Duygu Analizi", frame)
    if cv2.waitKey(1) & 0xFF == ord("q"):
        break

cap.release()
cv2.destroyAllWindows()
