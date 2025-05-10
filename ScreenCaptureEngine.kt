package com.example.screenrecorder.capture

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.media.*
import android.net.Uri
import android.opengl.*
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import com.example.screenrecorder.utils.ScreenBlocker
private const val BLOCK_COOLDOWN_MS = 1000L
class ScreenCaptureEngine(
    private val context: Context,
    private val width: Int,
    private val height: Int,
    private val bitRate: Int = 5_000_000,
    private val fps: Int = 30,
    private val onFrameCaptured: ((Bitmap) -> Unit)? = null
) {
    private val TAG = "SafeScreenCaptureEngine"

    private lateinit var mediaCodec: MediaCodec
    private lateinit var mediaMuxer: MediaMuxer
    private lateinit var inputSurface: Surface
    private lateinit var eglDisplay: EGLDisplay
    private lateinit var eglContext: EGLContext
    private lateinit var eglSurface: EGLSurface
    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit var surface: Surface
    private lateinit var drawer: GLDrawer2D
    private lateinit var outputFile: File

    private var textureId: Int = -1
    private var videoTrackIndex = -1
    private var muxerStarted = false
    private var capturing = false
    private var glThread: Thread? = null
    private val frameSyncObject = Object()
    @Volatile private var frameAvailable = false

    private var onSurfaceReady: (() -> Unit)? = null

    fun setOnSurfaceReadyCallback(callback: () -> Unit) {
        onSurfaceReady = callback
    }

    fun getVirtualDisplaySurface(): Surface {
        check(::surface.isInitialized) { "Surface is not initialized yet!" }
        return surface
    }

    fun start(outputFile: File) {
        this.outputFile = outputFile
        setupEncoder()

        capturing = true
        glThread = Thread {
            try {
                setupGL()
                setupEGL()
                renderLoop()
            } catch (e: Exception) {
                Log.e(TAG, "GL thread failed: ${e.message}", e)
            }
        }
        glThread?.start()
    }

    private fun setupEncoder() {
        val safeWidth = width / 2 * 2
        val safeHeight = height / 2 * 2

        val format = MediaFormat.createVideoFormat("video/avc", safeWidth, safeHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        mediaCodec = MediaCodec.createEncoderByType("video/avc")
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = mediaCodec.createInputSurface()

        if (!inputSurface.isValid) {
            throw IllegalStateException("🚨 inputSurface is NOT valid!")
        }

        mediaCodec.start()
        mediaMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    private fun setupGL() {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture.setDefaultBufferSize(width, height)
        surface = Surface(surfaceTexture)
        onSurfaceReady?.invoke()
    }

    private fun setupEGL() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        EGL14.eglInitialize(eglDisplay, null, 0, null, 0)

        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], inputSurface, intArrayOf(EGL14.EGL_NONE), 0)

        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        drawer = GLDrawer2D()
    }

    private fun renderLoop() {
        Thread.sleep(200)

        val handlerThread = HandlerThread("GLThread").apply { start() }
        val handler = Handler(handlerThread.looper)

        surfaceTexture.setOnFrameAvailableListener({
            synchronized(frameSyncObject) {
                frameAvailable = true
                frameSyncObject.notifyAll()
            }
        }, handler)

        var isContextSet = false

        while (capturing) {
            synchronized(frameSyncObject) {
                if (!frameAvailable) frameSyncObject.wait(2500)
                frameAvailable = false
            }

            if (!isContextSet) {
                EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
                isContextSet = true
            }

            surfaceTexture.updateTexImage()
            GLES20.glViewport(0, 0, width, height)
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            drawer.draw(textureId)

            EGL14.eglSwapBuffers(eglDisplay, eglSurface)

            drainEncoder(false)

            onFrameCaptured?.let { callback ->
                val buffer = ByteBuffer.allocateDirect(width * height * 4)
                GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bmp.copyPixelsFromBuffer(buffer)
                callback(bmp)

                uploadFrameToServer(bmp)
            }
        }

        handlerThread.quitSafely()
    }

    fun stop() {
        if (!capturing) return
        capturing = false

        try {
            drainEncoder(true)
        } catch (e: Exception) {
            Log.e(TAG, "drainEncoder failed: ${e.message}")
        }

        try {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
        } catch (e: Exception) {
            Log.e(TAG, "EGL cleanup failed: ${e.message}")
        }

        try {
            mediaCodec.stop()
            mediaCodec.release()
        } catch (e: Exception) {
            Log.e(TAG, "mediaCodec release failed: ${e.message}")
        }

        if (muxerStarted) {
            try {
                mediaMuxer.stop()
                mediaMuxer.release()
            } catch (e: Exception) {
                Log.e(TAG, "mediaMuxer cleanup failed: ${e.message}")
            }
        }

        try {
            surface.release()
            inputSurface.release()
        } catch (e: Exception) {
            Log.e(TAG, "surface cleanup failed: ${e.message}")
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (outputFile.exists()) {
                saveToGallery(context, outputFile)
            } else {
                Log.w(TAG, "视频文件尚未生成，保存失败：${outputFile.absolutePath}")
            }
        }, 500)
    }

    private fun drainEncoder(endOfStream: Boolean) {
        if (endOfStream) {
            try {
                mediaCodec.signalEndOfInputStream()
            } catch (e: Exception) {
                Log.e(TAG, "signalEndOfInputStream failed: ${e.message}")
                return
            }
        }

        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            try {
                val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000)
                when {
                    outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!endOfStream) break
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxerStarted) throw RuntimeException("format changed twice")
                        videoTrackIndex = mediaMuxer.addTrack(mediaCodec.outputFormat)
                        mediaMuxer.start()
                        muxerStarted = true
                    }
                    outputBufferIndex >= 0 -> {
                        val encodedData = mediaCodec.getOutputBuffer(outputBufferIndex) ?: continue
                        if (bufferInfo.size > 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            mediaMuxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "drainEncoder error: ${e.message}")
                break
            }
        }
    }

    fun saveToGallery(context: Context, file: File): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ScreenRecorder")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null

        resolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    private fun uploadFrameToServer(bitmap: Bitmap) {
        try {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val imageBytes = baos.toByteArray()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "frame", "frame.jpg",
                    RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageBytes)
                )
                .build()

            val request = Request.Builder()
                .url("http://192.168.43.54:5000/analyze")
                .post(requestBody)
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("Upload", "❌ 上传失败: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.body?.string()
                        Log.d("Upload", "✅ 云端识别结果: $result")

                        val json = JSONObject(result ?: "")
                        val label = json.optString("label", "").lowercase()
                        val confidence = json.optDouble("confidence", 0.0)

                        // 🚫 黑名单标签 + 高置信度判断
                        val blacklistLabels = listOf("porn", "violence", "parent")
                        val threshold = 0.75

                        if (label in blacklistLabels && confidence >= threshold) {
                            blockUnsafeContent(label)
                        } else {
                            Log.d("Upload", "✅ 内容安全: label=$label, confidence=$confidence")
                        }

                    } catch (e: Exception) {
                        Log.e("Upload", "⚠️ 解析响应失败: ${e.message}")
                    }
                }


            })
        } catch (e: Exception) {
            Log.e("Upload", "🚨 上传异常: ${e.message}")
        }
    }
    private var lastBlockTime = 0L


    private var isBlocking = false

    private fun blockUnsafeContent(label: String) {
        if (isBlocking) return // 👉 避免重复触发

        isBlocking = true
        Log.w(TAG, "🚫 检测到不良内容：$label，触发黑屏")

        Handler(Looper.getMainLooper()).post {
            ScreenBlocker.show(context)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            ScreenBlocker.hide(context)
            isBlocking = false // 5秒后解除状态
        }, 5000)
    }

}

