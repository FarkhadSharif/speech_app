package com.farkhad.speechapp.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.farkhad.speechapp.model.Curriculum
import com.farkhad.speechapp.model.CurriculumLevel
import com.farkhad.speechapp.model.GameActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressRepository(context: Context, uid: String) {
    private val preferences = context.getSharedPreferences("speech_progress_$uid", Context.MODE_PRIVATE)

    var revision by mutableStateOf(0)
        private set

    val completedActivityIds: Set<String>
        get() = preferences.getStringSet(KEY_COMPLETED, emptySet()).orEmpty().toSet()

    val completedCount: Int
        get() = completedActivityIds.size

    val totalStars: Int
        get() = Curriculum.allActivities.sumOf { starsFor(it.id) }

    val totalSessions: Int
        get() = preferences.getInt(KEY_SESSIONS, 0)

    val activeDays: Int
        get() = preferences.getStringSet(KEY_ACTIVE_DAYS, emptySet()).orEmpty().size

    val practicedRounds: Int
        get() = Curriculum.allActivities
            .filter { it.id in completedActivityIds }
            .sumOf { it.rounds.size }

    val overallProgress: Float
        get() = if (Curriculum.allActivities.isEmpty()) 0f
        else completedCount.toFloat() / Curriculum.allActivities.size

    fun isCompleted(activityId: String): Boolean = activityId in completedActivityIds

    fun isLevelUnlocked(levelId: Int): Boolean {
        if (levelId <= 1) return true
        val previousLevel = Curriculum.level(levelId - 1)
        return previousLevel.activities.all { isCompleted(it.id) }
    }

    fun levelProgress(level: CurriculumLevel): Float {
        if (level.activities.isEmpty()) return 0f
        val complete = level.activities.count { isCompleted(it.id) }
        return complete.toFloat() / level.activities.size
    }

    fun levelStars(level: CurriculumLevel): Int = level.activities.sumOf { starsFor(it.id) }

    fun starsFor(activityId: String): Int = preferences.getInt("stars_$activityId", 0)

    fun bestScoreFor(activityId: String): Int = preferences.getInt("score_$activityId", 0)

    fun attemptsFor(activityId: String): Int = preferences.getInt("attempts_$activityId", 0)

    fun completeActivity(activity: GameActivity, score: Int) {
        val safeScore = score.coerceIn(0, 100)
        val stars = when {
            safeScore >= 85 -> 3
            safeScore >= 60 -> 2
            else -> 1
        }
        val completed = completedActivityIds.toMutableSet().apply { add(activity.id) }
        val days = preferences.getStringSet(KEY_ACTIVE_DAYS, emptySet()).orEmpty().toMutableSet().apply {
            add(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        }

        preferences.edit()
            .putStringSet(KEY_COMPLETED, completed)
            .putStringSet(KEY_ACTIVE_DAYS, days)
            .putInt("score_${activity.id}", maxOf(bestScoreFor(activity.id), safeScore))
            .putInt("stars_${activity.id}", maxOf(starsFor(activity.id), stars))
            .putInt("attempts_${activity.id}", attemptsFor(activity.id) + 1)
            .putInt(KEY_SESSIONS, totalSessions + 1)
            .apply()
        revision += 1
    }

    fun logPronunciationFail(word: String) {
        val currentFails = getPronunciationFailsMap().toMutableMap()
        currentFails[word] = (currentFails[word] ?: 0) + 1
        
        // Simple serialization: word1:count1,word2:count2
        val serialized = currentFails.entries.joinToString(",") { "${it.key}:${it.value}" }
        preferences.edit().putString(KEY_PRONUNCIATION_FAILS, serialized).apply()
        revision += 1
    }

    fun getDifficultWords(): List<Pair<String, Int>> {
        return getPronunciationFailsMap().toList()
            .sortedByDescending { it.second }
            .take(10)
    }

    private fun getPronunciationFailsMap(): Map<String, Int> {
        val serialized = preferences.getString(KEY_PRONUNCIATION_FAILS, "") ?: ""
        if (serialized.isBlank()) return emptyMap()
        
        return try {
            serialized.split(",").associate {
                val parts = it.split(":")
                parts[0] to parts[1].toInt()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    val learnedWordIds: Set<String>
        get() = preferences.getStringSet(KEY_LEARNED_WORDS, emptySet()).orEmpty().toSet()

    fun markWordAsLearned(wordId: String) {
        val learned = learnedWordIds.toMutableSet().apply { add(wordId) }
        preferences.edit().putStringSet(KEY_LEARNED_WORDS, learned).apply()
        revision += 1
    }

    fun updateLearnedWords(wordIds: Set<String>) {
        preferences.edit().putStringSet(KEY_LEARNED_WORDS, wordIds).apply()
        revision += 1
    }

    fun resetProgress() {
        preferences.edit().clear().apply()
        revision += 1
    }

    companion object {
        private const val KEY_COMPLETED = "completed_activities"
        private const val KEY_ACTIVE_DAYS = "active_days"
        private const val KEY_SESSIONS = "sessions"
        private const val KEY_PRONUNCIATION_FAILS = "pronunciation_fails"
        private const val KEY_LEARNED_WORDS = "learned_words"
    }
}

