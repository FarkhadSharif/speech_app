@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.farkhad.speechapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.farkhad.speechapp.assessment.AssessmentAttempt
import com.farkhad.speechapp.assessment.AssessmentPrompt
import com.farkhad.speechapp.assessment.SpeechAssessmentEngine
import com.farkhad.speechapp.assessment.SpeechProfile
import com.farkhad.speechapp.assessment.SoundMapEngine
import com.farkhad.speechapp.audio.SpeechAudio
import com.farkhad.speechapp.data.ExerciseAnalysisType
import com.farkhad.speechapp.data.ExerciseSessionReport
import com.farkhad.speechapp.data.ExerciseSessionSource
import com.farkhad.speechapp.data.ExerciseStepReport
import com.farkhad.speechapp.data.ProgressRepository
import com.farkhad.speechapp.ui.theme.AppBackground
import com.farkhad.speechapp.ui.theme.AppBlue
import com.farkhad.speechapp.ui.theme.AppGreen
import com.farkhad.speechapp.ui.theme.AppOrange
import com.farkhad.speechapp.ui.theme.AppPurple
import com.farkhad.speechapp.ui.theme.AppNavy
import com.farkhad.speechapp.ui.theme.AppOutline
import com.farkhad.speechapp.ui.theme.AppRed
import com.farkhad.speechapp.ui.theme.AppText

private enum class AssessmentStage {
    INTRO,
    SPEECH,
    FACE_MAP,
    RESULT,
}

@Composable
fun ExpressAssessmentScreen(
    audio: SpeechAudio,
    progress: ProgressRepository,
    onBack: () -> Unit,
    onReport: (ExerciseSessionReport) -> Unit = {},
) {
    var stageName by rememberSaveable { mutableStateOf(AssessmentStage.INTRO.name) }
    var assessmentStartedAt by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var reportSaved by rememberSaveable { mutableStateOf(false) }
    val attempts = remember { mutableStateListOf<AssessmentAttempt>() }
    val stage = AssessmentStage.valueOf(stageName)

    when (stage) {
        AssessmentStage.INTRO -> AssessmentIntro(
            onStart = {
                assessmentStartedAt = System.currentTimeMillis()
                reportSaved = false
                stageName = AssessmentStage.SPEECH.name
            },
            onBack = onBack,
        )

        AssessmentStage.SPEECH -> SpeechAssessmentStep(
            audio = audio,
            onAttemptAccepted = { attempt ->
                attempts.removeAll { it.prompt.id == attempt.prompt.id }
                attempts += attempt
            },
            onComplete = { stageName = AssessmentStage.FACE_MAP.name },
            onBack = { stageName = AssessmentStage.INTRO.name },
        )

        AssessmentStage.FACE_MAP -> FaceMapScreen(
            audio = audio,
            onBack = { stageName = AssessmentStage.SPEECH.name },
            onCompleted = {
                progress.saveAssessedSoundScores(SoundMapEngine.scoresFrom(attempts))
                stageName = AssessmentStage.RESULT.name
            },
            onAnalysisCompleted = { faceSteps, _ ->
                if (!reportSaved) {
                    reportSaved = true
                    val completedAt = System.currentTimeMillis()
                    val speechSteps = attempts.map { attempt ->
                        ExerciseStepReport(
                            stepId = "assessment_${attempt.prompt.id}",
                            title = attempt.prompt.text,
                            score = attempt.score,
                            attempts = 1,
                            analysisType = ExerciseAnalysisType.AUDIO,
                            targetText = attempt.prompt.text,
                            recognizedText = attempt.recognizedText,
                            feedback = if (attempt.score >= 85) {
                                "Сөз анық танылды"
                            } else {
                                "Қайталап жаттықтыру ұсынылады"
                            },
                        )
                    }
                    val allSteps = speechSteps + faceSteps
                    onReport(
                        ExerciseSessionReport(
                            sessionId = "${completedAt}_speech_check",
                            activityId = "speech_check",
                            activityTitle = "Сөйлеуді тексеру",
                            levelId = 0,
                            levelTitle = "Экспресс-бағалау",
                            completedAt = completedAt,
                            durationSeconds = ((completedAt - assessmentStartedAt) / 1000L)
                                .coerceAtLeast(1L),
                            score = allSteps.map { it.score }.average()
                                .takeIf { !it.isNaN() }?.toInt() ?: 0,
                            attemptNumber = progress.exerciseSessionReports.count {
                                it.activityId == "speech_check"
                            } + 1,
                            source = ExerciseSessionSource.ASSESSMENT,
                            steps = allSteps,
                        ),
                    )
                }
            },
        )

        AssessmentStage.RESULT -> AssessmentResult(
            profile = SpeechAssessmentEngine.buildProfile(attempts),
            attempts = attempts,
            onFinish = onBack,
            onRepeat = {
                attempts.clear()
                reportSaved = false
                stageName = AssessmentStage.INTRO.name
            },
        )
    }
}

@Composable
private fun AssessmentIntro(
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text(
                "Экспресс-бағалау",
                modifier = Modifier.padding(start = 14.dp),
                color = AppText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Text(
            "01  /  БАҒАЛАУ",
            color = AppRed,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
        )
        Text(
            "Сөйлеуді\nтексеру",
            modifier = Modifier.padding(top = 9.dp),
            color = AppText,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 41.sp,
        )
        Text(
            "4 сөйлеу тапсырмасы  ·  Face Map  ·  шамамен 60 секунд",
            modifier = Modifier.padding(top = 12.dp),
            color = AppText.copy(alpha = 0.58f),
            fontSize = 13.sp,
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            color = Color.Transparent,
            shape = RoundedCornerShape(0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppOutline),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                AssessmentIntroRow("1", "Сөздерді тыңдап, қайталаңыз / Повторите слова")
                AssessmentIntroRow("2", "Қысқа сөйлем айтыңыз / Произнесите фразу")
                AssessmentIntroRow("3", "Face Map жаттығуын орындаңыз / Пройдите Face Map")
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppNavy),
            shape = RoundedCornerShape(4.dp),
        ) {
            Text("Бастау / Начать", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        }
        Text(
            "Бұл медициналық диагноз емес / Это не медицинская диагностика",
            modifier = Modifier.padding(top = 12.dp),
            color = AppText.copy(alpha = 0.48f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AssessmentIntroRow(number: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(number.padStart(2, '0'), color = AppRed, fontWeight = FontWeight.Black, fontSize = 11.sp)
        Text(
            text,
            modifier = Modifier.padding(start = 12.dp),
            color = AppText.copy(alpha = 0.76f),
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SpeechAssessmentStep(
    audio: SpeechAudio,
    onAttemptAccepted: (AssessmentAttempt) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val prompts = SpeechAssessmentEngine.prompts
    var promptIndex by rememberSaveable { mutableStateOf(0) }
    var attempt by remember { mutableStateOf<AssessmentAttempt?>(null) }
    var listening by remember { mutableStateOf(false) }
    var helperText by remember { mutableStateOf("Микрофонды басып, анық айтыңыз / Нажмите микрофон") }
    var partialText by remember { mutableStateOf("") }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val recognitionAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    val recognizer = remember(recognitionAvailable) {
        if (recognitionAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    val prompt = prompts[promptIndex]

    val startListening = {
        if (recognizer == null) {
            helperText = "Дауыс тану қолжетімсіз / Распознавание речи недоступно"
        } else {
            audio.stop()
            attempt = null
            partialText = ""
            helperText = "Тыңдап тұрмын… / Слушаю…"
            listening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "kk-KZ")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt.text)
            }
            try {
                recognizer.startListening(intent)
            } catch (_: Exception) {
                listening = false
                helperText = "Микрофон іске қосылмады / Не удалось включить микрофон"
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (granted) startListening()
        else helperText = "Микрофонға рұқсат қажет / Нужен доступ к микрофону"
    }

    DisposableEffect(recognizer, prompt.id) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                helperText = "Айта беріңіз… / Говорите…"
            }

            override fun onBeginningOfSpeech() {
                helperText = "Дауысыңыз естілді / Слышу вас"
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                listening = false
                helperText = "Нәтижені есептеп жатырмын… / Считаю результат…"
            }

            override fun onError(error: Int) {
                listening = false
                helperText = recognitionErrorText(error)
            }

            override fun onResults(results: Bundle?) {
                listening = false
                val recognized = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (recognized.isBlank()) {
                    helperText = "Сөз естілмеді. Қайта көріңіз / Слово не распознано"
                } else {
                    partialText = recognized
                    attempt = SpeechAssessmentEngine.evaluate(prompt, recognized)
                    helperText = "Нәтиже дайын / Результат готов"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialText = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        onDispose {
            recognizer?.cancel()
        }
    }

    DisposableEffect(recognizer) {
        onDispose { recognizer?.destroy() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
    ) {
        AssessmentHeader(
            title = "Дауыс / Голос",
            progressText = "${promptIndex + 1}/${prompts.size}",
            onBack = onBack,
        )
        LinearProgressIndicator(
            progress = (promptIndex + 1f) / prompts.size,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp),
            color = AppPurple,
            trackColor = AppBlue.copy(alpha = 0.12f),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(prompt.kindLabel, color = AppPurple, fontWeight = FontWeight.Bold)
                    Icon(
                        Icons.Outlined.RecordVoiceOver,
                        contentDescription = null,
                        tint = AppRed,
                        modifier = Modifier.padding(top = 14.dp).size(48.dp),
                    )
                    Text(
                        prompt.text,
                        modifier = Modifier.padding(top = 10.dp),
                        color = AppText,
                        fontSize = if (prompt.text.length > 15) 24.sp else 34.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedButton(
                        onClick = { audio.speak(prompt.text, slower = true) },
                        modifier = Modifier.padding(top = 18.dp),
                        shape = RoundedCornerShape(50),
                    ) {
                        Icon(Icons.Outlined.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Тыңдау / Послушать")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                onClick = {
                    if (!listening) {
                        if (hasPermission) startListening()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.size(92.dp),
                shape = CircleShape,
                color = if (listening) AppOrange else AppBlue,
                shadowElevation = 12.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(if (listening) 43.dp else 38.dp),
                    )
                }
            }
            Text(
                helperText,
                modifier = Modifier.padding(top = 14.dp),
                color = AppText.copy(alpha = 0.67f),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
            )

            if (partialText.isNotBlank()) {
                Text(
                    "«$partialText»",
                    modifier = Modifier.padding(top = 10.dp),
                    color = AppText,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }

            attempt?.let { result ->
                AssessmentScoreCard(result)
                Button(
                    onClick = {
                        onAttemptAccepted(result)
                        if (promptIndex == prompts.lastIndex) {
                            onComplete()
                        } else {
                            promptIndex += 1
                            attempt = null
                            partialText = ""
                            helperText = "Микрофонды басып, анық айтыңыз / Нажмите микрофон"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
                    shape = RoundedCornerShape(17.dp),
                ) {
                    Text(
                        if (promptIndex == prompts.lastIndex) {
                            "Face Map-қа өту / Перейти к Face Map"
                        } else {
                            "Келесі / Далее"
                        },
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AssessmentScoreCard(attempt: AssessmentAttempt) {
    val scoreColor = when {
        attempt.score >= 85 -> AppGreen
        attempt.score >= 60 -> AppOrange
        else -> Color(0xFFE65B65)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp),
        color = scoreColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = scoreColor) {
                Box(modifier = Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                    Text("${attempt.score}%", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    if (attempt.score >= 85) "Керемет! / Отлично!" else "Жақсы талпыныс / Хорошая попытка",
                    color = AppText,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    "Распознано: ${attempt.recognizedText}",
                    color = AppText.copy(alpha = 0.64f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun AssessmentResult(
    profile: SpeechProfile,
    attempts: List<AssessmentAttempt>,
    onFinish: () -> Unit,
    onRepeat: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
    ) {
        AssessmentHeader(title = "Сөйлеу профилі / Речевой профиль", onBack = onFinish)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = AppRed,
                modifier = Modifier.size(48.dp),
            )
            Text(
                profile.levelTitle,
                color = AppText,
                fontWeight = FontWeight.Black,
                fontSize = 23.sp,
                textAlign = TextAlign.Center,
            )
            Surface(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(132.dp),
                shape = CircleShape,
                color = AppPurple,
                shadowElevation = 12.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("${profile.overallScore}%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 34.sp)
                    Text("сәйкестік", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                }
            }

            ProfileSection(
                title = "Күшті жақтар / Сильные стороны",
                text = if (profile.strongSounds.isEmpty()) {
                    "Тапсырмалар орындалды / Задания выполнены"
                } else {
                    profile.strongSounds.joinToString("  ") { it.uppercase() }
                },
                color = AppGreen,
            )
            ProfileSection(
                title = "Назар аударатын дыбыстар / Звуки для тренировки",
                text = if (profile.focusSounds.isEmpty()) {
                    "Айқын қиындық табылған жоқ / Явных трудностей не найдено"
                } else {
                    profile.focusSounds.joinToString("  ") { it.uppercase() }
                },
                color = AppOrange,
            )
            ProfileSection(
                title = "Ұсыныс / Рекомендация",
                text = profile.recommendation,
                color = AppPurple,
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                color = AppBlue.copy(alpha = 0.09f),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Нәтижелер / Результаты", color = AppText, fontWeight = FontWeight.ExtraBold)
                    attempts.forEach { attempt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = AppRed,
                                    modifier = Modifier.size(17.dp),
                                )
                                Text(
                                    attempt.prompt.text,
                                    modifier = Modifier.padding(start = 7.dp),
                                    color = AppText,
                                    fontSize = 12.sp,
                                )
                            }
                            Text("${attempt.score}%", color = AppPurple, fontWeight = FontWeight.Black)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Face Map", color = AppText, fontSize = 12.sp)
                        Text("Орындалды / Готово", color = AppGreen, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }

            Text(
                "Баға распознанған мәтіннің үлгімен сәйкестігін көрсетеді. Медициналық қорытынды емес.\nОценка показывает совпадение распознанного текста с образцом и не является диагнозом.",
                modifier = Modifier.padding(top = 16.dp),
                color = AppText.copy(alpha = 0.48f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Дайын / Готово", fontWeight = FontWeight.ExtraBold)
            }
            TextButton(onClick = onRepeat) {
                Text("Қайта өту / Пройти ещё раз", color = AppPurple)
            }
        }
    }
}

@Composable
private fun ProfileSection(title: String, text: String, color: Color) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Text(title, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Text(text, modifier = Modifier.padding(top = 7.dp), color = AppText, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun AssessmentHeader(
    title: String,
    progressText: String? = null,
    onBack: () -> Unit,
) {
    Surface(color = Color.White, shadowElevation = 5.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack)
            Text(
                title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                color = AppText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
            )
            progressText?.let {
                Surface(color = AppPurple.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                    Text(
                        it,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        color = AppPurple,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

private fun recognitionErrorText(error: Int): String = when (error) {
    SpeechRecognizer.ERROR_NO_MATCH,
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
    -> "Сөз анық естілмеді. Қайта айтыңыз / Повторите ещё раз"

    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
    -> "Интернетті тексеріңіз / Проверьте интернет"

    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
        "Бір сәт күтіп, қайталаңыз / Подождите и повторите"

    else -> "Дауыс танылмады. Қайта көріңіз / Не удалось распознать речь"
}
