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
import android.widget.FrameLayout
import android.widget.TextView
import java.util.Locale

class JarvisFloatingHUDService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var edgeHandle: FrameLayout
    private lateinit var handleBar: View
    private lateinit var statusText: TextView
    private var speechRecognizer: SpeechRecognizer? = null
    private var isExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Root container for edge handle
        edgeHandle = FrameLayout(this)

        // Native phone-like slim rounded edge bar (Grey translucent)
        handleBar = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f
                setColor(Color.parseColor("#994B5563")) // Sleek semi-transparent grey
            }
        }

        statusText = TextView(this).apply {
            text = "AI"
            setTextColor(Color.WHITE)
            textSize = 10f
            gravity = Gravity.CENTER
            visibility = View.GONE
        }

        val barParams = FrameLayout.LayoutParams(16, 140).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        }
        edgeHandle.addView(handleBar, barParams)
        edgeHandle.addView(statusText, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        })

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
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            x = 0
            y = 0
        }

        edgeHandle.setOnTouchListener(object : View.OnTouchListener {
            private var initialY = 0
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialY = params.y
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dy) > 15) isClick = false
                        params.y = initialY + dy
                        windowManager.updateViewLayout(edgeHandle, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            toggleVoiceAssistant()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(edgeHandle, params)
    }

    private fun toggleVoiceAssistant() {
        if (!isExpanded) {
            isExpanded = true
            // Expand to sleek active bar
            handleBar.layoutParams.width = 160
            handleBar.requestLayout()
            (handleBar.background as GradientDrawable).setColor(Color.parseColor("#EE2563EB")) // Active Blue
            statusText.text = "Listening..."
            statusText.visibility = View.VISIBLE
            listenVoiceCommand()
        } else {
            resetToEdge()
        }
    }

    private fun listenVoiceCommand() {
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
                statusText.text = "Running..."
                executeAutonomousIntent(query)
                resetToEdge()
            }
            override fun onError(error: Int) {
                resetToEdge()
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
        val service = JarvisAccessibilityService.instance ?: return

        when {
            cmd.contains("pay") || cmd.contains("scan") || cmd.contains("qr") -> {
                // Autonomous Payment Trigger: Temporarily hide overlay so Bank security doesn't block!
                edgeHandle.visibility = View.GONE
                service.launchAppOrQR("com.google.android.apps.nbu.paisa.user") // GPay or Scanner
                edgeHandle.postDelayed({ edgeHandle.visibility = View.VISIBLE }, 8000)
            }
            cmd.contains("home") -> service.goHome()
            cmd.contains("click") || cmd.contains("open") || cmd.contains("tap") -> {
                val target = cmd.replace("click", "").replace("open", "").replace("tap", "").trim()
                service.clickElementByText(target)
            }
            cmd.contains("type") || cmd.contains("write") -> {
                val textToType = cmd.replace("type", "").replace("write", "").trim()
                service.typeIntoFocusedInput(textToType)
            }
            else -> service.clickElementByText(cmd)
        }
    }

    private fun resetToEdge() {
        isExpanded = false
        handleBar.layoutParams.width = 16
        handleBar.requestLayout()
        (handleBar.background as GradientDrawable).setColor(Color.parseColor("#994B5563"))
        statusText.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        if (::edgeHandle.isInitialized) windowManager.removeView(edgeHandle)
    }
}
