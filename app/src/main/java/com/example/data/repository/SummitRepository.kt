package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AccountRole
import com.example.data.model.ActivityRecord
import com.example.data.model.ActivitySource
import com.example.data.model.ActivityType
import com.example.data.model.ChallengeStatus
import com.example.data.model.DifficultyTier
import com.example.data.model.DistanceUnit
import com.example.data.model.MilestoneRecord
import com.example.data.model.SeasonTheme
import com.example.data.model.TavernParty
import com.example.data.model.UserProfile
import com.example.data.model.WeeklyChallenge
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.random.Random

class SummitRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun partyDoc(partyCode: String) = db.collection("parties").document(partyCode)

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId"
        emitAll(
            userDoc(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfActive(error, userId, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeActivities(userId: String): Flow<List<ActivityRecord>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/activities"
        emitAll(
            userDoc(userId).collection("activities")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ActivityRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.startedAtEpochMs }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, userId, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeChallenges(userId: String): Flow<List<WeeklyChallenge>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/challenges"
        emitAll(
            userDoc(userId).collection("challenges")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WeeklyChallenge::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.weekKey }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, userId, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeMilestones(userId: String): Flow<List<MilestoneRecord>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/milestones"
        emitAll(
            userDoc(userId).collection("milestones")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MilestoneRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.unlockedAtEpochMs }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, userId, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeTavernParty(partyCode: String): Flow<TavernParty?> = flow {
        val clean = normalizePartyCode(partyCode)
        if (clean.length < 6) {
            emit(null)
            return@flow
        }
        val uid = auth.currentUser?.uid ?: ""
        val path = "parties/$clean"
        emitAll(
            partyDoc(clean)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            TavernParty::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfActive(error, uid, OperationType.GET, path)
                    emit(null)
                }
        )
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        uid: String,
        opType: OperationType,
        path: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, opType, path)
        }
    }

    suspend fun ensureUserProfile(
        displayName: String,
        initialRole: AccountRole = AccountRole.PRIMARY
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid"
        return try {
            val docRef = userDoc(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val safeName = displayName.trim().ifBlank { "Trail Explorer" }.take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "preferredUnit" to DistanceUnit.MILES.name,
                    "weeklyBaselineMiles" to 10.0,
                    "corosConnected" to false,
                    "garminConnected" to false,
                    "stravaConnected" to false,
                    "pathboundConnected" to false,
                    "healthConnectAutoExport" to false,
                    "currentStreakWeeks" to 0L,
                    "accountRole" to initialRole.name,
                    "seasonTheme" to SeasonTheme.AUTO.name,
                    "wolfCoat" to com.example.data.model.WolfCoat.TIMBER_GREY.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                // Keep accountRole updated if user explicitly switched login slot
                val existingRole = snapshot.getString("accountRole")
                if (existingRole != initialRole.name) {
                    docRef.update(
                        mapOf(
                            "accountRole" to initialRole.name,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreferences(
        displayName: String? = null,
        preferredUnit: DistanceUnit? = null,
        weeklyBaselineMiles: Double? = null,
        corosConnected: Boolean? = null,
        garminConnected: Boolean? = null,
        stravaConnected: Boolean? = null,
        pathboundConnected: Boolean? = null,
        healthConnectAutoExport: Boolean? = null,
        accountRole: AccountRole? = null,
        partnerPartyCode: String? = null,
        seasonTheme: SeasonTheme? = null,
        wolfCoat: com.example.data.model.WolfCoat? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid"
        return try {
            requireUserId()
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            displayName?.trim()?.takeIf { it.isNotEmpty() }?.let { updates["displayName"] = it.take(80) }
            preferredUnit?.let { updates["preferredUnit"] = it.name }
            weeklyBaselineMiles?.let { updates["weeklyBaselineMiles"] = it.coerceIn(1.0, 150.0) }
            corosConnected?.let { updates["corosConnected"] = it }
            garminConnected?.let { updates["garminConnected"] = it }
            stravaConnected?.let { updates["stravaConnected"] = it }
            pathboundConnected?.let { updates["pathboundConnected"] = it }
            healthConnectAutoExport?.let { updates["healthConnectAutoExport"] = it }
            accountRole?.let { updates["accountRole"] = it.name }
            partnerPartyCode?.let { updates["partnerPartyCode"] = normalizePartyCode(it).take(16) }
            seasonTheme?.let { updates["seasonTheme"] = it.name }
            wolfCoat?.let { updates["wolfCoat"] = it.name }

            userDoc(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Public Tavern Hall Chat & Private Pack Chat ---
    fun observePublicChat(): Flow<List<com.example.data.model.TavernChatMessage>> = flow {
        val uid = auth.currentUser?.uid ?: ""
        emitAll(
            db.collection("public_chat")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        com.example.data.model.TavernChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.sentAtEpochMs }.takeLast(60)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, uid, OperationType.LIST, "public_chat")
                    emit(emptyList())
                }
        )
    }

    fun observePackChat(partyCode: String): Flow<List<com.example.data.model.TavernChatMessage>> = flow {
        val clean = normalizePartyCode(partyCode)
        if (clean.length < 6) {
            emit(emptyList())
            return@flow
        }
        val uid = auth.currentUser?.uid ?: ""
        emitAll(
            partyDoc(clean).collection("messages")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        com.example.data.model.TavernChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.sentAtEpochMs }.takeLast(60)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, uid, OperationType.LIST, "parties/$clean/messages")
                    emit(emptyList())
                }
        )
    }

    suspend fun sendPublicChatMessage(
        senderName: String,
        wolfCoat: com.example.data.model.WolfCoat,
        packClass: String,
        text: String
    ): Result<Unit> {
        val cleanText = text.trim().take(400)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty"))
        return try {
            val uid = requireUserId()
            val ref = db.collection("public_chat").document()
            ref.set(
                mapOf(
                    "senderUid" to uid,
                    "senderName" to senderName.trim().ifBlank { "Wolf Strider" }.take(80),
                    "senderWolfCoat" to wolfCoat.name,
                    "senderPackClass" to packClass.trim().ifBlank { "Pack Scout" }.take(60),
                    "text" to cleanText,
                    "sentAtEpochMs" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "public_chat")
            Result.failure(e)
        }
    }

    suspend fun sendPackChatMessage(
        partyCode: String,
        senderName: String,
        wolfCoat: com.example.data.model.WolfCoat,
        packClass: String,
        text: String
    ): Result<Unit> {
        val code = normalizePartyCode(partyCode)
        val cleanText = text.trim().take(400)
        if (code.length < 6 || cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("Join a Wolf Pack first to send Pack Chat."))
        }
        return try {
            val uid = requireUserId()
            val ref = partyDoc(code).collection("messages").document()
            ref.set(
                mapOf(
                    "senderUid" to uid,
                    "senderName" to senderName.trim().ifBlank { "Wolf Strider" }.take(80),
                    "senderWolfCoat" to wolfCoat.name,
                    "senderPackClass" to packClass.trim().ifBlank { "Pack Scout" }.take(60),
                    "text" to cleanText,
                    "sentAtEpochMs" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "parties/$code/messages")
            Result.failure(e)
        }
    }

    // --- Live Solo & Pack Ranking Boards ---
    fun observeSoloRankings(): Flow<List<com.example.data.model.SoloRankingEntry>> = flow {
        val uid = auth.currentUser?.uid ?: ""
        emitAll(
            db.collection("solo_rankings")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        com.example.data.model.SoloRankingEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.totalMiles }.take(40)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, uid, OperationType.LIST, "solo_rankings")
                    emit(emptyList())
                }
        )
    }

    fun observePackRankings(): Flow<List<com.example.data.model.PackRankingEntry>> = flow {
        val uid = auth.currentUser?.uid ?: ""
        emitAll(
            db.collection("pack_rankings")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        com.example.data.model.PackRankingEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.combinedMiles }.take(40)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, uid, OperationType.LIST, "pack_rankings")
                    emit(emptyList())
                }
        )
    }

    suspend fun publishSoloRanking(
        displayName: String,
        wolfCoat: com.example.data.model.WolfCoat,
        packClass: String,
        level: Int,
        totalMiles: Double,
        weeklyMiles: Double
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("solo_rankings").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "displayName" to displayName.trim().ifBlank { "Wolf Strider" }.take(80),
                    "wolfCoat" to wolfCoat.name,
                    "packClass" to packClass.trim().ifBlank { "Pack Scout" }.take(60),
                    "level" to level.coerceIn(1, 500),
                    "totalMiles" to totalMiles.coerceIn(0.0, 100000.0),
                    "weeklyMiles" to weeklyMiles.coerceIn(0.0, 2000.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "solo_rankings")
            Result.failure(e)
        }
    }

    suspend fun publishPackRanking(
        party: TavernParty,
        leadWolfCoat: com.example.data.model.WolfCoat
    ): Result<Unit> {
        val code = normalizePartyCode(party.partyCode)
        if (code.length < 6) return Result.success(Unit)
        return try {
            requireUserId()
            val packLabel = party.resolvedPackName
            db.collection("pack_rankings").document(code).set(
                mapOf(
                    "partyCode" to code,
                    "packName" to packLabel.take(80),
                    "memberCount" to party.memberUids.size.coerceIn(1, 20),
                    "combinedMiles" to party.combinedMiles.coerceIn(0.0, 500000.0),
                    "leadWolfCoat" to leadWolfCoat.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "pack_rankings/$code")
            Result.failure(e)
        }
    }

    suspend fun updateStreak(streakWeeks: Long): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid"
        return try {
            requireUserId()
            userDoc(uid).update(
                mapOf(
                    "currentStreakWeeks" to streakWeeks.coerceIn(0L, 520L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Partner Tavern Party Co-op ---
    suspend fun createTavernParty(
        displayName: String,
        totalMiles: Double,
        lastQuestTitle: String,
        packName: String = "",
        packCrest: com.example.data.model.PackCrest = com.example.data.model.PackCrest.ALPHA_HOWL,
        weeklyGoalMiles: Double = 250.0,
        isPublic: Boolean = false
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val code = "WOLF-${Random.nextInt(1000, 9999)}"
            val docRef = partyDoc(code)
            val safeName = displayName.trim().ifBlank { "Primary Adventurer" }.take(80)
            val safePackName = packName.trim().ifBlank { "$safeName's Pack" }.take(80)
            val payload = mapOf(
                "partyCode" to code,
                "creatorUid" to uid,
                "memberUids" to listOf(uid),
                "packName" to safePackName,
                "packCrest" to packCrest.name,
                "weeklyGoalMiles" to weeklyGoalMiles.coerceIn(10.0, 2000.0),
                "isPublic" to isPublic,
                "primaryName" to safeName,
                "primaryTotalMiles" to totalMiles.coerceIn(0.0, 100000.0),
                "primaryLastQuest" to lastQuestTitle.trim().ifBlank { "Ready at the Hearth" }.take(120),
                "partnerTotalMiles" to 0.0,
                "cheerCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            updateProfilePreferences(partnerPartyCode = code)
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "parties")
            Result.failure(e)
        }
    }

    suspend fun joinTavernParty(
        rawCode: String,
        partnerDisplayName: String,
        partnerMiles: Double,
        partnerLastQuest: String
    ): Result<String> {
        val code = normalizePartyCode(rawCode)
        if (code.length < 6) {
            return Result.failure(IllegalArgumentException("Enter a valid 6+ character Party Code (e.g. WOLF-4821)."))
        }
        return try {
            val uid = requireUserId()
            val docRef = partyDoc(code)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return Result.failure(IllegalStateException("No Tavern Party found for code $code."))
            }
            val existing = snap.toObject(TavernParty::class.java)
                ?: return Result.failure(IllegalStateException("Could not read party data."))

            if (uid !in existing.memberUids) {
                if (existing.memberUids.size >= 2) {
                    return Result.failure(IllegalStateException("This Tavern Party already has 2 linked partners."))
                }
                val newMembers = existing.memberUids + uid
                docRef.update(
                    mapOf(
                        "memberUids" to newMembers,
                        "partnerUid" to uid,
                        "partnerName" to partnerDisplayName.trim().ifBlank { "Partner Adventurer" }.take(80),
                        "partnerTotalMiles" to partnerMiles.coerceIn(0.0, 100000.0),
                        "partnerLastQuest" to partnerLastQuest.trim().ifBlank { "Joined the Tavern!" }.take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            updateProfilePreferences(partnerPartyCode = code)
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "parties/$code")
            Result.failure(e)
        }
    }

    suspend fun renameTavernParty(partyCode: String, newPackName: String): Result<Unit> {
        val code = normalizePartyCode(partyCode)
        val cleanName = newPackName.trim().take(80)
        if (code.length < 6 || cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a valid Pack name."))
        }
        return try {
            requireUserId()
            partyDoc(code).update(
                mapOf(
                    "packName" to cleanName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "parties/$code")
            Result.failure(e)
        }
    }

    suspend fun syncMyStatsToParty(
        party: TavernParty,
        myDisplayName: String,
        myTotalMiles: Double,
        myLastQuest: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (uid !in party.memberUids) return Result.success(Unit)
        return try {
            val isCreator = party.creatorUid == uid
            val updates = if (isCreator) {
                mapOf(
                    "primaryName" to myDisplayName.trim().ifBlank { "Adventurer" }.take(80),
                    "primaryTotalMiles" to myTotalMiles.coerceIn(0.0, 100000.0),
                    "primaryLastQuest" to myLastQuest.trim().ifBlank { "On the trail" }.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf(
                    "partnerName" to myDisplayName.trim().ifBlank { "Partner" }.take(80),
                    "partnerTotalMiles" to myTotalMiles.coerceIn(0.0, 100000.0),
                    "partnerLastQuest" to myLastQuest.trim().ifBlank { "On the trail" }.take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            partyDoc(party.partyCode).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "parties/${party.partyCode}")
            Result.failure(e)
        }
    }

    suspend fun sendPartyCheer(partyCode: String): Result<Unit> {
        val code = normalizePartyCode(partyCode)
        return try {
            requireUserId()
            partyDoc(code).update(
                mapOf(
                    "cheerCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "parties/$code")
            Result.failure(e)
        }
    }

    suspend fun saveActivity(
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
        aiCoachingSummary: String? = null,
        routePoints: List<String> = emptyList(),
        splitPacesSec: List<Long> = emptyList()
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/activities"
        return try {
            val uid = requireUserId()
            val clampedDistance = distanceMiles.coerceIn(0.01, 500.0)
            val clampedDuration = durationSeconds.coerceIn(1L, 604800L)
            val computedPace = (clampedDuration / clampedDistance).toLong().coerceIn(120L, 7200L)
            val clampedElevation = elevationGainFeet.coerceIn(0L, 50000L)
            val safeStartedAt = startedAtEpochMs.coerceIn(946684800000L, 4102444800000L)
            val safeTitle = title.trim().ifBlank { "${activityType.questLabel} Session" }.take(100)

            val docRef = userDoc(uid).collection("activities").document()
            docPath = docRef.path

            val rawMap = mutableMapOf<String, Any?>(
                "userId" to uid,
                "title" to safeTitle,
                "activityType" to activityType.name,
                "source" to source.name,
                "distanceMiles" to clampedDistance,
                "durationSeconds" to clampedDuration,
                "avgPaceSecPerMile" to computedPace,
                "elevationGainFeet" to clampedElevation,
                "startedAtEpochMs" to safeStartedAt,
                "externalId" to externalId?.trim()?.takeIf { it.isNotEmpty() }?.take(128),
                "gearId" to gearId?.trim()?.takeIf { it.isNotEmpty() }?.take(64),
                "notes" to notes?.trim()?.takeIf { it.isNotEmpty() }?.take(500),
                "voiceTranscript" to voiceTranscript?.trim()?.takeIf { it.isNotEmpty() }?.take(1000),
                "aiCoachingSummary" to aiCoachingSummary?.trim()?.takeIf { it.isNotEmpty() }?.take(1200),
                "routePoints" to routePoints.take(250).takeIf { it.isNotEmpty() },
                "splitPacesSec" to splitPacesSec.take(100).takeIf { it.isNotEmpty() },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            val filtered = rawMap.filterValues { it != null } as Map<String, Any>
            docRef.set(filtered).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveActivityCoachingSummary(activityId: String, summary: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/activities/$activityId"
        return try {
            val uid = requireUserId()
            require(activityId.isNotBlank()) { "Activity ID must not be blank" }
            val docRef = userDoc(uid).collection("activities").document(activityId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "aiCoachingSummary" to summary.trim().take(1200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveActivityVoiceTranscript(activityId: String, transcript: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/activities/$activityId"
        return try {
            val uid = requireUserId()
            require(activityId.isNotBlank()) { "Activity ID must not be blank" }
            val docRef = userDoc(uid).collection("activities").document(activityId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "voiceTranscript" to transcript.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteActivity(activityId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/activities/$activityId"
        return try {
            val uid = requireUserId()
            require(activityId.isNotBlank()) { "Activity ID must not be blank" }
            val docRef = userDoc(uid).collection("activities").document(activityId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertWeeklyChallenge(
        weekKey: String,
        targetMiles: Double,
        loggedMiles: Double,
        difficultyTier: DifficultyTier,
        progressionReason: String,
        challengeStatus: ChallengeStatus
    ): Result<Unit> {
        val safeWeekId = weekKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/challenges/$safeWeekId"
        return try {
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("challenges").document(safeWeekId)
            docPath = docRef.path
            val existing = docRef.get().await()
            val clampedTarget = targetMiles.coerceIn(1.0, 200.0)
            val clampedLogged = loggedMiles.coerceIn(0.0, 500.0)
            val safeReason = progressionReason.trim().ifBlank { "Steady weekly build." }.take(240)

            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "weekKey" to safeWeekId,
                    "targetMiles" to clampedTarget,
                    "loggedMiles" to clampedLogged,
                    "difficultyTier" to difficultyTier.name,
                    "progressionReason" to safeReason,
                    "challengeStatus" to challengeStatus.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val currentStatus = ChallengeStatus.fromRaw(existing.getString("challengeStatus"))
                when (currentStatus) {
                    ChallengeStatus.ACTIVE -> {
                        val nextStatus = if (clampedLogged >= clampedTarget) {
                            ChallengeStatus.COMPLETED
                        } else {
                            challengeStatus
                        }
                        docRef.update(
                            mapOf(
                                "loggedMiles" to clampedLogged,
                                "targetMiles" to clampedTarget,
                                "difficultyTier" to difficultyTier.name,
                                "progressionReason" to safeReason,
                                "challengeStatus" to nextStatus.name,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                    ChallengeStatus.COMPLETED -> {
                        docRef.update(
                            mapOf(
                                "loggedMiles" to clampedLogged,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                    ChallengeStatus.PAST -> Unit
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun unlockMilestoneIfAbsent(
        badgeKey: String,
        title: String,
        subtitle: String,
        metricHighlight: String,
        unlockedAtEpochMs: Long = System.currentTimeMillis()
    ): Result<Boolean> {
        val safeKey = badgeKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(50)
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/milestones/$safeKey"
        return try {
            val uid = requireUserId()
            val docRef = userDoc(uid).collection("milestones").document(safeKey)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (existing.exists()) {
                return Result.success(false)
            }
            val payload = mapOf(
                "userId" to uid,
                "badgeKey" to safeKey,
                "title" to title.trim().take(80),
                "subtitle" to subtitle.trim().take(160),
                "metricHighlight" to metricHighlight.trim().take(60),
                "unlockedAtEpochMs" to unlockedAtEpochMs.coerceIn(946684800000L, 4102444800000L),
                "sharedCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun incrementMilestoneShareCount(milestoneId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/milestones/$milestoneId"
        return try {
            val uid = requireUserId()
            require(milestoneId.isNotBlank()) { "Milestone ID must not be blank" }
            val docRef = userDoc(uid).collection("milestones").document(milestoneId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "sharedCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAccountAndCloudData(): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Sign in with Google before deleting your account."))
        val uid = user.uid
        return try {
            val subcollections = listOf("activities", "challenges", "milestones")
            for (collectionName in subcollections) {
                val snapshot = userDoc(uid).collection(collectionName).get().await()
                val docs = snapshot.documents
                docs.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { doc -> batch.delete(doc.reference) }
                    batch.commit().await()
                }
            }
            runCatching {
                db.collection("solo_rankings").document(uid).delete().await()
            }
            runCatching {
                userDoc(uid).delete().await()
            }
            user.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Result.failure(
                IllegalStateException(
                    "For your security, please sign out, sign back in with Google, and tap Delete Account again to finalize account removal."
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }

    companion object {
        fun normalizePartyCode(raw: String): String =
            raw.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9\\-]"), "")
    }
}
