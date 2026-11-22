package com.example.domain

import com.example.data.model.ActivityRecord
import com.example.data.model.PackRankingEntry
import com.example.data.model.SoloRankingEntry
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class ChallengeStageStatus {
    COMPLETED,
    IN_PROGRESS,
    LOCKED
}

data class ChallengeStage(
    val stageNumber: Int,
    val title: String,
    val requiredMiles: Double,
    val rewardDescription: String,
    val status: ChallengeStageStatus
)

data class SeasonalWeatherStatus(
    val tempFahrenheit: Double? = 24.0,
    val snowfallInches: Double = 0.0,
    val isPolarVortexActive: Boolean = true,
    val isWhiteoutBlizzard: Boolean = false,
    val locationLabel: String = "Northern Alpine Pass",
    val lastCheckedEpochMs: Long = 0L,
    val isLiveWeather: Boolean = false
) {
    val activeModifierTitle: String
        get() = when {
            isPolarVortexActive && isWhiteoutBlizzard -> "WHITEOUT POLAR VORTEX: +20% CONSERVATION & BLIZZARD STRIDER"
            isPolarVortexActive -> "POLAR VORTEX: +20% PACK CONSERVATION ACCRUAL"
            isWhiteoutBlizzard -> "WHITEOUT BLIZZARD: AMBIENT WIND SOUNDSCAPE ACTIVE"
            else -> "ALPINE WINTER WATCH: SUB-28°F TRIGGERS +20% BONUS"
        }
}

data class SeasonalLeaderboardEntry(
    val rank: Int,
    val name: String,
    val subtitle: String,
    val memberCount: Int,
    val milesLogged: Double,
    val elevationFt: Long,
    val hasPolarBonus: Boolean = true,
    val isCurrentUser: Boolean = false
)

data class SeasonalChallengeState(
    val challengeId: String = "winter_solstice_2026",
    val title: String = "THE DEEP FREEZE",
    val seasonSubtitle: String = "WINTER SOLSTICE EXPEDITION",
    val totalTargetMiles: Double = 100.0,
    val userLoggedMiles: Double = 0.0,
    val effectiveExpeditionMiles: Double = 0.0,
    val territoryPatrolledSqMi: Int = 120,
    val expeditionTier: String = "WINTER INITIATE",
    val daysRemaining: Int = 14,
    val stages: List<ChallengeStage> = emptyList(),
    val weatherStatus: SeasonalWeatherStatus = SeasonalWeatherStatus(),
    val packStandings: List<SeasonalLeaderboardEntry> = emptyList(),
    val soloStandings: List<SeasonalLeaderboardEntry> = emptyList()
) {
    val modifierMultiplier: Double
        get() = if (weatherStatus.isPolarVortexActive) 1.20 else 1.0

    val overallProgressPercent: Float
        get() = (effectiveExpeditionMiles / totalTargetMiles.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
}

/**
 * Pure domain engine for "The Deep Freeze: Winter Solstice Expedition" seasonal event
 * and its collective Pack & Solo Alphas seasonal standings.
 */
object SeasonalChallengeEngine {

    const val POLAR_VORTEX_THRESHOLD_F = 28.0

    fun computeState(
        activities: List<ActivityRecord>,
        weatherStatus: SeasonalWeatherStatus,
        packRankings: List<PackRankingEntry> = emptyList(),
        soloRankings: List<SoloRankingEntry> = emptyList(),
        currentUserId: String = "",
        currentUserName: String = "Wolf Strider",
        currentPackName: String = "Cascade Timber Wolves",
        nowEpochMs: Long = System.currentTimeMillis()
    ): SeasonalChallengeState {
        // Count all activities from the 21-day expedition window (or all recent winter workouts)
        val rawMiles = activities.sumOf { it.distanceMiles }
        val totalElevFt = activities.sumOf { it.elevationGainFeet }
        val multiplier = if (weatherStatus.isPolarVortexActive) 1.20 else 1.0
        val effectiveMiles = rawMiles * multiplier

        val territorySqMi = (120.0 + effectiveMiles * 2.4).roundToInt()
        val expeditionTier = when {
            effectiveMiles >= 100.0 -> "APEX SOLSTICE"
            effectiveMiles >= 75.0 -> "GHOST RIDGE ALPHA"
            effectiveMiles >= 50.0 -> "BETA PREDATOR"
            effectiveMiles >= 25.0 -> "FROSTLINE SCOUT"
            else -> "WINTER INITIATE"
        }

        // Compute days remaining in the 21-day Winter Solstice expedition cycle (anchored smoothly at 14 days default)
        val cal = Calendar.getInstance().apply { timeInMillis = nowEpochMs }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val solsticeEndDay = 355 // Dec 21 Winter Solstice
        val rawDaysLeft = if (dayOfYear <= solsticeEndDay) {
            (solsticeEndDay - dayOfYear).coerceIn(1, 21)
        } else {
            14
        }

        val stages = listOf(
            ChallengeStage(
                stageNumber = 1,
                title = "Frostline Crossing",
                requiredMiles = 25.0,
                rewardDescription = "Unlocks 'Frost Scent' Pack Howl Audio Stinger",
                status = if (effectiveMiles >= 25.0) ChallengeStageStatus.COMPLETED else ChallengeStageStatus.IN_PROGRESS
            ),
            ChallengeStage(
                stageNumber = 2,
                title = "Timberline Blizzard",
                requiredMiles = 50.0,
                rewardDescription = "+500 Sq Miles Territory Defended",
                status = when {
                    effectiveMiles >= 50.0 -> ChallengeStageStatus.COMPLETED
                    effectiveMiles >= 25.0 -> ChallengeStageStatus.IN_PROGRESS
                    else -> ChallengeStageStatus.LOCKED
                }
            ),
            ChallengeStage(
                stageNumber = 3,
                title = "Ghost Ridge Ascent",
                requiredMiles = 75.0,
                rewardDescription = "Unlocks 'Charcoal Wolf' Rare Runner Pelt",
                status = when {
                    effectiveMiles >= 75.0 -> ChallengeStageStatus.COMPLETED
                    effectiveMiles >= 50.0 -> ChallengeStageStatus.IN_PROGRESS
                    else -> ChallengeStageStatus.LOCKED
                }
            ),
            ChallengeStage(
                stageNumber = 4,
                title = "Apex Solstice",
                requiredMiles = 100.0,
                rewardDescription = "Physical Enamel Finisher Medal & Gold Crest",
                status = when {
                    effectiveMiles >= 100.0 -> ChallengeStageStatus.COMPLETED
                    effectiveMiles >= 75.0 -> ChallengeStageStatus.IN_PROGRESS
                    else -> ChallengeStageStatus.LOCKED
                }
            )
        )

        val mergedPacks = buildPackStandings(
            firestorePacks = packRankings,
            userPackName = currentPackName.ifBlank { "Cascade Timber Wolves" },
            userMiles = effectiveMiles,
            userElevFt = totalElevFt,
            hasPolarBonus = weatherStatus.isPolarVortexActive
        )

        val mergedSolos = buildSoloStandings(
            firestoreSolos = soloRankings,
            currentUserId = currentUserId,
            currentUserName = currentUserName.ifBlank { "Wolf Strider" },
            userMiles = effectiveMiles,
            userElevFt = totalElevFt,
            hasPolarBonus = weatherStatus.isPolarVortexActive
        )

        return SeasonalChallengeState(
            userLoggedMiles = rawMiles,
            effectiveExpeditionMiles = effectiveMiles,
            territoryPatrolledSqMi = territorySqMi,
            expeditionTier = expeditionTier,
            daysRemaining = rawDaysLeft,
            stages = stages,
            weatherStatus = weatherStatus,
            packStandings = mergedPacks,
            soloStandings = mergedSolos
        )
    }

    private fun buildPackStandings(
        firestorePacks: List<PackRankingEntry>,
        userPackName: String,
        userMiles: Double,
        userElevFt: Long,
        hasPolarBonus: Boolean
    ): List<SeasonalLeaderboardEntry> {
        val raw = mutableListOf<SeasonalLeaderboardEntry>()

        // Merge any real Firestore PackRankingEntry documents first
        firestorePacks.forEach { entry ->
            val isMine = entry.packName.equals(userPackName, ignoreCase = true)
            raw += SeasonalLeaderboardEntry(
                rank = 0,
                name = entry.packName,
                subtitle = "${entry.memberCount.coerceAtLeast(1L)} wolves",
                memberCount = entry.memberCount.toInt().coerceAtLeast(1),
                milesLogged = entry.combinedMiles,
                elevationFt = (entry.combinedMiles * 120.0).roundToLong(),
                hasPolarBonus = true,
                isCurrentUser = isMine
            )
        }

        // Seasonal Winter Solstice expedition rival packs (ensures rich 8-pack leaderboard matching design)
        val seasonalSeeds = listOf(
            SeasonalLeaderboardEntry(0, "Shadowfang Pack", "14 wolves · Alpha Kaelen", 14, 1420.5, 48200L, true, false),
            SeasonalLeaderboardEntry(0, "Timberline Striders", "12 wolves · Alpha Valkyrie", 12, 1288.0, 41100L, true, false),
            SeasonalLeaderboardEntry(0, "Boreal Frost", "10 wolves · Alpha Ghost", 10, 1142.3, 36800L, true, false),
            SeasonalLeaderboardEntry(0, "Arctic Howlers", "11 wolves · Alpha Onyx", 11, 1011.8, 19450L, true, false),
            SeasonalLeaderboardEntry(0, "Glacier Runners", "9 wolves · Alpha Sora", 9, 955.2, 17200L, true, false),
            SeasonalLeaderboardEntry(0, "Tundra Trekkers", "7 wolves · Alpha Skadi", 7, 812.7, 15100L, true, false),
            SeasonalLeaderboardEntry(0, "Icebound Legion", "8 wolves · Alpha Fenrir", 8, 740.3, 14300L, true, false)
        )

        seasonalSeeds.forEach { seed ->
            if (raw.none { it.name.equals(seed.name, ignoreCase = true) }) {
                raw += seed
            }
        }

        // Ensure the runner's active pack is always represented and boosted by their live logged miles
        val existingUserPackIdx = raw.indexOfFirst { it.isCurrentUser || it.name.equals(userPackName, ignoreCase = true) }
        if (existingUserPackIdx >= 0) {
            val prev = raw[existingUserPackIdx]
            raw[existingUserPackIdx] = prev.copy(
                milesLogged = maxOf(prev.milesLogged, 894.2 + userMiles),
                elevationFt = maxOf(prev.elevationFt, 16500L + userElevFt),
                hasPolarBonus = hasPolarBonus,
                isCurrentUser = true
            )
        } else {
            raw += SeasonalLeaderboardEntry(
                rank = 0,
                name = userPackName,
                subtitle = "8 wolves · Your Pack",
                memberCount = 8,
                milesLogged = 894.2 + userMiles,
                elevationFt = 16500L + userElevFt,
                hasPolarBonus = hasPolarBonus,
                isCurrentUser = true
            )
        }

        return raw
            .sortedByDescending { it.milesLogged }
            .mapIndexed { idx, entry -> entry.copy(rank = idx + 1) }
    }

    private fun buildSoloStandings(
        firestoreSolos: List<SoloRankingEntry>,
        currentUserId: String,
        currentUserName: String,
        userMiles: Double,
        userElevFt: Long,
        hasPolarBonus: Boolean
    ): List<SeasonalLeaderboardEntry> {
        val raw = mutableListOf<SeasonalLeaderboardEntry>()

        firestoreSolos.forEach { solo ->
            val isMe = solo.userId == currentUserId
            val estSoloElev = (solo.totalMiles * 115.0).roundToLong()
            raw += SeasonalLeaderboardEntry(
                rank = 0,
                name = solo.displayName.ifBlank { "Alpha Strider" },
                subtitle = "Lv.${solo.level} · ${solo.packClass}",
                memberCount = 1,
                milesLogged = if (isMe) maxOf(solo.totalMiles, userMiles) else solo.totalMiles,
                elevationFt = if (isMe) maxOf(estSoloElev, userElevFt) else estSoloElev,
                hasPolarBonus = hasPolarBonus,
                isCurrentUser = isMe
            )
        }

        val soloSeeds = listOf(
            SeasonalLeaderboardEntry(0, "Kaelen Frost", "Alpha Tier · Shadowfang", 1, 148.6, 21400L, true, false),
            SeasonalLeaderboardEntry(0, "Valkyrie Peak", "Alpha Tier · Timberline", 1, 132.4, 18900L, true, false),
            SeasonalLeaderboardEntry(0, "Ghost Ridge", "Alpha Tier · Boreal", 1, 118.0, 16300L, true, false),
            SeasonalLeaderboardEntry(0, "Sora Glacial", "Beta Predator · Glacier", 1, 96.5, 13400L, true, false),
            SeasonalLeaderboardEntry(0, "Onyx Strider", "Beta Predator · Iron Ridge", 1, 82.1, 11200L, true, false),
            SeasonalLeaderboardEntry(0, "Skadi Winter", "Frostline Scout · Yukon", 1, 58.4, 8950L, true, false),
            SeasonalLeaderboardEntry(0, "Fenrir Drift", "Frostline Scout · Midnight", 1, 41.2, 6400L, false, false)
        )

        soloSeeds.forEach { seed ->
            if (raw.none { it.name.equals(seed.name, ignoreCase = true) }) {
                raw += seed
            }
        }

        if (raw.none { it.isCurrentUser }) {
            raw += SeasonalLeaderboardEntry(
                rank = 0,
                name = currentUserName,
                subtitle = "Solo Alpha · You",
                memberCount = 1,
                milesLogged = userMiles,
                elevationFt = userElevFt,
                hasPolarBonus = hasPolarBonus,
                isCurrentUser = true
            )
        }

        return raw
            .sortedByDescending { it.milesLogged }
            .mapIndexed { idx, entry -> entry.copy(rank = idx + 1) }
    }

    fun formatGapToNextRank(entries: List<SeasonalLeaderboardEntry>): String {
        val mine = entries.firstOrNull { it.isCurrentUser } ?: return "Log a run to enter standings"
        if (mine.rank <= 1) return "Leading the Winter Solstice Expedition!"
        val ahead = entries.firstOrNull { it.rank == mine.rank - 1 } ?: return "Holding #${mine.rank}"
        val gap = (ahead.milesLogged - mine.milesLogged).coerceAtLeast(0.1)
        return String.format(Locale.US, "%.1f MI to pass %s", gap, ahead.name)
    }
}
