package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.PaceCoachService
import com.example.data.audio.ClimbAudioMode
import com.example.data.audio.SoundboardTrigger
import com.example.data.audio.TavernAudioEngine
import com.example.data.audio.VoiceTranscriber
import com.example.data.integrations.BleHeartRateUiState
import com.example.data.integrations.CorosIntegrationManager
import com.example.data.integrations.ExternalFitnessConnectors
import com.example.data.integrations.HealthConnectAvailability
import com.example.data.integrations.HealthConnectManager
import com.example.data.integrations.HealthConnectSnapshot
import com.example.data.integrations.WatchBleHeartRateManager
import com.example.data.local.CustomTrailEntity
import com.example.data.local.GearItemEntity
import com.example.data.local.LocalSqlRepository
import com.example.data.maps.OfflineMapRegion
import com.example.data.maps.OfflineMapTileManager
import com.example.data.maps.PresetTrailRegion
import com.example.data.model.AccountRole
import com.example.data.model.ActivityRecord
import com.example.data.model.ActivitySource
import com.example.data.model.ActivityType
import com.example.data.model.AdaptiveTrainingPlan
import com.example.data.model.AdventurerRank
import com.example.data.model.CoachUiState
import com.example.data.model.DistanceUnit
import com.example.data.model.EquestrianGait
import com.example.data.model.EquestrianIntervalState
import com.example.data.model.GeoPointEle
import com.example.data.model.MilestoneRecord
import com.example.data.model.PackRankingEntry
import com.example.data.model.RouteAnalysisReport
import com.example.data.model.SeasonTheme
import com.example.data.model.SoloRankingEntry
import com.example.data.model.SummitFormatters
import com.example.data.model.TavernChatMessage
import com.example.data.model.TavernParty
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WeeklyChallenge
import com.example.data.model.WolfCoat
import com.example.data.model.WolfGait
import com.example.data.model.WolfStoryChapter
import com.example.data.repository.SummitRepository
import com.example.data.safety.GloveGestureDetector
import com.example.data.safety.GloveGestureType
import com.example.data.safety.HardwareKeyController
import com.example.data.safety.HeadphoneHardwareAction
import com.example.data.safety.PhoneStepCounterManager
import com.example.data.safety.PhoneStepCounterState
import com.example.data.safety.WinterFallDetector
import com.example.data.safety.WinterSosController
import com.example.data.weather.PolarVortexWeatherAlertService
import com.example.domain.GpsSmoothingService
import com.example.domain.GpxTrailAnalyzer
import com.example.domain.GpxTrailSummary
import com.example.domain.PaceTriggerService
import com.example.domain.PaceZone
import com.example.domain.PackChronicleEngine
import com.example.domain.PackChronicleSnapshot
import com.example.domain.SeasonalChallengeEngine
import com.example.domain.SeasonalChallengeState
import com.example.domain.SeasonalWeatherStatus
import com.example.domain.TrackPoint
import com.example.domain.WeeklyChallengeEngine
import com.example.domain.WeeklyChallengePlan
import com.example.domain.YearlyTravelSummary
import com.example.ui.components.ConservationDonorBadge
import com.example.ui.components.DonorTier
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val TAG = "SummitViewModel"

enum class TrackingStatus {
    IDLE,
    RECORDING,
    PAUSED
}

data class LiveTrackingState(
    val status: TrackingStatus = TrackingStatus.IDLE,
    val activityType: ActivityType = ActivityType.RUN,
    val elapsedSeconds: Long = 0L,
    val distanceMiles: Double = 0.0,
    val elevationGainFeet: Double = 0.0,
    val currentPaceSecPerMile: Long = 0L,
    val avgPaceSecPerMile: Long = 0L,
    val routePoints: List<GeoPointEle> = emptyList(),
    val splitPacesSec: List<Long> = emptyList(),
    val lastKnownLocation: GeoPointEle? = null,
    val startedAtEpochMs: Long = 0L,
    val offlineOnlyMapMode: Boolean = false,
    val selectedGearId: String? = null,
    val equestrianState: EquestrianIntervalState = EquestrianIntervalState(),
    val estimatedHeartRateBpm: Int = 0,
    val gpsAccuracyMeters: Float = 0f,
    val gpsRejectedOutliers: Int = 0,
    val currentGradePercent: Double = 0.0,
    val climbGrowlIntensity: Float = 0f,
    val activePaceZone: PaceZone = PaceZone.ON_TARGET,
    val stepCount: Long = 0L,
    val cadenceSpm: Int = 0,
    val stepDistanceMiles: Double = 0.0,
    val stepSensorLabel: String = "Phone Step Sensor Ready",
    val isStepOnlyMode: Boolean = false
)

data class OfflineMapDownloadState(
    val isDownloading: Boolean = false,
    val downloadingName: String = "",
    val downloadedTiles: Int = 0,
    val totalTiles: Int = 0,
    val savedRegions: List<OfflineMapRegion> = emptyList()
)

data class IntegrationsState(
    val healthConnectAvailability: HealthConnectAvailability = HealthConnectAvailability.UNAVAILABLE,
    val healthConnectPermissionsGranted: Boolean = false,
    val healthConnectSnapshot: HealthConnectSnapshot = HealthConnectSnapshot(),
    val isSyncingHealthConnect: Boolean = false,
    val isSyncingCoros: Boolean = false,
    val isSyncingStrava: Boolean = false,
    val isSyncingGarmin: Boolean = false,
    val corosOAuthConfigured: Boolean = false,
    val stravaOAuthConfigured: Boolean = false,
    val garminOAuthConfigured: Boolean = false,
    val hasCorosSession: Boolean = false,
    val hasStravaSession: Boolean = false,
    val hasGarminSession: Boolean = false,
    val lastSyncSummary: String? = null
)

data class AlphaSoundboardUiState(
    val isMuted: Boolean = false,
    val launchSoundEnabled: Boolean = true,
    val ambientVolume: Float = 0.65f,
    val climbGrowlThresholdPct: Float = 7.0f,
    val climbAudioMode: ClimbAudioMode = ClimbAudioMode.PERIODIC_PULSE,
    val activeTrigger: SoundboardTrigger? = null,
    val isGeneratingAlphaCue: Boolean = false,
    val lastAlphaCueText: String? = null,
    val lastAlphaError: String? = null
)

data class WinterSosUiState(
    val fallDetectionEnabled: Boolean = true,
    val isCountdownActive: Boolean = false,
    val countdownSecondsLeft: Int = 45,
    val triggerReason: String = "Manual Winter Trail SOS",
    val isBeaconActive: Boolean = false,
    val isAcousticBeaconPlaying: Boolean = false,
    val sosStartedAtMs: Long = 0L,
    val activeBeaconSeconds: Long = 0L,
    val batteryPercent: Int = 100,
    val contactsNotifiedCount: Int = 0,
    val emergencyContacts: String = ""
)

data class GloveControlUiState(
    val isGloveModeEnabled: Boolean = false,
    val scentMarkersPlaced: Int = 0,
    val lastHardwareFeedback: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SummitViewModel(
    private val repository: SummitRepository,
    private val localSql: LocalSqlRepository,
    private val paceCoachService: PaceCoachService,
    val healthConnectManager: HealthConnectManager,
    val corosManager: CorosIntegrationManager,
    val externalConnectors: ExternalFitnessConnectors,
    val offlineMapTileManager: OfflineMapTileManager,
    val audioEngine: TavernAudioEngine,
    val voiceTranscriber: VoiceTranscriber,
    val bleWatchManager: WatchBleHeartRateManager,
    val fallDetector: WinterFallDetector,
    val sosController: WinterSosController,
    val gloveGestureDetector: GloveGestureDetector,
    val phoneStepCounter: PhoneStepCounterManager,
    val weatherAlertService: PolarVortexWeatherAlertService,
    val currentUserId: String,
    initialDisplayName: String,
    initialRole: AccountRole = AccountRole.PRIMARY
) : ViewModel() {

    val bleHeartRateState: StateFlow<BleHeartRateUiState> = bleWatchManager.state
    val phoneStepState: StateFlow<PhoneStepCounterState> = phoneStepCounter.state

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    val profileState: StateFlow<UiState<UserProfile?>> = repository.observeProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).message ?: "Unable to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activitiesState: StateFlow<UiState<List<ActivityRecord>>> = repository.observeActivities(currentUserId)
        .map<List<ActivityRecord>, UiState<List<ActivityRecord>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).message ?: "Unable to load activities"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val challengesState: StateFlow<UiState<List<WeeklyChallenge>>> = repository.observeChallenges(currentUserId)
        .map<List<WeeklyChallenge>, UiState<List<WeeklyChallenge>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).message ?: "Unable to load challenges"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val milestonesState: StateFlow<UiState<List<MilestoneRecord>>> = repository.observeMilestones(currentUserId)
        .map<List<MilestoneRecord>, UiState<List<MilestoneRecord>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).message ?: "Unable to load milestones"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    // Local SQLite Room streams
    val gearState: StateFlow<List<GearItemEntity>> = localSql.observeGear(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val trainingPlansState: StateFlow<List<AdaptiveTrainingPlan>> = localSql.observeTrainingPlans(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val customTrailsState: StateFlow<List<CustomTrailEntity>> = localSql.observeCustomTrails(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Partner Pack Party stream
    val tavernPartyState: StateFlow<TavernParty?> = profileState
        .map { (it as? UiState.Success)?.data?.partnerPartyCode.orEmpty() }
        .distinctUntilChanged()
        .flatMapLatest { code ->
            if (code.length >= 6) repository.observeTavernParty(code) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    // Public Tavern Hall Chat & Private Pack Chat
    val publicChatState: StateFlow<List<TavernChatMessage>> = repository.observePublicChat()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val packChatState: StateFlow<List<TavernChatMessage>> = profileState
        .map { (it as? UiState.Success)?.data?.partnerPartyCode.orEmpty() }
        .distinctUntilChanged()
        .flatMapLatest { code ->
            if (code.length >= 6) repository.observePackChat(code) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Live Solo & Pack Ranking Boards
    val soloRankingsState: StateFlow<List<SoloRankingEntry>> = repository.observeSoloRankings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val packRankingsState: StateFlow<List<PackRankingEntry>> = repository.observePackRankings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val weeklyPlanState: StateFlow<WeeklyChallengePlan> = combine(
        profileState,
        activitiesState,
        challengesState
    ) { prof, acts, challs ->
        val baseline = (prof as? UiState.Success)?.data?.weeklyBaselineMiles ?: 10.0
        val activityList = (acts as? UiState.Success)?.data.orEmpty()
        val challengeList = (challs as? UiState.Success)?.data.orEmpty()
        WeeklyChallengeEngine.calculateProgressivePlan(
            activities = activityList,
            pastChallenges = challengeList,
            baselineMiles = baseline
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        WeeklyChallengeEngine.calculateProgressivePlan(emptyList(), emptyList(), 10.0)
    )

    val adventurerRankState: StateFlow<AdventurerRank> = activitiesState
        .map { AdventurerRank.compute((it as? UiState.Success)?.data.orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AdventurerRank.compute(emptyList()))

    val wolfStoryState: StateFlow<WolfStoryChapter> = activitiesState
        .map { state ->
            val miles = (state as? UiState.Success)?.data.orEmpty().sumOf { it.distanceMiles }
            WolfStoryChapter.resolveForMiles(miles)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), WolfStoryChapter.resolveForMiles(0.0))

    val yearlySummaryState: StateFlow<YearlyTravelSummary> = activitiesState
        .map { WeeklyChallengeEngine.buildYearlySummary((it as? UiState.Success)?.data.orEmpty()) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            WeeklyChallengeEngine.buildYearlySummary(emptyList())
        )

    private val _coachState = MutableStateFlow<CoachUiState>(CoachUiState.Idle)
    val coachState: StateFlow<CoachUiState> = _coachState.asStateFlow()

    private val _trackingState = MutableStateFlow(LiveTrackingState())
    val trackingState: StateFlow<LiveTrackingState> = _trackingState.asStateFlow()

    private val _offlineMapState = MutableStateFlow(OfflineMapDownloadState())
    val offlineMapState: StateFlow<OfflineMapDownloadState> = _offlineMapState.asStateFlow()

    private val _integrationsState = MutableStateFlow(
        IntegrationsState(
            healthConnectAvailability = healthConnectManager.getAvailability(),
            corosOAuthConfigured = corosManager.hasConfiguredOAuthCredentials(),
            stravaOAuthConfigured = externalConnectors.hasStravaCredentials(),
            garminOAuthConfigured = externalConnectors.hasGarminCredentials(),
            hasCorosSession = corosManager.hasStoredSession(),
            hasStravaSession = externalConnectors.hasStoredStravaSession(),
            hasGarminSession = externalConnectors.hasStoredGarminSession()
        )
    )
    val integrationsState: StateFlow<IntegrationsState> = _integrationsState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _activityRecapLoadingId = MutableStateFlow<String?>(null)
    val activityRecapLoadingId: StateFlow<String?> = _activityRecapLoadingId.asStateFlow()

    private val _routeAnalysisLoadingId = MutableStateFlow<String?>(null)
    val routeAnalysisLoadingId: StateFlow<String?> = _routeAnalysisLoadingId.asStateFlow()

    private val _trainingPlanLoading = MutableStateFlow(false)
    val trainingPlanLoading: StateFlow<Boolean> = _trainingPlanLoading.asStateFlow()

    private val _wolfHowlTrigger = MutableStateFlow(false)
    val wolfHowlTrigger: StateFlow<Boolean> = _wolfHowlTrigger.asStateFlow()

    private val _voiceCoachEnabled = MutableStateFlow(true)
    val voiceCoachEnabled: StateFlow<Boolean> = _voiceCoachEnabled.asStateFlow()

    private val _targetPaceSecPerMile = MutableStateFlow(570L) // 9:30 /mi default
    val targetPaceSecPerMile: StateFlow<Long> = _targetPaceSecPerMile.asStateFlow()

    private val _voiceChannelAutoReadEnabled = MutableStateFlow(false)
    val voiceChannelAutoReadEnabled: StateFlow<Boolean> = _voiceChannelAutoReadEnabled.asStateFlow()

    private val _isListeningVoice = MutableStateFlow(false)
    val isListeningVoice: StateFlow<Boolean> = _isListeningVoice.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _recentlySavedActivityId = MutableStateFlow<String?>(null)
    val recentlySavedActivityId: StateFlow<String?> = _recentlySavedActivityId.asStateFlow()

    private val _soundboardState = MutableStateFlow(
        AlphaSoundboardUiState(
            isMuted = audioEngine.isMuted,
            launchSoundEnabled = audioEngine.isLaunchSoundEnabled(),
            ambientVolume = audioEngine.ambientVolume
        )
    )
    val soundboardState: StateFlow<AlphaSoundboardUiState> = _soundboardState.asStateFlow()

    private val _sosState = MutableStateFlow(
        WinterSosUiState(
            batteryPercent = sosController.readBatteryPercent(),
            emergencyContacts = sosController.getSavedEmergencyContacts()
        )
    )
    val sosState: StateFlow<WinterSosUiState> = _sosState.asStateFlow()

    private val _gloveControlState = MutableStateFlow(GloveControlUiState())
    val gloveControlState: StateFlow<GloveControlUiState> = _gloveControlState.asStateFlow()

    private val _previewGpxTrail = MutableStateFlow<GpxTrailSummary?>(null)
    val previewGpxTrail: StateFlow<GpxTrailSummary?> = _previewGpxTrail.asStateFlow()

    private val _activePlannedTrail = MutableStateFlow<GpxTrailSummary?>(null)
    val activePlannedTrail: StateFlow<GpxTrailSummary?> = _activePlannedTrail.asStateFlow()

    private val _weatherStatus = MutableStateFlow(SeasonalWeatherStatus())
    val weatherStatusState: StateFlow<SeasonalWeatherStatus> = _weatherStatus.asStateFlow()

    private val _donorBadgeState = MutableStateFlow<ConservationDonorBadge?>(null)
    val donorBadgeState: StateFlow<ConservationDonorBadge?> = _donorBadgeState.asStateFlow()

    val packChronicleState: StateFlow<PackChronicleSnapshot> = combine(
        activitiesState,
        tavernPartyState,
        _donorBadgeState
    ) { actsState, party, donorBadge ->
        val list = (actsState as? UiState.Success)?.data.orEmpty()
        val packName = party?.resolvedPackName?.takeIf { it.isNotBlank() } ?: "Blacktail Ridge Pack"
        PackChronicleEngine.simulateFromActivities(
            activities = list,
            packName = packName,
            hasPackProtectorBonus = donorBadge != null
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        PackChronicleEngine.simulateFromActivities(emptyList())
    )

    val seasonalChallengeState: StateFlow<SeasonalChallengeState> = combine(
        activitiesState,
        _weatherStatus,
        packRankingsState,
        soloRankingsState,
        combine(profileState, tavernPartyState) { p, party -> Pair(p, party) }
    ) { actsState, weather, packs, solos, profAndParty ->
        val acts = (actsState as? UiState.Success)?.data.orEmpty()
        val prof = (profAndParty.first as? UiState.Success)?.data
        val party = profAndParty.second
        SeasonalChallengeEngine.computeState(
            activities = acts,
            weatherStatus = weather,
            packRankings = packs,
            soloRankings = solos,
            currentUserId = currentUserId,
            currentUserName = prof?.displayName ?: "Wolf Strider",
            currentPackName = party?.resolvedPackName?.takeIf { it.isNotBlank() } ?: "Cascade Timber Wolves"
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        SeasonalChallengeEngine.computeState(emptyList(), SeasonalWeatherStatus())
    )

    private var timerJob: Job? = null
    private var howlJob: Job? = null
    private var alphaCueJob: Job? = null
    private var sosCountdownJob: Job? = null
    private var sosActiveTimerJob: Job? = null
    private var lastPaceDropAlertMs = 0L
    private var lastHrZone5AlertMs = 0L
    private var lastClimbCueMs = 0L
    private val gpsSmoother = GpsSmoothingService(qMetersPerSecond = 3.5)
    private val paceTriggerService = PaceTriggerService(targetPaceSecPerMile = 570.0)
    private var locationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var lastMileMark = 0.0
    private var lastMileElapsedSec = 0L
    private var lastAnnouncedEquestrianGait: EquestrianGait? = null
    private var lastSpokenPublicMsgId: String? = null
    private var lastSpokenPackMsgId: String? = null

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(initialDisplayName, initialRole)
            refreshIntegrationStatus()
            refreshSavedOfflineRegions()
        }
        // Listen for Cold-Weather Hardware Key & Shokz Earbud actions when Glove Mode is enabled
        viewModelScope.launch {
            HardwareKeyController.events.collect { action ->
                handleHardwareKeyAction(action)
            }
        }
        // Live Garmin / COROS Bluetooth LE Heart Rate stream -> live tracking HUD + Zone 5 alert
        viewModelScope.launch {
            bleWatchManager.state
                .mapNotNull { it.liveHeartRateBpm }
                .distinctUntilChanged()
                .collect { liveBpm ->
                    _trackingState.update { it.copy(estimatedHeartRateBpm = liveBpm) }
                    if (_trackingState.value.status == TrackingStatus.RECORDING &&
                        _voiceCoachEnabled.value &&
                        liveBpm >= 172
                    ) {
                        val now = System.currentTimeMillis()
                        if (now - lastHrZone5AlertMs > 90_000L) {
                            lastHrZone5AlertMs = now
                            triggerAlphaSoundboardCue(SoundboardTrigger.HEART_RATE_ZONE_ALERT)
                        }
                    }
                }
        }
        // Watch-free Phone Step Counter stream -> live steps, cadence & indoor/no-GPS distance
        if (phoneStepCounter.hasRuntimeActivityPermission()) {
            phoneStepCounter.startDailyCounter(hasActivityPermission = true)
        }
        viewModelScope.launch {
            phoneStepCounter.state.collect { stepSnapshot ->
                _trackingState.update { curr ->
                    if (curr.status != TrackingStatus.RECORDING) {
                        curr.copy(stepSensorLabel = stepSnapshot.sensorSourceLabel)
                    } else {
                        val stepMiles = stepSnapshot.sessionDistanceMiles
                        // Use phone step distance when in step-only mode or before outdoor GPS has movement
                        val useStepDistance = curr.isStepOnlyMode || curr.routePoints.size < 2
                        val resolvedDist = if (useStepDistance) {
                            maxOf(curr.distanceMiles, stepMiles)
                        } else {
                            curr.distanceMiles
                        }
                        val updatedSplits = curr.splitPacesSec.toMutableList()
                        if (useStepDistance && resolvedDist - lastMileMark >= 1.0) {
                            val splitSec = (curr.elapsedSeconds - lastMileElapsedSec).coerceIn(180L, 5400L)
                            updatedSplits += splitSec
                            lastMileMark = resolvedDist
                            lastMileElapsedSec = curr.elapsedSeconds
                            if (_voiceCoachEnabled.value) {
                                triggerAlphaSoundboardCue(SoundboardTrigger.ALPHA_HOWL)
                            }
                        }
                        val avgPace = if (resolvedDist >= 0.03 && curr.elapsedSeconds > 0L) {
                            (curr.elapsedSeconds / resolvedDist).roundToLong().coerceIn(120L, 7200L)
                        } else {
                            curr.avgPaceSecPerMile
                        }
                        val cadenceSpeedMps = if (stepSnapshot.cadenceSpm > 0) {
                            (stepSnapshot.cadenceSpm / 60.0) * stepSnapshot.strideLengthMeters
                        } else 0.0
                        val instPace = if (useStepDistance && cadenceSpeedMps > 0.35) {
                            (1609.344 / cadenceSpeedMps).roundToLong().coerceIn(180L, 3600L)
                        } else {
                            curr.currentPaceSecPerMile
                        }
                        val cadenceHr = when {
                            stepSnapshot.cadenceSpm >= 170 -> 164
                            stepSnapshot.cadenceSpm >= 150 -> 148
                            stepSnapshot.cadenceSpm >= 115 -> 128
                            stepSnapshot.cadenceSpm >= 80 -> 114
                            else -> curr.estimatedHeartRateBpm
                        }
                        curr.copy(
                            distanceMiles = resolvedDist,
                            avgPaceSecPerMile = avgPace,
                            currentPaceSecPerMile = instPace,
                            splitPacesSec = updatedSplits,
                            estimatedHeartRateBpm = bleWatchManager.state.value.liveHeartRateBpm ?: cadenceHr,
                            stepCount = stepSnapshot.sessionSteps,
                            cadenceSpm = stepSnapshot.cadenceSpm,
                            stepDistanceMiles = stepMiles,
                            stepSensorLabel = stepSnapshot.sensorSourceLabel
                        )
                    }
                }
            }
        }
        // Hands-free TTS Voice Channel reader for incoming Public & Pack chat messages
        viewModelScope.launch {
            publicChatState.collect { msgs ->
                val latest = msgs.lastOrNull() ?: return@collect
                if (lastSpokenPublicMsgId == null) {
                    lastSpokenPublicMsgId = latest.id
                    return@collect
                }
                if (_voiceChannelAutoReadEnabled.value &&
                    latest.id != lastSpokenPublicMsgId &&
                    latest.senderUid != currentUserId
                ) {
                    lastSpokenPublicMsgId = latest.id
                    audioEngine.speakCoachCue(
                        "${latest.senderName} in Public Hall says: ${latest.text}",
                        force = true
                    )
                } else {
                    lastSpokenPublicMsgId = latest.id
                }
            }
        }
        viewModelScope.launch {
            packChatState.collect { msgs ->
                val latest = msgs.lastOrNull() ?: return@collect
                if (lastSpokenPackMsgId == null) {
                    lastSpokenPackMsgId = latest.id
                    return@collect
                }
                if (_voiceChannelAutoReadEnabled.value &&
                    latest.id != lastSpokenPackMsgId &&
                    latest.senderUid != currentUserId
                ) {
                    lastSpokenPackMsgId = latest.id
                    audioEngine.speakCoachCue(
                        "${latest.senderName} in Pack Voice says: ${latest.text}",
                        force = true
                    )
                } else {
                    lastSpokenPackMsgId = latest.id
                }
            }
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun consumeRecentlySavedActivityId() {
        _recentlySavedActivityId.value = null
    }

    fun postMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun currentUnit(): DistanceUnit =
        (profileState.value as? UiState.Success)?.data?.unitEnum() ?: DistanceUnit.MILES

    fun currentAthleteName(): String =
        (profileState.value as? UiState.Success)?.data?.displayName ?: "Wolf Strider"

    fun currentWolfCoat(): WolfCoat =
        (profileState.value as? UiState.Success)?.data?.wolfCoatEnum() ?: WolfCoat.TIMBER_GREY

    fun observeRouteAnalysis(activityId: String): Flow<RouteAnalysisReport?> =
        localSql.observeRouteAnalysis(activityId, currentUserId)

    fun playVictoryHowl() {
        audioEngine.playWolfHowl()
        howlJob?.cancel()
        howlJob = viewModelScope.launch {
            _wolfHowlTrigger.value = true
            delay(3600L)
            _wolfHowlTrigger.value = false
        }
    }

    fun currentWolfGait(): WolfGait {
        if (_wolfHowlTrigger.value) return WolfGait.HOWL
        val t = _trackingState.value
        return SummitFormatters.resolveWolfGait(
            isRecording = t.status == TrackingStatus.RECORDING,
            activityType = t.activityType,
            paceSecPerMile = t.currentPaceSecPerMile.takeIf { it > 0L } ?: t.avgPaceSecPerMile,
            equestrianGait = if (t.activityType == ActivityType.EQUESTRIAN) t.equestrianState.currentGait else null
        )
    }

    // --- Voice Coach, Dynamic Alpha Soundboard & Speech-to-Text ---
    fun toggleVoiceCoach(enabled: Boolean) {
        _voiceCoachEnabled.value = enabled
        if (enabled) {
            audioEngine.speakCoachCue("The Alpha is on the line. Stay steady with the pack.", force = true)
        }
    }

    fun toggleSoundboardMute(muted: Boolean) {
        audioEngine.setMutedState(muted)
        _soundboardState.update { it.copy(isMuted = muted, activeTrigger = if (muted) null else it.activeTrigger) }
    }

    fun setSoundboardAmbientVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        audioEngine.setAmbientVolumeLevel(clamped)
        _soundboardState.update { it.copy(ambientVolume = clamped) }
    }

    fun setClimbGrowlThresholdPct(thresholdPct: Float) {
        val clamped = thresholdPct.coerceIn(5.0f, 15.0f)
        _soundboardState.update { it.copy(climbGrowlThresholdPct = clamped) }
    }

    fun setClimbAudioMode(mode: ClimbAudioMode) {
        _soundboardState.update { it.copy(climbAudioMode = mode) }
    }

    fun stopSoundboardAudio() {
        alphaCueJob?.cancel()
        audioEngine.stopAll()
        _soundboardState.update { it.copy(activeTrigger = null, isGeneratingAlphaCue = false) }
    }

    fun toggleLaunchHowlEnabled(enabled: Boolean) {
        audioEngine.setLaunchSoundEnabled(enabled)
        _soundboardState.update { it.copy(launchSoundEnabled = enabled) }
        _snackbarMessage.value = if (enabled) {
            "Opening Lone Wolf Howl enabled on app launch."
        } else {
            "Opening Lone Wolf Howl muted on launch."
        }
    }

    fun previewOpeningHowl() {
        audioEngine.playLoneWolfOpeningHowl(forcePreview = true)
    }

    fun triggerAlphaSoundboardCue(
        trigger: SoundboardTrigger,
        speakWithGeminiAlpha: Boolean = true
    ) {
        if (_soundboardState.value.isMuted) {
            _snackbarMessage.value = "Soundboard is muted. Unmute to hear Pack FX & The Alpha."
            return
        }

        // Play procedural hunting FX & synced haptics immediately
        audioEngine.playSoundboardTrigger(trigger = trigger, spokenAlphaCue = null)
        if (trigger == SoundboardTrigger.ALPHA_HOWL || trigger == SoundboardTrigger.CONSERVATION_MILESTONE) {
            howlJob?.cancel()
            howlJob = viewModelScope.launch {
                _wolfHowlTrigger.value = true
                delay(3600L)
                _wolfHowlTrigger.value = false
            }
        }

        if (!speakWithGeminiAlpha) {
            _soundboardState.update { it.copy(activeTrigger = trigger, lastAlphaError = null) }
            return
        }

        val snapshot = _trackingState.value
        val unit = currentUnit()
        val paceSec = snapshot.currentPaceSecPerMile.takeIf { it > 0L } ?: snapshot.avgPaceSecPerMile
        val paceLabel = if (paceSec > 0L) SummitFormatters.formatPace(paceSec, unit) else "warming up"
        val distLabel = SummitFormatters.formatDistanceShort(snapshot.distanceMiles, unit)
        val hrBpm = bleWatchManager.state.value.liveHeartRateBpm ?: snapshot.estimatedHeartRateBpm

        alphaCueJob?.cancel()
        alphaCueJob = viewModelScope.launch {
            _soundboardState.update {
                it.copy(
                    activeTrigger = trigger,
                    isGeneratingAlphaCue = true,
                    lastAlphaError = null
                )
            }
            try {
                val cue = paceCoachService.generateAlphaCoachCue(
                    trigger = trigger,
                    currentPace = paceLabel,
                    heartRateBpm = hrBpm,
                    distanceLabel = distLabel
                )
                _soundboardState.update {
                    it.copy(
                        isGeneratingAlphaCue = false,
                        lastAlphaCueText = cue,
                        lastAlphaError = null
                    )
                }
                audioEngine.speakCoachCue(cue, force = true)
            } catch (e: TimeoutCancellationException) {
                _soundboardState.update {
                    it.copy(
                        isGeneratingAlphaCue = false,
                        lastAlphaError = "The Alpha timed out. Check connection and tap a cue to retry."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Alpha soundboard cue failed", e)
                _soundboardState.update {
                    it.copy(
                        isGeneratingAlphaCue = false,
                        lastAlphaError = e.localizedMessage ?: "Couldn't reach The Alpha coach right now."
                    )
                }
            }
        }
    }

    fun updateTargetPaceSec(secPerMile: Long) {
        val clamped = secPerMile.coerceIn(240L, 1200L)
        _targetPaceSecPerMile.value = clamped
        paceTriggerService.setTargetPaceSeconds(clamped)
    }

    fun setTargetPaceMinSec(minutes: Int, seconds: Int) {
        val total = ((minutes * 60) + seconds).toLong().coerceIn(240L, 1200L)
        updateTargetPaceSec(total)
        val unit = currentUnit()
        _snackbarMessage.value = "Target pace locked to ${SummitFormatters.formatPace(total, unit)}."
    }

    fun toggleVoiceChannelAutoRead(enabled: Boolean) {
        _voiceChannelAutoReadEnabled.value = enabled
        _snackbarMessage.value = if (enabled) {
            "Pack Voice Channel ON — incoming voice & chat messages will read aloud."
        } else {
            "Pack Voice Channel muted."
        }
    }

    fun speakChatMessage(message: TavernChatMessage) {
        audioEngine.speakCoachCue("${message.senderName} says: ${message.text}", force = true)
    }

    fun startVoiceDictation(onFinalTranscript: (String) -> Unit) {
        _isListeningVoice.value = true
        _liveTranscript.value = ""
        voiceTranscriber.startListening(
            onPartialResult = { partial ->
                _liveTranscript.value = partial
            },
            onFinalResult = { finalText ->
                _isListeningVoice.value = false
                _liveTranscript.value = finalText
                onFinalTranscript(finalText)
            },
            onError = { err ->
                _isListeningVoice.value = false
                _snackbarMessage.value = err
            }
        )
    }

    fun stopVoiceDictation() {
        voiceTranscriber.stopListening()
        _isListeningVoice.value = false
    }

    fun saveVoiceNoteToActivity(activityId: String, transcript: String) {
        if (transcript.isBlank()) return
        viewModelScope.launch {
            localSql.saveVoiceNote(currentUserId, activityId, transcript)
            repository.saveActivityVoiceTranscript(activityId, transcript)
                .onSuccess { _snackbarMessage.value = "Voice trail note saved!" }
                .onFailure { _snackbarMessage.value = "Saved locally; cloud sync pending." }
        }
    }

    // --- AI Coaching, Adaptive Training Plans & Route Analysis ---
    fun requestPaceCoaching() {
        val activities = (activitiesState.value as? UiState.Success)?.data.orEmpty()
        val plan = weeklyPlanState.value
        val unit = currentUnit()

        viewModelScope.launch {
            _coachState.value = CoachUiState.Loading
            try {
                val insight = paceCoachService.generatePaceCoaching(activities, plan, unit)
                _coachState.value = CoachUiState.Ready(insight)
            } catch (e: TimeoutCancellationException) {
                _coachState.value = CoachUiState.Error(
                    "Coach took too long to respond. Please check your connection and tap Retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI coaching error", e)
                _coachState.value = CoachUiState.Error(
                    e.localizedMessage ?: "Couldn't reach your pace coach right now."
                )
            }
        }
    }

    fun generateAndSaveTrainingPlan(goalTitle: String, discipline: ActivityType) {
        val cleanGoal = goalTitle.trim()
        if (cleanGoal.isEmpty()) {
            _snackbarMessage.value = "Enter a training goal first."
            return
        }
        viewModelScope.launch {
            _trainingPlanLoading.value = true
            try {
                val activities = (activitiesState.value as? UiState.Success)?.data.orEmpty()
                val prof = (profileState.value as? UiState.Success)?.data
                val sources = buildList {
                    if (prof?.corosConnected == true) add("COROS Watch")
                    if (prof?.garminConnected == true) add("Garmin Connect")
                    if (prof?.stravaConnected == true) add("Strava")
                    if (_integrationsState.value.healthConnectPermissionsGranted) add("Health Connect")
                    if (isEmpty()) add("Striders Howl GPS & Manual Logs")
                }.joinToString(", ")

                val plan = paceCoachService.generateAdaptiveTrainingPlan(
                    goalTitle = cleanGoal,
                    discipline = discipline,
                    activities = activities,
                    connectedWatchSources = sources,
                    unit = currentUnit()
                )
                localSql.saveTrainingPlan(currentUserId, plan)
                _snackbarMessage.value = "Personalized training plan forged & saved!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Training plan error", e)
                _snackbarMessage.value = e.localizedMessage ?: "Could not generate training plan right now."
            } finally {
                _trainingPlanLoading.value = false
            }
        }
    }

    fun deleteTrainingPlan(planId: Long) {
        viewModelScope.launch {
            localSql.deleteTrainingPlan(currentUserId, planId)
            _snackbarMessage.value = "Training plan removed."
        }
    }

    fun analyzeRouteForActivity(activity: ActivityRecord) {
        if (activity.id.isBlank()) return
        viewModelScope.launch {
            _routeAnalysisLoadingId.value = activity.id
            try {
                val points = activity.decodedRoute()
                val startPt = points.firstOrNull()
                val weatherSummary = if (startPt != null) {
                    externalConnectors.fetchHistoricalWeatherSummary(
                        latitude = startPt.latitude,
                        longitude = startPt.longitude,
                        epochMs = activity.startedAtEpochMs.takeIf { it > 0L } ?: System.currentTimeMillis()
                    )
                } else {
                    "No GPS coordinates recorded — seasonal trail estimate"
                }

                // Compute elevation loss from route points (or mirror ~85% of gain for loop trails)
                var elevLoss = 0.0
                if (points.size >= 2) {
                    for (i in 1 until points.size) {
                        val diff = points[i - 1].elevationFeet - points[i].elevationFeet
                        if (diff in 3.0..180.0) elevLoss += diff
                    }
                }
                val finalLoss = if (elevLoss >= 10.0) elevLoss.roundToLong() else (activity.elevationGainFeet * 0.85).roundToLong()

                // Estimate incline slowdown % from fastest vs slowest mile split or climb density
                val inclineSlowdownPct = when {
                    activity.splitPacesSec.size >= 2 -> {
                        val minSplit = activity.splitPacesSec.minOrNull()?.toDouble() ?: 500.0
                        val maxSplit = activity.splitPacesSec.maxOrNull()?.toDouble() ?: minSplit
                        (((maxSplit - minSplit) / minSplit.coerceAtLeast(120.0)) * 100.0)
                            .roundToInt().coerceIn(0, 65)
                    }
                    activity.distanceMiles > 0.2 && activity.elevationGainFeet > 50L -> {
                        val ftPerMile = activity.elevationGainFeet / activity.distanceMiles
                        (ftPerMile / 28.0).roundToInt().coerceIn(3, 45)
                    }
                    else -> 0
                }

                val report = paceCoachService.analyzeCompletedRoute(
                    activity = activity,
                    weatherSummary = weatherSummary,
                    elevationLossFeet = finalLoss,
                    inclineSlowdownPct = inclineSlowdownPct,
                    unit = currentUnit()
                )
                localSql.saveRouteAnalysis(currentUserId, report)
                _snackbarMessage.value = "Route analysis complete!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Route analysis failed", e)
                _snackbarMessage.value = e.localizedMessage ?: "Route analysis failed."
            } finally {
                _routeAnalysisLoadingId.value = null
            }
        }
    }

    fun generateRecapForActivity(activity: ActivityRecord) {
        if (activity.id.isBlank()) return
        viewModelScope.launch {
            _activityRecapLoadingId.value = activity.id
            try {
                val recap = paceCoachService.generateActivityRecap(activity, currentUnit())
                repository.saveActivityCoachingSummary(activity.id, recap)
                    .onFailure { err ->
                        _snackbarMessage.value = "Could not save coaching note: ${err.localizedMessage}"
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to generate pace recap."
            } finally {
                _activityRecapLoadingId.value = null
            }
        }
    }

    // --- Live GPS Tracking & Equestrian Interval Engine ---
    fun setTrackingActivityType(type: ActivityType) {
        val completedRides = (activitiesState.value as? UiState.Success)?.data
            .orEmpty().count { it.typeEnum() == ActivityType.EQUESTRIAN }
        _trackingState.update {
            it.copy(
                activityType = type,
                equestrianState = EquestrianIntervalState.computeForElapsed(it.elapsedSeconds, completedRides)
            )
        }
    }

    fun selectTrackingGear(gearId: String?) {
        _trackingState.update { it.copy(selectedGearId = gearId) }
    }

    fun toggleOfflineOnlyMapMode() {
        _trackingState.update { it.copy(offlineOnlyMapMode = !it.offlineOnlyMapMode) }
    }

    @SuppressLint("MissingPermission")
    fun startLiveTracking(
        context: Context,
        hasLocationPermission: Boolean,
        hasActivityRecognitionPermission: Boolean = phoneStepCounter.hasRuntimeActivityPermission(),
        stepOnlyMode: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        lastMileMark = 0.0
        lastMileElapsedSec = 0L
        gpsSmoother.reset()
        paceTriggerService.reset()
        paceTriggerService.setTargetPaceSeconds(_targetPaceSecPerMile.value)
        audioEngine.stopContinuousClimbGrowl()
        val completedRides = (activitiesState.value as? UiState.Success)?.data
            .orEmpty().count { it.typeEnum() == ActivityType.EQUESTRIAN }
        val eqInit = EquestrianIntervalState.computeForElapsed(0L, completedRides)
        lastAnnouncedEquestrianGait = eqInit.currentGait

        val effectiveStepOnly = stepOnlyMode || !hasLocationPermission
        phoneStepCounter.startSession(
            activityType = _trackingState.value.activityType,
            hasActivityPermission = hasActivityRecognitionPermission
        )

        _trackingState.update {
            it.copy(
                status = TrackingStatus.RECORDING,
                elapsedSeconds = 0L,
                distanceMiles = 0.0,
                elevationGainFeet = 0.0,
                currentPaceSecPerMile = 0L,
                avgPaceSecPerMile = 0L,
                routePoints = emptyList(),
                splitPacesSec = emptyList(),
                startedAtEpochMs = now,
                equestrianState = eqInit,
                estimatedHeartRateBpm = bleWatchManager.state.value.liveHeartRateBpm ?: 112,
                gpsAccuracyMeters = 0f,
                gpsRejectedOutliers = 0,
                currentGradePercent = 0.0,
                climbGrowlIntensity = 0f,
                activePaceZone = PaceZone.ON_TARGET,
                stepCount = 0L,
                cadenceSpm = 0,
                stepDistanceMiles = 0.0,
                stepSensorLabel = phoneStepCounter.state.value.sensorSourceLabel,
                isStepOnlyMode = effectiveStepOnly
            )
        }
        if (_trackingState.value.activityType == ActivityType.EQUESTRIAN && _voiceCoachEnabled.value) {
            audioEngine.speakCoachCue("Equestrian endurance started. Begin with ${eqInit.currentGait.cueText}", force = true)
        }
        startTimerLoop()
        if (_sosState.value.fallDetectionEnabled) {
            fallDetector.startListening {
                triggerManualSosCountdown("Automated Winter Fall Detected (3.8g + 30s immobility)")
            }
        }
        if (_gloveControlState.value.isGloveModeEnabled) {
            gloveGestureDetector.startListening { gesture ->
                handleGloveGesture(gesture)
            }
        }
        if (!stepOnlyMode && hasLocationPermission) {
            attachGpsUpdates(context)
        } else {
            _snackbarMessage.value = "Phone Step Counter active (${phoneStepCounter.state.value.sensorSourceLabel}) — no watch or GPS required."
        }
    }

    fun startDailyPhoneStepCounter(hasActivityRecognitionPermission: Boolean) {
        phoneStepCounter.startDailyCounter(hasActivityPermission = hasActivityRecognitionPermission)
        val label = phoneStepCounter.state.value.sensorSourceLabel
        _snackbarMessage.value = "Phone Step Counter enabled ($label) — counting your steps with no watch needed."
    }

    fun stopDailyPhoneStepCounter() {
        phoneStepCounter.stopListening()
    }

    fun logTodayPhoneStepsAsQuest(activityType: ActivityType = ActivityType.HIKE) {
        val snap = phoneStepCounter.state.value
        val stepsToLog = snap.unloggedTodaySteps
        if (stepsToLog < 25L) {
            _snackbarMessage.value = "Walk at least 25 new steps with your phone to log a step quest."
            return
        }
        val distMiles = snap.unloggedDistanceMiles.coerceIn(0.05, 100.0)
        // Estimate realistic walking/running duration from step count (~105 spm walking, ~155 spm running)
        val estSpm = if (activityType == ActivityType.RUN) 155.0 else 105.0
        val estDurationSec = ((stepsToLog / estSpm) * 60.0).roundToLong().coerceIn(60L, 43200L)
        phoneStepCounter.markTodayStepsLogged()
        logActivityAndRecalculate(
            title = "Phone Step Quest (${stepsToLog} steps)",
            activityType = activityType,
            source = ActivitySource.MANUAL,
            distanceMiles = distMiles,
            durationSeconds = estDurationSec,
            elevationGainFeet = 0L,
            startedAtEpochMs = System.currentTimeMillis() - estDurationSec * 1000L,
            gearId = _trackingState.value.selectedGearId,
            notes = "Logged directly from ${snap.sensorSourceLabel}: $stepsToLog phone steps (no watch required).",
            openDetailOnSave = false
        )
    }

    @SuppressLint("MissingPermission")
    fun attachSingleLocationFix(context: Context) {
        try {
            val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
            client.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    _trackingState.update {
                        it.copy(
                            lastKnownLocation = GeoPointEle(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                elevationFeet = loc.altitude * 3.28084
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read last location", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun attachGpsUpdates(
        context: Context,
        intervalMs: Long = 2500L,
        minDistanceMeters: Float = 3f
    ) {
        detachGpsUpdates()
        try {
            val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
            locationClient = client
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                .setMinUpdateDistanceMeters(minDistanceMeters)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val state = _trackingState.value
                    if (state.status != TrackingStatus.RECORDING) return
                    val maxSpeed = if (state.activityType == ActivityType.EQUESTRIAN) {
                        GpsSmoothingService.MAX_EQUESTRIAN_SPEED_MPS
                    } else {
                        GpsSmoothingService.MAX_HUMAN_TRAIL_SPEED_MPS
                    }
                    for (loc in result.locations) {
                        val reportedAccuracy = if (loc.hasAccuracy() && loc.accuracy > 0f) {
                            loc.accuracy.toDouble()
                        } else {
                            12.0
                        }
                        val rawTrackPoint = TrackPoint(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            elevationMeters = loc.altitude,
                            timestampMs = loc.time.takeIf { it > 0L } ?: System.currentTimeMillis(),
                            accuracyMeters = reportedAccuracy,
                            speedMps = if (loc.hasSpeed()) loc.speed.toDouble() else 0.0
                        )
                        val smoothed = gpsSmoother.processLivePoint(rawTrackPoint, maxSpeedMps = maxSpeed)
                        if (smoothed == null) {
                            _trackingState.update { it.copy(gpsRejectedOutliers = gpsSmoother.rejectedOutlierCount) }
                            continue
                        }
                        val nextPoint = smoothed.toGeoPointEle()
                        _trackingState.update { curr ->
                            val prev = curr.routePoints.lastOrNull()
                            var addedMiles = 0.0
                            var addedElev = 0.0
                            var liveGradePct = curr.currentGradePercent
                            var liveGrowlIntensity = curr.climbGrowlIntensity
                            if (prev != null) {
                                val segMeters = GpsSmoothingService.calculateHaversineDistance(
                                    prev.latitude,
                                    prev.longitude,
                                    nextPoint.latitude,
                                    nextPoint.longitude
                                )
                                if (segMeters in 1.5..200.0) {
                                    addedMiles = segMeters / 1609.344
                                }
                                val dEle = nextPoint.elevationFeet - prev.elevationFeet
                                if (dEle in 2.0..150.0) {
                                    addedElev = dEle
                                }
                                if (segMeters >= 5.0) {
                                    liveGradePct = GpsSmoothingService.calculateGeoGradePercentage(prev, nextPoint)
                                    val sb = _soundboardState.value
                                    if (!sb.isMuted) {
                                        when (sb.climbAudioMode) {
                                            ClimbAudioMode.PERIODIC_PULSE -> {
                                                liveGrowlIntensity = PaceTriggerService.calculateGrowlVolume(
                                                    gradePercent = liveGradePct,
                                                    minGrade = sb.climbGrowlThresholdPct.toDouble()
                                                ).toFloat()
                                                if (liveGradePct >= sb.climbGrowlThresholdPct.toDouble()) {
                                                    val now = System.currentTimeMillis()
                                                    if (now - lastClimbCueMs > 45_000L) {
                                                        lastClimbCueMs = now
                                                        if (_voiceCoachEnabled.value) {
                                                            triggerAlphaSoundboardCue(SoundboardTrigger.STEEP_CLIMB_GROWL)
                                                        } else {
                                                            audioEngine.playSteepClimbGrowl(liveGradePct, continuousRumble = false)
                                                        }
                                                    }
                                                }
                                            }
                                            ClimbAudioMode.CONTINUOUS_RUMBLE -> {
                                                // Dedicated looping AudioTrack with EMA volume smoothing (4% -> 15%+)
                                                liveGrowlIntensity = audioEngine.updateContinuousClimbGrowl(
                                                    gradePercent = liveGradePct,
                                                    minGrade = 4.0
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            val newDist = curr.distanceMiles + addedMiles
                            val newElev = curr.elevationGainFeet + addedElev
                            val updatedSplits = curr.splitPacesSec.toMutableList()
                            if (newDist - lastMileMark >= 1.0) {
                                val splitSec = (curr.elapsedSeconds - lastMileElapsedSec).coerceIn(180L, 5400L)
                                updatedSplits += splitSec
                                lastMileMark = newDist
                                lastMileElapsedSec = curr.elapsedSeconds
                                if (_voiceCoachEnabled.value) {
                                    triggerAlphaSoundboardCue(SoundboardTrigger.ALPHA_HOWL)
                                }
                            }
                            val avgPace = if (newDist >= 0.03 && curr.elapsedSeconds > 0L) {
                                (curr.elapsedSeconds / newDist).roundToLong().coerceIn(120L, 7200L)
                            } else {
                                curr.avgPaceSecPerMile
                            }

                            // 12-second rolling window Pace Trigger evaluation (eliminates GPS pace spikes)
                            val paceEval = if (curr.activityType != ActivityType.EQUESTRIAN) {
                                paceTriggerService.updatePace(smoothed)
                            } else null

                            val instPace = paceEval?.rollingPaceSecPerMile
                                ?: if (loc.hasSpeed() && loc.speed > 0.4f) {
                                    (1609.344 / loc.speed).roundToLong().coerceIn(120L, 7200L)
                                } else {
                                    avgPace
                                }

                            val activeZone = paceEval?.currentZone ?: curr.activePaceZone
                            val zoneTransition = paceEval?.triggeredZoneTransition
                            if (zoneTransition != null && _voiceCoachEnabled.value) {
                                when (zoneTransition) {
                                    PaceZone.HUNTING_SURGE -> triggerAlphaSoundboardCue(SoundboardTrigger.ALPHA_HOWL)
                                    PaceZone.FALLING_BEHIND -> triggerAlphaSoundboardCue(SoundboardTrigger.STRIDE_CADENCE)
                                    PaceZone.ON_TARGET -> triggerAlphaSoundboardCue(
                                        trigger = SoundboardTrigger.PACK_LEADER_OVERTAKE,
                                        speakWithGeminiAlpha = false
                                    )
                                }
                            }

                            val effortHr = when {
                                instPace in 120L..450L -> 168
                                instPace in 451L..600L -> 154
                                instPace in 601L..840L -> 138
                                else -> 122
                            }
                            curr.copy(
                                distanceMiles = newDist,
                                elevationGainFeet = newElev,
                                avgPaceSecPerMile = avgPace,
                                currentPaceSecPerMile = instPace,
                                routePoints = curr.routePoints + nextPoint,
                                splitPacesSec = updatedSplits,
                                lastKnownLocation = nextPoint,
                                estimatedHeartRateBpm = bleWatchManager.state.value.liveHeartRateBpm ?: effortHr,
                                gpsAccuracyMeters = reportedAccuracy.toFloat(),
                                gpsRejectedOutliers = gpsSmoother.rejectedOutlierCount,
                                currentGradePercent = liveGradePct,
                                climbGrowlIntensity = liveGrowlIntensity,
                                activePaceZone = activeZone
                            )
                        }
                    }
                }
            }
            locationCallback = callback
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting GPS location updates", e)
        }
    }

    private fun detachGpsUpdates() {
        val cb = locationCallback
        val client = locationClient
        if (cb != null && client != null) {
            try {
                client.removeLocationUpdates(cb)
            } catch (_: Exception) {
            }
        }
        locationCallback = null
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val completedRides = (activitiesState.value as? UiState.Success)?.data
                    .orEmpty().count { it.typeEnum() == ActivityType.EQUESTRIAN }

                _trackingState.update { curr ->
                    if (curr.status != TrackingStatus.RECORDING) {
                        curr
                    } else {
                        val nextSec = curr.elapsedSeconds + 1L
                        val avgPace = if (curr.distanceMiles >= 0.03) {
                            (nextSec / curr.distanceMiles).roundToLong().coerceIn(120L, 7200L)
                        } else {
                            curr.avgPaceSecPerMile
                        }
                        val nextEq = EquestrianIntervalState.computeForElapsed(nextSec, completedRides)
                        if (curr.activityType == ActivityType.EQUESTRIAN &&
                            nextEq.currentGait != lastAnnouncedEquestrianGait
                        ) {
                            lastAnnouncedEquestrianGait = nextEq.currentGait
                            if (_voiceCoachEnabled.value) {
                                audioEngine.speakCoachCue(
                                    "Transition now to ${nextEq.currentGait.label}. ${nextEq.currentGait.cueText}",
                                    force = true
                                )
                            }
                        }
                        curr.copy(
                            elapsedSeconds = nextSec,
                            avgPaceSecPerMile = avgPace,
                            equestrianState = nextEq
                        )
                    }
                }
            }
        }
    }

    fun pauseTracking() {
        audioEngine.stopContinuousClimbGrowl()
        fallDetector.stopListening()
        gloveGestureDetector.stopListening()
        phoneStepCounter.pauseSession()
        _trackingState.update { it.copy(status = TrackingStatus.PAUSED, climbGrowlIntensity = 0f) }
    }

    fun resumeTracking() {
        _trackingState.update { it.copy(status = TrackingStatus.RECORDING) }
        phoneStepCounter.resumeSession()
        if (_sosState.value.fallDetectionEnabled) {
            fallDetector.startListening {
                triggerManualSosCountdown("Automated Winter Fall Detected (3.8g + 30s immobility)")
            }
        }
        if (_gloveControlState.value.isGloveModeEnabled) {
            gloveGestureDetector.startListening { gesture ->
                handleGloveGesture(gesture)
            }
        }
    }

    fun discardTracking() {
        timerJob?.cancel()
        detachGpsUpdates()
        fallDetector.stopListening()
        gloveGestureDetector.stopListening()
        phoneStepCounter.endSession(keepDailyCounterRunning = true)
        gpsSmoother.reset()
        paceTriggerService.reset()
        audioEngine.stopContinuousClimbGrowl()
        _trackingState.update {
            LiveTrackingState(
                activityType = it.activityType,
                lastKnownLocation = it.lastKnownLocation,
                offlineOnlyMapMode = it.offlineOnlyMapMode,
                selectedGearId = it.selectedGearId,
                stepSensorLabel = phoneStepCounter.state.value.sensorSourceLabel
            )
        }
    }

    fun finishAndSaveTrackedSession(
        customTitle: String,
        notes: String,
        voiceTranscript: String? = null
    ) {
        val snapshot = _trackingState.value
        timerJob?.cancel()
        detachGpsUpdates()
        fallDetector.stopListening()
        gloveGestureDetector.stopListening()
        phoneStepCounter.endSession(keepDailyCounterRunning = true)
        gpsSmoother.reset()
        paceTriggerService.reset()
        audioEngine.stopContinuousClimbGrowl()
        val distanceToSave = maxOf(snapshot.distanceMiles, snapshot.stepDistanceMiles).coerceAtLeast(0.05)
        val durationToSave = snapshot.elapsedSeconds.coerceAtLeast(60L)
        val title = customTitle.trim().ifBlank {
            "${snapshot.activityType.questLabel} Quest"
        }
        val stepNoteSuffix = if (snapshot.stepCount > 0L) {
            val cadenceStr = if (snapshot.cadenceSpm > 0) " · ${snapshot.cadenceSpm} spm" else ""
            "Phone Steps: ${snapshot.stepCount}$cadenceStr (${snapshot.stepSensorLabel})"
        } else ""
        val combinedNotes = listOf(notes.trim(), stepNoteSuffix)
            .filter { it.isNotEmpty() }
            .joinToString(" · ")

        // Simplify recorded trail with Ramer-Douglas-Peucker (2.5m epsilon) to preserve switchbacks cleanly
        val rdpSimplifiedRoute = GpsSmoothingService.simplifyGeoRoute(
            points = snapshot.routePoints,
            epsilonMeters = 2.5
        )

        _trackingState.update {
            LiveTrackingState(
                activityType = it.activityType,
                lastKnownLocation = it.lastKnownLocation,
                offlineOnlyMapMode = it.offlineOnlyMapMode,
                selectedGearId = it.selectedGearId,
                stepSensorLabel = phoneStepCounter.state.value.sensorSourceLabel
            )
        }

        logActivityAndRecalculate(
            title = title,
            activityType = snapshot.activityType,
            source = ActivitySource.GPS,
            distanceMiles = distanceToSave,
            durationSeconds = durationToSave,
            elevationGainFeet = snapshot.elevationGainFeet.roundToLong().coerceAtLeast(0L),
            startedAtEpochMs = if (snapshot.startedAtEpochMs > 0L) snapshot.startedAtEpochMs else System.currentTimeMillis(),
            gearId = snapshot.selectedGearId,
            notes = combinedNotes,
            voiceTranscript = voiceTranscript?.trim()?.takeIf { it.isNotEmpty() },
            routePoints = SummitFormatters.downsampleRoute(rdpSimplifiedRoute),
            splitPacesSec = snapshot.splitPacesSec,
            openDetailOnSave = true
        )
    }

    fun logManualActivity(
        title: String,
        activityType: ActivityType,
        distanceValueInUserUnit: Double,
        durationMinutes: Long,
        elevationInUserUnit: Long,
        notes: String,
        gearId: String? = null
    ) {
        val unit = currentUnit()
        val distanceMiles = if (unit == DistanceUnit.KILOMETERS) {
            distanceValueInUserUnit / 1.60934
        } else {
            distanceValueInUserUnit
        }.coerceIn(0.05, 250.0)

        val elevationFeet = if (unit == DistanceUnit.KILOMETERS) {
            (elevationInUserUnit * 3.28084).roundToLong()
        } else {
            elevationInUserUnit
        }.coerceIn(0L, 50000L)

        val durationSec = (durationMinutes * 60L).coerceIn(60L, 604800L)

        logActivityAndRecalculate(
            title = title.trim().ifBlank { "${activityType.questLabel} Session" },
            activityType = activityType,
            source = ActivitySource.MANUAL,
            distanceMiles = distanceMiles,
            durationSeconds = durationSec,
            elevationGainFeet = elevationFeet,
            startedAtEpochMs = System.currentTimeMillis() - durationSec * 1000L,
            gearId = gearId,
            notes = notes.trim().takeIf { it.isNotEmpty() },
            routePoints = emptyList(),
            splitPacesSec = emptyList(),
            openDetailOnSave = false
        )
    }

    private fun logActivityAndRecalculate(
        title: String,
        activityType: ActivityType,
        source: ActivitySource,
        distanceMiles: Double,
        durationSeconds: Long,
        elevationGainFeet: Long,
        startedAtEpochMs: Long,
        externalId: String? = null,
        gearId: String? = null,
        notes: String? = null,
        voiceTranscript: String? = null,
        routePoints: List<String> = emptyList(),
        splitPacesSec: List<Long> = emptyList(),
        openDetailOnSave: Boolean = false
    ) {
        viewModelScope.launch {
            val saveResult = repository.saveActivity(
                title = title,
                activityType = activityType,
                source = source,
                distanceMiles = distanceMiles,
                durationSeconds = durationSeconds,
                elevationGainFeet = elevationGainFeet,
                startedAtEpochMs = startedAtEpochMs,
                externalId = externalId,
                gearId = gearId,
                notes = notes,
                voiceTranscript = voiceTranscript,
                routePoints = routePoints,
                splitPacesSec = splitPacesSec
            )

            saveResult.onSuccess { newId ->
                if (!gearId.isNullOrBlank()) {
                    localSql.addMilesToGear(currentUserId, gearId, distanceMiles)
                }
                if (!voiceTranscript.isNullOrBlank()) {
                    localSql.saveVoiceNote(currentUserId, newId, voiceTranscript)
                }

                // Trigger 3D Wolf Victory Howl + audio
                playVictoryHowl()

                val currentProfile = (profileState.value as? UiState.Success)?.data
                val existingActivities = (activitiesState.value as? UiState.Success)?.data.orEmpty()
                val newActivity = ActivityRecord(
                    id = newId,
                    userId = currentUserId,
                    title = title,
                    activityType = activityType.name,
                    source = source.name,
                    distanceMiles = distanceMiles,
                    durationSeconds = durationSeconds,
                    avgPaceSecPerMile = (durationSeconds / distanceMiles.coerceAtLeast(0.01)).roundToLong().coerceIn(120L, 7200L),
                    elevationGainFeet = elevationGainFeet,
                    startedAtEpochMs = startedAtEpochMs,
                    externalId = externalId,
                    gearId = gearId,
                    notes = notes,
                    voiceTranscript = voiceTranscript,
                    routePoints = routePoints,
                    splitPacesSec = splitPacesSec
                )
                val updatedList = listOf(newActivity) + existingActivities.filterNot { it.id == newId }
                syncChallengesMilestonesAndRankings(updatedList, title)

                if (currentProfile?.healthConnectAutoExport == true &&
                    source != ActivitySource.HEALTH_CONNECT &&
                    _integrationsState.value.healthConnectPermissionsGranted
                ) {
                    healthConnectManager.exportActivity(newActivity)
                }

                if (openDetailOnSave) {
                    _recentlySavedActivityId.value = newId
                }
                _snackbarMessage.value = "Quest complete: \"$title\"! The wolf pack howls in tribute."
            }.onFailure { err ->
                _snackbarMessage.value = "Failed to save quest: ${err.localizedMessage}"
            }
        }
    }

    fun deleteActivity(activityId: String) {
        viewModelScope.launch {
            repository.deleteActivity(activityId)
                .onSuccess {
                    val remaining = (activitiesState.value as? UiState.Success)?.data.orEmpty()
                        .filterNot { it.id == activityId }
                    syncChallengesMilestonesAndRankings(remaining, remaining.firstOrNull()?.title ?: "Ready at the Hearth")
                    _snackbarMessage.value = "Quest removed."
                }
                .onFailure { err ->
                    _snackbarMessage.value = "Delete failed: ${err.localizedMessage}"
                }
        }
    }

    private suspend fun syncChallengesMilestonesAndRankings(
        activities: List<ActivityRecord>,
        latestQuestTitle: String
    ) {
        val prof = (profileState.value as? UiState.Success)?.data
        val baseline = prof?.weeklyBaselineMiles ?: 10.0
        val challenges = (challengesState.value as? UiState.Success)?.data.orEmpty()
        val plan = WeeklyChallengeEngine.calculateProgressivePlan(
            activities = activities,
            pastChallenges = challenges,
            baselineMiles = baseline
        )

        repository.upsertWeeklyChallenge(
            weekKey = plan.weekKey,
            targetMiles = plan.targetMiles,
            loggedMiles = plan.loggedMiles,
            difficultyTier = plan.difficultyTier,
            progressionReason = plan.progressionReason,
            challengeStatus = plan.challengeStatus
        )
        repository.updateStreak(plan.computedStreakWeeks)

        val candidates = WeeklyChallengeEngine.evaluateMilestones(activities, plan)
        var newlyUnlockedTitle: String? = null
        for (c in candidates) {
            val res = repository.unlockMilestoneIfAbsent(
                badgeKey = c.badgeKey,
                title = c.title,
                subtitle = c.subtitle,
                metricHighlight = c.metricHighlight
            )
            if (res.getOrNull() == true && newlyUnlockedTitle == null) {
                newlyUnlockedTitle = c.title
            }
        }
        if (newlyUnlockedTitle != null) {
            _snackbarMessage.value = "Achievement Unlocked: $newlyUnlockedTitle!"
        }

        // Publish live Solo & Pack Rankings immediately as distance changes
        val totalMiles = activities.sumOf { it.distanceMiles }
        val rank = AdventurerRank.compute(activities)
        val coat = prof?.wolfCoatEnum() ?: WolfCoat.TIMBER_GREY
        val displayName = prof?.displayName ?: "Wolf Strider"
        repository.publishSoloRanking(
            displayName = displayName,
            wolfCoat = coat,
            packClass = rank.packClass,
            level = rank.level,
            totalMiles = totalMiles,
            weeklyMiles = plan.loggedMiles
        )

        val party = tavernPartyState.value
        if (party != null) {
            repository.syncMyStatsToParty(
                party = party,
                myDisplayName = displayName,
                myTotalMiles = totalMiles,
                myLastQuest = latestQuestTitle
            )
            val updatedParty = if (party.creatorUid == currentUserId) {
                party.copy(primaryName = displayName, primaryTotalMiles = totalMiles, primaryLastQuest = latestQuestTitle)
            } else {
                party.copy(partnerName = displayName, partnerTotalMiles = totalMiles, partnerLastQuest = latestQuestTitle)
            }
            repository.publishPackRanking(updatedParty, coat)
        }
    }

    fun markMilestoneShared(milestoneId: String) {
        viewModelScope.launch {
            repository.incrementMilestoneShareCount(milestoneId)
        }
    }

    // --- Public & Pack Chat + Partner Party Co-op ---
    fun sendPublicChat(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.sendPublicChatMessage(
                senderName = currentAthleteName(),
                wolfCoat = currentWolfCoat(),
                packClass = adventurerRankState.value.packClass,
                text = clean
            ).onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Unable to post to Public Hall."
            }
        }
    }

    fun sendPackChat(text: String) {
        val clean = text.trim()
        val code = (profileState.value as? UiState.Success)?.data?.partnerPartyCode.orEmpty()
        if (clean.isEmpty() || code.length < 6) {
            _snackbarMessage.value = "Create or join a Wolf Pack first to use Pack Chat."
            return
        }
        viewModelScope.launch {
            repository.sendPackChatMessage(
                partyCode = code,
                senderName = currentAthleteName(),
                wolfCoat = currentWolfCoat(),
                packClass = adventurerRankState.value.packClass,
                text = clean
            ).onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Unable to post to Pack Chat."
            }
        }
    }

    fun createPackParty(
        packName: String = "",
        packCrest: com.example.data.model.PackCrest = com.example.data.model.PackCrest.ALPHA_HOWL,
        weeklyGoalMiles: Double = 250.0,
        isPublic: Boolean = false
    ) {
        viewModelScope.launch {
            val acts = (activitiesState.value as? UiState.Success)?.data.orEmpty()
            val totalMiles = acts.sumOf { it.distanceMiles }
            val lastTitle = acts.firstOrNull()?.title ?: "Ready at the Hearth"
            val resolvedName = packName.trim().ifBlank { "${currentAthleteName()}'s Pack" }
            repository.createTavernParty(
                displayName = currentAthleteName(),
                totalMiles = totalMiles,
                lastQuestTitle = lastTitle,
                packName = resolvedName,
                packCrest = packCrest,
                weeklyGoalMiles = weeklyGoalMiles,
                isPublic = isPublic
            ).onSuccess { code ->
                repository.publishPackRanking(
                    TavernParty(
                        partyCode = code,
                        creatorUid = currentUserId,
                        memberUids = listOf(currentUserId),
                        packName = resolvedName,
                        packCrest = packCrest.name,
                        weeklyGoalMiles = weeklyGoalMiles,
                        isPublic = isPublic,
                        primaryName = currentAthleteName(),
                        primaryTotalMiles = totalMiles,
                        primaryLastQuest = lastTitle
                    ),
                    currentWolfCoat()
                )
                playVictoryHowl()
                _snackbarMessage.value = "$resolvedName launched! Invite code: $code"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to create Pack."
            }
        }
    }

    fun joinPackParty(rawCode: String) {
        viewModelScope.launch {
            val acts = (activitiesState.value as? UiState.Success)?.data.orEmpty()
            val totalMiles = acts.sumOf { it.distanceMiles }
            val lastTitle = acts.firstOrNull()?.title ?: "Joined the Pack!"
            repository.joinTavernParty(
                rawCode = rawCode,
                partnerDisplayName = currentAthleteName(),
                partnerMiles = totalMiles,
                partnerLastQuest = lastTitle
            ).onSuccess { code ->
                _snackbarMessage.value = "Joined Wolf Pack $code! Combined miles are now live."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not join Wolf Pack."
            }
        }
    }

    fun renamePackParty(newPackName: String) {
        val party = tavernPartyState.value ?: return
        val clean = newPackName.trim().take(60)
        if (clean.isEmpty()) {
            _snackbarMessage.value = "Pack name cannot be empty."
            return
        }
        viewModelScope.launch {
            repository.renameTavernParty(party.partyCode, clean)
                .onSuccess {
                    repository.publishPackRanking(
                        party.copy(packName = clean),
                        currentWolfCoat()
                    )
                    _snackbarMessage.value = "Wolf Pack renamed to \"$clean\"!"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Unable to rename Pack."
                }
        }
    }

    fun updateDisplayName(newUsername: String) {
        val clean = newUsername.trim().take(40)
        if (clean.length < 2) {
            _snackbarMessage.value = "Username must be at least 2 characters."
            return
        }
        viewModelScope.launch {
            repository.updateProfilePreferences(displayName = clean)
                .onSuccess {
                    val acts = (activitiesState.value as? UiState.Success)?.data.orEmpty()
                    val rank = AdventurerRank.compute(acts)
                    repository.publishSoloRanking(
                        displayName = clean,
                        wolfCoat = currentWolfCoat(),
                        packClass = rank.packClass,
                        level = rank.level,
                        totalMiles = acts.sumOf { it.distanceMiles },
                        weeklyMiles = weeklyPlanState.value.loggedMiles
                    )
                    tavernPartyState.value?.let { party ->
                        repository.syncMyStatsToParty(
                            party = party,
                            myDisplayName = clean,
                            myTotalMiles = acts.sumOf { it.distanceMiles },
                            myLastQuest = acts.firstOrNull()?.title ?: "On the trail"
                        )
                    }
                    _snackbarMessage.value = "Strider username updated to $clean!"
                }
                .onFailure { err ->
                    _snackbarMessage.value = err.localizedMessage ?: "Could not update username."
                }
        }
    }

    fun sendPartyCheer() {
        val code = tavernPartyState.value?.partyCode ?: return
        viewModelScope.launch {
            repository.sendPartyCheer(code)
                .onSuccess {
                    playVictoryHowl()
                    _snackbarMessage.value = "Sent a rallying Pack Howl to your partner!"
                }
        }
    }

    // --- Gear Mileage (Room SQLite: Shoes & Equestrian Saddles) ---
    fun addGearItem(
        name: String,
        category: String,
        brand: String,
        startingMiles: Double,
        maxMiles: Double
    ) {
        viewModelScope.launch {
            localSql.saveGearItem(
                userId = currentUserId,
                name = name,
                category = category,
                brand = brand,
                startingMiles = startingMiles,
                maxMiles = maxMiles
            )
            _snackbarMessage.value = "Added $name to your Gear Rack."
        }
    }

    fun toggleGearRetired(gearId: String, retired: Boolean) {
        viewModelScope.launch {
            localSql.toggleGearRetired(currentUserId, gearId, retired)
        }
    }

    fun deleteGear(gearId: String) {
        viewModelScope.launch {
            localSql.deleteGear(currentUserId, gearId)
            _snackbarMessage.value = "Gear item removed."
        }
    }

    // --- Custom Quest Trails (Room SQLite + COROS GPX Export) ---
    fun saveCustomTrail(
        name: String,
        discipline: ActivityType,
        points: List<GeoPointEle>,
        distanceMiles: Double
    ) {
        if (points.size < 2) {
            _snackbarMessage.value = "Tap at least 2 points on the map to save a quest trail."
            return
        }
        viewModelScope.launch {
            localSql.saveCustomTrail(
                userId = currentUserId,
                name = name,
                discipline = discipline,
                encodedPoints = points.map { it.encode() },
                distanceMiles = distanceMiles
            )
            _snackbarMessage.value = "Saved custom trail \"$name\" (${SummitFormatters.formatDistance(distanceMiles, currentUnit())})!"
        }
    }

    fun deleteCustomTrail(trailId: String) {
        viewModelScope.launch {
            localSql.deleteCustomTrail(currentUserId, trailId)
            _snackbarMessage.value = "Custom trail removed."
        }
    }

    // --- Profile Preferences, Wolf Coat & Season Theme ---
    fun updateUnit(unit: DistanceUnit) {
        viewModelScope.launch {
            repository.updateProfilePreferences(preferredUnit = unit)
                .onFailure { _snackbarMessage.value = "Could not update unit preference." }
        }
    }

    fun updateWolfCoat(coat: WolfCoat) {
        viewModelScope.launch {
            repository.updateProfilePreferences(wolfCoat = coat)
                .onSuccess {
                    val acts = (activitiesState.value as? UiState.Success)?.data.orEmpty()
                    val rank = AdventurerRank.compute(acts)
                    repository.publishSoloRanking(
                        displayName = currentAthleteName(),
                        wolfCoat = coat,
                        packClass = rank.packClass,
                        level = rank.level,
                        totalMiles = acts.sumOf { it.distanceMiles },
                        weeklyMiles = weeklyPlanState.value.loggedMiles
                    )
                    _snackbarMessage.value = "Wolf companion coat updated to ${coat.displayName}!"
                }
        }
    }

    fun updateSeasonTheme(theme: SeasonTheme) {
        viewModelScope.launch {
            repository.updateProfilePreferences(seasonTheme = theme)
                .onSuccess { _snackbarMessage.value = "Tavern atmosphere set to ${theme.label}." }
        }
    }

    fun markPathboundConnected() {
        viewModelScope.launch {
            repository.updateProfilePreferences(pathboundConnected = true)
        }
    }

    fun updateWeeklyBaseline(miles: Double) {
        viewModelScope.launch {
            repository.updateProfilePreferences(weeklyBaselineMiles = miles)
                .onSuccess {
                    val acts = (activitiesState.value as? UiState.Success)?.data.orEmpty()
                    val challenges = (challengesState.value as? UiState.Success)?.data.orEmpty()
                    val plan = WeeklyChallengeEngine.calculateProgressivePlan(acts, challenges, miles)
                    repository.upsertWeeklyChallenge(
                        weekKey = plan.weekKey,
                        targetMiles = plan.targetMiles,
                        loggedMiles = plan.loggedMiles,
                        difficultyTier = plan.difficultyTier,
                        progressionReason = plan.progressionReason,
                        challengeStatus = plan.challengeStatus
                    )
                    _snackbarMessage.value = "Weekly Pack quest baseline updated!"
                }
                .onFailure { _snackbarMessage.value = "Failed updating weekly baseline." }
        }
    }

    fun toggleHealthConnectAutoExport(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateProfilePreferences(healthConnectAutoExport = enabled)
        }
    }

    // --- Health Connect, COROS, Garmin & Strava Integrations ---
    fun refreshIntegrationStatus() {
        viewModelScope.launch {
            val snapshot = healthConnectManager.getStatusSnapshot()
            _integrationsState.update {
                it.copy(
                    healthConnectAvailability = snapshot.availability,
                    healthConnectPermissionsGranted = snapshot.allPermissionsGranted,
                    healthConnectSnapshot = snapshot,
                    corosOAuthConfigured = corosManager.hasConfiguredOAuthCredentials(),
                    stravaOAuthConfigured = externalConnectors.hasStravaCredentials(),
                    garminOAuthConfigured = externalConnectors.hasGarminCredentials(),
                    hasCorosSession = corosManager.hasStoredSession(),
                    hasStravaSession = externalConnectors.hasStoredStravaSession(),
                    hasGarminSession = externalConnectors.hasStoredGarminSession()
                )
            }
        }
    }

    fun syncFromHealthConnect() {
        viewModelScope.launch {
            _integrationsState.update { it.copy(isSyncingHealthConnect = true) }
            val result = healthConnectManager.readRecentRunsAndHikes(30L)
            result.onSuccess { imported ->
                val existingExternalIds = (activitiesState.value as? UiState.Success)?.data
                    .orEmpty().mapNotNull { it.externalId }.toSet()

                var added = 0
                for (item in imported) {
                    if (item.externalId in existingExternalIds) continue
                    val saveRes = repository.saveActivity(
                        title = item.title,
                        activityType = item.activityType,
                        source = item.source,
                        distanceMiles = item.distanceMiles,
                        durationSeconds = item.durationSeconds,
                        elevationGainFeet = item.elevationGainFeet,
                        startedAtEpochMs = item.startedAtEpochMs,
                        externalId = item.externalId,
                        notes = item.notes
                    )
                    if (saveRes.isSuccess) added++
                }
                if (added > 0) {
                    val acts = (activitiesState.value as? UiState.Success)?.data.orEmpty()
                    syncChallengesMilestonesAndRankings(acts, imported.firstOrNull()?.title ?: "Health Connect Sync")
                }
                val summary = if (added > 0) {
                    "Synced $added new workout(s) from Health Connect!"
                } else {
                    "Health Connect is up to date (${imported.size} session(s) checked)."
                }
                _integrationsState.update {
                    it.copy(isSyncingHealthConnect = false, lastSyncSummary = summary)
                }
                _snackbarMessage.value = summary
            }.onFailure { err ->
                _integrationsState.update {
                    it.copy(
                        isSyncingHealthConnect = false,
                        lastSyncSummary = err.localizedMessage ?: "Health Connect sync failed."
                    )
                }
                _snackbarMessage.value = err.localizedMessage ?: "Health Connect sync failed."
            }
        }
    }

    fun handleCorosOAuthCallback(authCode: String) {
        viewModelScope.launch {
            _integrationsState.update { it.copy(isSyncingCoros = true) }
            val result = corosManager.exchangeAndSyncCorosWorkouts(authCode)
            handleCorosSyncResult(result, connectedNow = true)
        }
    }

    fun syncConnectedCoros(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _integrationsState.update { it.copy(isSyncingCoros = true) }
            val result = corosManager.refreshAndSyncCorosWorkouts(forceRefresh = forceRefresh)
            handleCorosSyncResult(result, connectedNow = false)
        }
    }

    private suspend fun handleCorosSyncResult(
        result: Result<List<com.example.data.integrations.ImportedExternalActivity>>,
        connectedNow: Boolean
    ) {
        result.onSuccess { workouts ->
            repository.updateProfilePreferences(corosConnected = true)
            val existingIds = (activitiesState.value as? UiState.Success)?.data
                .orEmpty().mapNotNull { it.externalId }.toSet()
            var added = 0
            for (w in workouts) {
                if (w.externalId in existingIds) continue
                if (repository.saveActivity(
                        title = w.title,
                        activityType = w.activityType,
                        source = ActivitySource.COROS,
                        distanceMiles = w.distanceMiles,
                        durationSeconds = w.durationSeconds,
                        elevationGainFeet = w.elevationGainFeet,
                        startedAtEpochMs = w.startedAtEpochMs,
                        externalId = w.externalId,
                        notes = w.notes
                    ).isSuccess
                ) added++
            }
            val msg = if (connectedNow) {
                "COROS connected via Cloud Functions! Imported $added new workout(s)."
            } else {
                "COROS synced! Imported $added new workout(s)."
            }
            _integrationsState.update {
                it.copy(
                    isSyncingCoros = false,
                    hasCorosSession = corosManager.hasStoredSession(),
                    lastSyncSummary = msg
                )
            }
            _snackbarMessage.value = msg
        }.onFailure { err ->
            _integrationsState.update {
                it.copy(isSyncingCoros = false, lastSyncSummary = err.localizedMessage)
            }
            _snackbarMessage.value = err.localizedMessage ?: "COROS sync failed."
        }
    }

    fun handleStravaOAuthCallback(authCode: String) {
        viewModelScope.launch {
            _integrationsState.update { it.copy(isSyncingStrava = true) }
            val result = externalConnectors.exchangeAndSyncStrava(authCode)
            handleStravaSyncResult(result, connectedNow = true)
        }
    }

    fun syncConnectedStrava(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _integrationsState.update { it.copy(isSyncingStrava = true) }
            val result = externalConnectors.refreshAndSyncStrava(forceRefresh = forceRefresh)
            handleStravaSyncResult(result, connectedNow = false)
        }
    }

    private suspend fun handleStravaSyncResult(
        result: Result<List<com.example.data.integrations.ImportedExternalActivity>>,
        connectedNow: Boolean
    ) {
        result.onSuccess { workouts ->
            repository.updateProfilePreferences(stravaConnected = true)
            val existingIds = (activitiesState.value as? UiState.Success)?.data
                .orEmpty().mapNotNull { it.externalId }.toSet()
            var added = 0
            for (w in workouts) {
                if (w.externalId in existingIds) continue
                if (repository.saveActivity(
                        title = w.title,
                        activityType = w.activityType,
                        source = ActivitySource.STRAVA,
                        distanceMiles = w.distanceMiles,
                        durationSeconds = w.durationSeconds,
                        elevationGainFeet = w.elevationGainFeet,
                        startedAtEpochMs = w.startedAtEpochMs,
                        externalId = w.externalId,
                        notes = w.notes
                    ).isSuccess
                ) added++
            }
            val msg = if (connectedNow) {
                "Strava connected via Cloud Functions! Imported $added new workout(s)."
            } else {
                "Strava synced! Imported $added new workout(s)."
            }
            _integrationsState.update {
                it.copy(
                    isSyncingStrava = false,
                    hasStravaSession = externalConnectors.hasStoredStravaSession(),
                    lastSyncSummary = msg
                )
            }
            _snackbarMessage.value = msg
        }.onFailure { err ->
            _integrationsState.update {
                it.copy(isSyncingStrava = false, lastSyncSummary = err.localizedMessage)
            }
            _snackbarMessage.value = err.localizedMessage ?: "Strava sync failed."
        }
    }

    fun handleGarminOAuthCallback(authCode: String = "") {
        viewModelScope.launch {
            if (authCode.isBlank() || authCode == "authorized") {
                repository.updateProfilePreferences(garminConnected = true)
                _snackbarMessage.value = "Garmin Connect authorized! Sync via Google Health Connect or GPX/TCX import."
                return@launch
            }
            _integrationsState.update { it.copy(isSyncingGarmin = true) }
            val result = externalConnectors.exchangeGarminAuthorizationCode(authCode)
            result.onSuccess {
                repository.updateProfilePreferences(garminConnected = true)
                val msg = "Garmin Connect linked via Cloud Functions! Ready for sync & GPX/TCX import."
                _integrationsState.update {
                    it.copy(
                        isSyncingGarmin = false,
                        hasGarminSession = externalConnectors.hasStoredGarminSession(),
                        lastSyncSummary = msg
                    )
                }
                _snackbarMessage.value = msg
            }.onFailure { err ->
                _integrationsState.update {
                    it.copy(isSyncingGarmin = false, lastSyncSummary = err.localizedMessage)
                }
                _snackbarMessage.value = err.localizedMessage ?: "Garmin OAuth exchange failed."
            }
        }
    }

    fun refreshConnectedGarmin() {
        viewModelScope.launch {
            _integrationsState.update { it.copy(isSyncingGarmin = true) }
            val result = externalConnectors.refreshGarminToken()
            result.onSuccess {
                val msg = "Garmin session refreshed securely via Cloud Functions."
                _integrationsState.update {
                    it.copy(
                        isSyncingGarmin = false,
                        hasGarminSession = externalConnectors.hasStoredGarminSession(),
                        lastSyncSummary = msg
                    )
                }
                _snackbarMessage.value = msg
            }.onFailure { err ->
                _integrationsState.update {
                    it.copy(isSyncingGarmin = false, lastSyncSummary = err.localizedMessage)
                }
                _snackbarMessage.value = err.localizedMessage ?: "Garmin session refresh failed."
            }
        }
    }

    fun importWatchGpxFile(context: Context, uri: Uri, source: ActivitySource = ActivitySource.COROS) {
        viewModelScope.launch {
            _integrationsState.update { it.copy(isSyncingCoros = true) }
            val parsed = corosManager.parseGpxOrTcxFromUri(context, uri)
            parsed.onSuccess { track ->
                when (source) {
                    ActivitySource.COROS -> repository.updateProfilePreferences(corosConnected = true)
                    ActivitySource.GARMIN -> repository.updateProfilePreferences(garminConnected = true)
                    ActivitySource.STRAVA -> repository.updateProfilePreferences(stravaConnected = true)
                    else -> Unit
                }
                logActivityAndRecalculate(
                    title = track.title,
                    activityType = track.activityType,
                    source = source,
                    distanceMiles = track.distanceMiles,
                    durationSeconds = track.durationSeconds,
                    elevationGainFeet = track.elevationGainFeet,
                    startedAtEpochMs = track.startedAtEpochMs,
                    externalId = track.externalId,
                    notes = "Imported from ${source.displayName} GPX/TCX file",
                    routePoints = SummitFormatters.downsampleRoute(track.routePoints),
                    splitPacesSec = track.splitPacesSec,
                    openDetailOnSave = false
                )
                _integrationsState.update {
                    it.copy(
                        isSyncingCoros = false,
                        lastSyncSummary = "Imported ${source.displayName} track \"${track.title}\"!"
                    )
                }
            }.onFailure { err ->
                _integrationsState.update { it.copy(isSyncingCoros = false) }
                _snackbarMessage.value = err.localizedMessage ?: "Could not parse GPX/TCX file."
            }
        }
    }

    // --- Offline Maps ---
    fun refreshSavedOfflineRegions() {
        viewModelScope.launch {
            val regions = offlineMapTileManager.getSavedRegions()
            _offlineMapState.update { it.copy(savedRegions = regions) }
        }
    }

    fun downloadPresetRegion(preset: PresetTrailRegion) {
        downloadOfflineRegion(
            regionId = preset.id,
            name = preset.name,
            lat = preset.centerLat,
            lon = preset.centerLon,
            radiusKm = preset.radiusKm
        )
    }

    fun downloadCurrentAreaOffline(customName: String, lat: Double, lon: Double) {
        val safeName = customName.trim().ifBlank { "My Local Trail Pack" }
        val id = "custom_${System.currentTimeMillis()}"
        downloadOfflineRegion(id, safeName, lat, lon, radiusKm = 3.0)
    }

    private fun downloadOfflineRegion(
        regionId: String,
        name: String,
        lat: Double,
        lon: Double,
        radiusKm: Double
    ) {
        if (_offlineMapState.value.isDownloading) return
        viewModelScope.launch {
            _offlineMapState.update {
                it.copy(
                    isDownloading = true,
                    downloadingName = name,
                    downloadedTiles = 0,
                    totalTiles = 1
                )
            }
            val result = offlineMapTileManager.downloadOfflineRegion(
                regionId = regionId,
                name = name,
                centerLat = lat,
                centerLon = lon,
                radiusKm = radiusKm
            ) { done, total ->
                _offlineMapState.update {
                    it.copy(downloadedTiles = done, totalTiles = total.coerceAtLeast(1))
                }
            }
            val updatedRegions = offlineMapTileManager.getSavedRegions()
            _offlineMapState.update {
                it.copy(isDownloading = false, savedRegions = updatedRegions)
            }
            result.onSuccess { reg ->
                _snackbarMessage.value = "Downloaded \"${reg.name}\" (${reg.tileCount} map tiles) for offline use!"
            }.onFailure { err ->
                _snackbarMessage.value = "Offline download failed: ${err.localizedMessage}"
            }
        }
    }

    fun deleteOfflineRegion(regionId: String) {
        viewModelScope.launch {
            val remaining = offlineMapTileManager.deleteRegion(regionId)
            _offlineMapState.update { it.copy(savedRegions = remaining) }
            _snackbarMessage.value = "Removed offline map pack."
        }
    }

    // --- Direct Watch Bluetooth LE Heart Rate Stream (Garmin / COROS / BLE HR) ---
    fun startBleWatchScan() {
        bleWatchManager.startScanning()
    }

    fun stopBleWatchScan() {
        bleWatchManager.stopScanning()
    }

    fun connectBleWatch(address: String, displayName: String) {
        bleWatchManager.connectToWatch(address, displayName)
    }

    fun disconnectBleWatch() {
        bleWatchManager.disconnect()
    }

    // --- Winter Trail SOS ("Howl for Help") ---
    fun toggleFallDetection(enabled: Boolean) {
        _sosState.update { it.copy(fallDetectionEnabled = enabled) }
        if (!enabled) {
            fallDetector.stopListening()
        } else if (_trackingState.value.status == TrackingStatus.RECORDING) {
            fallDetector.startListening {
                triggerManualSosCountdown("Automated Winter Fall Detected (3.8g + 30s immobility)")
            }
        }
    }

    fun triggerManualSosCountdown(reason: String = "Manual Winter Trail SOS") {
        if (_sosState.value.isBeaconActive || _sosState.value.isCountdownActive) return
        sosCountdownJob?.cancel()
        sosController.announceCountdownWarning()
        _sosState.update {
            it.copy(
                isCountdownActive = true,
                countdownSecondsLeft = 45,
                triggerReason = reason,
                batteryPercent = sosController.readBatteryPercent()
            )
        }
        sosCountdownJob = viewModelScope.launch {
            for (sec in 44 downTo 0) {
                delay(1000L)
                if (!_sosState.value.isCountdownActive) return@launch
                _sosState.update { it.copy(countdownSecondsLeft = sec) }
                if (sec == 0) {
                    _sosState.update { it.copy(isCountdownActive = false) }
                    activateSosBeaconInternal(launchSmsIntent = false)
                }
            }
        }
    }

    fun cancelSosCountdown() {
        sosCountdownJob?.cancel()
        _sosState.update { it.copy(isCountdownActive = false, countdownSecondsLeft = 45) }
        _snackbarMessage.value = "Winter SOS countdown cancelled."
    }

    fun confirmAndActivateSos(context: Context) {
        sosCountdownJob?.cancel()
        _sosState.update { it.copy(isCountdownActive = false) }
        activateSosBeaconInternal(launchSmsIntent = true, contextForSms = context)
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            // Switch GPS to 30-second cold-weather battery preservation polling
            attachGpsUpdates(context, intervalMs = 30_000L, minDistanceMeters = 5f)
        }
    }

    private fun activateSosBeaconInternal(
        launchSmsIntent: Boolean,
        contextForSms: Context? = null
    ) {
        audioEngine.stopContinuousClimbGrowl()
        sosController.startAcousticBeacon()
        val contactsCsv = sosController.getSavedEmergencyContacts()
        val contactCount = contactsCsv.split(",", ";").map { it.trim() }.count { it.isNotEmpty() }
        val batt = sosController.readBatteryPercent()
        val startedAt = System.currentTimeMillis()
        _sosState.update {
            it.copy(
                isBeaconActive = true,
                isAcousticBeaconPlaying = true,
                sosStartedAtMs = startedAt,
                activeBeaconSeconds = 0L,
                batteryPercent = batt,
                contactsNotifiedCount = contactCount.coerceAtLeast(1),
                emergencyContacts = contactsCsv
            )
        }
        sosActiveTimerJob?.cancel()
        sosActiveTimerJob = viewModelScope.launch {
            while (_sosState.value.isBeaconActive) {
                delay(1000L)
                _sosState.update { curr ->
                    curr.copy(
                        activeBeaconSeconds = curr.activeBeaconSeconds + 1L,
                        batteryPercent = if (curr.activeBeaconSeconds % 30L == 0L) {
                            sosController.readBatteryPercent()
                        } else {
                            curr.batteryPercent
                        }
                    )
                }
            }
        }
        if (launchSmsIntent && contextForSms != null) {
            dispatchSosSms(contextForSms)
        }
    }

    fun toggleAcousticRescueBeacon(enabled: Boolean) {
        if (enabled) {
            sosController.startAcousticBeacon()
        } else {
            sosController.stopAcousticBeacon()
        }
        _sosState.update { it.copy(isAcousticBeaconPlaying = enabled) }
    }

    fun dispatchSosSms(context: Context) {
        val loc = _trackingState.value.routePoints.lastOrNull() ?: _trackingState.value.lastKnownLocation
        val launched = sosController.launchEmergencySmsComposer(
            activityContext = context,
            lastLocation = loc
        )
        if (!launched) {
            _snackbarMessage.value = "SMS app unavailable — acoustic distress beacon & GPS coordinates remain active."
        }
    }

    fun deactivateSos(context: Context) {
        sosCountdownJob?.cancel()
        sosActiveTimerJob?.cancel()
        sosController.stopAcousticBeacon()
        _sosState.update {
            it.copy(
                isCountdownActive = false,
                isBeaconActive = false,
                isAcousticBeaconPlaying = false,
                activeBeaconSeconds = 0L
            )
        }
        if (_trackingState.value.status == TrackingStatus.RECORDING &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            attachGpsUpdates(context, intervalMs = 2500L, minDistanceMeters = 3f)
        }
        _snackbarMessage.value = "Winter Trail SOS beacon deactivated."
    }

    fun saveEmergencyContacts(contactsCsv: String) {
        sosController.saveEmergencyContacts(contactsCsv)
        val saved = sosController.getSavedEmergencyContacts()
        val count = saved.split(",", ";").map { it.trim() }.count { it.isNotEmpty() }
        _sosState.update { it.copy(emergencyContacts = saved, contactsNotifiedCount = count) }
        _snackbarMessage.value = "Saved $count emergency contact(s) for offline SOS relay."
    }

    // --- GPX Route Importer & Trail Elevation Preview ---
    fun importGpxRouteForPreview(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val xml = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: error("Could not read selected GPX file.")
                GpxTrailAnalyzer.parseGpxString(gpxContent = xml)
            }.onSuccess { summary ->
                _previewGpxTrail.value = summary
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Invalid GPX route file."
            }
        }
    }

    fun dismissGpxPreview() {
        _previewGpxTrail.value = null
    }

    fun downloadOfflinePackForGpx(summary: GpxTrailSummary) {
        val (lat, lon) = summary.centerCoordinate()
        downloadCurrentAreaOffline(
            customName = "${summary.trailName} (GPX Pack)",
            lat = lat,
            lon = lon
        )
    }

    fun confirmStartPackRunWithGpx(context: Context, hasLocationPermission: Boolean) {
        val trail = _previewGpxTrail.value ?: return
        _activePlannedTrail.value = trail
        _previewGpxTrail.value = null
        startLiveTracking(context, hasLocationPermission)
        _snackbarMessage.value = "Loaded \"${trail.trailName}\" (${String.format(java.util.Locale.US, "%.1f", trail.totalDistanceMiles)} mi) onto live topo map!"
    }

    fun clearActivePlannedTrail() {
        _activePlannedTrail.value = null
    }

    // --- Seasonal Challenge Polar Vortex Weather Alerts ---
    fun refreshPolarVortexWeather(notifyUser: Boolean = false) {
        viewModelScope.launch {
            val loc = _trackingState.value.routePoints.lastOrNull() ?: _trackingState.value.lastKnownLocation
            val lat = loc?.latitude ?: 45.3311
            val lon = loc?.longitude ?: -121.7112
            val label = if (loc != null) "Current Trailhead" else "Mt. Hood Alpine Pass"
            val updated = weatherAlertService.fetchLiveWinterConditions(
                latitude = lat,
                longitude = lon,
                locationLabel = label,
                notifyIfFreezing = notifyUser
            )
            _weatherStatus.value = updated
            if (notifyUser) {
                val tempInt = (updated.tempFahrenheit ?: 24.0).roundToInt()
                val sent = weatherAlertService.dispatchPolarVortexNotification(tempInt, label)
                _snackbarMessage.value = if (sent) {
                    "Polar Vortex Alert sent (${tempInt}°F in $label · +20% pack bonus active!)"
                } else {
                    "Weather checked: ${tempInt}°F in $label. Enable notifications to receive freeze alerts."
                }
            }
        }
    }

    // --- Glove-Friendly & Hardware Controls (Double-Chop Wrist + Earbud / Volume Keys) ---
    fun toggleGloveHardwareMode(enabled: Boolean) {
        HardwareKeyController.isGloveHardwareModeEnabled = enabled
        _gloveControlState.update {
            it.copy(
                isGloveModeEnabled = enabled,
                lastHardwareFeedback = if (enabled) "Glove & Hardware Controls ARMED" else "Glove Controls Off"
            )
        }
        if (enabled) {
            gloveGestureDetector.startListening { gesture ->
                handleGloveGesture(gesture)
            }
            _snackbarMessage.value = "Glove Mode armed: Double-chop = Scent Mark · 1x Earbud = Voice Split · 2x = Howl · Hold = SOS"
        } else {
            gloveGestureDetector.stopListening()
            HardwareKeyController.resetState()
        }
    }

    fun handleGloveGesture(gesture: GloveGestureType) {
        if (gesture == GloveGestureType.DOUBLE_CHOP) {
            audioEngine.playDeepPowderFootfall()
            _gloveControlState.update {
                it.copy(
                    scentMarkersPlaced = it.scentMarkersPlaced + 1,
                    lastHardwareFeedback = it.lastGestureFeedback()
                )
            }
            _snackbarMessage.value = "Scent Marker Placed: +1.2 Sq Miles Patrolled"
        }
    }

    private fun GloveControlUiState.lastGestureFeedback(): String =
        "Double-Chop Recognized · Scent Marker #${scentMarkersPlaced + 1}"

    private fun handleHardwareKeyAction(action: HeadphoneHardwareAction) {
        val state = _trackingState.value
        val unit = currentUnit()
        when (action) {
            HeadphoneHardwareAction.SINGLE_TAP_VOICE_CUE -> {
                val paceStr = SummitFormatters.formatPaceRaw(
                    if (state.currentPaceSecPerMile > 0L) state.currentPaceSecPerMile else state.avgPaceSecPerMile,
                    unit
                )
                val distStr = SummitFormatters.formatDistanceShort(state.distanceMiles, unit)
                val cue = "The Alpha report: $distStr covered. Current pace $paceStr ${unit.paceSuffix}. Pack zone: ${state.activePaceZone.label}."
                audioEngine.speakCoachCue(cue, force = true)
                _gloveControlState.update { it.copy(lastHardwareFeedback = "Earbud 1x Tap: Spoken Telemetry Split") }
            }
            HeadphoneHardwareAction.DOUBLE_TAP_ALPHA_HOWL -> {
                triggerAlphaSoundboardCue(SoundboardTrigger.ALPHA_HOWL, speakWithGeminiAlpha = false)
                _gloveControlState.update { it.copy(lastHardwareFeedback = "Earbud 2x Tap: Acoustic Alpha Howl") }
            }
            HeadphoneHardwareAction.LONG_PRESS_SOS -> {
                triggerManualSosCountdown("Hardware Long-Press Winter SOS Trigger")
                _gloveControlState.update { it.copy(lastHardwareFeedback = "Long-Press 3s: Winter SOS Armed") }
            }
            HeadphoneHardwareAction.VOLUME_UP_GROWL_SENSITIVITY -> {
                val nextThreshold = (_soundboardState.value.climbGrowlThresholdPct - 1f).coerceIn(4f, 15f)
                setClimbGrowlThresholdPct(nextThreshold)
                _gloveControlState.update { it.copy(lastHardwareFeedback = "Vol+: Growl Threshold ${nextThreshold.toInt()}% grade") }
            }
            HeadphoneHardwareAction.VOLUME_DOWN_GROWL_SENSITIVITY -> {
                val nextThreshold = (_soundboardState.value.climbGrowlThresholdPct + 1f).coerceIn(4f, 15f)
                setClimbGrowlThresholdPct(nextThreshold)
                _gloveControlState.update { it.copy(lastHardwareFeedback = "Vol-: Growl Threshold ${nextThreshold.toInt()}% grade") }
            }
        }
    }

    // --- Story Badge Stinger, Pack Protector Donor Badge & Winter Terrain FX ---
    fun awardPackProtectorBadge(organizationName: String, tier: DonorTier = DonorTier.GUARDIAN) {
        val badge = ConservationDonorBadge(
            tier = tier,
            dateAwardedMs = System.currentTimeMillis(),
            organizationName = organizationName,
            isEquippedAsProfileFlair = true
        )
        _donorBadgeState.value = badge
        audioEngine.playBadgeUnlockStinger()
        _snackbarMessage.value = "Pack Protector Flair Equipped! +5% Territory Morale active in Pack Chronicles."
    }

    fun playBadgeUnlockStinger() {
        audioEngine.playBadgeUnlockStinger()
    }

    fun playDeepPowderFootfall() {
        audioEngine.playDeepPowderFootfall()
    }

    fun playGlacialIceCrack() {
        audioEngine.playGlacialIceCrack()
    }

    fun deleteAccountAndData(onDeleted: () -> Unit) {
        if (_isDeletingAccount.value) return
        viewModelScope.launch {
            _isDeletingAccount.value = true
            try {
                timerJob?.cancel()
                detachGpsUpdates()
                sosController.stopAcousticBeacon()
                bleWatchManager.disconnect()

                val cloudResult = repository.deleteAccountAndCloudData()
                if (cloudResult.isSuccess) {
                    runCatching { localSql.deleteAllDataForUser(currentUserId) }
                    runCatching { corosManager.clearStoredSession(currentUserId) }
                    runCatching { externalConnectors.clearStoredSessions(currentUserId) }
                    runCatching { sosController.clearEmergencyContacts() }
                    runCatching { phoneStepCounter.clearLocalSteps() }
                    _snackbarMessage.value = "Account and all associated Striders Howl data permanently deleted."
                    onDeleted()
                } else {
                    _snackbarMessage.value = cloudResult.exceptionOrNull()?.message
                        ?: "Could not delete account right now. Please try again."
                }
            } finally {
                _isDeletingAccount.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        howlJob?.cancel()
        alphaCueJob?.cancel()
        sosCountdownJob?.cancel()
        sosActiveTimerJob?.cancel()
        detachGpsUpdates()
        fallDetector.stopListening()
        gloveGestureDetector.stopListening()
        phoneStepCounter.stopListening()
        HardwareKeyController.isGloveHardwareModeEnabled = false
        HardwareKeyController.resetState()
        sosController.release()
        voiceTranscriber.stopListening()
        bleWatchManager.release()
        audioEngine.stopAll()
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
}
