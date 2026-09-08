package com.farkhad.speechapp.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.farkhad.speechapp.model.Curriculum
import com.farkhad.speechapp.model.CurriculumLevel
import com.farkhad.speechapp.model.GameActivity
import org.json.JSONArray
import org.json.JSONObject
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

    val assessedSoundScores: Map<Char, Int>
        get() {
            val serialized = preferences.getString(KEY_SOUND_SCORES, "").orEmpty()
            if (serialized.isBlank()) return emptyMap()
            return serialized.split(",").mapNotNull { entry ->
                val parts = entry.split(":", limit = 2)
                val sound = parts.getOrNull(0)?.singleOrNull() ?: return@mapNotNull null
                val score = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 100)
                    ?: return@mapNotNull null
                sound to score
            }.toMap()
        }

    val lastAssessmentAt: Long
        get() = preferences.getLong(KEY_LAST_ASSESSMENT_AT, 0L)

    val parentReflections: List<ParentReflection>
        get() {
            val source = preferences.getString(KEY_PARENT_REFLECTIONS, "[]").orEmpty()
            return runCatching {
                val array = JSONArray(source)
                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        val winsArray = item.optJSONArray("wins") ?: JSONArray()
                        val wins = buildSet {
                            for (winIndex in 0 until winsArray.length()) {
                                winsArray.optString(winIndex).takeIf { it.isNotBlank() }?.let(::add)
                            }
                        }
                        add(
                            ParentReflection(
                                id = item.optLong("id"),
                                dateKey = item.optString("dateKey"),
                                createdAt = item.optLong("createdAt"),
                                engagement = item.optInt("engagement", 3).coerceIn(1, 5),
                                clarity = item.optInt("clarity", 3).coerceIn(1, 5),
                                independence = item.optInt("independence", 3).coerceIn(1, 5),
                                practiceMinutes = item.optInt("practiceMinutes", 5).coerceAtLeast(0),
                                context = item.optString("context"),
                                wins = wins,
                                note = item.optString("note"),
                                nextStep = item.optString("nextStep"),
                            ),
                        )
                    }
                }.sortedByDescending { it.createdAt }
            }.getOrDefault(emptyList())
        }

    fun saveParentReflection(reflection: ParentReflection) {
        val updated = parentReflections
            .filterNot { it.dateKey == reflection.dateKey }
            .plus(reflection)
            .sortedByDescending { it.createdAt }
            .take(MAX_PARENT_REFLECTIONS)
        persistParentReflections(updated)
        revision += 1
    }

    fun mergeParentReflections(remote: List<ParentReflection>) {
        if (remote.isEmpty()) return
        val merged = (parentReflections + remote)
            .groupBy { it.dateKey }
            .mapNotNull { (_, entries) -> entries.maxByOrNull { it.createdAt } }
            .sortedByDescending { it.createdAt }
            .take(MAX_PARENT_REFLECTIONS)
        persistParentReflections(merged)
        revision += 1
    }

    private fun persistParentReflections(entries: List<ParentReflection>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("id", entry.id)
                    .put("dateKey", entry.dateKey)
                    .put("createdAt", entry.createdAt)
                    .put("engagement", entry.engagement)
                    .put("clarity", entry.clarity)
                    .put("independence", entry.independence)
                    .put("practiceMinutes", entry.practiceMinutes)
                    .put("context", entry.context)
                    .put("wins", JSONArray(entry.wins.toList()))
                    .put("note", entry.note)
                    .put("nextStep", entry.nextStep),
            )
        }
        preferences.edit().putString(KEY_PARENT_REFLECTIONS, array.toString()).apply()
    }

    fun saveAssessedSoundScores(scores: Map<Char, Int>) {
        val serialized = scores.entries.joinToString(",") { (sound, score) ->
            "$sound:${score.coerceIn(0, 100)}"
        }
        preferences.edit()
            .putString(KEY_SOUND_SCORES, serialized)
            .putLong(KEY_LAST_ASSESSMENT_AT, System.currentTimeMillis())
            .apply()
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
        private const val KEY_SOUND_SCORES = "assessed_sound_scores"
        private const val KEY_LAST_ASSESSMENT_AT = "last_assessment_at"
        private const val KEY_PARENT_REFLECTIONS = "parent_reflections"
        private const val MAX_PARENT_REFLECTIONS = 90
    }
}

