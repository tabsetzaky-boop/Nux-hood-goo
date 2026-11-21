package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.NuxPreferencesRepository
import com.example.core.NuxText
import com.example.core.NuxUserPrefs
import com.example.core.configureAppCheck
import com.example.data.firebase.authStateFlow
import com.example.data.model.NuxRoutes
import com.example.ui.components.NuxSidebar
import com.example.ui.components.NuxToastBanner
import com.example.ui.screens.ApkStudioScreen
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CustomizeScreen
import com.example.ui.screens.EngineScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.screens.ImagesScreen
import com.example.ui.screens.LegalScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LockedChatsScreen
import com.example.ui.screens.LoginPinGateScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PinnedChatsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProjectBrainScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.RemoteControlScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SiteBuilderScreen
import com.example.ui.theme.NuxTheme
import com.example.viewmodel.NuxViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must configure Firebase App Check before any Firebase AI Logic / Auth / Firestore call.
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val prefsRepo = NuxPreferencesRepository(applicationContext)

        setContent {
            val prefs by prefsRepo.prefsFlow.collectAsStateWithLifecycle(initialValue = NuxUserPrefs())
            val authUser by remember { Firebase.auth.authStateFlow() }
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
            val scope = rememberCoroutineScope()

            val strings = remember(prefs.appLanguage) {
                NuxText(isArabic = prefs.appLanguage.isRtl)
            }

            LaunchedEffect(authUser?.uid) {
                if (authUser != null && prefs.guestMode) {
                    prefsRepo.setGuestMode(false)
                }
            }

            NuxTheme(
                themeMode = prefs.themeMode,
                layoutDirection = prefs.appLanguage.layoutDirection
            ) {
                val user = authUser
                when {
                    user != null -> {
                        key(user.uid) {
                            NuxAppHost(
                                uid = user.uid,
                                user = user,
                                strings = strings
                            )
                        }
                    }
                    prefs.guestMode -> {
                        key(NuxViewModel.GUEST_USER_ID) {
                            NuxAppHost(
                                uid = NuxViewModel.GUEST_USER_ID,
                                user = null,
                                strings = strings
                            )
                        }
                    }
                    else -> {
                        AuthScreen(
                            strings = strings,
                            onContinueWithoutSignIn = {
                                scope.launch { prefsRepo.setGuestMode(true) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NuxAppHost(
    uid: String,
    user: FirebaseUser?,
    strings: NuxText
) {
    val viewModel: NuxViewModel = viewModel(
        key = uid,
        factory = NuxViewModel.provideFactory(uid)
    )

    LaunchedEffect(uid) {
        if (user != null) {
            viewModel.ensureProfileExists(
                displayName = user.displayName.orEmpty(),
                email = user.email.orEmpty(),
                photoUrl = user.photoUrl?.toString()
            )
        }
    }

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val pinsState by viewModel.securityPinsState.collectAsStateWithLifecycle()
    val activeConvId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val loginPinVerified by viewModel.loginPinVerified.collectAsStateWithLifecycle()
    val deviceId by viewModel.deviceId.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val projects = (projectsState as? UiState.Success)?.data.orEmpty()
    val pins = (pinsState as? UiState.Success)?.data.orEmpty()

    val loginPin = pins.firstOrNull { it.kind == "login" && it.enabled }
    val requiresLoginPin = loginPin != null && !loginPinVerified && deviceId.isNotBlank() && deviceId !in loginPin.trustedDevices

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            profile != null && !profile.profileCompleted -> {
                OnboardingScreen(
                    strings = strings,
                    onComplete = { gender, age ->
                        viewModel.completeOnboarding(gender, age)
                    }
                )
            }

            requiresLoginPin -> {
                LoginPinGateScreen(
                    strings = strings,
                    onVerifyPin = { entered -> viewModel.verifyPin("login", entered) },
                    onVerified = { viewModel.markLoginPinVerified() }
                )
            }

            else -> {
                NuxWorkspaceShell(
                    viewModel = viewModel,
                    strings = strings,
                    profile = profile,
                    conversations = conversations,
                    projects = projects,
                    activeConvId = activeConvId
                )
            }
        }

        NuxToastBanner(
            toast = toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun NuxWorkspaceShell(
    viewModel: NuxViewModel,
    strings: NuxText,
    profile: com.example.data.model.UserProfile?,
    conversations: List<com.example.data.model.Conversation>,
    projects: List<com.example.data.model.Project>,
    activeConvId: String?
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: NuxRoutes.CHAT
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun navigateTo(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun navigateBackToChat() {
        val popped = navController.popBackStack(NuxRoutes.CHAT, inclusive = false)
        if (!popped) {
            navController.navigate(NuxRoutes.CHAT) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun openChat(convId: String) {
        viewModel.openConversation(convId)
        navigateBackToChat()
    }

    fun startNewChat(projectId: String? = null, initialQuery: String? = null) {
        viewModel.startNewChat(projectId, initialQuery)
        navigateBackToChat()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 820.dp

        val sidebarContent: @Composable () -> Unit = {
            NuxSidebar(
                strings = strings,
                currentRoute = currentRoute,
                activeConversationId = activeConvId,
                profile = profile,
                conversations = conversations,
                projects = projects,
                isWideScreen = isWideScreen,
                onCloseDrawer = { scope.launch { drawerState.close() } },
                onNewChat = { startNewChat() },
                onNavigate = { route -> navigateTo(route) },
                onOpenConversation = { id -> openChat(id) },
                onRenameConversation = { id, title -> viewModel.renameConversation(id, title) },
                onTogglePinConversation = { conv -> viewModel.togglePinConversation(conv) },
                onToggleArchiveConversation = { conv -> viewModel.toggleArchiveConversation(conv) },
                onToggleLockConversation = { conv -> viewModel.toggleLockConversation(conv) },
                onMoveConversationToProject = { id, projId -> viewModel.moveConversationToProject(id, projId) },
                onDeleteConversation = { id -> viewModel.deleteConversation(id) }
            )
        }

        val navHostContent: @Composable () -> Unit = {
            NavHost(
                navController = navController,
                startDestination = NuxRoutes.CHAT,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(NuxRoutes.CHAT) {
                    ChatScreen(
                        viewModel = viewModel,
                        strings = strings,
                        isWideScreen = isWideScreen,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateRoute = { navigateTo(it) }
                    )
                }
                composable(
                    route = NuxRoutes.CHAT_WITH_ID,
                    arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
                ) { entry ->
                    val convId = entry.arguments?.getString("conversationId")
                    LaunchedEffect(convId) {
                        if (!convId.isNullOrBlank()) viewModel.openConversation(convId)
                    }
                    ChatScreen(
                        viewModel = viewModel,
                        strings = strings,
                        isWideScreen = isWideScreen,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onNavigateRoute = { navigateTo(it) }
                    )
                }
                composable(NuxRoutes.SEARCH) {
                    SearchScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() },
                        onOpenConversation = { openChat(it) },
                        onNavigateRoute = { navigateTo(it) }
                    )
                }
                composable(NuxRoutes.IMAGES) {
                    ImagesScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() },
                        onOpenConversation = { openChat(it) }
                    )
                }
                composable(NuxRoutes.LIBRARY) {
                    LibraryScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() },
                        onOpenConversation = { openChat(it) },
                        onNavigateRoute = { navigateTo(it) }
                    )
                }
                composable(NuxRoutes.FILES) {
                    FilesScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() },
                        onOpenConversation = { openChat(it) }
                    )
                }
                composable(NuxRoutes.PROJECTS) {
                    ProjectsScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() },
                        onStartProjectChat = { projId -> startNewChat(projectId = projId) },
                        onNavigateRoute = { navigateTo(it) }
                    )
                }
                composable(NuxRoutes.CUSTOMIZE) {
                    CustomizeScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() },
                        onNavigateRoute = { navigateTo(it) },
                        onStartNewChat = { startNewChat() }
                    )
                }
                composable(NuxRoutes.MEMORY) {
                    MemoryScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NuxRoutes.EXPLORE) {
                    ExploreScreen(
                        strings = strings,
                        onBack = { navController.popBackStack() },
                        onSelectIdea = { prompt -> startNewChat(initialQuery = prompt) }
                    )
                }
                composable(NuxRoutes.SETTINGS) {
                    SettingsScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() },
                        onNavigateRoute = { navigateTo(it) }
                    )
                }
                composable(NuxRoutes.SECURITY) {
                    SecurityScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NuxRoutes.ARCHIVE) {
                    ArchiveScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() },
                        onOpenConversation = { openChat(it) }
                    )
                }
                composable(NuxRoutes.LOCKED) {
                    LockedChatsScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() },
                        onOpenConversation = { openChat(it) }
                    )
                }
                composable(NuxRoutes.PINNED) {
                    PinnedChatsScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() },
                        onOpenConversation = { openChat(it) }
                    )
                }
                composable(NuxRoutes.PROFILE) {
                    ProfileScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NuxRoutes.GUIDE) {
                    GuideScreen(
                        strings = strings,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = NuxRoutes.LEGAL,
                    arguments = listOf(navArgument("doc") { type = NavType.StringType })
                ) { entry ->
                    LegalScreen(
                        doc = entry.arguments?.getString("doc") ?: "privacy",
                        strings = strings,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NuxRoutes.BUILDER) {
                    SiteBuilderScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NuxRoutes.DEPLOY) {
                    SiteBuilderScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NuxRoutes.APK) {
                    ApkStudioScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NuxRoutes.BRAIN) {
                    ProjectBrainScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NuxRoutes.REMOTE) {
                    RemoteControlScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NuxRoutes.ENGINE) {
                    EngineScreen(
                        viewModel = viewModel,
                        strings = strings,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        if (isWideScreen) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(300.dp),
                        drawerContainerColor = Color.Transparent
                    ) {
                        sidebarContent()
                    }
                }
            ) {
                navHostContent()
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(304.dp),
                        drawerContainerColor = Color.Transparent
                    ) {
                        sidebarContent()
                    }
                }
            ) {
                navHostContent()
            }
        }
    }
}
