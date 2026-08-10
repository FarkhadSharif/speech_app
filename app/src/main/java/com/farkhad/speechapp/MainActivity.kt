package com.farkhad.speechapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.farkhad.speechapp.ui.SpeechApp
import com.farkhad.speechapp.ui.theme.SpeechAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeechAppTheme {
                SpeechApp()
            }
        }
    }
}

