package com.example.domain

import com.example.data.model.ActivityRecord
import com.example.data.model.ActivityType
import com.example.data.model.ChallengeStatus
import com.example.data.model.DifficultyTier
import com.example.data.model.WeeklyChallenge
import java.util.Calendar
import java.util.Locale
import kotlin.math.round

data class WeeklyChallengePlan(
    val weekKey: String,
    val targetMiles: Double,
    val loggedMiles: Double,
    val difficultyTier: DifficultyTier,
    val progressionReason: String,
    val challengeStatus: ChallengeStatus,
    val dailyMilesMonToSun: List<Double>,
    val computedStreakWeeks: Long
)

data class MilestoneCandidate(
    val badgeKey: String,
    val title: String,
    val subtitle: String,
    val metricHighlight: String
)

data class YearlyTravelSummary(
    val year: Int,
    val totalMiles: Double,
    val lifetimeMiles: Double,
    val totalSessions: Int,
    val totalElevationFeet: Long,
    val totalHours: Double,
    val runMiles: Double,
    val hikeMiles: Double,
    val equestrianMiles: Double,
    val bestPaceSecPerMile: Long?,
    val longestQuestMiles: Double,
    val topMonthLabel: String,
    val realmJourneyComparison: String
)

object WeeklyChallengeEngine {

    fun currentWeekKey(nowMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            timeInMillis = nowMs
        }
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.US, "%04d-W%02d", year, week)
    }

    fun startOfCurrentWeekMs(nowMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            timeInMillis = nowMs
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun calculateProgressivePlan(
        activities: List<ActivityRecord>,
        pastChallenges: List<WeeklyChallenge>,
        baselineMiles: Double,
        nowMs: Long = System.currentTimeMillis()
    ): WeeklyChallengePlan {
        val thisWeekKey = currentWeekKey(nowMs)
        val weekStartMs = startOfCurrentWeekMs(nowMs)
        val oneWeekMs = 7L * 24L * 60L * 60L * 1000L

        val currentWeekActivities = activities.filter { it.startedAtEpochMs >= weekStartMs }
        val loggedThisWeek = roundOneDecimal(currentWeekActivities.sumOf { it.distanceMiles })

        val dailyTotals = MutableList(7) { 0.0 }
        currentWeekActivities.forEach { act ->
            val cal = Calendar.getInstance().apply { timeInMillis = act.startedAtEpochMs }
            val dayIndex = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                else -> 6
            }
            dailyTotals[dayIndex] += act.distanceMiles
        }

        val lastWeekMiles = activities
            .filter { it.startedAtEpochMs in (weekStartMs - oneWeekMs) until weekStartMs }
            .sumOf { it.distanceMiles }
        val twoWeeksAgoMiles = activities
            .filter { it.startedAtEpochMs in (weekStartMs - 2 * oneWeekMs) until (weekStartMs - oneWeekMs) }
            .sumOf { it.distanceMiles }
        val threeWeeksAgoMiles = activities
            .filter { it.startedAtEpochMs in (weekStartMs - 3 * oneWeekMs) until (weekStartMs - 2 * oneWeekMs) }
            .sumOf { it.distanceMiles }

        val priorChallenges = pastChallenges
            .filter { it.weekKey != thisWeekKey }
            .sortedByDescending { it.weekKey }

        var streak = 0L
        for (ch in priorChallenges) {
            if (ch.statusEnum() == ChallengeStatus.COMPLETED || ch.loggedMiles >= ch.targetMiles) {
                streak++
            } else {
                break
            }
        }

        val lastChallenge = priorChallenges.firstOrNull()
        val safeBaseline = baselineMiles.coerceIn(3.0, 120.0)

        val (targetMiles, reason) = when {
            lastChallenge != null && lastChallenge.loggedMiles >= lastChallenge.targetMiles * 1.15 -> {
                val stepped = roundHalfMile(lastChallenge.targetMiles * 1.12)
                stepped to "Quest Difficulty +12% after smashing last week's board by 15%+!"
            }
            lastChallenge != null && lastChallenge.loggedMiles >= lastChallenge.targetMiles -> {
                if (streak >= 3L && streak % 3L == 0L) {
                    val deload = roundHalfMile(lastChallenge.targetMiles * 0.92)
                    deload to "Tavern recovery week (-8%) after a $streak-week streak to recharge your stamina."
                } else {
                    val stepped = roundHalfMile(lastChallenge.targetMiles * 1.08)
                    stepped to "Progressive +8% guild step-up after conquering ${roundOneDecimal(lastChallenge.targetMiles)} mi!"
                }
            }
            lastWeekMiles > safeBaseline -> {
                val stepped = roundHalfMile(lastWeekMiles * 1.10)
                stepped to "Calibrated +10% above last week's ${roundOneDecimal(lastWeekMiles)} mi to level up endurance."
            }
            lastWeekMiles > 0.5 -> {
                val recentAvg = listOf(lastWeekMiles, twoWeeksAgoMiles, threeWeeksAgoMiles)
                    .filter { it > 0.0 }
                    .average()
                val stepped = roundHalfMile((recentAvg * 1.08).coerceAtLeast(safeBaseline * 0.85))
                stepped to "Steady +8% quest build from your ${roundOneDecimal(recentAvg)} mi weekly average."
            }
            else -> {
                val starter = roundHalfMile(safeBaseline)
                starter to "Fresh tavern quest tuned to your ${roundOneDecimal(starter)} mi baseline. Every stride counts!"
            }
        }

        val clampedTarget = targetMiles.coerceIn(3.0, 150.0)
        val status = if (loggedThisWeek >= clampedTarget) {
            ChallengeStatus.COMPLETED
        } else {
            ChallengeStatus.ACTIVE
        }
        val finalStreak = if (status == ChallengeStatus.COMPLETED) streak + 1L else streak

        val tier = when {
            clampedTarget >= 35.0 -> DifficultyTier.SUMMIT_CRUSHER
            clampedTarget >= 22.0 -> DifficultyTier.ALPINE_CLIMBER
            clampedTarget >= 13.0 -> DifficultyTier.RIDGE_RUNNER
            else -> DifficultyTier.TRAILHEAD
        }

        return WeeklyChallengePlan(
            weekKey = thisWeekKey,
            targetMiles = clampedTarget,
            loggedMiles = loggedThisWeek,
            difficultyTier = tier,
            progressionReason = reason,
            challengeStatus = status,
            dailyMilesMonToSun = dailyTotals.map { roundOneDecimal(it) },
            computedStreakWeeks = finalStreak
        )
    }

    /**
     * Tabletop fantasy-named achievement badges including 100-mile, challenging hike,
     * personal best pace, weekly quest participation, and equestrian endurance milestones.
     */
    fun evaluateMilestones(
        activities: List<ActivityRecord>,
        currentPlan: WeeklyChallengePlan,
        completedChallengesCount: Int = 0
    ): List<MilestoneCandidate> {
        val unlocked = mutableListOf<MilestoneCandidate>()
        if (activities.isEmpty()) return unlocked

        val totalMiles = activities.sumOf { it.distanceMiles }
        val totalElevation = activities.sumOf { it.elevationGainFeet }
        val runs = activities.filter { it.typeEnum() == ActivityType.RUN }
        val hikes = activities.filter { it.typeEnum() == ActivityType.HIKE }
        val rides = activities.filter { it.typeEnum() == ActivityType.EQUESTRIAN }
        val fastestRunPace = runs.filter { it.distanceMiles >= 1.0 }.minOfOrNull { it.avgPaceSecPerMile }
        val longestSingle = activities.maxOfOrNull { it.distanceMiles } ?: 0.0
        val challengingHike = hikes.firstOrNull { it.distanceMiles >= 4.0 || it.elevationGainFeet >= 800L }

        unlocked += MilestoneCandidate(
            badgeKey = "oath_first_trail",
            title = "First Howl · Oath of the Trail",
            subtitle = "Signed the tavern ledger and completed your opening quest.",
            metricHighlight = "${roundOneDecimal(activities.last().distanceMiles)} mi quest"
        )

        if (longestSingle >= 3.1 || totalMiles >= 3.1) {
            unlocked += MilestoneCandidate(
                badgeKey = "5k_first_frost",
                title = "5K · First Frost",
                subtitle = "Conquered a 5K frontier run or trail expedition!",
                metricHighlight = "5K (3.1 mi)"
            )
        }

        if (longestSingle >= 6.2 || totalMiles >= 10.0) {
            unlocked += MilestoneCandidate(
                badgeKey = "10k_ice_runner",
                title = "10K · Ice Runner",
                subtitle = "Blazed past 10K with crisp frost-wolf consistency!",
                metricHighlight = "${roundOneDecimal(totalMiles)} mi logged"
            )
        }

        if (longestSingle >= 13.1 || totalMiles >= 25.0) {
            unlocked += MilestoneCandidate(
                badgeKey = "half_marathon_alpha_strider",
                title = "Half Marathon 13.1 · Alpha Strider",
                subtitle = "Endured 13.1 miles or 25+ cumulative pack miles!",
                metricHighlight = "13.1 mi Alpha"
            )
        }

        if (longestSingle >= 26.2 || totalMiles >= 50.0) {
            unlocked += MilestoneCandidate(
                badgeKey = "marathon_26_winter_wolf",
                title = "Marathon 26.2 · Winter Wolf",
                subtitle = "Mastered marathon endurance under the mountain moon!",
                metricHighlight = "26.2 mi Winter Wolf"
            )
        }

        if (longestSingle >= 31.0 || totalMiles >= 75.0) {
            unlocked += MilestoneCandidate(
                badgeKey = "50k_ultra_strider",
                title = "50K Ultra · Mountain Crest",
                subtitle = "Entered ultra-trail territory with relentless stamina!",
                metricHighlight = "50K Ultra"
            )
        }

        if (totalMiles >= 100.0) {
            unlocked += MilestoneCandidate(
                badgeKey = "ultra_legend_100m",
                title = "Ultra Legend 100M · Pack Legend",
                subtitle = "First 100 miles logged — a legend sung in Striders Howl Tavern!",
                metricHighlight = "${roundOneDecimal(totalMiles)} mi Legend"
            )
        }

        if (challengingHike != null) {
            unlocked += MilestoneCandidate(
                badgeKey = "dragonspine_endurance_hunter",
                title = "Endurance Hunter · Dragonspine Peak",
                subtitle = "Completed a challenging mountain hike with grit and elevation!",
                metricHighlight = "${roundOneDecimal(challengingHike.distanceMiles)} mi · +${challengingHike.elevationGainFeet} ft"
            )
        }

        if (totalElevation >= 1000L) {
            unlocked += MilestoneCandidate(
                badgeKey = "cragborn_climber_1000",
                title = "Cragborn Summit Climber",
                subtitle = "Turned 1,000+ feet of vertical ascent into mountain strength!",
                metricHighlight = "+$totalElevation ft climbed"
            )
        }

        if (fastestRunPace != null && fastestRunPace <= 420L) {
            val mins = fastestRunPace / 60
            val secs = fastestRunPace % 60
            unlocked += MilestoneCandidate(
                badgeKey = "sub_7_pack_leader",
                title = "Sub-7 Min · Pack Leader",
                subtitle = "Blistering sub-7:00/mi turnover worthy of an Alpha!",
                metricHighlight = String.format(Locale.US, "%d:%02d /mi", mins, secs)
            )
        } else if (fastestRunPace != null && (runs.size >= 2 || fastestRunPace <= 540L)) {
            val mins = fastestRunPace / 60
            val secs = fastestRunPace % 60
            unlocked += MilestoneCandidate(
                badgeKey = "windrunner_lightning_sprint",
                title = "Lightning Sprint · Personal Best",
                subtitle = "Set a swift new personal best running pace on the trail!",
                metricHighlight = String.format(Locale.US, "%d:%02d /mi PB", mins, secs)
            )
        }

        if (rides.isNotEmpty()) {
            unlocked += MilestoneCandidate(
                badgeKey = "cavalry_outrider_initiate",
                title = "Cavalry Outrider Initiate",
                subtitle = "Completed an equestrian walk-trot-canter endurance ride!",
                metricHighlight = "${rides.size} ride(s)"
            )
        }

        if ((runs.isNotEmpty() && hikes.isNotEmpty()) || (rides.isNotEmpty() && (runs.isNotEmpty() || hikes.isNotEmpty()))) {
            unlocked += MilestoneCandidate(
                badgeKey = "ranger_all_realms",
                title = "Ranger of All Realms",
                subtitle = "Mastered multiple disciplines across foot and saddle.",
                metricHighlight = "${runs.size}R · ${hikes.size}H · ${rides.size}E"
            )
        }

        if (currentPlan.challengeStatus == ChallengeStatus.COMPLETED) {
            unlocked += MilestoneCandidate(
                badgeKey = "weekly_quest_${currentPlan.weekKey}",
                title = "Pack Weekly Champion",
                subtitle = "Conquered your progressive ${roundOneDecimal(currentPlan.targetMiles)} mi Pack quest for ${currentPlan.weekKey}!",
                metricHighlight = "${roundOneDecimal(currentPlan.loggedMiles)} / ${roundOneDecimal(currentPlan.targetMiles)} mi"
            )
        }

        val totalCompletedWeeks = completedChallengesCount + if (currentPlan.challengeStatus == ChallengeStatus.COMPLETED) 1 else 0
        if (totalCompletedWeeks >= 3 || currentPlan.computedStreakWeeks >= 3L) {
            unlocked += MilestoneCandidate(
                badgeKey = "order_eternal_hearth_3w",
                title = "Pack of the Eternal Hearth",
                subtitle = "Participated in and triumphed across 3+ weekly Pack quests!",
                metricHighlight = "$totalCompletedWeeks weekly quests"
            )
        }

        return unlocked
    }

    fun buildYearlySummary(
        activities: List<ActivityRecord>,
        year: Int = Calendar.getInstance().get(Calendar.YEAR)
    ): YearlyTravelSummary {
        val lifetimeMiles = activities.sumOf { it.distanceMiles }
        val yearActs = activities.filter { act ->
            val cal = Calendar.getInstance().apply { timeInMillis = act.startedAtEpochMs }
            cal.get(Calendar.YEAR) == year
        }
        val totalMiles = yearActs.sumOf { it.distanceMiles }
        val totalElev = yearActs.sumOf { it.elevationGainFeet }
        val totalHours = yearActs.sumOf { it.durationSeconds } / 3600.0
        val runMiles = yearActs.filter { it.typeEnum() == ActivityType.RUN }.sumOf { it.distanceMiles }
        val hikeMiles = yearActs.filter { it.typeEnum() == ActivityType.HIKE }.sumOf { it.distanceMiles }
        val eqMiles = yearActs.filter { it.typeEnum() == ActivityType.EQUESTRIAN }.sumOf { it.distanceMiles }
        val bestPace = yearActs.filter { it.typeEnum() == ActivityType.RUN && it.distanceMiles >= 1.0 }
            .minOfOrNull { it.avgPaceSecPerMile }
        val longest = yearActs.maxOfOrNull { it.distanceMiles } ?: 0.0

        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val byMonth = DoubleArray(12)
        yearActs.forEach { act ->
            val cal = Calendar.getInstance().apply { timeInMillis = act.startedAtEpochMs }
            val m = cal.get(Calendar.MONTH).coerceIn(0, 11)
            byMonth[m] += act.distanceMiles
        }
        val bestMonthIdx = byMonth.indices.maxByOrNull { byMonth[it] } ?: 0
        val topMonth = if (byMonth[bestMonthIdx] > 0.0) {
            "${monthNames[bestMonthIdx]} (${roundOneDecimal(byMonth[bestMonthIdx])} mi)"
        } else {
            "Journey beginning"
        }

        val comparison = when {
            totalMiles >= 500.0 -> "Crossed an entire mountain kingdom (${roundOneDecimal(totalMiles)} mi — equal to the Colorado Trail!)"
            totalMiles >= 211.0 -> "Completed the full John Muir Trail distance (211 mi)!"
            totalMiles >= 100.0 -> "Passed the 100-mile Centurion frontier across alpine passes!"
            totalMiles >= 26.2 -> "Covered ${(totalMiles / 26.2).toInt()}+ full marathon distances this year!"
            totalMiles >= 10.0 -> "Blazed ${roundOneDecimal(totalMiles)} miles into the high frontier!"
            else -> "Your $year chronicle is open — every trail mile adds to your legend."
        }

        return YearlyTravelSummary(
            year = year,
            totalMiles = roundOneDecimal(totalMiles),
            lifetimeMiles = roundOneDecimal(lifetimeMiles),
            totalSessions = yearActs.size,
            totalElevationFeet = totalElev,
            totalHours = roundOneDecimal(totalHours),
            runMiles = roundOneDecimal(runMiles),
            hikeMiles = roundOneDecimal(hikeMiles),
            equestrianMiles = roundOneDecimal(eqMiles),
            bestPaceSecPerMile = bestPace,
            longestQuestMiles = roundOneDecimal(longest),
            topMonthLabel = topMonth,
            realmJourneyComparison = comparison
        )
    }

    private fun roundHalfMile(value: Double): Double = round(value * 2.0) / 2.0
    private fun roundOneDecimal(value: Double): Double = round(value * 10.0) / 10.0
}
