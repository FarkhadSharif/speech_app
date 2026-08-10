package com.farkhad.speechapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumTest {
    @Test
    fun curriculumHasSixSequentialLevelsAndEighteenActivities() {
        assertEquals((1..6).toList(), Curriculum.levels.map { it.id })
        assertEquals(18, Curriculum.allActivities.size)
        assertEquals(18, Curriculum.allActivities.map { it.id }.distinct().size)
        assertTrue(Curriculum.allActivities.all { it.rounds.size >= 4 })
    }

    @Test
    fun everyRoundHasAUniqueIdAndValidAnswer() {
        val rounds = Curriculum.allActivities.flatMap { it.rounds }
        assertEquals(rounds.size, rounds.map { it.id }.distinct().size)
        assertTrue(rounds.size >= 75)

        rounds.filterIsInstance<ChoiceRound>().forEach { round ->
            assertTrue(round.correctIndex in round.options.indices)
            assertTrue(round.options.size >= 2)
            assertTrue(round.speech.isNotBlank())
        }
        rounds.filterIsInstance<StoryOrderRound>().forEach { round ->
            assertTrue(round.cardsInOrder.size >= 3)
        }
    }

    @Test
    fun progressionIntroducesMoreComplexLanguageTasks() {
        val levelOneRounds = Curriculum.level(1).activities.flatMap { it.rounds }
        val levelFourRounds = Curriculum.level(4).activities.flatMap { it.rounds }
        val levelFiveRounds = Curriculum.level(5).activities.flatMap { it.rounds }
        val levelSixRounds = Curriculum.level(6).activities.flatMap { it.rounds }

        assertTrue(levelOneRounds.none { it is ArrangeWordsRound || it is StoryOrderRound })
        assertTrue(levelFourRounds.any { it is ArrangeWordsRound })
        assertTrue(levelFiveRounds.any { it is StoryOrderRound })
        assertTrue(levelSixRounds.filterIsInstance<ArrangeWordsRound>().all { it.wordsInOrder.size >= 5 })
        assertTrue(levelSixRounds.filterIsInstance<StoryOrderRound>().all { it.cardsInOrder.size >= 4 })
    }

    @Test
    fun spokenPhraseScoringIsPositiveAndTolerant() {
        assertEquals(100, scoreSpokenPhrase("Мен алма жеймін", "Мен алма жеймін"))
        assertTrue(scoreSpokenPhrase("Мен алма жеймін", "алма жеймін") >= 60)
        assertEquals(0, scoreSpokenPhrase("Мен алма жеймін", ""))
    }
}

