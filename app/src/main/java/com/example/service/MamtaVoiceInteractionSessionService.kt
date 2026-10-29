package com.example.service

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.example.MainActivity

/**
 * Session service for Mamta digital assistant.
 * Launches MainActivity directly with microphone listening active.
 */
class MamtaVoiceInteractionSessionService : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return MamtaVoiceInteractionSession(this)
    }

    private class MamtaVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

        override fun onShow(args: Bundle?, showFlags: Int) {
            super.onShow(args, showFlags)
            launchAssistantActivity()
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onHandleAssist(
            data: Bundle?,
            structure: AssistStructure?,
            content: AssistContent?
        ) {
            super.onHandleAssist(data, structure, content)
            launchAssistantActivity()
        }

        private fun launchAssistantActivity() {
            try {
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_ASSIST
                    putExtra("EXTRA_ASSIST_LAUNCH", true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(intent)
                hide()
            } catch (_: Exception) {}
        }
    }
}
