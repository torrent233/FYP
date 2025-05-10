package com.example.screenrecorder.utils

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.*
import android.widget.FrameLayout
import android.graphics.PixelFormat
import android.view.*
import android.widget.TextView

object ScreenBlocker {
    private var overlayView: View? = null

    fun show(context: Context) {
        if (overlayView != null) return

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START

        val blockView = FrameLayout(context).apply {
            setBackgroundColor(Color.argb(240, 0, 0, 0)) // 黑色半透明
        }
        val textView = TextView(context).apply {
            text = "⚠️ 不良内容"
            setTextColor(Color.WHITE)
            textSize = 24f
            setPadding(50, 50, 50, 50)
            gravity = Gravity.CENTER
        }
        blockView.addView(
            textView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        try {
            windowManager.addView(blockView, layoutParams)
            overlayView = blockView
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hide(context: Context) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }
}
