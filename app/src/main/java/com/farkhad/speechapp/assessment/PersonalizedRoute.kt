package com.farkhad.speechapp.assessment

import com.farkhad.speechapp.model.Curriculum
import com.farkhad.speechapp.model.GameActivity
import com.farkhad.speechapp.model.VocabularyWord
import com.farkhad.speechapp.model.VoiceRound

data class RouteWord(
    val word: VocabularyWord,
    val levelId: Int,
)

data class RouteActivity(
    val activity: GameActivity,
    val levelId: Int,
)

data class PersonalizedRoute(
    val needsAssessment: Boolean,
    val focusSounds: List<Char>,
    val words: List<RouteWord>,
    val activities: List<RouteActivity>,
    val repeatAssessmentAfterDays: Int,
)

object PersonalizedRouteEngine {
    fun build(soundScores: Map<Char, Int>): PersonalizedRoute {
        if (soundScores.isEmpty()) {
            return PersonalizedRoute(
                needsAssessment = true,
                focusSounds = emptyList(),
                words = emptyList(),
                activities = emptyList(),
                repeatAssessmentAfterDays = 0,
            )
        }

        val rankedScores = soundScores.entries.sortedBy { it.value }
        val soundsNeedingWork = rankedScores.filter { it.value < GOOD_SCORE }
        val focusSounds = (soundsNeedingWork.ifEmpty { rankedScores })
            .take(MAX_FOCUS_SOUNDS)
            .map { it.key }
        val words = Curriculum.levels.flatMap { level ->
            level.vocabulary
                .filter { word -> focusSounds.any { it in word.word.lowercase() } }
                .map { RouteWord(it, level.id) }
        }.distinctBy { it.word.word.lowercase() }
            .take(MAX_WORDS)

        val preferredLevelIds = words.map { it.levelId }.distinct()
        val activities = preferredLevelIds.flatMap { levelId ->
            Curriculum.level(levelId).activities
                .sortedByDescending { activity -> activity.rounds.any { it is VoiceRound } }
                .map { RouteActivity(it, levelId) }
        }.distinctBy { it.activity.id }
            .take(MAX_ACTIVITIES)

        return PersonalizedRoute(
            needsAssessment = false,
            focusSounds = focusSounds,
            words = words,
            activities = activities,
            repeatAssessmentAfterDays = when {
                rankedScores.any { it.value < 60 } -> 3
                rankedScores.any { it.value < GOOD_SCORE } -> 5
                else -> 7
            },
        )
    }

    private const val GOOD_SCORE = 85
    private const val MAX_FOCUS_SOUNDS = 3
    private const val MAX_WORDS = 6
    private const val MAX_ACTIVITIES = 3
}
