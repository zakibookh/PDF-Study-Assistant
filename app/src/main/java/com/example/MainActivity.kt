package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.UserDto
import com.example.ui.components.ServerStatusPill
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DocumentReaderScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.theme.StudyPdfTheme
import com.example.ui.viewmodel.SessionState
import com.example.ui.viewmodel.StudyViewModel

const val ROUTE_STUDY = "study"
const val ROUTE_LIBRARY = "library"
const val ROUTE_HISTORY = "history"
const val ROUTE_ADMIN = "admin"
const val ROUTE_SETTINGS = "settings"

private data class NavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyPdfTheme {
                StudyPdfApp()
            }
        }
    }
}

@Composable
fun StudyPdfApp(
    viewModel: StudyViewModel = viewModel(),
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    val studyState by viewModel.studyUiState.collectAsStateWithLifecycle()
    val libraryState by viewModel.libraryUiState.collectAsStateWithLifecycle()
    val readerState by viewModel.readerUiState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyUiState.collectAsStateWithLifecycle()
    val adminState by viewModel.adminUiState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsUiState.collectAsStateWithLifecycle()

    when (val session = sessionState) {
        SessionState.Checking -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        SessionState.Unauthenticated -> {
            AuthScreen(
                authState = authState,
                settingsState = settingsState,
                onToggleMode = viewModel::toggleAuthMode,
                onNameChange = viewModel::onAuthNameChange,
                onEmailChange = viewModel::onAuthEmailChange,
                onPasswordChange = viewModel::onAuthPasswordChange,
                onAdminCodeChange = viewModel::onAdminCodeChange,
                onToggleAdminField = viewModel::toggleAdminCodeField,
                onSubmit = viewModel::submitAuth,
                onBaseUrlChange = viewModel::onBaseUrlInputChange,
                onSaveAndTestUrl = viewModel::saveAndTestServerUrl,
                onResetDefaultUrl = viewModel::resetServerUrl,
            )
        }

        is SessionState.Authenticated -> {
            if (readerState.isOpen) {
                DocumentReaderScreen(
                    readerState = readerState,
                    onClose = viewModel::closeDocumentReader,
                    onLoadPage = { page -> viewModel.loadReaderPage(page = page) },
                    onDownloadPdf = viewModel::downloadPdfForViewing,
                    onAskAboutDocument = { doc ->
                        viewModel.closeDocumentReader()
                        viewModel.selectStudyDocument(doc)
                    },
                )
            } else {
                AuthenticatedShell(
                    user = session.user,
                    viewModel = viewModel,
                    studyState = studyState,
                    libraryState = libraryState,
                    historyState = historyState,
                    adminState = adminState,
                    settingsState = settingsState,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShell(
    user: UserDto,
    viewModel: StudyViewModel,
    studyState: com.example.ui.viewmodel.StudyUiState,
    libraryState: com.example.ui.viewmodel.LibraryUiState,
    historyState: com.example.ui.viewmodel.HistoryUiState,
    adminState: com.example.ui.viewmodel.AdminUiState,
    settingsState: com.example.ui.viewmodel.SettingsUiState,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_STUDY

    val destinations = buildList {
        add(
            NavDestination(
                route = ROUTE_STUDY,
                labelRes = R.string.tab_study,
                selectedIcon = Icons.Filled.QuestionAnswer,
                unselectedIcon = Icons.Outlined.QuestionAnswer,
                testTag = "nav_study",
            ),
        )
        add(
            NavDestination(
                route = ROUTE_LIBRARY,
                labelRes = R.string.tab_library,
                selectedIcon = Icons.AutoMirrored.Filled.LibraryBooks,
                unselectedIcon = Icons.AutoMirrored.Outlined.LibraryBooks,
                testTag = "nav_library",
            ),
        )
        add(
            NavDestination(
                route = ROUTE_HISTORY,
                labelRes = R.string.tab_history,
                selectedIcon = Icons.Filled.History,
                unselectedIcon = Icons.Outlined.History,
                testTag = "nav_history",
            ),
        )
        if (user.isAdmin) {
            add(
                NavDestination(
                    route = ROUTE_ADMIN,
                    labelRes = R.string.tab_admin,
                    selectedIcon = Icons.Filled.AdminPanelSettings,
                    unselectedIcon = Icons.Outlined.AdminPanelSettings,
                    testTag = "nav_admin",
                ),
            )
        }
        add(
            NavDestination(
                route = ROUTE_SETTINGS,
                labelRes = R.string.tab_settings,
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_settings",
            ),
        )
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                ROUTE_STUDY -> "StudyPDF"
                                ROUTE_LIBRARY -> "PDF Library"
                                ROUTE_HISTORY -> "Chat History"
                                ROUTE_ADMIN -> "Admin Studio"
                                ROUTE_SETTINGS -> "Settings"
                                else -> "StudyPDF"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    },
                    actions = {
                        ServerStatusPill(
                            healthStatus = settingsState.healthStatus,
                            onClick = { navigateToTab(ROUTE_SETTINGS) },
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    },
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label,
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail {
                        destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label,
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_STUDY,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(ROUTE_STUDY) {
                        StudyScreen(
                            studyState = studyState,
                            availableDocuments = libraryState.documents,
                            onQuestionChange = viewModel::onQuestionInputChange,
                            onSelectDocument = viewModel::selectStudyDocument,
                            onSelectAnswerMode = viewModel::setAnswerMode,
                            onToggleSaveToHistory = viewModel::setSaveToHistory,
                            onSubmitQuestion = { viewModel.submitStudyQuestion() },
                            onClearThread = viewModel::clearStudyThread,
                            onOpenCitationPage = { docId, title, page ->
                                viewModel.openDocumentReader(docId, title, page)
                            },
                        )
                    }

                    composable(ROUTE_LIBRARY) {
                        LibraryScreen(
                            libraryState = libraryState,
                            onSearchQueryChange = viewModel::onLibrarySearchQueryChange,
                            onSelectCategory = viewModel::selectLibraryCategory,
                            onRefresh = viewModel::refreshLibrary,
                            onReadDocument = { doc ->
                                viewModel.openDocumentReader(doc.id, doc.title, 1)
                            },
                            onAskDocument = { doc ->
                                viewModel.selectStudyDocument(doc)
                                navigateToTab(ROUTE_STUDY)
                            },
                        )
                    }

                    composable(ROUTE_HISTORY) {
                        HistoryScreen(
                            historyState = historyState,
                            onRefresh = { viewModel.refreshHistory() },
                            onClearHistory = viewModel::clearChatHistory,
                            onOpenCitation = { docId, title, page ->
                                viewModel.openDocumentReader(docId, title, page)
                            },
                        )
                    }

                    composable(ROUTE_ADMIN) {
                        AdminScreen(
                            adminState = adminState,
                            onRefresh = viewModel::refreshAdminData,
                            onStatusFilterChange = viewModel::setAdminStatusFilter,
                            onUploadPdf = viewModel::uploadAdminPdf,
                            onUpdateDocument = viewModel::updateAdminDocument,
                            onReprocessDocument = viewModel::reprocessAdminDocument,
                            onDeleteDocument = viewModel::deleteAdminDocument,
                            onCreateCategory = viewModel::createCategory,
                            onUpdateCategory = viewModel::updateCategory,
                            onDeleteCategory = viewModel::deleteCategory,
                            onDismissFeedback = viewModel::dismissAdminFeedback,
                        )
                    }

                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            user = user,
                            settingsState = settingsState,
                            onBaseUrlChange = viewModel::onBaseUrlInputChange,
                            onSaveAndTest = viewModel::saveAndTestServerUrl,
                            onResetDefault = viewModel::resetServerUrl,
                            onLogout = viewModel::logout,
                        )
                    }
                }
            }
        }
    }
}
