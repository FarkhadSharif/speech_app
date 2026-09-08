@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.farkhad.speechapp.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farkhad.speechapp.assessment.PersonalizedRoute
import com.farkhad.speechapp.assessment.PersonalizedRouteEngine
import com.farkhad.speechapp.assessment.RouteActivity
import com.farkhad.speechapp.assessment.RouteWord
import com.farkhad.speechapp.assessment.SoundMapEngine
import com.farkhad.speechapp.assessment.SoundStatus
import com.farkhad.speechapp.audio.SpeechAudio
import com.farkhad.speechapp.data.ProgressRepository
import com.farkhad.speechapp.model.GameActivity
import com.farkhad.speechapp.ui.theme.AppBackground
import com.farkhad.speechapp.ui.theme.AppBlue
import com.farkhad.speechapp.ui.theme.AppGreen
import com.farkhad.speechapp.ui.theme.AppOrange
import com.farkhad.speechapp.ui.theme.AppPurple
import com.farkhad.speechapp.ui.theme.AppNavy
import com.farkhad.speechapp.ui.theme.AppRed
import com.farkhad.speechapp.ui.theme.AppText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun PersonalizedRouteScreen(
    progress: ProgressRepository,
    progressRevision: Int,
    audio: SpeechAudio,
    onBack: () -> Unit,
    onStartAssessment: () -> Unit,
    onOpenSoundMap: () -> Unit,
    onOpenActivity: (GameActivity) -> Unit,
) {
    val route = remember(progressRevision) {
        PersonalizedRouteEngine.build(progress.assessedSoundScores)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
    ) {
        RouteTopBar(onBack)
        if (route.needsAssessment) {
            EmptyRoute(onStartAssessment)
        } else {
            RouteContent(
                route = route,
                soundScores = progress.assessedSoundScores,
                lastAssessmentAt = progress.lastAssessmentAt,
                audio = audio,
                onOpenSoundMap = onOpenSoundMap,
                onStartAssessment = onStartAssessment,
                onOpenActivity = onOpenActivity,
            )
        }
    }
}

@Composable
private fun RouteTopBar(onBack: () -> Unit) {
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
                    "Бүгінгі жаттығуың",
                    color = AppText,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                )
                Text(
                    "Твоя тренировка сегодня",
                    color = AppText.copy(alpha = 0.58f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun EmptyRoute(onStartAssessment: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🧭", fontSize = 72.sp)
        Text(
            "Жеке маршрут құрайық\nСоздадим персональный маршрут",
            modifier = Modifier.padding(top = 20.dp),
            color = AppText,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            "Алдымен экспресс-бағалаудан өтіңіз. Нәтижеге сай дыбыстар, сөздер және ойындар таңдалады.\n\nСначала пройдите экспресс-оценку — приложение подберёт звуки, слова и игры.",
            modifier = Modifier.padding(top = 16.dp),
            color = AppText.copy(alpha = 0.64f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
        Button(
            onClick = onStartAssessment,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Бағалауды бастау / Начать оценку", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun RouteContent(
    route: PersonalizedRoute,
    soundScores: Map<Char, Int>,
    lastAssessmentAt: Long,
    audio: SpeechAudio,
    onOpenSoundMap: () -> Unit,
    onStartAssessment: () -> Unit,
    onOpenActivity: (GameActivity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(AppNavy)
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("03", color = AppRed, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Column(modifier = Modifier.padding(start = 18.dp)) {
                        Text(
                            "Жеке маршрут дайын!",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                        )
                        Text(
                            "Персональный маршрут готов",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                        )
                        Text(
                            "${route.focusSounds.size} дыбыс • ${route.words.size} сөз • ${route.activities.size} ойын",
                            modifier = Modifier.padding(top = 8.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        SectionTitle("1", "Жаттығатын дыбыстар / Звуки для тренировки")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            route.focusSounds.forEach { sound ->
                val score = soundScores[sound]
                val color = routeSoundColor(SoundMapEngine.statusFor(score))
                Surface(
                    onClick = onOpenSoundMap,
                    modifier = Modifier
                        .weight(1f)
                        .height(82.dp),
                    color = color,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 5.dp,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(sound.uppercase(), color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black)
                        Text("${score ?: 0}%", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp)
                    }
                }
            }
        }

        SectionTitle("2", "Қайталайтын сөздер / Слова для повторения")
        route.words.chunked(2).forEach { rowWords ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                rowWords.forEach { item ->
                    RouteWordCard(
                        item = item,
                        onClick = { audio.speak(item.word.word, slower = true) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowWords.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        SectionTitle("3", "Алдымен осы ойындар / Сначала эти игры")
        route.activities.forEachIndexed { index, item ->
            RouteActivityCard(
                number = index + 1,
                item = item,
                onClick = { onOpenActivity(item.activity) },
            )
        }

        SectionTitle("4", "Қайта тексеру / Повторная проверка")
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppGreen.copy(alpha = 0.10f),
            shape = RoundedCornerShape(22.dp),
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = AppGreen) {
                    Box(contentAlignment = Alignment.Center) { Text("📅", fontSize = 25.sp) }
                }
                Column(modifier = Modifier.padding(start = 13.dp).weight(1f)) {
                    Text(
                        nextAssessmentLabel(lastAssessmentAt, route.repeatAssessmentAfterDays),
                        color = AppText,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        "Жаттығудан кейін нәтижені салыстырамыз / Затем сравним результат",
                        color = AppText.copy(alpha = 0.58f),
                        fontSize = 11.sp,
                    )
                }
            }
        }
        Button(
            onClick = onStartAssessment,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
            shape = RoundedCornerShape(17.dp),
        ) {
            Text("Қайта тексеру / Проверить снова", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SectionTitle(number: String, title: String) {
    Row(
        modifier = Modifier.padding(top = 22.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = AppPurple) {
            Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                Text(number, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
        Text(
            title,
            modifier = Modifier.padding(start = 10.dp),
            color = AppText,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun RouteWordCard(item: RouteWord, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(item.word.emoji, fontSize = 24.sp)
            Text(
                item.word.word,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
                color = AppText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Text("🔊", fontSize = 15.sp)
        }
    }
}

@Composable
private fun RouteActivityCard(number: Int, item: RouteActivity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = AppBlue) {
                Box(contentAlignment = Alignment.Center) {
                    Text(number.toString(), color = Color.White, fontWeight = FontWeight.Black)
                }
            }
            Text(item.activity.emoji, modifier = Modifier.padding(start = 12.dp), fontSize = 28.sp)
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(item.activity.title, color = AppText, fontWeight = FontWeight.ExtraBold)
                Text(
                    "${item.activity.minutes} мин • ${item.activity.subtitle}",
                    color = AppText.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                )
            }
            Text("›", color = AppPurple, fontWeight = FontWeight.Black, fontSize = 25.sp)
        }
    }
}

private fun routeSoundColor(status: SoundStatus): Color = when (status) {
    SoundStatus.GOOD -> AppGreen
    SoundStatus.REPEAT -> AppOrange
    SoundStatus.TRAIN -> Color(0xFFE65B65)
    SoundStatus.UNCHECKED -> Color(0xFFB0BEC5)
}

private fun nextAssessmentLabel(lastAssessmentAt: Long, afterDays: Int): String {
    if (lastAssessmentAt <= 0L || afterDays <= 0) return "Бағалаудан кейін / После оценки"
    val nextDate = Date(lastAssessmentAt + TimeUnit.DAYS.toMillis(afterDays.toLong()))
    val formatted = SimpleDateFormat("dd.MM.yyyy", Locale.US).format(nextDate)
    return "$afterDays күннен кейін: $formatted / Через $afterDays дней"
}
