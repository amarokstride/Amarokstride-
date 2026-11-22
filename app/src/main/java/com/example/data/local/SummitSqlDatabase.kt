package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ActivityType
import com.example.data.model.AdaptiveTrainingPlan
import com.example.data.model.RouteAnalysisReport
import com.example.data.model.TrainingDaySuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "gear_items")
data class GearItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val category: String, // "SHOE" or "SADDLE"
    val brand: String,
    val loggedMiles: Double,
    val maxMiles: Double,
    val retired: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
) {
    val wearFraction: Float
        get() = if (maxMiles <= 0.0) 0f else (loggedMiles / maxMiles).toFloat().coerceIn(0f, 1f)
}

@Entity(tableName = "training_plans")
data class TrainingPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: String,
    val goalTitle: String,
    val discipline: String,
    val summary: String,
    val weeklyScheduleJson: String,
    val restDayGuidance: String,
    val motivationalTip: String,
    val watchSyncNote: String,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "route_analyses")
data class RouteAnalysisEntity(
    @PrimaryKey val activityId: String,
    val userId: String,
    val elevationGainFeet: Long,
    val elevationLossFeet: Long,
    val terrainType: String,
    val weatherSummary: String,
    val inclineSlowdownPct: Int,
    val bestTimeAdvice: String,
    val insightsJson: String,
    val analyzedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "voice_notes")
data class VoiceNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: String,
    val activityId: String?,
    val transcript: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_trails")
data class CustomTrailEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val discipline: String,
    val encodedPoints: String, // pipe-separated "lat,lon,ele"
    val distanceMiles: Double,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Dao
interface SummitSqlDao {
    @Query("SELECT * FROM gear_items WHERE userId = :userId ORDER BY retired ASC, createdAtMs DESC")
    fun observeGear(userId: String): Flow<List<GearItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGear(item: GearItemEntity)

    @Query("UPDATE gear_items SET loggedMiles = loggedMiles + :deltaMiles WHERE id = :gearId AND userId = :userId")
    suspend fun addGearMiles(gearId: String, userId: String, deltaMiles: Double)

    @Query("UPDATE gear_items SET retired = :retired WHERE id = :gearId AND userId = :userId")
    suspend fun setGearRetired(gearId: String, userId: String, retired: Boolean)

    @Query("DELETE FROM gear_items WHERE id = :gearId AND userId = :userId")
    suspend fun deleteGear(gearId: String, userId: String)

    @Query("SELECT * FROM training_plans WHERE userId = :userId ORDER BY updatedAtMs DESC")
    fun observeTrainingPlans(userId: String): Flow<List<TrainingPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainingPlan(plan: TrainingPlanEntity): Long

    @Query("DELETE FROM training_plans WHERE id = :planId AND userId = :userId")
    suspend fun deleteTrainingPlan(planId: Long, userId: String)

    @Query("SELECT * FROM route_analyses WHERE activityId = :activityId AND userId = :userId LIMIT 1")
    fun observeRouteAnalysis(activityId: String, userId: String): Flow<RouteAnalysisEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRouteAnalysis(entity: RouteAnalysisEntity)

    @Query("SELECT * FROM voice_notes WHERE userId = :userId ORDER BY createdAtMs DESC LIMIT 30")
    fun observeVoiceNotes(userId: String): Flow<List<VoiceNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceNote(note: VoiceNoteEntity): Long

    @Query("SELECT * FROM custom_trails WHERE userId = :userId ORDER BY createdAtMs DESC")
    fun observeCustomTrails(userId: String): Flow<List<CustomTrailEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomTrail(trail: CustomTrailEntity)

    @Query("DELETE FROM custom_trails WHERE id = :trailId AND userId = :userId")
    suspend fun deleteCustomTrail(trailId: String, userId: String)

    @Query("DELETE FROM gear_items WHERE userId = :userId")
    suspend fun deleteAllGearForUser(userId: String)

    @Query("DELETE FROM training_plans WHERE userId = :userId")
    suspend fun deleteAllTrainingPlansForUser(userId: String)

    @Query("DELETE FROM route_analyses WHERE userId = :userId")
    suspend fun deleteAllRouteAnalysesForUser(userId: String)

    @Query("DELETE FROM voice_notes WHERE userId = :userId")
    suspend fun deleteAllVoiceNotesForUser(userId: String)

    @Query("DELETE FROM custom_trails WHERE userId = :userId")
    suspend fun deleteAllCustomTrailsForUser(userId: String)
}

@Database(
    entities = [
        GearItemEntity::class,
        TrainingPlanEntity::class,
        RouteAnalysisEntity::class,
        VoiceNoteEntity::class,
        CustomTrailEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SummitSqlDatabase : RoomDatabase() {
    abstract fun dao(): SummitSqlDao

    companion object {
        @Volatile
        private var INSTANCE: SummitSqlDatabase? = null

        fun getInstance(context: Context): SummitSqlDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SummitSqlDatabase::class.java,
                    "summitpace_tavern_sql.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

class LocalSqlRepository(private val dao: SummitSqlDao) {

    fun observeGear(userId: String): Flow<List<GearItemEntity>> = dao.observeGear(userId)

    suspend fun saveGearItem(
        userId: String,
        name: String,
        category: String,
        brand: String,
        startingMiles: Double,
        maxMiles: Double
    ) {
        val safeCategory = if (category == "SADDLE") "SADDLE" else "SHOE"
        val id = "${safeCategory.lowercase()}_${System.currentTimeMillis()}"
        dao.upsertGear(
            GearItemEntity(
                id = id,
                userId = userId,
                name = name.trim().ifBlank { if (safeCategory == "SADDLE") "Trail Saddle" else "Trail Runners" },
                category = safeCategory,
                brand = brand.trim().ifBlank { "Guild Outfitter" },
                loggedMiles = startingMiles.coerceAtLeast(0.0),
                maxMiles = maxMiles.coerceIn(50.0, 5000.0)
            )
        )
    }

    suspend fun addMilesToGear(userId: String, gearId: String, miles: Double) {
        if (gearId.isBlank() || miles <= 0.0) return
        dao.addGearMiles(gearId, userId, miles)
    }

    suspend fun toggleGearRetired(userId: String, gearId: String, retired: Boolean) {
        dao.setGearRetired(gearId, userId, retired)
    }

    suspend fun deleteGear(userId: String, gearId: String) {
        dao.deleteGear(gearId, userId)
    }

    fun observeTrainingPlans(userId: String): Flow<List<AdaptiveTrainingPlan>> {
        return dao.observeTrainingPlans(userId).map { list ->
            list.map { entity ->
                val days = mutableListOf<TrainingDaySuggestion>()
                try {
                    val arr = JSONArray(entity.weeklyScheduleJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        days += TrainingDaySuggestion(
                            dayLabel = obj.optString("dayLabel", "Day ${i + 1}"),
                            workoutTitle = obj.optString("workoutTitle", "Trail Session"),
                            details = obj.optString("details", ""),
                            isRestDay = obj.optBoolean("isRestDay", false)
                        )
                    }
                } catch (_: Exception) {
                }
                AdaptiveTrainingPlan(
                    id = entity.id,
                    goalTitle = entity.goalTitle,
                    discipline = ActivityType.fromRaw(entity.discipline),
                    summary = entity.summary,
                    weeklySchedule = days,
                    restDayGuidance = entity.restDayGuidance,
                    motivationalTip = entity.motivationalTip,
                    watchSyncNote = entity.watchSyncNote,
                    updatedAtMs = entity.updatedAtMs
                )
            }
        }
    }

    suspend fun saveTrainingPlan(userId: String, plan: AdaptiveTrainingPlan) {
        val arr = JSONArray()
        plan.weeklySchedule.forEach { day ->
            arr.put(
                JSONObject().apply {
                    put("dayLabel", day.dayLabel)
                    put("workoutTitle", day.workoutTitle)
                    put("details", day.details)
                    put("isRestDay", day.isRestDay)
                }
            )
        }
        dao.insertTrainingPlan(
            TrainingPlanEntity(
                id = plan.id,
                userId = userId,
                goalTitle = plan.goalTitle,
                discipline = plan.discipline.name,
                summary = plan.summary,
                weeklyScheduleJson = arr.toString(),
                restDayGuidance = plan.restDayGuidance,
                motivationalTip = plan.motivationalTip,
                watchSyncNote = plan.watchSyncNote,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteTrainingPlan(userId: String, planId: Long) {
        dao.deleteTrainingPlan(planId, userId)
    }

    fun observeRouteAnalysis(activityId: String, userId: String): Flow<RouteAnalysisReport?> {
        return dao.observeRouteAnalysis(activityId, userId).map { entity ->
            if (entity == null) null
            else {
                val insights = mutableListOf<String>()
                try {
                    val arr = JSONArray(entity.insightsJson)
                    for (i in 0 until arr.length()) {
                        insights += arr.optString(i)
                    }
                } catch (_: Exception) {
                }
                RouteAnalysisReport(
                    activityId = entity.activityId,
                    elevationGainFeet = entity.elevationGainFeet,
                    elevationLossFeet = entity.elevationLossFeet,
                    terrainType = entity.terrainType,
                    weatherSummary = entity.weatherSummary,
                    inclineSlowdownPct = entity.inclineSlowdownPct,
                    bestTimeAdvice = entity.bestTimeAdvice,
                    insights = insights,
                    analyzedAtMs = entity.analyzedAtMs
                )
            }
        }
    }

    suspend fun saveRouteAnalysis(userId: String, report: RouteAnalysisReport) {
        val arr = JSONArray()
        report.insights.forEach { arr.put(it) }
        dao.upsertRouteAnalysis(
            RouteAnalysisEntity(
                activityId = report.activityId,
                userId = userId,
                elevationGainFeet = report.elevationGainFeet,
                elevationLossFeet = report.elevationLossFeet,
                terrainType = report.terrainType,
                weatherSummary = report.weatherSummary,
                inclineSlowdownPct = report.inclineSlowdownPct,
                bestTimeAdvice = report.bestTimeAdvice,
                insightsJson = arr.toString(),
                analyzedAtMs = report.analyzedAtMs
            )
        )
    }

    fun observeVoiceNotes(userId: String): Flow<List<VoiceNoteEntity>> = dao.observeVoiceNotes(userId)

    suspend fun saveVoiceNote(userId: String, activityId: String?, transcript: String) {
        if (transcript.isBlank()) return
        dao.insertVoiceNote(
            VoiceNoteEntity(
                userId = userId,
                activityId = activityId,
                transcript = transcript.trim().take(1000)
            )
        )
    }

    fun observeCustomTrails(userId: String): Flow<List<CustomTrailEntity>> = dao.observeCustomTrails(userId)

    suspend fun saveCustomTrail(
        userId: String,
        name: String,
        discipline: ActivityType,
        encodedPoints: List<String>,
        distanceMiles: Double
    ) {
        val id = "trail_${System.currentTimeMillis()}"
        dao.upsertCustomTrail(
            CustomTrailEntity(
                id = id,
                userId = userId,
                name = name.trim().ifBlank { "Custom Quest Trail" }.take(80),
                discipline = discipline.name,
                encodedPoints = encodedPoints.joinToString("|"),
                distanceMiles = distanceMiles.coerceAtLeast(0.05)
            )
        )
    }

    suspend fun deleteCustomTrail(userId: String, trailId: String) {
        dao.deleteCustomTrail(trailId, userId)
    }

    suspend fun deleteAllDataForUser(userId: String) {
        if (userId.isBlank()) return
        dao.deleteAllGearForUser(userId)
        dao.deleteAllTrainingPlansForUser(userId)
        dao.deleteAllRouteAnalysesForUser(userId)
        dao.deleteAllVoiceNotesForUser(userId)
        dao.deleteAllCustomTrailsForUser(userId)
    }
}
