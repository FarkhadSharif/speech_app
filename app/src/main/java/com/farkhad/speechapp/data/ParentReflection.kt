package com.farkhad.speechapp.data

data class ParentReflection(
    val id: Long,
    val dateKey: String,
    val createdAt: Long,
    val engagement: Int,
    val clarity: Int,
    val independence: Int,
    val practiceMinutes: Int,
    val context: String,
    val wins: Set<String>,
    val note: String,
    val nextStep: String,
) {
    val averageScore: Float
        get() = (engagement + clarity + independence) / 3f
}

data class ReflectionInsight(
    val title: String,
    val detail: String,
    val completedDays: Int,
    val averageScore: Float,
)

object ParentReflectionEngine {
    private const val WEEK_MS = 7L * 24L * 60L * 60L * 1000L

    fun weeklyInsight(
        reflections: List<ParentReflection>,
        now: Long = System.currentTimeMillis(),
    ): ReflectionInsight {
        val recent = reflections.filter { now - it.createdAt in 0..WEEK_MS }
        if (recent.isEmpty()) {
            return ReflectionInsight(
                title = "Бақылауды бүгін бастаңыз",
                detail = "Күн соңында 1 минуттық жазба қалдырсаңыз, апта соңында нақты үрдіс көрінеді.",
                completedDays = 0,
                averageScore = 0f,
            )
        }

        val engagement = recent.map { it.engagement }.average().toFloat()
        val clarity = recent.map { it.clarity }.average().toFloat()
        val independence = recent.map { it.independence }.average().toFloat()
        val weakest = minOf(engagement, clarity, independence)
        val recommendation = when (weakest) {
            engagement -> "Бала өзі таңдаған тақырыптан бастап, жауапқа асықпай уақыт беріңіз."
            clarity -> "Бір қысқа сөзді баяу үлгі етіп, сынамай табиғи түрде қайталаңыз."
            else -> "Сұрақтан кейін бірден көмектеспей, балаға өздігінен жауап беруге мүмкіндік беріңіз."
        }
        val average = recent.map { it.averageScore }.average().toFloat()
        val title = when {
            recent.size < 3 -> "Үрдіс қалыптасып келеді"
            average >= 4f -> "Апта тұрақты өтті"
            average >= 3f -> "Жақсы негіз бар"
            else -> "Қысқа әрі жеңіл қадам таңдаңыз"
        }

        return ReflectionInsight(
            title = title,
            detail = recommendation,
            completedDays = recent.map { it.dateKey }.distinct().size,
            averageScore = average,
        )
    }
}
