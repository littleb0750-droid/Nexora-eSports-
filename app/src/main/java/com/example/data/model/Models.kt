package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class TournamentMode(val label: String, val maxSlots: Long, val playersPerSlot: Long) {
    SQUAD("Squad (25×4)", 25L, 4L),
    DUO("Duo (50×2)", 50L, 2L),
    SOLO("Solo (100×1)", 100L, 1L);

    companion object {
        fun fromRaw(raw: String): TournamentMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SQUAD
    }
}

enum class TournamentStatus(val label: String) {
    UPCOMING("UPCOMING"),
    LIVE("LIVE"),
    COMPLETED("COMPLETED");

    companion object {
        fun fromRaw(raw: String): TournamentStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: UPCOMING
    }
}

enum class WalletTxType(val label: String) {
    DEPOSIT("UPI Deposit"),
    WITHDRAWAL("UPI Withdrawal"),
    ENTRY_FEE("Match Entry"),
    PRIZE("Prize Credit");

    companion object {
        fun fromRaw(raw: String): WalletTxType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DEPOSIT
    }
}

enum class WalletTxStatus(val label: String) {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED");

    companion object {
        fun fromRaw(raw: String): WalletTxStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val bgmiIgn: String = "",
    val bgmiUid: String = "",
    val depositBalance: Long = 0L,
    val winningBalance: Long = 0L,
    val matchesPlayed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val totalBalance: Long
        get() = depositBalance + winningBalance
}

@Keep
@IgnoreExtraProperties
data class PlatformConfig(
    @DocumentId val id: String = "",
    val announcementText: String = "Welcome to Naxora eSports Arena • Custom 25-Slot BGMI Lobbies Live Daily!",
    val adminUpiId: String = "naxora.esports@upi",
    val adminUpiName: String = "Amreshwar Maravi (Naxora)",
    val whatsappSupportUrl: String = "https://wa.me/919999999999",
    val youtubeUrl: String = "https://www.youtube.com/@Itachiplays",
    val instagramUrl: String = "https://www.instagram.com/ig.amreshwar/",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminPortalConfig(
    @DocumentId val id: String = "",
    val adminEmail: String = "",
    val recoveryEmail: String = "",
    val passcodeHash: String = "",
    val recoveryHint: String = "",
    val updatedByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminRecoveryLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val recoveryEmail: String = "",
    val eventType: String = "PASSWORD_RESET",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val title: String = "",
    val mapName: String = "Erangel",
    val mode: String = "SQUAD",
    val maxSlots: Long = 25L,
    val playersPerSlot: Long = 4L,
    val bookedSlotNumbers: List<Long> = emptyList(),
    val entryFee: Long = 0L,
    val prizePool: Long = 0L,
    val perKillPrize: Long = 0L,
    val startTimeMillis: Long = System.currentTimeMillis() + 3600_000L,
    val status: String = "UPCOMING",
    val roomPublished: Boolean = false,
    val roomForceVisible: Boolean = false,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val modeEnum: TournamentMode get() = TournamentMode.fromRaw(mode)
    val statusEnum: TournamentStatus get() = TournamentStatus.fromRaw(status)
    val totalPlayerCapacity: Long get() = maxSlots * playersPerSlot
    val bookedPlayerCount: Long get() = bookedSlotNumbers.size * playersPerSlot
}

@Keep
@IgnoreExtraProperties
data class RoomAccess(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val roomPassword: String = "",
    val roomPublished: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TournamentRegistration(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tournamentId: String = "",
    val slotNumber: Long = 1L,
    val teamName: String = "",
    val playerIgns: List<String> = emptyList(),
    val playerUids: List<String> = emptyList(),
    val entryFeePaid: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val type: String = "DEPOSIT",
    val amount: Long = 0L,
    val status: String = "PENDING",
    val utrNumber: String = "",
    val upiId: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val typeEnum: WalletTxType get() = WalletTxType.fromRaw(type)
    val statusEnum: WalletTxStatus get() = WalletTxStatus.fromRaw(status)
}

data class SquadPlayerInput(
    val ign: String = "",
    val uid: String = ""
)

/**
 * Parses a pasted multi-line squad IGN/UID list into up to [maxPlayers] entries.
 * Supports delimiters like "-", ":", "|", ",", or whitespace before a 6-15 digit BGMI UID.
 */
fun parseSquadRosterText(rawText: String, maxPlayers: Int): List<SquadPlayerInput> {
    val lines = rawText.lines()
        .map { it.trim().replaceFirst(Regex("^\\d+[.)\\]]\\s*"), "") }
        .filter { it.isNotBlank() }
        .take(maxPlayers)

    val parsed = lines.map { line ->
        val uidMatch = Regex("(\\d{6,15})").findAll(line).lastOrNull()
        if (uidMatch != null) {
            val uid = uidMatch.value
            val ignPart = line.removeRange(uidMatch.range)
                .replace(Regex("[-:|,/]+"), " ")
                .replace(Regex("\\s*(IGN|UID|ID)\\s*", RegexOption.IGNORE_CASE), " ")
                .trim()
                .ifBlank { "Player" }
            SquadPlayerInput(ign = ignPart.take(36), uid = uid)
        } else {
            SquadPlayerInput(ign = line.take(36), uid = "")
        }
    }

    return List(maxPlayers) { idx ->
        parsed.getOrElse(idx) { SquadPlayerInput() }
    }
}
