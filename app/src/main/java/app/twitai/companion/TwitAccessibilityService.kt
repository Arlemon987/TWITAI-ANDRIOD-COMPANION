package app.twitai.companion

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.Toast

class TwitAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var floatingView: View? = null
    private var lastText = ""

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // X Android package. This can change, so the helper also accepts Twitter.
        if (packageName != "com.twitter.android" && packageName != "com.twitter.android.lite") return

        val text = event.text?.joinToString("")?.trim().orEmpty()
        if (text.length < 2 || text == lastText) return

        lastText = text
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            showAction(text, event.source)
        }, 120)
    }

    private fun showAction(text: String, source: android.view.accessibility.AccessibilityNodeInfo?) {
        removeAction()

        val button = Button(this).apply {
            this.text = "Send to Twit AI"
            textSize = 12f
            setAllCaps(false)
            setOnClickListener {
                removeAction()
                TwitOverlay.show(this@TwitAccessibilityService, text)
            }
        }

        floatingView = button

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val location = Rect()
        source?.getBoundsInScreen(location)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = location.left.coerceAtLeast(8)
        params.y = (location.bottom + 8).coerceAtLeast(60)

        try {
            wm.addView(button, params)
            handler.postDelayed({ removeAction() }, 7000)
        } catch (_: Exception) {
            floatingView = null
        }
    }

    private fun removeAction() {
        floatingView?.let {
            try {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            } catch (_: Exception) {}
        }
        floatingView = null
    }

    override fun onInterrupt() {
        removeAction()
    }

    override fun onDestroy() {
        removeAction()
        super.onDestroy()
    }
}
