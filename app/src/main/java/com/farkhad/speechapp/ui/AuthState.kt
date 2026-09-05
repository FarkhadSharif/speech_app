package com.farkhad.speechapp.ui

import com.farkhad.speechapp.data.AuthenticatedUser

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object Unauthenticated : AuthUiState
    data class VerificationRequired(val user: AuthenticatedUser) : AuthUiState
    data class Authenticated(val user: AuthenticatedUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

sealed interface AccountActionUiState {
    data object Idle : AccountActionUiState
    data object Loading : AccountActionUiState
    data class Success(val message: String) : AccountActionUiState
    data class Error(val message: String) : AccountActionUiState
}
