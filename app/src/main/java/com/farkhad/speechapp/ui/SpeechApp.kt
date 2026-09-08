package com.farkhad.speechapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farkhad.speechapp.audio.SpeechAudio
import com.farkhad.speechapp.audio.rememberSpeechAudio
import com.farkhad.speechapp.assessment.PersonalizedRouteEngine
import com.farkhad.speechapp.data.FirebaseRepository
import com.farkhad.speechapp.data.ProgressRepository
import com.farkhad.speechapp.model.Curriculum
import com.farkhad.speechapp.model.CurriculumLevel
import com.farkhad.speechapp.model.GameActivity
import com.farkhad.speechapp.model.VocabularyWord
import com.farkhad.speechapp.ui.theme.*
import kotlinx.coroutines.launch

private enum class AppScreen {
    Role,
    ChildHome,
    Level,
    Game,
    WordLibrary,
    Assessment,
    SoundMap,
    PersonalizedRoute,
    FaceMap,
    ParentDashboard,
    Statistics,
    ParentGuide,
    ChildInfo,
}

@Composable
fun SpeechApp(
    userId: String,
    isDemoMode: Boolean = false,
    onSignOut: () -> Unit = {},
    onSignOutEverywhere: () -> Unit = {},
) {
    val context = LocalContext.current
    val progress = remember(userId) { ProgressRepository(context.applicationContext, userId) }
    val audio = rememberSpeechAudio()
    val progressRevision = progress.revision
    val scope = rememberCoroutineScope()

    var screenName by rememberSaveable { mutableStateOf(AppScreen.Role.name) }
    var selectedLevelId by rememberSaveable { mutableStateOf(1) }
    var selectedActivityId by rememberSaveable { mutableStateOf(Curriculum.level(1).activities.first().id) }
    var completedThisSession by rememberSaveable { mutableStateOf(0) }
    var showBreakReminder by rememberSaveable { mutableStateOf(false) }

    var showParentalGate by remember { mutableStateOf(false) }
    var parentalGateTarget by remember { mutableStateOf<AppScreen?>(null) }
    var correctPin by remember { mutableStateOf("1234") }
    // Presentation demo is intentionally local: do not initialize Firebase on
    // its hot path. Besides avoiding unnecessary network work, this prevents a
    // slow Play Services startup from delaying the first screen transition.
    val repository = remember(isDemoMode) {
        if (isDemoMode) null else FirebaseRepository()
    }

    LaunchedEffect(userId, isDemoMode) {
        if (!isDemoMode) {
            repository?.getChildInfo()?.onSuccess { profile ->
                if (profile != null) {
                    correctPin = profile.parentPin
                }
            }
            // Sync learned words from Firebase
            repository?.getLearnedWords()?.onSuccess { wordIds ->
                progress.updateLearnedWords(wordIds)
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
            AppScreen.Role -> RoleSelectionScreen(
                onChild = { navigate(AppScreen.ChildHome) },
                onParent = { navigate(AppScreen.ParentDashboard) },
                onSignOut = onSignOut,
                onSignOutEverywhere = onSignOutEverywhere,
                isDemoMode = isDemoMode,
            )

            AppScreen.ChildHome -> ChildHomeScreen(
                progress = progress,
                progressRevision = progressRevision,
                audio = audio,
                onOpenLevel = { levelId ->
                    selectedLevelId = levelId
                    navigate(AppScreen.Level)
                },
                onOpenWords = { navigate(AppScreen.WordLibrary) },
                onOpenAssessment = { navigate(AppScreen.Assessment) },
                onOpenSoundMap = { navigate(AppScreen.SoundMap) },
                onOpenTraining = { navigate(AppScreen.PersonalizedRoute) },
                onOpenFaceMap = { navigate(AppScreen.FaceMap) },
                isDemoMode = isDemoMode,
                onBack = { requestParentalGate(AppScreen.Role) }
            )

            AppScreen.Level -> LevelHubScreen(
                level = Curriculum.level(selectedLevelId),
                progress = progress,
                progressRevision = progressRevision,
                audio = audio,
                repository = repository,
                scope = scope,
                onBack = { navigate(AppScreen.ChildHome) },
                onOpenActivity = { activity ->
                    selectedActivityId = activity.id
                    navigate(AppScreen.Game)
                },
            )

            AppScreen.Game -> {
                val activity = Curriculum.activity(selectedActivityId)
                val next = Curriculum.nextActivity(activity.id)
                val nextIsUnlocked = next != null && (
                    isDemoMode || progress.isLevelUnlocked(Curriculum.levelForActivity(next.id).id)
                )
                GameSessionScreen(
                    activity = activity,
                    progress = progress,
                    audio = audio,
                    repository = repository,
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
                        if (next == null || (
                                !isDemoMode &&
                                    !progress.isLevelUnlocked(Curriculum.levelForActivity(next.id).id)
                            )
                        ) {
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
                progress = progress,
                audio = audio,
                onBack = { navigate(AppScreen.ChildHome) },
            )

            AppScreen.Assessment -> ExpressAssessmentScreen(
                audio = audio,
                progress = progress,
                onBack = { navigate(AppScreen.ChildHome) },
            )

            AppScreen.SoundMap -> SoundMapScreen(
                progress = progress,
                progressRevision = progressRevision,
                audio = audio,
                onBack = { navigate(AppScreen.ChildHome) },
                onStartAssessment = { navigate(AppScreen.Assessment) },
                onOpenLevel = { levelId ->
                    selectedLevelId = levelId
                    navigate(AppScreen.Level)
                },
            )

            AppScreen.PersonalizedRoute -> PersonalizedRouteScreen(
                progress = progress,
                progressRevision = progressRevision,
                audio = audio,
                onBack = { navigate(AppScreen.ChildHome) },
                onStartAssessment = { navigate(AppScreen.Assessment) },
                onOpenSoundMap = { navigate(AppScreen.SoundMap) },
                onOpenActivity = { activity ->
                    selectedActivityId = activity.id
                    selectedLevelId = Curriculum.levelForActivity(activity.id).id
                    navigate(AppScreen.Game)
                },
            )

            AppScreen.FaceMap -> FaceMapScreen(
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
                repository = repository,
                isDemoMode = isDemoMode,
                onBack = { navigate(AppScreen.ParentDashboard) },
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
    onSignOut: () -> Unit,
    onSignOutEverywhere: () -> Unit,
    isDemoMode: Boolean,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        NetworkImageBackground(
            url = "https://images.unsplash.com/photo-1542332213-9b5a5a3fad35?auto=format&fit=crop&q=80&w=1000",
            overlayColor = Color.Black.copy(alpha = 0.3f)
        )
        KazakhPatternBackground(color = Color.White.copy(alpha = 0.15f))
        
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp),
        ) {
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
            ) {
                Text(if (isDemoMode) "Демодан шығу" else "Шығу / Выйти")
            }
            if (!isDemoMode) {
                TextButton(
                    onClick = onSignOutEverywhere,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                ) {
                    Text(
                        text = "Барлық құрылғылардан\nНа всех устройствах",
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "Тыңда • ойна • сөйле • әңгімеле",
                modifier = Modifier.padding(top = 8.dp),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(38.dp))
            RoleButton(
                text = "🧒  Балалар Әлемі",
                subtitle = "Ойын арқылы үйрену",
                color = AppOrange,
                onClick = onChild,
            )
            Spacer(modifier = Modifier.height(16.dp))
            RoleButton(
                text = "👪  Ата-ана бұрышы",
                subtitle = "Даму статистикасы",
                color = AppGreen,
                onClick = onParent,
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Қазақша білім беру қосымшасы",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
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
        contentPadding = PaddingValues(horizontal = 20.dp),
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
    onOpenLevel: (Int) -> Unit,
    onOpenWords: () -> Unit,
    onOpenAssessment: () -> Unit,
    onOpenSoundMap: () -> Unit,
    onOpenTraining: () -> Unit,
    onOpenFaceMap: () -> Unit,
    isDemoMode: Boolean,
    onBack: () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val observeRevision = progressRevision
    val trainingRoute = remember(progressRevision) {
        PersonalizedRouteEngine.build(progress.assessedSoundScores)
    }
    
    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        NetworkImageBackground(
            url = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&q=80&w=1000",
            alpha = 0.15f
        )
        KazakhPatternBackground(color = AppBlue.copy(alpha = 0.05f))
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            AppTopBar(
                title = "Сөйлеу Саяхаты",
                onBack = null,
                trailing = "🏠",
                onTrailing = onBack,
            )
            
            Box(modifier = Modifier.weight(1f)) {
                LevelRouteMap(
                    levels = Curriculum.levels,
                    progress = progress,
                    unlockAll = isDemoMode,
                    onOpenLevel = onOpenLevel
                )
                
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(onClick = onOpenTraining),
                        color = Color(0xFF3953D7),
                        shadowElevation = 12.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("🧭", fontSize = 36.sp)
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(
                                    "Твоя тренировка сегодня",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                )
                                Text(
                                    if (trainingRoute.needsAssessment) {
                                        "Алдымен экспресс-бағалаудан өт"
                                    } else {
                                        "Дыбыстар: ${trainingRoute.focusSounds.joinToString(" • ") { it.uppercase() }}  ·  ${trainingRoute.activities.size} ойын"
                                    },
                                    color = Color.White.copy(alpha = 0.80f),
                                    fontSize = 11.sp,
                                )
                            }
                            Surface(color = AppOrange, shape = RoundedCornerShape(50)) {
                                Text(
                                    "СТАРТ",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(onClick = onOpenAssessment),
                        color = AppBlue,
                        shadowElevation = 12.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("🎙️", fontSize = 34.sp)
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(
                                    "Сөйлеуді тексеру",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                )
                                Text(
                                    "Экспресс-оценка речи + Face Map",
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 11.sp,
                                )
                            }
                            Surface(color = AppOrange, shape = RoundedCornerShape(50)) {
                                Text(
                                    "60 сек",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(onClick = onOpenFaceMap),
                        color = AppPurple,
                        shadowElevation = 10.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("✨", fontSize = 30.sp)
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(
                                    "Face Map",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                )
                                Text(
                                    "Жаңа артикуляциялық айна",
                                    color = Color.White.copy(alpha = 0.78f),
                                    fontSize = 11.sp,
                                )
                            }
                            Surface(color = AppOrange, shape = RoundedCornerShape(50)) {
                                Text(
                                    "NEW",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(onClick = onOpenSoundMap),
                            color = AppOrange,
                            shadowElevation = 8.dp,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "🎯 Дыбыстар",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(onClick = onOpenWords),
                            color = AppGreen,
                            shadowElevation = 8.dp,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "🔊 Сөздер",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelRouteMap(
    levels: List<CurriculumLevel>,
    progress: ProgressRepository,
    unlockAll: Boolean,
    onOpenLevel: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState, reverseScrolling = true)
            .padding(top = 100.dp, bottom = 370.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        levels.reversed().forEachIndexed { index, level ->
            val unlocked = unlockAll || progress.isLevelUnlocked(level.id)
            val levelProgress = progress.levelProgress(level)
            val stars = progress.levelStars(level)
            
            val alignment = when (index % 3) {
                0 -> Alignment.Center
                1 -> Alignment.CenterStart
                2 -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 60.dp),
                contentAlignment = alignment
            ) {
                LevelNode(
                    level = level,
                    unlocked = unlocked,
                    progress = levelProgress,
                    stars = stars,
                    onClick = { if (unlocked) onOpenLevel(level.id) }
                )
            }
            
            if (index < levels.lastIndex) {
                MapPathConnector(index)
            }
        }
    }
    
    LaunchedEffect(Unit) {
        val currentLevelIndex = levels.indexOfFirst { !progress.isLevelUnlocked(it.id) }.let { if (it == -1) levels.size - 1 else it }
        // Potential auto-scroll logic here
    }
}

@Composable
private fun MapPathConnector(index: Int) {
    Canvas(modifier = Modifier.height(120.dp).fillMaxWidth(0.6f)) {
        val w = size.width
        val h = size.height
        val path = Path()
        
        val startX = when (index % 3) {
            0 -> w / 2
            1 -> w * 0.2f
            2 -> w * 0.8f
            else -> w / 2
        }
        
        val endX = when ((index + 1) % 3) {
            0 -> w / 2
            1 -> w * 0.2f
            2 -> w * 0.8f
            else -> w / 2
        }

        path.moveTo(startX, 0f)
        path.cubicTo(startX, h * 0.5f, endX, h * 0.5f, endX, h)
        
        drawPath(
            path = path,
            color = Color(0xFFD1D9E0).copy(alpha = 0.6f),
            style = Stroke(
                width = 8.dp.toPx(), 
                cap = StrokeCap.Round, 
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 25f))
            )
        )
    }
}

@Composable
private fun LevelNode(
    level: CurriculumLevel,
    unlocked: Boolean,
    progress: Float,
    stars: Int,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(140.dp)
            .clickable(enabled = unlocked, onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp)
        ) {
            Surface(
                modifier = Modifier.size(86.dp).padding(top = 6.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.15f)
            ) {}
            
            Surface(
                modifier = Modifier.size(86.dp),
                shape = CircleShape,
                color = if (unlocked) Color.White else Color(0xFFE0E0E0),
                border = BorderStroke(4.dp, if (unlocked) levelNodeColor(level.id) else Color.White),
                shadowElevation = if (unlocked) 6.dp else 0.dp
            ) {
                if (unlocked) {
                    NetworkImageBackground(url = level.imageUrl, alpha = 0.7f)
                }
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (unlocked) level.emoji else "🔒", 
                        fontSize = 42.sp,
                        modifier = Modifier.graphicsLayer {
                            if (!unlocked) alpha = 0.5f
                        }
                    )
                }
            }
            
            if (unlocked && progress > 0f) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    drawArc(
                        color = AppGreen,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            if (unlocked && stars > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 4.dp).size(32.dp),
                    shape = CircleShape,
                    color = AppGold,
                    border = BorderStroke(2.dp, Color.White),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$stars", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            color = if (unlocked) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (unlocked) levelNodeColor(level.id) else Color.Transparent)
        ) {
            Text(
                text = level.title,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = if (unlocked) AppText else AppText.copy(alpha = 0.5f),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun levelNodeColor(levelId: Int): Color = when (levelId) {
    1 -> Color(0xFF81D4FA)
    2 -> Color(0xFFA5D6A7)
    3 -> Color(0xFFFFE082)
    4 -> Color(0xFFCE93D8)
    5 -> Color(0xFFFFAB91)
    else -> Color(0xFFB0BEC5)
}

@Composable
private fun LevelHubScreen(
    level: CurriculumLevel,
    progress: ProgressRepository,
    progressRevision: Int,
    audio: SpeechAudio,
    repository: FirebaseRepository?,
    scope: kotlinx.coroutines.CoroutineScope,
    onBack: () -> Unit,
    onOpenActivity: (GameActivity) -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val observeRevision = progressRevision
    Box(modifier = Modifier.fillMaxSize()) {
        NetworkImageBackground(url = level.imageUrl, alpha = 0.2f)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            AppTopBar(title = level.title, onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(level.emoji, fontSize = 56.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = level.title,
                                    color = AppText,
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                                Text(level.subtitle, color = AppText.copy(alpha = 0.66f), fontSize = 14.sp)
                            }
                        }
                        
                        Text(
                            text = level.focus,
                            modifier = Modifier.padding(top = 12.dp),
                            color = AppPurple,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        
                        LinearProgressIndicator(
                            progress = progress.levelProgress(level),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(10.dp)
                                .clip(CircleShape),
                            color = AppGreen,
                            trackColor = AppBlue.copy(alpha = 0.1f),
                        )
                    }
                }

                if (level.vocabulary.isNotEmpty()) {
                    Text(
                        text = "Жаңа сөздер",
                        modifier = Modifier.padding(top = 22.dp, bottom = 12.dp),
                        color = AppText,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        level.vocabulary.forEach { word ->
                        VocabularyCard(
                            word = word,
                            learned = progress.learnedWordIds.contains(word.id),
                            audio = audio,
                            onLearn = { 
                                progress.markWordAsLearned(word.id)
                                scope.launch {
                                    repository?.saveLearnedWord(word.id)
                                }
                            }
                        )
                    }
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
                    emoji = "⭐",
                    text = "Барлық ойындарды аяқтап, жұлдыздарды жина! Сенің қолыңнан келеді!",
                    color = Color(0xFFFFF9E6),
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
private fun WordLibraryScreen(
    progress: ProgressRepository,
    audio: SpeechAudio, 
    onBack: () -> Unit
) {
    val learnedIds = progress.learnedWordIds
    val learnedWords = Curriculum.levels.flatMap { it.vocabulary }.filter { it.id in learnedIds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .statusBarsPadding(),
    ) {
        AppTopBar(title = "Сөздік қорым", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            if (learnedWords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏜️", fontSize = 64.sp)
                        Text(
                            "Әзірге сөздер жоқ.",
                            color = AppText.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        Text(
                            "Деңгейлерді өтіп, жаңа сөздерді аш!",
                            color = AppText.copy(alpha = 0.4f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "Сен ${learnedWords.size} сөз үйрендің! Қайталап көр:",
                    color = AppText.copy(alpha = 0.67f),
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    mainAxisSpacing = 12.dp,
                    crossAxisSpacing = 12.dp
                ) {
                    learnedWords.forEach { word ->
                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { audio.speak(word.word) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            border = BorderStroke(1.dp, AppBlue.copy(alpha = 0.1f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(AppBlue.copy(alpha = 0.05f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(word.emoji, fontSize = 32.sp)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        word.word, 
                                        color = AppText, 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AppBlue.copy(alpha = 0.1f))
                                            .clickable { audio.speak(word.word) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "🔊 Тыңдау", 
                                            color = AppBlue, 
                                            fontSize = 10.sp, 
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            InfoStrip(
                emoji = "🚀",
                text = "Сөздерді тыңдап, оларды достарыңа немесе ойыншықтарыңа айтып бер! Сенің сөздерің сиқырлы!",
                color = Color(0xFFEAF8FF),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        var x = 0
        var y = 0
        var rowMaxHeight = 0
        val positions = mutableListOf<Pair<Int, Int>>()
        
        placeables.forEach { placeable ->
            if (x + placeable.width > constraints.maxWidth && x > 0) {
                x = 0
                y += rowMaxHeight + crossAxisSpacing.roundToPx()
                rowMaxHeight = 0
            }
            positions.add(x to y)
            rowMaxHeight = maxOf(rowMaxHeight, placeable.height)
            x += placeable.width + mainAxisSpacing.roundToPx()
        }
        
        val totalHeight = if (placeables.isEmpty()) 0 else y + rowMaxHeight
        layout(constraints.maxWidth, totalHeight) {
            placeables.forEachIndexed { index, placeable ->
                val (px, py) = positions[index]
                placeable.place(px, py)
            }
        }
    }
}

@Composable
private fun VocabularyCard(
    word: VocabularyWord,
    learned: Boolean,
    audio: SpeechAudio,
    onLearn: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(150.dp)
            .height(170.dp)
            .clickable { 
                showDialog = true
                audio.speak(word.word)
                if (!learned) onLearn()
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (learned) Color.White else Color(0xFFF5F7F9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (learned) 4.dp else 1.dp),
        border = if (learned) BorderStroke(2.dp, AppGreen.copy(alpha = 0.4f)) else BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (learned) AppGreen.copy(alpha = 0.1f) else Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(word.emoji, fontSize = 46.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = word.word,
                    color = AppText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    textAlign = TextAlign.Center
                )
                if (learned) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text("✅ Білемін", color = AppGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Listen Icon in top right
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clickable { audio.speak(word.word) },
                color = AppBlue.copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🔊", fontSize = 14.sp)
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(
                    onClick = { showDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Жарайды")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { audio.speak(word.word) },
                    border = BorderStroke(2.dp, AppBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("🔊 Тыңдау", color = AppBlue)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = AppBlue.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(word.emoji, fontSize = 28.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(word.word, fontWeight = FontWeight.Black, fontSize = 24.sp, color = AppText)
                }
            },
            text = {
                Column {
                    Text(word.definition, fontSize = 16.sp, color = AppText)
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = AppBlue.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppBlue.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = "Үлгі: \"${word.example}\"",
                            modifier = Modifier.padding(12.dp),
                            color = AppBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
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

            val diffCount = progress.getDifficultWords().size
            if (diffCount > 0) {
                InfoStrip(
                    emoji = "🗣️",
                    text = "Балаңыз $diffCount сөзді айтуда қиындық көріп жүр. Нәтижелер бөлімінен толығырақ көре аласыз.",
                    color = Color(0xFFFFF3E0)
                )
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
                text = "Ұпайлар клиникалық өлшем емес. Олар тек қолданбадағы тапсырмаларды аяқтау мен қайталатуды көрсетеді.",
                color = Color(0xFFEAF8FF),
            )
            
            val difficultWords = progress.getDifficultWords()
            if (difficultWords.isNotEmpty()) {
                Text(
                    text = "Қайталауды қажет ететін сөздер",
                    modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
                    color = AppPurple,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5EBF0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        difficultWords.forEach { (word, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(word, color = AppText, fontWeight = FontWeight.Bold)
                                Text("$count рет қате", color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            
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
                text = "Ата-ана аккаунты Firebase Authentication арқылы басқарылады. Бала туралы негізгі ақпарат пен үйренген сөздер Firebase ішінде, ал толық жаттығу нәтижелері осы құрылғыда сақталады. Қолданба дауыс жазбасын сақтамайды; сөйлеуді тану Android қызметіне байланысты және желіні қолдануы мүмкін.",
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
private fun StatPill(emoji: String, value: String, label: String, modifier: Modifier = Modifier, color: Color = AppText) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.9f),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, fontSize = 21.sp)
            Text(value, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(label, color = color.copy(alpha = 0.7f), fontSize = 9.sp, textAlign = TextAlign.Center)
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Brush.horizontalGradient(listOf(AppBlue, AppTurquoise.copy(alpha = 0.8f))))
    ) {
        KazakhPatternBackground(
            modifier = Modifier.fillMaxSize(),
            color = Color.White.copy(alpha = 0.15f),
            strokeWidth = 1f
        )
        
        Row(
            modifier = Modifier
                .fillMaxSize()
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
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            if (trailing != null) {
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable(onClick = onTrailing),
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape,
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(trailing, fontSize = 21.sp) }
                }
            } else {
                Spacer(modifier = Modifier.size(42.dp))
            }
        }
    }
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
    repository: FirebaseRepository?,
    isDemoMode: Boolean,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(repository != null) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository) {
        repository?.getChildInfo()?.onSuccess { profile ->
            if (profile != null) {
                name = profile.name
                age = profile.age
            }
            isLoading = false
        }?.onFailure {
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
                    if (isDemoMode) {
                        "Демо режимінде бұл бөлім тек көрсетіледі. Деректер Firebase-ке жазылмайды."
                    } else {
                        "Мұнда баланың есімі мен жасын өзгерте аласыз. Ата-ана PIN-коды бұл нұсқада бұлтқа сақталмайды."
                    },
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
                            repository?.updateChildInfo(name, age)
                                ?.onSuccess {
                                    message = "Мәліметтер сақталды"
                                    isSaving = false
                                }
                                ?.onFailure {
                                    message = "Сақтау қатесі"
                                    isSaving = false
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isSaving && repository != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlue)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            if (isDemoMode) "Демода сақтау өшірулі" else "Сақтау",
                            fontWeight = FontWeight.Bold,
                        )
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
