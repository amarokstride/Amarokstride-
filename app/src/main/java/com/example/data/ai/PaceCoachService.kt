package com.example.data.ai

import android.util.Log
import com.example.data.audio.SoundboardTrigger
import com.example.data.model.ActivityRecord
import com.example.data.model.ActivityType
import com.example.data.model.AdaptiveTrainingPlan
import com.example.data.model.CoachingInsight
import com.example.data.model.DistanceUnit
import com.example.data.model.RouteAnalysisReport
import com.example.data.model.SummitFormatters
import com.example.data.model.TrainingDaySuggestion
import com.example.domain.WeeklyChallengePlan
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "PaceCoachService"

class PaceCoachService {

    private val coachingSchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string(description = "Short warm encouraging headline under 8 words"),
            "encouragement" to Schema.string(description = "Warm celebration of what the strider is doing well (1-2 sentences)"),
            "improvementFocus" to Schema.string(description = "Gentle, constructive, specific suggestion to improve pace or pacing consistency (1-2 sentences)"),
            "nextSessionDrill" to Schema.string(description = "One concrete, approachable workout or trail pacing tip for next session (1-2 sentences)"),
            "targetPaceSuggestion" to Schema.string(description = "Realistic next target pace range string, e.g. '8:45 - 9:05 /mi'")
        )
    )

    private val trainingPlanSchema = Schema.obj(
        properties = mapOf(
            "summary" to Schema.string(description = "Encouraging overview of how this week's plan adapts to goal and recent watch/GPS data"),
            "restDayGuidance" to Schema.string(description = "Practical recovery and rest day guidance"),
            "motivationalTip" to Schema.string(description = "Warm fantasy wolf-pack tavern motivational pep talk"),
            "watchSyncNote" to Schema.string(description = "How COROS/Garmin/Strava/Health Connect history calibrated this week's plan"),
            "days" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "dayLabel" to Schema.string(description = "Mon, Tue, Wed, Thu, Fri, Sat, or Sun"),
                        "workoutTitle" to Schema.string(description = "Short workout name"),
                        "details" to Schema.string(description = "Specific duration, distance, or gait/pace guidance"),
                        "isRestDay" to Schema.boolean(description = "True if active recovery or full rest day")
                    )
                )
            )
        )
    )

    private val routeAnalysisSchema = Schema.obj(
        properties = mapOf(
            "terrainType" to Schema.string(description = "Concise terrain label e.g. 'Rolling Technical Singletrack' or 'Steep Alpine Switchbacks'"),
            "bestTimeAdvice" to Schema.string(description = "Specific best time-of-day recommendation based on historical weather and sun exposure"),
            "insights" to Schema.array(
                items = Schema.string(description = "Actionable route/elevation/weather performance insight")
            )
        )
    )

    private val coachModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = coachingSchema
                temperature = 0.7f
            }
        )
    }

    private val trainingPlanModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = trainingPlanSchema
                temperature = 0.65f
            }
        )
    }

    private val routeAnalysisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = routeAnalysisSchema
                temperature = 0.6f
            }
        )
    }

    private val alphaCoachModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
            },
            systemInstruction = content {
                text(
                    """
                    You are 'The Alpha'—the legendary running and endurance coach for Striders Howl.
                    Your persona is grounded, stoic, athletic, and fiercely motivating, like a seasoned wilderness guide leading a wolf pack across frozen mountain passes.

                    Guidance Rules:
                    - Keep every cue strictly under 25 words (1 to 2 punchy sentences). Runners are moving at high speed and need clear, impactful spoken cues.
                    - Tone: Deep, calm, steady, and resonant.
                    - Metaphors: Pack resilience, breath control in cold air, striding in rhythm, hunting the horizon.
                    - Never use robotic language, markdown formatting, emojis, or filler greetings. Output ONLY the spoken tactical audio cue.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun generatePaceCoaching(
        activities: List<ActivityRecord>,
        weeklyPlan: WeeklyChallengePlan,
        unit: DistanceUnit
    ): CoachingInsight {
        val recent = activities.take(6)
        val runs = recent.filter { it.typeEnum() == ActivityType.RUN }
        val hikes = recent.filter { it.typeEnum() == ActivityType.HIKE }
        val rides = recent.filter { it.typeEnum() == ActivityType.EQUESTRIAN }

        val avgRunPace = if (runs.isNotEmpty()) {
            SummitFormatters.formatPace(runs.map { it.avgPaceSecPerMile }.average().toLong(), unit)
        } else "No runs yet"
        val avgHikePace = if (hikes.isNotEmpty()) {
            SummitFormatters.formatPace(hikes.map { it.avgPaceSecPerMile }.average().toLong(), unit)
        } else "No hikes yet"

        val sessionSummary = if (recent.isEmpty()) {
            "The strider is starting a ${SummitFormatters.formatDistanceShort(weeklyPlan.targetMiles, unit)} weekly Pack mileage quest."
        } else {
            recent.joinToString("\n") { act ->
                "- ${act.typeEnum().displayName} (${act.sourceEnum().displayName}): ${SummitFormatters.formatDistance(act.distanceMiles, unit)}, " +
                    "avg pace ${SummitFormatters.formatPace(act.avgPaceSecPerMile, unit)}, " +
                    "elev ${SummitFormatters.formatElevation(act.elevationGainFeet, unit)}"
            }
        }

        val prompt = """
            You are an encouraging running, hiking, and equestrian endurance coach at the Striders Howl tavern.
            Analyze the athlete's recent performance and weekly progressive Pack quest, then offer warm encouragement plus ONE gentle, actionable pace improvement suggestion.
            Use ${unit.shortLabel} and ${unit.paceSuffix} units.
            Weekly Quest: ${SummitFormatters.formatDistanceShort(weeklyPlan.loggedMiles, unit)} of ${SummitFormatters.formatDistanceShort(weeklyPlan.targetMiles, unit)} (${weeklyPlan.difficultyTier.label}).
            Average Run Pace: $avgRunPace | Average Hike Pace: $avgHikePace | Recent Equestrian Rides: ${rides.size}
            Recent Sessions:
            $sessionSummary
        """.trimIndent()

        return try {
            val response = withTimeout(45_000L) { coachModel.generateContent(prompt) }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty coaching response."))
            CoachingInsight(
                headline = json.getString("headline"),
                encouragement = json.getString("encouragement"),
                improvementFocus = json.getString("improvementFocus"),
                nextSessionDrill = json.getString("nextSessionDrill"),
                targetPaceSuggestion = json.getString("targetPaceSuggestion")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Pace coaching timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Pace coaching generation failed", e)
            throw e
        }
    }

    suspend fun generateAdaptiveTrainingPlan(
        goalTitle: String,
        discipline: ActivityType,
        activities: List<ActivityRecord>,
        connectedWatchSources: String,
        unit: DistanceUnit
    ): AdaptiveTrainingPlan {
        val recentHistory = activities.take(8).joinToString("\n") { act ->
            "- ${act.typeEnum().displayName} [${act.sourceEnum().displayName}]: ${SummitFormatters.formatDistance(act.distanceMiles, unit)}, " +
                "pace ${SummitFormatters.formatPace(act.avgPaceSecPerMile, unit)}, elev ${SummitFormatters.formatElevation(act.elevationGainFeet, unit)}"
        }.ifBlank { "No prior sessions logged yet — starting from a fresh foundation." }

        val equestrianInstruction = if (discipline == ActivityType.EQUESTRIAN) {
            "Because this is an EQUESTRIAN horse endurance goal, build progressive walk/trot/canter intervals starting from 10 min walk, 10 min trot, 10 min canter repeating and progressively extending endurance blocks like a marathon plan, with mandatory equine recovery/rest days."
        } else {
            "Include balanced easy days, one quality/hill pace session, one weekend long outing, and at least 2 rest or active mobility days."
        }

        val prompt = """
            Build a personalized 7-day adaptive training week (Mon through Sun) for a Striders Howl athlete.
            Goal: "$goalTitle"
            Primary Discipline: ${discipline.displayName}
            Connected Watches/Apps: $connectedWatchSources
            Preferred Units: ${unit.shortLabel} (${unit.paceSuffix})
            $equestrianInstruction
            Adapt volume and intensity in real time based on these recent workouts:
            $recentHistory
        """.trimIndent()

        return try {
            val response = withTimeout(50_000L) { trainingPlanModel.generateContent(prompt) }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty training plan response."))
            val daysArr = json.optJSONArray("days")
            val days = mutableListOf<TrainingDaySuggestion>()
            if (daysArr != null) {
                for (i in 0 until daysArr.length()) {
                    val obj = daysArr.getJSONObject(i)
                    days += TrainingDaySuggestion(
                        dayLabel = obj.optString("dayLabel", "Day ${i + 1}"),
                        workoutTitle = obj.optString("workoutTitle", "Quest Session"),
                        details = obj.optString("details", ""),
                        isRestDay = obj.optBoolean("isRestDay", false)
                    )
                }
            }
            AdaptiveTrainingPlan(
                goalTitle = goalTitle.trim(),
                discipline = discipline,
                summary = json.optString("summary", "Adaptive weekly plan tailored to your Pack history."),
                weeklySchedule = days,
                restDayGuidance = json.optString("restDayGuidance", "Protect rest days for strong tendon and muscle adaptation."),
                motivationalTip = json.optString("motivationalTip", "Run steady with the pack—consistency always wins the long trail."),
                watchSyncNote = json.optString("watchSyncNote", "Calibrated from $connectedWatchSources."),
                updatedAtMs = System.currentTimeMillis()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Training plan generation failed", e)
            throw e
        }
    }

    suspend fun analyzeCompletedRoute(
        activity: ActivityRecord,
        weatherSummary: String,
        elevationLossFeet: Long,
        inclineSlowdownPct: Int,
        unit: DistanceUnit
    ): RouteAnalysisReport {
        val avgGradePct = if (activity.distanceMiles > 0.05) {
            ((activity.elevationGainFeet.toDouble() / (activity.distanceMiles * 5280.0)) * 100.0)
        } else 0.0

        val splitsSummary = if (activity.splitPacesSec.isNotEmpty()) {
            activity.splitPacesSec.take(10).joinToString(", ") { SummitFormatters.formatPaceRaw(it, unit) }
        } else "Continuous pace"

        val prompt = """
            You are an expert trail & endurance route analyst.
            Analyze this completed ${activity.typeEnum().displayName} route:
            Title: ${activity.title}
            Distance: ${SummitFormatters.formatDistance(activity.distanceMiles, unit)}
            Average Pace: ${SummitFormatters.formatPace(activity.avgPaceSecPerMile, unit)}
            Elevation Gain: +${activity.elevationGainFeet} ft | Elevation Loss: -$elevationLossFeet ft | Avg Climb Grade: ${String.format("%.1f", avgGradePct)}%
            Measured Incline Slowdown vs Flat Splits: $inclineSlowdownPct%
            Mile Splits: $splitsSummary
            Historical Weather: $weatherSummary

            Provide:
            1. terrainType (concise classification)
            2. bestTimeAdvice (e.g. early morning to avoid heat or afternoon warmth in winter)
            3. 3 specific, actionable insights (explicitly referencing the $inclineSlowdownPct% incline slowdown if > 0 and strength/cadence or equine conditioning advice).
        """.trimIndent()

        return try {
            val response = withTimeout(45_000L) { routeAnalysisModel.generateContent(prompt) }
            val json = JSONObject(response.text ?: throw IllegalStateException("Empty route analysis."))
            val insightsArr = json.optJSONArray("insights")
            val insights = mutableListOf<String>()
            if (insightsArr != null) {
                for (i in 0 until insightsArr.length()) {
                    val item = insightsArr.optString(i).trim()
                    if (item.isNotEmpty()) insights += item
                }
            }
            RouteAnalysisReport(
                activityId = activity.id,
                elevationGainFeet = activity.elevationGainFeet,
                elevationLossFeet = elevationLossFeet,
                terrainType = json.optString("terrainType", "Mixed Trail & Grade"),
                weatherSummary = weatherSummary,
                inclineSlowdownPct = inclineSlowdownPct,
                bestTimeAdvice = json.optString("bestTimeAdvice", "Early morning offers the coolest air and best footing."),
                insights = insights,
                analyzedAtMs = System.currentTimeMillis()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Route analysis failed", e)
            throw e
        }
    }

    suspend fun generateActivityRecap(
        activity: ActivityRecord,
        unit: DistanceUnit
    ): String {
        val splitsText = if (activity.splitPacesSec.size >= 2) {
            "Mile splits: " + activity.splitPacesSec.joinToString(", ") {
                SummitFormatters.formatPaceRaw(it, unit)
            }
        } else "Single continuous split."

        val prompt = """
            You are an encouraging running, hiking, and equestrian coach at the Striders Howl tavern.
            Write a concise, uplifting 2-sentence recap for this quest: celebrate one strength and suggest one gentle pacing improvement for next time.
            Activity: ${activity.typeEnum().displayName} "${activity.title}"
            Distance: ${SummitFormatters.formatDistance(activity.distanceMiles, unit)}
            Average Pace: ${SummitFormatters.formatPace(activity.avgPaceSecPerMile, unit)}
            Elevation Gain: ${SummitFormatters.formatElevation(activity.elevationGainFeet, unit)}
            $splitsText
        """.trimIndent()

        val textModel = Firebase.ai.generativeModel("gemini-3.8-flash")
        return try {
            val response = withTimeout(45_000L) { textModel.generateContent(prompt) }
            response.text?.trim()?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Empty coaching recap returned.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Activity recap generation failed", e)
            throw e
        }
    }

    /**
     * Generates a low-latency (<25 words) spoken tactical cue in 'The Alpha' persona
     * via Firebase AI Logic for the Dynamic Soundboard & live trail triggers.
     */
    suspend fun generateAlphaCoachCue(
        trigger: SoundboardTrigger,
        currentPace: String,
        heartRateBpm: Int,
        distanceLabel: String
    ): String {
        val hrText = if (heartRateBpm > 0) "$heartRateBpm bpm" else "not yet locked"
        val prompt = """
            The runner is currently on a trail session.
            - Event: ${trigger.title} (${trigger.scenarioPrompt})
            - Distance Logged: $distanceLabel
            - Current Pace: $currentPace
            - Current Heart Rate: $hrText

            Deliver an instantaneous 1-to-2 sentence spoken coaching cue under 25 words.
        """.trimIndent()

        return try {
            val response = withTimeout(30_000L) { alphaCoachModel.generateContent(prompt) }
            response.text?.trim()?.replace("\"", "")?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("The Alpha returned an empty audio cue.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Alpha coach cue timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Alpha coach cue generation failed", e)
            throw e
        }
    }
}
