package com.jarvis.assistant

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
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
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class JarvisFloatingHUDService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var edgeHandle: FrameLayout
    private lateinit var handleBar: View
    private lateinit var statusText: TextView
    private lateinit var audioManager: AudioManager
    private var speechRecognizer: SpeechRecognizer? = null
    private var isExpanded = false

    private val scoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
            if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                // Bluetooth mic locked and active
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Register Hardware Bluetooth SCO Audio Bridge
        registerReceiver(scoReceiver, IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED))
        startBluetoothAudio()

        setupEdgeUI()
    }

    private fun startBluetoothAudio() {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
        } catch (e: Exception) {}
    }

    private fun setupEdgeUI() {
        edgeHandle = FrameLayout(this)

        handleBar = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(Color.parseColor("#99374151")) // Sleek edge translucent
            }
        }

        statusText = TextView(this).apply {
            text = "JARVIS"
            setTextColor(Color.WHITE)
            textSize = 10f
            gravity = Gravity.CENTER
            visibility = View.GONE
        }

        edgeHandle.addView(handleBar, FrameLayout.LayoutParams(14, 150).apply {
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
        })
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
                        if (isClick) toggleListening()
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(edgeHandle, params)
    }

    private fun toggleListening() {
        if (!isExpanded) {
            isExpanded = true
            handleBar.layoutParams.width = 160
            handleBar.requestLayout()
            (handleBar.background as GradientDrawable).setColor(Color.parseColor("#EE2563EB"))
            statusText.text = "Listening..."
            statusText.visibility = View.VISIBLE
            listenSpeech()
        } else {
            resetEdge()
        }
    }

    private fun listenSpeech() {
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
                val voiceQuery = matches?.firstOrNull() ?: ""
                statusText.text = "Executing..."
                sendToTermuxBrain(voiceQuery)
                resetEdge()
            }
            override fun onError(error: Int) { resetEdge() }
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

    private fun sendToTermuxBrain(query: String) {
        if (query.isEmpty()) return
        thread {
            try {
                val url = URL("http://127.0.0.1:8765/command")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 3000
                OutputStreamWriter(conn.outputStream).use { it.write("query=" + query) }
                conn.responseCode
            } catch (e: Exception) {
                // Fallback direct execution via accessibility
                JarvisAccessibilityService.instance?.handleDirectVoice(query)
            }
        }
    }

    private fun resetEdge() {
        isExpanded = false
        handleBar.layoutParams.width = 14
        handleBar.requestLayout()
        (handleBar.background as GradientDrawable).setColor(Color.parseColor("#99374151"))
        statusText.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            audioManager.stopBluetoothSco()
            audioManager.isBluetoothScoOn = false
            unregisterReceiver(scoReceiver)
        } catch (e: Exception) {}
        speechRecognizer?.destroy()
        if (::edgeHandle.isInitialized) windowManager.removeView(edgeHandle)
    }
}
