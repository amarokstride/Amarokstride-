package com.example.data.integrations

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import com.example.data.model.ActivityRecord
import com.example.data.model.ActivitySource
import com.example.data.model.ActivityType
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToLong

private const val TAG = "HealthConnectManager"
private const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
private const val METERS_PER_MILE = 1609.344
private const val METERS_TO_FEET = 3.28084

enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE
}

data class HealthConnectSnapshot(
    val availability: HealthConnectAvailability = HealthConnectAvailability.UNAVAILABLE,
    val grantedPermissionsCount: Int = 0,
    val totalRequiredPermissions: Int = HealthConnectManager.REQUIRED_PERMISSIONS.size,
    val weeklyDistanceMiles: Double = 0.0,
    val weeklyElevationFeet: Long = 0L,
    val recentSessionCount: Int = 0
) {
    val allPermissionsGranted: Boolean
        get() = availability == HealthConnectAvailability.AVAILABLE &&
            totalRequiredPermissions > 0 &&
            grantedPermissionsCount >= totalRequiredPermissions
}

data class ImportedExternalActivity(
    val title: String,
    val activityType: ActivityType,
    val source: ActivitySource,
    val distanceMiles: Double,
    val durationSeconds: Long,
    val elevationGainFeet: Long,
    val startedAtEpochMs: Long,
    val externalId: String,
    val notes: String? = null
)

class HealthConnectManager(private val context: Context) {

    companion object {
        val REQUIRED_PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getWritePermission(DistanceRecord::class),
            HealthPermission.getReadPermission(SpeedRecord::class),
            HealthPermission.getWritePermission(SpeedRecord::class),
            HealthPermission.getReadPermission(ElevationGainedRecord::class),
            HealthPermission.getWritePermission(ElevationGainedRecord::class)
        )
    }

    val requiredPermissions: Set<String> = REQUIRED_PERMISSIONS

    fun getAvailability(): HealthConnectAvailability {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return HealthConnectAvailability.UNAVAILABLE
        }
        return try {
            when (HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PROVIDER_PACKAGE)) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                    HealthConnectAvailability.UPDATE_REQUIRED
                else -> HealthConnectAvailability.UNAVAILABLE
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query Health Connect SDK status", e)
            HealthConnectAvailability.UNAVAILABLE
        }
    }

    fun buildInstallOrUpdateIntent(): Intent {
        val playStoreUri = Uri.parse(
            "market://details?id=$HEALTH_CONNECT_PROVIDER_PACKAGE&url=healthconnect%3A%2F%2Fonboarding"
        )
        return Intent(Intent.ACTION_VIEW, playStoreUri).apply {
            setPackage("com.android.vending")
            putExtra("overlay", true)
            putExtra("callerId", context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildHealthConnectSettingsIntent(): Intent {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
        } else {
            "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
        }
        return Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun getClientOrNull(): HealthConnectClient? {
        if (getAvailability() != HealthConnectAvailability.AVAILABLE) return null
        return try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to obtain HealthConnectClient", e)
            null
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = getClientOrNull() ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(requiredPermissions)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check granted Health Connect permissions", e)
            false
        }
    }

    suspend fun getStatusSnapshot(): HealthConnectSnapshot {
        val availability = getAvailability()
        if (availability != HealthConnectAvailability.AVAILABLE) {
            return HealthConnectSnapshot(availability = availability)
        }
        val client = getClientOrNull() ?: return HealthConnectSnapshot(availability = availability)
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            val grantedRequiredCount = requiredPermissions.count { it in granted }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && grantedRequiredCount > 0) {
                val now = Instant.now()
                val weekAgo = now.minusSeconds(7L * 86400L)
                val weeklyAgg = runCatching {
                    client.aggregate(
                        AggregateRequest(
                            metrics = setOf(
                                DistanceRecord.DISTANCE_TOTAL,
                                ElevationGainedRecord.ELEVATION_GAINED_TOTAL
                            ),
                            timeRangeFilter = TimeRangeFilter.between(weekAgo, now)
                        )
                    )
                }.getOrNull()
                val recentSessions = runCatching {
                    client.readRecords(
                        ReadRecordsRequest(
                            recordType = ExerciseSessionRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(weekAgo, now)
                        )
                    ).records.size
                }.getOrDefault(0)
                val weeklyMiles = (weeklyAgg?.get(DistanceRecord.DISTANCE_TOTAL)?.inMeters ?: 0.0) / METERS_PER_MILE
                val weeklyElevFt = ((weeklyAgg?.get(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)?.inMeters ?: 0.0) * METERS_TO_FEET)
                    .roundToLong()
                    .coerceAtLeast(0L)
                HealthConnectSnapshot(
                    availability = availability,
                    grantedPermissionsCount = grantedRequiredCount,
                    totalRequiredPermissions = requiredPermissions.size,
                    weeklyDistanceMiles = weeklyMiles,
                    weeklyElevationFeet = weeklyElevFt,
                    recentSessionCount = recentSessions
                )
            } else {
                HealthConnectSnapshot(
                    availability = availability,
                    grantedPermissionsCount = grantedRequiredCount,
                    totalRequiredPermissions = requiredPermissions.size
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to build Health Connect status snapshot", e)
            HealthConnectSnapshot(availability = availability)
        }
    }

    suspend fun readRecentRunsAndHikes(daysBack: Long = 30L): Result<List<ImportedExternalActivity>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return Result.failure(IllegalStateException("Google Health Connect requires Android 8.0 or higher."))
        }
        val client = getClientOrNull()
            ?: return Result.failure(IllegalStateException("Google Health Connect is not available on this device."))

        return try {
            val granted = client.permissionController.getGrantedPermissions()
            if (HealthPermission.getReadPermission(ExerciseSessionRecord::class) !in granted) {
                return Result.failure(
                    IllegalStateException("Grant Google Health Connect Exercise permissions to sync workouts.")
                )
            }

            val now = Instant.now()
            val start = now.minusSeconds(daysBack.coerceIn(1L, 90L) * 86400L)
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, now)
                )
            )

            val imported = mutableListOf<ImportedExternalActivity>()
            for (session in response.records) {
                val mappedType = when (session.exerciseType) {
                    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> ActivityType.RUN
                    ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
                    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> ActivityType.HIKE
                    else -> continue
                }

                val durationSec = (session.endTime.epochSecond - session.startTime.epochSecond)
                    .coerceAtLeast(60L)

                val agg = runCatching {
                    client.aggregate(
                        AggregateRequest(
                            metrics = setOf(
                                DistanceRecord.DISTANCE_TOTAL,
                                ElevationGainedRecord.ELEVATION_GAINED_TOTAL
                            ),
                            timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime)
                        )
                    )
                }.getOrNull()

                val meters = agg?.get(DistanceRecord.DISTANCE_TOTAL)?.inMeters ?: 0.0
                val distanceMiles = if (meters > 15.0) {
                    meters / METERS_PER_MILE
                } else {
                    (durationSec / 600.0).coerceAtLeast(0.25)
                }
                val elevationFeet = ((agg?.get(ElevationGainedRecord.ELEVATION_GAINED_TOTAL)?.inMeters ?: 0.0) * METERS_TO_FEET)
                    .roundToLong()
                    .coerceAtLeast(0L)

                val originPkg = session.metadata.dataOrigin.packageName.lowercase()
                val source = when {
                    originPkg.contains("coros") -> ActivitySource.COROS
                    originPkg.contains("garmin") -> ActivitySource.GARMIN
                    originPkg.contains("strava") -> ActivitySource.STRAVA
                    else -> ActivitySource.HEALTH_CONNECT
                }

                val defaultTitle = when (source) {
                    ActivitySource.COROS -> "COROS ${mappedType.displayName}"
                    ActivitySource.GARMIN -> "Garmin ${mappedType.displayName}"
                    ActivitySource.STRAVA -> "Strava ${mappedType.displayName}"
                    else -> "Health Connect ${mappedType.displayName}"
                }

                imported += ImportedExternalActivity(
                    title = session.title?.takeIf { it.isNotBlank() } ?: defaultTitle,
                    activityType = mappedType,
                    source = source,
                    distanceMiles = distanceMiles,
                    durationSeconds = durationSec,
                    elevationGainFeet = elevationFeet,
                    startedAtEpochMs = session.startTime.toEpochMilli(),
                    externalId = "hc_${session.metadata.id}",
                    notes = session.notes ?: "Synced via Google Health Connect (${source.displayName})"
                )
            }
            Result.success(imported)
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading Health Connect sessions", e)
            Result.failure(e)
        }
    }

    suspend fun exportActivity(activity: ActivityRecord): Result<Unit> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return Result.failure(IllegalStateException("Google Health Connect requires Android 8.0 or higher."))
        }
        val client = getClientOrNull()
            ?: return Result.failure(IllegalStateException("Google Health Connect is not available."))

        return try {
            val granted = client.permissionController.getGrantedPermissions()
            if (HealthPermission.getWritePermission(ExerciseSessionRecord::class) !in granted) {
                return Result.failure(
                    IllegalStateException("Grant Google Health Connect write permissions to export workouts.")
                )
            }

            val startInstant = Instant.ofEpochMilli(activity.startedAtEpochMs)
            val endInstant = startInstant.plusSeconds(activity.durationSeconds.coerceAtLeast(60L))
            val zoneOffset = ZoneOffset.systemDefault().rules.getOffset(startInstant)
            val exerciseType = when (activity.typeEnum()) {
                ActivityType.RUN -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
                ActivityType.HIKE -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
                ActivityType.EQUESTRIAN -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            }

            val clientIdPrefix = "summitpace_${activity.id.ifBlank { activity.startedAtEpochMs.toString() }}"
            val sessionRecord = ExerciseSessionRecord(
                startTime = startInstant,
                startZoneOffset = zoneOffset,
                endTime = endInstant,
                endZoneOffset = zoneOffset,
                exerciseType = exerciseType,
                title = activity.title,
                notes = activity.notes,
                metadata = Metadata.manualEntry(clientRecordId = "${clientIdPrefix}_session")
            )
            val records = mutableListOf<androidx.health.connect.client.records.Record>(sessionRecord)

            if (HealthPermission.getWritePermission(DistanceRecord::class) in granted) {
                records += DistanceRecord(
                    startTime = startInstant,
                    startZoneOffset = zoneOffset,
                    endTime = endInstant,
                    endZoneOffset = zoneOffset,
                    distance = Length.meters(activity.distanceMiles * METERS_PER_MILE),
                    metadata = Metadata.manualEntry(clientRecordId = "${clientIdPrefix}_distance")
                )
            }
            if (activity.elevationGainFeet > 0L &&
                HealthPermission.getWritePermission(ElevationGainedRecord::class) in granted
            ) {
                records += ElevationGainedRecord(
                    startTime = startInstant,
                    startZoneOffset = zoneOffset,
                    endTime = endInstant,
                    endZoneOffset = zoneOffset,
                    elevation = Length.meters(activity.elevationGainFeet / METERS_TO_FEET),
                    metadata = Metadata.manualEntry(clientRecordId = "${clientIdPrefix}_elev")
                )
            }
            client.insertRecords(records)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed exporting activity to Health Connect", e)
            Result.failure(e)
        }
    }
}
