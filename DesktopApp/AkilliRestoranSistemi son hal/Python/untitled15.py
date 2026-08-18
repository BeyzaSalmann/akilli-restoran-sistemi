# -*- coding: utf-8 -*-
"""MobileNetV2 transfer öğrenmesi ile duygu tanıma modeli eğitimi."""

import tensorflow as tf
from tensorflow.keras.preprocessing import image_dataset_from_directory
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Dense, GlobalAveragePooling2D, Dropout, Input
from tensorflow.keras.applications import MobileNetV2
from tensorflow.keras.optimizers import Adam
import matplotlib.pyplot as plt

TRAIN_DIR = "veri_seti/train"
VAL_DIR = "veri_seti/validation"
IMAGE_SIZE = (96, 96)
BATCH_SIZE = 32
EPOCHS = 15

# MobileNetV2 ImageNet ağırlıkları 3 kanallı girdi bekler.
train_dataset = image_dataset_from_directory(
    TRAIN_DIR,
    label_mode="int",
    image_size=IMAGE_SIZE,
    batch_size=BATCH_SIZE,
    color_mode="rgb",
)

validation_dataset = image_dataset_from_directory(
    VAL_DIR,
    label_mode="int",
    image_size=IMAGE_SIZE,
    batch_size=BATCH_SIZE,
    color_mode="rgb",
)

class_names = train_dataset.class_names
print("Bulunan Sınıflar:", class_names)
NUM_CLASSES = len(class_names)

AUTOTUNE = tf.data.AUTOTUNE
train_dataset = train_dataset.prefetch(buffer_size=AUTOTUNE)
validation_dataset = validation_dataset.prefetch(buffer_size=AUTOTUNE)

inputs = Input(shape=(IMAGE_SIZE[0], IMAGE_SIZE[1], 3))
x = tf.keras.applications.mobilenet_v2.preprocess_input(inputs)

base_model = MobileNetV2(
    input_shape=(IMAGE_SIZE[0], IMAGE_SIZE[1], 3),
    include_top=False,
    weights="imagenet",
)
base_model.trainable = False
x = base_model(x, training=False)

x = GlobalAveragePooling2D()(x)
x = Dropout(0.3)(x)
x = Dense(128, activation="relu")(x)
outputs = Dense(NUM_CLASSES, activation="softmax")(x)

model = Model(inputs, outputs)
model.compile(
    optimizer=Adam(learning_rate=0.001),
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"],
)
model.summary()

print(f"\nModel {EPOCHS} epoch için eğitiliyor...")
history = model.fit(
    train_dataset,
    validation_data=validation_dataset,
    epochs=EPOCHS,
)
print("İlk eğitim tamamlandı.")

model.save("duygu_tanima_model_v1.h5")
print("Model 'duygu_tanima_model_v1.h5' olarak kaydedildi.")

acc = history.history["accuracy"]
val_acc = history.history["validation_accuracy"]
loss = history.history["loss"]
val_loss = history.history["validation_loss"]
epochs_range = range(EPOCHS)

plt.figure(figsize=(12, 4))
plt.subplot(1, 2, 1)
plt.plot(epochs_range, acc, label="Eğitim Doğruluğu")
plt.plot(epochs_range, val_acc, label="Doğrulama Doğruluğu")
plt.legend(loc="lower right")
plt.title("Eğitim ve Doğrulama Doğruluğu")

plt.subplot(1, 2, 2)
plt.plot(epochs_range, loss, label="Eğitim Kaybı")
plt.plot(epochs_range, val_loss, label="Doğrulama Kaybı")
plt.legend(loc="upper right")
plt.title("Eğitim ve Doğrulama Kaybı")
plt.show()
