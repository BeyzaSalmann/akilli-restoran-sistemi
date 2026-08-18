"""macOS'ta iPhone Continuity Camera yerine yerleşik kamerayı seçer."""

import json
import os
import subprocess
import sys

import cv2


PHONE_MARKERS = ("iphone", "ipad", "continuity", "desk view")
BUILTIN_MARKERS = ("facetime", "built-in", "built in", "macbook", "hd kamera", "hd camera")


def _normalize(text):
    return (text or "").casefold()


def _is_phone_camera(name, model_id=""):
    blob = f"{name} {model_id}"
    return any(marker in _normalize(blob) for marker in PHONE_MARKERS)


def _is_builtin_camera(name, model_id=""):
    blob = f"{name} {model_id}"
    return any(marker in _normalize(blob) for marker in BUILTIN_MARKERS) and not _is_phone_camera(name, model_id)


def _mac_camera_info():
    if sys.platform != "darwin":
        return []
    try:
        raw = subprocess.check_output(
            ["system_profiler", "SPCameraDataType", "-json"],
            stderr=subprocess.DEVNULL,
        )
        payload = json.loads(raw.decode("utf-8"))
    except (OSError, json.JSONDecodeError, subprocess.CalledProcessError):
        return []

    cameras = []
    for device in payload.get("SPCameraDataType", []):
        cameras.append({
            "name": device.get("_name", ""),
            "model": device.get("spcamera_model-id", ""),
            "unique_id": device.get("spcamera_unique-id", ""),
        })
    return cameras


def _try_open(source, backend):
    cap = cv2.VideoCapture(source, backend)
    if cap.isOpened():
        cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
        return cap
    cap.release()
    return None


def open_preferred_camera():
    """Yerleşik Mac kamerasını açar; iPhone Continuity Camera'yı atlar."""
    backends = []
    if hasattr(cv2, "CAP_AVFOUNDATION"):
        backends.append(cv2.CAP_AVFOUNDATION)
    backends.append(cv2.CAP_ANY)

    index_override = os.environ.get("CAMERA_INDEX", "").strip()
    if index_override:
        index = int(index_override)
        for backend in backends:
            cap = _try_open(index, backend)
            if cap is not None:
                print(f"Kamera CAMERA_INDEX={index} ile açıldı.")
                return cap
        print(f"CAMERA_INDEX={index_override} açılamadı, otomatik seçime geçiliyor.")

    cameras = _mac_camera_info()
    preferred = [c for c in cameras if _is_builtin_camera(c["name"], c["model"])]
    skipped = [c for c in cameras if _is_phone_camera(c["name"], c["model"])]
    if cameras:
        print("Algılanan kameralar:")
        for cam in cameras:
            tag = " (atlanacak)" if _is_phone_camera(cam["name"], cam["model"]) else ""
            print(f"  - {cam['name']}{tag}")

    sources = []
    if os.environ.get("CAMERA_NAME", "").strip():
        sources.append(os.environ["CAMERA_NAME"].strip())
    for cam in preferred:
        if cam.get("unique_id"):
            sources.append(cam["unique_id"])
        if cam.get("name"):
            sources.append(cam["name"])

    for source in sources:
        for backend in backends:
            cap = _try_open(source, backend)
            if cap is not None:
                print(f"Kamera açıldı: {source}")
                return cap

    indices = list(range(6))
    if skipped and len(cameras) > 1:
        # Continuity Camera macOS'ta genelde 0. indekstir; yerleşik kamera 1 olur.
        indices = [1, 2, 3, 4, 5, 0]

    for index in indices:
        for backend in backends:
            cap = _try_open(index, backend)
            if cap is not None:
                print(f"Kamera indeks {index} ile açıldı.")
                return cap

    return None
