package com.example.tools

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.view.WindowManager

/**
 * Helper to wake the screen when Mamta needs user attention and trigger Android's
 * native keyguard dismiss / unlock prompt.
 *
 * NOTE: Mamta NEVER requests, reads, stores, or handles the user's PIN, pattern, or
 * password. It solely delegates authentication to Android's native KeyguardManager.
 */
object ScreenWakeHelper {

    private const val PREFS_NAME = "mamta_screen_lock_prefs"
    const val KEY_WAKE_SCREEN = "wake_screen_when_needed"

    fun isWakeScreenEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_WAKE_SCREEN, true) // ON by default
    }

    fun setWakeScreenEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_WAKE_SCREEN, enabled).apply()
    }

    /**
     * Wakes the screen and requests Android's native unlock prompt (PIN/pattern/biometric).
     */
    fun wakeScreenAndShowUnlockPrompt(activity: Activity) {
        if (!isWakeScreenEnabled(activity)) return

        // 1. Acquire momentary wake lock if screen is interactive/off
        wakeScreen(activity)

        // 2. Configure window to turn screen on and show above lock
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // 3. Request native Android unlock prompt if keyguard is locked
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager != null && keyguardManager.isKeyguardLocked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                keyguardManager.requestDismissKeyguard(
                    activity,
                    object : KeyguardManager.KeyguardDismissCallback() {
                        override fun onDismissError() {}
                        override fun onDismissSucceeded() {}
                        override fun onDismissCancelled() {}
                    }
                )
            }
        }
    }

    /**
     * Lightweight screen wake using PowerManager for background services
     */
    fun wakeScreen(context: Context) {
        if (!isWakeScreenEnabled(context)) return
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null && !powerManager.isInteractive) {
                @Suppress("DEPRECATION")
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "mamta:screen_wake"
                )
                wakeLock.acquire(4000) // 4 seconds
            }
        } catch (_: Exception) {}
    }
}
