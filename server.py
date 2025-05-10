from flask import Flask, request, jsonify
import os

# 创建 Flask 应用
app = Flask(__name__)

# 设置接收文件的保存路径
UPLOAD_FOLDER = 'uploads'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# 全局变量用于交替返回 true/false
toggle = True

@app.route('/upload', methods=['POST'])
def upload_file():
    global toggle

    if 'image' not in request.files:
        return jsonify({"error": "No file part"}), 400

    file = request.files['image']
    if file.filename == '':
        return jsonify({"error": "No selected file"}), 400

    # 保存文件
    file_path = os.path.join(UPLOAD_FOLDER, file.filename)
    file.save(file_path)
    print(f"Received image and saved to {file_path}")

    # 返回交替的 true/false 值
    response_value = "true" if toggle else "false"
    toggle = not toggle  # 交替切换 true 和 false

    return jsonify(response_value)

# 启动服务器
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
