package com.example.screenrecorder

import android.graphics.PixelFormat
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.*
import android.widget.FrameLayout

object ContentBlocker {

    private var blockerView: View? = null

    fun show(context: Context) {
        if (blockerView != null) return // 已存在

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            format = PixelFormat.TRANSLUCENT
            flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            gravity = Gravity.TOP or Gravity.START
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
        }

        val overlay = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 1.0f
        }

        wm.addView(overlay, params)
        blockerView = overlay
    }

    fun hide(context: Context) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        blockerView?.let {
            wm.removeView(it)
            blockerView = null
        }
    }
}
