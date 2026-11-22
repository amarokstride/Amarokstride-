package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class ActivityType(val displayName: String, val questLabel: String) {
    RUN("Run", "Swiftstride Run"),
    HIKE("Hike", "Mountain Expedition"),
    EQUESTRIAN("Equestrian", "Cavalry Endurance");

    companion object {
        fun fromRaw(raw: String?): ActivityType =
            entries.firstOrNull { it.name == raw } ?: RUN
    }
}

enum class ActivitySource(val displayName: String) {
    GPS("GPS"),
    MANUAL("Tavern Log"),
    HEALTH_CONNECT("Health Connect"),
    COROS("COROS"),
    GARMIN("Garmin"),
    STRAVA("Strava"),
    PATHBOUND("Pathbound");

    companion object {
        fun fromRaw(raw: String?): ActivitySource =
            entries.firstOrNull { it.name == raw } ?: GPS
    }
}

enum class AccountRole(val label: String, val subtitle: String) {
    PRIMARY("My Adventurer", "Primary tavern profile & quest log"),
    PARTNER("Partner Adventurer", "Co-op partner profile linked via party code");

    companion object {
        fun fromRaw(raw: String?): AccountRole =
            entries.firstOrNull { it.name == raw } ?: PRIMARY
    }
}

enum class SeasonTheme(val label: String, val effectSummary: String) {
    AUTO("Auto Season", "Matches current calendar season"),
    WINTER("Winter Snow", "Gentle falling snowflakes"),
    SPRING("Spring Blossoms", "Drifting mountain flowers"),
    SUMMER("Summer Wind", "Warm golden alpine breeze"),
    FALL("Autumn Leaves", "Falling ember & oak leaves");

    fun resolveEffectiveSeason(): SeasonTheme {
        if (this != AUTO) return this
        val month = Calendar.getInstance().get(Calendar.MONTH)
        return when (month) {
            Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> WINTER
            Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> SPRING
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> SUMMER
            else -> FALL
        }
    }

    companion object {
        fun fromRaw(raw: String?): SeasonTheme =
            entries.firstOrNull { it.name == raw } ?: AUTO
    }
}

enum class WolfGait(val jsKey: String, val label: String) {
    IDLE("idle", "Resting by the Hearth"),
    WALK("walk", "Walking Pace"),
    TROT("trot", "Trotting Pace"),
    RUN("run", "Full Sprint"),
    HOWL("howl", "Victory Howl!")
}

enum class EquestrianGait(val label: String, val cueText: String) {
    WALK("Walk", "10-minute steady walk — keep rhythm relaxed and warm up."),
    TROT("Trot", "10-minute working trot — build aerobic endurance steadily."),
    CANTER("Canter", "10-minute balanced canter — maintain smooth cadence and posture.")
}

data class EquestrianIntervalState(
    val stageNumber: Int = 1,
    val stageTitle: String = "Stage 1 · Foundation (10m Walk / 10m Trot / 10m Canter)",
    val currentGait: EquestrianGait = EquestrianGait.WALK,
    val cycleNumber: Int = 1,
    val segmentDurationSeconds: Long = 600L,
    val secondsRemainingInSegment: Long = 600L
) {
    companion object {
        /**
         * Progressive horse endurance conditioning schedule mirroring marathon build-up:
         * Starts with 10 min walk, 10 min trot, 10 min canter repeating, then scales
         * working trot/canter endurance blocks as lifetime equestrian rides increase.
         */
        fun computeForElapsed(elapsedSeconds: Long, completedRidesCount: Int): EquestrianIntervalState {
            val stage = when {
                completedRidesCount >= 24 -> 6
                completedRidesCount >= 18 -> 5
                completedRidesCount >= 12 -> 4
                completedRidesCount >= 7 -> 3
                completedRidesCount >= 3 -> 2
                else -> 1
            }
            val (walkSec, trotSec, canterSec, title) = when (stage) {
                1 -> Quad(600L, 600L, 600L, "Stage 1 · Foundation (10m Walk / 10m Trot / 10m Canter)")
                2 -> Quad(600L, 720L, 600L, "Stage 2 · Base Build (10m Walk / 12m Trot / 10m Canter)")
                3 -> Quad(600L, 900L, 720L, "Stage 3 · Trail Tempo (10m Walk / 15m Trot / 12m Canter)")
                4 -> Quad(600L, 1080L, 900L, "Stage 4 · Half-Endurance (10m Walk / 18m Trot / 15m Canter)")
                5 -> Quad(600L, 1200L, 1080L, "Stage 5 · Long Ride Prep (10m Walk / 20m Trot / 18m Canter)")
                else -> Quad(600L, 1500L, 1200L, "Stage 6 · Marathon Steed (10m Walk / 25m Trot / 20m Canter)")
            }
            val cycleTotal = walkSec + trotSec + canterSec
            val cycleNum = (elapsedSeconds / cycleTotal).toInt() + 1
            val inCycle = elapsedSeconds % cycleTotal
            return when {
                inCycle < walkSec -> EquestrianIntervalState(
                    stageNumber = stage,
                    stageTitle = title,
                    currentGait = EquestrianGait.WALK,
                    cycleNumber = cycleNum,
                    segmentDurationSeconds = walkSec,
                    secondsRemainingInSegment = walkSec - inCycle
                )
                inCycle < walkSec + trotSec -> EquestrianIntervalState(
                    stageNumber = stage,
                    stageTitle = title,
                    currentGait = EquestrianGait.TROT,
                    cycleNumber = cycleNum,
                    segmentDurationSeconds = trotSec,
                    secondsRemainingInSegment = (walkSec + trotSec) - inCycle
                )
                else -> EquestrianIntervalState(
                    stageNumber = stage,
                    stageTitle = title,
                    currentGait = EquestrianGait.CANTER,
                    cycleNumber = cycleNum,
                    segmentDurationSeconds = canterSec,
                    secondsRemainingInSegment = cycleTotal - inCycle
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

enum class PlayMode(val label: String, val description: String) {
    SOLO("Solo Strider", "Run, hike, or ride solo with your wolf companion"),
    PARTY("Wolf Pack Party", "Link with your partner to share miles, cheers & quests");

    companion object {
        fun fromRaw(raw: String?): PlayMode =
            entries.firstOrNull { it.name == raw } ?: SOLO
    }
}

data class AdventurerRank(
    val level: Int,
    val packClass: String,
    val xpTotal: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int
) {
    val progressFraction: Float
        get() = if (xpForNextLevel <= 0) 1f else (xpIntoLevel.toFloat() / xpForNextLevel.toFloat()).coerceIn(0f, 1f)

    companion object {
        fun compute(activities: List<ActivityRecord>): AdventurerRank {
            val totalMiles = activities.sumOf { it.distanceMiles }
            val totalElev = activities.sumOf { it.elevationGainFeet }
            val xp = (totalMiles * 45.0).roundToInt() + (totalElev / 25L).toInt() + activities.size * 25
            val xpPerLevel = 350
            val level = (xp / xpPerLevel) + 1
            val inLevel = xp % xpPerLevel

            val runCount = activities.count { it.typeEnum() == ActivityType.RUN }
            val hikeCount = activities.count { it.typeEnum() == ActivityType.HIKE }
            val rideCount = activities.count { it.typeEnum() == ActivityType.EQUESTRIAN }

            val packClass = when {
                level >= 20 -> "Alpha Wolfpack Master"
                rideCount > runCount && rideCount >= hikeCount && rideCount > 0 -> "Cavalry Outrider"
                hikeCount > runCount && hikeCount > 0 -> "Ironhearth Pack Ranger"
                runCount > 0 && hikeCount > 0 && rideCount > 0 -> "All-Realm Pack Pathfinder"
                level >= 8 -> "Silverwolf Strider"
                else -> "Hearthstone Pack Scout"
            }
            return AdventurerRank(
                level = level,
                packClass = packClass,
                xpTotal = xp,
                xpIntoLevel = inLevel,
                xpForNextLevel = xpPerLevel
            )
        }
    }
}

enum class DistanceUnit(val shortLabel: String, val paceSuffix: String, val elevationLabel: String) {
    MILES("mi", "/mi", "ft"),
    KILOMETERS("km", "/km", "m");

    companion object {
        fun fromRaw(raw: String?): DistanceUnit =
            entries.firstOrNull { it.name == raw } ?: MILES
    }
}

enum class DifficultyTier(val label: String, val subtitle: String) {
    TRAILHEAD("Tavern Novice", "Building joyful momentum"),
    RIDGE_RUNNER("Ridge Ranger", "Steady weekly progression"),
    ALPINE_CLIMBER("Ironpeak Warden", "Strong aerobic endurance"),
    SUMMIT_CRUSHER("Dragonspine Champion", "Peak trail consistency");

    companion object {
        fun fromRaw(raw: String?): DifficultyTier =
            entries.firstOrNull { it.name == raw } ?: TRAILHEAD
    }
}

enum class ChallengeStatus {
    ACTIVE,
    COMPLETED,
    PAST;

    companion object {
        fun fromRaw(raw: String?): ChallengeStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class WolfCoat(
    val displayName: String,
    val bodyHex: Long,
    val darkHex: Long,
    val frostHex: Long,
    val eyeHex: Long
) {
    TIMBER_GREY("Timber Grey", 0xFF5C6872, 0xFF2B343D, 0xFFD9E6EE, 0xFF7CE4F8),
    ARCTIC_WHITE("Arctic White", 0xFFEAF3F8, 0xFF8CA5B5, 0xFFFFFFFF, 0xFF4CC9F0),
    OBSIDIAN_BLACK("Obsidian Shadow", 0xFF232B33, 0xFF10151A, 0xFF6B8191, 0xFF5CF2FF),
    RED_TIMBER("Red Timber", 0xFF9E5132, 0xFF4E2313, 0xFFEBD2BD, 0xFFF4B942),
    SILVER_FROST("Silver Frost", 0xFF9FB2BF, 0xFF4A5C6B, 0xFFE8F6FF, 0xFF70D6FF);

    companion object {
        fun fromRaw(raw: String?): WolfCoat =
            entries.firstOrNull { it.name == raw } ?: TIMBER_GREY
    }
}

data class WolfStoryChapter(
    val chapterNumber: Int,
    val title: String,
    val narrative: String,
    val minMiles: Double,
    val nextMilestoneMiles: Double
) {
    companion object {
        fun resolveForMiles(totalMiles: Double): WolfStoryChapter = when {
            totalMiles >= 250.0 -> WolfStoryChapter(
                6,
                "Chapter VI · Legend of the Aurora Summit",
                "Your pawprints glow across every ridge from frost valley to high pass. Young striders gather by the hearth to hear tales of your endless stride.",
                250.0,
                500.0
            )
            totalMiles >= 100.0 -> WolfStoryChapter(
                5,
                "Chapter V · Moonlit Run of the Elder Wolves",
                "Having crossed the hundred-mile frontier, the silver moon lights your trail. Your howl rallies packs across the northern ranges.",
                100.0,
                250.0
            )
            totalMiles >= 50.0 -> WolfStoryChapter(
                4,
                "Chapter IV · The Ironcairn Pack Trial",
                "Half a century of trail miles has tempered your stride. Steep switchbacks and winter winds now fuel your endurance instead of slowing you down.",
                50.0,
                100.0
            )
            totalMiles >= 25.0 -> WolfStoryChapter(
                3,
                "Chapter III · Howl of the Silver Pass",
                "Beyond the treeline, you and your wolf companion find your rhythm—trotting the flats, surging the climbs, and claiming weekly quests.",
                25.0,
                50.0
            )
            totalMiles >= 10.0 -> WolfStoryChapter(
                2,
                "Chapter II · Tracks Across Whispering Ridge",
                "Ten miles behind you and the scent of pine ahead. The tavern keeper pins your name onto the Striders Howl board as a rising Pathfinder.",
                10.0,
                25.0
            )
            else -> WolfStoryChapter(
                1,
                "Chapter I · Awakening at Frostpine Hearth",
                "The carved wolf doors creak open. Lace your boots or cinch your saddle—your first ten miles awaken the pack spirit within.",
                0.0,
                10.0
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Trail Explorer",
    val preferredUnit: String = DistanceUnit.MILES.name,
    val weeklyBaselineMiles: Double = 10.0,
    val corosConnected: Boolean = false,
    val garminConnected: Boolean = false,
    val stravaConnected: Boolean = false,
    val pathboundConnected: Boolean = false,
    val healthConnectAutoExport: Boolean = false,
    val currentStreakWeeks: Long = 0L,
    val accountRole: String = AccountRole.PRIMARY.name,
    val partnerPartyCode: String? = null,
    val seasonTheme: String = SeasonTheme.AUTO.name,
    val wolfCoat: String = WolfCoat.TIMBER_GREY.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun unitEnum(): DistanceUnit = DistanceUnit.fromRaw(preferredUnit)
    fun roleEnum(): AccountRole = AccountRole.fromRaw(accountRole)
    fun seasonEnum(): SeasonTheme = SeasonTheme.fromRaw(seasonTheme)
    fun wolfCoatEnum(): WolfCoat = WolfCoat.fromRaw(wolfCoat)
}

@Keep
@IgnoreExtraProperties
data class TavernChatMessage(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderName: String = "Wolf Strider",
    val senderWolfCoat: String = WolfCoat.TIMBER_GREY.name,
    val senderPackClass: String = "Hearthstone Pack Scout",
    val text: String = "",
    val sentAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun coatEnum(): WolfCoat = WolfCoat.fromRaw(senderWolfCoat)
}

@Keep
@IgnoreExtraProperties
data class SoloRankingEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Wolf Strider",
    val wolfCoat: String = WolfCoat.TIMBER_GREY.name,
    val packClass: String = "Hearthstone Pack Scout",
    val level: Long = 1L,
    val totalMiles: Double = 0.0,
    val weeklyMiles: Double = 0.0,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun coatEnum(): WolfCoat = WolfCoat.fromRaw(wolfCoat)
}

@Keep
@IgnoreExtraProperties
data class PackRankingEntry(
    @DocumentId val id: String = "",
    val partyCode: String = "",
    val packName: String = "Wolf Pack",
    val memberCount: Long = 1L,
    val combinedMiles: Double = 0.0,
    val leadWolfCoat: String = WolfCoat.TIMBER_GREY.name,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun coatEnum(): WolfCoat = WolfCoat.fromRaw(leadWolfCoat)
}

@Keep
@IgnoreExtraProperties
data class ActivityRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val activityType: String = ActivityType.RUN.name,
    val source: String = ActivitySource.GPS.name,
    val distanceMiles: Double = 0.0,
    val durationSeconds: Long = 0L,
    val avgPaceSecPerMile: Long = 600L,
    val elevationGainFeet: Long = 0L,
    val startedAtEpochMs: Long = 0L,
    val externalId: String? = null,
    val gearId: String? = null,
    val notes: String? = null,
    val voiceTranscript: String? = null,
    val aiCoachingSummary: String? = null,
    val routePoints: List<String> = emptyList(),
    val splitPacesSec: List<Long> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun typeEnum(): ActivityType = ActivityType.fromRaw(activityType)
    fun sourceEnum(): ActivitySource = ActivitySource.fromRaw(source)
    fun decodedRoute(): List<GeoPointEle> = routePoints.mapNotNull { GeoPointEle.decode(it) }
}

@Keep
@IgnoreExtraProperties
data class WeeklyChallenge(
    @DocumentId val id: String = "",
    val userId: String = "",
    val weekKey: String = "",
    val targetMiles: Double = 10.0,
    val loggedMiles: Double = 0.0,
    val difficultyTier: String = DifficultyTier.TRAILHEAD.name,
    val progressionReason: String = "",
    val challengeStatus: String = ChallengeStatus.ACTIVE.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun tierEnum(): DifficultyTier = DifficultyTier.fromRaw(difficultyTier)
    fun statusEnum(): ChallengeStatus = ChallengeStatus.fromRaw(challengeStatus)
    val progressFraction: Float
        get() = if (targetMiles <= 0.0) 0f else (loggedMiles / targetMiles).toFloat().coerceIn(0f, 1f)
}

@Keep
@IgnoreExtraProperties
data class MilestoneRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val badgeKey: String = "",
    val title: String = "",
    val subtitle: String = "",
    val metricHighlight: String = "",
    val unlockedAtEpochMs: Long = 0L,
    val sharedCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class PackCrest(
    val displayName: String,
    val badgeKey: String,
    val accentHex: Long
) {
    ALPHA_HOWL("Alpha Howl", "wolf_alpha_howl", 0xFF00F2FE),
    ARCTIC_SENTINEL("Arctic Sentinel", "wolf_arctic_sentinel", 0xFF4FACFE),
    SHADOW_STRIDER("Shadow Strider", "wolf_shadow_strider", 0xFF93D5FF),
    FROST_MOON("Frost Moon", "wolf_frost_moon", 0xFF66F4D2),
    TIMBER_CROWN("Timber Crown", "wolf_timber_crown", 0xFFFFB703);

    companion object {
        fun fromRaw(raw: String?): PackCrest =
            entries.firstOrNull { it.name == raw } ?: ALPHA_HOWL
    }
}

@Keep
@IgnoreExtraProperties
data class TavernParty(
    @DocumentId val id: String = "",
    val partyCode: String = "",
    val creatorUid: String = "",
    val memberUids: List<String> = emptyList(),
    val packName: String = "",
    val packCrest: String = PackCrest.ALPHA_HOWL.name,
    val weeklyGoalMiles: Double = 250.0,
    val isPublic: Boolean = false,
    val primaryName: String = "Primary Adventurer",
    val primaryTotalMiles: Double = 0.0,
    val primaryLastQuest: String = "Ready at the Hearth",
    val partnerUid: String? = null,
    val partnerName: String? = null,
    val partnerTotalMiles: Double = 0.0,
    val partnerLastQuest: String? = null,
    val cheerCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val combinedMiles: Double get() = primaryTotalMiles + partnerTotalMiles
    val isPartnerJoined: Boolean get() = memberUids.size >= 2 && !partnerUid.isNullOrBlank()
    val resolvedPackName: String
        get() = packName.ifBlank {
            if (!partnerName.isNullOrBlank()) "$primaryName & $partnerName's Pack" else "$primaryName's Pack"
        }
    fun crestEnum(): PackCrest = PackCrest.fromRaw(packCrest)
}

data class GeoPointEle(
    val latitude: Double,
    val longitude: Double,
    val elevationFeet: Double = 0.0
) {
    fun encode(): String = String.format(
        Locale.US,
        "%.5f,%.5f,%.0f",
        latitude,
        longitude,
        elevationFeet
    )

    companion object {
        fun decode(raw: String): GeoPointEle? {
            val parts = raw.split(",")
            if (parts.size < 2) return null
            val lat = parts[0].toDoubleOrNull() ?: return null
            val lon = parts[1].toDoubleOrNull() ?: return null
            val ele = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            return GeoPointEle(lat, lon, ele)
        }
    }
}

data class CoachingInsight(
    val headline: String,
    val encouragement: String,
    val improvementFocus: String,
    val nextSessionDrill: String,
    val targetPaceSuggestion: String
)

data class TrainingDaySuggestion(
    val dayLabel: String,
    val workoutTitle: String,
    val details: String,
    val isRestDay: Boolean
)

data class AdaptiveTrainingPlan(
    val id: Long = 0L,
    val goalTitle: String,
    val discipline: ActivityType,
    val summary: String,
    val weeklySchedule: List<TrainingDaySuggestion>,
    val restDayGuidance: String,
    val motivationalTip: String,
    val watchSyncNote: String,
    val updatedAtMs: Long = System.currentTimeMillis()
)

data class RouteAnalysisReport(
    val activityId: String,
    val elevationGainFeet: Long,
    val elevationLossFeet: Long,
    val terrainType: String,
    val weatherSummary: String,
    val inclineSlowdownPct: Int,
    val bestTimeAdvice: String,
    val insights: List<String>,
    val analyzedAtMs: Long = System.currentTimeMillis()
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface CoachUiState {
    data object Idle : CoachUiState
    data object Loading : CoachUiState
    data class Ready(val insight: CoachingInsight) : CoachUiState
    data class Error(val message: String) : CoachUiState
}

object SummitFormatters {
    private const val KM_PER_MILE = 1.60934
    private const val METERS_PER_FOOT = 0.3048

    fun formatDistance(miles: Double, unit: DistanceUnit): String {
        val value = if (unit == DistanceUnit.KILOMETERS) miles * KM_PER_MILE else miles
        return String.format(Locale.US, "%.2f %s", value, unit.shortLabel)
    }

    fun formatDistanceShort(miles: Double, unit: DistanceUnit): String {
        val value = if (unit == DistanceUnit.KILOMETERS) miles * KM_PER_MILE else miles
        return String.format(Locale.US, "%.1f %s", value, unit.shortLabel)
    }

    fun formatPace(secPerMile: Long, unit: DistanceUnit): String {
        val safeSec = secPerMile.coerceIn(120L, 7200L)
        val unitSec = if (unit == DistanceUnit.KILOMETERS) {
            (safeSec / KM_PER_MILE).roundToInt()
        } else {
            safeSec.toInt()
        }
        val minutes = unitSec / 60
        val seconds = unitSec % 60
        return String.format(Locale.US, "%d:%02d %s", minutes, seconds, unit.paceSuffix)
    }

    fun formatPaceRaw(secPerMile: Long, unit: DistanceUnit): String {
        val safeSec = secPerMile.coerceIn(120L, 7200L)
        val unitSec = if (unit == DistanceUnit.KILOMETERS) {
            (safeSec / KM_PER_MILE).roundToInt()
        } else {
            safeSec.toInt()
        }
        return String.format(Locale.US, "%d:%02d", unitSec / 60, unitSec % 60)
    }

    fun formatDuration(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0L)
        val hrs = safe / 3600
        val mins = (safe % 3600) / 60
        val secs = safe % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatElevation(feet: Long, unit: DistanceUnit): String {
        return if (unit == DistanceUnit.KILOMETERS) {
            val meters = (feet * METERS_PER_FOOT).roundToInt()
            "+$meters m"
        } else {
            "+$feet ft"
        }
    }

    fun formatDate(epochMs: Long): String {
        if (epochMs <= 0L) return "Just now"
        val formatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        return formatter.format(Date(epochMs))
    }

    fun resolveWolfGait(
        isRecording: Boolean,
        activityType: ActivityType,
        paceSecPerMile: Long,
        equestrianGait: EquestrianGait?
    ): WolfGait {
        if (!isRecording) return WolfGait.IDLE
        if (activityType == ActivityType.EQUESTRIAN && equestrianGait != null) {
            return when (equestrianGait) {
                EquestrianGait.WALK -> WolfGait.WALK
                EquestrianGait.TROT -> WolfGait.TROT
                EquestrianGait.CANTER -> WolfGait.RUN
            }
        }
        if (paceSecPerMile <= 0L) return WolfGait.WALK
        return when {
            paceSecPerMile <= 570L -> WolfGait.RUN     // faster than 9:30/mi -> running
            paceSecPerMile <= 840L -> WolfGait.TROT    // 9:30 - 14:00/mi -> jogging/trotting
            else -> WolfGait.WALK                       // slower than 14:00/mi -> walking
        }
    }

    fun downsampleRoute(points: List<GeoPointEle>, maxCount: Int = 220): List<String> {
        if (points.isEmpty()) return emptyList()
        if (points.size <= maxCount) return points.map { it.encode() }
        val step = (points.size - 1).toDouble() / (maxCount - 1).toDouble()
        return (0 until maxCount).map { idx ->
            val sourceIndex = (idx * step).roundToInt().coerceIn(0, points.lastIndex)
            points[sourceIndex].encode()
        }
    }
}
