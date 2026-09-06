package com.farkhad.speechapp.facemap

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.hypot

class FaceMapAnalyzer(
    private val onFrame: (FaceMapFrame) -> Unit,
) : ImageAnalysis.Analyzer, Closeable {
    private val processing = AtomicBoolean(false)
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setMinFaceSize(0.25f)
            .build(),
    )

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || !processing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val outputWidth = if (rotation % 180 == 0) imageProxy.width else imageProxy.height
        val outputHeight = if (rotation % 180 == 0) imageProxy.height else imageProxy.width
        val input = InputImage.fromMediaImage(mediaImage, rotation)

        detector.process(input)
            .addOnSuccessListener { faces ->
                onFrame(
                    faces.firstOrNull()?.toFaceMapFrame(outputWidth, outputHeight)
                        ?: FaceMapFrame(),
                )
            }
            .addOnFailureListener {
                onFrame(FaceMapFrame())
            }
            .addOnCompleteListener {
                processing.set(false)
                imageProxy.close()
            }
    }

    override fun close() {
        detector.close()
    }

    private fun Face.toFaceMapFrame(width: Int, height: Int): FaceMapFrame {
        val widthFloat = width.coerceAtLeast(1).toFloat()
        val heightFloat = height.coerceAtLeast(1).toFloat()
        val contourTypes = listOf(
            FaceContour.FACE,
            FaceContour.LEFT_EYEBROW_TOP,
            FaceContour.LEFT_EYEBROW_BOTTOM,
            FaceContour.RIGHT_EYEBROW_TOP,
            FaceContour.RIGHT_EYEBROW_BOTTOM,
            FaceContour.LEFT_EYE,
            FaceContour.RIGHT_EYE,
            FaceContour.NOSE_BRIDGE,
            FaceContour.NOSE_BOTTOM,
            FaceContour.UPPER_LIP_TOP,
            FaceContour.UPPER_LIP_BOTTOM,
            FaceContour.LOWER_LIP_TOP,
            FaceContour.LOWER_LIP_BOTTOM,
        )
        val contours = contourTypes.mapNotNull { type ->
            val points = getContour(type)?.points.orEmpty()
            if (points.size < 2) return@mapNotNull null
            FaceContourPath(
                type = type,
                points = points.map { point ->
                    NormalizedFacePoint(
                        x = (1f - point.x / widthFloat).coerceIn(0f, 1f),
                        y = (point.y / heightFloat).coerceIn(0f, 1f),
                    )
                },
            )
        }

        val upperInner = getContour(FaceContour.UPPER_LIP_BOTTOM)?.points.orEmpty()
        val lowerInner = getContour(FaceContour.LOWER_LIP_TOP)?.points.orEmpty()
        val lipPoints = listOf(
            FaceContour.UPPER_LIP_TOP,
            FaceContour.UPPER_LIP_BOTTOM,
            FaceContour.LOWER_LIP_TOP,
            FaceContour.LOWER_LIP_BOTTOM,
        ).flatMap { type -> getContour(type)?.points.orEmpty() }

        val mouthWidth = if (lipPoints.isEmpty()) {
            0f
        } else {
            lipPoints.maxOf { it.x } - lipPoints.minOf { it.x }
        }
        val innerGap = if (upperInner.isEmpty() || lowerInner.isEmpty()) {
            0f
        } else {
            abs(lowerInner.map { it.y }.average() - upperInner.map { it.y }.average()).toFloat()
        }
        val bounds = boundingBox
        val centerX = bounds.exactCenterX() / widthFloat
        val centerY = bounds.exactCenterY() / heightFloat

        return FaceMapFrame(
            faceFound = true,
            contours = contours,
            mouthOpenRatio = if (mouthWidth > 1f) innerGap / mouthWidth else 0f,
            mouthWidthRatio = if (bounds.width() > 0) mouthWidth / bounds.width() else 0f,
            centerOffset = hypot(centerX - 0.5f, centerY - 0.48f),
            headYawDegrees = headEulerAngleY,
            headRollDegrees = headEulerAngleZ,
            sourceAspectRatio = widthFloat / heightFloat,
        )
    }
}
