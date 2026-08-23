package com.farkhad.speechapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farkhad.speechapp.ui.AuthScreen
import com.farkhad.speechapp.ui.AuthViewModel
import com.farkhad.speechapp.ui.SpeechApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val authViewModel: AuthViewModel = viewModel()

            if (!authViewModel.isAuthenticated) {
                AuthScreen(
                    viewModel = authViewModel,
                    onAuthSuccess = { authViewModel.isAuthenticated = true }
                )
            } else {
                SpeechApp(
                    userId = authViewModel.currentUserUid ?: "anonymous",
                    onSignOut = {
                        authViewModel.signOut()
                    }
                )
            }
        }
    }
}