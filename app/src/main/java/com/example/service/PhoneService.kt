package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PhoneService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can be used to monitor active changes if needed
    }

    override fun onInterrupt() {
        // Accessibility interrupted
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: PhoneService? = null

        fun isActive(): Boolean = instance != null

        private fun offMessage(): String =
            "Accessibility service is inactive. Please enable 'AI Assistant' in Android Accessibility Settings."

        fun readScreen(): String {
            val service = instance ?: return offMessage()
            val root = service.rootInActiveWindow ?: return "Could not access screen content."
            val sb = StringBuilder()
            walkNode(root, sb)
            return if (sb.isEmpty()) "No text found on active screen." else sb.toString().trim()
        }

        private fun walkNode(node: AccessibilityNodeInfo?, sb: StringBuilder) {
            if (node == null || sb.length > 3500) return
            var text = node.text
            if (text.isNullOrBlank()) {
                text = node.contentDescription
            }
            if (!text.isNullOrBlank()) {
                val prefix = if (node.isClickable) "[Button] " else ""
                sb.append(prefix).append(text.toString().trim()).append("\n")
            }
            for (i in 0 until node.childCount) {
                walkNode(node.getChild(i), sb)
            }
        }

        fun tapText(targetText: String): String {
            val service = instance ?: return offMessage()
            val root = service.rootInActiveWindow ?: return "Could not access screen."
            val matchedNodes = root.findAccessibilityNodeInfosByText(targetText)
            if (matchedNodes.isNullOrEmpty()) {
                return "Text '$targetText' not found on active screen."
            }

            for (node in matchedNodes) {
                var current: AccessibilityNodeInfo? = node
                while (current != null && !current.isClickable) {
                    current = current.parent
                }
                if (current != null && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return "Tapped: $targetText"
                }
            }
            return "Found '$targetText' but it could not be clicked."
        }

        fun typeText(inputText: String): String {
            val service = instance ?: return offMessage()
            val root = service.rootInActiveWindow ?: return "Could not access screen."
            val focusedNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: return "No active input/text box is currently focused."

            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, inputText)
            }
            return if (focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                "Typed: $inputText"
            } else {
                "Failed to type text into active field."
            }
        }

        fun pressButton(button: String): String {
            val service = instance ?: return offMessage()
            val action = when (button.lowercase()) {
                "home" -> GLOBAL_ACTION_HOME
                "back" -> GLOBAL_ACTION_BACK
                "recents" -> GLOBAL_ACTION_RECENTS
                "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
                else -> return "Invalid button '$button'. Valid options: home, back, recents, notifications."
            }
            return if (service.performGlobalAction(action)) {
                "Pressed: $button"
            } else {
                "Failed to trigger $button action."
            }
        }

        private fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) {
                val scrollable = findScrollable(node.getChild(i))
                if (scrollable != null) return scrollable
            }
            return null
        }

        fun scroll(direction: String): String {
            val service = instance ?: return offMessage()
            val root = service.rootInActiveWindow ?: return "Could not access screen."
            val scrollable = findScrollable(root) ?: return "No scrollable container found on screen."
            val action = if (direction.equals("up", ignoreCase = true)) {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }
            return if (scrollable.performAction(action)) {
                "Scrolled: $direction"
            } else {
                "Could not scroll: $direction"
            }
        }
    }
}
