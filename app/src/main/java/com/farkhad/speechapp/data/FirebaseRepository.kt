package com.farkhad.speechapp.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

data class UserProgress(
    val levelId: String = "",
    val levelNumber: Int = 1,
    val isCompleted: Boolean = false,
    val stars: Int = 0,
)

data class ChildProfile(
    val name: String = "",
    val age: String = "",
    // Read-only legacy value. New PIN values are not written to Firestore.
    val parentPin: String = "1234",
)

class FirebaseRepository : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()

    override val currentUser: AuthenticatedUser?
        get() = auth.currentUser?.toAuthenticatedUser()

    override fun observeAuthState(listener: (AuthenticatedUser?) -> Unit): AuthStateSubscription {
        val firebaseListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            listener(firebaseAuth.currentUser?.toAuthenticatedUser())
        }
        auth.addAuthStateListener(firebaseListener)
        return AuthStateSubscription { auth.removeAuthStateListener(firebaseListener) }
    }

    override suspend fun createAccount(
        email: String,
        password: String,
    ): Result<AuthenticatedUser> = authResult(
        defaultReason = AuthFailureReason.Unknown,
    ) {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: throw AuthFailure(AuthFailureReason.Unknown)
        user.toAuthenticatedUser()
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<AuthenticatedUser> = authResult(
        defaultReason = AuthFailureReason.InvalidCredentials,
    ) {
        val user = auth.signInWithEmailAndPassword(email, password).await().user
            ?: throw AuthFailure(AuthFailureReason.InvalidCredentials)
        user.toAuthenticatedUser()
    }

    override suspend fun sendEmailVerification(): Result<Unit> = authResult {
        val user = auth.currentUser ?: throw AuthFailure(AuthFailureReason.NotAuthenticated)
        auth.useAppLanguage()
        user.sendEmailVerification().await()
    }

    override suspend fun refreshCurrentUser(): Result<AuthenticatedUser> = authResult {
        val user = auth.currentUser ?: throw AuthFailure(AuthFailureReason.NotAuthenticated)
        user.reload().await()
        auth.currentUser?.toAuthenticatedUser()
            ?: throw AuthFailure(AuthFailureReason.NotAuthenticated)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.useAppLanguage()
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (error: FirebaseAuthInvalidUserException) {
            // The UI intentionally returns the same result whether an account exists or not.
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error.toAuthFailure(AuthFailureReason.Unknown))
        }
    }

    override suspend fun reauthenticate(password: String): Result<Unit> = authResult(
        defaultReason = AuthFailureReason.InvalidCredentials,
    ) {
        val user = auth.currentUser ?: throw AuthFailure(AuthFailureReason.NotAuthenticated)
        val email = user.email ?: throw AuthFailure(AuthFailureReason.ReauthenticationRequired)
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
    }

    override suspend fun revokeAllSessions(): Result<Unit> = authResult(
        defaultReason = AuthFailureReason.ServiceUnavailable,
    ) {
        functions
            .getHttpsCallable(REVOKE_SESSIONS_FUNCTION)
            .call(emptyMap<String, Any>())
            .await()
    }

    override fun signOut() {
        auth.signOut()
    }

    suspend fun updateChildInfo(childName: String, childAge: String): Result<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Пайдаланушы табылмады")
            val updates = mapOf(
                "childName" to childName,
                "childAge" to childAge,
            )
            db.collection("users").document(uid).set(updates, SetOptions.merge()).await()
            Result.success(true)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getChildInfo(): Result<ChildProfile?> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Пайдаланушы табылмады")
            val doc = db.collection("users").document(uid).get().await()
            val profile = ChildProfile(
                name = doc.getString("childName") ?: "",
                age = doc.getString("childAge") ?: "",
                parentPin = doc.getString("parentPin") ?: "1234",
            )
            Result.success(profile)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun saveLevelProgress(levelNumber: Int, stars: Int): Result<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val data = mapOf(
                "levelNumber" to levelNumber,
                "isCompleted" to true,
                "stars" to stars,
            )
            db.collection("users").document(uid)
                .collection("levels").document("level_$levelNumber")
                .set(data, SetOptions.merge()).await()
            Result.success(true)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun saveLearnedWord(wordId: String): Result<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val docRef = db.collection("users").document(uid)
                .collection("metadata").document("learned_words")
            val doc = docRef.get().await()
            val currentIds = (doc.get("ids") as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty()
            if (wordId !in currentIds) {
                docRef.set(mapOf("ids" to currentIds + wordId), SetOptions.merge()).await()
            }
            Result.success(true)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getLearnedWords(): Result<Set<String>> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val doc = db.collection("users").document(uid)
                .collection("metadata").document("learned_words")
                .get().await()
            val ids = (doc.get("ids") as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty()
            Result.success(ids.toSet())
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private suspend fun <T> authResult(
        defaultReason: AuthFailureReason = AuthFailureReason.Unknown,
        action: suspend () -> T,
    ): Result<T> {
        return try {
            Result.success(action())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error.toAuthFailure(defaultReason))
        }
    }

    private fun Exception.toAuthFailure(defaultReason: AuthFailureReason): AuthFailure {
        if (this is AuthFailure) return this
        val reason = when (this) {
            is FirebaseAuthUserCollisionException -> AuthFailureReason.EmailAlreadyInUse
            is FirebaseAuthWeakPasswordException -> AuthFailureReason.WeakPassword
            is FirebaseAuthRecentLoginRequiredException -> AuthFailureReason.ReauthenticationRequired
            is FirebaseAuthInvalidUserException -> AuthFailureReason.InvalidCredentials
            is FirebaseAuthInvalidCredentialsException -> defaultReason
            is FirebaseNetworkException -> AuthFailureReason.NetworkUnavailable
            is FirebaseTooManyRequestsException -> AuthFailureReason.TooManyRequests
            is FirebaseFunctionsException -> when (code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED -> AuthFailureReason.NotAuthenticated
                FirebaseFunctionsException.Code.UNAVAILABLE,
                FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> AuthFailureReason.NetworkUnavailable
                else -> AuthFailureReason.ServiceUnavailable
            }
            else -> defaultReason
        }
        return AuthFailure(reason, this)
    }

    private fun FirebaseUser.toAuthenticatedUser() = AuthenticatedUser(
        id = uid,
        email = email,
        isEmailVerified = isEmailVerified,
    )

    private companion object {
        const val REVOKE_SESSIONS_FUNCTION = "revokeOwnSessions"
    }
}
