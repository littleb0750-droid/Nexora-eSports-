package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminPortalConfig
import com.example.data.model.AdminRecoveryLog
import com.example.data.model.PlatformConfig
import com.example.data.model.RoomAccess
import com.example.data.model.Tournament
import com.example.data.model.TournamentMode
import com.example.data.model.TournamentRegistration
import com.example.data.model.TournamentStatus
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.repository.NaxoraRepository
import com.example.data.repository.sha256Hex
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DashboardTab(val label: String) {
    ARENA("Arena"),
    WALLET("Wallet"),
    PROFILE("Profile")
}

class NaxoraViewModel(
    private val repository: NaxoraRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(DashboardTab.ARENA)
    val selectedTab: StateFlow<DashboardTab> = _selectedTab.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isActionBusy = MutableStateFlow(false)
    val isActionBusy: StateFlow<Boolean> = _isActionBusy.asStateFlow()

    // Secret Admin Portal (5-7 tap trigger)
    private val _showAdminPortalModal = MutableStateFlow(false)
    val showAdminPortalModal: StateFlow<Boolean> = _showAdminPortalModal.asStateFlow()

    private val _isAdminSessionUnlocked = MutableStateFlow(false)
    val isAdminSessionUnlocked: StateFlow<Boolean> = _isAdminSessionUnlocked.asStateFlow()

    private val _isAuthorizedAdmin = MutableStateFlow(false)
    val isAuthorizedAdmin: StateFlow<Boolean> = _isAuthorizedAdmin.asStateFlow()

    private val _adminPortalConfig = MutableStateFlow<AdminPortalConfig?>(null)
    val adminPortalConfig: StateFlow<AdminPortalConfig?> = _adminPortalConfig.asStateFlow()

    private val _recoveryVerifiedEmail = MutableStateFlow<String?>(null)
    val recoveryVerifiedEmail: StateFlow<String?> = _recoveryVerifiedEmail.asStateFlow()

    private val _recoveryError = MutableStateFlow<String?>(null)
    val recoveryError: StateFlow<String?> = _recoveryError.asStateFlow()

    // Cached Room Access per tournament ID
    private val _roomAccessByTournament = MutableStateFlow<Map<String, RoomAccess>>(emptyMap())
    val roomAccessByTournament: StateFlow<Map<String, RoomAccess>> = _roomAccessByTournament.asStateFlow()

    // Admin inspected tournament ID for 25-slot view
    private val _inspectedTournamentId = MutableStateFlow<String?>(null)
    val inspectedTournamentId: StateFlow<String?> = _inspectedTournamentId.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId)
            .toUiStateFlow("Failed to load player profile")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val platformConfig: StateFlow<PlatformConfig> =
        repository.observePlatformConfig()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PlatformConfig())

    val tournamentsState: StateFlow<UiState<List<Tournament>>> =
        repository.observeTournaments()
            .toUiStateFlow("Failed to load tournaments")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val userTransactionsState: StateFlow<UiState<List<WalletTransaction>>> =
        repository.observeUserTransactions(currentUserId)
            .toUiStateFlow("Failed to load wallet transactions")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    // Joined registration map keyed by tournamentId for current user
    private val _myRegistrations = MutableStateFlow<Map<String, TournamentRegistration>>(emptyMap())
    val myRegistrations: StateFlow<Map<String, TournamentRegistration>> = _myRegistrations.asStateFlow()

    // Admin-only streams gated behind _isAdminSessionUnlocked
    @OptIn(ExperimentalCoroutinesApi::class)
    val adminAllTransactions: StateFlow<List<WalletTransaction>> =
        _isAdminSessionUnlocked.flatMapLatest { unlocked ->
            if (!unlocked) flowOf(emptyList())
            else repository.observeAllTransactions().catch { emit(emptyList()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminAllUsers: StateFlow<List<UserProfile>> =
        _isAdminSessionUnlocked.flatMapLatest { unlocked ->
            if (!unlocked) flowOf(emptyList())
            else repository.observeAllUsers().catch { emit(emptyList()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminInspectedRegistrations: StateFlow<List<TournamentRegistration>> =
        _inspectedTournamentId.flatMapLatest { tid ->
            if (tid.isNullOrBlank() || !_isAdminSessionUnlocked.value) flowOf(emptyList())
            else repository.observeAllRegistrations(tid).catch { emit(emptyList()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminRecoveryLogs: StateFlow<List<AdminRecoveryLog>> =
        _isAdminSessionUnlocked.flatMapLatest { unlocked ->
            if (!unlocked) flowOf(emptyList())
            else repository.observeAdminRecoveryLogs().catch { emit(emptyList()) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private var tapCount = 0
    private var lastTapMillis = 0L

    init {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            if (user != null) {
                repository.ensureUserProfile(
                    firebaseUser = user,
                    pendingName = "",
                    pendingPhone = "",
                    pendingBgmiIgn = "",
                    pendingBgmiUid = ""
                )
                val isAdmin = repository.checkIsAdminAccount(user)
                _isAuthorizedAdmin.value = isAdmin
                if (isAdmin) {
                    _adminPortalConfig.value = repository.fetchAdminPortalConfig().getOrNull()
                }
            }
        }
    }

    fun selectTab(tab: DashboardTab) {
        _selectedTab.value = tab
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun applyPendingSignupProfile(
        displayName: String,
        phone: String,
        bgmiIgn: String,
        bgmiUid: String
    ) {
        val user = Firebase.auth.currentUser ?: return
        if (displayName.isBlank() && phone.isBlank() && bgmiIgn.isBlank() && bgmiUid.isBlank()) return
        viewModelScope.launch {
            repository.ensureUserProfile(
                firebaseUser = user,
                pendingName = displayName,
                pendingPhone = phone,
                pendingBgmiIgn = bgmiIgn,
                pendingBgmiUid = bgmiUid
            )
        }
    }

    fun showMessage(msg: String) {
        _actionMessage.value = msg
    }

    /**
     * Secret Admin Trigger: Tapping "Naxora eSports" 5 to 7 times within 3 seconds
     * opens the hidden "Naxora Admin — Master Control Portal" modal.
     */
    fun onBrandTitleTapped() {
        val now = System.currentTimeMillis()
        if (now - lastTapMillis > 3000L) {
            tapCount = 1
        } else {
            tapCount++
        }
        lastTapMillis = now
        if (tapCount >= 5) {
            tapCount = 0
            openSecretAdminPortal()
        }
    }

    fun openSecretAdminPortal() {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            val isAdmin = repository.checkIsAdminAccount(user)
            _isAuthorizedAdmin.value = isAdmin
            if (isAdmin) {
                _adminPortalConfig.value = repository.fetchAdminPortalConfig().getOrNull()
            }
            _showAdminPortalModal.value = true
        }
    }

    fun dismissSecretAdminPortal() {
        _showAdminPortalModal.value = false
        clearRecoveryState()
    }

    fun clearRecoveryState() {
        _recoveryVerifiedEmail.value = null
        _recoveryError.value = null
    }

    fun refreshAdminStatusAfterGoogleReauth() {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            val isAdmin = repository.checkIsAdminAccount(user)
            _isAuthorizedAdmin.value = isAdmin
            if (isAdmin) {
                _adminPortalConfig.value = repository.fetchAdminPortalConfig().getOrNull()
                _recoveryError.value = null
            }
        }
    }

    fun verifyAdminRecoveryEmail(enteredEmail: String) {
        viewModelScope.launch {
            _recoveryError.value = null
            _isActionBusy.value = true
            val res = repository.verifyRegisteredRecoveryEmail(enteredEmail)
            _isActionBusy.value = false
            if (res.isSuccess) {
                _recoveryVerifiedEmail.value = res.getOrNull()
                _actionMessage.value = "Recovery email verified via Firebase."
            } else {
                _recoveryVerifiedEmail.value = null
                _recoveryError.value = res.exceptionOrNull()?.localizedMessage ?: "Verification failed."
            }
        }
    }

    fun attemptAdminPortalLogin(email: String, password: String) {
        viewModelScope.launch {
            if (!_isAuthorizedAdmin.value) {
                _actionMessage.value = "Access denied: Current Google account does not hold Master Admin privileges."
                return@launch
            }
            val cleanEmail = email.trim()
            val cleanPass = password.trim()
            if (cleanEmail.isBlank() || cleanPass.length < 4) {
                _actionMessage.value = "Enter Admin Email and Password (min 4 chars)."
                return@launch
            }

            val existingConfig = _adminPortalConfig.value
            if (existingConfig == null) {
                // First-time bootstrap by verified Founder Admin: save initial credentials + recovery email
                val fallbackRecovery = Firebase.auth.currentUser?.email ?: cleanEmail
                _isActionBusy.value = true
                val res = repository.saveAdminPortalCredentials(
                    adminEmail = cleanEmail,
                    recoveryEmail = fallbackRecovery,
                    newPasscode = cleanPass,
                    recoveryHint = "Master Founder Key"
                )
                _isActionBusy.value = false
                if (res.isSuccess) {
                    _adminPortalConfig.value = repository.fetchAdminPortalConfig().getOrNull()
                    _isAdminSessionUnlocked.value = true
                    _showAdminPortalModal.value = false
                    _actionMessage.value = "Master Control Portal initialized & unlocked."
                } else {
                    _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to initialize portal."
                }
            } else {
                val inputHash = sha256Hex(cleanPass)
                if (existingConfig.adminEmail.equals(cleanEmail, ignoreCase = true) &&
                    existingConfig.passcodeHash == inputHash
                ) {
                    _isAdminSessionUnlocked.value = true
                    _showAdminPortalModal.value = false
                    _actionMessage.value = "Welcome to Naxora Master Control Portal."
                } else {
                    _actionMessage.value = "Invalid Admin ID or Password."
                }
            }
        }
    }

    fun recoverOrResetAdminCredentials(
        newAdminEmail: String,
        newRecoveryEmail: String,
        newPassword: String,
        recoveryHint: String,
        onResetSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!_isAuthorizedAdmin.value) {
                _actionMessage.value = "Admin Recovery is restricted exclusively to verified Admin accounts."
                return@launch
            }
            _recoveryError.value = null
            _isActionBusy.value = true
            val res = repository.saveAdminPortalCredentials(
                adminEmail = newAdminEmail,
                recoveryEmail = newRecoveryEmail,
                newPasscode = newPassword,
                recoveryHint = recoveryHint
            )
            _isActionBusy.value = false
            if (res.isSuccess) {
                _adminPortalConfig.value = repository.fetchAdminPortalConfig().getOrNull()
                _recoveryVerifiedEmail.value = null
                _actionMessage.value = "Admin credentials reset & audited in Firebase!"
                onResetSuccess()
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "Recovery failed."
                _recoveryError.value = err
                _actionMessage.value = err
            }
        }
    }

    fun lockAdminDashboard() {
        _isAdminSessionUnlocked.value = false
        _inspectedTournamentId.value = null
    }

    fun observeRegistrationForTournament(tournamentId: String) {
        if (_myRegistrations.value.containsKey(tournamentId)) return
        viewModelScope.launch {
            repository.observeMyRegistration(tournamentId, currentUserId).collect { reg ->
                val updated = _myRegistrations.value.toMutableMap()
                if (reg != null) {
                    updated[tournamentId] = reg
                } else {
                    updated.remove(tournamentId)
                }
                _myRegistrations.value = updated
            }
        }
    }

    fun loadRoomAccessIfJoined(tournament: Tournament) {
        val joined = _myRegistrations.value.containsKey(tournament.id)
        if (!joined && !_isAdminSessionUnlocked.value) return
        if (!tournament.roomPublished && !_isAdminSessionUnlocked.value) return
        viewModelScope.launch {
            val res = repository.fetchRoomAccess(tournament.id)
            val access = res.getOrNull()
            if (access != null) {
                _roomAccessByTournament.value = _roomAccessByTournament.value + (tournament.id to access)
            }
        }
    }

    fun joinTournamentSlot(
        tournament: Tournament,
        userProfile: UserProfile,
        slotNumber: Long,
        teamName: String,
        playerIgns: List<String>,
        playerUids: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val res = repository.joinTournamentSlot(
                tournament = tournament,
                userProfile = userProfile,
                slotNumber = slotNumber,
                teamName = teamName,
                playerIgns = playerIgns,
                playerUids = playerUids
            )
            _isActionBusy.value = false
            if (res.isSuccess) {
                _actionMessage.value = "Slot #$slotNumber confirmed for $teamName!"
                onSuccess()
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Could not join slot."
            }
        }
    }

    fun updateProfile(displayName: String, phone: String, bgmiIgn: String, bgmiUid: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val res = repository.updateUserProfile(currentUserId, displayName, phone, bgmiIgn, bgmiUid)
            _isActionBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                "Fighter profile updated!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Profile update failed."
            }
        }
    }

    fun submitDeposit(userProfile: UserProfile, amountText: String, utr: String, onSuccess: () -> Unit) {
        val amount = amountText.trim().toLongOrNull()
        if (amount == null || amount < 10L) {
            _actionMessage.value = "Enter a valid deposit amount (min ₹10)."
            return
        }
        viewModelScope.launch {
            _isActionBusy.value = true
            val res = repository.submitDepositRequest(userProfile, amount, utr)
            _isActionBusy.value = false
            if (res.isSuccess) {
                _actionMessage.value = "Deposit request (₹$amount) submitted! Awaiting Admin UTR verification."
                onSuccess()
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Deposit failed."
            }
        }
    }

    fun submitWithdrawal(userProfile: UserProfile, amountText: String, upiId: String, onSuccess: () -> Unit) {
        val amount = amountText.trim().toLongOrNull()
        if (amount == null || amount < 20L) {
            _actionMessage.value = "Minimum withdrawal amount is ₹20."
            return
        }
        viewModelScope.launch {
            _isActionBusy.value = true
            val res = repository.submitWithdrawalRequest(userProfile, amount, upiId)
            _isActionBusy.value = false
            if (res.isSuccess) {
                _actionMessage.value = "Withdrawal (₹$amount) queued for UPI payout."
                onSuccess()
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Withdrawal failed."
            }
        }
    }

    // --- Admin Actions ---

    fun inspectTournamentSlots(tournamentId: String?) {
        _inspectedTournamentId.value = tournamentId
        if (!tournamentId.isNullOrBlank()) {
            viewModelScope.launch {
                val res = repository.fetchRoomAccess(tournamentId)
                val access = res.getOrNull()
                if (access != null) {
                    _roomAccessByTournament.value = _roomAccessByTournament.value + (tournamentId to access)
                }
            }
        }
    }

    fun saveTournament(
        existingId: String?,
        title: String,
        mapName: String,
        mode: TournamentMode,
        entryFee: Long,
        prizePool: Long,
        perKillPrize: Long,
        startTimeMillis: Long,
        status: TournamentStatus,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val res = repository.createOrUpdateTournament(
                existingId = existingId,
                title = title,
                mapName = mapName,
                mode = mode,
                entryFee = entryFee,
                prizePool = prizePool,
                perKillPrize = perKillPrize,
                startTimeMillis = startTimeMillis,
                status = status
            )
            _isActionBusy.value = false
            if (res.isSuccess) {
                _actionMessage.value = if (existingId == null) "Tournament created!" else "Tournament updated!"
                onSuccess()
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to save tournament."
            }
        }
    }

    fun publishRoom(
        tournamentId: String,
        roomId: String,
        roomPassword: String,
        roomPublished: Boolean,
        roomForceVisible: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val res = repository.publishRoomCredentials(
                tournamentId = tournamentId,
                roomId = roomId,
                roomPassword = roomPassword,
                roomPublished = roomPublished,
                roomForceVisible = roomForceVisible
            )
            _isActionBusy.value = false
            if (res.isSuccess) {
                val latest = repository.fetchRoomAccess(tournamentId).getOrNull()
                if (latest != null) {
                    _roomAccessByTournament.value = _roomAccessByTournament.value + (tournamentId to latest)
                }
                _actionMessage.value = if (roomPublished) "Room ID & Password broadcast live!" else "Room credentials hidden."
                onSuccess()
            } else {
                _actionMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to publish room."
            }
        }
    }

    fun deleteTournament(tournamentId: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val res = repository.deleteTournament(tournamentId)
            _isActionBusy.value = false
            _actionMessage.value = if (res.isSuccess) "Tournament deleted." else "Could not delete tournament."
        }
    }

    fun kickRegistration(tournamentId: String, registration: TournamentRegistration) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val res = repository.kickSlotRegistration(tournamentId, registration)
            _isActionBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                "Removed Slot #${registration.slotNumber} (${registration.teamName})."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to remove slot."
            }
        }
    }

    fun resolveTransaction(tx: WalletTransaction, approve: Boolean, note: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val res = repository.resolveWalletTransaction(tx, approve, note)
            _isActionBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                if (approve) "Transaction approved." else "Transaction rejected."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update transaction."
            }
        }
    }

    fun creditPrize(targetUser: UserProfile, amountText: String, reason: String) {
        val amount = amountText.trim().toLongOrNull()
        if (amount == null || amount < 1L) {
            _actionMessage.value = "Enter a valid prize amount."
            return
        }
        viewModelScope.launch {
            _isActionBusy.value = true
            val res = repository.creditUserPrize(targetUser, amount, reason)
            _isActionBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                "Credited ₹$amount winnings to ${targetUser.displayName}."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to credit winnings."
            }
        }
    }

    fun savePlatformConfig(
        announcementText: String,
        adminUpiId: String,
        adminUpiName: String,
        whatsappUrl: String,
        youtubeUrl: String,
        instagramUrl: String
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            val res = repository.savePlatformConfig(
                announcementText, adminUpiId, adminUpiName, whatsappUrl, youtubeUrl, instagramUrl
            )
            _isActionBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                "Platform config & live banner updated!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update config."
            }
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(defaultErrorMsg: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(UiState.Error(rootError.message ?: defaultErrorMsg))
            }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
