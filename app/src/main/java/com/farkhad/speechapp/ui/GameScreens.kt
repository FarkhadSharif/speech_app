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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.farkhad.speechapp.audio.SpeechAudio
import com.farkhad.speechapp.model.ArrangeWordsRound
import com.farkhad.speechapp.model.ChoiceRound
import com.farkhad.speechapp.model.ExerciseRound
import com.farkhad.speechapp.model.GameActivity
import com.farkhad.speechapp.model.StoryOrderRound
import com.farkhad.speechapp.model.TapCountRound
import com.farkhad.speechapp.model.VoiceRound
import com.farkhad.speechapp.model.scoreSpokenPhrase
import com.farkhad.speechapp.ui.theme.AppBackground
import com.farkhad.speechapp.ui.theme.AppBlue
import com.farkhad.speechapp.ui.theme.AppGreen
import com.farkhad.speechapp.ui.theme.AppOrange
import com.farkhad.speechapp.ui.theme.AppPurple
import com.farkhad.speechapp.ui.theme.AppText
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.random.Random

@Composable
fun GameSessionScreen(
    activity: GameActivity,
    audio: SpeechAudio,
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

    LaunchedEffect(round.id, audio.ready) {
        if (!audio.ready) return@LaunchedEffect
        delay(450)
        audio.speak(round.spokenModel(), slower = round is VoiceRound || round is TapCountRound)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
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
                .height(7.dp),
            color = AppGreen,
            trackColor = Color(0xFFE7EEF3),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${roundIndex + 1}/${activity.rounds.size}",
                color = AppText.copy(alpha = 0.55f),
                fontWeight = FontWeight.Bold,
            )
            ListenButton(
                enabled = audio.ready,
                onClick = { audio.speak(round.spokenModel(), slower = round is VoiceRound || round is TapCountRound) },
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Text(
                text = round.instruction,
                modifier = Modifier.fillMaxWidth(),
                color = AppBlue,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                    audio = audio,
                    enabled = !roundFinished,
                    onEvaluated = { points ->
                        if (!roundFinished) {
                            earnedPoints += points
                            roundFinished = true
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        AnimatedVisibility(visible = roundFinished) {
            Button(
                onClick = {
                    if (roundIndex == activity.rounds.lastIndex) {
                        val score = (earnedPoints.toFloat() / activity.rounds.size).toInt().coerceIn(0, 100)
                        resultScore = score
                        onSaveResult(score)
                        audio.playCelebration()
                    } else {
                        roundIndex += 1
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
            ) {
                Text(
                    text = if (roundIndex == activity.rounds.lastIndex) "Нәтижені көру ⭐" else "Келесі тапсырма →",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
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
            fontSize = if (round.display.length <= 8) 40.sp else 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Spacer(modifier = Modifier.height(18.dp))

    round.options.forEachIndexed { index, option ->
        val isSelected = selected == index
        val isCorrectAnswer = index == round.correctIndex
        val container = when {
            selected != null && isCorrectAnswer -> Color(0xFFDDF8D8)
            isSelected -> Color(0xFFFFE8D1)
            else -> Color.White
        }
        val border = when {
            selected != null && isCorrectAnswer -> AppGreen
            isSelected -> AppOrange
            else -> Color(0xFFDDE5EC)
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = enabled && selected == null) {
                    selected = index
                    if (isCorrectAnswer) {
                        audio.playCorrect()
                        audio.speak("Жарайсың!")
                        onEvaluated(100)
                    } else {
                        audio.playTryAgain()
                        audio.speak("Жақсы әрекет. Дұрыс жауабын бірге қарайық.")
                        onEvaluated(55)
                    }
                },
            color = container,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, border),
        ) {
            Text(
                text = option,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
                color = AppText,
                fontSize = 18.sp,
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
        Text(round.emoji, fontSize = 72.sp)
        Text(
            text = round.word,
            color = AppPurple,
            fontSize = 31.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = "Буын санынша барабанды бас",
            color = AppText.copy(alpha = 0.66f),
            textAlign = TextAlign.Center,
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .size(126.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled && !checked && taps < 6) {
                    taps += 1
                    audio.playTryAgain()
                },
            shape = CircleShape,
            color = Color(0xFFFFD99E),
            border = BorderStroke(4.dp, AppOrange),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🥁", fontSize = 62.sp)
            }
        }
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = "$taps",
            color = AppBlue,
            fontSize = 52.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
    Spacer(modifier = Modifier.height(18.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = { taps = 0 },
            modifier = Modifier.weight(1f),
            enabled = enabled && !checked && taps > 0,
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Қайта сана")
        }
        Button(
            onClick = {
                checked = true
                if (correct) {
                    audio.playCorrect()
                    onEvaluated(100)
                } else {
                    audio.playTryAgain()
                    onEvaluated(55)
                }
            },
            modifier = Modifier.weight(1f),
            enabled = enabled && !checked && taps > 0,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
        ) {
            Text("Тексеру")
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
        Text("💬", fontSize = 58.sp)
        Text(
            text = if (selectedWords.isEmpty()) "Сөздерді ретімен таңда" else selectedWords.joinToString(" "),
            modifier = Modifier.fillMaxWidth(),
            color = if (selectedWords.isEmpty()) AppText.copy(alpha = 0.42f) else AppText,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 29.sp,
        )
    }
    Spacer(modifier = Modifier.height(16.dp))

    shuffledIndices.filter { it !in selectedIndices }.forEach { index ->
        WordOrderButton(
            text = round.wordsInOrder[index],
            enabled = enabled && !checked,
            onClick = { selectedIndices = selectedIndices + index },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = { selectedIndices = emptyList() },
            modifier = Modifier.weight(1f),
            enabled = enabled && !checked && selectedIndices.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Тазалау")
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
                    onEvaluated(55)
                }
            },
            modifier = Modifier.weight(1f),
            enabled = enabled && !checked && selectedIndices.size == round.wordsInOrder.size,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
        ) {
            Text("Тексеру")
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
            text = if (selectedIndices.isEmpty()) "1 → 2 → 3" else selectedIndices.mapIndexed { position, index ->
                "${position + 1}. ${round.cardsInOrder[index].emoji}"
            }.joinToString("   "),
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AppPurple,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(modifier = Modifier.height(14.dp))

    shuffledIndices.filter { it !in selectedIndices }.forEach { index ->
        val card = round.cardsInOrder[index]
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(enabled = enabled && !checked) {
                    selectedIndices = selectedIndices + index
                },
            color = Color.White,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFFDCE5EC)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(card.emoji, fontSize = 38.sp)
                Text(
                    text = card.text,
                    modifier = Modifier.padding(start = 16.dp),
                    color = AppText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = { selectedIndices = emptyList() },
            modifier = Modifier.weight(1f),
            enabled = enabled && !checked && selectedIndices.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
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
                    onEvaluated(55)
                }
            },
            modifier = Modifier.weight(1f),
            enabled = enabled && !checked && selectedIndices.size == round.cardsInOrder.size,
            shape = RoundedCornerShape(14.dp),
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
    audio: SpeechAudio,
    enabled: Boolean,
    onEvaluated: (Int) -> Unit,
) {
    val context = LocalContext.current
    var heardText by remember(round.id) { mutableStateOf("") }
    var listening by remember(round.id) { mutableStateOf(false) }
    var recognitionMessage by remember(round.id) { mutableStateOf("") }
    var evaluated by remember(round.id) { mutableStateOf(false) }
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
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "kk-KZ")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "kk-KZ")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            recognizer.startListening(intent)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (granted) startRecognition()
        else recognitionMessage = "Микрофонға рұқсат берілмеді. Ересекпен бірге тексеруге болады."
    }

    DisposableEffect(recognizer, round.id) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                recognitionMessage = "Тыңдап тұрмын…"
            }

            override fun onBeginningOfSpeech() {
                recognitionMessage = "Айта бер…"
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                listening = false
                recognitionMessage = "Тексеріп жатырмын…"
            }

            override fun onError(error: Int) {
                listening = false
                recognitionMessage = "Дауыс анық естілмеді. Қайта айтып көр немесе ересекпен бірге тексер."
            }

            override fun onResults(results: Bundle?) {
                listening = false
                heardText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                recognitionMessage = if (heardText.isBlank()) "Сөз естілмеді." else "Құрылғы естігені: «$heardText»"
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
        Text(round.picture, fontSize = if (round.picture.length <= 4) 76.sp else 48.sp)
        Text(
            text = round.modelText,
            modifier = Modifier.fillMaxWidth(),
            color = AppGreen,
            fontSize = if (round.modelText.length < 24) 28.sp else 21.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp,
        )
    }
    Spacer(modifier = Modifier.height(18.dp))
    Button(
        onClick = { audio.speak(round.modelText, slower = true) },
        modifier = Modifier.fillMaxWidth(),
        enabled = audio.ready,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
    ) {
        Text("🔊  Үлгіні қайта тыңдау", fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedButton(
        onClick = {
            if (hasPermission) startRecognition() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled && !evaluated && recognitionAvailable && !listening,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, AppPurple),
    ) {
        Text(if (listening) "🎙️ Тыңдап тұрмын…" else "🎙️ Менің дауысымды тыңда", color = AppPurple)
    }

    if (!recognitionAvailable) {
        Text(
            text = "Бұл құрылғыда дауысты тану жоқ. Сөзді ересекпен бірге айтып көр.",
            modifier = Modifier.padding(top = 10.dp),
            color = AppText.copy(alpha = 0.65f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
    if (recognitionMessage.isNotBlank()) {
        Text(
            text = recognitionMessage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            color = AppText.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
        )
    }

    if (heardText.isNotBlank() && !evaluated) {
        val speechScore = scoreSpokenPhrase(round.modelText, heardText)
        Button(
            onClick = {
                evaluated = true
                audio.playCorrect()
                onEvaluated(if (speechScore >= 60) 100 else 70)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
        ) {
            Text("Жалғастыру • ұқсастық $speechScore%")
        }
    }

    if (!evaluated) {
        OutlinedButton(
            onClick = {
                evaluated = true
                audio.playCorrect()
                onEvaluated(85)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("👪  Ересекпен бірге айтып көрдім")
        }
    }

    FeedbackCard(
        visible = evaluated,
        success = true,
        successText = "Айтып көруге батылдық жасадың — жарайсың!",
        supportText = "",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        color = Color(0xFFF3EDFF),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Ересекке кеңес", color = AppPurple, fontWeight = FontWeight.ExtraBold)
            Text(
                text = round.parentPrompt,
                modifier = Modifier.padding(top = 5.dp),
                color = AppText.copy(alpha = 0.72f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
    }
}

@Composable
private fun QuestionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, Color(0xFFE8EEF2)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun WordOrderButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = Color(0xFFF2F8FF),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppBlue.copy(alpha = 0.35f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            color = AppBlue,
            fontSize = 17.sp,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            color = if (success) Color(0xFFDFF7DB) else Color(0xFFFFEEDB),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(
                text = if (success) "🌟  $successText" else "💛  $supportText",
                modifier = Modifier.padding(16.dp),
                color = AppText,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 21.sp,
            )
        }
    }
}

@Composable
private fun ListenButton(enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        border = BorderStroke(1.dp, AppBlue.copy(alpha = 0.45f)),
    ) {
        Text(if (enabled) "🔊 Тыңдау" else "Дауыс дайындалуда…", color = AppBlue, fontSize = 13.sp)
    }
}

@Composable
private fun GameHeader(title: String, emoji: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackButton(onClick = onBack)
        Text(
            text = "$emoji  $title",
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            color = AppText,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.size(42.dp))
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
        3 -> "Тамаша жұмыс! Сен мұқият тыңдап, батыл жауап бердің."
        2 -> "Өте жақсы! Қайталаған сайын сөздер анық әрі сенімді болады."
        else -> "Жарайсың! Ең маңыздысы — байқап көру және бірге сөйлесу."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE8F8FF), Color(0xFFFFF7E9)),
                ),
            )
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("${"⭐".repeat(stars)}${"☆".repeat(3 - stars)}", fontSize = 52.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ойын аяқталды!",
            style = MaterialTheme.typography.headlineMedium,
            color = AppBlue,
            textAlign = TextAlign.Center,
        )
        Text(
            text = activity.title,
            modifier = Modifier.padding(top = 8.dp),
            color = AppText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$score%",
            modifier = Modifier.padding(top = 16.dp),
            color = AppGreen,
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 12.dp, bottom = 26.dp),
            color = AppText.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
        )
        Button(
            onClick = if (hasNext) onNext else onExit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
        ) {
            Text(if (hasNext) "Келесі ойын →" else "Картаға қайту", fontWeight = FontWeight.ExtraBold)
        }
        OutlinedButton(
            onClick = onRepeat,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("↻ Қайта ойнау")
        }
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.dp, Color.Transparent),
        ) {
            Text("Деңгейге қайту", color = AppText.copy(alpha = 0.62f))
        }
    }
}
