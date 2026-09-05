package com.farkhad.speechapp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farkhad.speechapp.data.AuthFailure
import com.farkhad.speechapp.data.AuthFailureReason
import com.farkhad.speechapp.data.AuthRepository
import com.farkhad.speechapp.data.AuthStateSubscription
import com.farkhad.speechapp.data.AuthenticatedUser
import com.farkhad.speechapp.data.FirebaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository = FirebaseRepository(),
    private val verificationCooldownDurationSeconds: Int = DEFAULT_VERIFICATION_COOLDOWN_SECONDS,
) : ViewModel() {
    private var authSubscription: AuthStateSubscription? = null
    private var authenticationInProgress = false
    private var verificationCooldownJob: Job? = null

    var authState by mutableStateOf<AuthUiState>(AuthUiState.Loading)
        private set

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var isPasswordVisible by mutableStateOf(false)
        private set

    var isSignUpMode by mutableStateOf(false)
        private set

    var emailError by mutableStateOf<String?>(null)
        private set

    var passwordError by mutableStateOf<String?>(null)
        private set

    var verificationActionState by mutableStateOf<AccountActionUiState>(AccountActionUiState.Idle)
        private set

    var verificationCooldownSeconds by mutableStateOf(0)
        private set

    var isPasswordResetDialogVisible by mutableStateOf(false)
        private set

    var passwordResetEmail by mutableStateOf("")
        private set

    var passwordResetEmailError by mutableStateOf<String?>(null)
        private set

    var passwordResetState by mutableStateOf<AccountActionUiState>(AccountActionUiState.Idle)
        private set

    var isSignOutEverywhereDialogVisible by mutableStateOf(false)
        private set

    var reauthenticationPassword by mutableStateOf("")
        private set

    var isReauthenticationPasswordVisible by mutableStateOf(false)
        private set

    var signOutEverywhereState by mutableStateOf<AccountActionUiState>(AccountActionUiState.Idle)
        private set

    var authNoticeMessage by mutableStateOf<String?>(null)
        private set

    val currentUserUid: String?
        get() = when (val state = authState) {
            is AuthUiState.Authenticated -> state.user.id
            is AuthUiState.VerificationRequired -> state.user.id
            else -> repository.currentUserUid
        }

    val errorMessage: String?
        get() = (authState as? AuthUiState.Error)?.message

    init {
        authSubscription = repository.observeAuthState { user ->
            if (authenticationInProgress && user == null) return@observeAuthState
            routeUser(user)
        }
    }

    fun onEmailChanged(value: String) {
        email = value
        emailError = null
        authNoticeMessage = null
        clearAuthenticationError()
    }

    fun onPasswordChanged(value: String) {
        password = value
        passwordError = null
        authNoticeMessage = null
        clearAuthenticationError()
    }

    fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
    }

    fun toggleAuthenticationMode() {
        isSignUpMode = !isSignUpMode
        password = ""
        emailError = null
        passwordError = null
        authNoticeMessage = null
        clearAuthenticationError()
    }

    fun authenticate() {
        if (authenticationInProgress) return

        val validation = AuthValidator.validate(email, password, isSignUpMode)
        emailError = validation.emailError
        passwordError = validation.passwordError
        if (!validation.isValid) {
            authState = AuthUiState.Error(
                listOfNotNull(validation.emailError, validation.passwordError).joinToString("\n"),
            )
            return
        }

        val normalizedEmail = email.trim()
        val submittedPassword = password
        val createNewAccount = isSignUpMode
        authenticationInProgress = true
        authNoticeMessage = null
        authState = AuthUiState.Loading

        viewModelScope.launch {
            val result = if (createNewAccount) {
                repository.createAccount(normalizedEmail, submittedPassword)
            } else {
                repository.signIn(normalizedEmail, submittedPassword)
            }

            authenticationInProgress = false
            result.fold(
                onSuccess = { user ->
                    password = ""
                    routeUser(user)
                    if (createNewAccount && !user.isEmailVerified) {
                        sendVerificationEmailInternal()
                    }
                },
                onFailure = { error ->
                    authState = AuthUiState.Error(error.toUserMessage())
                },
            )
        }
    }

    fun resendVerificationEmail() {
        if (verificationCooldownSeconds > 0 ||
            verificationActionState is AccountActionUiState.Loading
        ) {
            return
        }
        sendVerificationEmailInternal()
    }

    fun refreshEmailVerification() {
        if (verificationActionState is AccountActionUiState.Loading) return
        verificationActionState = AccountActionUiState.Loading

        viewModelScope.launch {
            repository.refreshCurrentUser().fold(
                onSuccess = { user ->
                    if (user.isEmailVerified) {
                        verificationActionState = AccountActionUiState.Idle
                        verificationCooldownJob?.cancel()
                        verificationCooldownSeconds = 0
                        routeUser(user)
                    } else {
                        routeUser(user)
                        verificationActionState = AccountActionUiState.Error(
                            AuthMessages.EmailStillUnverified,
                        )
                    }
                },
                onFailure = { error ->
                    if (error.authFailureReason() == AuthFailureReason.NotAuthenticated) {
                        signOut()
                    } else {
                        verificationActionState = AccountActionUiState.Error(error.toUserMessage())
                    }
                },
            )
        }
    }

    fun openPasswordResetDialog() {
        passwordResetEmail = email.trim()
        passwordResetEmailError = null
        passwordResetState = AccountActionUiState.Idle
        isPasswordResetDialogVisible = true
    }

    fun closePasswordResetDialog() {
        if (passwordResetState is AccountActionUiState.Loading) return
        isPasswordResetDialogVisible = false
        passwordResetEmail = ""
        passwordResetEmailError = null
        passwordResetState = AccountActionUiState.Idle
    }

    fun onPasswordResetEmailChanged(value: String) {
        passwordResetEmail = value
        passwordResetEmailError = null
        if (passwordResetState is AccountActionUiState.Error) {
            passwordResetState = AccountActionUiState.Idle
        }
    }

    fun sendPasswordResetEmail() {
        if (passwordResetState is AccountActionUiState.Loading) return
        val validationError = AuthValidator.validateEmail(passwordResetEmail)
        passwordResetEmailError = validationError
        if (validationError != null) return

        passwordResetState = AccountActionUiState.Loading
        val normalizedEmail = passwordResetEmail.trim()
        viewModelScope.launch {
            repository.sendPasswordResetEmail(normalizedEmail).fold(
                onSuccess = {
                    passwordResetState = AccountActionUiState.Success(
                        AuthMessages.PasswordResetSent,
                    )
                },
                onFailure = { error ->
                    passwordResetState = AccountActionUiState.Error(error.toUserMessage())
                },
            )
        }
    }

    fun openSignOutEverywhereDialog() {
        reauthenticationPassword = ""
        signOutEverywhereState = AccountActionUiState.Idle
        isSignOutEverywhereDialogVisible = true
    }

    fun closeSignOutEverywhereDialog() {
        if (signOutEverywhereState is AccountActionUiState.Loading) return
        reauthenticationPassword = ""
        isReauthenticationPasswordVisible = false
        signOutEverywhereState = AccountActionUiState.Idle
        isSignOutEverywhereDialogVisible = false
    }

    fun onReauthenticationPasswordChanged(value: String) {
        reauthenticationPassword = value
        if (signOutEverywhereState is AccountActionUiState.Error) {
            signOutEverywhereState = AccountActionUiState.Idle
        }
    }

    fun toggleReauthenticationPasswordVisibility() {
        isReauthenticationPasswordVisible = !isReauthenticationPasswordVisible
    }

    fun signOutEverywhere() {
        if (signOutEverywhereState is AccountActionUiState.Loading) return
        if (reauthenticationPassword.isBlank()) {
            signOutEverywhereState = AccountActionUiState.Error(AuthMessages.PasswordRequired)
            return
        }

        val submittedPassword = reauthenticationPassword
        signOutEverywhereState = AccountActionUiState.Loading
        viewModelScope.launch {
            val reauthentication = repository.reauthenticate(submittedPassword)
            if (reauthentication.isFailure) {
                reauthenticationPassword = ""
                val reason = reauthentication.exceptionOrNull().authFailureReason()
                signOutEverywhereState = AccountActionUiState.Error(
                    if (reason == AuthFailureReason.InvalidCredentials) {
                        AuthMessages.InvalidCredentials
                    } else {
                        reauthentication.exceptionOrNull()?.toUserMessage()
                            ?: AuthMessages.UnexpectedError
                    },
                )
                return@launch
            }

            repository.revokeAllSessions().fold(
                onSuccess = {
                    signOutEverywhereState = AccountActionUiState.Success(
                        AuthMessages.SessionsRevoked,
                    )
                    isSignOutEverywhereDialogVisible = false
                    performLocalSignOut(AuthMessages.SessionsRevoked)
                },
                onFailure = { error ->
                    reauthenticationPassword = ""
                    signOutEverywhereState = AccountActionUiState.Error(error.toUserMessage())
                },
            )
        }
    }

    fun signOut() {
        performLocalSignOut(notice = null)
    }

    private fun sendVerificationEmailInternal() {
        verificationActionState = AccountActionUiState.Loading
        viewModelScope.launch {
            repository.sendEmailVerification().fold(
                onSuccess = {
                    verificationActionState = AccountActionUiState.Success(
                        AuthMessages.VerificationSent,
                    )
                    startVerificationCooldown()
                },
                onFailure = { error ->
                    if (error.authFailureReason() == AuthFailureReason.NotAuthenticated) {
                        signOut()
                    } else {
                        verificationActionState = AccountActionUiState.Error(error.toUserMessage())
                    }
                },
            )
        }
    }

    private fun startVerificationCooldown() {
        verificationCooldownJob?.cancel()
        verificationCooldownSeconds = verificationCooldownDurationSeconds
        verificationCooldownJob = viewModelScope.launch {
            while (verificationCooldownSeconds > 0) {
                delay(1_000)
                verificationCooldownSeconds -= 1
            }
        }
    }

    private fun performLocalSignOut(notice: String?) {
        authenticationInProgress = false
        verificationCooldownJob?.cancel()
        verificationCooldownSeconds = 0
        repository.signOut()
        email = ""
        password = ""
        emailError = null
        passwordError = null
        reauthenticationPassword = ""
        isReauthenticationPasswordVisible = false
        isSignOutEverywhereDialogVisible = false
        verificationActionState = AccountActionUiState.Idle
        passwordResetState = AccountActionUiState.Idle
        signOutEverywhereState = AccountActionUiState.Idle
        authNoticeMessage = notice
        authState = AuthUiState.Unauthenticated
    }

    private fun routeUser(user: AuthenticatedUser?) {
        authState = when {
            user == null -> AuthUiState.Unauthenticated
            user.isEmailVerified -> AuthUiState.Authenticated(user)
            else -> AuthUiState.VerificationRequired(user)
        }
    }

    private fun clearAuthenticationError() {
        if (authState is AuthUiState.Error) {
            authState = AuthUiState.Unauthenticated
        }
    }

    override fun onCleared() {
        verificationCooldownJob?.cancel()
        authSubscription?.remove()
        authSubscription = null
        super.onCleared()
    }

    private fun Throwable?.authFailureReason(): AuthFailureReason {
        return (this as? AuthFailure)?.reason ?: AuthFailureReason.Unknown
    }

    private fun Throwable.toUserMessage(): String {
        return when (authFailureReason()) {
            AuthFailureReason.EmailAlreadyInUse -> AuthMessages.EmailAlreadyInUse
            AuthFailureReason.InvalidCredentials -> AuthMessages.InvalidCredentials
            AuthFailureReason.InvalidEmail -> AuthMessages.EmailInvalid
            AuthFailureReason.WeakPassword -> AuthMessages.PasswordTooShort
            AuthFailureReason.NetworkUnavailable -> AuthMessages.NetworkUnavailable
            AuthFailureReason.TooManyRequests -> AuthMessages.TooManyRequests
            AuthFailureReason.NotAuthenticated -> AuthMessages.NotAuthenticated
            AuthFailureReason.ReauthenticationRequired -> AuthMessages.ReauthenticationRequired
            AuthFailureReason.ServiceUnavailable -> AuthMessages.SessionsServiceUnavailable
            AuthFailureReason.Unknown -> AuthMessages.UnexpectedError
        }
    }

    private companion object {
        const val DEFAULT_VERIFICATION_COOLDOWN_SECONDS = 60
    }
}
