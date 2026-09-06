package com.farkhad.speechapp.facemap

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceMapEvaluatorTest {
    @Test
    fun missingFaceNeverCompletesExercise() {
        val result = FaceMapEvaluator.evaluate(FaceMapExercise.OPEN_MOUTH, FaceMapFrame())

        assertFalse(result.success)
        assertTrue(result.progress == 0f)
    }

    @Test
    fun centeredFrontalFaceCompletesCalibration() {
        val result = FaceMapEvaluator.evaluate(
            FaceMapExercise.CENTER,
            FaceMapFrame(
                faceFound = true,
                centerOffset = 0.01f,
                headYawDegrees = 1f,
                headRollDegrees = 2f,
            ),
        )

        assertTrue(result.success)
    }

    @Test
    fun openMouthCompletesSoundA() {
        val result = FaceMapEvaluator.evaluate(
            FaceMapExercise.OPEN_MOUTH,
            FaceMapFrame(faceFound = true, mouthOpenRatio = 0.19f),
        )

        assertTrue(result.success)
    }

    @Test
    fun narrowOpenMouthCompletesRoundedLips() {
        val result = FaceMapEvaluator.evaluate(
            FaceMapExercise.ROUND_LIPS,
            FaceMapFrame(
                faceFound = true,
                mouthOpenRatio = 0.28f,
                mouthWidthRatio = 0.27f,
            ),
        )

        assertTrue(result.success)
    }

    @Test
    fun wideMouthCompletesSmile() {
        val result = FaceMapEvaluator.evaluate(
            FaceMapExercise.SMILE,
            FaceMapFrame(faceFound = true, mouthWidthRatio = 0.39f),
        )

        assertTrue(result.success)
    }

    @Test
    fun narrowRoundedLipsCompleteUExercise() {
        val result = FaceMapEvaluator.evaluate(
            FaceMapExercise.NARROW_LIPS,
            FaceMapFrame(
                faceFound = true,
                mouthOpenRatio = 0.17f,
                mouthWidthRatio = 0.28f,
            ),
        )

        assertTrue(result.success)
    }
}
