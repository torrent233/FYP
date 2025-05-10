import os
import cv2

# Function to resize the image to a fixed size
def resize_image(image, size=(224, 224)):
    """
    调整图像大小为指定尺寸
    :param image: 输入的图像
    :param size: 目标尺寸 (宽, 高)，默认是 (224, 224)
    :return: 调整后的图像
    """
    resized_image = cv2.resize(image, size)
    return resized_image

# Function to process each video
def process_video(video_path, output_dir, fps=1, size=(224, 224)):

    video_capture = cv2.VideoCapture(video_path)
    total_frames = int(video_capture.get(cv2.CAP_PROP_FRAME_COUNT))
    video_fps = video_capture.get(cv2.CAP_PROP_FPS)
    step = int(video_fps / fps)  # 控制提取的帧率

    frame_count = 0
    extracted_count = 0
    while video_capture.isOpened():
        ret, frame = video_capture.read()
        if not ret:
            break  # 视频读取结束
        if frame_count % step == 0:  # 每隔指定帧数提取一次
            # 处理图像：将图像调整为指定大小
            frame = resize_image(frame, size=size)
            # 保存处理后的图像
            frame_filename = os.path.join(output_dir, f"{os.path.basename(video_path)}_frame_{extracted_count}.jpg")
            cv2.imwrite(frame_filename, frame)
            extracted_count += 1
        frame_count += 1
    video_capture.release()

# Function to process all videos in a folder
def process_all_videos_in_folder(folder_path, output_folder, fps=1, size=(224, 224)):
    """
    处理文件夹中的所有视频文件，并将每帧调整为指定大小
    :param folder_path: 输入视频文件夹的路径
    :param output_folder: 保存处理后图像的文件夹
    :param fps: 每秒提取的帧数
    :param size: 调整图像的目标尺寸，默认 (224, 224)
    """
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    for filename in os.listdir(folder_path):
        if filename.endswith((".mp4", ".avi", ".mkv")):  # 处理特定格式的视频文件
            video_path = os.path.join(folder_path, filename)
            print(f"Processing video: {video_path}")

            process_video(video_path, output_folder, fps=fps, size=size)
def rename_files_in_folder(folder_path, start_num=1):
    """
    将文件夹中的所有文件按照顺序重命名为数字。
    :param folder_path: 文件夹的路径
    :param start_num: 重命名的起始数字（默认从1开始）
    """
    if not os.path.exists(folder_path):
        print(f"文件夹路径不存在: {folder_path}")
        return

    # 获取文件夹中的所有文件
    files = sorted(os.listdir(folder_path))

    # 过滤掉隐藏文件和文件夹，只处理文件
    files = [f for f in files if os.path.isfile(os.path.join(folder_path, f))]

    # 遍历所有文件并重命名
    for idx, filename in enumerate(files, start=start_num):
        file_extension = os.path.splitext(filename)[1]  # 获取文件扩展名
        new_name = f"{idx}{file_extension}"  # 生成新的文件名
        old_file = os.path.join(folder_path, filename)
        new_file = os.path.join(folder_path, new_name)

        # 重命名文件
        os.rename(old_file, new_file)
        print(f"文件 {filename} 已重命名为 {new_name}")


# 示例用法
folder_path = r'C:\Users\hp\Desktop\guonei\safe'  # 输入视频文件夹路径
rename_files_in_folder(folder_path, start_num=1)
output_folder = r'D:\FYP2\FYP\complete\safe'  # 处理后图像保存的文件夹
fps = 1  # 每秒提取的帧数
size = (224, 224)  # 统一的图像尺寸
process_all_videos_in_folder(folder_path, output_folder, fps, size)
