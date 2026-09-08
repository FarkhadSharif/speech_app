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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VolumeUp
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.farkhad.speechapp.data.ExerciseSessionReport
import com.farkhad.speechapp.data.ExerciseSessionSource
import com.farkhad.speechapp.data.ParentReflectionEngine
import com.farkhad.speechapp.data.ProgressRepository
import com.farkhad.speechapp.model.Curriculum
import com.farkhad.speechapp.model.CurriculumLevel
import com.farkhad.speechapp.model.GameActivity
import com.farkhad.speechapp.model.VocabularyWord
import com.farkhad.speechapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    ParentReflection,
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
            repository?.getParentReflections()?.onSuccess { reflections ->
                progress.mergeParentReflections(reflections)
            }
            repository?.getExerciseSessionReports()?.onSuccess { reports ->
                progress.mergeExerciseSessionReports(reports)
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
                    onSaveResult = { report ->
                        progress.completeActivity(activity, report.score)
                        progress.saveExerciseSessionReport(report)
                        scope.launch { repository?.saveExerciseSessionReport(report) }
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
                onReport = { report ->
                    progress.saveExerciseSessionReport(report)
                    scope.launch { repository?.saveExerciseSessionReport(report) }
                },
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
                onAnalysisCompleted = { steps, durationSeconds ->
                    val completedAt = System.currentTimeMillis()
                    val report = ExerciseSessionReport(
                        sessionId = "${completedAt}_face_map",
                        activityId = "face_map",
                        activityTitle = "Артикуляциялық айна",
                        levelId = 0,
                        levelTitle = "Face Map",
                        completedAt = completedAt,
                        durationSeconds = durationSeconds,
                        score = steps.map { it.score }.average().takeIf { !it.isNaN() }?.toInt() ?: 0,
                        attemptNumber = progress.exerciseSessionReports.count {
                            it.activityId == "face_map"
                        } + 1,
                        source = ExerciseSessionSource.FACE_MAP,
                        steps = steps,
                    )
                    progress.saveExerciseSessionReport(report)
                    scope.launch { repository?.saveExerciseSessionReport(report) }
                },
            )

            AppScreen.ParentDashboard -> ParentDashboardScreen(
                progress = progress,
                progressRevision = progressRevision,
                onBack = { navigate(AppScreen.Role) },
                onReflection = { navigate(AppScreen.ParentReflection) },
                onStatistics = { navigate(AppScreen.Statistics) },
                onGuide = { navigate(AppScreen.ParentGuide) },
                onChildInfo = { navigate(AppScreen.ChildInfo) }
            )

            AppScreen.ParentReflection -> ParentReflectionScreen(
                progress = progress,
                progressRevision = progressRevision,
                repository = repository,
                onBack = { navigate(AppScreen.ParentDashboard) },
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
            icon = {
                Icon(Icons.Outlined.Spa, contentDescription = null, tint = AppGreen, modifier = Modifier.size(34.dp))
            },
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
    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .padding(top = 18.dp, bottom = 18.dp)
                .background(AppRed),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 26.dp, end = 22.dp, top = 20.dp, bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        "SÓYLE",
                        color = AppText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.8.sp,
                    )
                    Text(
                        "БАЛАМЕН БІРГЕ СӨЙЛЕСЕМІЗ",
                        color = AppText.copy(alpha = 0.48f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.textButtonColors(contentColor = AppText.copy(alpha = 0.62f)),
                ) {
                    Text(
                        if (isDemoMode) "Демодан шығу" else "Шығу",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(196.dp).padding(top = 24.dp)) {
                Column(modifier = Modifier.align(Alignment.TopStart)) {
                    Text(
                        "БҮГІН БІРГЕ ЖАТТЫҒАМЫЗ",
                        color = AppRed,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.9.sp,
                    )
                    Text(
                        "Тыңдаймыз.\nҚайталаймыз.\nСөйлесеміз.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = AppText,
                        style = MaterialTheme.typography.headlineLarge,
                        fontSize = 36.sp,
                        lineHeight = 40.sp,
                    )
                }
                Text(
                    "Ә",
                    modifier = Modifier.align(Alignment.BottomEnd),
                    color = AppRed.copy(alpha = 0.16f),
                    fontSize = 126.sp,
                    fontWeight = FontWeight.Normal,
                )
            }

            Text(
                "Балаға — қысқа әрі түсінікті жаттығу. Ата-анаға — нақты динамика мен келесі қадам.",
                modifier = Modifier.fillMaxWidth(0.86f),
                color = AppText.copy(alpha = 0.64f),
                style = MaterialTheme.typography.bodyMedium,
            )

            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                color = AppNavy,
                shape = RoundedCornerShape(2.dp),
            ) {
                Text(
                    "Ә   Ғ   Қ   Ң   Ө   Ұ   Ү   І",
                    modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp),
                    color = AppBackground,
                    fontSize = 16.sp,
                    letterSpacing = 1.1.sp,
                )
            }

            Text(
                "Кім жалғастырады?",
                modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
                color = AppText,
                style = MaterialTheme.typography.titleLarge,
            )
            RoleButton(
                symbol = "Б",
                title = "Балаға арналған бөлім",
                subtitle = "Ойындар · дыбыстар · жеке маршрут",
                color = AppRed,
                onClick = onChild,
            )
            RoleButton(
                symbol = "А",
                title = "Ата-анаға арналған бөлім",
                subtitle = "Нәтижелер · динамика · ұсыныстар",
                color = AppBlue,
                onClick = onParent,
            )

            Divider(modifier = Modifier.padding(top = 18.dp), color = AppOutline)
            Text(
                "Деректер ата-ана аккаунтымен қорғалған. Қосымша медициналық диагноз қоймайды.",
                modifier = Modifier.padding(top = 12.dp),
                color = AppText.copy(alpha = 0.50f),
                style = MaterialTheme.typography.bodySmall,
            )
            if (!isDemoMode) {
                TextButton(
                    onClick = onSignOutEverywhere,
                    modifier = Modifier.padding(top = 2.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = AppText.copy(alpha = 0.58f)),
                ) {
                    Text("Барлық құрылғылардан шығу / Выйти везде", fontSize = 11.sp)
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun RoleButton(
    symbol: String,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, AppOutline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                symbol,
                modifier = Modifier.width(42.dp),
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = AppText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    subtitle,
                    modifier = Modifier.padding(top = 3.dp),
                    color = AppText.copy(alpha = 0.54f),
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
            Text("Кіру", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            AppTopBar(
                title = "Баланың жеке бағдары",
                onBack = null,
                trailing = "ҮЙ",
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
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("${progress.totalStars} жұлдыз", color = AppRed, style = MaterialTheme.typography.labelSmall)
                        Text("${progress.totalSessions} жаттығу", color = AppText.copy(alpha = 0.54f), style = MaterialTheme.typography.labelSmall)
                        Text("${progress.activeDays} белсенді күн", color = AppText.copy(alpha = 0.54f), style = MaterialTheme.typography.labelSmall)
                    }
                    Divider(modifier = Modifier.padding(top = 9.dp), color = AppOutline)
                }

                HomeActionPanel(
                    needsAssessment = trainingRoute.needsAssessment,
                    focusSounds = trainingRoute.focusSounds.joinToString(" • ") { it.uppercase() },
                    activityCount = trainingRoute.activities.size,
                    onOpenTraining = onOpenTraining,
                    onOpenAssessment = onOpenAssessment,
                    onOpenFaceMap = onOpenFaceMap,
                    onOpenSoundMap = onOpenSoundMap,
                    onOpenWords = onOpenWords,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 0.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeActionPanel(
    needsAssessment: Boolean,
    focusSounds: String,
    activityCount: Int,
    onOpenTraining: () -> Unit,
    onOpenAssessment: () -> Unit,
    onOpenFaceMap: () -> Unit,
    onOpenSoundMap: () -> Unit,
    onOpenWords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFF9F6ED),
        shape = RoundedCornerShape(0.dp),
        shadowElevation = 7.dp,
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(AppRed))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clickable(onClick = onOpenTraining)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.width(3.dp).height(48.dp).background(AppRed))
                Column(modifier = Modifier.padding(start = 13.dp).weight(1f)) {
                        Text(
                            "Бүгінгі шағын қадам",
                            color = AppRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (needsAssessment) "Алдымен сөйлеуді бағалайық" else "Жаттығуды жалғастыру",
                        modifier = Modifier.padding(top = 3.dp),
                        color = AppText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (needsAssessment) "Нәтижеден кейін жеке маршрут құрылады" else "$focusSounds  ·  $activityCount жаттығу",
                        modifier = Modifier.padding(top = 2.dp),
                        color = AppText.copy(alpha = 0.52f),
                        fontSize = 10.sp,
                    )
                }
                Text("Бастау", color = AppNavy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Divider(color = AppOutline)
            Row(
                modifier = Modifier.fillMaxWidth().height(62.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeQuickAction(Icons.Outlined.Assessment, "Тексеру", AppRed, onOpenAssessment, Modifier.weight(1f))
                HomeRailDivider()
                HomeQuickAction(Icons.Outlined.Person, "Айна", AppText, onOpenFaceMap, Modifier.weight(1f))
                HomeRailDivider()
                HomeQuickAction(Icons.Outlined.RecordVoiceOver, "Дыбыстар", AppText, onOpenSoundMap, Modifier.weight(1f))
                HomeRailDivider()
                HomeQuickAction(Icons.Outlined.MenuBook, "Сөздер", AppText, onOpenWords, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HomeRailDivider() {
    Box(modifier = Modifier.width(1.dp).height(30.dp).background(AppOutline))
}

@Composable
private fun HomeQuickAction(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        Text(
            label,
            modifier = Modifier.padding(top = 3.dp),
            color = AppText.copy(alpha = 0.66f),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
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
            .padding(top = 72.dp, bottom = 190.dp),
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
            color = AppBlue.copy(alpha = 0.20f),
            style = Stroke(
                width = 5.dp.toPx(),
                cap = StrokeCap.Round, 
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 20f))
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
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = if (unlocked) Color.White else Color(0xFFE7ECEE),
                border = BorderStroke(2.dp, if (unlocked) levelNodeColor(level.id) else AppOutline),
                shadowElevation = if (unlocked) 8.dp else 0.dp,
            ) {
                Box(
                    modifier = Modifier.background(
                        if (unlocked) levelNodeColor(level.id).copy(alpha = 0.09f) else Color.Transparent,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (unlocked) levelMaterialIcon(level.id) else Icons.Outlined.Lock,
                        contentDescription = level.title,
                        tint = if (unlocked) levelNodeColor(level.id) else AppText.copy(alpha = 0.4f),
                        modifier = Modifier.size(37.dp),
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
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 3.dp, bottom = 3.dp).size(30.dp),
                    shape = CircleShape,
                    color = AppNavy,
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
            color = if (unlocked) Color.White else AppSurfaceMuted,
            shape = RoundedCornerShape(2.dp),
            border = BorderStroke(1.dp, if (unlocked) AppOutline else Color.Transparent),
            shadowElevation = if (unlocked) 2.dp else 0.dp,
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
    1 -> AppBlue
    2 -> AppGreen
    3 -> AppOrange
    4 -> AppPurple
    5 -> Color(0xFFB96F68)
    else -> Color(0xFF718089)
}

private fun levelMaterialIcon(levelId: Int): ImageVector = when (levelId) {
    1 -> Icons.Outlined.Landscape
    2 -> Icons.Outlined.Home
    3 -> Icons.Outlined.RecordVoiceOver
    4 -> Icons.Outlined.MenuBook
    5 -> Icons.Outlined.School
    else -> Icons.Outlined.Star
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(levelGradient(level.id))),
    ) {
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
                            Icon(
                                levelMaterialIcon(level.id),
                                contentDescription = null,
                                tint = levelNodeColor(level.id),
                                modifier = Modifier.size(52.dp),
                            )
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
                    icon = Icons.Outlined.Star,
                    text = "Барлық ойындарды аяқтап, жұлдыздарды жина! Сенің қолыңнан келеді!",
                    color = Color(0xFFFFF9E6),
                )
                if (!audio.ready) {
                    InfoStrip(
                        icon = Icons.Outlined.VolumeUp,
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
                Icon(Icons.Outlined.School, contentDescription = null, tint = AppRed, modifier = Modifier.size(29.dp))
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
                Icon(
                    if (completed) Icons.Outlined.CheckCircle else Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = if (completed) AppGreen else AppText.copy(alpha = 0.54f),
                    modifier = Modifier.size(21.dp),
                )
                if (completed) Text("$stars / 3  ·  $bestScore%", color = AppText.copy(alpha = 0.55f), fontSize = 10.sp)
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
                        Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = AppRed, modifier = Modifier.size(58.dp))
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
                                        Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, tint = AppRed, modifier = Modifier.size(29.dp))
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
                                            "Тыңдау",
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
                icon = Icons.Outlined.School,
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
                    Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, tint = AppRed, modifier = Modifier.size(42.dp))
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
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = AppGreen, modifier = Modifier.size(14.dp))
                        Text("Білемін", modifier = Modifier.padding(start = 4.dp), color = AppGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    Icon(Icons.Outlined.VolumeUp, contentDescription = "Тыңдау", tint = AppBlue, modifier = Modifier.size(16.dp))
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
                    Icon(Icons.Outlined.VolumeUp, contentDescription = null, tint = AppBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Тыңдау", color = AppBlue)
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
                            Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, tint = AppRed, modifier = Modifier.size(26.dp))
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
    onReflection: () -> Unit,
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
    val reflections = remember(progressRevision) { progress.parentReflections }
    val reflectionInsight = remember(reflections) { ParentReflectionEngine.weeklyInsight(reflections) }
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val reflectedToday = reflections.any { it.dateKey == todayKey }
    val exerciseReports = remember(progressRevision) { progress.exerciseSessionReports }
    val todayExerciseCount = exerciseReports.count {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.completedAt)) == todayKey
    }

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
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                "Балаңыздың бүгінгі жолы",
                color = AppRed,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    "${(progress.overallProgress * 100).toInt()}",
                    color = AppText,
                    fontSize = 54.sp,
                    lineHeight = 56.sp,
                    fontWeight = FontWeight.Normal,
                )
                Text("%", modifier = Modifier.padding(start = 3.dp, bottom = 6.dp), color = AppRed, fontSize = 18.sp)
                Column(modifier = Modifier.padding(start = 16.dp, bottom = 7.dp)) {
                    Text("Оқу жолы аяқталды", color = AppText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Тек орындалған жаттығулар бойынша", color = AppText.copy(alpha = 0.48f), fontSize = 10.sp)
                }
            }
            LinearProgressIndicator(
                progress = progress.overallProgress,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(4.dp),
                color = AppRed,
                trackColor = AppOutline.copy(alpha = 0.55f),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ParentMetric("${progress.totalSessions}", "жаттығу", Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).height(34.dp).background(AppOutline))
                ParentMetric("${progress.practicedRounds}", "тапсырма", Modifier.weight(1f))
                Box(modifier = Modifier.width(1.dp).height(34.dp).background(AppOutline))
                ParentMetric("${progress.activeDays}", "белсенді күн", Modifier.weight(1f))
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .clickable(onClick = onReflection),
                color = Color.Transparent,
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, if (reflectedToday) AppGreen else AppRed),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Assessment,
                        contentDescription = null,
                        tint = if (reflectedToday) AppGreen else AppRed,
                        modifier = Modifier.size(25.dp),
                    )
                    Column(modifier = Modifier.padding(start = 15.dp).weight(1f)) {
                        Text("Толық сөйлеу күнделігі", color = AppText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            when {
                                todayExerciseCount > 0 -> "Бүгін $todayExerciseCount жаттығу: әр қадам, аудио және Face Map"
                                reflectedToday -> "Бүгінгі ата-ана байқауы сақталды"
                                else -> "Әр жаттығудың орындалуы және ата-ана байқауы"
                            },
                            modifier = Modifier.padding(top = 3.dp),
                            color = AppText.copy(alpha = 0.53f),
                            fontSize = 10.sp,
                        )
                    }
                    Text(
                        "Ашу",
                        color = if (reflectedToday) AppGreen else AppRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                color = AppNavy,
                shape = RoundedCornerShape(2.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth().height(66.dp), verticalAlignment = Alignment.CenterVertically) {
                    ParentAction(Icons.Outlined.Assessment, "Нәтижелер", onStatistics, Modifier.weight(1f))
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                    ParentAction(Icons.Outlined.MenuBook, "Кеңестер", onGuide, Modifier.weight(1f))
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = 0.18f)))
                    ParentAction(Icons.Outlined.Person, "Бала туралы", onChildInfo, Modifier.weight(1f))
                }
            }

            Text(
                "Аптаның шағын қорытындысы  ·  ${reflectionInsight.completedDays}/7 күн",
                modifier = Modifier.padding(top = 25.dp),
                color = AppRed,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                reflectionInsight.title,
                modifier = Modifier.padding(top = 6.dp),
                color = AppText,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                reflectionInsight.detail,
                modifier = Modifier.padding(top = 7.dp),
                color = AppText.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = "Күнделікті тілдік қолдау",
                modifier = Modifier.padding(top = 28.dp, bottom = 10.dp),
                color = AppText,
                style = MaterialTheme.typography.titleLarge,
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
                    icon = Icons.Outlined.RecordVoiceOver,
                    text = "Балаңыз $diffCount сөзді айтуда қиындық көріп жүр. Нәтижелер бөлімінен толығырақ көре аласыз.",
                    color = Color(0xFFFFF3E0)
                )
            }

            InfoStrip(
                icon = Icons.Outlined.LocalHospital,
                text = "Егер бала айтылғанды жиі түсінбесе, дыбысқа жауап бермесе, сөйлеуі кері кетсе немесе отбасы алаңдаса, педиатрға, есту маманына не логопедке жүгініңіз. Қосымша ерте көмек пайдалы болуы мүмкін.",
                color = Color(0xFFFFEDED),
            )
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Composable
private fun ParentMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = AppText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(
            label,
            modifier = Modifier.padding(top = 2.dp),
            color = AppText.copy(alpha = 0.44f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun ParentAction(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, AppOutline),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                levelMaterialIcon(level.id),
                                contentDescription = null,
                                tint = levelNodeColor(level.id),
                                modifier = Modifier.size(29.dp),
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp),
                            ) {
                                Text("${level.id}. ${level.title}", color = AppText, fontWeight = FontWeight.ExtraBold)
                                Text(level.focus, color = AppText.copy(alpha = 0.52f), fontSize = 10.sp)
                            }
                            Text("${progress.levelStars(level)}/9 жұлдыз", color = AppOrange, fontWeight = FontWeight.Bold)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (progress.isCompleted(activity.id)) Icons.Outlined.CheckCircle else Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        tint = if (progress.isCompleted(activity.id)) AppGreen else AppText.copy(alpha = 0.42f),
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        text = activity.title,
                                        modifier = Modifier.padding(start = 6.dp),
                                        color = AppText.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                    )
                                }
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
                icon = Icons.Outlined.Info,
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
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, AppOutline)
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
                icon = Icons.Outlined.SwapHoriz,
                title = "Кезектесіп сөйлесу",
                text = "Баланың назарына еріңіз: ол бір нәрсені көрсетсе немесе айтса, жауап беріп, атын атаңыз. Әңгіме теннис сияқты кезекпен жүрсін.",
            )
            GuideSection(
                icon = Icons.Outlined.Add,
                title = "Сөзін кеңейту",
                text = "Бала «қызыл доп» десе, «Иә, қызыл доп домалап барады» деп бір қадам күрделірек үлгі беріңіз. Қайталатуға мәжбүрлемеңіз.",
            )
            GuideSection(
                icon = Icons.Outlined.MenuBook,
                title = "Бірге оқу және әңгімелеу",
                text = "Суретті кітапты бірге қарап, «Не болып жатыр?», «Кейін не болады?», «Неліктен?» деп сөйлесіңіз. Оқиғаны қайталап айтуға немесе ойнап көрсетуге болады.",
            )
            GuideSection(
                icon = Icons.Outlined.MusicNote,
                title = "Дыбыс пен сөз ойыны",
                text = "Ұйқас, алғашқы дыбыс және буын шапалағы сөздің дыбыстық құрылымын байқауға көмектеседі. Бірақ қолданба нақты дыбыс бұзылысын анықтамайды және емдемейді.",
            )
            GuideSection(
                icon = Icons.Outlined.Public,
                title = "Көптілділік — кедергі емес",
                text = "Үйде өзіңіз еркін сөйлейтін тілдерді қолданыңыз. Тілдерді араластыру көптілді дамудың қалыпты бөлігі болуы мүмкін.",
            )
            GuideSection(
                icon = Icons.Outlined.Timer,
                title = "Қысқа және шынайы өмірмен байланысты",
                text = "Күніне 10–15 минут бірге ойнау жеткілікті. Әр 2–3 ойыннан кейін экраннан тыс үзіліс жасап, сөздерді тамақта, киінуде, серуенде және ойында қолданыңыз.",
            )
            GuideSection(
                icon = Icons.Outlined.Lock,
                title = "Құпиялық",
                text = "Ата-ана аккаунты Firebase Authentication арқылы басқарылады. Бала туралы негізгі ақпарат пен үйренген сөздер Firebase ішінде, ал толық жаттығу нәтижелері осы құрылғыда сақталады. Қолданба дауыс жазбасын сақтамайды; сөйлеуді тану Android қызметіне байланысты және желіні қолдануы мүмкін.",
            )
            GuideSection(
                icon = Icons.Outlined.LocalHospital,
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
private fun GuideSection(icon: ImageVector, title: String, text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE6EBEF)),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = AppRed, modifier = Modifier.size(27.dp))
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
private fun InfoStrip(icon: ImageVector, text: String, color: Color) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        color = color,
        shape = RoundedCornerShape(17.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = AppText.copy(alpha = 0.68f), modifier = Modifier.size(23.dp))
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
internal fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: String? = null,
    onTrailing: () -> Unit = {},
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        color = AppBackground,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    BackButton(onClick = onBack)
                } else {
                    Box(modifier = Modifier.width(3.dp).height(30.dp).background(AppRed))
                }
                Text(
                    text = title,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 13.dp, end = 10.dp),
                    color = AppText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                if (trailing != null) {
                    Text(
                        trailing,
                        modifier = Modifier.clickable(onClick = onTrailing).padding(9.dp),
                        color = AppRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.7.sp,
                    )
                } else {
                    Spacer(modifier = Modifier.width(36.dp))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppOutline))
        }
    }
}

@Composable
internal fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(36.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "←",
            color = AppText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}

private fun levelGradient(levelId: Int): List<Color> = when (levelId) {
    1 -> listOf(Color(0xFFFCE8BF), Color(0xFFFFF8E9))
    2 -> listOf(Color(0xFFDDEFE9), Color(0xFFF3FAF7))
    3 -> listOf(Color(0xFFDCECF1), Color(0xFFF2F8FA))
    4 -> listOf(Color(0xFFE5E8F3), Color(0xFFF5F6FA))
    5 -> listOf(Color(0xFFF3E4E1), Color(0xFFFBF5F3))
    else -> listOf(Color(0xFFE8ECEF), Color(0xFFF8FAFA))
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
