@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.farkhad.speechapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farkhad.speechapp.data.FirebaseRepository
import com.farkhad.speechapp.data.ParentReflection
import com.farkhad.speechapp.data.ParentReflectionEngine
import com.farkhad.speechapp.data.ProgressRepository
import com.farkhad.speechapp.ui.theme.AppBackground
import com.farkhad.speechapp.ui.theme.AppGreen
import com.farkhad.speechapp.ui.theme.AppNavy
import com.farkhad.speechapp.ui.theme.AppOutline
import com.farkhad.speechapp.ui.theme.AppRed
import com.farkhad.speechapp.ui.theme.AppSurfaceMuted
import com.farkhad.speechapp.ui.theme.AppText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ParentReflectionScreen(
    progress: ProgressRepository,
    progressRevision: Int,
    repository: FirebaseRepository?,
    onBack: () -> Unit,
) {
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val reflections = remember(progressRevision) { progress.parentReflections }
    val todayEntry = reflections.firstOrNull { it.dateKey == todayKey }
    val insight = remember(reflections) { ParentReflectionEngine.weeklyInsight(reflections) }
    val scope = rememberCoroutineScope()

    var engagement by rememberSaveable(todayEntry?.id) { mutableStateOf(todayEntry?.engagement ?: 3) }
    var clarity by rememberSaveable(todayEntry?.id) { mutableStateOf(todayEntry?.clarity ?: 3) }
    var independence by rememberSaveable(todayEntry?.id) { mutableStateOf(todayEntry?.independence ?: 3) }
    var practiceMinutes by rememberSaveable(todayEntry?.id) { mutableStateOf(todayEntry?.practiceMinutes ?: 10) }
    var context by rememberSaveable(todayEntry?.id) { mutableStateOf(todayEntry?.context.orEmpty()) }
    var wins by remember(todayEntry?.id) { mutableStateOf(todayEntry?.wins.orEmpty()) }
    var note by rememberSaveable(todayEntry?.id) { mutableStateOf(todayEntry?.note.orEmpty()) }
    var nextStep by rememberSaveable(todayEntry?.id) { mutableStateOf(todayEntry?.nextStep.orEmpty()) }
    var saveMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
    ) {
        AppTopBar(title = "Ата-ана рефлексиясы", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text(
                "Бүгінгі қысқа жазба",
                color = AppRed,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                "Бүгін қалай\nөтті?",
                modifier = Modifier.padding(top = 8.dp),
                color = AppText,
                style = MaterialTheme.typography.headlineLarge,
                lineHeight = 42.sp,
            )
            Text(
                SimpleDateFormat("d MMMM, EEEE", Locale("kk")).format(Date()),
                modifier = Modifier.padding(top = 10.dp),
                color = AppText.copy(alpha = 0.52f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Баланы ешкіммен салыстырмаймыз. Тек бірге өткен күннен маңызды сәттерді сақтаймыз.",
                modifier = Modifier.padding(top = 16.dp),
                color = AppText.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodyMedium,
            )

            ReflectionSectionTitle("Не байқадыңыз?")
            ReflectionScale(
                title = "Қарым-қатынасқа ынтасы",
                hint = "Өзі әңгіме немесе ойын бастауға талпынды ма?",
                value = engagement,
                onValueChange = { engagement = it },
            )
            ReflectionScale(
                title = "Сөздерінің анықтығы",
                hint = "Таныс сөздерді түсіну бүгін қаншалықты оңай болды?",
                value = clarity,
                onValueChange = { clarity = it },
            )
            ReflectionScale(
                title = "Өздігінен айтуы",
                hint = "Үлгісіз немесе еске салусыз сөз қолданды ма?",
                value = independence,
                onValueChange = { independence = it },
            )

            ReflectionSectionTitle("Қалай жаттықтыңыз?")
            ReflectionPrompt("Бүгін қанша минут бірге жаттықтыңыз?")
            ChoiceRow(
                options = listOf("0", "5", "10", "15", "20+"),
                selected = if (practiceMinutes >= 20) "20+" else practiceMinutes.toString(),
                onSelected = { practiceMinutes = it.removeSuffix("+").toInt() },
            )

            ReflectionPrompt("Қай жағдайда сөйлеу жеңіл болды?")
            ChoicePair("Ойын", "Кітап", context) { context = it }
            ChoicePair("Күнделікті іс", "Серуен", context) { context = it }

            ReflectionSectionTitle("Кішкентай жеңістер")
            listOf(
                "Өзі жаңа сөз қолданды",
                "Қысқа фразаны қайталады",
                "Сұраққа жауап берді",
                "Қиын дыбысты анық айтты",
            ).forEach { win ->
                ReflectionCheck(
                    text = win,
                    checked = win in wins,
                    onCheckedChange = { checked ->
                        wins = if (checked) wins + win else wins - win
                    },
                )
            }

            ReflectionSectionTitle("Есте қалған сәт")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(400) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Бала не айтты немесе не көмектесті?") },
                supportingText = { Text("${note.length}/400") },
                maxLines = 4,
                shape = RoundedCornerShape(4.dp),
            )

            ReflectionPrompt("Келесі шағын қадам")
            ChoicePair("Сол сөздерді қайталау", "Жаңа дыбыс", nextStep) { nextStep = it }
            ChoicePair("Face Map", "Еркін әңгіме", nextStep) { nextStep = it }

            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val reflection = ParentReflection(
                        id = todayEntry?.id ?: now,
                        dateKey = todayKey,
                        createdAt = now,
                        engagement = engagement,
                        clarity = clarity,
                        independence = independence,
                        practiceMinutes = practiceMinutes,
                        context = context,
                        wins = wins,
                        note = note.trim(),
                        nextStep = nextStep,
                    )
                    progress.saveParentReflection(reflection)
                    saveMessage = "Бүгінгі жазба сақталды"
                    if (repository != null) {
                        scope.launch {
                            saveMessage = repository.saveParentReflection(reflection).fold(
                                onSuccess = { "Сақталды — жазба аккаунтыңызда да қолжетімді" },
                                onFailure = { "Телефонда сақталды. Интернет пайда болғанда қайта синхрондап көріңіз" },
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp).padding(top = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppNavy),
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(if (todayEntry == null) "Рефлексияны сақтау" else "Бүгінгі жазбаны жаңарту")
            }
            saveMessage?.let { message ->
                Text(
                    message,
                    modifier = Modifier.padding(top = 12.dp),
                    color = AppGreen,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            ReflectionSectionTitle("Аптадан бір ой")
            Text(insight.title, color = AppText, style = MaterialTheme.typography.titleLarge)
            Text(
                insight.detail,
                modifier = Modifier.padding(top = 7.dp),
                color = AppText.copy(alpha = 0.64f),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = insight.completedDays / 7f,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(4.dp),
                color = AppRed,
                trackColor = AppOutline.copy(alpha = 0.55f),
            )
            Text(
                "${insight.completedDays}/7 күн толтырылды",
                modifier = Modifier.padding(top = 7.dp),
                color = AppText.copy(alpha = 0.48f),
                style = MaterialTheme.typography.labelSmall,
            )

            if (reflections.isNotEmpty()) {
                ReflectionSectionTitle("Алдыңғы жазбалар")
                reflections.take(7).forEachIndexed { index, entry ->
                    ReflectionHistoryRow(entry)
                    if (index < reflections.take(7).lastIndex) Divider(color = AppOutline)
                }
            }

            Text(
                "Рефлексия — ата-ананың жеке бақылауы. Ол медициналық бағалау немесе диагноз емес.",
                modifier = Modifier.padding(top = 26.dp, bottom = 18.dp),
                color = AppText.copy(alpha = 0.44f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ReflectionSectionTitle(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 30.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(22.dp).height(2.dp).background(AppRed))
        Text(
            title,
            modifier = Modifier.padding(start = 12.dp),
            color = AppText,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ReflectionScale(
    title: String,
    hint: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    val labels = listOf("Қиын", "Қолдаумен", "Тұрақты", "Жақсы", "Өте жақсы")
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = AppText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(hint, modifier = Modifier.padding(top = 3.dp), color = AppText.copy(alpha = 0.48f), fontSize = 10.sp)
            }
            Text(labels[value - 1], color = AppRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            (1..5).forEach { score ->
                Surface(
                    modifier = Modifier.weight(1f).height(40.dp).clickable { onValueChange(score) },
                    color = if (score == value) AppNavy else Color.Transparent,
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(1.dp, if (score == value) AppNavy else AppOutline),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(score.toString(), color = if (score == value) Color.White else AppText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReflectionPrompt(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 9.dp, bottom = 9.dp),
        color = AppText,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ChoiceRow(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            ReflectionChoice(option, option == selected, { onSelected(option) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChoicePair(
    first: String,
    second: String,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp)) {
        ReflectionChoice(first, first == selected, { onSelected(first) }, Modifier.weight(1f))
        Spacer(modifier = Modifier.width(7.dp))
        ReflectionChoice(second, second == selected, { onSelected(second) }, Modifier.weight(1f))
    }
}

@Composable
private fun ReflectionChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(44.dp).clickable(onClick = onClick),
        color = if (selected) AppSurfaceMuted else Color.Transparent,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, if (selected) AppText else AppOutline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = AppText, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun ReflectionCheck(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = AppNavy),
        )
        Text(text, color = AppText.copy(alpha = 0.76f), fontSize = 13.sp)
    }
}

@Composable
private fun ReflectionHistoryRow(entry: ParentReflection) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            SimpleDateFormat("dd.MM", Locale.US).format(Date(entry.createdAt)),
            modifier = Modifier.width(54.dp),
            color = AppRed,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${entry.averageScore.toInt()}/5  ·  ${entry.practiceMinutes} минут  ·  ${entry.context.ifBlank { "контекст көрсетілмеді" }}",
                color = AppText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (entry.wins.isNotEmpty()) {
                Text(
                    entry.wins.joinToString(" · "),
                    modifier = Modifier.padding(top = 4.dp),
                    color = AppText.copy(alpha = 0.52f),
                    fontSize = 10.sp,
                    maxLines = 2,
                )
            }
            if (entry.note.isNotBlank()) {
                Text(
                    entry.note,
                    modifier = Modifier.padding(top = 5.dp),
                    color = AppText.copy(alpha = 0.68f),
                    fontSize = 11.sp,
                    maxLines = 2,
                )
            }
        }
    }
}
