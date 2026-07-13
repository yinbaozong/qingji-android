package com.dreamjournal.app.ui.navigation

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dreamjournal.app.AppContainer
import com.dreamjournal.app.domain.model.RecordType
import com.dreamjournal.app.domain.settings.SpeechProviderType
import com.dreamjournal.app.ui.screens.calendar.CalendarScreen
import com.dreamjournal.app.ui.screens.calendar.CalendarViewModel
import com.dreamjournal.app.ui.screens.detail.DetailScreen
import com.dreamjournal.app.ui.screens.detail.DetailViewModel
import com.dreamjournal.app.ui.screens.home.HomeScreen
import com.dreamjournal.app.ui.screens.home.HomeViewModel
import com.dreamjournal.app.ui.screens.settings.SettingsScreen
import com.dreamjournal.app.ui.screens.settings.SettingsViewModel
import com.dreamjournal.app.ui.theme.QingJiTheme
import java.util.Locale

private data class BottomNavItem(
    val destination: AppDestination,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun DreamJournalApp(container: AppContainer) {
    val navController = rememberNavController()

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(
            dreamRepository = container.dreamRepository,
            audioRecorderManager = container.audioRecorderManager,
            aiService = container.aiService,
            weatherRepository = container.weatherRepository
        )
    )
    val calendarViewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.factory(container.dreamRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(
            settingsRepository = container.settingsRepository,
            dreamRepository = container.dreamRepository,
            txtExportRepository = container.txtExportRepository,
            aiService = container.aiService
        )
    )

    val homeUiState by homeViewModel.uiState.collectAsState()
    val calendarUiState by calendarViewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()

    QingJiTheme(themeMode = settingsUiState.settings.themeMode) {
        val context = LocalContext.current
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                homeViewModel.startRecording(homeUiState.activeRecordType)
            } else {
                homeViewModel.clearError()
            }
        }
        val locationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            homeViewModel.refreshWeather()
        }
        val speechRecognizerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val list = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val text = list?.firstOrNull().orEmpty()
                homeViewModel.createEntryFromSystemRecognition(text, homeUiState.activeRecordType)
            }
        }

        LaunchedEffect(Unit) {
            homeViewModel.newEntryEvent.collect { id ->
                navController.navigate(AppDestination.Detail.createRoute(id))
            }
        }

        LaunchedEffect(Unit) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        val showBottomBar = currentRoute != null && !currentRoute.startsWith("detail/")

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        tonalElevation = 0.dp
                    ) {
                        val items = listOf(
                            BottomNavItem(
                                destination = AppDestination.Home,
                                label = AppDestination.Home.label,
                                icon = { Icon(Icons.Default.Home, contentDescription = AppDestination.Home.label) }
                            ),
                            BottomNavItem(
                                destination = AppDestination.Calendar,
                                label = AppDestination.Calendar.label,
                                icon = { Icon(Icons.Default.DateRange, contentDescription = AppDestination.Calendar.label) }
                            ),
                            BottomNavItem(
                                destination = AppDestination.Settings,
                                label = AppDestination.Settings.label,
                                icon = { Icon(Icons.Default.Person, contentDescription = AppDestination.Settings.label) }
                            )
                        )
                        items.forEach { item ->
                            val destination = item.destination
                            val selected = currentRoute == destination.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(AppDestination.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { item.icon() },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                modifier = Modifier.padding(padding),
                navController = navController,
                startDestination = AppDestination.Home.route
            ) {
                composable(AppDestination.Home.route) {
                    HomeScreen(
                        uiState = homeUiState,
                        onStartRecording = { recordType ->
                            val isSystemMode = settingsUiState.settings.speechProviderType == SpeechProviderType.SYSTEM
                            if (isSystemMode) {
                                homeViewModel.showRecordingMode(recordType)
                                val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                                    putExtra(
                                        RecognizerIntent.EXTRA_PROMPT,
                                        "请说出你想记录的内容"
                                    )
                                }
                                val canHandle = speechIntent.resolveActivity(context.packageManager) != null
                                if (!canHandle) {
                                    homeViewModel.showError("当前手机没有可用的系统语音识别服务，请改用百度或阿里云语音识别。")
                                    return@HomeScreen
                                }
                                runCatching {
                                    speechRecognizerLauncher.launch(speechIntent)
                                }.onFailure { error ->
                                    val message = if (error is ActivityNotFoundException) {
                                        "系统语音识别服务不可用，请改用云端语音识别。"
                                    } else {
                                        "启动系统语音识别失败：${error.message}"
                                    }
                                    homeViewModel.showError(message)
                                }
                                return@HomeScreen
                            }
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                homeViewModel.startRecording(recordType)
                            } else {
                                homeViewModel.showRecordingMode(recordType)
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStopRecording = { homeViewModel.stopRecordingAndCreateEntry() },
                        onCreateTextEntry = homeViewModel::createTextEntry,
                        onOpenEntry = { entryId ->
                            navController.navigate(AppDestination.Detail.createRoute(entryId))
                        },
                        onClearError = { homeViewModel.clearError() }
                    )
                }

                composable(AppDestination.Calendar.route) {
                    CalendarScreen(
                        uiState = calendarUiState,
                        onDateSelected = calendarViewModel::onDateSelected,
                        onOpenEntry = { entryId ->
                            navController.navigate(AppDestination.Detail.createRoute(entryId))
                        }
                    )
                }

                composable(AppDestination.Settings.route) {
                    SettingsScreen(
                        uiState = settingsUiState,
                        onSetThemeMode = settingsViewModel::setThemeMode,
                        onSetSpeechProviderType = settingsViewModel::setSpeechProviderType,
                        onSetAnalysisProviderType = settingsViewModel::setAnalysisProviderType,
                        onSetAnalysisServiceType = settingsViewModel::setAnalysisServiceType,
                        onSetSpeechBaseUrl = settingsViewModel::setSpeechBaseUrl,
                        onSetSpeechApiPath = settingsViewModel::setSpeechApiPath,
                        onSetSpeechModel = settingsViewModel::setSpeechModel,
                        onSetBaiduSpeechUrl = settingsViewModel::setBaiduSpeechUrl,
                        onSetBaiduTokenUrl = settingsViewModel::setBaiduTokenUrl,
                        onSetBaiduApiKey = settingsViewModel::setBaiduApiKey,
                        onSetBaiduSecretKey = settingsViewModel::setBaiduSecretKey,
                        onSetBaiduAppId = settingsViewModel::setBaiduAppId,
                        onSetBaiduDevPid = settingsViewModel::setBaiduDevPid,
                        onSetAliyunSpeechApiKey = settingsViewModel::setAliyunSpeechApiKey,
                        onSetAliyunSpeechModel = settingsViewModel::setAliyunSpeechModel,
                        onSetAnalysisModel = settingsViewModel::setAnalysisModel,
                        onSetSpeechApiKey = settingsViewModel::setSpeechApiKey,
                        onSetAnalysisApiKey = settingsViewModel::setAnalysisApiKey,
                        onSetAnalysisPromptTemplate = settingsViewModel::setAnalysisPromptTemplate,
                        onAddCustomTag = settingsViewModel::addCustomTag,
                        onRemoveCustomTag = settingsViewModel::removeCustomTag,
                        onSetExportStartDate = settingsViewModel::setExportStartDate,
                        onSetExportEndDate = settingsViewModel::setExportEndDate,
                        onSetExportFormat = settingsViewModel::setExportFormat,
                        onToggleAdvancedSettings = settingsViewModel::toggleAdvancedSettings,
                        onUseMiniMaxDefaults = settingsViewModel::useMiniMaxDefaults,
                        onUseQwenDefaults = settingsViewModel::useQwenDefaults,
                        onUseAliyunSpeechDefaults = settingsViewModel::useAliyunSpeechDefaults,
                        onSaveSpeechSettings = settingsViewModel::saveAndTestSpeechSettings,
                        onSaveAnalysisSettings = settingsViewModel::saveAndTestAnalysisSettings,
                        onGenerateTodaySummary = settingsViewModel::generateTodaySummary,
                        onExportSelectedRange = settingsViewModel::exportSelectedRange,
                        onExportAll = settingsViewModel::exportAll,
                        onExportSingle = settingsViewModel::exportSingle,
                        onClearExportedFiles = settingsViewModel::clearExportedFiles
                    )
                }

                composable(
                    route = AppDestination.Detail.route,
                    arguments = listOf(navArgument("entryId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val entryId = backStackEntry.arguments?.getLong("entryId") ?: return@composable
                    val detailViewModel: DetailViewModel = viewModel(
                        key = "detail_$entryId",
                        factory = DetailViewModel.factory(
                            entryId = entryId,
                            dreamRepository = container.dreamRepository,
                            settingsRepository = container.settingsRepository,
                            entryAssetRepository = container.entryAssetRepository,
                            aiService = container.aiService,
                            txtExportRepository = container.txtExportRepository,
                            audioPlayerManager = container.audioPlayerManager,
                            audioRecorderManager = container.audioRecorderManager
                        )
                    )
                    val detailUiState by detailViewModel.uiState.collectAsState()
                    LaunchedEffect(detailViewModel) {
                        detailViewModel.deletedEvent.collect {
                            navController.popBackStack()
                        }
                    }

                    DetailScreen(
                        uiState = detailUiState,
                        onBack = { navController.popBackStack() },
                        onTitleChange = detailViewModel::onTitleChange,
                        onTagToggle = detailViewModel::onTagToggle,
                        onUpdateTextBlock = detailViewModel::updateTextBlock,
                        onFocusTextBlock = detailViewModel::focusTextBlock,
                        onSetImageWidth = detailViewModel::setImageWidth,
                        onAddPhotos = detailViewModel::addPhotos,
                        onRemovePhoto = detailViewModel::removePhoto,
                        onAddTodo = detailViewModel::addTodo,
                        onUpdateTodoText = detailViewModel::updateTodoText,
                        onToggleTodo = detailViewModel::toggleTodo,
                        onRemoveTodo = detailViewModel::removeTodo,
                        onSaveDraft = detailViewModel::saveDraft,
                        onRunAnalysis = detailViewModel::runAnalysis,
                        onChatInputChange = detailViewModel::onChatInputChange,
                        onSendChat = detailViewModel::sendChat,
                        onDeleteEntry = detailViewModel::deleteCurrentEntry,
                        onToggleAudio = detailViewModel::toggleAudioPlayback,
                        onTranscribeAudio = detailViewModel::transcribeAudio,
                        onStartAdditionalRecording = detailViewModel::startAdditionalRecording,
                        onStopAdditionalRecording = detailViewModel::stopAdditionalRecording,
                        onClearError = detailViewModel::clearError,
                        onClearStatus = detailViewModel::clearStatus
                    )
                }
            }
        }
    }
}
