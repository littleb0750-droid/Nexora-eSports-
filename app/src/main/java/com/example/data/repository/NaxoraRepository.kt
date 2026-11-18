package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminPortalConfig
import com.example.data.model.AdminRecoveryLog
import com.example.data.model.PlatformConfig
import com.example.data.model.RoomAccess
import com.example.data.model.Tournament
import com.example.data.model.TournamentMode
import com.example.data.model.TournamentRegistration
import com.example.data.model.TournamentStatus
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WalletTxStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
import java.util.UUID

class NaxoraRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        firebaseUser: FirebaseUser,
        pendingName: String,
        pendingPhone: String,
        pendingBgmiIgn: String,
        pendingBgmiUid: String
    ): Result<Unit> {
        val uid = firebaseUser.uid
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = pendingName.trim().ifBlank {
                    firebaseUser.displayName?.trim()?.ifBlank { null }
                        ?: firebaseUser.email?.substringBefore("@")
                        ?: "Naxora Fighter"
                }.take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to (firebaseUser.email ?: "").take(160),
                    "phone" to pendingPhone.trim().take(24),
                    "bgmiIgn" to pendingBgmiIgn.trim().take(40),
                    "bgmiUid" to pendingBgmiUid.trim().take(24),
                    "depositBalance" to 0L,
                    "winningBalance" to 0L,
                    "matchesPlayed" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else if (pendingBgmiIgn.isNotBlank() || pendingBgmiUid.isNotBlank() || pendingPhone.isNotBlank()) {
                val updates = mutableMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (pendingName.isNotBlank()) updates["displayName"] = pendingName.trim().take(80)
                if (pendingPhone.isNotBlank()) updates["phone"] = pendingPhone.trim().take(24)
                if (pendingBgmiIgn.isNotBlank()) updates["bgmiIgn"] = pendingBgmiIgn.trim().take(40)
                if (pendingBgmiUid.isNotBlank()) updates["bgmiUid"] = pendingBgmiUid.trim().take(24)
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        phone: String,
        bgmiIgn: String,
        bgmiUid: String
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Refusing cross-user profile update." }
            val cleanName = displayName.trim()
            require(cleanName.isNotEmpty() && cleanName.length <= 80) { "Display name must be 1–80 characters." }
            db.collection("users").document(userId).update(
                mapOf(
                    "displayName" to cleanName,
                    "phone" to phone.trim().take(24),
                    "bgmiIgn" to bgmiIgn.trim().take(40),
                    "bgmiUid" to bgmiUid.trim().take(24),
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

    // --- Platform Config ---

    fun observePlatformConfig(): Flow<PlatformConfig> = flow {
        val path = "platformConfig/main"
        emitAll(
            db.collection("platformConfig").document("main")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(PlatformConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: PlatformConfig()
                    } else {
                        PlatformConfig()
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError != null && auth.currentUser != null) {
                        handleFirestoreError(firestoreError, OperationType.GET, path)
                    }
                    emit(PlatformConfig())
                }
        )
    }

    suspend fun savePlatformConfig(
        announcementText: String,
        adminUpiId: String,
        adminUpiName: String,
        whatsappSupportUrl: String,
        youtubeUrl: String,
        instagramUrl: String
    ): Result<Unit> {
        val path = "platformConfig/main"
        return try {
            requireUserId()
            val cleanBanner = announcementText.trim().take(300)
            val cleanUpi = adminUpiId.trim().take(80)
            val cleanUpiName = adminUpiName.trim().take(80)
            require(cleanBanner.isNotEmpty()) { "Announcement banner cannot be empty." }
            require(cleanUpi.length >= 3) { "Valid Admin UPI ID required." }
            require(cleanUpiName.isNotEmpty()) { "Admin UPI Name required." }

            val docRef = db.collection("platformConfig").document("main")
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "announcementText" to cleanBanner,
                        "adminUpiId" to cleanUpi,
                        "adminUpiName" to cleanUpiName,
                        "whatsappSupportUrl" to whatsappSupportUrl.trim().take(250),
                        "youtubeUrl" to youtubeUrl.trim().take(250),
                        "instagramUrl" to instagramUrl.trim().take(250),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "announcementText" to cleanBanner,
                        "adminUpiId" to cleanUpi,
                        "adminUpiName" to cleanUpiName,
                        "whatsappSupportUrl" to whatsappSupportUrl.trim().take(250),
                        "youtubeUrl" to youtubeUrl.trim().take(250),
                        "instagramUrl" to instagramUrl.trim().take(250),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Tournaments & 25-Slot Room System ---

    fun observeTournaments(): Flow<List<Tournament>> = flow {
        val path = "tournaments"
        emitAll(
            db.collection(path)
                .whereIn("status", listOf("UPCOMING", "LIVE", "COMPLETED"))
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Tournament::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.startTimeMillis }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyRegistration(tournamentId: String, userId: String): Flow<TournamentRegistration?> = flow {
        val path = "tournaments/$tournamentId/registrations/$userId"
        emitAll(
            db.collection("tournaments").document(tournamentId)
                .collection("registrations")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.firstOrNull()
                        ?.toObject(TournamentRegistration::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(firestoreError, OperationType.LIST, path)
                    }
                    emit(null)
                }
        )
    }

    suspend fun fetchRoomAccess(tournamentId: String): Result<RoomAccess?> {
        val path = "tournaments/$tournamentId/roomAccess/credentials"
        return try {
            requireUserId()
            val snap = db.collection("tournaments").document(tournamentId)
                .collection("roomAccess").document("credentials")
                .get().await()
            val access = if (snap.exists()) {
                snap.toObject(RoomAccess::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(access)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun joinTournamentSlot(
        tournament: Tournament,
        userProfile: UserProfile,
        slotNumber: Long,
        teamName: String,
        playerIgns: List<String>,
        playerUids: List<String>
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "tournaments/${tournament.id}/registrations/$uid"
        return try {
            require(slotNumber in 1L..tournament.maxSlots) { "Invalid slot number #$slotNumber" }
            require(!tournament.bookedSlotNumbers.contains(slotNumber)) { "Slot #$slotNumber is already booked." }
            val cleanTeam = teamName.trim().take(50)
            require(cleanTeam.isNotEmpty()) { "Team name is required." }
            require(playerIgns.isNotEmpty() && playerIgns.all { it.isNotBlank() }) { "All player IGNs are required." }
            require(playerUids.isNotEmpty() && playerUids.all { it.isNotBlank() }) { "All player Character UIDs are required." }

            val fee = tournament.entryFee
            require(userProfile.totalBalance >= fee) {
                "Insufficient balance (₹${userProfile.totalBalance}). Please add ₹${fee - userProfile.totalBalance} in Wallet."
            }

            // Deduct from depositBalance first, then winningBalance
            val fromDeposit = minOf(userProfile.depositBalance, fee)
            val fromWinning = fee - fromDeposit
            val newDeposit = userProfile.depositBalance - fromDeposit
            val newWinning = userProfile.winningBalance - fromWinning

            val batch = db.batch()

            val userRef = db.collection("users").document(uid)
            batch.update(
                userRef,
                mapOf(
                    "depositBalance" to newDeposit,
                    "winningBalance" to newWinning,
                    "matchesPlayed" to userProfile.matchesPlayed + 1L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val regRef = db.collection("tournaments").document(tournament.id)
                .collection("registrations").document(uid)
            batch.set(
                regRef,
                mapOf(
                    "userId" to uid,
                    "tournamentId" to tournament.id,
                    "slotNumber" to slotNumber,
                    "teamName" to cleanTeam,
                    "playerIgns" to playerIgns.map { it.trim().take(40) },
                    "playerUids" to playerUids.map { it.trim().take(24) },
                    "entryFeePaid" to fee,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val tourneyRef = db.collection("tournaments").document(tournament.id)
            batch.update(
                tourneyRef,
                mapOf(
                    "bookedSlotNumbers" to FieldValue.arrayUnion(slotNumber),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (fee > 0L) {
                val txId = "entry_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val txRef = db.collection("walletTransactions").document(txId)
                batch.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "userName" to userProfile.displayName.ifBlank { "Player" }.take(80),
                        "type" to "ENTRY_FEE",
                        "amount" to fee,
                        "status" to "APPROVED",
                        "note" to "Joined ${tournament.title} (Slot #$slotNumber)".take(200),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Wallet ---

    fun observeUserTransactions(userId: String): Flow<List<WalletTransaction>> = flow {
        val path = "walletTransactions"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun submitDepositRequest(
        userProfile: UserProfile,
        amount: Long,
        utrNumber: String
    ): Result<Unit> {
        val uid = requireUserId()
        val txId = "dep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "walletTransactions/$txId"
        return try {
            require(amount in 10L..100000L) { "Deposit amount must be between ₹10 and ₹1,00,000." }
            val cleanUtr = utrNumber.trim()
            require(cleanUtr.matches(Regex("^[0-9]{12}$"))) { "UTR must be exactly 12 digits." }

            db.collection("walletTransactions").document(txId).set(
                mapOf(
                    "userId" to uid,
                    "userName" to userProfile.displayName.ifBlank { "Player" }.take(80),
                    "type" to "DEPOSIT",
                    "amount" to amount,
                    "status" to "PENDING",
                    "utrNumber" to cleanUtr,
                    "note" to "UPI Add Cash (UTR: $cleanUtr)",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitWithdrawalRequest(
        userProfile: UserProfile,
        amount: Long,
        upiId: String
    ): Result<Unit> {
        val uid = requireUserId()
        val txId = "wdr_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "walletTransactions/$txId"
        return try {
            require(amount in 20L..100000L) { "Minimum withdrawal is ₹20." }
            require(userProfile.winningBalance >= amount) {
                "Insufficient Winning Cash (₹${userProfile.winningBalance} available)."
            }
            val cleanUpi = upiId.trim().take(80)
            require(cleanUpi.length >= 3 && cleanUpi.contains("@")) { "Enter a valid UPI ID (e.g., player@upi)." }

            val batch = db.batch()
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "winningBalance" to (userProfile.winningBalance - amount),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("walletTransactions").document(txId),
                mapOf(
                    "userId" to uid,
                    "userName" to userProfile.displayName.ifBlank { "Player" }.take(80),
                    "type" to "WITHDRAWAL",
                    "amount" to amount,
                    "status" to "PENDING",
                    "upiId" to cleanUpi,
                    "note" to "UPI Withdrawal to $cleanUpi".take(200),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Admin Portal & Management ---

    suspend fun checkIsAdminAccount(firebaseUser: FirebaseUser?): Boolean {
        if (firebaseUser == null) return false
        if (firebaseUser.email == BOOTSTRAP_ADMIN_EMAIL && firebaseUser.isEmailVerified) return true
        return try {
            val doc = db.collection("admins").document(firebaseUser.uid).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchAdminPortalConfig(): Result<AdminPortalConfig?> {
        val path = "adminConfig/master"
        return try {
            requireUserId()
            val snap = db.collection("adminConfig").document("master").get().await()
            val cfg = if (snap.exists()) {
                snap.toObject(AdminPortalConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(cfg)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeAdminRecoveryLogs(): Flow<List<AdminRecoveryLog>> = flow {
        val path = "adminRecoveryLogs"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AdminRecoveryLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                        .take(15)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun verifyRegisteredRecoveryEmail(enteredEmail: String): Result<String> {
        val path = "adminConfig/master"
        return try {
            val user = auth.currentUser
                ?: return Result.failure(IllegalStateException("Sign in with Google first to verify your admin recovery email."))
            if (!user.isEmailVerified) {
                return Result.failure(IllegalStateException("Your Google account email must be verified."))
            }
            val cleanEntered = enteredEmail.trim().lowercase()
            require(cleanEntered.contains("@") && cleanEntered.length >= 5) {
                "Enter a valid registered recovery email address."
            }

            val snap = db.collection("adminConfig").document("master").get().await()
            val cfg = if (snap.exists()) {
                snap.toObject(AdminPortalConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null

            val registeredRecovery = cfg?.recoveryEmail?.trim()?.lowercase()?.ifBlank { null }
                ?: cfg?.adminEmail?.trim()?.lowercase()?.ifBlank { null }
                ?: BOOTSTRAP_ADMIN_EMAIL.lowercase()

            if (cleanEntered != registeredRecovery) {
                return Result.failure(
                    IllegalArgumentException("Entered email does not match the registered Admin recovery email in Firebase.")
                )
            }

            val signedInEmail = user.email?.trim()?.lowercase().orEmpty()
            if (signedInEmail != registeredRecovery && signedInEmail != BOOTSTRAP_ADMIN_EMAIL.lowercase()) {
                return Result.failure(
                    IllegalAccessException("Signed-in Google account ($signedInEmail) does not match registered recovery email ($registeredRecovery). Tap 'Verify with Google Account' below.")
                )
            }

            val logId = "rec_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            db.collection("adminRecoveryLogs").document(logId).set(
                mapOf(
                    "adminUid" to user.uid,
                    "recoveryEmail" to registeredRecovery.take(160),
                    "eventType" to "RECOVERY_VERIFIED",
                    "note" to "Recovery email verified via Firebase Auth ($signedInEmail)".take(200),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(registeredRecovery)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun saveAdminPortalCredentials(
        adminEmail: String,
        recoveryEmail: String,
        newPasscode: String,
        recoveryHint: String
    ): Result<Unit> {
        val path = "adminConfig/master"
        return try {
            val uid = requireUserId()
            val cleanEmail = adminEmail.trim().take(160)
            val cleanRecovery = recoveryEmail.trim().lowercase().take(160)
            require(cleanEmail.length >= 3 && cleanEmail.contains("@")) { "Enter a valid Admin Email ID." }
            require(cleanRecovery.length >= 3 && cleanRecovery.contains("@")) { "Enter a valid Registered Recovery Email." }
            require(newPasscode.trim().length >= 4) { "Admin password must be at least 4 characters." }
            val hash = sha256Hex(newPasscode.trim())

            val docRef = db.collection("adminConfig").document("master")
            val existing = docRef.get().await()
            val logId = "rst_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val logRef = db.collection("adminRecoveryLogs").document(logId)

            val batch = db.batch()
            if (existing.exists()) {
                batch.update(
                    docRef,
                    mapOf(
                        "adminEmail" to cleanEmail,
                        "recoveryEmail" to cleanRecovery,
                        "passcodeHash" to hash,
                        "recoveryHint" to recoveryHint.trim().take(120),
                        "updatedByUid" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    logRef,
                    mapOf(
                        "adminUid" to uid,
                        "recoveryEmail" to cleanRecovery,
                        "eventType" to "PASSWORD_RESET",
                        "note" to "Admin credentials reset via verified recovery email ($cleanRecovery)".take(200),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    docRef,
                    mapOf(
                        "adminEmail" to cleanEmail,
                        "recoveryEmail" to cleanRecovery,
                        "passcodeHash" to hash,
                        "recoveryHint" to recoveryHint.trim().take(120),
                        "updatedByUid" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    logRef,
                    mapOf(
                        "adminUid" to uid,
                        "recoveryEmail" to cleanRecovery,
                        "eventType" to "PORTAL_INITIALIZED",
                        "note" to "Master Control Portal initialized with recovery email $cleanRecovery".take(200),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateTournament(
        existingId: String?,
        title: String,
        mapName: String,
        mode: TournamentMode,
        entryFee: Long,
        prizePool: Long,
        perKillPrize: Long,
        startTimeMillis: Long,
        status: TournamentStatus
    ): Result<String> {
        val uid = requireUserId()
        val tid = existingId?.takeIf { it.isNotBlank() }
            ?: "match_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "tournaments/$tid"
        return try {
            val cleanTitle = title.trim().take(100)
            require(cleanTitle.length >= 3) { "Match title must be at least 3 characters." }
            val docRef = db.collection("tournaments").document(tid)
            if (existingId.isNullOrBlank()) {
                docRef.set(
                    mapOf(
                        "title" to cleanTitle,
                        "mapName" to mapName.trim().take(40),
                        "mode" to mode.name,
                        "maxSlots" to mode.maxSlots,
                        "playersPerSlot" to mode.playersPerSlot,
                        "bookedSlotNumbers" to emptyList<Long>(),
                        "entryFee" to entryFee,
                        "prizePool" to prizePool,
                        "perKillPrize" to perKillPrize,
                        "startTimeMillis" to startTimeMillis,
                        "status" to status.name,
                        "roomPublished" to false,
                        "roomForceVisible" to false,
                        "createdBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "mapName" to mapName.trim().take(40),
                        "mode" to mode.name,
                        "maxSlots" to mode.maxSlots,
                        "playersPerSlot" to mode.playersPerSlot,
                        "entryFee" to entryFee,
                        "prizePool" to prizePool,
                        "perKillPrize" to perKillPrize,
                        "startTimeMillis" to startTimeMillis,
                        "status" to status.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(tid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun publishRoomCredentials(
        tournamentId: String,
        roomId: String,
        roomPassword: String,
        roomPublished: Boolean,
        roomForceVisible: Boolean
    ): Result<Unit> {
        val path = "tournaments/$tournamentId/roomAccess/credentials"
        return try {
            requireUserId()
            val cleanRoomId = roomId.trim().take(40)
            val cleanPass = roomPassword.trim().take(40)
            if (roomPublished) {
                require(cleanRoomId.isNotEmpty() && cleanPass.isNotEmpty()) {
                    "Room ID and Password cannot be empty when publishing."
                }
            }
            val accessRef = db.collection("tournaments").document(tournamentId)
                .collection("roomAccess").document("credentials")
            val existingAccess = accessRef.get().await()

            val batch = db.batch()
            if (existingAccess.exists()) {
                batch.update(
                    accessRef,
                    mapOf(
                        "roomId" to cleanRoomId,
                        "roomPassword" to cleanPass,
                        "roomPublished" to roomPublished,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    accessRef,
                    mapOf(
                        "roomId" to cleanRoomId,
                        "roomPassword" to cleanPass,
                        "roomPublished" to roomPublished,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.update(
                db.collection("tournaments").document(tournamentId),
                mapOf(
                    "roomPublished" to roomPublished,
                    "roomForceVisible" to roomForceVisible,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTournament(tournamentId: String): Result<Unit> {
        val path = "tournaments/$tournamentId"
        return try {
            requireUserId()
            db.collection("tournaments").document(tournamentId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeAllRegistrations(tournamentId: String): Flow<List<TournamentRegistration>> = flow {
        val path = "tournaments/$tournamentId/registrations"
        emitAll(
            db.collection("tournaments").document(tournamentId)
                .collection("registrations")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(TournamentRegistration::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.slotNumber }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun kickSlotRegistration(
        tournamentId: String,
        registration: TournamentRegistration
    ): Result<Unit> {
        val path = "tournaments/$tournamentId/registrations/${registration.id}"
        return try {
            requireUserId()
            val batch = db.batch()
            batch.delete(
                db.collection("tournaments").document(tournamentId)
                    .collection("registrations").document(registration.id)
            )
            batch.update(
                db.collection("tournaments").document(tournamentId),
                mapOf(
                    "bookedSlotNumbers" to FieldValue.arrayRemove(registration.slotNumber),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeAllTransactions(): Flow<List<WalletTransaction>> = flow {
        val path = "walletTransactions"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun resolveWalletTransaction(
        tx: WalletTransaction,
        approve: Boolean,
        adminNote: String
    ): Result<Unit> {
        val path = "walletTransactions/${tx.id}"
        return try {
            requireUserId()
            require(tx.statusEnum == WalletTxStatus.PENDING) { "Transaction is already finalized." }
            val newStatus = if (approve) "APPROVED" else "REJECTED"
            val note = adminNote.trim().ifBlank {
                if (approve) "Approved by Admin" else "Rejected by Admin"
            }.take(200)

            val batch = db.batch()
            batch.update(
                db.collection("walletTransactions").document(tx.id),
                mapOf(
                    "status" to newStatus,
                    "note" to note,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val userRef = db.collection("users").document(tx.userId)
            if (tx.type == "DEPOSIT" && approve) {
                batch.update(
                    userRef,
                    mapOf(
                        "depositBalance" to FieldValue.increment(tx.amount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else if (tx.type == "WITHDRAWAL" && !approve) {
                // Refund held winningBalance on rejection
                batch.update(
                    userRef,
                    mapOf(
                        "winningBalance" to FieldValue.increment(tx.amount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.totalBalance }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser != null) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun creditUserPrize(
        targetUser: UserProfile,
        amount: Long,
        reason: String
    ): Result<Unit> {
        val txId = "prize_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "users/${targetUser.userId}"
        return try {
            requireUserId()
            require(amount in 1L..500000L) { "Prize amount must be between ₹1 and ₹5,00,000." }
            val batch = db.batch()
            batch.update(
                db.collection("users").document(targetUser.userId),
                mapOf(
                    "winningBalance" to FieldValue.increment(amount),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("walletTransactions").document(txId),
                mapOf(
                    "userId" to targetUser.userId,
                    "userName" to targetUser.displayName.ifBlank { "Player" }.take(80),
                    "type" to "PRIZE",
                    "amount" to amount,
                    "status" to "APPROVED",
                    "note" to reason.trim().ifBlank { "Tournament Winnings Payout" }.take(200),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "littleb0750@gmail.com"
    }
}
