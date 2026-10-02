package com.example.hermes.service

import android.content.ComponentName
import android.content.Context
import android.service.voice.VoiceInteractionService
import android.util.Log

/**
 * System-level Voice Interaction Service.
 *
 * Enables Hermes to be selected as the Android Default Digital Assistant
 * in System Settings > Apps > Default Apps > Digital Assistant App.
 */
class HermesVoiceInteractionService : VoiceInteractionService() {

    companion object {
        private const val TAG = "HermesVoiceService"

        fun isHermesSelectedAsAssistant(context: Context): Boolean {
            return isActiveService(
                context,
                ComponentName(context, HermesVoiceInteractionService::class.java)
            )
        }
    }

    override fun onReady() {
        super.onReady()
        Log.i(TAG, "HermesVoiceInteractionService is ready and active as system digital assistant")
    }

    override fun onShutdown() {
        Log.i(TAG, "HermesVoiceInteractionService is shutting down")
        super.onShutdown()
    }
}
