/*package com.example.screenrecorder

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ScreenRecordService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED)
        val data = intent?.getParcelableExtra<Intent>("data")

        if (resultCode != null && data != null) {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = manager.getMediaProjection(resultCode, data)

            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.w("ScreenRecord", "MediaProjection 被系统强制关闭")
                    stopSelf()
                }
            }, Handler(Looper.getMainLooper()))

            startRecording()
        }

        return START_NOT_STICKY
    }

    private fun startRecording() {
        val metrics = DisplayMetrics()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val dpi = metrics.densityDpi

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val resolver = applicationContext.contentResolver
        val videoName = "Record_$timestamp.mp4"

        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, videoName)
            put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES) // 保存到系统Movies文件夹
            put(android.provider.MediaStore.Video.Media.IS_PENDING, 1) // 在写入中
        }


        val videoUri = resolver.insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        val outputFd = resolver.openFileDescriptor(videoUri!!, "w")?.fileDescriptor

        mediaRecorder = MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoSize(width, height)
            setVideoEncodingBitRate(5 * 1024 * 1024)
            setVideoFrameRate(30)
            setOutputFile(outputFd!!)  // 👈 设置目标文件描述符
            prepare()
            start()
        }

// 创建虚拟显示
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder!!.surface, null, null
        )

        Log.d("ScreenRecord", "录制开始: content://media/Movies/$videoName")

// 自动 3 秒后停止并发布视频
        Handler(Looper.getMainLooper()).postDelayed({
            // ✅ 写入完成标记为 done
            contentValues.clear()
            contentValues.put(android.provider.MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(videoUri, contentValues, null, null)

            stopSelf()
        }, 30000)

    }

    override fun onDestroy() {
        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }
        virtualDisplay?.release()
        mediaProjection?.stop()

        Log.d("ScreenRecord", "录制已结束")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "record_channel")
            .setContentTitle("屏幕录制中")
            .setContentText("正在后台录制3秒...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel("record_channel", "录屏服务", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(chan)
        }
    }
}
*/

package com.example.screenrecorder
import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.screenrecorder.capture.ScreenCaptureEngine
import java.io.OutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ScreenRecordService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var screenCaptureEngine: ScreenCaptureEngine? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED)
        val data = intent?.getParcelableExtra<Intent>("data")

        if (resultCode != null && data != null) {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = manager.getMediaProjection(resultCode, data)

            startRecording()
        }

        return START_NOT_STICKY
    }

    private fun startRecording() {
        val metrics = DisplayMetrics()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val dpi = metrics.densityDpi

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(getExternalFilesDir(null), "Screen_$timestamp.mp4")

        screenCaptureEngine = ScreenCaptureEngine(
            context = this,
            width = width,
            height = height,
            fps = 30,
            onFrameCaptured = { bmp: Bitmap ->
                saveFrameToGallery(bmp)
            }
        )

        // 设置回调：当 OpenGL Surface 初始化完毕后才创建 VirtualDisplay
        screenCaptureEngine?.setOnSurfaceReadyCallback {
            mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width, height, dpi,
                0,
                screenCaptureEngine!!.getVirtualDisplaySurface(),
                null, null
            )
            Log.d("ScreenRecord", "🎥 VirtualDisplay 创建完成")
        }

        screenCaptureEngine?.start(outputFile)

        // 注册回调：MediaProjection 被系统中断时自动停止服务
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.w("ScreenRecord", "⚠️ MediaProjection 被系统关闭")
                stopSelf()
            }
        }, Handler(Looper.getMainLooper()))

        // 自动停止录制（30秒）
        Handler(Looper.getMainLooper()).postDelayed({
            stopSelf()
        }, 90_000)

        Log.d("ScreenRecord", "✅ 开始录制: ${outputFile.absolutePath}")
    }

    private fun saveFrameToGallery(bitmap: Bitmap) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Frame_$timestamp.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ScreenFrames")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            val outputStream: OutputStream? = contentResolver.openOutputStream(it)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream!!)
            outputStream.close()
            Log.d("ScreenRecord", "🖼️ 抽帧已保存至相册")
        }
    }

    override fun onDestroy() {
        try {
            screenCaptureEngine?.stop()
        } catch (e: Exception) {
            Log.e("ScreenRecord", "停止引擎失败: ${e.message}")
        }

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.e("ScreenRecord", "停止MediaProjection失败: ${e.message}")
        }

        super.onDestroy()
        Log.d("ScreenRecord", "⏹️ 录制结束")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "record_channel")
            .setContentTitle("屏幕录制中")
            .setContentText("后台录制 + 抽帧中...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "record_channel",
                "录屏服务",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
