package com.farkhad.speechapp.assessment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechAssessmentEngineTest {
    @Test
    fun `profile summarizes completed prompts and stays in valid range`() {
        val attempts = SpeechAssessmentEngine.prompts.map { prompt ->
            SpeechAssessmentEngine.evaluate(prompt, prompt.text)
        }

        val profile = SpeechAssessmentEngine.buildProfile(attempts)

        assertEquals(100, profile.overallScore)
        assertTrue(profile.focusSounds.isEmpty())
        assertEquals("Өте жақсы / Очень хорошо", profile.levelTitle)
        assertTrue(SoundMapEngine.scoresFrom(attempts).values.all { it == 100 })
        assertEquals(SoundStatus.GOOD, SoundMapEngine.statusFor(100))
        assertEquals(SoundStatus.REPEAT, SoundMapEngine.statusFor(70))
        assertEquals(SoundStatus.TRAIN, SoundMapEngine.statusFor(40))
        assertEquals(SoundStatus.UNCHECKED, SoundMapEngine.statusFor(null))
    }

    @Test
    fun `personal route prioritizes weakest sounds and course content`() {
        val route = PersonalizedRouteEngine.build(mapOf('қ' to 42, 'ң' to 68, 'ә' to 94))

        assertEquals(listOf('қ', 'ң'), route.focusSounds)
        assertTrue(route.words.isNotEmpty())
        assertTrue(route.activities.isNotEmpty())
        assertEquals(3, route.repeatAssessmentAfterDays)
    }
}
