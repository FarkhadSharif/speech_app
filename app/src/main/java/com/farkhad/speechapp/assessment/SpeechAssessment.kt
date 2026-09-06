package com.farkhad.speechapp.assessment

import com.farkhad.speechapp.audio.CharStatus
import com.farkhad.speechapp.audio.PronunciationEvaluator

data class AssessmentPrompt(
    val id: String,
    val text: String,
    val kindLabel: String,
    val emoji: String,
)

data class AssessmentAttempt(
    val prompt: AssessmentPrompt,
    val recognizedText: String,
    val score: Int,
    val strongSounds: Set<Char>,
    val focusSounds: Set<Char>,
)

data class SpeechProfile(
    val overallScore: Int,
    val levelTitle: String,
    val strongSounds: List<Char>,
    val focusSounds: List<Char>,
    val recommendation: String,
)

enum class SoundStatus {
    GOOD,
    REPEAT,
    TRAIN,
    UNCHECKED,
}

object SoundMapEngine {
    val trackedSounds = listOf('ә', 'ғ', 'қ', 'ң', 'ө', 'ұ', 'ү', 'і')

    fun scoresFrom(attempts: List<AssessmentAttempt>): Map<Char, Int> {
        return trackedSounds.mapNotNull { sound ->
            val relevant = attempts.filter { sound in normalizedLetters(it.prompt.text) }
            if (relevant.isEmpty()) return@mapNotNull null

            val scores = relevant.map { attempt ->
                when {
                    sound in attempt.focusSounds -> minOf(attempt.score, 49)
                    sound in attempt.strongSounds -> attempt.score
                    else -> 0
                }
            }
            sound to scores.average().toInt().coerceIn(0, 100)
        }.toMap()
    }

    fun statusFor(score: Int?): SoundStatus = when {
        score == null -> SoundStatus.UNCHECKED
        score >= 85 -> SoundStatus.GOOD
        score >= 60 -> SoundStatus.REPEAT
        else -> SoundStatus.TRAIN
    }

    private fun normalizedLetters(text: String): Set<Char> = text
        .lowercase()
        .filter { it in trackedSounds }
        .toSet()
}

object SpeechAssessmentEngine {
    val prompts = listOf(
        AssessmentPrompt("apple", "Алма", "Сөз / Слово", "🍎"),
        AssessmentPrompt("bird", "Құс", "Сөз / Слово", "🐦"),
        AssessmentPrompt("ski", "Шаңғы", "Сөз / Слово", "⛷️"),
        AssessmentPrompt("phrase", "Әжем маған ертегі айтады", "Сөйлем / Фраза", "💬"),
    )

    fun evaluate(prompt: AssessmentPrompt, recognizedText: String): AssessmentAttempt {
        val wordResults = PronunciationEvaluator.evaluate(prompt.text, recognizedText)
        val characters = wordResults.flatMap { it.charResults }
        val score = if (characters.isEmpty()) {
            0
        } else {
            (characters.count { it.status == CharStatus.CORRECT } * 100f / characters.size)
                .toInt()
                .coerceIn(0, 100)
        }

        val soundResults = characters.filter { it.char.lowercaseChar() in KAZAKH_SOUNDS }
        return AssessmentAttempt(
            prompt = prompt,
            recognizedText = recognizedText.trim(),
            score = score,
            strongSounds = soundResults
                .filter { it.status == CharStatus.CORRECT }
                .map { it.char.lowercaseChar() }
                .toSet(),
            focusSounds = soundResults
                .filter { it.status != CharStatus.CORRECT }
                .map { it.char.lowercaseChar() }
                .toSet(),
        )
    }

    fun buildProfile(attempts: List<AssessmentAttempt>): SpeechProfile {
        val overall = attempts.map { it.score }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
        val focusCounts = attempts
            .flatMap { it.focusSounds }
            .groupingBy { it }
            .eachCount()
        val focus = focusCounts.entries
            .sortedWith(compareByDescending<Map.Entry<Char, Int>> { it.value }.thenBy { it.key })
            .map { it.key }
        val strong = attempts
            .flatMap { it.strongSounds }
            .filterNot { it in focus }
            .distinct()
            .sorted()

        return SpeechProfile(
            overallScore = overall,
            levelTitle = when {
                overall >= 85 -> "Өте жақсы / Очень хорошо"
                overall >= 65 -> "Жақсы негіз / Хорошая основа"
                else -> "Жаттығуды бастайық / Начнём тренировку"
            },
            strongSounds = strong,
            focusSounds = focus,
            recommendation = recommendationFor(focus, overall),
        )
    }

    private fun recommendationFor(focus: List<Char>, overall: Int): String = when {
        focus.any { it == 'қ' || it == 'ғ' } ->
            "Қ–Ғ дыбыстарын баяу қайталап, тілдің артқы бөлігінің қозғалысына назар аударыңыз. / Повторяйте Қ–Ғ медленно."
        'ң' in focus ->
            "Ң дыбысын сөздің ортасы мен соңында жаттықтырыңыз. / Потренируйте звук Ң в середине и конце слова."
        focus.any { it in setOf('ә', 'ө', 'ү', 'ұ') } ->
            "Дауысты дыбыстарды Face Map айнасымен бірге қайталаңыз. / Повторите гласные вместе с Face Map."
        overall >= 85 ->
            "Күрделірек сөйлемдерге өтуге болады. / Можно переходить к более сложным фразам."
        else ->
            "Сөздерді алдымен тыңдап, кейін баяу және анық қайталаңыз. / Сначала слушайте, затем повторяйте медленно и чётко."
    }

    private val KAZAKH_SOUNDS = SoundMapEngine.trackedSounds.toSet()
}
