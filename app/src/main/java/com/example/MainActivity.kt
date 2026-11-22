package com.example

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.ai.PaceCoachService
import com.example.data.audio.TavernAudioEngine
import com.example.data.audio.VoiceTranscriber
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.firebase.signOutUser
import com.example.data.integrations.CorosIntegrationManager
import com.example.data.integrations.ExternalFitnessConnectors
import com.example.data.integrations.HealthConnectManager
import com.example.data.integrations.WatchBleHeartRateManager
import com.example.data.local.LocalSqlRepository
import com.example.data.local.SummitSqlDatabase
import com.example.data.maps.OfflineMapTileManager
import com.example.data.model.AccountRole
import com.example.data.model.PlayMode
import com.example.data.model.SeasonTheme
import com.example.data.model.UiState
import com.example.data.repository.SummitRepository
import com.example.data.safety.GloveGestureDetector
import com.example.data.safety.HardwareKeyController
import com.example.data.safety.PhoneStepCounterManager
import com.example.data.safety.WinterFallDetector
import com.example.data.safety.WinterSosController
import com.example.data.weather.PolarVortexWeatherAlertService
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.example.ui.components.RunningWolvesBackgroundOverlay
import com.example.ui.components.SeasonalParticleOverlay
import com.example.ui.screens.ActivityDetailScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChallengesMilestonesScreen
import com.example.ui.screens.ConservationDashboardScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PackChronicleScreen
import com.example.ui.screens.PackHallScreen
import com.example.ui.screens.SeasonalChallengeScreen
import com.example.ui.screens.SeasonalLeaderboardScreen
import com.example.ui.screens.StridersSplashScreen
import com.example.ui.screens.SyncProfileScreen
import com.example.ui.screens.TavernDoorIntroScreen
import com.example.ui.screens.TrackScreen
import com.example.ui.screens.TrailDesignerScreen
import com.example.ui.screens.TrainingPlansScreen
import com.example.ui.theme.SummitPaceTheme
import com.example.ui.viewmodel.SummitViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val ROUTE_HOME = "home"
private const val ROUTE_TRACK = "track"
private const val ROUTE_PACK_HALL = "pack_hall"
private const val ROUTE_QUESTS = "quests"
private const val ROUTE_PROFILE = "profile"
private const val ROUTE_ACTIVITY_DETAIL = "activity_detail/{activityId}"
private const val ROUTE_TRAINING_PLANS = "training_plans"
private const val ROUTE_TRAIL_DESIGNER = "trail_designer"
private const val ROUTE_CONSERVATION = "conservation"
private const val ROUTE_CHRONICLES = "chronicles"
private const val ROUTE_SEASONAL_CHALLENGE = "seasonal_challenge"
private const val ROUTE_SEASONAL_LEADERBOARD = "seasonal_leaderboard"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val TopLevelDestinations = listOf(
    NavDestinationItem(ROUTE_HOME, "Tavern", Icons.Filled.Home, Icons.Outlined.Home),
    NavDestinationItem(ROUTE_TRACK, "Run", Icons.Filled.MyLocation, Icons.Outlined.MyLocation),
    NavDestinationItem(ROUTE_PACK_HALL, "Pack", Icons.Filled.Groups, Icons.Outlined.Groups),
    NavDestinationItem(ROUTE_QUESTS, "Quests", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    NavDestinationItem(ROUTE_PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

class MainActivity : ComponentActivity() {

    private val pendingCorosAuthCode = MutableStateFlow<String?>(null)
    private val pendingStravaAuthCode = MutableStateFlow<String?>(null)
    private val pendingGarminAuthCode = MutableStateFlow<String?>(null)
    private val showHealthConnectRationale = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase ops
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        extractOAuthCallbacks(intent)

        setContent {
            SummitPaceTheme(darkTheme = true) {
                val showRationale by showHealthConnectRationale.collectAsStateWithLifecycle()
                AppNavigation(
                    pendingCorosCodeFlow = pendingCorosAuthCode.asStateFlow(),
                    onCorosCodeConsumed = { pendingCorosAuthCode.value = null },
                    pendingStravaCodeFlow = pendingStravaAuthCode.asStateFlow(),
                    onStravaCodeConsumed = { pendingStravaAuthCode.value = null },
                    pendingGarminCodeFlow = pendingGarminAuthCode.asStateFlow(),
                    onGarminCodeConsumed = { pendingGarminAuthCode.value = null }
                )
                if (showRationale) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showHealthConnectRationale.value = false },
                        title = { Text("Google Health Connect Privacy", fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "Striders Howl connects to Google Health Connect solely to import your runs, hikes, distance, and elevation gain into your trail log and optionally export completed GPS quests back to Health Connect. Health data is never sold or shared with advertisers."
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(
                                onClick = { showHealthConnectRationale.value = false }
                            ) {
                                Text("Got it")
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractOAuthCallbacks(intent)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (HardwareKeyController.handleAndroidKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun extractOAuthCallbacks(intent: Intent?) {
        val action = intent?.action
        if (action == "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" ||
            action == "android.intent.action.VIEW_PERMISSION_USAGE"
        ) {
            showHealthConnectRationale.value = true
            return
        }
        val data = intent?.data ?: return
        if (data.scheme != "summitpace") return
        when (data.host) {
            "coros" -> data.getQueryParameter("code")?.takeIf { it.isNotBlank() }?.let {
                pendingCorosAuthCode.value = it
            }
            "strava" -> data.getQueryParameter("code")?.takeIf { it.isNotBlank() }?.let {
                pendingStravaAuthCode.value = it
            }
            "garmin" -> {
                val code = data.getQueryParameter("code")?.takeIf { it.isNotBlank() }
                    ?: data.getQueryParameter("oauth_token")?.takeIf { it.isNotBlank() }
                    ?: "authorized"
                pendingGarminAuthCode.value = code
            }
        }
    }
}

@Composable
fun AppNavigation(
    pendingCorosCodeFlow: StateFlow<String?>,
    onCorosCodeConsumed: () -> Unit,
    pendingStravaCodeFlow: StateFlow<String?>,
    onStravaCodeConsumed: () -> Unit,
    pendingGarminCodeFlow: StateFlow<String?>,
    onGarminCodeConsumed: () -> Unit
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var splashFinished by rememberSaveable { mutableStateOf(false) }
    var doorOpened by rememberSaveable { mutableStateOf(false) }
    var selectedPlayMode by rememberSaveable { mutableStateOf(PlayMode.SOLO) }
    var selectedRole by rememberSaveable { mutableStateOf(AccountRole.PRIMARY) }

    val introAudioEngine = remember(context) { TavernAudioEngine(context.applicationContext) }

    AnimatedContent(
        targetState = splashFinished,
        transitionSpec = {
            fadeIn(animationSpec = tween(700)) togetherWith fadeOut(animationSpec = tween(700))
        },
        label = "launch_splash_transition"
    ) { isReady ->
        if (!isReady) {
            StridersSplashScreen(
                onTriggerOpeningHowl = { introAudioEngine.playLoneWolfOpeningHowl() },
                onFinished = { splashFinished = true }
            )
        } else if (!doorOpened) {
            TavernDoorIntroScreen(
                onPlayDoorCreak = { introAudioEngine.playDoorHingeCreak() },
                onDoorOpened = { doorOpened = true }
            )
        } else {
            val user = currentUser
            if (user == null) {
                AuthScreen(
                    selectedPlayMode = selectedPlayMode,
                    onSelectPlayMode = { selectedPlayMode = it },
                    onSelectAccountRole = { selectedRole = it },
                    onAuthSuccess = {}
                )
            } else {
                key(user.uid) {
                    SummitMainShell(
                        currentUserId = user.uid,
                        userDisplayName = user.displayName ?: "Wolf Strider",
                        activeRole = selectedRole,
                        onSwitchRoleRequest = { nextRole ->
                            selectedRole = nextRole
                            selectedPlayMode = PlayMode.PARTY
                        },
                        pendingCorosCodeFlow = pendingCorosCodeFlow,
                        onCorosCodeConsumed = onCorosCodeConsumed,
                        pendingStravaCodeFlow = pendingStravaCodeFlow,
                        onStravaCodeConsumed = onStravaCodeConsumed,
                        pendingGarminCodeFlow = pendingGarminCodeFlow,
                        onGarminCodeConsumed = onGarminCodeConsumed
                    )
                }
            }
        }
    }
}

@Composable
private fun SummitMainShell(
    currentUserId: String,
    userDisplayName: String,
    activeRole: AccountRole,
    onSwitchRoleRequest: (AccountRole) -> Unit,
    pendingCorosCodeFlow: StateFlow<String?>,
    onCorosCodeConsumed: () -> Unit,
    pendingStravaCodeFlow: StateFlow<String?>,
    onStravaCodeConsumed: () -> Unit,
    pendingGarminCodeFlow: StateFlow<String?>,
    onGarminCodeConsumed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: SummitViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val sqlDb = SummitSqlDatabase.getInstance(app)
                val sharedAudioEngine = TavernAudioEngine(app)
                SummitViewModel(
                    repository = SummitRepository(db),
                    localSql = LocalSqlRepository(sqlDb.dao()),
                    paceCoachService = PaceCoachService(),
                    healthConnectManager = HealthConnectManager(app),
                    corosManager = CorosIntegrationManager(app),
                    externalConnectors = ExternalFitnessConnectors(app),
                    offlineMapTileManager = OfflineMapTileManager(app),
                    audioEngine = sharedAudioEngine,
                    voiceTranscriber = VoiceTranscriber(app),
                    bleWatchManager = WatchBleHeartRateManager(app),
                    fallDetector = WinterFallDetector(app),
                    sosController = WinterSosController(app, sharedAudioEngine),
                    gloveGestureDetector = GloveGestureDetector(app),
                    phoneStepCounter = PhoneStepCounterManager(app),
                    weatherAlertService = PolarVortexWeatherAlertService(app),
                    currentUserId = currentUserId,
                    initialDisplayName = userDisplayName,
                    initialRole = activeRole
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val pendingCorosCode by pendingCorosCodeFlow.collectAsStateWithLifecycle()
    val pendingStravaCode by pendingStravaCodeFlow.collectAsStateWithLifecycle()
    val pendingGarminCode by pendingGarminCodeFlow.collectAsStateWithLifecycle()

    val seasonTheme = (profileState as? UiState.Success)?.data?.seasonEnum() ?: SeasonTheme.AUTO

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_HOME
    val isTopLevel = TopLevelDestinations.any { it.route == currentRoute }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    LaunchedEffect(pendingCorosCode) {
        val code = pendingCorosCode
        if (!code.isNullOrBlank()) {
            onCorosCodeConsumed()
            viewModel.handleCorosOAuthCallback(code)
        }
    }

    LaunchedEffect(pendingStravaCode) {
        val code = pendingStravaCode
        if (!code.isNullOrBlank()) {
            onStravaCodeConsumed()
            viewModel.handleStravaOAuthCallback(code)
        }
    }

    LaunchedEffect(pendingGarminCode) {
        val code = pendingGarminCode
        if (!code.isNullOrBlank()) {
            onGarminCodeConsumed()
            viewModel.handleGarminOAuthCallback(code)
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Parallax pack of animated wolves running in the background across all screens
        RunningWolvesBackgroundOverlay(
            intensity = 1.0f,
            showHorizonMist = true,
            modifier = Modifier.fillMaxSize()
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavRail && isTopLevel) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        TopLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("nav_rail_${dest.route}")
                            )
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = Color.Transparent,
                    bottomBar = {
                        if (!useNavRail && isTopLevel) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                TopLevelDestinations.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = { Text(dest.label) },
                                        modifier = Modifier.testTag("nav_bar_${dest.route}")
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                viewModel = viewModel,
                                contentPadding = innerPadding,
                                onOpenActivityDetail = { id -> navController.navigate("activity_detail/$id") },
                                onNavigateToTrack = { navigateToTab(ROUTE_TRACK) },
                                onNavigateToPackHall = { navigateToTab(ROUTE_PACK_HALL) },
                                onNavigateToTrainingPlans = { navController.navigate(ROUTE_TRAINING_PLANS) },
                                onNavigateToTrailDesigner = { navController.navigate(ROUTE_TRAIL_DESIGNER) },
                                onNavigateToQuests = { navigateToTab(ROUTE_QUESTS) },
                                onNavigateToConservation = { navController.navigate(ROUTE_CONSERVATION) },
                                onNavigateToChronicles = { navController.navigate(ROUTE_CHRONICLES) },
                                onNavigateToSeasonalChallenge = { navController.navigate(ROUTE_SEASONAL_CHALLENGE) }
                            )
                        }

                        composable(ROUTE_TRACK) {
                            TrackScreen(
                                viewModel = viewModel,
                                contentPadding = innerPadding,
                                onOpenTrailDesigner = { navController.navigate(ROUTE_TRAIL_DESIGNER) },
                                onOpenSavedActivity = { savedId ->
                                    navController.navigate("activity_detail/$savedId")
                                }
                            )
                        }

                        composable(ROUTE_PACK_HALL) {
                            PackHallScreen(
                                viewModel = viewModel,
                                contentPadding = innerPadding
                            )
                        }

                        composable(ROUTE_QUESTS) {
                            ChallengesMilestonesScreen(
                                viewModel = viewModel,
                                contentPadding = innerPadding,
                                onOpenConservation = { navController.navigate(ROUTE_CONSERVATION) },
                                onOpenChronicles = { navController.navigate(ROUTE_CHRONICLES) },
                                onOpenSeasonalChallenge = { navController.navigate(ROUTE_SEASONAL_CHALLENGE) },
                                onOpenSeasonalLeaderboard = { navController.navigate(ROUTE_SEASONAL_LEADERBOARD) }
                            )
                        }

                        composable(ROUTE_PROFILE) {
                            SyncProfileScreen(
                                viewModel = viewModel,
                                contentPadding = innerPadding,
                                onOpenTrailDesigner = { navController.navigate(ROUTE_TRAIL_DESIGNER) },
                                onSwitchLoginRole = { nextRole ->
                                    onSwitchRoleRequest(nextRole)
                                    signOutUser(credentialManager, scope)
                                },
                                onSignOut = {
                                    signOutUser(credentialManager, scope)
                                }
                            )
                        }

                        composable(
                            route = ROUTE_ACTIVITY_DETAIL,
                            arguments = listOf(navArgument("activityId") { type = NavType.StringType })
                        ) { backStack ->
                            val activityId = backStack.arguments?.getString("activityId").orEmpty()
                            ActivityDetailScreen(
                                activityId = activityId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onOpenLeaderboard = {
                                    navController.popBackStack()
                                    navigateToTab(ROUTE_PACK_HALL)
                                },
                                onOpenChronicles = {
                                    navController.navigate(ROUTE_CHRONICLES)
                                }
                            )
                        }

                        composable(ROUTE_TRAINING_PLANS) {
                            TrainingPlansScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_TRAIL_DESIGNER) {
                            TrailDesignerScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_CONSERVATION) {
                            ConservationDashboardScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(ROUTE_CHRONICLES) {
                            PackChronicleScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onStartPatrolRun = {
                                    navController.popBackStack()
                                    navigateToTab(ROUTE_TRACK)
                                }
                            )
                        }

                        composable(ROUTE_SEASONAL_CHALLENGE) {
                            SeasonalChallengeScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onOpenSeasonalLeaderboard = {
                                    navController.navigate(ROUTE_SEASONAL_LEADERBOARD)
                                },
                                onLogExpeditionRun = {
                                    navController.popBackStack()
                                    navigateToTab(ROUTE_TRACK)
                                }
                            )
                        }

                        composable(ROUTE_SEASONAL_LEADERBOARD) {
                            SeasonalLeaderboardScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onContributeRun = {
                                    navController.popBackStack()
                                    navigateToTab(ROUTE_TRACK)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Seasonal Atmospheric Weather Particle Overlay (Winter snow, Spring blossoms, Summer wind, Autumn leaves)
        SeasonalParticleOverlay(
            seasonTheme = seasonTheme,
            modifier = Modifier.fillMaxSize()
        )
    }
}
