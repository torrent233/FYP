import torch
import torch.nn as nn
from torch.utils.data import DataLoader
import torchvision.transforms as transforms
from torchvision.datasets import ImageFolder
from sklearn.metrics import classification_report, confusion_matrix
from torchvision import datasets, models, transforms
# 1. 加载预训练的模型
model = models.resnet18(pretrained=False)
num_features = model.fc.in_features
model.fc = nn.Linear(num_features, 3)  # 输出3类
model.load_state_dict(torch.load('best_resnet18_model.pth'))  # 加载训练好的模型权重

# 2. 设置模型为评估模式
model.eval()

# 3. 数据预处理（与训练时相同）
transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
])

# 4. 加载验证集
val_dataset = ImageFolder('path_to_val_data', transform=transform)
val_loader = DataLoader(val_dataset, batch_size=32, shuffle=False)

# 5. 评估模型
true_labels = []
pred_labels = []

with torch.no_grad():  # 暂时计算梯度，节省内存
    for inputs, labels in val_loader:
        outputs = model(inputs)
        _, preds = torch.max(outputs, 1)

        # 收集真实标签和预测标签
        true_labels.extend(labels.cpu().numpy())
        pred_labels.extend(preds.cpu().numpy())

# 6. 计算准确率、精确率、召回率、F1分数
print(classification_report(true_labels, pred_labels, target_names=val_dataset.classes))

# 7. 生成混淆矩阵
conf_matrix = confusion_matrix(true_labels, pred_labels)
print("Confusion Matrix:\n", conf_matrix)
