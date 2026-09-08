package com.farkhad.speechapp.data

object ExerciseAnalysisType {
    const val INTERACTION = "interaction"
    const val AUDIO = "audio"
    const val VIDEO = "video"
    const val ADULT_ASSISTED = "adult_assisted"
}

object ExerciseSessionSource {
    const val LESSON = "lesson"
    const val FACE_MAP = "face_map"
    const val ASSESSMENT = "assessment"
}

data class ExerciseStepReport(
    val stepId: String,
    val title: String,
    val score: Int,
    val attempts: Int = 1,
    val analysisType: String = ExerciseAnalysisType.INTERACTION,
    val targetText: String = "",
    val recognizedText: String = "",
    val feedback: String = "",
    val mouthOpeningPercent: Int = -1,
    val lipRoundingPercent: Int = -1,
    val holdSeconds: Int = 0,
)

data class ExerciseSessionReport(
    val sessionId: String,
    val activityId: String,
    val activityTitle: String,
    val levelId: Int,
    val levelTitle: String,
    val completedAt: Long,
    val durationSeconds: Long,
    val score: Int,
    val attemptNumber: Int,
    val source: String,
    val steps: List<ExerciseStepReport>,
) {
    val audioAnalyzedSteps: Int
        get() = steps.count { it.analysisType == ExerciseAnalysisType.AUDIO }

    val videoAnalyzedSteps: Int
        get() = steps.count { it.analysisType == ExerciseAnalysisType.VIDEO }
}

data class AudioRoundEvidence(
    val attempts: Int,
    val targetText: String,
    val recognizedText: String,
    val automaticAnalysisUsed: Boolean,
)
