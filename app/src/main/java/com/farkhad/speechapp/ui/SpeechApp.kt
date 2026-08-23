package com.farkhad.speechapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farkhad.speechapp.audio.SpeechAudio
import com.farkhad.speechapp.audio.rememberSpeechAudio
import com.farkhad.speechapp.data.FirebaseRepository
import com.farkhad.speechapp.data.ProgressRepository
import com.farkhad.speechapp.model.Curriculum
import kotlinx.coroutines.launch
import com.farkhad.speechapp.model.CurriculumLevel
import com.farkhad.speechapp.model.GameActivity
import com.farkhad.speechapp.ui.theme.AppBackground
import com.farkhad.speechapp.ui.theme.AppBlue
import com.farkhad.speechapp.ui.theme.AppGreen
import com.farkhad.speechapp.ui.theme.AppOrange
import com.farkhad.speechapp.ui.theme.AppPurple
import com.farkhad.speechapp.ui.theme.AppText

private enum class AppScreen {
    Role,
    ChildHome,
    Level,
    Game,
    WordLibrary,
    ParentDashboard,
    Statistics,
    ParentGuide,
    ChildInfo,
}

@Composable
fun SpeechApp(
    userId: String,
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val progress = remember(userId) { ProgressRepository(context.applicationContext, userId) }
    val audio = rememberSpeechAudio()
    val progressRevision = progress.revision

    var screenName by rememberSaveable { mutableStateOf(AppScreen.Role.name) }
    var selectedLevelId by rememberSaveable { mutableStateOf(1) }
    var selectedActivityId by rememberSaveable { mutableStateOf(Curriculum.level(1).activities.first().id) }
    var completedThisSession by rememberSaveable { mutableStateOf(0) }
    var showBreakReminder by rememberSaveable { mutableStateOf(false) }

    var showParentalGate by remember { mutableStateOf(false) }
    var parentalGateTarget by remember { mutableStateOf<AppScreen?>(null) }
    var correctPin by remember { mutableStateOf("1234") }
    val repository = remember { FirebaseRepository() }

    LaunchedEffect(userId) {
        repository.getChildInfo().onSuccess { profile ->
            if (profile != null) {
                correctPin = profile.parentPin
            }
        }
    }

    val screen = AppScreen.valueOf(screenName)
    fun navigate(target: AppScreen) {
        screenName = target.name
    }

    fun requestParentalGate(target: AppScreen) {
        parentalGateTarget = target
        showParentalGate = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground,
    ) {
        when (screen) {
            AppScreen.Role -> Box(modifier = Modifier.fillMaxSize()) {
                RoleSelectionScreen(
                    onChild = { navigate(AppScreen.ChildHome) },
                    onParent = { navigate(AppScreen.ParentDashboard) },
                )

                // Sign Out button placed specifically on the Role Selection screen
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Text("Шығу")
                }
            }

            AppScreen.ChildHome -> ChildHomeScreen(
                progress = progress,
                progressRevision = progressRevision,
                audio = audio,
                onBack = { requestParentalGate(AppScreen.Role) },
                onOpenLevel = { levelId ->
                    selectedLevelId = levelId
                    navigate(AppScreen.Level)
                },
                onOpenWords = { navigate(AppScreen.WordLibrary) },
                onOpenParent = { requestParentalGate(AppScreen.ParentDashboard) },
            )

            AppScreen.Level -> LevelHubScreen(
                level = Curriculum.level(selectedLevelId),
                progress = progress,
                progressRevision = progressRevision,
                audio = audio,
                onBack = { navigate(AppScreen.ChildHome) },
                onOpenActivity = { activity ->
                    selectedActivityId = activity.id
                    navigate(AppScreen.Game)
                },
            )

            AppScreen.Game -> {
                val activity = Curriculum.activity(selectedActivityId)
                val next = Curriculum.nextActivity(activity.id)
                val nextIsUnlocked = next != null && progress.isLevelUnlocked(
                    Curriculum.levelForActivity(next.id).id,
                )
                GameSessionScreen(
                    activity = activity,
                    audio = audio,
                    onBack = {
                        selectedLevelId = Curriculum.levelForActivity(activity.id).id
                        navigate(AppScreen.Level)
                    },
                    onSaveResult = { score ->
                        progress.completeActivity(activity, score)
                        completedThisSession += 1
                        if (completedThisSession >= 3) showBreakReminder = true
                    },
                    onNextActivity = {
                        if (next == null || !progress.isLevelUnlocked(Curriculum.levelForActivity(next.id).id)) {
                            navigate(AppScreen.ChildHome)
                        } else {
                            selectedActivityId = next.id
                            selectedLevelId = Curriculum.levelForActivity(next.id).id
                            navigate(AppScreen.Game)
                        }
                    },
                    hasNextActivity = nextIsUnlocked,
                )
            }

            AppScreen.WordLibrary -> WordLibraryScreen(
                audio = audio,
                onBack = { navigate(AppScreen.ChildHome) },
            )

            AppScreen.ParentDashboard -> ParentDashboardScreen(
                progress = progress,
                progressRevision = progressRevision,
                onBack = { navigate(AppScreen.Role) },
                onStatistics = { navigate(AppScreen.Statistics) },
                onGuide = { navigate(AppScreen.ParentGuide) },
                onChildInfo = { navigate(AppScreen.ChildInfo) }
            )

            AppScreen.Statistics -> StatisticsScreen(
                progress = progress,
                progressRevision = progressRevision,
                onBack = { navigate(AppScreen.ParentDashboard) },
            )

            AppScreen.ParentGuide -> ParentGuideScreen(
                progress = progress,
                onBack = { navigate(AppScreen.ParentDashboard) },
                onReset = {
                    progress.resetProgress()
                    navigate(AppScreen.Role)
                },
            )

            AppScreen.ChildInfo -> ChildInfoScreen(
                onBack = { navigate(AppScreen.ParentDashboard) },
                onPinUpdated = { newPin -> correctPin = newPin }
            )
        }
    }

    if (showParentalGate) {
        ParentalGate(
            correctPin = correctPin,
            onSuccess = {
                showParentalGate = false
                parentalGateTarget?.let { navigate(it) }
            },
            onDismiss = { showParentalGate = false }
        )
    }

    if (showBreakReminder) {
        AlertDialog(
            onDismissRequest = {
                showBreakReminder = false
                completedThisSession = 0
            },
            icon = { Text("🌿", fontSize = 36.sp) },
            title = { Text("Кішкентай үзіліс жасайық") },
            text = {
                Text(
                    "Үш ойын аяқталды. Енді 5–10 минут экраннан алыстап, жаңа сөздерді ойыншықпен, кітаппен немесе серуенде қайталап көріңдер.",
                    lineHeight = 21.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBreakReminder = false
                        completedThisSession = 0
                        navigate(AppScreen.ChildHome)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
                ) {
                    Text("Үзіліс жасау")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBreakReminder = false
                        completedThisSession = 0
                    },
                ) {
                    Text("Қазір емес")
                }
            },
        )
    }
}

@Composable
private fun RoleSelectionScreen(
    onChild: () -> Unit,
    onParent: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF2FBFF), Color(0xFFFFFBF0)),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.fillMaxHeight(0.11f))
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(AppBlue, Color(0xFF65C7FF)))),
            contentAlignment = Alignment.Center,
        ) {
            Text("🗣️", fontSize = 54.sp)
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Сөйле",
            color = AppBlue,
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "Тыңда • ойна • сөйле • әңгімеле",
            modifier = Modifier.padding(top = 8.dp),
            color = AppText.copy(alpha = 0.62f),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(38.dp))
        RoleButton(
            text = "🧒  Балаға арналған жол",
            subtitle = "6 деңгей • 18 ойын",
            color = AppOrange,
            onClick = onChild,
        )
        Spacer(modifier = Modifier.height(16.dp))
        RoleButton(
            text = "👪  Ата-ана бөлімі",
            subtitle = "Нәтиже, кеңес және қауіпсіздік",
            color = AppGreen,
            onClick = onParent,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Қысқа, көңілді сабақтар. Жарнама жоқ. Дерек серверге жіберілмейді.",
            color = AppText.copy(alpha = 0.48f),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RoleButton(
    text: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ChildHomeScreen(
    progress: ProgressRepository,
    progressRevision: Int,
    audio: SpeechAudio,
    onBack: () -> Unit,
    onOpenLevel: (Int) -> Unit,
    onOpenWords: () -> Unit,
    onOpenParent: () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val observeRevision = progressRevision
    val currentLevel = Curriculum.levels.firstOrNull { level ->
        progress.isLevelUnlocked(level.id) && progress.levelProgress(level) < 1f
    } ?: Curriculum.levels.last()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
    ) {
        AppTopBar(
            title = "Менің сөйлеу жолым",
            onBack = null, // Hidden for child protection
            trailing = "👪",
            onTrailing = onOpenParent,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill("⭐", "${progress.totalStars}", "жұлдыз", Modifier.weight(1f))
                StatPill("🏆", "${progress.completedCount}/18", "ойын", Modifier.weight(1f))
                StatPill("🌱", "${progress.activeDays}", "күн", Modifier.weight(1f))
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                color = Color(0xFFEAF8FF),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentLevel.emoji, fontSize = 32.sp)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Бүгінгі ұсыныс", color = AppBlue, fontWeight = FontWeight.ExtraBold)
                            Text(currentLevel.title, color = AppText, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "Бір ойыннан баста. Тыңдап болған соң экраннан тыс жерде жаңа сөзді тағы бір рет қолдан.",
                        modifier = Modifier.padding(top = 10.dp),
                        color = AppText.copy(alpha = 0.68f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                    Button(
                        onClick = { onOpenLevel(currentLevel.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Жалғастыру →", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!audio.supportsKazakh && audio.ready) {
                InfoStrip(
                    emoji = "🔊",
                    text = "Құрылғыда қазақша дауыс табылмады. Android баптауларынан қазақша TTS дауысын орнатуға болады.",
                    color = Color(0xFFFFF0D7),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Деңгейлер", color = AppText, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text("қиындық ↑", color = AppPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Curriculum.levels.forEach { level ->
                val unlocked = progress.isLevelUnlocked(level.id)
                LevelCard(
                    level = level,
                    unlocked = unlocked,
                    progress = progress.levelProgress(level),
                    stars = progress.levelStars(level),
                    onClick = { if (unlocked) onOpenLevel(level.id) },
                )
            }

            OutlinedButton(
                onClick = onOpenWords,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, AppGreen.copy(alpha = 0.55f)),
            ) {
                Text("🔊  Сөздерді тыңдау кітапханасы", color = AppGreen, fontWeight = FontWeight.ExtraBold)
            }
            Text(
                text = "Ескерту: бұл ойындар сөйлеу мен тілді күнделікті қолдауға арналған. Олар маман бағалауын немесе емін алмастырмайды.",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 20.dp),
                color = AppText.copy(alpha = 0.45f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LevelCard(
    level: CurriculumLevel,
    unlocked: Boolean,
    progress: Float,
    stars: Int,
    onClick: () -> Unit,
) {
    val colors = levelGradient(level.id)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .clickable(enabled = unlocked, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (unlocked) 3.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (unlocked) Brush.linearGradient(colors)
                    else Brush.linearGradient(listOf(Color(0xFFE8EBEE), Color(0xFFF2F3F4))),
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (unlocked) level.emoji else "🔒", fontSize = 31.sp)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    text = "${level.id}-деңгей • ${level.title}",
                    color = if (unlocked) AppText else AppText.copy(alpha = 0.48f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = if (unlocked) level.focus else "Алдыңғы деңгейді аяқта",
                    modifier = Modifier.padding(top = 3.dp),
                    color = AppText.copy(alpha = 0.58f),
                    fontSize = 11.sp,
                )
                if (unlocked) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 9.dp)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = AppGreen,
                        trackColor = Color.White.copy(alpha = 0.65f),
                    )
                }
            }
            if (unlocked) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("$stars/9 ⭐", color = AppText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("→", color = AppBlue, fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
private fun LevelHubScreen(
    level: CurriculumLevel,
    progress: ProgressRepository,
    progressRevision: Int,
    audio: SpeechAudio,
    onBack: () -> Unit,
    onOpenActivity: (GameActivity) -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val observeRevision = progressRevision
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
    ) {
        AppTopBar(title = "${level.id}-деңгей", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(levelGradient(level.id)))
                        .padding(20.dp),
                ) {
                    Text(level.emoji, fontSize = 48.sp)
                    Text(
                        text = level.title,
                        modifier = Modifier.padding(top = 8.dp),
                        color = AppText,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(level.subtitle, color = AppText.copy(alpha = 0.66f), fontSize = 14.sp)
                    Text(
                        text = level.focus,
                        modifier = Modifier.padding(top = 12.dp),
                        color = AppPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    LinearProgressIndicator(
                        progress = progress.levelProgress(level),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(8.dp)
                            .clip(CircleShape),
                        color = AppGreen,
                        trackColor = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            Text(
                text = "Ойындар",
                modifier = Modifier.padding(top = 22.dp, bottom = 8.dp),
                color = AppText,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            level.activities.forEachIndexed { index, activity ->
                ActivityCard(
                    number = index + 1,
                    activity = activity,
                    completed = progress.isCompleted(activity.id),
                    stars = progress.starsFor(activity.id),
                    bestScore = progress.bestScoreFor(activity.id),
                    onClick = { onOpenActivity(activity) },
                )
            }

            InfoStrip(
                emoji = "👪",
                text = "Ересекпен бірге ойнаса, пайдасы артады: баланың жауабын күтіңіз, сөзін бөлмеңіз, кейін бір-екі сөз қосып дұрыс үлгі беріңіз.",
                color = Color(0xFFF2E9FF),
            )
            if (!audio.ready) {
                InfoStrip(
                    emoji = "🔊",
                    text = "Дауыс қозғалтқышы дайындалып жатыр. Бірнеше секундтан кейін тыңдау түймелері белсенді болады.",
                    color = Color(0xFFFFF1D9),
                )
            }
            Text(
                text = "Бұл деңгейде уақыт шектеуі жоқ. Қате жауап жазаланбайды; дұрыс үлгі көрсетіліп, бала келесі тапсырмаға өте алады.",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 18.dp),
                color = AppText.copy(alpha = 0.48f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun ActivityCard(
    number: Int,
    activity: GameActivity,
    completed: Boolean,
    stars: Int,
    bestScore: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (completed) AppGreen.copy(alpha = 0.4f) else Color(0xFFE6EBEF)),
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (completed) Color(0xFFE4F8DE) else Color(0xFFFFF0D8)),
                contentAlignment = Alignment.Center,
            ) {
                Text(activity.emoji, fontSize = 31.sp)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 13.dp),
            ) {
                Text("$number. ${activity.title}", color = AppText, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = activity.subtitle,
                    modifier = Modifier.padding(top = 3.dp),
                    color = AppText.copy(alpha = 0.58f),
                    fontSize = 12.sp,
                )
                Text(
                    text = "${activity.rounds.size} тапсырма • ~${activity.minutes} минут",
                    modifier = Modifier.padding(top = 5.dp),
                    color = AppBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(if (completed) "✅" else "▶", fontSize = 21.sp)
                if (completed) Text("$stars⭐ • $bestScore%", color = AppText.copy(alpha = 0.55f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun WordLibraryScreen(audio: SpeechAudio, onBack: () -> Unit) {
    val groups = listOf(
        "Жемістер" to listOf("🍎" to "алма", "🍐" to "алмұрт", "🍌" to "банан", "🍉" to "қарбыз", "🍇" to "жүзім", "🍓" to "құлпынай"),
        "Жануарлар" to listOf("🐱" to "мысық", "🐶" to "күшік", "🐟" to "балық", "🐦" to "құс", "🐘" to "піл", "🦋" to "көбелек"),
        "Әрекеттер" to listOf("🏃" to "жүгіру", "🦘" to "секіру", "🥤" to "ішу", "🍽️" to "жеу", "📖" to "оқу", "😴" to "ұйықтау"),
        "Сезімдер" to listOf("😊" to "қуанышты", "😢" to "мұңды", "😨" to "қорықты", "😠" to "ренжіді", "😮" to "таңғалды", "😌" to "тыныш"),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
    ) {
        AppTopBar(title = "Сөздерді тыңдау", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Сөзді басып тыңда. Кейін сол сөзбен қысқа сөйлем ойлап көр.",
                color = AppText.copy(alpha = 0.67f),
                lineHeight = 20.sp,
            )
            groups.forEach { (title, words) ->
                Text(
                    text = title,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                    color = AppPurple,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    words.forEach { (emoji, word) ->
                        Card(
                            modifier = Modifier
                                .width(132.dp)
                                .height(128.dp)
                                .clickable(enabled = audio.ready) { audio.speak(word, slower = true) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(emoji, fontSize = 47.sp)
                                Text(word, color = AppText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("🔊 тыңдау", color = AppBlue, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            InfoStrip(
                emoji = "💡",
                text = "Тек қайталатумен шектелмеңіз. «Мысық» дегеннен кейін «Мысық не істеп жатыр?» деп қысқа әңгіме бастаңыз.",
                color = Color(0xFFEAF8E5),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ParentDashboardScreen(
    progress: ProgressRepository,
    progressRevision: Int,
    onBack: () -> Unit,
    onStatistics: () -> Unit,
    onGuide: () -> Unit,
    onChildInfo: () -> Unit = {},
) {
    @Suppress("UNUSED_VARIABLE") val observeRevision = progressRevision
    val tips = listOf(
        "Баланың жауабын күту үшін 5 секунд тыныштық беріңіз.",
        "Бір сөз айтса, оны 2–3 сөздік дұрыс сөйлемге кеңейтіңіз.",
        "Кітап суреттері туралы кім, не, қайда, неліктен деп сөйлесіңіз.",
        "Үйдегі заттарды топтаңыз: тағам, киім, ойыншық, жануар.",
        "Ұйқас, ән және буын шапалағын қысқа ойынға айналдырыңыз.",
        "Қатесін сынамай, дұрыс сөзді табиғи түрде қайталап үлгі беріңіз.",
        "Барлық қолданатын тілдеріңізде еркін сөйлесіңіз.",
    )
    val checked = remember { mutableStateListOf(false, false, false, false, false, false, false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
    ) {
        AppTopBar(title = "Ата-ана панелі", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProgressRing(
                    progress = progress.overallProgress,
                    color = AppGreen,
                    label = "Курс",
                    modifier = Modifier.weight(1f),
                )
                ProgressRing(
                    progress = progress.totalStars / 54f,
                    color = AppOrange,
                    label = "Жұлдыз",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatPill("🎮", "${progress.totalSessions}", "ойын сеансы", Modifier.weight(1f))
                StatPill("🗣️", "${progress.practicedRounds}", "тапсырма", Modifier.weight(1f))
                StatPill("📅", "${progress.activeDays}", "белсенді күн", Modifier.weight(1f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onStatistics,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                ) {
                    Text("📊 Нәтижелер")
                }
                Button(
                    onClick = onGuide,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPurple),
                ) {
                    Text("📘 Нұсқаулық")
                }
            }

            Button(
                onClick = onChildInfo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppOrange),
            ) {
                Text("🧒 Бала туралы ақпарат")
            }

            Text(
                text = "Күнделікті тілдік қолдау",
                modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
                color = AppPurple,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            tips.forEachIndexed { index, tip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { checked[index] = !checked[index] }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked[index],
                        onCheckedChange = { checked[index] = it },
                        colors = CheckboxDefaults.colors(checkedColor = AppGreen),
                    )
                    Text(
                        text = tip,
                        modifier = Modifier.weight(1f),
                        color = AppText.copy(alpha = if (checked[index]) 0.5f else 0.76f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
            }

            InfoStrip(
                emoji = "🩺",
                text = "Егер бала айтылғанды жиі түсінбесе, дыбысқа жауап бермесе, сөйлеуі кері кетсе немесе отбасы алаңдаса, педиатрға, есту маманына не логопедке жүгініңіз. Қосымша ерте көмек пайдалы болуы мүмкін.",
                color = Color(0xFFFFEDED),
            )
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Composable
private fun StatisticsScreen(
    progress: ProgressRepository,
    progressRevision: Int,
    onBack: () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val observeRevision = progressRevision
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
    ) {
        AppTopBar(title = "Оқу нәтижелері", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            Curriculum.levels.forEach { level ->
                val levelProgress = progress.levelProgress(level)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5EBF0)),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(level.emoji, fontSize = 30.sp)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp),
                            ) {
                                Text("${level.id}. ${level.title}", color = AppText, fontWeight = FontWeight.ExtraBold)
                                Text(level.focus, color = AppText.copy(alpha = 0.52f), fontSize = 10.sp)
                            }
                            Text("${progress.levelStars(level)}/9 ⭐", color = AppOrange, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = levelProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .height(7.dp)
                                .clip(CircleShape),
                            color = AppGreen,
                            trackColor = Color(0xFFE8EEF2),
                        )
                        level.activities.forEach { activity ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "${if (progress.isCompleted(activity.id)) "✓" else "○"} ${activity.title}",
                                    color = AppText.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                )
                                if (progress.isCompleted(activity.id)) {
                                    Text(
                                        text = "${progress.bestScoreFor(activity.id)}% • ${progress.attemptsFor(activity.id)} рет",
                                        color = AppBlue,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            InfoStrip(
                emoji = "ℹ️",
                text = "Ұпайлар клиникалық өлшем емес. Олар тек қолданбадағы тапсырмаларды аяқтау мен қайталауды көрсетеді.",
                color = Color(0xFFEAF8FF),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ParentGuideScreen(
    progress: ProgressRepository,
    onBack: () -> Unit,
    onReset: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
    ) {
        AppTopBar(title = "Ата-ана нұсқаулығы", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
            GuideSection(
                emoji = "↔️",
                title = "Кезектесіп сөйлесу",
                text = "Баланың назарына еріңіз: ол бір нәрсені көрсетсе немесе айтса, жауап беріп, атын атаңыз. Әңгіме теннис сияқты кезекпен жүрсін.",
            )
            GuideSection(
                emoji = "➕",
                title = "Сөзін кеңейту",
                text = "Бала «қызыл доп» десе, «Иә, қызыл доп домалап барады» деп бір қадам күрделірек үлгі беріңіз. Қайталатуға мәжбүрлемеңіз.",
            )
            GuideSection(
                emoji = "📖",
                title = "Бірге оқу және әңгімелеу",
                text = "Суретті кітапты бірге қарап, «Не болып жатыр?», «Кейін не болады?», «Неліктен?» деп сөйлесіңіз. Оқиғаны қайталап айтуға немесе ойнап көрсетуге болады.",
            )
            GuideSection(
                emoji = "🎵",
                title = "Дыбыс пен сөз ойыны",
                text = "Ұйқас, алғашқы дыбыс және буын шапалағы сөздің дыбыстық құрылымын байқауға көмектеседі. Бірақ қолданба нақты дыбыс бұзылысын анықтамайды және емдемейді.",
            )
            GuideSection(
                emoji = "🌍",
                title = "Көптілділік — кедергі емес",
                text = "Үйде өзіңіз еркін сөйлейтін тілдерді қолданыңыз. Тілдерді араластыру көптілді дамудың қалыпты бөлігі болуы мүмкін.",
            )
            GuideSection(
                emoji = "⏱️",
                title = "Қысқа және шынайы өмірмен байланысты",
                text = "Күніне 10–15 минут бірге ойнау жеткілікті. Әр 2–3 ойыннан кейін экраннан тыс үзіліс жасап, сөздерді тамақта, киінуде, серуенде және ойында қолданыңыз.",
            )
            GuideSection(
                emoji = "🔐",
                title = "Құпиялық",
                text = "Нәтижелер тек осы құрылғыдағы SharedPreferences ішінде сақталады. Дауыс жаттығуында қолданба аудионы сақтамайды; тану Android жүйесінің сөйлеу қызметіне байланысты және кей құрылғыда желіні қолдануы мүмкін. Микрофон міндетті емес.",
            )
            GuideSection(
                emoji = "🩺",
                title = "Қашан маманға жүгіну керек",
                text = "Бала дыбысқа жауап бермесе, айтылғанды түсінуі немесе ойын жеткізуі алаңдатса, бұрынғы дағдыларын жоғалтса, педиатрға, аудиологқа немесе логопедке жүгініңіз. Естуді тексеру маңызды болуы мүмкін.",
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                color = Color(0xFFFFF1DD),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    text = "Маңызды: бұл білім беру қолданбасы. Ол диагноз қоймайды, жеке терапия жоспарын жасамайды және логопед қызметін алмастырмайды.",
                    modifier = Modifier.padding(16.dp),
                    color = AppText,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp,
                )
            }
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 28.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFCC6C6C)),
                enabled = progress.completedCount > 0,
            ) {
                Text("Оқу нәтижелерін өшіру", color = Color(0xFFB44545))
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Нәтижелерді өшіру керек пе?") },
            text = { Text("Барлық жұлдыздар, аяқталған ойындар және ашылған деңгейлер өшеді. Бұл әрекетті қайтару мүмкін емес.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmReset = false
                        onReset()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB44545)),
                ) {
                    Text("Өшіру")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Бас тарту") }
            },
        )
    }
}

@Composable
private fun GuideSection(emoji: String, title: String, text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE6EBEF)),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text(emoji, fontSize = 28.sp)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title, color = AppBlue, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Text(
                    text = text,
                    modifier = Modifier.padding(top = 5.dp),
                    color = AppText.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }
        }
    }
}

@Composable
private fun ProgressRing(
    progress: Float,
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(124.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = color.copy(alpha = 0.17f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(13.dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(13.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%", color = color, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(label, color = AppText.copy(alpha = 0.56f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StatPill(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, Color(0xFFE6EBEF)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, fontSize = 21.sp)
            Text(value, color = AppText, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(label, color = AppText.copy(alpha = 0.52f), fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun InfoStrip(emoji: String, text: String, color: Color) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        color = color,
        shape = RoundedCornerShape(17.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Text(emoji, fontSize = 23.sp)
            Text(
                text = text,
                modifier = Modifier.padding(start = 10.dp),
                color = AppText.copy(alpha = 0.72f),
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: String? = null,
    onTrailing: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            BackButton(onClick = onBack)
        } else {
            Spacer(modifier = Modifier.size(42.dp))
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            color = AppText,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        if (trailing != null) {
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clickable(onClick = onTrailing),
                color = Color(0xFFEAF8FF),
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) { Text(trailing, fontSize = 21.sp) }
            }
        } else {
            Spacer(modifier = Modifier.size(42.dp))
        }
    }
    Divider(color = Color(0xFFE9EEF2), thickness = 1.dp)
}

@Composable
internal fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(42.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color(0xFFF0F4F7),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "‹",
                color = AppText.copy(alpha = 0.68f),
                fontSize = 35.sp,
                fontWeight = FontWeight.Light,
            )
        }
    }
}

private fun levelGradient(levelId: Int): List<Color> = when (levelId) {
    1 -> listOf(Color(0xFFFFE2A8), Color(0xFFFFF3D8))
    2 -> listOf(Color(0xFFDDF8D6), Color(0xFFF0FFE9))
    3 -> listOf(Color(0xFFD8F1FF), Color(0xFFEEF9FF))
    4 -> listOf(Color(0xFFE7D8FF), Color(0xFFF6F0FF))
    5 -> listOf(Color(0xFFFFD8E8), Color(0xFFFFF0F6))
    else -> listOf(Color(0xFFFFE9A8), Color(0xFFFFF9DA))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildInfoScreen(
    onBack: () -> Unit,
    onPinUpdated: (String) -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repository.getChildInfo().onSuccess { profile ->
            if (profile != null) {
                name = profile.name
                age = profile.age
                pin = profile.parentPin
            }
            isLoading = false
        }.onFailure {
            isLoading = false
            message = "Деректерді жүктеу қатесі"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
    ) {
        AppTopBar(title = "Бала туралы ақпарат", onBack = onBack)

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Мұнда баланың есімі мен жасын өзгерте аласыз. Бұл мәліметтер оқу барысын жекелендіруге көмектеседі.",
                    color = AppText.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Баланың есімі") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Баланың жасы") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text("Ата-ана PIN-коды (4 сан)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                message?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(it, color = if (it.contains("қате")) Color.Red else AppGreen, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            message = null
                            repository.updateChildInfo(name, age, pin)
                                .onSuccess {
                                    message = "Мәліметтер сақталды"
                                    isSaving = false
                                    onPinUpdated(pin)
                                }
                                .onFailure {
                                    message = "Сақтау қатесі"
                                    isSaving = false
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlue)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Сақтау", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentalGate(
    correctPin: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ата-ана бақылауы", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Ата-ана бөліміне өту үшін 4 таңбалы PIN-кодты енгізіңіз.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) input = it },
                    label = { Text("PIN-код") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(150.dp),
                    isError = error
                )
                if (error) {
                    Text(
                        "PIN-код қате, қайта көріңіз",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (input == correctPin) {
                        onSuccess()
                    } else {
                        error = true
                        input = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppBlue)
            ) {
                Text("Растау")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Бас тарту")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
