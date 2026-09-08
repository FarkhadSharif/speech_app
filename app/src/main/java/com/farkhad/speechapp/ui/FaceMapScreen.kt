@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.farkhad.speechapp.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.farkhad.speechapp.audio.SpeechAudio
import com.farkhad.speechapp.facemap.FaceContourPath
import com.farkhad.speechapp.facemap.FaceMapAnalyzer
import com.farkhad.speechapp.facemap.FaceMapEvaluator
import com.farkhad.speechapp.facemap.FaceMapExercise
import com.farkhad.speechapp.facemap.FaceMapFrame
import com.farkhad.speechapp.ui.theme.AppBackground
import com.farkhad.speechapp.ui.theme.AppBlue
import com.farkhad.speechapp.ui.theme.AppGreen
import com.farkhad.speechapp.ui.theme.AppOrange
import com.farkhad.speechapp.ui.theme.AppPurple
import com.farkhad.speechapp.ui.theme.AppNavy
import com.farkhad.speechapp.ui.theme.AppRed
import com.farkhad.speechapp.ui.theme.AppText
import com.google.mlkit.vision.face.FaceContour
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun FaceMapScreen(
    audio: SpeechAudio,
    onBack: () -> Unit,
    onCompleted: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> cameraGranted = granted }

    if (!cameraGranted) {
        FaceMapPermissionScreen(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onBack = onBack,
        )
        return
    }

    FaceMapCameraExperience(
        audio = audio,
        onBack = onBack,
        onCompleted = onCompleted,
    )
}

@Composable
private fun FaceMapPermissionScreen(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("FM / 02", color = AppRed, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(
            text = "Артикуляциялық\nайна",
            modifier = Modifier.padding(top = 10.dp),
            color = AppText,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Камера ерін мен жақ қозғалысын экранда көрсетеді. Кадрлар сақталмайды және интернетке жіберілмейді.\n\nКамера показывает движения губ и челюсти. Кадры не сохраняются и не отправляются в интернет.",
            color = AppText.copy(alpha = 0.68f),
            lineHeight = 21.sp,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AppNavy),
            shape = RoundedCornerShape(4.dp),
        ) {
            Text("Камераны қосу / Включить камеру", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onBack) {
            Text("Кейін / Позже", color = AppText.copy(alpha = 0.64f))
        }
    }
}

@Composable
private fun FaceMapCameraExperience(
    audio: SpeechAudio,
    onBack: () -> Unit,
    onCompleted: (() -> Unit)?,
) {
    val exercises = FaceMapExercise.entries
    var exerciseIndex by rememberSaveable { mutableStateOf(0) }
    var completedExercises by remember { mutableStateOf(emptySet<FaceMapExercise>()) }
    var frame by remember { mutableStateOf(FaceMapFrame()) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val exercise = exercises[exerciseIndex]
    var holdProgress by remember(exercise) { mutableStateOf(0f) }
    val evaluation = FaceMapEvaluator.evaluate(exercise, frame)
    val allComplete = completedExercises.size == exercises.size

    LaunchedEffect(exercise) {
        delay(250)
        audio.speak(exercise.voicePrompt, slower = true)
    }

    LaunchedEffect(exercise, evaluation.success) {
        if (!evaluation.success) {
            holdProgress = 0f
            return@LaunchedEffect
        }
        if (exercise in completedExercises) return@LaunchedEffect

        repeat(HOLD_STEPS) { step ->
            delay(HOLD_STEP_MS)
            holdProgress = (step + 1f) / HOLD_STEPS.toFloat()
        }
        completedExercises = completedExercises + exercise
        audio.playCorrect()
        delay(650)
        if (exerciseIndex < exercises.lastIndex) {
            exerciseIndex += 1
        } else {
            audio.playCelebration()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF071426))
            .statusBarsPadding(),
    ) {
        FaceMapTopBar(
            completed = completedExercises.size,
            total = exercises.size,
            onBack = onBack,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.Black),
        ) {
            FaceCameraPreview(
                onFrame = { frame = it },
                onError = { cameraError = it },
            )
            FaceContourOverlay(
                frame = frame,
                exercise = exercise,
                evaluationProgress = evaluation.progress,
                modifier = Modifier.fillMaxSize(),
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp),
                color = if (frame.faceFound) {
                    Color(0xCC123A43)
                } else {
                    Color(0xCC4A2430)
                },
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    text = if (frame.faceFound) {
                        "● FACE MAP белсенді / активен"
                    } else {
                        "○ Бет ізделуде / Ищем лицо"
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }

            if (exercise in completedExercises && !allComplete) {
                SuccessGlow(modifier = Modifier.align(Alignment.Center))
            }

            cameraError?.let { error ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    color = Color(0xE62B1720),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(18.dp),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        FaceMapExercisePanel(
            exercise = exercise,
            evaluationProgress = evaluation.progress,
            openingScore = evaluation.openingScore,
            roundingScore = evaluation.roundingScore,
            holdProgress = holdProgress,
            status = evaluation.status,
            completed = completedExercises,
            allComplete = allComplete,
            onSelectExercise = { exerciseIndex = exercises.indexOf(it) },
            onRestart = {
                completedExercises = emptySet()
                exerciseIndex = 0
            },
            onComplete = onCompleted,
        )
    }
}

@Composable
private fun FaceMapTopBar(
    completed: Int,
    total: Int,
    onBack: () -> Unit,
) {
    Surface(color = Color(0xFF0C1B31), shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    "✨ Face Map",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                )
                Text(
                    "Артикуляциялық айна / Артикуляционное зеркало",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 10.sp,
                )
            }
            Surface(
                color = AppPurple,
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    "$completed/$total",
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FaceMapExercisePanel(
    exercise: FaceMapExercise,
    evaluationProgress: Float,
    openingScore: Float,
    roundingScore: Float,
    holdProgress: Float,
    status: String,
    completed: Set<FaceMapExercise>,
    allComplete: Boolean,
    onSelectExercise: (FaceMapExercise) -> Unit,
    onRestart: () -> Unit,
    onComplete: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF102139),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (allComplete) {
                Text("🏆", fontSize = 38.sp)
                Text(
                    "Барлық жаттығу орындалды!",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                )
                Text(
                    "Все упражнения выполнены!",
                    color = Color.White.copy(alpha = 0.72f),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onComplete ?: onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
                ) {
                    Text(
                        if (onComplete == null) {
                            "Қайталау / Повторить"
                        } else {
                            "Нәтижені көру / Показать результат"
                        },
                    )
                }
                if (onComplete != null) {
                    TextButton(onClick = onRestart) {
                        Text("Қайталау / Повторить", color = Color.White)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(exercise.emoji, fontSize = 38.sp)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            exercise.title,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                        )
                        Text(
                            exercise.instruction,
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 12.sp,
                        )
                        Text(
                            exercise.hint,
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FaceMetric(
                        label = "Ауыз / Открытие",
                        value = openingScore,
                        modifier = Modifier.weight(1f),
                    )
                    FaceMetric(
                        label = "Ерін / Округление",
                        value = roundingScore,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = evaluationProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = faceFeedbackColor(evaluationProgress),
                    trackColor = Color.White.copy(alpha = 0.12f),
                )
                Text(
                    text = if (holdProgress > 0f && holdProgress < 1f) {
                        "Қалыпты 2 секунд ұстаңыз / Удерживайте положение 2 секунды"
                    } else {
                        status
                    },
                    modifier = Modifier.padding(top = 7.dp),
                    color = if (evaluationProgress >= 0.82f) AppGreen else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                if (holdProgress > 0f && holdProgress < 1f) {
                    LinearProgressIndicator(
                        progress = holdProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 7.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50)),
                        color = AppGreen,
                        trackColor = Color.White.copy(alpha = 0.12f),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                FaceMapExercise.entries.forEach { item ->
                    val selected = item == exercise && !allComplete
                    val done = item in completed
                    Surface(
                        modifier = Modifier.size(if (selected) 44.dp else 38.dp),
                        onClick = { onSelectExercise(item) },
                        shape = CircleShape,
                        color = when {
                            done -> AppGreen
                            selected -> AppPurple
                            else -> Color.White.copy(alpha = 0.10f)
                        },
                        border = if (selected) {
                            androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                        } else {
                            null
                        },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (done) "✓" else item.emoji, fontSize = 18.sp, color = Color.White)
                        }
                    }
                }
            }
            Text(
                text = "Тренажёр • диагноз емес / не является диагностикой\nКадрлар сақталмайды • Кадры не сохраняются",
                modifier = Modifier.padding(top = 10.dp),
                color = Color.White.copy(alpha = 0.38f),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FaceMetric(
    label: String,
    value: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, color = Color.White.copy(alpha = 0.62f), fontSize = 9.sp)
            Text(
                "${(value * 100).toInt().coerceIn(0, 100)}%",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        LinearProgressIndicator(
            progress = value,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50)),
            color = faceFeedbackColor(value),
            trackColor = Color.White.copy(alpha = 0.10f),
        )
    }
}

@Composable
private fun FaceCameraPreview(
    onFrame: (FaceMapFrame) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize(),
    )

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val analyzer = FaceMapAnalyzer(onFrame)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var cameraProvider: ProcessCameraProvider? = null
        var disposed = false

        providerFuture.addListener(
            {
                if (disposed) return@addListener
                try {
                    cameraProvider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(cameraExecutor, analyzer) }

                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analysis,
                    )
                } catch (_: Exception) {
                    onError("Камераны іске қосу мүмкін болмады / Не удалось запустить камеру")
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            disposed = true
            cameraProvider?.unbindAll()
            analyzer.close()
            cameraExecutor.shutdown()
        }
    }
}

@Composable
private fun FaceContourOverlay(
    frame: FaceMapFrame,
    exercise: FaceMapExercise,
    evaluationProgress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val feedbackColor = faceFeedbackColor(evaluationProgress)
        drawRoundRect(
            color = if (frame.faceFound) feedbackColor.copy(alpha = 0.88f) else Color(0xFF74E7FF),
            topLeft = Offset(size.width * 0.12f, size.height * 0.08f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.76f, size.height * 0.80f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(48f, 48f),
            style = Stroke(width = if (evaluationProgress >= 0.82f) 6f else 3f),
        )

        drawTargetLipGuide(frame, exercise)
        frame.contours.forEach { contour ->
            drawContour(
                contour = contour,
                lipColor = feedbackColor,
                sourceAspectRatio = frame.sourceAspectRatio,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawContour(
    contour: FaceContourPath,
    lipColor: Color,
    sourceAspectRatio: Float,
) {
    if (contour.points.size < 2) return
    val isLip = contour.type in LIP_CONTOURS
    val color = when {
        isLip -> lipColor
        contour.type == FaceContour.FACE -> Color(0xFF7FE9FF)
        else -> Color.White.copy(alpha = 0.86f)
    }
    val path = Path().apply {
        contour.points.forEachIndexed { index, point ->
            val mapped = mapFillCenterPoint(point.x, point.y, sourceAspectRatio)
            val x = mapped.x
            val y = mapped.y
            if (index == 0) moveTo(x, y) else lineTo(x, y)
        }
        if (contour.type == FaceContour.FACE || isLip) close()
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = if (isLip) 5f else 2.5f,
            cap = StrokeCap.Round,
        ),
    )
    contour.points.forEachIndexed { index, point ->
        if (isLip || index % 2 == 0) {
            val mapped = mapFillCenterPoint(point.x, point.y, sourceAspectRatio)
            drawCircle(
                color = color,
                radius = if (isLip) 3.5f else 2.2f,
                center = mapped,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTargetLipGuide(
    frame: FaceMapFrame,
    exercise: FaceMapExercise,
) {
    if (!frame.faceFound || exercise == FaceMapExercise.CENTER) return
    val face = frame.contours.firstOrNull { it.type == FaceContour.FACE } ?: return
    val facePoints = face.points.map {
        mapFillCenterPoint(it.x, it.y, frame.sourceAspectRatio)
    }
    if (facePoints.size < 2) return

    val faceLeft = facePoints.minOf { it.x }
    val faceRight = facePoints.maxOf { it.x }
    val faceTop = facePoints.minOf { it.y }
    val faceBottom = facePoints.maxOf { it.y }
    val faceWidth = faceRight - faceLeft
    val lipPoints = frame.contours
        .filter { it.type in LIP_CONTOURS }
        .flatMap { it.points }
        .map { mapFillCenterPoint(it.x, it.y, frame.sourceAspectRatio) }
    val centerX = lipPoints.takeIf { it.isNotEmpty() }?.map { it.x }?.average()?.toFloat()
        ?: (faceLeft + faceRight) / 2f
    val centerY = lipPoints.takeIf { it.isNotEmpty() }?.map { it.y }?.average()?.toFloat()
        ?: faceTop + (faceBottom - faceTop) * 0.70f
    val (widthFactor, heightFactor) = when (exercise) {
        FaceMapExercise.OPEN_MOUTH -> 0.34f to 0.72f
        FaceMapExercise.ROUND_LIPS -> 0.27f to 0.78f
        FaceMapExercise.NARROW_LIPS -> 0.22f to 0.58f
        FaceMapExercise.SMILE -> 0.46f to 0.22f
        FaceMapExercise.CENTER -> return
    }
    val guideWidth = faceWidth * widthFactor
    val guideHeight = guideWidth * heightFactor
    val topLeft = Offset(centerX - guideWidth / 2f, centerY - guideHeight / 2f)
    val guideSize = androidx.compose.ui.geometry.Size(guideWidth, guideHeight)

    drawOval(
        color = Color.White.copy(alpha = 0.08f),
        topLeft = topLeft,
        size = guideSize,
    )
    drawOval(
        color = Color.White.copy(alpha = 0.68f),
        topLeft = topLeft,
        size = guideSize,
        style = Stroke(
            width = 5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
        ),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.mapFillCenterPoint(
    normalizedX: Float,
    normalizedY: Float,
    sourceAspectRatio: Float,
): Offset {
    val safeSourceAspect = sourceAspectRatio.coerceAtLeast(0.1f)
    val destinationAspect = size.width / size.height.coerceAtLeast(1f)
    return if (safeSourceAspect > destinationAspect) {
        val scaledWidth = size.height * safeSourceAspect
        val cropX = (scaledWidth - size.width) / 2f
        Offset(normalizedX * scaledWidth - cropX, normalizedY * size.height)
    } else {
        val scaledHeight = size.width / safeSourceAspect
        val cropY = (scaledHeight - size.height) / 2f
        Offset(normalizedX * size.width, normalizedY * scaledHeight - cropY)
    }
}

@Composable
private fun SuccessGlow(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(116.dp),
        shape = CircleShape,
        color = AppGreen.copy(alpha = 0.92f),
        shadowElevation = 18.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("✓", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text("Керемет!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private val LIP_CONTOURS = setOf(
    FaceContour.UPPER_LIP_TOP,
    FaceContour.UPPER_LIP_BOTTOM,
    FaceContour.LOWER_LIP_TOP,
    FaceContour.LOWER_LIP_BOTTOM,
)

private fun faceFeedbackColor(progress: Float): Color = when {
    progress >= 0.82f -> AppGreen
    progress >= 0.45f -> AppOrange
    else -> Color(0xFFFF5D6C)
}

private const val HOLD_STEPS = 20
private const val HOLD_STEP_MS = 100L
