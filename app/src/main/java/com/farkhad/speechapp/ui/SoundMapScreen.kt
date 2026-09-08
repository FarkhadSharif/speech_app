@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.farkhad.speechapp.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farkhad.speechapp.assessment.SoundMapEngine
import com.farkhad.speechapp.assessment.SoundStatus
import com.farkhad.speechapp.audio.SpeechAudio
import com.farkhad.speechapp.data.ProgressRepository
import com.farkhad.speechapp.model.Curriculum
import com.farkhad.speechapp.model.VocabularyWord
import com.farkhad.speechapp.ui.theme.AppBackground
import com.farkhad.speechapp.ui.theme.AppBlue
import com.farkhad.speechapp.ui.theme.AppGreen
import com.farkhad.speechapp.ui.theme.AppOrange
import com.farkhad.speechapp.ui.theme.AppPurple
import com.farkhad.speechapp.ui.theme.AppText

private data class SoundWord(
    val word: VocabularyWord,
    val levelId: Int,
)

@Composable
fun SoundMapScreen(
    progress: ProgressRepository,
    progressRevision: Int,
    audio: SpeechAudio,
    onBack: () -> Unit,
    onStartAssessment: () -> Unit,
    onOpenLevel: (Int) -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val observeRevision = progressRevision
    val scores = progress.assessedSoundScores
    var selectedSound by remember { mutableStateOf(SoundMapEngine.trackedSounds.first()) }
    val matchingWords = remember(selectedSound) {
        Curriculum.levels.flatMap { level ->
            level.vocabulary
                .filter { selectedSound in it.word.lowercase() }
                .map { SoundWord(it, level.id) }
        }.distinctBy { it.word.word.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
    ) {
        Surface(color = Color.White, shadowElevation = 5.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BackButton(onClick = onBack)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        "Дыбыстар картасы",
                        color = AppText,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                    )
                    Text(
                        "Карта сложных звуков",
                        color = AppText.copy(alpha = 0.58f),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                "Нәтижелер бір қарағанда / Результаты одним взглядом",
                color = AppText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
            )
            SoundLegend()

            if (scores.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    color = AppBlue.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = null,
                            tint = AppBlue,
                            modifier = Modifier.size(33.dp),
                        )
                        Text(
                            "Алдымен сөйлеуді тексеріңіз\nСначала пройдите экспресс-оценку",
                            modifier = Modifier.padding(top = 8.dp),
                            color = AppText,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Button(
                            onClick = onStartAssessment,
                            modifier = Modifier.padding(top = 14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                        ) {
                            Text("Тексеруді бастау / Начать")
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(top = 16.dp)) {
                SoundMapEngine.trackedSounds.chunked(4).forEach { rowSounds ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowSounds.forEach { sound ->
                            SoundTile(
                                sound = sound,
                                score = scores[sound],
                                selected = selectedSound == sound,
                                onClick = { selectedSound = sound },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(4 - rowSounds.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            SoundPracticePanel(
                sound = selectedSound,
                score = scores[selectedSound],
                words = matchingWords,
                audio = audio,
                onOpenLevel = onOpenLevel,
            )

            Text(
                "Карта оқу жаттығуларына арналған және медициналық қорытынды емес.\nКарта предназначена для обучения и не является диагнозом.",
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                color = AppText.copy(alpha = 0.46f),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun SoundLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LegendItem(AppGreen, "Жақсы\nХорошо")
        LegendItem(AppOrange, "Қайталау\nПовторить")
        LegendItem(Color(0xFFE65B65), "Жаттығу\nТренировать")
        LegendItem(Color(0xFFB0BEC5), "Тексерілмеді\nНе проверен")
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(14.dp), shape = CircleShape, color = color) {}
        Text(
            text,
            modifier = Modifier.padding(top = 5.dp),
            color = AppText.copy(alpha = 0.62f),
            textAlign = TextAlign.Center,
            fontSize = 8.sp,
            lineHeight = 10.sp,
        )
    }
}

@Composable
private fun SoundTile(
    sound: Char,
    score: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = SoundMapEngine.statusFor(score)
    val color = statusColor(status)
    Surface(
        onClick = onClick,
        modifier = modifier.height(86.dp),
        color = color,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = if (selected) 9.dp else 3.dp,
        border = if (selected) BorderStroke(3.dp, AppPurple) else null,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(sound.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 31.sp)
            Text(
                score?.let { "$it%" } ?: "—",
                color = Color.White.copy(alpha = 0.84f),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun SoundPracticePanel(
    sound: Char,
    score: Int?,
    words: List<SoundWord>,
    audio: SpeechAudio,
    onOpenLevel: (Int) -> Unit,
) {
    val status = SoundMapEngine.statusFor(score)
    val color = statusColor(status)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 5.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = color) {
                    Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                        Text(sound.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 25.sp)
                    }
                }
                Column(modifier = Modifier.padding(start = 13.dp)) {
                    Text(
                        soundStatusTitle(status),
                        color = AppText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        score?.let { "Сәйкестік / Совпадение: $it%" }
                            ?: "Экспресс-бағалауда әлі тексерілмеді",
                        color = AppText.copy(alpha = 0.58f),
                        fontSize = 11.sp,
                    )
                }
            }

            Text(
                "Сөздер / Слова",
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                color = AppText,
                fontWeight = FontWeight.ExtraBold,
            )
            words.take(6).chunked(2).forEach { rowWords ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowWords.forEach { item ->
                        Surface(
                            onClick = { audio.speak(item.word.word, slower = true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            color = AppBlue.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(item.word.word, color = AppText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Icon(
                                    Icons.Outlined.VolumeUp,
                                    contentDescription = "Тыңдау",
                                    tint = AppBlue,
                                    modifier = Modifier.padding(start = 7.dp).size(17.dp),
                                )
                            }
                        }
                    }
                    if (rowWords.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            words.firstOrNull()?.let { first ->
                Button(
                    onClick = { onOpenLevel(first.levelId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPurple),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Жаттығуды ашу / Открыть упражнения", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

private fun statusColor(status: SoundStatus): Color = when (status) {
    SoundStatus.GOOD -> AppGreen
    SoundStatus.REPEAT -> AppOrange
    SoundStatus.TRAIN -> Color(0xFFE65B65)
    SoundStatus.UNCHECKED -> Color(0xFFB0BEC5)
}

private fun soundStatusTitle(status: SoundStatus): String = when (status) {
    SoundStatus.GOOD -> "Жақсы шығады / Получается хорошо"
    SoundStatus.REPEAT -> "Қайталау керек / Требуется повторение"
    SoundStatus.TRAIN -> "Жаттығу ұсынылады / Рекомендуется тренировка"
    SoundStatus.UNCHECKED -> "Тексерілмеді / Ещё не проверен"
}
