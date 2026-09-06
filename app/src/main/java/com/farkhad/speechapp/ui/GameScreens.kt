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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.farkhad.speechapp.audio.*
import com.farkhad.speechapp.data.FirebaseRepository
import com.farkhad.speechapp.data.ProgressRepository
import com.farkhad.speechapp.model.*
import com.farkhad.speechapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun GameSessionScreen(
    activity: GameActivity,
    progress: ProgressRepository,
    audio: SpeechAudio,
    repository: FirebaseRepository?,
    onBack: () -> Unit,
    onSaveResult: (Int) -> Unit,
    onNextActivity: () -> Unit,
    hasNextActivity: Boolean,
) {
    var roundIndex by rememberSaveable(activity.id) { mutableStateOf(0) }
    var earnedPoints by rememberSaveable(activity.id) { mutableStateOf(0) }
    var roundFinished by rememberSaveable(activity.id, roundIndex) { mutableStateOf(false) }
    var resultScore by rememberSaveable(activity.id) { mutableStateOf<Int?>(null) }

    if (resultScore != null) {
        GameResultScreen(
            activity = activity,
            score = resultScore!!,
            onRepeat = {
                roundIndex = 0
                earnedPoints = 0
                roundFinished = false
                resultScore = null
            },
            onNext = onNextActivity,
            hasNext = hasNextActivity,
            onExit = onBack,
        )
        return
    }

    val round = activity.rounds[roundIndex]
    val roundProgress = (roundIndex + 1).toFloat() / activity.rounds.size
    val level = remember(activity.id) { Curriculum.levelForActivity(activity.id) }

    LaunchedEffect(round.id, audio.ready) {
        if (!audio.ready) return@LaunchedEffect
        delay(600)
        audio.speak(round.spokenModel(), slower = round is VoiceRound || round is TapCountRound)
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        NetworkImageBackground(url = level.imageUrl, alpha = 0.15f)
        KazakhPatternBackground(
            modifier = Modifier.fillMaxSize(),
            color = AppBlue.copy(alpha = 0.05f),
            strokeWidth = 3f
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            GameHeader(
                title = activity.title,
                emoji = activity.emoji,
                onBack = onBack,
            )
            
            LinearProgressIndicator(
                progress = roundProgress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .padding(horizontal = 24.dp)
                    .clip(CircleShape)
                    .shadow(2.dp, CircleShape),
                color = AppGreen,
                trackColor = AppBlue.copy(alpha = 0.1f),
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = AppBlue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AppBlue.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = " 🇰🇿 Тапсырма ${roundIndex + 1}/${activity.rounds.size} ",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = AppText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                ListenButton(
                    enabled = audio.ready,
                    onClick = { audio.speak(round.spokenModel(), slower = round is VoiceRound || round is TapCountRound) },
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 4.dp,
                    border = BorderStroke(2.dp, AppBlue.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = round.instruction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        color = AppText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                when (round) {
                    is ChoiceRound -> ChoiceExercise(
                        round = round,
                        audio = audio,
                        enabled = !roundFinished,
                        onEvaluated = { points ->
                            if (!roundFinished) {
                                earnedPoints += points
                                roundFinished = true
                            }
                        },
                    )
                    is TapCountRound -> TapCountExercise(
                        round = round,
                        audio = audio,
                        enabled = !roundFinished,
                        onEvaluated = { points ->
                            if (!roundFinished) {
                                earnedPoints += points
                                roundFinished = true
                            }
                        },
                    )
                    is ArrangeWordsRound -> ArrangeWordsExercise(
                        round = round,
                        audio = audio,
                        enabled = !roundFinished,
                        onEvaluated = { points ->
                            if (!roundFinished) {
                                earnedPoints += points
                                roundFinished = true
                            }
                        },
                    )
                    is StoryOrderRound -> StoryOrderExercise(
                        round = round,
                        audio = audio,
                        enabled = !roundFinished,
                        onEvaluated = { points ->
                            if (!roundFinished) {
                                earnedPoints += points
                                roundFinished = true
                            }
                        },
                    )
                    is VoiceRound -> VoiceExercise(
                        round = round,
                        progress = progress,
                        audio = audio,
                        repository = repository,
                        enabled = !roundFinished,
                        onEvaluated = { points ->
                            if (!roundFinished) {
                                earnedPoints += points
                                roundFinished = true
                            }
                        },
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            AnimatedVisibility(
                visible = roundFinished,
                modifier = Modifier.padding(24.dp)
            ) {
                Button(
                    onClick = {
                        if (roundIndex == activity.rounds.lastIndex) {
                            val score = (earnedPoints.toFloat() / activity.rounds.size).toInt().coerceIn(0, 100)
                            resultScore = score
                            onSaveResult(score)
                            audio.playCelebration()
                        } else {
                            roundIndex += 1
                            roundFinished = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
                ) {
                    Text(
                        text = if (roundIndex == activity.rounds.lastIndex) "Нәтижені көру ✨" else "Алға, келесі тапсырма →",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                    )
                }
            }
        }
    }
}

private fun ExerciseRound.spokenModel(): String = when (this) {
    is ChoiceRound -> speech
    is TapCountRound -> word.replace("-", "")
    is ArrangeWordsRound -> speech
    is StoryOrderRound -> speech
    is VoiceRound -> modelText
}

@Composable
private fun ChoiceExercise(
    round: ChoiceRound,
    audio: SpeechAudio,
    enabled: Boolean,
    onEvaluated: (Int) -> Unit,
) {
    var selected by remember(round.id) { mutableStateOf<Int?>(null) }
    val wasCorrect = selected == round.correctIndex

    QuestionCard {
        Text(
            text = round.display,
            fontSize = if (round.display.length <= 8) 48.sp else 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(modifier = Modifier.height(24.dp))

    round.options.forEachIndexed { index, option ->
        val isSelected = selected == index
        val isCorrectAnswer = index == round.correctIndex
        val container = when {
            selected != null && isCorrectAnswer -> Color(0xFFE8F5E9)
            isSelected -> Color(0xFFFFF3E0)
            else -> Color.White
        }
        val border = when {
            selected != null && isCorrectAnswer -> AppGreen
            isSelected -> AppOrange
            else -> Color(0xFFE0E0E0)
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = enabled && selected == null) {
                    selected = index
                    if (isCorrectAnswer) {
                        audio.playCorrect()
                        audio.speak("Керемет жауап!")
                        onEvaluated(100)
                    } else {
                        audio.playTryAgain()
                        audio.speak("Жақсы талпыныс. Дұрыс жауабын бірге көрейік.")
                        onEvaluated(60)
                    }
                },
            color = container,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, border),
            shadowElevation = 2.dp
        ) {
            Text(
                text = option,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                color = AppText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }

    FeedbackCard(
        visible = selected != null,
        success = wasCorrect,
        successText = "Дәл таптың! ${round.hint}",
        supportText = "Бірге үйрендік. ${round.hint}",
    )
}

@Composable
private fun TapCountExercise(
    round: TapCountRound,
    audio: SpeechAudio,
    enabled: Boolean,
    onEvaluated: (Int) -> Unit,
) {
    var taps by remember(round.id) { mutableStateOf(0) }
    var checked by remember(round.id) { mutableStateOf(false) }
    val correct = taps == round.correctTaps

    QuestionCard {
        Text(round.emoji, fontSize = 84.sp)
        Text(
            text = round.word,
            color = AppPurple,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = "Буын санынша барабанды бас",
            color = AppText.copy(alpha = 0.7f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(modifier = Modifier.height(32.dp))
    
    val infiniteTransition = rememberInfiniteTransition(label = "drum")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(CircleShape)
                .clickable(enabled = enabled && !checked && taps < 9) {
                    taps += 1
                    audio.playTryAgain()
                },
            shape = CircleShape,
            color = Color(0xFFFFE0B2),
            border = BorderStroke(4.dp, AppOrange),
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🥁", fontSize = 72.sp)
            }
        }
        Spacer(modifier = Modifier.width(32.dp))
        Text(
            text = "$taps",
            color = AppBlue,
            fontSize = 64.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(
            onClick = { taps = 0 },
            modifier = Modifier.weight(1f).height(56.dp),
            enabled = enabled && !checked && taps > 0,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(2.dp, AppBlue.copy(alpha = 0.5f))
        ) {
            Text("Қайта сана", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = {
                checked = true
                if (correct) {
                    audio.playCorrect()
                    onEvaluated(100)
                } else {
                    audio.playTryAgain()
                    onEvaluated(60)
                }
            },
            modifier = Modifier.weight(1f).height(56.dp),
            enabled = enabled && !checked && taps > 0,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text("Тексеру", fontWeight = FontWeight.ExtraBold)
        }
    }
    FeedbackCard(
        visible = checked,
        success = correct,
        successText = "Керемет ырғақ! ${round.hint}",
        supportText = "Бірге санайық: ${round.hint}",
    )
}

@Composable
private fun ArrangeWordsExercise(
    round: ArrangeWordsRound,
    audio: SpeechAudio,
    enabled: Boolean,
    onEvaluated: (Int) -> Unit,
) {
    val shuffledIndices = remember(round.id) {
        round.wordsInOrder.indices.shuffled(Random(round.id.hashCode()))
    }
    var selectedIndices by remember(round.id) { mutableStateOf(emptyList<Int>()) }
    var checked by remember(round.id) { mutableStateOf(false) }
    val selectedWords = selectedIndices.map { round.wordsInOrder[it] }
    val correct = selectedWords == round.wordsInOrder

    QuestionCard {
        Text("💬", fontSize = 64.sp)
        Text(
            text = if (selectedWords.isEmpty()) "Сөздерді ретімен таңда" else selectedWords.joinToString(" "),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            color = if (selectedWords.isEmpty()) AppText.copy(alpha = 0.4f) else AppText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp,
        )
    }
    Spacer(modifier = Modifier.height(24.dp))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        shuffledIndices.filter { it !in selectedIndices }.forEach { index ->
            WordOrderButton(
                text = round.wordsInOrder[index],
                enabled = enabled && !checked,
                onClick = { selectedIndices = selectedIndices + index },
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(
            onClick = { selectedIndices = emptyList() },
            modifier = Modifier.weight(1f).height(56.dp),
            enabled = enabled && !checked && selectedIndices.isNotEmpty(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Тазалау", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = {
                checked = true
                if (correct) {
                    audio.playCorrect()
                    audio.speak(round.speech)
                    onEvaluated(100)
                } else {
                    audio.playTryAgain()
                    onEvaluated(60)
                }
            },
            modifier = Modifier.weight(1f).height(56.dp),
            enabled = enabled && !checked && selectedIndices.size == round.wordsInOrder.size,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
        ) {
            Text("Тексеру", fontWeight = FontWeight.ExtraBold)
        }
    }
    FeedbackCard(
        visible = checked,
        success = correct,
        successText = "Өте жақсы сөйлем! ${round.speech}",
        supportText = "Дұрыс үлгі: ${round.speech}",
    )
}

@Composable
private fun StoryOrderExercise(
    round: StoryOrderRound,
    audio: SpeechAudio,
    enabled: Boolean,
    onEvaluated: (Int) -> Unit,
) {
    val shuffledIndices = remember(round.id) {
        round.cardsInOrder.indices.shuffled(Random(round.id.hashCode()))
    }
    var selectedIndices by remember(round.id) { mutableStateOf(emptyList<Int>()) }
    var checked by remember(round.id) { mutableStateOf(false) }
    val correct = selectedIndices == round.cardsInOrder.indices.toList()

    QuestionCard {
        Text(
            text = if (selectedIndices.isEmpty()) "1 → 2 → 3" else selectedIndices.mapIndexed { pos, idx ->
                "${pos + 1}. ${round.cardsInOrder[idx].emoji}"
            }.joinToString("   "),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppPurple,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(modifier = Modifier.height(20.dp))

    shuffledIndices.filter { it !in selectedIndices }.forEach { index ->
        val card = round.cardsInOrder[index]
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = enabled && !checked) {
                    selectedIndices = selectedIndices + index
                },
            color = Color.White,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(card.emoji, fontSize = 42.sp)
                Text(
                    text = card.text,
                    modifier = Modifier.padding(start = 20.dp),
                    color = AppText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(
            onClick = { selectedIndices = emptyList() },
            modifier = Modifier.weight(1f).height(56.dp),
            enabled = enabled && !checked && selectedIndices.isNotEmpty(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Қайта реттеу")
        }
        Button(
            onClick = {
                checked = true
                if (correct) {
                    audio.playCorrect()
                    audio.speak(round.speech)
                    onEvaluated(100)
                } else {
                    audio.playTryAgain()
                    onEvaluated(60)
                }
            },
            modifier = Modifier.weight(1f).height(56.dp),
            enabled = enabled && !checked && selectedIndices.size == round.cardsInOrder.size,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
        ) {
            Text("Тексеру")
        }
    }
    FeedbackCard(
        visible = checked,
        success = correct,
        successText = "Оқиғаның ретін таптың!",
        supportText = "Бірге реттейік: ${round.speech}",
    )
}

@Composable
private fun VoiceExercise(
    round: VoiceRound,
    progress: ProgressRepository,
    audio: SpeechAudio,
    repository: FirebaseRepository?,
    enabled: Boolean,
    onEvaluated: (Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var heardText by remember(round.id) { mutableStateOf("") }
    var listening by remember(round.id) { mutableStateOf(false) }
    var recognitionMessage by remember(round.id) { mutableStateOf("") }
    var evaluated by remember(round.id) { mutableStateOf(false) }
    var attempts by remember(round.id) { mutableStateOf(0) }
    var wordResults by remember(round.id) { mutableStateOf<List<WordResult>?>(null) }
    var showRestDialog by remember(round.id) { mutableStateOf(false) }
    var rmsLevel by remember { mutableStateOf(0f) }

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

    val startRecognition = {
        if (recognizer != null && hasPermission) {
            heardText = ""
            recognitionMessage = ""
            listening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "kk-KZ")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // Ensure online mode for high accuracy in Kazakh
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                // Increase silence timeouts
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            }
            recognizer.startListening(intent)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (granted) startRecognition()
        else recognitionMessage = "Микрофонға рұқсат берілмеді."
    }

    DisposableEffect(recognizer, round.id) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                rmsLevel = 0f
                recognitionMessage = "Тыңдап тұрмын…"
            }
            override fun onBeginningOfSpeech() { recognitionMessage = "Айта бер…" }
            override fun onRmsChanged(rmsdB: Float) {
                rmsLevel = rmsdB
            }
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                listening = false
                rmsLevel = 0f
                recognitionMessage = "Тексеріп жатырмын…"
            }
            override fun onError(error: Int) {
                listening = false
                recognitionMessage = "Дауыс анық естілмеді. Қайта айтып көр."
            }
            override fun onResults(results: Bundle?) {
                listening = false
                val transcription = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                heardText = transcription
                
                if (transcription.isNotBlank()) {
                    val evaluation = PronunciationEvaluator.evaluate(round.modelText, transcription)
                    wordResults = evaluation
                    attempts++
                    
                    if (PronunciationEvaluator.isOverallSuccess(evaluation)) {
                        recognitionMessage = "Керемет! Өте жақсы айттың."
                        audio.playCorrect()
                        evaluated = true
                        
                        // Mark target words as learned if the pronunciation was good
                        evaluation.forEach { res ->
                            if (res.status == WordStatus.CORRECT) {
                                // Find matching vocabulary word ID if exists
                                val vocabWord = Curriculum.levels.flatMap { it.vocabulary }
                                    .find { it.word.lowercase() == res.word.lowercase() }
                                if (vocabWord != null) {
                                    progress.markWordAsLearned(vocabWord.id)
                                    scope.launch {
                                        repository?.saveLearnedWord(vocabWord.id)
                                    }
                                }
                            }
                        }

                        onEvaluated(100)
                    } else {
                        recognitionMessage = "Жақсы талпыныс. Кейбір сөздерді түзетіп көрейік."
                        audio.playTryAgain()
                        audio.speak(round.modelText, slower = true)
                        
                        evaluation.filter { it.status != WordStatus.CORRECT }.forEach {
                            progress.logPronunciationFail(it.word)
                        }

                        if (attempts >= 3) {
                            showRestDialog = true
                        }
                    }
                } else {
                    recognitionMessage = "Сөз естілмеді. Қайта айтып көр."
                }
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose {
            recognizer?.cancel()
            recognizer?.destroy()
        }
    }

    QuestionCard {
        Text(round.picture, fontSize = 84.sp)
        
        if (wordResults != null) {
            Text(
                text = buildAnnotatedString {
                    wordResults!!.forEachIndexed { wordIdx, wordRes ->
                        wordRes.charResults.forEach { charRes ->
                            val color = when (charRes.status) {
                                CharStatus.CORRECT -> AppGreen
                                CharStatus.WRONG -> if (wordRes.status == WordStatus.OMITTED) Color.Gray else Color.Red
                            }
                            val decoration = when {
                                wordRes.status == WordStatus.OMITTED -> TextDecoration.LineThrough
                                charRes.status == CharStatus.WRONG -> TextDecoration.Underline
                                else -> TextDecoration.None
                            }
                            
                            withStyle(style = SpanStyle(color = color, textDecoration = decoration)) {
                                append(charRes.char)
                            }
                        }
                        if (wordIdx < wordResults!!.size - 1) append(" ")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                fontSize = if (round.modelText.length < 20) 32.sp else 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
            )
        } else {
            Text(
                text = round.modelText,
                modifier = Modifier.fillMaxWidth(),
                color = AppGreen,
                fontSize = if (round.modelText.length < 20) 32.sp else 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = { audio.speak(round.modelText, slower = true) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = audio.ready,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
    ) {
        Text("🔊  Үлгіні тыңдау", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
    Spacer(modifier = Modifier.height(12.dp))
    
    // Pulsing Mic for Child Feedback
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(80.dp)) {
        val scale by animateFloatAsState(
            targetValue = if (listening) (1f + (rmsLevel.coerceIn(0f, 10f) / 15f)) else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "micScale"
        )
        
        Surface(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(CircleShape)
                .clickable(
                    enabled = enabled && !evaluated && recognitionAvailable && !listening,
                    onClick = { if (hasPermission) startRecognition() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                ),
            color = if (listening) AppGreen else AppPurple,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(if (listening) "🎤" else "🎙️", fontSize = 32.sp)
            }
        }
    }

    Text(
        text = if (listening) "Сөйле…" else "Микрофонды бас та, айтып көр (${attempts}/3)",
        modifier = Modifier.fillMaxWidth(),
        color = if (listening) AppGreen else AppPurple,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )

    if (recognitionMessage.isNotBlank()) {
        Text(
            text = recognitionMessage,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            color = if (evaluated) AppGreen else AppText.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }

    if (!evaluated) {
        TextButton(
            onClick = {
                evaluated = true
                audio.playCorrect()
                onEvaluated(85)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            enabled = enabled,
        ) {
            Text("👪  Ересекпен бірге айттым", color = AppText.copy(alpha = 0.6f))
        }
    }

    FeedbackCard(
        visible = evaluated,
        success = true,
        successText = "Жарайсың! Сен нағыз батырсың! 🐎",
        supportText = "",
    )
    
    if (showRestDialog) {
        AlertDialog(
            onDismissRequest = { showRestDialog = false },
            title = { Text("Кішкене демалайық па?") },
            text = { Text("Бұл сөз сәл қиындау болды. Қазірше өткізіп жіберіп, кейін қайта көруге болады.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRestDialog = false
                        evaluated = true
                        onEvaluated(60) // Partial points for trying
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppOrange)
                ) {
                    Text("Өткізіп жіберу")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestDialog = false }) {
                    Text("Тағы көремін")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        color = AppBlue.copy(alpha = 0.08f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppBlue.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🚀 Күнделікті мақсат", color = AppBlue, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(
                text = round.childEncouragement,
                modifier = Modifier.padding(top = 8.dp),
                color = AppText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun QuestionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, AppBlue.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun WordOrderButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = Color(0xFFF5F9FF),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, AppBlue.copy(alpha = 0.3f)),
        shadowElevation = 1.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            color = AppBlue,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FeedbackCard(
    visible: Boolean,
    success: Boolean,
    successText: String,
    supportText: String,
) {
    AnimatedVisibility(visible = visible) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            color = if (success) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 2.dp
        ) {
            Text(
                text = if (success) "🌟  $successText" else "💛  $supportText",
                modifier = Modifier.padding(20.dp),
                color = AppText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            )
        }
    }
}

@Composable
private fun ListenButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AppBlue),
        border = BorderStroke(2.dp, AppBlue),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(if (enabled) "🔊 Тыңдау" else "Дайындалуда…", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    }
}

@Composable
private fun GameHeader(title: String, emoji: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onClick = onBack)
        Text(
            text = "$emoji  $title",
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            color = AppText,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun GameResultScreen(
    activity: GameActivity,
    score: Int,
    onRepeat: () -> Unit,
    onNext: () -> Unit,
    hasNext: Boolean,
    onExit: () -> Unit,
) {
    val stars = when {
        score >= 85 -> 3
        score >= 60 -> 2
        else -> 1
    }
    val message = when (stars) {
        3 -> "Керемет жетістік! Сен нағыз білім шыңын бағындырдың."
        2 -> "Өте жақсы нәтиже! Қайталаған сайын сөздерің анықтала түседі."
        else -> "Жарайсың! Байқап көрудің өзі — үлкен қадам. Алға!"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFAF0), Color(0xFFFFF1CC)),
                )
            )
    ) {
        KazakhPatternBackground(
            modifier = Modifier.fillMaxSize(),
            color = AppGold.copy(alpha = 0.12f),
            strokeWidth = 4f
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier.size(160.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 12.dp,
                border = BorderStroke(6.dp, AppGold)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (stars == 3) "🏆" else "🌟", fontSize = 84.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = if (stars == 3) "КЕРЕМЕТ ЖЕҢІС!" else "ЖАРАЙСЫҢ!",
                style = MaterialTheme.typography.headlineLarge,
                color = AppText,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            
            Text(
                text = activity.title,
                modifier = Modifier.padding(top = 8.dp),
                color = AppBlue,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            
            Row(
                modifier = Modifier.padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { i ->
                    Text(
                        text = if (i < stars) "⭐" else "☆",
                        fontSize = 56.sp,
                        color = AppGold
                    )
                }
            }

            Surface(
                color = AppGreen.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, AppGreen.copy(alpha = 0.3f))
            ) {
                Text(
                    text = "Дәлдік: $score%",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = AppGreen,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            Text(
                text = message,
                modifier = Modifier.padding(top = 28.dp, bottom = 40.dp),
                color = AppText.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = if (hasNext) onNext else onExit,
                modifier = Modifier.fillMaxWidth().height(68.dp).shadow(12.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
            ) {
                Text(
                    text = if (hasNext) "Келесі кезеңге →" else "Картаға оралу",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onRepeat,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(2.dp, AppBlue)
            ) {
                Text("↻ Қайта ойнау", color = AppBlue, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            
            TextButton(
                onClick = onExit,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Бас мәзірге қайту", color = AppText.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            }
        }
    }
}
