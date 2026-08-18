# -*- coding: utf-8 -*-
"""untitled15.py sonrası MobileNetV2 ince ayar (fine-tuning) eğitimi."""

from tensorflow.keras.optimizers import Adam
import matplotlib.pyplot as plt

initial_epochs = EPOCHS
FINE_TUNE_EPOCHS = 10
LOW_LEARNING_RATE = 0.00001
total_epochs = initial_epochs + FINE_TUNE_EPOCHS

print("[BİLGİ] İnce ayar için temel model çözülüyor...")
base_model.trainable = True
print(f"Temel modeldeki toplam katman sayısı: {len(base_model.layers)}")

fine_tune_at = 100
for layer in base_model.layers[:fine_tune_at]:
    layer.trainable = False

print(f"Temel modelin ilk {fine_tune_at} katmanı donduruldu, geri kalanı çözüldü.")
print("[BİLGİ] Model, ince ayar için yeniden derleniyor...")
model.compile(
    optimizer=Adam(learning_rate=LOW_LEARNING_RATE),
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"],
)
model.summary()

print(f"[BİLGİ] {FINE_TUNE_EPOCHS} epoch için ince ayar başlıyor...")
history_fine_tune = model.fit(
    train_dataset,
    validation_data=validation_dataset,
    epochs=total_epochs,
    initial_epoch=history.epoch[-1] + 1,
)
print("İnce ayar eğitimi tamamlandı.")

model.save("duygu_tanima_model_final.h5")
print("İnce ayarı yapılmış model 'duygu_tanima_model_final.h5' olarak kaydedildi.")

acc = history.history["accuracy"]
val_acc = history.history["validation_accuracy"]
loss = history.history["loss"]
val_loss = history.history["validation_loss"]

acc.extend(history_fine_tune.history["accuracy"])
val_acc.extend(history_fine_tune.history["validation_accuracy"])
loss.extend(history_fine_tune.history["loss"])
val_loss.extend(history_fine_tune.history["validation_loss"])

plt.figure(figsize=(12, 6))
plt.subplot(2, 1, 1)
plt.plot(acc, label="Eğitim Doğruluğu")
plt.plot(val_acc, label="Doğrulama Doğruluğu")
plt.axvline(x=initial_epochs - 1, label="İnce Ayar Başlangıcı (Fine-Tune)", color="r", linestyle="--")
plt.ylim([min(plt.ylim()), 1])
plt.title("Eğitim ve Doğrulama Doğruluğu (Tüm Aşamalar)")
plt.legend(loc="lower right")

plt.subplot(2, 1, 2)
plt.plot(loss, label="Eğitim Kaybı")
plt.plot(val_loss, label="Doğrulama Kaybı")
plt.axvline(x=initial_epochs - 1, label="İnce Ayar Başlangıcı (Fine-Tune)", color="r", linestyle="--")
plt.title("Eğitim ve Doğrulama Kaybı (Tüm Aşamalar)")
plt.legend(loc="upper right")
plt.xlabel("Epoch")
plt.tight_layout()
plt.show()
