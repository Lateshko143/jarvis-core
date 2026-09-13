package com.jarvis.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        var instance: JarvisAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: ""
        // Stealth Mode on Banking screens: Do not hijack inputs to prevent security errors
        if (pkg.contains("paisa") || pkg.contains("phonepe") || pkg.contains("paytm")) {
            return
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun handleDirectVoice(cmd: String) {
        val text = cmd.lowercase()
        when {
            text.contains("home") -> goHome()
            text.contains("back") -> performGlobalAction(GLOBAL_ACTION_BACK)
            text.contains("scan") || text.contains("pay") -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                try { startActivity(intent) } catch (e: Exception) {}
            }
            text.contains("open") || text.contains("click") -> {
                val target = text.replace("open", "").replace("click", "").trim()
                clickElementByText(target)
            }
        }
    }

    fun clickElementByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        if (!nodes.isNullOrEmpty()) {
            for (node in nodes) {
                if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    parent = parent.parent
                }
                val rect = Rect()
                node.getBoundsInScreen(rect)
                return tapCoordinates(rect.centerX().toFloat(), rect.centerY().toFloat())
            }
        }
        return false
    }

    fun tapCoordinates(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 70)
        return dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
