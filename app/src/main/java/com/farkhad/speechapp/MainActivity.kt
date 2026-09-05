package com.farkhad.speechapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farkhad.speechapp.ui.AuthLoadingScreen
import com.farkhad.speechapp.ui.AuthScreen
import com.farkhad.speechapp.ui.AuthUiState
import com.farkhad.speechapp.ui.AuthViewModel
import com.farkhad.speechapp.ui.EmailVerificationScreen
import com.farkhad.speechapp.ui.SignOutEverywhereDialog
import com.farkhad.speechapp.ui.SpeechApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val authViewModel: AuthViewModel = viewModel()

            when (val authState = authViewModel.authState) {
                AuthUiState.Loading -> AuthLoadingScreen()
                AuthUiState.Unauthenticated,
                is AuthUiState.Error -> AuthScreen(authViewModel)
                is AuthUiState.VerificationRequired -> EmailVerificationScreen(
                    viewModel = authViewModel,
                    user = authState.user,
                )
                is AuthUiState.Authenticated -> SpeechApp(
                    userId = authState.user.id,
                    onSignOut = authViewModel::signOut,
                    onSignOutEverywhere = authViewModel::openSignOutEverywhereDialog,
                )
            }

            if (authViewModel.isSignOutEverywhereDialogVisible) {
                SignOutEverywhereDialog(authViewModel)
            }
        }
    }
}
