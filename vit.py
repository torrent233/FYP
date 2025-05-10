from transformers import ViTForImageClassification, ViTImageProcessor,AutoModelForImageClassification
from torchvision.datasets import ImageFolder
from torch.utils.data import DataLoader
from torchvision import transforms
import torch
from sklearn.metrics import classification_report
import os

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
"""
# 加载预处理器 & 模型（来自 HuggingFace）
processor = ViTImageProcessor.from_pretrained("facebook/deit-tiny-patch16-224")
model = ViTForImageClassification.from_pretrained("facebook/deit-tiny-patch16-224", num_labels=3, ignore_mismatched_sizes=True)
model.to(device)
model.eval()

# 数据路径
data_dir = r'D:\FYP2\FYP\path_to_val_data'

# 自定义处理器封装
def preprocess_fn(img):
    inputs = processor(images=img, return_tensors="pt")
    return inputs['pixel_values'].squeeze()

class ViTDataset(torch.utils.data.Dataset):
    def __init__(self, folder):
        self.data = ImageFolder(folder)
    def __getitem__(self, index):
        img, label = self.data[index]
        pixel_values = preprocess_fn(img)
        return pixel_values, label
    def __len__(self):
        return len(self.data)

dataset = ViTDataset(data_dir)
loader = DataLoader(dataset, batch_size=16, shuffle=False)

# 推理
all_preds, all_labels = [], []
with torch.no_grad():
    for inputs, labels in loader:
        inputs = inputs.to(device)
        labels = labels.to(device)
        outputs = model(inputs).logits
        preds = outputs.argmax(dim=-1)
        all_preds.extend(preds.cpu().tolist())
        all_labels.extend(labels.cpu().tolist())

print(classification_report(all_labels, all_preds, target_names=os.listdir(data_dir)))
# 保存为 Hugging Face 格式（包括 config 和 tokenizer）
model.save_pretrained("deit_tiny_finetuned/")
processor.save_pretrained("deit_tiny_finetuned/")

# 以后可直接使用：
# model = ViTForImageClassification.from_pretrained("deit_tiny_finetuned")
# processor = ViTImageProcessor.from_pretrained("deit_tiny_finetuned")


"""
ViTForImageClassification.from_pretrained("facebook/deit-tiny-patch16-224")
# 设置设备
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# 数据路径
train_path =  r'D:\FYP2\FYP\path_to_train_data'
val_path = r'D:\FYP2\FYP\path_to_val_data'

# 加载预处理器
processor = ViTImageProcessor.from_pretrained("facebook/deit-tiny-patch16-224")

# 转换器
transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(mean=processor.image_mean, std=processor.image_std)
])

# 加载数据集
train_dataset = ImageFolder(train_path, transform=transform)
val_dataset = ImageFolder(val_path, transform=transform)

train_loader = DataLoader(train_dataset, batch_size=16, shuffle=True)
val_loader = DataLoader(val_dataset, batch_size=16, shuffle=False)

# 加载预训练模型


model = AutoModelForImageClassification.from_pretrained("facebook/deit-tiny-patch16-224", num_labels=3, ignore_mismatched_sizes=True)
model.to(device)

# 训练设置
criterion = torch.nn.CrossEntropyLoss()
optimizer = torch.optim.AdamW(model.parameters(), lr=5e-5)

# 微调训练
def train(model, loader):
    model.train()
    total_loss = 0
    for inputs, labels in loader:
        inputs, labels = inputs.to(device), labels.to(device)
        outputs = model(pixel_values=inputs)
        loss = criterion(outputs.logits, labels)
        loss.backward()
        optimizer.step()
        optimizer.zero_grad()
        total_loss += loss.item()
    return total_loss / len(loader)

# 评估
def evaluate(model, loader):
    model.eval()
    preds, trues = [], []
    with torch.no_grad():
        for inputs, labels in loader:
            inputs, labels = inputs.to(device), labels.to(device)
            outputs = model(pixel_values=inputs)
            pred_labels = outputs.logits.argmax(dim=1)
            preds.extend(pred_labels.cpu().tolist())
            trues.extend(labels.cpu().tolist())
    return classification_report(trues, preds, target_names=val_dataset.classes)

# 训练和验证
for epoch in range(10):
    loss = train(model, train_loader)
    print(f"[Epoch {epoch+1}] Train Loss: {loss:.4f}")

print("\nValidation Report:")
print(evaluate(model, val_loader))
# 保存为 Hugging Face 格式（包括 config 和 tokenizer）
model.save_pretrained("deit_tiny_finetuned/")
processor.save_pretrained("deit_tiny_finetuned/")

# 以后可直接使用：
# model = ViTForImageClassification.from_pretrained("deit_tiny_finetuned")
# processor = ViTImageProcessor.from_pretrained("deit_tiny_finetuned")

