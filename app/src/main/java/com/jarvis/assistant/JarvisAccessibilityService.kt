package com.jarvis.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import fi.iki.elonen.NanoHTTPD

class JarvisAccessibilityService : AccessibilityService() {

    private var server: JarvisHttpServer? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            server = JarvisHttpServer(8080, this)
            server?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        server?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
    }

    fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 80)
        return dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun performGlobal(action: Int): Boolean {
        return performGlobalAction(action)
    }
}

class JarvisHttpServer(port: Int, private val service: JarvisAccessibilityService) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        val params = session.parms
        val uri = session.uri

        return when (uri) {
            "/status" -> newFixedLengthResponse("JARVIS_SERVICE_ONLINE")
            "/tap" -> {
                val x = params["x"]?.toFloatOrNull() ?: 0f
                val y = params["y"]?.toFloatOrNull() ?: 0f
                service.tap(x, y)
                newFixedLengthResponse("OK: Tap ($x, $y)")
            }
            "/type" -> {
                val text = params["text"] ?: ""
                service.typeText(text)
                newFixedLengthResponse("OK: Type $text")
            }
            "/home" -> {
                service.performGlobal(AccessibilityService.GLOBAL_ACTION_HOME)
                newFixedLengthResponse("OK: HOME")
            }
            "/back" -> {
                service.performGlobal(AccessibilityService.GLOBAL_ACTION_BACK)
                newFixedLengthResponse("OK: BACK")
            }
            else -> newFixedLengthResponse("UNKNOWN_ENDPOINT")
        }
    }
}
