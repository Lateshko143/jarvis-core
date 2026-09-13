package com.jarvis.assistant

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 60)
            setBackgroundColor(Color.parseColor("#0F172A"))
        }

        val title = TextView(this).apply {
            text = "JARVIS AUTONOMOUS COPILOT"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 40)
        }
        layout.addView(title)

        val btnOverlay = Button(this).apply {
            text = "1. Enable Screen Overlay (Pill HUD)"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                } else {
                    startService(Intent(this@MainActivity, JarvisFloatingHUDService::class.java))
                }
            }
        }
        layout.addView(btnOverlay)

        val btnAccess = Button(this).apply {
            text = "2. Enable Accessibility Service"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        layout.addView(btnAccess)

        val btnStart = Button(this).apply {
            text = "3. Launch JARVIS Floating Assistant"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                startService(Intent(this@MainActivity, JarvisFloatingHUDService::class.java))
                finish()
            }
        }
        layout.addView(btnStart)

        setContentView(layout)
    }
}
