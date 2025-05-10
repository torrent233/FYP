from flask import Flask, request, jsonify, send_file
from PIL import Image
import torch
from torchvision import models, transforms
import io
import os
import torch.nn as nn

app = Flask(__name__)

# 共享状态
lock_flag = False

class_names = ['harmful', 'no_porn', 'porn']

val_transforms = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                         std=[0.229, 0.224, 0.225])
])

device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
model = models.resnet18(weights=None)
model.fc = nn.Sequential(
    nn.Dropout(0.5),
    nn.Linear(model.fc.in_features, 3)
)
model.load_state_dict(torch.load('best_resnet18_model.pth', map_location=device))
model.to(device)
model.eval()

def predict_image(img: Image.Image):
    input_tensor = val_transforms(img).unsqueeze(0).to(device)
    with torch.no_grad():
        outputs = model(input_tensor)
        probs = torch.nn.functional.softmax(outputs, dim=1)
        conf, pred = torch.max(probs, 1)
    return class_names[pred.item()], conf.item()

@app.route('/analyze', methods=['POST'])
def analyze():
    global lock_flag

    # ✅ 如果父母设置了锁定，就直接返回伪造“违规标签”
    if lock_flag:
        lock_flag = False  # ✅ 只触发一次锁定
        return jsonify({
            "label": "parent",
            "confidence": 0.99
        })

    if 'frame' not in request.files:
        return jsonify({"error": "No file uploaded"}), 400

    file = request.files['frame']
    image_bytes = file.read()

    with open("latest.jpg", "wb") as f:
        f.write(image_bytes)

    img = Image.open(io.BytesIO(image_bytes)).convert('RGB')
    label, conf = predict_image(img)

    if label in ['porn', 'harmful'] and conf >= 0.8:
        lock_flag = True

    return jsonify({
        "label": label,
        "confidence": round(conf, 4)
    })

@app.route('/lock', methods=['POST'])
def lock_from_parent():
    global lock_flag
    lock_flag = True
    return jsonify({"status": "lock_set_by_parent"})

@app.route('/lock_status', methods=['GET'])
def lock_status():
    # ✅ 同样这里也返回伪造信息以兼容轮询机制
    if lock_flag:
        return jsonify({
            "label": "parent",
            "confidence": 0.99
        })
    return jsonify({
        "label": "none",
        "confidence": 0.0
    })

@app.route('/unlock', methods=['POST'])
def unlock():
    global lock_flag
    lock_flag = False
    return jsonify({"status": "unlocked"})

@app.route('/latest_image', methods=['GET'])
def latest_image():
    if os.path.exists("latest.jpg"):
        return send_file('latest.jpg', mimetype='image/jpeg')
    else:
        return jsonify({"error": "No image found"}), 404

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
