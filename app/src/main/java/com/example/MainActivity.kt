package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.tools.ScreenWakeHelper
import com.example.ui.AssistantScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AssistantViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleAssistIntent(intent)

        setContent {
            MyApplicationTheme {
                AssistantScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAssistIntent(intent)
    }

    private fun handleAssistIntent(intent: Intent?) {
        val isAssist = intent?.action == Intent.ACTION_ASSIST ||
                intent?.getBooleanExtra("EXTRA_ASSIST_LAUNCH", false) == true
        if (isAssist) {
            ScreenWakeHelper.wakeScreenAndShowUnlockPrompt(this)
            viewModel.triggerVoiceInput()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshServiceStatus()
    }
}
