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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farkhad.speechapp.data.FirebaseRepository
import com.farkhad.speechapp.data.ExerciseAnalysisType
import com.farkhad.speechapp.data.ExerciseSessionReport
import com.farkhad.speechapp.data.ExerciseSessionSource
import com.farkhad.speechapp.data.ExerciseStepReport
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
    val exerciseReports = remember(progressRevision) { progress.exerciseSessionReports }
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
        AppTopBar(title = "Сөйлеу күнделігі", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text(
                "Балаңыздың нақты жолы",
                color = AppRed,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                "Әр жаттығу\nесте қалады.",
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
                "Автоматты журнал әр орындалған қадамды көрсетеді. Төменде өз байқауыңызды да қоса аласыз.",
                modifier = Modifier.padding(top = 16.dp),
                color = AppText.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodyMedium,
            )

            ExerciseJournalSummary(exerciseReports)
            ReflectionSectionTitle("Әр жаттығудың есебі")
            if (exerciseReports.isEmpty()) {
                EmptyExerciseJournal()
            } else {
                exerciseReports.take(12).forEach { report ->
                    ExerciseReportCard(report)
                    Spacer(modifier = Modifier.height(9.dp))
                }
            }

            AnalysisPrivacyNote()

            ReflectionSectionTitle("Бүгін сіз не байқадыңыз?")
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
private fun ExerciseJournalSummary(reports: List<ExerciseSessionReport>) {
    val weekAgo = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
    val recent = reports.filter { it.completedAt >= weekAgo }
    val averageScore = recent.map { it.score }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
    val minutes = (recent.sumOf { it.durationSeconds } / 60L).coerceAtLeast(0L)
    val analyzed = recent.sumOf { it.audioAnalyzedSteps + it.videoAnalyzedSteps }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        color = AppNavy,
        shape = RoundedCornerShape(3.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Assessment,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.86f),
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    "Соңғы 7 күн",
                    modifier = Modifier.padding(start = 9.dp),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 15.dp)) {
                JournalMetric(recent.size.toString(), "орындалды", Modifier.weight(1f))
                JournalMetric("$averageScore%", "орташа", Modifier.weight(1f))
                JournalMetric("$minutes мин", "жаттығу", Modifier.weight(1f))
                JournalMetric(analyzed.toString(), "талдау", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun JournalMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(
            label,
            modifier = Modifier.padding(top = 2.dp),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun EmptyExerciseJournal() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppSurfaceMuted,
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(1.dp, AppOutline),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Assessment,
                contentDescription = null,
                tint = AppRed,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Алғашқы есеп әлі жоқ", color = AppText, fontWeight = FontWeight.SemiBold)
                Text(
                    "Бала бір жаттығуды аяқтағаннан кейін нәтижесі осында автоматты түрде шығады.",
                    modifier = Modifier.padding(top = 3.dp),
                    color = AppText.copy(alpha = 0.56f),
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun ExerciseReportCard(report: ExerciseSessionReport) {
    var expanded by rememberSaveable(report.sessionId) { mutableStateOf(false) }
    val sourceIcon = when (report.source) {
        ExerciseSessionSource.FACE_MAP -> Icons.Outlined.Face
        ExerciseSessionSource.ASSESSMENT -> Icons.Outlined.Assessment
        else -> Icons.Outlined.CheckCircle
    }
    val scoreColor = when {
        report.score >= 85 -> AppGreen
        report.score >= 60 -> Color(0xFFB86B27)
        else -> AppRed
    }
    val formattedTime = remember(report.completedAt) {
        SimpleDateFormat("d MMM, HH:mm", Locale("kk")).format(Date(report.completedAt))
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        color = Color.Transparent,
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(1.dp, AppOutline),
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    sourceIcon,
                    contentDescription = null,
                    tint = AppRed,
                    modifier = Modifier.size(24.dp),
                )
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        report.activityTitle,
                        color = AppText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        "${report.levelTitle}  ·  $formattedTime",
                        modifier = Modifier.padding(top = 2.dp),
                        color = AppText.copy(alpha = 0.48f),
                        fontSize = 10.sp,
                    )
                }
                Text("${report.score}%", color = scoreColor, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Жасыру" else "Толығырақ",
                    tint = AppText.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 7.dp).size(20.dp),
                )
            }

            Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, null, tint = AppText.copy(alpha = 0.48f), modifier = Modifier.size(16.dp))
                Text(
                    formatDuration(report.durationSeconds),
                    modifier = Modifier.padding(start = 5.dp),
                    color = AppText.copy(alpha = 0.58f),
                    fontSize = 10.sp,
                )
                Text(
                    "${report.attemptNumber}-орындау",
                    modifier = Modifier.padding(start = 13.dp),
                    color = AppText.copy(alpha = 0.58f),
                    fontSize = 10.sp,
                )
                if (report.audioAnalyzedSteps > 0) {
                    Icon(Icons.Outlined.RecordVoiceOver, null, tint = AppGreen, modifier = Modifier.padding(start = 13.dp).size(16.dp))
                    Text("аудио ${report.audioAnalyzedSteps}", modifier = Modifier.padding(start = 4.dp), color = AppGreen, fontSize = 10.sp)
                }
                if (report.videoAnalyzedSteps > 0) {
                    Icon(Icons.Outlined.Videocam, null, tint = AppGreen, modifier = Modifier.padding(start = 13.dp).size(16.dp))
                    Text("видео ${report.videoAnalyzedSteps}", modifier = Modifier.padding(start = 4.dp), color = AppGreen, fontSize = 10.sp)
                }
            }

            if (expanded) {
                Divider(modifier = Modifier.padding(top = 14.dp, bottom = 4.dp), color = AppOutline)
                report.steps.forEachIndexed { index, step ->
                    ExerciseStepRow(index + 1, step)
                    if (index < report.steps.lastIndex) {
                        Divider(color = AppOutline.copy(alpha = 0.65f))
                    }
                }
                SessionAnalysisExplanation(report)
            }
        }
    }
}

@Composable
private fun ExerciseStepRow(number: Int, step: ExerciseStepReport) {
    val icon = when (step.analysisType) {
        ExerciseAnalysisType.AUDIO -> Icons.Outlined.RecordVoiceOver
        ExerciseAnalysisType.VIDEO -> Icons.Outlined.Videocam
        ExerciseAnalysisType.ADULT_ASSISTED -> Icons.Outlined.VolumeUp
        else -> Icons.Outlined.TouchApp
    }
    val mode = when (step.analysisType) {
        ExerciseAnalysisType.AUDIO -> "Дауыс автоматты талданды"
        ExerciseAnalysisType.VIDEO -> "Ерін қимылы құрылғыда талданды"
        ExerciseAnalysisType.ADULT_ASSISTED -> "Ересекпен бірге орындалды"
        else -> "Жауап пен әрекет тексерілді"
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                number.toString().padStart(2, '0'),
                color = AppRed,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(step.title, color = AppText, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                Row(modifier = Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = AppText.copy(alpha = 0.48f), modifier = Modifier.size(15.dp))
                    Text(mode, modifier = Modifier.padding(start = 5.dp), color = AppText.copy(alpha = 0.53f), fontSize = 9.sp)
                }
            }
            Text("${step.score}%", color = if (step.score >= 85) AppGreen else AppRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        if (step.analysisType == ExerciseAnalysisType.AUDIO) {
            if (step.targetText.isNotBlank()) {
                EvidenceLine("Үлгі", step.targetText)
            }
            if (step.recognizedText.isNotBlank()) {
                EvidenceLine("Танылғаны", step.recognizedText)
            }
            EvidenceLine("Талпыныс", "${step.attempts}")
        }
        if (step.analysisType == ExerciseAnalysisType.VIDEO) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 25.dp, top = 8.dp)) {
                FaceEvidence("Ауыз ашылуы", step.mouthOpeningPercent, Modifier.weight(1f))
                FaceEvidence("Ерін дөңгеленуі", step.lipRoundingPercent, Modifier.weight(1f))
                FaceEvidence("Ұстау", step.holdSeconds, Modifier.weight(1f), " сек")
            }
        }
        if (step.feedback.isNotBlank()) {
            Text(
                step.feedback,
                modifier = Modifier.padding(start = 25.dp, top = 6.dp),
                color = AppText.copy(alpha = 0.68f),
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun EvidenceLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 25.dp, top = 6.dp)) {
        Text("$label:", modifier = Modifier.width(72.dp), color = AppText.copy(alpha = 0.45f), fontSize = 9.sp)
        Text(value, modifier = Modifier.weight(1f), color = AppText.copy(alpha = 0.74f), fontSize = 10.sp)
    }
}

@Composable
private fun FaceEvidence(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    suffix: String = "%",
) {
    Column(modifier = modifier) {
        Text(if (value >= 0) "$value$suffix" else "—", color = AppText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text(label, modifier = Modifier.padding(top = 2.dp), color = AppText.copy(alpha = 0.43f), fontSize = 8.sp)
    }
}

@Composable
private fun SessionAnalysisExplanation(report: ExerciseSessionReport) {
    val audioText = when {
        report.audioAnalyzedSteps > 0 -> "Аудио: қазақша сөйлеуді тану нәтижесі мәтіндік үлгімен салыстырылды. Дауыс файлы сақталмады."
        report.steps.any { it.analysisType == ExerciseAnalysisType.ADULT_ASSISTED } -> "Аудио: автоматты талдау қолданылмады, орындауды ересек растады."
        else -> "Аудио: нұсқаулық тыңдатылды, бірақ баланың дауысы бұл тапсырмада талданған жоқ."
    }
    val videoText = if (report.videoAnalyzedSteps > 0) {
        "Видео: Face Map ауыздың ашылуы мен еріннің дөңгеленуін құрылғыда есептеді. Кадрлар сақталмады."
    } else {
        "Видео: бұл жаттығуға камера қажет болмады және видео талдау жасалған жоқ."
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        color = AppSurfaceMuted,
        shape = RoundedCornerShape(2.dp),
    ) {
        Column(modifier = Modifier.padding(11.dp)) {
            Text(audioText, color = AppText.copy(alpha = 0.63f), fontSize = 9.sp, lineHeight = 13.sp)
            Text(videoText, modifier = Modifier.padding(top = 6.dp), color = AppText.copy(alpha = 0.63f), fontSize = 9.sp, lineHeight = 13.sp)
        }
    }
}

@Composable
private fun AnalysisPrivacyNote() {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Outlined.Info, null, tint = AppText.copy(alpha = 0.42f), modifier = Modifier.size(17.dp))
        Text(
            "Ұпай — қолданбадағы оқу көрсеткіші, диагноз емес. Аудио мен камера кадрлары сақталмайды; журналда тек нәтиже, танылған мәтін және есептелген пайыздар қалады.",
            modifier = Modifier.padding(start = 8.dp).weight(1f),
            color = AppText.copy(alpha = 0.49f),
            fontSize = 9.sp,
            lineHeight = 13.sp,
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val minutes = safe / 60L
    val remaining = safe % 60L
    return if (minutes > 0L) "$minutes мин $remaining сек" else "$remaining сек"
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
