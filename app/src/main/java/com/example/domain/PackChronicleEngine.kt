package com.example.domain

import com.example.data.model.ActivityRecord
import com.example.data.model.SummitFormatters
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

enum class SeasonPhase(val displayName: String) {
    EARLY_FROST("Early Frost"),
    DEEP_FREEZE("Deep Freeze"),
    HIGH_PASS_SNOWPACK("High Pass Snowpack"),
    SPRING_THAW("Spring Thaw")
}

enum class PackEventSeverity(val badgeLabel: String) {
    ROUTINE("PATROL LOG"),
    CRITICAL("CAUTION"),
    TRIUMPH("ALPHA TRIUMPH"),
    PERIL("SURVIVAL PERIL")
}

data class PackMember(
    val name: String,
    val role: String,
    val coatColor: String,
    val energyLevel: Float = 1.0f
)

data class PackSimulationState(
    val packName: String = "Blacktail Ridge Pack",
    val territorySquareMiles: Double = 120.0,
    val caloricReservesDays: Int = 4,
    val packMorale: Float = 0.85f,
    val season: SeasonPhase = SeasonPhase.DEEP_FREEZE,
    val daysSurviving: Int = 1,
    val members: List<PackMember> = defaultMembers(),
    val activeTerritoryWaypoints: List<String> = listOf(
        "Thunder Pass",
        "Deadfall Ravine",
        "Frozen River Bend",
        "Timberline Ridge"
    )
) {
    val packHealthTier: String
        get() = when {
            caloricReservesDays >= 6 && packMorale >= 0.8f -> "ALPHA TIER"
            caloricReservesDays >= 3 && packMorale >= 0.55f -> "RESILIENT"
            caloricReservesDays >= 2 -> "LEAN WINTER"
            else -> "PERILOUS"
        }

    companion object {
        fun defaultMembers(): List<PackMember> = listOf(
            PackMember("Kaelen", "Alpha Male", "Jet-Black", 0.94f),
            PackMember("Frostbite", "Lead Scout", "Charcoal Black", 0.90f),
            PackMember("Kira", "Tracker", "Silver-Tipped Black", 0.88f),
            PackMember("Onyx", "Yearling Guard", "Jet-Black", 0.85f)
        )
    }
}

data class ChronicleDispatch(
    val id: String,
    val activityId: String,
    val title: String,
    val dateString: String,
    val journalBody: String,
    val runnerImpactSummary: String,
    val severity: PackEventSeverity,
    val milesClaimed: Double,
    val caloriesGainedDays: Int,
    val timestampMs: Long
)

enum class StoryBadgeType(
    val title: String,
    val conditionSummary: String,
    val packRewardDescription: String,
    val isGoldEmblem: Boolean
) {
    MIDNIGHT_PATROL(
        title = "Midnight Patrol",
        conditionSummary = "Log a run between 8:00 PM and 5:00 AM",
        packRewardDescription = "+15 sq miles stealth perimeter patrol; deters rival pack encroachment.",
        isGoldEmblem = false
    ),
    RIDGE_HOWLER(
        title = "Ridge Howler",
        conditionSummary = "Climb 1,000+ vertical feet in a single session",
        packRewardDescription = "Unlocks the Summit Howl audio stinger; reinforces territory scent marks.",
        isGoldEmblem = true
    ),
    QUARRY_PURSUIT(
        title = "Quarry Pursuit",
        conditionSummary = "Sustain sub-7:30 /mi pace for 3+ miles",
        packRewardDescription = "+3 days caloric meat reserves from an intercepted herd quarry.",
        isGoldEmblem = false
    ),
    DEEP_FREEZE_ZERO(
        title = "Deep Freeze 0°",
        conditionSummary = "Complete a 5K+ (3.1 mi) winter trail outing",
        packRewardDescription = "Unlocks the Frostbitten Alpha winter pelt & extreme cold endurance.",
        isGoldEmblem = false
    ),
    ALPHA_GUARDIAN(
        title = "Alpha Guardian",
        conditionSummary = "Log 50+ total miles in a single calendar week",
        packRewardDescription = "Full pack morale boost; unlocks defensive Alpha howl cue.",
        isGoldEmblem = true
    ),
    CORRIDOR_PIONEER(
        title = "Corridor Pioneer",
        conditionSummary = "Complete a 10+ mile wilderness route",
        packRewardDescription = "Unlocks 50 acres in the community Wild Wolf Corridor fund.",
        isGoldEmblem = false
    )
}

data class StorySurvivalBadge(
    val type: StoryBadgeType,
    val unlocked: Boolean,
    val progressFraction: Float,
    val progressLabel: String
)

data class PackChronicleSnapshot(
    val packState: PackSimulationState,
    val dispatches: List<ChronicleDispatch>,
    val storyBadges: List<StorySurvivalBadge> = emptyList()
)

/**
 * Ecologically grounded Living Pack Chronicles simulation engine.
 * Deterministically replays all persisted user activities in chronological order so every
 * logged run, hike, or equestrian ride directly evolves territory sq mi, caloric reserves,
 * seasonal survival cycle, and generates field-biologist narrative dispatches.
 */
object PackChronicleEngine {

    private val bonusCorridors = listOf(
        "Bitterroot Saddle",
        "Cascades Frost Shelf",
        "Glacier Creek Narrows",
        "Yukon Boreal Overlook",
        "Obsidian Basin"
    )

    fun simulateFromActivities(
        activities: List<ActivityRecord>,
        packName: String = "Blacktail Ridge Pack",
        hasPackProtectorBonus: Boolean = false
    ): PackChronicleSnapshot {
        val chronological = activities.sortedBy { it.startedAtEpochMs }

        var territorySqMi = 120.0
        var caloricDays = 4
        var morale = if (hasPackProtectorBonus) 0.90f else 0.85f
        var daysSurviving = 1
        val waypoints = mutableListOf(
            "Thunder Pass",
            "Deadfall Ravine",
            "Frozen River Bend",
            "Timberline Ridge"
        )
        val dispatches = mutableListOf<ChronicleDispatch>()

        var lastEpochMs: Long? = null

        chronological.forEachIndexed { index, activity ->
            // Multi-day rest gaps simulate winter blizzards where the pack draws on fat reserves
            val prevMs = lastEpochMs
            if (prevMs != null) {
                val gapDays = ((activity.startedAtEpochMs - prevMs) / 86_400_000L).toInt().coerceAtLeast(0)
                if (gapDays >= 3) {
                    val blizzardBurn = (gapDays / 3).coerceAtMost(3)
                    caloricDays = (caloricDays - blizzardBurn).coerceIn(1, 10)
                    morale = (morale - 0.05f * blizzardBurn).coerceIn(0.35f, 1.0f)
                }
            }
            lastEpochMs = activity.startedAtEpochMs

            daysSurviving += 1
            val runMiles = activity.distanceMiles.coerceAtLeast(0.1)
            val elevFt = activity.elevationGainFeet.toDouble().coerceAtLeast(0.0)
            val paceMinPerMile = if (activity.avgPaceSecPerMile > 0L) {
                activity.avgPaceSecPerMile / 60.0
            } else {
                9.5
            }

            // Estimate winter season & cold factor from activity calendar month
            val cal = Calendar.getInstance().apply { timeInMillis = activity.startedAtEpochMs }
            val month = cal.get(Calendar.MONTH)
            val isWinterMonth = month in listOf(Calendar.NOVEMBER, Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH)
            val estimatedTempF = when {
                isWinterMonth && elevFt >= 800.0 -> 19
                isWinterMonth -> 26
                elevFt >= 1000.0 -> 34
                else -> 44
            }

            // 1. Ecological Impact Calculations
            val territoryGain = (runMiles * 2.4).coerceIn(1.0, 35.0)
            territorySqMi += territoryGain

            val huntSuccessful = runMiles >= 6.0 || paceMinPerMile < 8.0
            val foodDaysAdded = if (huntSuccessful) {
                val added = if (runMiles >= 10.0) 3 else 2
                caloricDays = (caloricDays + added).coerceIn(0, 10)
                morale = (morale + 0.06f).coerceIn(0.35f, 1.0f)
                added
            } else {
                caloricDays = (caloricDays - 1).coerceIn(0, 10)
                morale = (morale - 0.02f).coerceIn(0.35f, 1.0f)
                0
            }

            val highRidgePatrol = elevFt >= 800.0
            val isBitterCold = estimatedTempF <= 28

            if (isBitterCold || highRidgePatrol) {
                val nextCorridor = bonusCorridors.firstOrNull { it !in waypoints }
                if (nextCorridor != null && territorySqMi >= 135.0 + waypoints.size * 18.0) {
                    waypoints.add(nextCorridor)
                }
            }

            val milesStr = String.format(Locale.US, "%.1f", runMiles)
            val gainStr = String.format(Locale.US, "%.1f", territoryGain)

            val title: String
            var narrative: String
            val impact: String
            val severity: PackEventSeverity

            when {
                huntSuccessful && highRidgePatrol -> {
                    severity = PackEventSeverity.TRIUMPH
                    title = "Summit Ridge: The High Country Intercept"
                    narrative =
                        "Climbing through deep powder toward the wind-blown saddle (+${elevFt.roundToInt()} ft), " +
                        "the pack caught a heavy elk scent carrying down the thermal draft. With the lead scout pushing pace " +
                        "through the drifts on \"${activity.title}\", Kaelen and the black wolf vanguard flanked the valley below. " +
                        "By dusk, the pack secured an elk quarry in the shelter of the timberline, anchoring stores for the storm ahead."
                    impact = "+$gainStr sq mi secured • +$foodDaysAdded days meat supply • High-pass howling boundary reinforced."
                }
                huntSuccessful -> {
                    severity = PackEventSeverity.TRIUMPH
                    title = "Frozen River Run: Elk Tracks in the Ravine"
                    narrative =
                        "Following frozen river switchbacks at steady stride ($milesStr mi at ${SummitFormatters.formatPaceRaw(activity.avgPaceSecPerMile, com.example.data.model.DistanceUnit.MILES)} /mi), " +
                        "the pack covered ground without breaking formation. The black wolves cut across a sheltered drainage " +
                        "where a lone bull had yarded up in dense spruce, closing the split before freezing dusk set in."
                    impact = "+$gainStr sq mi patrolled • +$foodDaysAdded days food reserves added."
                }
                caloricDays <= 1 -> {
                    severity = PackEventSeverity.PERIL
                    title = "Deep Freeze: Caloric Hunger at the Border"
                    narrative =
                        "Sub-zero winds stripped the timber ridges. Despite covering $milesStr miles through drifting snow, " +
                        "no fresh game trails were discovered. The pack conserved motion, clustering closely in a rock overhang " +
                        "as temperatures dropped. A longer patrol (6+ mi) or tempo push (<8:00/mi) is needed to locate the herd."
                    impact = "Food stores critical ($caloricDays day left) • Extended patrol needed to replenish pack energy."
                }
                highRidgePatrol -> {
                    severity = PackEventSeverity.CRITICAL
                    title = "Alpine Ridge Howl Survey: High-Pass Vigil"
                    narrative =
                        "Ascending +${elevFt.roundToInt()} feet up wind-scoured scree, Kaelen and Frostbite claimed the high ridge " +
                        "above ${waypoints.last()}. Their dusk chorus echoed across two frozen basins, warning neighboring packs away " +
                        "from winter ungulate corridors."
                    impact = "+$gainStr sq mi surveyed • Ridge acoustic territory dominance locked."
                }
                else -> {
                    severity = PackEventSeverity.ROUTINE
                    title = "Perimeter Scent Marking on the Eastern Ridge"
                    narrative =
                        "A steady winter endurance patrol. The pack traversed $milesStr miles along the eastern canyon boundary " +
                        "during \"${activity.title}\", freshening scent marks on frosted boulders and down timber. " +
                        "Neighboring rivals tested the lower pass, but turned back after hearing the pack's evening chorus."
                    impact = "+$gainStr sq mi defended • Boundary dominance maintained."
                }
            }

            if (isBitterCold) {
                narrative += " Ice formed thick along their muzzles, yet the dark winter coats absorbed the pale alpine sun, preserving core endurance."
            }

            dispatches.add(
                ChronicleDispatch(
                    id = "dispatch_${activity.id.ifBlank { index.toString() }}",
                    activityId = activity.id,
                    title = title,
                    dateString = "WINTER CYCLE — DAY $daysSurviving · ${SummitFormatters.formatDate(activity.startedAtEpochMs).uppercase(Locale.US)}",
                    journalBody = narrative,
                    runnerImpactSummary = impact,
                    severity = severity,
                    milesClaimed = territoryGain,
                    caloriesGainedDays = foodDaysAdded,
                    timestampMs = activity.startedAtEpochMs
                )
            )
        }

        val seasonPhase = when {
            daysSurviving >= 24 -> SeasonPhase.SPRING_THAW
            daysSurviving >= 14 -> SeasonPhase.HIGH_PASS_SNOWPACK
            daysSurviving >= 5 -> SeasonPhase.DEEP_FREEZE
            else -> SeasonPhase.EARLY_FROST
        }

        val memberEnergyBase = ((caloricDays / 8f) * 0.65f + morale * 0.35f).coerceIn(0.32f, 1.0f)
        val members = listOf(
            PackMember("Kaelen", "Alpha Male", "Jet-Black", (memberEnergyBase + 0.06f).coerceIn(0.35f, 1.0f)),
            PackMember("Frostbite", "Lead Scout", "Charcoal Black", (memberEnergyBase + 0.02f).coerceIn(0.32f, 1.0f)),
            PackMember("Kira", "Tracker", "Silver-Tipped Black", memberEnergyBase),
            PackMember("Onyx", "Yearling Guard", "Jet-Black", (memberEnergyBase - 0.04f).coerceIn(0.30f, 1.0f))
        )

        return PackChronicleSnapshot(
            packState = PackSimulationState(
                packName = packName,
                territorySquareMiles = territorySqMi,
                caloricReservesDays = caloricDays,
                packMorale = morale,
                season = seasonPhase,
                daysSurviving = daysSurviving,
                members = members,
                activeTerritoryWaypoints = waypoints
            ),
            dispatches = dispatches.reversed(), // newest first
            storyBadges = evaluateStoryBadges(chronological)
        )
    }

    fun evaluateStoryBadges(activities: List<ActivityRecord>): List<StorySurvivalBadge> {
        // 1. Midnight Patrol: run logged between 8:00 PM (20) and 5:00 AM (0..4)
        val midnightRuns = activities.count { act ->
            val hour = Calendar.getInstance().apply { timeInMillis = act.startedAtEpochMs }
                .get(Calendar.HOUR_OF_DAY)
            hour >= 20 || hour < 5
        }
        val midnightUnlocked = midnightRuns >= 1

        // 2. Ridge Howler: climb 1,000+ vertical feet in a single trail session
        val maxSingleClimbFt = activities.maxOfOrNull { it.elevationGainFeet } ?: 0L
        val ridgeUnlocked = maxSingleClimbFt >= 1000L

        // 3. Quarry Pursuit: sustain sub-7:30/mi (<= 450 sec/mi) for 3+ miles
        val bestFastRunMiles = activities
            .filter { it.avgPaceSecPerMile in 1L..450L }
            .maxOfOrNull { it.distanceMiles } ?: 0.0
        val quarryUnlocked = bestFastRunMiles >= 3.0

        // 4. Deep Freeze 0°: complete a 5K+ (3.1 mi) trail outing
        val maxDistanceMiles = activities.maxOfOrNull { it.distanceMiles } ?: 0.0
        val deepFreezeUnlocked = maxDistanceMiles >= 3.1

        // 5. Alpha Guardian: log 50+ total miles in a single calendar week
        val milesByWeek = activities.groupBy { act ->
            val c = Calendar.getInstance().apply { timeInMillis = act.startedAtEpochMs }
            "${c.get(Calendar.YEAR)}-W${c.get(Calendar.WEEK_OF_YEAR)}"
        }.mapValues { (_, list) -> list.sumOf { it.distanceMiles } }
        val peakWeeklyMiles = milesByWeek.values.maxOrNull() ?: 0.0
        val alphaGuardianUnlocked = peakWeeklyMiles >= 50.0

        // 6. Corridor Pioneer: complete a 10+ mile wilderness route
        val corridorUnlocked = maxDistanceMiles >= 10.0

        return listOf(
            StorySurvivalBadge(
                type = StoryBadgeType.MIDNIGHT_PATROL,
                unlocked = midnightUnlocked,
                progressFraction = if (midnightUnlocked) 1f else 0f,
                progressLabel = if (midnightUnlocked) "$midnightRuns night patrol(s) logged" else "Log a run 8 PM–5 AM"
            ),
            StorySurvivalBadge(
                type = StoryBadgeType.RIDGE_HOWLER,
                unlocked = ridgeUnlocked,
                progressFraction = (maxSingleClimbFt / 1000f).coerceIn(0f, 1f),
                progressLabel = "${maxSingleClimbFt} / 1,000 ft max climb"
            ),
            StorySurvivalBadge(
                type = StoryBadgeType.QUARRY_PURSUIT,
                unlocked = quarryUnlocked,
                progressFraction = (bestFastRunMiles / 3.0).toFloat().coerceIn(0f, 1f),
                progressLabel = if (quarryUnlocked) {
                    String.format(Locale.US, "%.1f mi sub-7:30/mi", bestFastRunMiles)
                } else {
                    "3.0+ mi at <7:30/mi pace"
                }
            ),
            StorySurvivalBadge(
                type = StoryBadgeType.DEEP_FREEZE_ZERO,
                unlocked = deepFreezeUnlocked,
                progressFraction = (maxDistanceMiles / 3.1).toFloat().coerceIn(0f, 1f),
                progressLabel = String.format(Locale.US, "%.1f / 3.1 mi best run", maxDistanceMiles)
            ),
            StorySurvivalBadge(
                type = StoryBadgeType.ALPHA_GUARDIAN,
                unlocked = alphaGuardianUnlocked,
                progressFraction = (peakWeeklyMiles / 50.0).toFloat().coerceIn(0f, 1f),
                progressLabel = String.format(Locale.US, "%.1f / 50.0 mi best week", peakWeeklyMiles)
            ),
            StorySurvivalBadge(
                type = StoryBadgeType.CORRIDOR_PIONEER,
                unlocked = corridorUnlocked,
                progressFraction = (maxDistanceMiles / 10.0).toFloat().coerceIn(0f, 1f),
                progressLabel = String.format(Locale.US, "%.1f / 10.0 mi single route", maxDistanceMiles)
            )
        )
    }
}
