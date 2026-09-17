package app.twitai.companion

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object TwitOverlay {
    private var root: LinearLayout? = null
    private var windowManager: WindowManager? = null

    fun show(service: TwitAccessibilityService, tweet: String) {
        remove()

        val wm = service.getSystemService(WindowManager::class.java)
        windowManager = wm

        val card = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            background = GradientDrawable().apply {
                setColor(Color.rgb(17, 24, 39))
                cornerRadius = 28f
            }
        }

        val title = TextView(service).apply {
            text = "Twit AI"
            setTextColor(Color.WHITE)
            textSize = 22f
        }

        val selected = TextView(service).apply {
            text = tweet
            setTextColor(Color.LTGRAY)
            textSize = 14f
            maxLines = 5
            setPadding(0, 12, 0, 12)
        }

        val count = EditText(service).apply {
            hint = "Replies, default 5"
            setText("5")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val minWords = EditText(service).apply {
            hint = "Minimum words"
            setText("10")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val maxWords = EditText(service).apply {
            hint = "Maximum words"
            setText("18")
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val tag = EditText(service).apply {
            hint = "Optional @tag"
        }

        val generate = Button(service).apply {
            text = "Generate replies"
            setOnClickListener {
                isEnabled = false
                text = "Generating..."
                val request = GenerateRequest(
                    tweet = tweet,
                    replyCount = count.text.toString().toIntOrNull()?.coerceIn(1, 10) ?: 5,
                    minWords = minWords.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 10,
                    maxWords = maxWords.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 18,
                    tone = "balanced",
                    tag = tag.text.toString(),
                    language = "auto"
                )

                CoroutineScope(Dispatchers.IO).launch {
                    val result = ApiClient().generate(request)
                    withContext(Dispatchers.Main) {
                        if (result.isSuccess) {
                            renderReplies(service, result.getOrThrow())
                        } else {
                            Toast.makeText(
                                service,
                                result.exceptionOrNull()?.message ?: "Generation failed",
                                Toast.LENGTH_LONG
                            ).show()
                            isEnabled = true
                            text = "Generate replies"
                        }
                    }
                }
            }
        }

        val close = Button(service).apply {
            text = "Close"
            setOnClickListener { remove() }
        }

        card.addView(title)
        card.addView(selected)
        card.addView(count)
        card.addView(minWords)
        card.addView(maxWords)
        card.addView(tag)
        card.addView(generate)
        card.addView(close)

        val params = WindowManager.LayoutParams(
            (service.resources.displayMetrics.widthPixels * 0.92).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

        root = card
        wm.addView(card, params)
    }

    private fun renderReplies(service: TwitAccessibilityService, replies: List<String>) {
        val card = root ?: return
        card.removeAllViews()

        val title = TextView(service).apply {
            text = "Generated replies"
            setTextColor(Color.WHITE)
            textSize = 22f
        }
        card.addView(title)

        val scroll = ScrollView(service)
        val list = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
        }

        replies.forEachIndexed { index, reply ->
            val box = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 18, 0, 18)
            }

            val text = TextView(service).apply {
                text = reply
                setTextColor(Color.WHITE)
                textSize = 15f
            }

            val copy = Button(service).apply {
                this.text = "Copy"
                setOnClickListener {
                    val clipboard = service.getSystemService(android.content.ClipboardManager::class.java)
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("Twit AI reply", reply)
                    )
                    Toast.makeText(service, "Copied", Toast.LENGTH_SHORT).show()
                }
            }

            box.addView(text)
            box.addView(copy)
            list.addView(box)
        }

        scroll.addView(list)
        card.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val copyAll = Button(service).apply {
            text = "Copy all"
            setOnClickListener {
                val clipboard = service.getSystemService(android.content.ClipboardManager::class.java)
                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText(
                        "Twit AI replies",
                        replies.joinToString("\n\n")
                    )
                )
                Toast.makeText(service, "All replies copied", Toast.LENGTH_SHORT).show()
            }
        }

        val close = Button(service).apply {
            text = "Close"
            setOnClickListener { remove() }
        }

        card.addView(copyAll)
        card.addView(close)
    }

    private fun remove() {
        root?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        root = null
        windowManager = null
    }
}
