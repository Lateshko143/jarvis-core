package com.jarvis.assistant

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.util.Locale

class JarvisFloatingHUDService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var hudView: TextView
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val bgShape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 50f
            setColor(Color.parseColor("#E11D48")) // Jarvis Arc Red/Pink Glowing Pill
            setStroke(3, Color.WHITE)
        }

        hudView = TextView(this).apply {
            text = " JARVIS "
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(35, 20, 35, 20)
            background = bgShape
            elevation = 20f
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        // Draggable floating pill behavior
        hudView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isClick = false
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(hudView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            listenVoiceCommand()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(hudView, params)
    }

    private fun listenVoiceCommand() {
        hudView.text = " Listening... "
        hudView.setBackgroundColor(Color.parseColor("#2563EB"))

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val query = matches?.firstOrNull() ?: ""
                executeAutonomousIntent(query)
            }
            override fun onError(error: Int) {
                resetHUD(" JARVIS ")
            }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun executeAutonomousIntent(command: String) {
        val cmd = command.lowercase(Locale.ROOT)
        hudView.text = " Running... "
        val service = JarvisAccessibilityService.instance

        if (service != null) {
            when {
                cmd.contains("home") -> service.goHome()
                cmd.contains("click") || cmd.contains("open") || cmd.contains("tap") -> {
                    val target = cmd.replace("click", "").replace("open", "").replace("tap", "").trim()
                    service.clickElementByText(target)
                }
                cmd.contains("type") || cmd.contains("write") -> {
                    val textToType = cmd.replace("type", "").replace("write", "").trim()
                    service.typeIntoFocusedInput(textToType)
                }
                else -> {
                    service.clickElementByText(cmd)
                }
            }
        }
        resetHUD(" JARVIS ")
    }

    private fun resetHUD(status: String) {
        hudView.postDelayed({
            hudView.text = status
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 50f
                setColor(Color.parseColor("#E11D48"))
                setStroke(3, Color.WHITE)
            }
            hudView.background = bg
        }, 1500)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        if (::hudView.isInitialized) windowManager.removeView(hudView)
    }
}
