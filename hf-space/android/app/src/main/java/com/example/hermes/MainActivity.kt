package com.example.hermes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.hermes.theme.HermesTheme

import android.content.Intent
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
  private val startVoice = mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    checkVoiceIntent(intent)
    enableEdgeToEdge()
    com.example.hermes.data.HermesDataRepository.initialize(applicationContext)
    setContent {
      HermesTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation(startVoice = startVoice.value) } }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    checkVoiceIntent(intent)
  }

  private fun checkVoiceIntent(intent: Intent?) {
    if (intent == null) return
    val shouldOpenVoice = intent.getBooleanExtra("open_voice", false) ||
            intent.action == Intent.ACTION_ASSIST ||
            intent.action == "com.example.hermes.ACTION_VOICE_ASSIST"
    if (shouldOpenVoice) {
      startVoice.value = true
    }
  }

  override fun onResume() {
    super.onResume()
    if (!com.example.hermes.data.HermesDataRepository.instance.isStreaming.value) {
      com.example.hermes.data.HermesDataRepository.syncActiveSession()
    }
  }
}
