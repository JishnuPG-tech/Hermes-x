package com.example.hermes.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log
import com.example.hermes.MainActivity

/**
 * Session service for handling assistant invocation triggers (hardware gesture, long-press power, swipe).
 */
class HermesVoiceInteractionSessionService : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return HermesVoiceInteractionSession(this)
    }

    private class HermesVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
        companion object {
            private const val TAG = "HermesVoiceSession"
        }

        override fun onShow(args: Bundle?, showFlags: Int) {
            super.onShow(args, showFlags)
            Log.i(TAG, "Assistant invoked via system trigger (showFlags=$showFlags). Launching Hermes voice interface...")

            try {
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = "com.example.hermes.ACTION_VOICE_ASSIST"
                    putExtra("open_voice", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch Hermes voice interface from assistant trigger", e)
            } finally {
                hide()
            }
        }
    }
}
