package com.farkhad.speechapp.data

enum class AuthFailureReason {
    EmailAlreadyInUse,
    InvalidCredentials,
    InvalidEmail,
    WeakPassword,
    NetworkUnavailable,
    TooManyRequests,
    NotAuthenticated,
    ReauthenticationRequired,
    ServiceUnavailable,
    Unknown,
}

data class AuthenticatedUser(
    val id: String,
    val email: String?,
    val isEmailVerified: Boolean,
)

class AuthFailure(
    val reason: AuthFailureReason,
    cause: Throwable? = null,
) : Exception(cause)

fun interface AuthStateSubscription {
    fun remove()
}

interface AuthRepository {
    val currentUser: AuthenticatedUser?

    val currentUserUid: String?
        get() = currentUser?.id

    fun observeAuthState(listener: (AuthenticatedUser?) -> Unit): AuthStateSubscription

    suspend fun createAccount(email: String, password: String): Result<AuthenticatedUser>

    suspend fun signIn(email: String, password: String): Result<AuthenticatedUser>

    suspend fun sendEmailVerification(): Result<Unit>

    suspend fun refreshCurrentUser(): Result<AuthenticatedUser>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun reauthenticate(password: String): Result<Unit>

    suspend fun revokeAllSessions(): Result<Unit>

    fun signOut()
}
