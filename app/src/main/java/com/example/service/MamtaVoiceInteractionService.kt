package com.example.service

import android.service.voice.VoiceInteractionService

/**
 * VoiceInteractionService implementation allowing Mamta to be configured
 * as the device's default digital assistant.
 */
class MamtaVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
    }

    override fun onShutdown() {
        super.onShutdown()
    }
}
