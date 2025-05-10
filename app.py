from flask import Flask, request, jsonify
from PIL import Image
import torch
from torchvision import models, transforms
import io
import torch
import torch.nn as nn
app = Flask(__name__)

# 类别标签（需要与训练时一致）
class_names = ['harmful', 'no_porn', 'porn']

# 图像预处理步骤（要和训练时的 val_transforms 一致）
val_transforms = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                         std=[0.229, 0.224, 0.225])
])

# 加载模型
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
model = models.resnet18(weights=None)
model.fc = nn.Sequential(
    nn.Dropout(0.5),        # 加这个是为了对上 key
    nn.Linear(model.fc.in_features, 3)
)

model.load_state_dict(torch.load('best_resnet18_model.pth', map_location=device))
model.to(device)
model.eval()

def predict_image(img: Image.Image):
    # 预处理图像
    input_tensor = val_transforms(img).unsqueeze(0).to(device)

    with torch.no_grad():
        outputs = model(input_tensor)
        probs = torch.nn.functional.softmax(outputs, dim=1)
        conf, pred = torch.max(probs, 1)

    return class_names[pred.item()], conf.item()

@app.route('/analyze', methods=['POST'])
def analyze():
    if 'frame' not in request.files:
        return jsonify({"error": "No file uploaded"}), 400

    file = request.files['frame']
    img = Image.open(file.stream).convert('RGB')

    label, conf = predict_image(img)

    return jsonify({
        "label": label,
        "confidence": round(conf, 4)
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
