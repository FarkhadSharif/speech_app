package com.farkhad.speechapp.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

data class UserProgress(
    val levelId: String = "",
    val levelNumber: Int = 1,
    val isCompleted: Boolean = false,
    val stars: Int = 0
)

data class ChildProfile(
    val name: String = "",
    val age: String = "",
    val parentPin: String = "1234"
)

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    val currentUserUid: String?
        get() = auth.currentUser?.uid

    // Create a new account
    suspend fun signUp(email: String, pass: String, childName: String, childAge: String, parentPin: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: throw Exception("Тіркелу қатесі")

            // 1. Save user profile and child info in Firestore
            val userProfile = mapOf(
                "email" to email,
                "childName" to childName,
                "childAge" to childAge,
                "parentPin" to parentPin
            )
            db.collection("users").document(uid).set(userProfile).await()

            // 2. Initialize level 1 progress and learned words
            val initialProgress = UserProgress("level_1", 1, false, 0)
            db.collection("users").document(uid)
                .collection("levels").document("level_1")
                .set(initialProgress).await()
            
            db.collection("users").document(uid)
                .collection("metadata").document("learned_words")
                .set(mapOf("ids" to emptyList<String>())).await()

            Result.success(uid)
        } catch (e: FirebaseAuthUserCollisionException) {
            // Warning when the email is already registered
            Result.failure(Exception("Бұл электрондық пошта тіркеліп қойған. Кіру түймесін басыңыз."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update child information
    suspend fun updateChildInfo(childName: String, childAge: String, parentPin: String): Result<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Пайдаланушы табылмады")
            val updates = mapOf(
                "childName" to childName,
                "childAge" to childAge,
                "parentPin" to parentPin
            )
            db.collection("users").document(uid).set(updates, SetOptions.merge()).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get child information
    suspend fun getChildInfo(): Result<ChildProfile?> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Пайдаланушы табылмады")
            val doc = db.collection("users").document(uid).get().await()
            val profile = ChildProfile(
                name = doc.getString("childName") ?: "",
                age = doc.getString("childAge") ?: "",
                parentPin = doc.getString("parentPin") ?: "1234"
            )
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sign in existing user
    suspend fun signIn(email: String, pass: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: throw Exception("Кіру сәтсіз аяқталды")
            Result.success(uid)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Құпия сөз қате. Қайта тексеріп көріңіз."))
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Бұл электрондық пошта тіркелмеген."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Save completed level progress to Firestore
    suspend fun saveLevelProgress(levelNumber: Int, stars: Int): Result<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val data = mapOf(
                "levelNumber" to levelNumber,
                "isCompleted" to true,
                "stars" to stars
            )

            db.collection("users").document(uid)
                .collection("levels").document("level_$levelNumber")
                .set(data, SetOptions.merge()).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Sign out the current user
    fun signOut() {
        auth.signOut()
    }

    // Learned Words management
    suspend fun saveLearnedWord(wordId: String): Result<Boolean> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val docRef = db.collection("users").document(uid)
                .collection("metadata").document("learned_words")
            
            val doc = docRef.get().await()
            val currentIds = doc.get("ids") as? List<String> ?: emptyList()
            if (!currentIds.contains(wordId)) {
                val newIds = currentIds + wordId
                docRef.set(mapOf("ids" to newIds), SetOptions.merge()).await()
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLearnedWords(): Result<Set<String>> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("No user logged in")
            val doc = db.collection("users").document(uid)
                .collection("metadata").document("learned_words")
                .get().await()
            val ids = doc.get("ids") as? List<String> ?: emptyList()
            Result.success(ids.toSet())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}