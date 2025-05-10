# Updated realistic Loss, Accuracy, Precision, Recall, and F1 score graph generation

import matplotlib.pyplot as plt
import numpy as np

# Epochs
epochs = np.arange(1, 51)

# Loss curves
training_loss = np.concatenate([
    np.linspace(1.0, 0.5, int(len(epochs) * 0.6)),
    np.linspace(0.5, 0.4, int(len(epochs) * 0.4))
]) + np.random.normal(0, 0.02, len(epochs))

validation_loss = np.concatenate([
    np.linspace(1.2, 0.6, int(len(epochs) * 0.4)),
    np.linspace(0.6, 0.5, int(len(epochs) * 0.6))
]) + np.random.normal(0, 0.02, len(epochs))

# Accuracy curves
training_accuracy = np.concatenate([
    np.linspace(0.7, 0.9, int(len(epochs) * 0.5)),
    np.linspace(0.9, 0.92, int(len(epochs) * 0.5))
]) + np.random.normal(0, 0.005, len(epochs))

validation_accuracy = np.concatenate([
    np.linspace(0.65, 0.85, int(len(epochs) * 0.3)),
    np.linspace(0.85, 0.88, int(len(epochs) * 0.7))
]) + np.random.normal(0, 0.01, len(epochs))

# Precision, Recall, and F1 score
precision = validation_accuracy + np.random.normal(0, 0.005, len(epochs))  # Slightly higher than Validation Accuracy
recall = validation_accuracy - np.random.normal(0.01, 0.005, len(epochs))  # Slightly lower than Validation Accuracy
f1_score = 2 * (precision * recall) / (precision + recall)  # F1 Score calculation

# Plot Loss and Accuracy
plt.figure(figsize=(10, 6))

plt.subplot(2, 1, 1)  # First subplot: Loss
plt.plot(epochs, training_loss, label="Training Loss", color='blue', linewidth=2)
plt.plot(epochs, validation_loss, label="Validation Loss", color='orange', linewidth=2)
plt.title("Loss over Epochs")
plt.xlabel("Epochs")
plt.ylabel("Loss")
plt.legend()
plt.grid(True)

plt.subplot(2, 1, 2)  # Second subplot: Accuracy
plt.plot(epochs, training_accuracy, label="Training Accuracy", color='green', linewidth=2)
plt.plot(epochs, validation_accuracy, label="Validation Accuracy", color='red', linewidth=2)
plt.title("Accuracy over Epochs")
plt.xlabel("Epochs")
plt.ylabel("Accuracy")
plt.legend()
plt.grid(True)

plt.tight_layout()
plt.show()

# Plot Precision, Recall, and F1 Score
plt.figure(figsize=(8, 5))
plt.plot(epochs, precision, label="Precision", linewidth=2, color='blue')
plt.plot(epochs, recall, label="Recall", linewidth=2, color='orange')
plt.plot(epochs, f1_score, label="F1 Score", linewidth=2, color='purple')
plt.title("Precision, Recall, and F1 Score over Epochs")
plt.xlabel("Epochs")
plt.ylabel("Score")
plt.ylim(0.4, 1.0)
plt.legend()
plt.grid(True)
plt.tight_layout()
plt.show()
