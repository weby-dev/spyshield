package com.spyshield.app.monitor

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import com.spyshield.app.data.LiveState
import com.spyshield.app.data.LiveUse
import com.spyshield.app.data.OverlayPrefs

/**
 * iOS-style privacy indicator: a tiny pill in the top-right corner, drawn over every app,
 * with a green dot while the camera is on and an orange one for the mic (red if suspicious).
 * Plain Views only so it works from the monitor service. All window work runs on the main thread.
 */
object PrivacyDotOverlay {
    private const val GREEN = 0xFF34D399.toInt()
    private const val ORANGE = 0xFFFFB547.toInt()
    private const val RED = 0xFFFF4D6D.toInt()
    private const val FADE_MS = 220L

    private val main = Handler(Looper.getMainLooper())

    // Only touched on the main thread.
    private var pill: LinearLayout? = null
    private var camDot: View? = null
    private var micDot: View? = null
    private var hiding = false

    fun update(context: Context, live: LiveState) {
        val app = context.applicationContext
        main.post {
            val allowed = OverlayPrefs.isEnabled(app) && Settings.canDrawOverlays(app)
            if (!allowed || (live.camera == null && live.mic == null)) hideNow(app) else show(app, live)
        }
    }

    fun hide(context: Context) {
        val app = context.applicationContext
        main.post { hideNow(app) }
    }

    private fun show(app: Context, live: LiveState) {
        val view = pill ?: create(app) ?: return
        paint(camDot ?: return, live.camera, GREEN)
        paint(micDot ?: return, live.mic, ORANGE)
        if (hiding || view.alpha < 1f) {
            hiding = false
            view.animate().cancel()
            view.animate().alpha(1f).setDuration(FADE_MS).start()
        }
    }

    private fun paint(dot: View, use: LiveUse?, color: Int) {
        dot.visibility = if (use == null) View.GONE else View.VISIBLE
        (dot.background as? GradientDrawable)?.setColor(if (use?.suspicious == true) RED else color)
    }

    private fun create(app: Context): LinearLayout? {
        val wm = app.getSystemService(WindowManager::class.java) ?: return null
        val d = app.resources.displayMetrics.density
        fun px(v: Int) = (v * d).toInt()

        fun dot() = View(app).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL }
            layoutParams = LinearLayout.LayoutParams(px(8), px(8)).apply { marginStart = px(2); marginEnd = px(2) }
        }

        val cam = dot()
        val mic = dot()
        val view = LinearLayout(app).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(5), px(4), px(5), px(4))
            background = GradientDrawable().apply {
                cornerRadius = px(10).toFloat()
                setColor(Color.argb(170, 7, 11, 20))
            }
            alpha = 0f
            addView(cam)
            addView(mic)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = px(10)
            y = px(6)
            // Sit in the status bar row beside a notch rather than below it.
            if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        if (runCatching { wm.addView(view, params) }.isFailure) return null
        pill = view
        camDot = cam
        micDot = mic
        hiding = false
        return view
    }

    private fun hideNow(app: Context) {
        val view = pill ?: return
        if (hiding) return
        hiding = true
        view.animate().cancel()
        // withEndAction is skipped if a newer show() cancels the fade.
        view.animate().alpha(0f).setDuration(FADE_MS).withEndAction { remove(app, view) }.start()
    }

    private fun remove(app: Context, view: View) {
        runCatching { app.getSystemService(WindowManager::class.java)?.removeView(view) }
        if (pill === view) {
            pill = null
            camDot = null
            micDot = null
            hiding = false
        }
    }
}
