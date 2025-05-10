from PIL import Image
import cv2
import numpy as np
from PIL import Image

"""
def crop_to_content(image_path):
    # 读取图像
    image = cv2.imread(image_path)

    # 转换为灰度图像
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # 二值化处理
    _, thresh = cv2.threshold(gray, 1, 255, cv2.THRESH_BINARY)

    # 找到内容区域的边界框
    x, y, w, h = cv2.boundingRect(thresh)

    # 裁剪出内容区域
    crop_image = image[y:y + h, x:x + w]

    # 转换颜色通道从BGR到RGB
    crop_image = cv2.cvtColor(crop_image, cv2.COLOR_BGR2RGB)

    # 转换为PIL图像
    return Image.fromarray(crop_image)


# 加载和显示裁剪后的图像
cropped_image = crop_to_content('5.jpg')
cropped_image.show()
"""
import cv2
import numpy as np
from PIL import Image

def remove_top_bar(image, top_height=100):
    img_height, img_width, ch= image.shape
    if img_height <= top_height:
        return image

    # 裁剪掉顶部的状态栏部分
    cropped_image = image[top_height:img_height, 0:img_width]

    return cropped_image
def crop_to_content(image_path):
    image = cv2.imread(image_path)
    image = remove_top_bar(image, top_height=100)
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    _, thresh = cv2.threshold(gray, 1, 255, cv2.THRESH_BINARY)
    x, y, w, h = cv2.boundingRect(thresh)
    crop_image = image[y:y+h, x:x+w]
    crop_image = cv2.cvtColor(crop_image, cv2.COLOR_BGR2RGB)
    return Image.fromarray(crop_image)

cropped_image = crop_to_content('5.jpg')

import cv2
cropped_image_np=np.array(cropped_image)
# 调整图像大小，假设统一调整到224x224像素
resized_image = cv2.resize(cropped_image_np, (224, 224))
normalized_image = resized_image / 255.0  # 归一化到[0, 1]
cv2.imshow("Resized Image", resized_image)
cv2.waitKey(0)
cv2.destroyAllWindows()
