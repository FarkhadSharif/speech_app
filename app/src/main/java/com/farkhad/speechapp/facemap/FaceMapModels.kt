package com.farkhad.speechapp.facemap

data class NormalizedFacePoint(
    val x: Float,
    val y: Float,
)

data class FaceContourPath(
    val type: Int,
    val points: List<NormalizedFacePoint>,
)

data class FaceMapFrame(
    val faceFound: Boolean = false,
    val contours: List<FaceContourPath> = emptyList(),
    val mouthOpenRatio: Float = 0f,
    val mouthWidthRatio: Float = 0f,
    val centerOffset: Float = 1f,
    val headYawDegrees: Float = 0f,
    val headRollDegrees: Float = 0f,
    val sourceAspectRatio: Float = 0.75f,
)

enum class FaceMapExercise(
    val emoji: String,
    val title: String,
    val instruction: String,
    val hint: String,
    val voicePrompt: String,
) {
    CENTER(
        emoji = "👀",
        title = "Бетіңізді көрсетіңіз / Покажите лицо",
        instruction = "Камераға тура қарап, бетіңізді жақтаудың ортасына орналастырыңыз.",
        hint = "Смотрите прямо и поместите лицо в центр рамки.",
        voicePrompt = "Камераға тура қараңыз",
    ),
    OPEN_MOUTH(
        emoji = "😮",
        title = "А–Ә дыбыстары / Звуки А–Ә",
        instruction = "А–Ә дыбыстарын айтқандай аузыңызды кең ашыңыз.",
        hint = "Широко откройте рот, как при звуках А–Ә.",
        voicePrompt = "А және Ә дыбыстарын айтыңыз",
    ),
    ROUND_LIPS(
        emoji = "😗",
        title = "О–Ө дыбыстары / Звуки О–Ө",
        instruction = "Ерніңізді дөңгелетіп, О–Ө дыбыстарын айтыңыз.",
        hint = "Округлите губы и произнесите О–Ө.",
        voicePrompt = "О және Ө дыбыстарын айтыңыз",
    ),
    NARROW_LIPS(
        emoji = "😙",
        title = "Ұ–Ү дыбыстары / Звуки Ұ–Ү",
        instruction = "Ерніңізді алға созып, Ұ–Ү дыбыстарын айтыңыз.",
        hint = "Вытяните губы вперёд и произнесите Ұ–Ү.",
        voicePrompt = "Ұ және Ү дыбыстарын айтыңыз",
    ),
    SMILE(
        emoji = "😁",
        title = "І дыбысы / Звук І",
        instruction = "Ерніңізді екі жаққа созып, І дыбысын айтыңыз.",
        hint = "Растяните губы в стороны и произнесите І.",
        voicePrompt = "І дыбысын айтыңыз",
    ),
}

data class FaceMapEvaluation(
    val progress: Float,
    val success: Boolean,
    val status: String,
    val openingScore: Float = 0f,
    val roundingScore: Float = 0f,
)

object FaceMapEvaluator {
    fun evaluate(exercise: FaceMapExercise, frame: FaceMapFrame): FaceMapEvaluation {
        if (!frame.faceFound) {
            return FaceMapEvaluation(
                progress = 0f,
                success = false,
                status = "Бет көрінбейді / Лицо не найдено",
            )
        }

        val openingScore = normalized(frame.mouthOpenRatio, 0.04f, 0.24f)
        val roundingScore = normalized(0.44f - frame.mouthWidthRatio, 0f, 0.16f)

        return when (exercise) {
            FaceMapExercise.CENTER -> {
                val centerProgress = (1f - frame.centerOffset / MAX_CENTER_OFFSET)
                    .coerceIn(0f, 1f)
                val angleProgress = (1f - maxOf(
                    kotlin.math.abs(frame.headYawDegrees),
                    kotlin.math.abs(frame.headRollDegrees),
                ) / MAX_HEAD_ANGLE).coerceIn(0f, 1f)
                val progress = minOf(centerProgress, angleProgress)
                FaceMapEvaluation(
                    progress = progress,
                    success = progress >= SUCCESS_PROGRESS,
                    status = if (progress >= SUCCESS_PROGRESS) {
                        "Керемет! / Отлично!"
                    } else {
                        "Камераға тура қараңыз / Смотрите прямо"
                    },
                    openingScore = openingScore,
                    roundingScore = roundingScore,
                )
            }

            FaceMapExercise.OPEN_MOUTH -> thresholdEvaluation(
                value = openingScore,
                start = 0f,
                target = 0.75f,
                workingStatus = "Аузыңызды кеңірек ашыңыз / Откройте рот шире",
                openingScore = openingScore,
                roundingScore = roundingScore,
            )

            FaceMapExercise.ROUND_LIPS -> {
                val openness = normalized(frame.mouthOpenRatio, 0.10f, 0.28f)
                val narrowness = normalized(0.40f - frame.mouthWidthRatio, 0f, 0.13f)
                val progress = minOf(openness, narrowness)
                FaceMapEvaluation(
                    progress = progress,
                    success = progress >= SUCCESS_PROGRESS,
                    status = if (progress >= SUCCESS_PROGRESS) {
                        "Дөңгелек О! / Отличный круглый О!"
                    } else {
                        "Ерінді дөңгелетіңіз / Округлите губы"
                    },
                    openingScore = openingScore,
                    roundingScore = roundingScore,
                )
            }

            FaceMapExercise.NARROW_LIPS -> {
                val openness = normalized(frame.mouthOpenRatio, 0.04f, 0.16f)
                val narrowness = normalized(0.44f - frame.mouthWidthRatio, 0f, 0.15f)
                val progress = minOf(openness, narrowness)
                FaceMapEvaluation(
                    progress = progress,
                    success = progress >= SUCCESS_PROGRESS,
                    status = if (progress >= SUCCESS_PROGRESS) {
                        "Ұ–Ү қалпы дұрыс! / Положение Ұ–Ү верное!"
                    } else {
                        "Ерінді алға созыңыз / Вытяните губы вперёд"
                    },
                    openingScore = openingScore,
                    roundingScore = roundingScore,
                )
            }

            FaceMapExercise.SMILE -> thresholdEvaluation(
                value = frame.mouthWidthRatio,
                start = 0.28f,
                target = 0.38f,
                workingStatus = "Кеңірек күлімдеңіз / Улыбнитесь шире",
                openingScore = openingScore,
                roundingScore = roundingScore,
            )
        }
    }

    private fun thresholdEvaluation(
        value: Float,
        start: Float,
        target: Float,
        workingStatus: String,
        openingScore: Float,
        roundingScore: Float,
    ): FaceMapEvaluation {
        val progress = normalized(value, start, target)
        return FaceMapEvaluation(
            progress = progress,
            success = progress >= SUCCESS_PROGRESS,
            status = if (progress >= SUCCESS_PROGRESS) {
                "Керемет! / Отлично!"
            } else {
                workingStatus
            },
            openingScore = openingScore,
            roundingScore = roundingScore,
        )
    }

    private fun normalized(value: Float, start: Float, target: Float): Float {
        if (target <= start) return 0f
        return ((value - start) / (target - start)).coerceIn(0f, 1f)
    }

    private const val MAX_CENTER_OFFSET = 0.22f
    private const val MAX_HEAD_ANGLE = 20f
    private const val SUCCESS_PROGRESS = 0.82f
}
