package com.farkhad.speechapp.ui

import com.farkhad.speechapp.data.AuthFailure
import com.farkhad.speechapp.data.AuthFailureReason
import com.farkhad.speechapp.data.AuthRepository
import com.farkhad.speechapp.data.AuthStateSubscription
import com.farkhad.speechapp.data.AuthenticatedUser
import com.farkhad.speechapp.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateStaysLoadingUntilFirebaseResponds() {
        val repository = FakeAuthRepository(emitInitialState = false)
        val viewModel = AuthViewModel(repository)

        assertEquals(AuthUiState.Loading, viewModel.authState)

        repository.emitAuthState(null)
        assertEquals(AuthUiState.Unauthenticated, viewModel.authState)
    }

    @Test
    fun verifiedExistingSessionIsRestoredAfterReopen() {
        val user = parent(verified = true)
        val viewModel = AuthViewModel(FakeAuthRepository(initialUser = user))

        assertEquals(AuthUiState.Authenticated(user), viewModel.authState)
        assertEquals(user.id, viewModel.currentUserUid)
    }

    @Test
    fun unverifiedExistingSessionOpensVerificationScreen() {
        val user = parent(verified = false)
        val viewModel = AuthViewModel(FakeAuthRepository(initialUser = user))

        assertEquals(AuthUiState.VerificationRequired(user), viewModel.authState)
    }

    @Test
    fun invalidInputDoesNotCallFirebase() {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.onEmailChanged("not-an-email")
        viewModel.onPasswordChanged("")
        viewModel.authenticate()

        assertTrue(viewModel.authState is AuthUiState.Error)
        assertEquals(0, repository.signInCalls)
        assertEquals(AuthMessages.EmailInvalid, viewModel.emailError)
        assertEquals(AuthMessages.PasswordRequired, viewModel.passwordError)
    }

    @Test
    fun successfulLoginUsesTrimmedEmailAndKeepsPasswordCharacters() = runTest {
        val repository = FakeAuthRepository()
        var submittedEmail = ""
        var submittedPassword = ""
        val user = parent(verified = true)
        repository.signInHandler = { email, password ->
            submittedEmail = email
            submittedPassword = password
            Result.success(user)
        }
        val viewModel = AuthViewModel(repository)

        viewModel.onEmailChanged("  parent@example.kz  ")
        viewModel.onPasswordChanged(" password with spaces ")
        viewModel.authenticate()
        advanceUntilIdle()

        assertEquals("parent@example.kz", submittedEmail)
        assertEquals(" password with spaces ", submittedPassword)
        assertEquals(AuthUiState.Authenticated(user), viewModel.authState)
        assertEquals("", viewModel.password)
    }

    @Test
    fun registrationSendsVerificationAndRequiresConfirmation() = runTest {
        val repository = FakeAuthRepository()
        val user = parent(id = "new-parent", verified = false)
        repository.createAccountHandler = { _, _ -> Result.success(user) }
        val viewModel = AuthViewModel(repository, verificationCooldownDurationSeconds = 1)

        viewModel.toggleAuthenticationMode()
        viewModel.onEmailChanged("new@example.kz")
        viewModel.onPasswordChanged("StrongPass1")
        viewModel.authenticate()
        advanceUntilIdle()

        assertEquals(1, repository.createAccountCalls)
        assertEquals(1, repository.sendVerificationCalls)
        assertEquals(AuthUiState.VerificationRequired(user), viewModel.authState)
        assertEquals(
            AccountActionUiState.Success(AuthMessages.VerificationSent),
            viewModel.verificationActionState,
        )
    }

    @Test
    fun resendVerificationHasCooldownAndRejectsRepeatedTap() = runTest {
        val repository = FakeAuthRepository(initialUser = parent(verified = false))
        val viewModel = AuthViewModel(repository, verificationCooldownDurationSeconds = 3)

        viewModel.resendVerificationEmail()
        runCurrent()
        viewModel.resendVerificationEmail()

        assertEquals(1, repository.sendVerificationCalls)
        assertEquals(3, viewModel.verificationCooldownSeconds)

        advanceTimeBy(3_000)
        runCurrent()
        viewModel.resendVerificationEmail()
        runCurrent()

        assertEquals(2, repository.sendVerificationCalls)
    }

    @Test
    fun refreshAfterEmailConfirmationUnlocksApplication() = runTest {
        val repository = FakeAuthRepository(initialUser = parent(verified = false))
        val verifiedUser = parent(verified = true)
        repository.refreshHandler = { Result.success(verifiedUser) }
        val viewModel = AuthViewModel(repository)

        viewModel.refreshEmailVerification()
        advanceUntilIdle()

        assertEquals(1, repository.refreshCalls)
        assertEquals(AuthUiState.Authenticated(verifiedUser), viewModel.authState)
    }

    @Test
    fun refreshWithoutConfirmationShowsRetryableMessage() = runTest {
        val unverified = parent(verified = false)
        val repository = FakeAuthRepository(initialUser = unverified)
        repository.refreshHandler = { Result.success(unverified) }
        val viewModel = AuthViewModel(repository)

        viewModel.refreshEmailVerification()
        advanceUntilIdle()

        assertEquals(
            AccountActionUiState.Error(AuthMessages.EmailStillUnverified),
            viewModel.verificationActionState,
        )
        assertEquals(AuthUiState.VerificationRequired(unverified), viewModel.authState)
    }

    @Test
    fun passwordResetReturnsSameSafeSuccessMessage() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)
        viewModel.openPasswordResetDialog()
        viewModel.onPasswordResetEmailChanged("unknown@example.kz")

        viewModel.sendPasswordResetEmail()
        advanceUntilIdle()

        assertEquals(1, repository.passwordResetCalls)
        assertEquals(
            AccountActionUiState.Success(AuthMessages.PasswordResetSent),
            viewModel.passwordResetState,
        )
        assertTrue(AuthMessages.PasswordResetSent.contains("Если аккаунт существует"))
    }

    @Test
    fun invalidCredentialsReturnSafeBilingualMessage() = runTest {
        val repository = FakeAuthRepository()
        repository.signInHandler = { _, _ ->
            Result.failure(AuthFailure(AuthFailureReason.InvalidCredentials))
        }
        val viewModel = AuthViewModel(repository)

        viewModel.onEmailChanged("parent@example.kz")
        viewModel.onPasswordChanged("wrong-password")
        viewModel.authenticate()
        advanceUntilIdle()

        assertEquals(AuthUiState.Error(AuthMessages.InvalidCredentials), viewModel.authState)
        assertTrue(viewModel.errorMessage!!.contains("/"))
    }

    @Test
    fun repeatedTapStartsOnlyOneAuthenticationRequest() = runTest {
        val repository = FakeAuthRepository()
        val deferredResult = CompletableDeferred<Result<AuthenticatedUser>>()
        repository.signInHandler = { _, _ -> deferredResult.await() }
        val viewModel = AuthViewModel(repository)
        viewModel.onEmailChanged("parent@example.kz")
        viewModel.onPasswordChanged("correct-password")

        viewModel.authenticate()
        viewModel.authenticate()
        runCurrent()

        assertEquals(1, repository.signInCalls)

        val user = parent(verified = true)
        deferredResult.complete(Result.success(user))
        advanceUntilIdle()
        assertEquals(AuthUiState.Authenticated(user), viewModel.authState)
    }

    @Test
    fun currentDeviceSignOutClearsLocalCredentials() {
        val repository = FakeAuthRepository(initialUser = parent(verified = true))
        val viewModel = AuthViewModel(repository)
        viewModel.onEmailChanged("parent@example.kz")
        viewModel.onPasswordChanged("password")

        viewModel.signOut()

        assertEquals(1, repository.signOutCalls)
        assertEquals("", viewModel.email)
        assertEquals("", viewModel.password)
        assertEquals(AuthUiState.Unauthenticated, viewModel.authState)
    }

    @Test
    fun signOutEverywhereReauthenticatesRevokesAndThenClearsLocalSession() = runTest {
        val repository = FakeAuthRepository(initialUser = parent(verified = true))
        val viewModel = AuthViewModel(repository)
        viewModel.openSignOutEverywhereDialog()
        viewModel.onReauthenticationPasswordChanged("StrongPass1")

        viewModel.signOutEverywhere()
        advanceUntilIdle()

        assertEquals(1, repository.reauthenticateCalls)
        assertEquals(1, repository.revokeCalls)
        assertEquals(1, repository.signOutCalls)
        assertEquals(AuthUiState.Unauthenticated, viewModel.authState)
        assertEquals(AuthMessages.SessionsRevoked, viewModel.authNoticeMessage)
    }

    @Test
    fun failedReauthenticationDoesNotRevokeOrSignOut() = runTest {
        val signedInUser = parent(verified = true)
        val repository = FakeAuthRepository(initialUser = signedInUser)
        repository.reauthenticateHandler = {
            Result.failure(AuthFailure(AuthFailureReason.InvalidCredentials))
        }
        val viewModel = AuthViewModel(repository)
        viewModel.openSignOutEverywhereDialog()
        viewModel.onReauthenticationPasswordChanged("wrong")

        viewModel.signOutEverywhere()
        advanceUntilIdle()

        assertEquals(0, repository.revokeCalls)
        assertEquals(0, repository.signOutCalls)
        assertEquals(AuthUiState.Authenticated(signedInUser), viewModel.authState)
        assertEquals(
            AccountActionUiState.Error(AuthMessages.InvalidCredentials),
            viewModel.signOutEverywhereState,
        )
    }

    @Test
    fun failedCloudFunctionKeepsLocalSessionForRetry() = runTest {
        val signedInUser = parent(verified = true)
        val repository = FakeAuthRepository(initialUser = signedInUser)
        repository.revokeHandler = {
            Result.failure(AuthFailure(AuthFailureReason.ServiceUnavailable))
        }
        val viewModel = AuthViewModel(repository)
        viewModel.openSignOutEverywhereDialog()
        viewModel.onReauthenticationPasswordChanged("StrongPass1")

        viewModel.signOutEverywhere()
        advanceUntilIdle()

        assertEquals(1, repository.revokeCalls)
        assertEquals(0, repository.signOutCalls)
        assertEquals(AuthUiState.Authenticated(signedInUser), viewModel.authState)
        assertEquals(
            AccountActionUiState.Error(AuthMessages.SessionsServiceUnavailable),
            viewModel.signOutEverywhereState,
        )
    }

    private fun parent(
        id: String = "parent-1",
        verified: Boolean,
    ) = AuthenticatedUser(
        id = id,
        email = "parent@example.kz",
        isEmailVerified = verified,
    )
}

private class FakeAuthRepository(
    initialUser: AuthenticatedUser? = null,
    private val emitInitialState: Boolean = true,
) : AuthRepository {
    private var listener: ((AuthenticatedUser?) -> Unit)? = null
    private var user: AuthenticatedUser? = initialUser

    var createAccountCalls = 0
        private set
    var signInCalls = 0
        private set
    var sendVerificationCalls = 0
        private set
    var refreshCalls = 0
        private set
    var passwordResetCalls = 0
        private set
    var reauthenticateCalls = 0
        private set
    var revokeCalls = 0
        private set
    var signOutCalls = 0
        private set

    var createAccountHandler: suspend (String, String) -> Result<AuthenticatedUser> = { _, _ ->
        Result.success(defaultUser("created-parent", verified = false))
    }
    var signInHandler: suspend (String, String) -> Result<AuthenticatedUser> = { _, _ ->
        Result.success(defaultUser("signed-in-parent", verified = true))
    }
    var sendVerificationHandler: suspend () -> Result<Unit> = { Result.success(Unit) }
    var refreshHandler: suspend () -> Result<AuthenticatedUser> = {
        user?.let { Result.success(it) }
            ?: Result.failure(AuthFailure(AuthFailureReason.NotAuthenticated))
    }
    var passwordResetHandler: suspend (String) -> Result<Unit> = { Result.success(Unit) }
    var reauthenticateHandler: suspend (String) -> Result<Unit> = { Result.success(Unit) }
    var revokeHandler: suspend () -> Result<Unit> = { Result.success(Unit) }

    override val currentUser: AuthenticatedUser?
        get() = user

    override fun observeAuthState(
        listener: (AuthenticatedUser?) -> Unit,
    ): AuthStateSubscription {
        this.listener = listener
        if (emitInitialState) listener(user)
        return AuthStateSubscription {
            if (this.listener === listener) this.listener = null
        }
    }

    override suspend fun createAccount(
        email: String,
        password: String,
    ): Result<AuthenticatedUser> {
        createAccountCalls += 1
        return createAccountHandler(email, password).also { result ->
            result.onSuccess { user = it }
        }
    }

    override suspend fun signIn(email: String, password: String): Result<AuthenticatedUser> {
        signInCalls += 1
        return signInHandler(email, password).also { result ->
            result.onSuccess { user = it }
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> {
        sendVerificationCalls += 1
        return sendVerificationHandler()
    }

    override suspend fun refreshCurrentUser(): Result<AuthenticatedUser> {
        refreshCalls += 1
        return refreshHandler().also { result -> result.onSuccess { user = it } }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        passwordResetCalls += 1
        return passwordResetHandler(email)
    }

    override suspend fun reauthenticate(password: String): Result<Unit> {
        reauthenticateCalls += 1
        return reauthenticateHandler(password)
    }

    override suspend fun revokeAllSessions(): Result<Unit> {
        revokeCalls += 1
        return revokeHandler()
    }

    override fun signOut() {
        signOutCalls += 1
        user = null
        listener?.invoke(null)
    }

    fun emitAuthState(user: AuthenticatedUser?) {
        this.user = user
        listener?.invoke(user)
    }

    private companion object {
        fun defaultUser(id: String, verified: Boolean) = AuthenticatedUser(
            id = id,
            email = "parent@example.kz",
            isEmailVerified = verified,
        )
    }
}
