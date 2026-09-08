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

    suspend fun saveParentReflection(reflection: ParentReflection): Result<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val data = mapOf(
                "createdAt" to reflection.createdAt,
                "engagement" to reflection.engagement,
                "clarity" to reflection.clarity,
                "independence" to reflection.independence,
                "practiceMinutes" to reflection.practiceMinutes,
                "context" to reflection.context,
                "wins" to reflection.wins.toList(),
                "note" to reflection.note,
                "nextStep" to reflection.nextStep,
            )
            db.collection("users").document(uid)
                .collection("reflections").document(reflection.dateKey)
                .set(data, SetOptions.merge()).await()
            Result.success(true)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getParentReflections(): Result<List<ParentReflection>> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val snapshot = db.collection("users").document(uid)
                .collection("reflections")
                .get().await()
            val entries = snapshot.documents.mapNotNull { document ->
                val createdAt = document.getLong("createdAt") ?: return@mapNotNull null
                ParentReflection(
                    id = createdAt,
                    dateKey = document.id,
                    createdAt = createdAt,
                    engagement = (document.getLong("engagement") ?: 3L).toInt().coerceIn(1, 5),
                    clarity = (document.getLong("clarity") ?: 3L).toInt().coerceIn(1, 5),
                    independence = (document.getLong("independence") ?: 3L).toInt().coerceIn(1, 5),
                    practiceMinutes = (document.getLong("practiceMinutes") ?: 0L).toInt().coerceAtLeast(0),
                    context = document.getString("context").orEmpty(),
                    wins = (document.get("wins") as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty(),
                    note = document.getString("note").orEmpty(),
                    nextStep = document.getString("nextStep").orEmpty(),
                )
            }.sortedByDescending { it.createdAt }
            Result.success(entries)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun saveExerciseSessionReport(report: ExerciseSessionReport): Result<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val steps = report.steps.map { step ->
                mapOf(
                    "stepId" to step.stepId,
                    "title" to step.title,
                    "score" to step.score,
                    "attempts" to step.attempts,
                    "analysisType" to step.analysisType,
                    "targetText" to step.targetText,
                    "recognizedText" to step.recognizedText,
                    "feedback" to step.feedback,
                    "mouthOpeningPercent" to step.mouthOpeningPercent,
                    "lipRoundingPercent" to step.lipRoundingPercent,
                    "holdSeconds" to step.holdSeconds,
                )
            }
            val data = mapOf(
                "sessionId" to report.sessionId,
                "activityId" to report.activityId,
                "activityTitle" to report.activityTitle,
                "levelId" to report.levelId,
                "levelTitle" to report.levelTitle,
                "completedAt" to report.completedAt,
                "durationSeconds" to report.durationSeconds,
                "score" to report.score,
                "attemptNumber" to report.attemptNumber,
                "source" to report.source,
                "steps" to steps,
            )
            db.collection("users").document(uid)
                .collection("exerciseReports").document(report.sessionId)
                .set(data, SetOptions.merge()).await()
            Result.success(true)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getExerciseSessionReports(): Result<List<ExerciseSessionReport>> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val snapshot = db.collection("users").document(uid)
                .collection("exerciseReports")
                .get().await()
            val reports = snapshot.documents.mapNotNull { document ->
                val sessionId = document.getString("sessionId") ?: document.id
                val completedAt = document.getLong("completedAt") ?: return@mapNotNull null
                val steps = (document.get("steps") as? List<*>)
                    .orEmpty()
                    .mapNotNull stepLoop@{ rawStep ->
                        val step = rawStep as? Map<*, *> ?: return@stepLoop null
                        ExerciseStepReport(
                            stepId = step["stepId"] as? String ?: return@stepLoop null,
                            title = step["title"] as? String ?: "",
                            score = (step["score"] as? Number)?.toInt()?.coerceIn(0, 100) ?: 0,
                            attempts = (step["attempts"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 1,
                            analysisType = step["analysisType"] as? String
                                ?: ExerciseAnalysisType.INTERACTION,
                            targetText = step["targetText"] as? String ?: "",
                            recognizedText = step["recognizedText"] as? String ?: "",
                            feedback = step["feedback"] as? String ?: "",
                            mouthOpeningPercent = (step["mouthOpeningPercent"] as? Number)?.toInt() ?: -1,
                            lipRoundingPercent = (step["lipRoundingPercent"] as? Number)?.toInt() ?: -1,
                            holdSeconds = (step["holdSeconds"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                        )
                    }
                ExerciseSessionReport(
                    sessionId = sessionId,
                    activityId = document.getString("activityId").orEmpty(),
                    activityTitle = document.getString("activityTitle").orEmpty(),
                    levelId = (document.getLong("levelId") ?: 0L).toInt(),
                    levelTitle = document.getString("levelTitle").orEmpty(),
                    completedAt = completedAt,
                    durationSeconds = (document.getLong("durationSeconds") ?: 1L).coerceAtLeast(1L),
                    score = (document.getLong("score") ?: 0L).toInt().coerceIn(0, 100),
                    attemptNumber = (document.getLong("attemptNumber") ?: 1L).toInt().coerceAtLeast(1),
                    source = document.getString("source") ?: ExerciseSessionSource.LESSON,
                    steps = steps,
                )
            }.sortedByDescending { it.completedAt }
            Result.success(reports)
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
