package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Tournament
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.signOutUser
import com.example.ui.theme.CyanButtonBg
import com.example.ui.theme.CyanButtonBorder
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianArenaBackground
import com.example.ui.theme.ObsidianCardSolid
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianInset
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DashboardTab
import com.example.ui.viewmodel.NaxoraViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun UserDashboardScreen(
    viewModel: NaxoraViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val platformConfig by viewModel.platformConfig.collectAsStateWithLifecycle()
    val tournamentsState by viewModel.tournamentsState.collectAsStateWithLifecycle()
    val userTransactionsState by viewModel.userTransactionsState.collectAsStateWithLifecycle()
    val myRegistrations by viewModel.myRegistrations.collectAsStateWithLifecycle()
    val roomAccessMap by viewModel.roomAccessByTournament.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isActionBusy.collectAsStateWithLifecycle()

    val showAdminPortalModal by viewModel.showAdminPortalModal.collectAsStateWithLifecycle()
    val isAdminSessionUnlocked by viewModel.isAdminSessionUnlocked.collectAsStateWithLifecycle()
    val isAuthorizedAdmin by viewModel.isAuthorizedAdmin.collectAsStateWithLifecycle()
    val adminPortalConfig by viewModel.adminPortalConfig.collectAsStateWithLifecycle()
    val recoveryVerifiedEmail by viewModel.recoveryVerifiedEmail.collectAsStateWithLifecycle()
    val recoveryError by viewModel.recoveryError.collectAsStateWithLifecycle()

    var activeJoinTournament by remember { mutableStateOf<Tournament?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val currentUserProfile = (profileState as? UiState.Success)?.data ?: UserProfile(
        userId = Firebase.auth.currentUser?.uid ?: "",
        displayName = Firebase.auth.currentUser?.displayName ?: "Fighter",
        email = Firebase.auth.currentUser?.email ?: ""
    )

    if (isAdminSessionUnlocked) {
        AdminDashboardScreen(
            viewModel = viewModel,
            snackbarHostState = snackbarHostState
        )
        return
    }

    ObsidianArenaBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 680.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = ObsidianCardSolid.copy(alpha = 0.94f),
                            tonalElevation = 0.dp,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            DashboardTab.entries.forEach { tab ->
                                val icon = when (tab) {
                                    DashboardTab.ARENA -> Icons.Rounded.SportsEsports
                                    DashboardTab.WALLET -> Icons.Rounded.AccountBalanceWallet
                                    DashboardTab.PROFILE -> Icons.Rounded.Person
                                }
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonCyan,
                                        selectedTextColor = NeonCyan,
                                        indicatorColor = CyanButtonBg,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = ObsidianCardSolid.copy(alpha = 0.94f),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            DashboardTab.entries.forEach { tab ->
                                val icon = when (tab) {
                                    DashboardTab.ARENA -> Icons.Rounded.SportsEsports
                                    DashboardTab.WALLET -> Icons.Rounded.AccountBalanceWallet
                                    DashboardTab.PROFILE -> Icons.Rounded.Person
                                }
                                NavigationRailItem(
                                    selected = selectedTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = NeonCyan,
                                        selectedTextColor = NeonCyan,
                                        indicatorColor = CyanButtonBg,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Bar with "Naxora eSports" 5-7 tap secret trigger + Wallet Pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_brand_trigger")
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { viewModel.onBrandTitleTapped() }
                            ) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Custom BGMI Arena • ${currentUserProfile.bgmiIgn.ifBlank { currentUserProfile.displayName }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(CyanButtonBg)
                                    .border(1.dp, CyanButtonBorder, RoundedCornerShape(50))
                                    .testTag("top_wallet_pill")
                                    .clickable { viewModel.selectTab(DashboardTab.WALLET) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.AccountBalanceWallet, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "₹${currentUserProfile.totalBalance}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NeonCyan
                                )
                            }
                        }

                        // Live Announcement Banner + Quick Stats on Arena Tab
                        if (selectedTab == DashboardTab.ARENA) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ObsidianInset)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Campaign, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = platformConfig.announcementText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val totalLobbies = (tournamentsState as? UiState.Success)?.data?.size ?: 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                QuickStatPill("Live Lobbies", "$totalLobbies", Modifier.weight(1f))
                                QuickStatPill("Joined", "${myRegistrations.size}", Modifier.weight(1f))
                                QuickStatPill("Matches", "${currentUserProfile.matchesPlayed}", Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Main Tab Body
                        Box(modifier = Modifier.weight(1f)) {
                            when (selectedTab) {
                                DashboardTab.ARENA -> TournamentsSection(
                                    tournamentsState = tournamentsState,
                                    myRegistrations = myRegistrations,
                                    roomAccessByTournament = roomAccessMap,
                                    onObserveRegistration = { tid -> viewModel.observeRegistrationForTournament(tid) },
                                    onRequestRoomAccess = { t -> viewModel.loadRoomAccessIfJoined(t) },
                                    onOpenJoinModal = { t -> activeJoinTournament = t },
                                    onCopiedNotice = { msg -> viewModel.showMessage(msg) }
                                )
                                DashboardTab.WALLET -> WalletSection(
                                    userProfile = currentUserProfile,
                                    platformConfig = platformConfig,
                                    transactionsState = userTransactionsState,
                                    isBusy = isBusy,
                                    onSubmitDeposit = { amt, utr, clear ->
                                        viewModel.submitDeposit(currentUserProfile, amt, utr, clear)
                                    },
                                    onSubmitWithdrawal = { amt, upi, clear ->
                                        viewModel.submitWithdrawal(currentUserProfile, amt, upi, clear)
                                    },
                                    onCopiedNotice = { msg -> viewModel.showMessage(msg) }
                                )
                                DashboardTab.PROFILE -> ProfileSection(
                                    userProfile = currentUserProfile,
                                    platformConfig = platformConfig,
                                    isBusy = isBusy,
                                    onSaveProfile = { name, phone, ign, uid ->
                                        viewModel.updateProfile(name, phone, ign, uid)
                                    },
                                    onSignOutConfirmed = {
                                        signOutUser(credentialManager, scope)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Join / 25-Slot Grid Modal
        val joinTarget = activeJoinTournament
        if (joinTarget != null) {
            JoinTournamentSheet(
                tournament = joinTarget,
                userProfile = currentUserProfile,
                existingRegistration = myRegistrations[joinTarget.id],
                isBusy = isBusy,
                onConfirmJoin = { slot, team, igns, uids ->
                    viewModel.joinTournamentSlot(
                        tournament = joinTarget,
                        userProfile = currentUserProfile,
                        slotNumber = slot,
                        teamName = team,
                        playerIgns = igns,
                        playerUids = uids,
                        onSuccess = { activeJoinTournament = null }
                    )
                },
                onDismiss = { activeJoinTournament = null }
            )
        }

        // Secret Admin Portal Modal (Unlocked via 5-7 taps on "Naxora eSports")
        if (showAdminPortalModal) {
            SecretAdminPortalModal(
                isAuthorizedAdmin = isAuthorizedAdmin,
                adminPortalConfig = adminPortalConfig,
                verifiedRecoveryEmail = recoveryVerifiedEmail,
                recoveryError = recoveryError,
                isBusy = isBusy,
                defaultEmail = Firebase.auth.currentUser?.email ?: "",
                onLoginSubmit = { email, pass -> viewModel.attemptAdminPortalLogin(email, pass) },
                onVerifyRecoveryEmail = { email -> viewModel.verifyAdminRecoveryEmail(email) },
                onReauthGoogleAccount = { viewModel.refreshAdminStatusAfterGoogleReauth() },
                onRecoverCredentials = { adminEmail, recoveryEmail, pass, hint, onResetSuccess ->
                    viewModel.recoverOrResetAdminCredentials(
                        newAdminEmail = adminEmail,
                        newRecoveryEmail = recoveryEmail,
                        newPassword = pass,
                        recoveryHint = hint,
                        onResetSuccess = onResetSuccess
                    )
                },
                onClearRecoveryState = { viewModel.clearRecoveryState() },
                onDismiss = { viewModel.dismissSecretAdminPortal() }
            )
        }
    }
}

@Composable
private fun QuickStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianDeep.copy(alpha = 0.65f))
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = NeonCyan)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
