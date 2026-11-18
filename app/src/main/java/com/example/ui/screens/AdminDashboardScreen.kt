package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Tournament
import com.example.data.model.TournamentMode
import com.example.data.model.TournamentRegistration
import com.example.data.model.TournamentStatus
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WalletTxStatus
import com.example.ui.theme.AmberBg
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonBg
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.DarkInsetTextField
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonTranslucentButton
import com.example.ui.theme.ObsidianArenaBackground
import com.example.ui.theme.ObsidianCardSolid
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianInset
import com.example.ui.theme.PurpleButtonBg
import com.example.ui.theme.PurpleButtonBorder
import com.example.ui.theme.PurpleSoftText
import com.example.ui.theme.StatusPill
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.NaxoraViewModel

private enum class AdminTab(val label: String) {
    LOBBIES("Lobbies & Slots"),
    WALLET("Wallet Queue"),
    USERS_CONFIG("Players & Config")
}

@Composable
fun AdminDashboardScreen(
    viewModel: NaxoraViewModel,
    snackbarHostState: SnackbarHostState
) {
    BackHandler { viewModel.lockAdminDashboard() }

    var activeTab by remember { mutableStateOf(AdminTab.LOBBIES) }
    val tournamentsState by viewModel.tournamentsState.collectAsStateWithLifecycle()
    val allTransactions by viewModel.adminAllTransactions.collectAsStateWithLifecycle()
    val allUsers by viewModel.adminAllUsers.collectAsStateWithLifecycle()
    val inspectedTournamentId by viewModel.inspectedTournamentId.collectAsStateWithLifecycle()
    val inspectedRegistrations by viewModel.adminInspectedRegistrations.collectAsStateWithLifecycle()
    val roomAccessMap by viewModel.roomAccessByTournament.collectAsStateWithLifecycle()
    val platformConfig by viewModel.platformConfig.collectAsStateWithLifecycle()
    val adminPortalConfig by viewModel.adminPortalConfig.collectAsStateWithLifecycle()
    val recoveryLogs by viewModel.adminRecoveryLogs.collectAsStateWithLifecycle()
    val isBusy by viewModel.isActionBusy.collectAsStateWithLifecycle()

    var editingTournament by remember { mutableStateOf<Tournament?>(null) }
    var creatingNewTournament by remember { mutableStateOf(false) }
    var publishingRoomFor by remember { mutableStateOf<Tournament?>(null) }

    val tournaments = (tournamentsState as? UiState.Success)?.data ?: emptyList()

    ObsidianArenaBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = ObsidianCardSolid.copy(alpha = 0.96f),
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    AdminTab.entries.forEach { tab ->
                        val icon = when (tab) {
                            AdminTab.LOBBIES -> Icons.Rounded.Groups
                            AdminTab.WALLET -> Icons.Rounded.Payments
                            AdminTab.USERS_CONFIG -> Icons.Rounded.Settings
                        }
                        NavigationBarItem(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            icon = { Icon(icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonPurple,
                                selectedTextColor = NeonPurple,
                                indicatorColor = PurpleButtonBg,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Admin Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AdminPanelSettings, null, tint = NeonPurple, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Naxora Admin", style = MaterialTheme.typography.titleLarge, color = NeonPurple)
                            Text("Master Control Portal", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(PurpleButtonBg)
                            .border(1.dp, PurpleButtonBorder, RoundedCornerShape(50))
                            .testTag("exit_admin_portal_button")
                            .clickable { viewModel.lockAdminDashboard() }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Lock, null, tint = PurpleSoftText, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Exit Admin", style = MaterialTheme.typography.labelMedium, color = PurpleSoftText)
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        AdminTab.LOBBIES -> {
                            val inspectedTournament = tournaments.firstOrNull { it.id == inspectedTournamentId }
                            if (inspectedTournament != null) {
                                AdminSlotInspectorView(
                                    tournament = inspectedTournament,
                                    registrations = inspectedRegistrations,
                                    onBack = { viewModel.inspectTournamentSlots(null) },
                                    onKickSlot = { reg -> viewModel.kickRegistration(inspectedTournament.id, reg) },
                                    onCopied = { msg -> viewModel.showMessage(msg) }
                                )
                            } else {
                                AdminLobbiesTab(
                                    tournaments = tournaments,
                                    onCreateNew = { creatingNewTournament = true },
                                    onEdit = { editingTournament = it },
                                    onPublishRoom = {
                                        viewModel.inspectTournamentSlots(it.id)
                                        publishingRoomFor = it
                                    },
                                    onInspectSlots = { viewModel.inspectTournamentSlots(it.id) },
                                    onDelete = { viewModel.deleteTournament(it.id) }
                                )
                            }
                        }
                        AdminTab.WALLET -> AdminWalletTab(
                            transactions = allTransactions,
                            isBusy = isBusy,
                            onResolve = { tx, approve, note -> viewModel.resolveTransaction(tx, approve, note) }
                        )
                        AdminTab.USERS_CONFIG -> AdminUsersAndConfigTab(
                            users = allUsers,
                            platformConfig = platformConfig,
                            adminPortalConfig = adminPortalConfig,
                            recoveryLogs = recoveryLogs,
                            isBusy = isBusy,
                            onSavePlatformConfig = { banner, upiId, upiName, wa, yt, ig ->
                                viewModel.savePlatformConfig(banner, upiId, upiName, wa, yt, ig)
                            },
                            onUpdateAdminCredentials = { adminEmail, recoveryEmail, newPass, hint ->
                                viewModel.recoverOrResetAdminCredentials(
                                    newAdminEmail = adminEmail,
                                    newRecoveryEmail = recoveryEmail,
                                    newPassword = newPass,
                                    recoveryHint = hint
                                )
                            },
                            onCreditPrize = { user, amt, reason -> viewModel.creditPrize(user, amt, reason) }
                        )
                    }
                }
            }
        }

        if (creatingNewTournament || editingTournament != null) {
            AdminTournamentFormDialog(
                existing = editingTournament,
                isBusy = isBusy,
                onSave = { title, map, mode, fee, prize, killPrize, startMs, status ->
                    viewModel.saveTournament(
                        existingId = editingTournament?.id,
                        title = title,
                        mapName = map,
                        mode = mode,
                        entryFee = fee,
                        prizePool = prize,
                        perKillPrize = killPrize,
                        startTimeMillis = startMs,
                        status = status,
                        onSuccess = {
                            creatingNewTournament = false
                            editingTournament = null
                        }
                    )
                },
                onDismiss = {
                    creatingNewTournament = false
                    editingTournament = null
                }
            )
        }

        val roomTarget = publishingRoomFor
        if (roomTarget != null) {
            AdminPublishRoomDialog(
                tournament = roomTarget,
                currentAccess = roomAccessMap[roomTarget.id],
                isBusy = isBusy,
                onPublish = { rId, rPass, published, forceVisible ->
                    viewModel.publishRoom(
                        tournamentId = roomTarget.id,
                        roomId = rId,
                        roomPassword = rPass,
                        roomPublished = published,
                        roomForceVisible = forceVisible,
                        onSuccess = {
                            publishingRoomFor = null
                            viewModel.inspectTournamentSlots(null)
                        }
                    )
                },
                onDismiss = {
                    publishingRoomFor = null
                    viewModel.inspectTournamentSlots(null)
                }
            )
        }
    }
}

@Composable
private fun AdminLobbiesTab(
    tournaments: List<Tournament>,
    onCreateNew: () -> Unit,
    onEdit: (Tournament) -> Unit,
    onPublishRoom: (Tournament) -> Unit,
    onInspectSlots: (Tournament) -> Unit,
    onDelete: (Tournament) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NeonTranslucentButton(
                text = "Create New Custom Match",
                isAdminAccent = true,
                tag = "admin_create_tournament_button",
                leadingIcon = { Icon(Icons.Rounded.Add, null, tint = PurpleSoftText) },
                onClick = onCreateNew
            )
        }

        items(tournaments, key = { it.id }) { t ->
            FrostedGlassCard(
                contentPadding = PaddingValues(16.dp),
                borderColor = PurpleButtonBorder.copy(alpha = 0.35f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                "${t.mapName} • ${t.modeEnum.label} • ${t.bookedSlotNumbers.size}/${t.maxSlots} Slots",
                                style = MaterialTheme.typography.bodySmall,
                                color = PurpleSoftText
                            )
                        }
                        StatusPill(
                            text = if (t.roomPublished) "ROOM LIVE" else t.statusEnum.label,
                            color = if (t.roomPublished) EmeraldWin else NeonPurple,
                            bgColor = if (t.roomPublished) EmeraldBg else PurpleButtonBg
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallAdminButton("Edit", Icons.Rounded.Edit, Modifier.weight(1f)) { onEdit(t) }
                        SmallAdminButton("Room ID/Pass", Icons.Rounded.Key, Modifier.weight(1.3f)) { onPublishRoom(t) }
                        SmallAdminButton("Slots (${t.bookedSlotNumbers.size})", Icons.Rounded.Groups, Modifier.weight(1.2f)) { onInspectSlots(t) }
                        IconButton(onClick = { onDelete(t) }) {
                            Icon(Icons.Rounded.Delete, "Delete tournament", tint = CrimsonDanger)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallAdminButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = ObsidianInset,
        border = BorderStroke(1.dp, PurpleButtonBorder.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = PurpleSoftText, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = PurpleSoftText)
        }
    }
}

@Composable
private fun AdminSlotInspectorView(
    tournament: Tournament,
    registrations: List<TournamentRegistration>,
    onBack: () -> Unit,
    onKickSlot: (TournamentRegistration) -> Unit,
    onCopied: (String) -> Unit
) {
    BackHandler { onBack() }
    val clipboard = LocalClipboardManager.current
    val regBySlot = remember(registrations) { registrations.associateBy { it.slotNumber } }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = PurpleSoftText)
                }
                Column {
                    Text("${tournament.title} — Slot Inspector", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("${registrations.size}/${tournament.maxSlots} Booked Slots • Verify IGN/UID", style = MaterialTheme.typography.bodySmall, color = PurpleSoftText)
                }
            }
        }

        items((1L..tournament.maxSlots).toList(), key = { it }) { slot ->
            val reg = regBySlot[slot]
            FrostedGlassCard(
                contentPadding = PaddingValues(14.dp),
                cornerRadius = 16.dp,
                borderColor = if (reg != null) PurpleButtonBorder.copy(alpha = 0.5f) else GlassBorder
            ) {
                if (reg == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Slot #$slot", style = MaterialTheme.typography.titleSmall, color = TextMuted)
                        Text("AVAILABLE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Slot #$slot • ${reg.teamName}", style = MaterialTheme.typography.titleSmall, color = NeonCyan)
                            Row {
                                IconButton(
                                    onClick = {
                                        val rosterText = reg.playerIgns.indices.joinToString("\n") { idx ->
                                            "${reg.playerIgns[idx]} - ${reg.playerUids.getOrElse(idx) { "" }}"
                                        }
                                        clipboard.setText(AnnotatedString("Slot #$slot (${reg.teamName})\n$rosterText"))
                                        onCopied("Copied Slot #$slot roster!")
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, "Copy slot roster", tint = PurpleSoftText, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onKickSlot(reg) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.Delete, "Remove team from slot", tint = CrimsonDanger, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        reg.playerIgns.forEachIndexed { idx, ign ->
                            val uid = reg.playerUids.getOrElse(idx) { "-" }
                            Text(
                                text = "P${idx + 1}: $ign   (UID: $uid)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminWalletTab(
    transactions: List<WalletTransaction>,
    isBusy: Boolean,
    onResolve: (WalletTransaction, Boolean, String) -> Unit
) {
    val pending = remember(transactions) { transactions.filter { it.statusEnum == WalletTxStatus.PENDING } }
    val history = remember(transactions) { transactions.filter { it.statusEnum != WalletTxStatus.PENDING }.take(30) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Pending Approvals (${pending.size})", style = MaterialTheme.typography.titleMedium, color = AmberGold)
        }
        if (pending.isEmpty()) {
            item {
                FrostedGlassCard(contentPadding = PaddingValues(16.dp)) {
                    Text("No pending UPI deposit or withdrawal requests.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        } else {
            items(pending, key = { it.id }) { tx ->
                FrostedGlassCard(
                    contentPadding = PaddingValues(14.dp),
                    borderColor = AmberGold.copy(alpha = 0.45f)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("${tx.typeEnum.label} • ${tx.userName}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                if (tx.utrNumber.isNotBlank()) {
                                    Text("UTR: ${tx.utrNumber}", style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                                }
                                if (tx.upiId.isNotBlank()) {
                                    Text("Pay to UPI: ${tx.upiId}", style = MaterialTheme.typography.bodySmall, color = EmeraldWin)
                                }
                            }
                            Text("₹${tx.amount}", style = MaterialTheme.typography.titleLarge, color = AmberGold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isBusy) { onResolve(tx, true, "Verified by Naxora Admin") },
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldBg,
                                border = BorderStroke(1.dp, EmeraldWin)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Check, null, tint = EmeraldWin, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Approve", style = MaterialTheme.typography.labelMedium, color = EmeraldWin)
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isBusy) { onResolve(tx, false, "Rejected by Naxora Admin") },
                                shape = RoundedCornerShape(10.dp),
                                color = CrimsonBg,
                                border = BorderStroke(1.dp, CrimsonDanger)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Close, null, tint = CrimsonDanger, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reject", style = MaterialTheme.typography.labelMedium, color = CrimsonDanger)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Recent Processed Ledger", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
        }
        items(history, key = { it.id }) { tx ->
            FrostedGlassCard(contentPadding = PaddingValues(12.dp), cornerRadius = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${tx.typeEnum.label} • ${tx.userName}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(tx.note, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₹${tx.amount}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        StatusPill(
                            text = tx.statusEnum.label,
                            color = if (tx.statusEnum == WalletTxStatus.APPROVED) EmeraldWin else CrimsonDanger,
                            bgColor = if (tx.statusEnum == WalletTxStatus.APPROVED) EmeraldBg else CrimsonBg
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUsersAndConfigTab(
    users: List<UserProfile>,
    platformConfig: com.example.data.model.PlatformConfig,
    adminPortalConfig: com.example.data.model.AdminPortalConfig?,
    recoveryLogs: List<com.example.data.model.AdminRecoveryLog>,
    isBusy: Boolean,
    onSavePlatformConfig: (String, String, String, String, String, String) -> Unit,
    onUpdateAdminCredentials: (String, String, String, String) -> Unit,
    onCreditPrize: (UserProfile, String, String) -> Unit
) {
    var banner by remember(platformConfig) { mutableStateOf(platformConfig.announcementText) }
    var upiId by remember(platformConfig) { mutableStateOf(platformConfig.adminUpiId) }
    var upiName by remember(platformConfig) { mutableStateOf(platformConfig.adminUpiName) }
    var waUrl by remember(platformConfig) { mutableStateOf(platformConfig.whatsappSupportUrl) }
    var ytUrl by remember(platformConfig) { mutableStateOf(platformConfig.youtubeUrl) }
    var igUrl by remember(platformConfig) { mutableStateOf(platformConfig.instagramUrl) }

    var cfgAdminEmail by remember(adminPortalConfig) { mutableStateOf(adminPortalConfig?.adminEmail ?: "") }
    var cfgRecoveryEmail by remember(adminPortalConfig) {
        mutableStateOf(adminPortalConfig?.recoveryEmail?.ifBlank { adminPortalConfig.adminEmail } ?: "")
    }
    var cfgNewPassword by remember { mutableStateOf("") }
    var cfgRecoveryHint by remember(adminPortalConfig) { mutableStateOf(adminPortalConfig?.recoveryHint ?: "") }

    var prizeTarget by remember { mutableStateOf<UserProfile?>(null) }
    var prizeAmount by remember { mutableStateOf("") }
    var prizeReason by remember { mutableStateOf("Match Winnings Payout") }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FrostedGlassCard(borderColor = PurpleButtonBorder.copy(alpha = 0.4f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Admin Portal Recovery Email & Security", style = MaterialTheme.typography.titleMedium, color = NeonPurple)
                    DarkInsetTextField(cfgAdminEmail, { cfgAdminEmail = it }, "admin@naxora.esports", label = "Admin Portal Email / ID", focusColor = NeonPurple)
                    DarkInsetTextField(cfgRecoveryEmail, { cfgRecoveryEmail = it }, "recovery@gmail.com", label = "Registered Recovery Email", focusColor = NeonPurple)
                    DarkInsetTextField(cfgNewPassword, { cfgNewPassword = it }, "Enter new password (min 4 chars)", label = "New Admin Password", focusColor = NeonPurple)
                    DarkInsetTextField(cfgRecoveryHint, { cfgRecoveryHint = it }, "Optional hint", label = "Recovery Hint", focusColor = NeonPurple)
                    NeonTranslucentButton(
                        text = "Update Admin Credentials",
                        isAdminAccent = true,
                        isLoading = isBusy,
                        tag = "admin_dashboard_update_credentials_button",
                        onClick = {
                            onUpdateAdminCredentials(cfgAdminEmail, cfgRecoveryEmail, cfgNewPassword, cfgRecoveryHint)
                            if (cfgNewPassword.trim().length >= 4 && cfgAdminEmail.contains("@") && cfgRecoveryEmail.contains("@")) {
                                cfgNewPassword = ""
                            }
                        }
                    )
                    if (recoveryLogs.isNotEmpty()) {
                        Text("Recent Security & Recovery Audit", style = MaterialTheme.typography.labelMedium, color = PurpleSoftText, modifier = Modifier.padding(top = 6.dp))
                        recoveryLogs.take(4).forEach { log ->
                            Text(
                                text = "• ${log.eventType}: ${log.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        item {
            FrostedGlassCard(borderColor = PurpleButtonBorder.copy(alpha = 0.4f)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Platform Banner, Admin UPI & Support", style = MaterialTheme.typography.titleMedium, color = NeonPurple)
                    DarkInsetTextField(banner, { banner = it }, "Live Announcement", label = "Live Dashboard Banner", focusColor = NeonPurple)
                    DarkInsetTextField(upiId, { upiId = it }, "admin@upi", label = "Admin UPI ID", focusColor = NeonPurple)
                    DarkInsetTextField(upiName, { upiName = it }, "Amreshwar Maravi", label = "Payee Name", focusColor = NeonPurple)
                    DarkInsetTextField(waUrl, { waUrl = it }, "https://wa.me/...", label = "WhatsApp Support URL", focusColor = NeonPurple)
                    DarkInsetTextField(ytUrl, { ytUrl = it }, "https://youtube.com/...", label = "YouTube URL", focusColor = NeonPurple)
                    DarkInsetTextField(igUrl, { igUrl = it }, "https://instagram.com/...", label = "Instagram URL", focusColor = NeonPurple)
                    NeonTranslucentButton(
                        text = "Save Platform Settings",
                        isAdminAccent = true,
                        isLoading = isBusy,
                        onClick = { onSavePlatformConfig(banner, upiId, upiName, waUrl, ytUrl, igUrl) }
                    )
                }
            }
        }

        item {
            Text("Registered Players (${users.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.padding(top = 6.dp))
        }

        items(users, key = { it.userId }) { u ->
            FrostedGlassCard(contentPadding = PaddingValues(14.dp), cornerRadius = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(u.displayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("IGN: ${u.bgmiIgn.ifBlank { "-" }} • UID: ${u.bgmiUid.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                        Text("Deposit ₹${u.depositBalance} • Winning ₹${u.winningBalance}", style = MaterialTheme.typography.labelSmall, color = EmeraldWin)
                    }
                    SmallAdminButton("Award Prize", Icons.Rounded.Payments) {
                        prizeTarget = u
                    }
                }
            }
        }
    }

    val target = prizeTarget
    if (target != null) {
        BackHandler { prizeTarget = null }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianDeep.copy(alpha = 0.88f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            FrostedGlassCard(borderColor = PurpleButtonBorder) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Credit Winning Cash to ${target.displayName}", style = MaterialTheme.typography.titleMedium, color = NeonPurple)
                    DarkInsetTextField(
                        value = prizeAmount,
                        onValueChange = { prizeAmount = it.filter { ch -> ch.isDigit() } },
                        placeholder = "Prize amount in ₹",
                        label = "Amount (₹)",
                        focusColor = NeonPurple,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    DarkInsetTextField(
                        value = prizeReason,
                        onValueChange = { prizeReason = it },
                        placeholder = "e.g., Match #1 Winner + 8 Kills",
                        label = "Payout Note",
                        focusColor = NeonPurple
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonTranslucentButton(
                            text = "Cancel",
                            modifier = Modifier.weight(1f),
                            onClick = { prizeTarget = null }
                        )
                        NeonTranslucentButton(
                            text = "Credit ₹",
                            isAdminAccent = true,
                            isLoading = isBusy,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onCreditPrize(target, prizeAmount, prizeReason)
                                prizeAmount = ""
                                prizeTarget = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTournamentFormDialog(
    existing: Tournament?,
    isBusy: Boolean,
    onSave: (String, String, TournamentMode, Long, Long, Long, Long, TournamentStatus) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    var title by remember { mutableStateOf(existing?.title ?: "Naxora Pro Scrims") }
    var mapName by remember { mutableStateOf(existing?.mapName ?: "Erangel") }
    var mode by remember { mutableStateOf(existing?.modeEnum ?: TournamentMode.SQUAD) }
    var entryFee by remember { mutableStateOf((existing?.entryFee ?: 40L).toString()) }
    var prizePool by remember { mutableStateOf((existing?.prizePool ?: 800L).toString()) }
    var perKill by remember { mutableStateOf((existing?.perKillPrize ?: 15L).toString()) }
    var hoursFromNow by remember { mutableStateOf("2") }
    var status by remember { mutableStateOf(existing?.statusEnum ?: TournamentStatus.UPCOMING) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep.copy(alpha = 0.9f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        FrostedGlassCard(borderColor = PurpleButtonBorder) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if (existing == null) "Create BGMI Tournament" else "Edit Tournament", style = MaterialTheme.typography.titleLarge, color = NeonPurple)
                DarkInsetTextField(title, { title = it }, "Match Title", label = "Title", focusColor = NeonPurple)
                DarkInsetTextField(mapName, { mapName = it }, "Erangel / Miramar / Sanhok / Livik", label = "Map", focusColor = NeonPurple)

                Text("Mode (Slot Math)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TournamentMode.entries.forEach { m ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { mode = m },
                            shape = RoundedCornerShape(10.dp),
                            color = if (mode == m) PurpleButtonBg else ObsidianInset,
                            border = BorderStroke(1.dp, if (mode == m) PurpleButtonBorder else GlassBorder)
                        ) {
                            Text(
                                text = m.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (mode == m) PurpleSoftText else TextSecondary,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkInsetTextField(
                        entryFee, { entryFee = it.filter { c -> c.isDigit() } }, "40",
                        label = "Entry (₹)", focusColor = NeonPurple,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    DarkInsetTextField(
                        prizePool, { prizePool = it.filter { c -> c.isDigit() } }, "800",
                        label = "Prize (₹)", focusColor = NeonPurple,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    DarkInsetTextField(
                        perKill, { perKill = it.filter { c -> c.isDigit() } }, "15",
                        label = "Per Kill (₹)", focusColor = NeonPurple,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (existing == null) {
                    DarkInsetTextField(
                        hoursFromNow, { hoursFromNow = it.filter { c -> c.isDigit() } }, "2",
                        label = "Starts in (Hours from now)", focusColor = NeonPurple,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                } else {
                    Text("Match Status", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TournamentStatus.entries.forEach { s ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { status = s },
                                shape = RoundedCornerShape(10.dp),
                                color = if (status == s) PurpleButtonBg else ObsidianInset,
                                border = BorderStroke(1.dp, if (status == s) PurpleButtonBorder else GlassBorder)
                            ) {
                                Text(s.name, style = MaterialTheme.typography.labelSmall, color = if (status == s) PurpleSoftText else TextSecondary, modifier = Modifier.padding(10.dp))
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                    NeonTranslucentButton("Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                    NeonTranslucentButton(
                        text = "Save Match",
                        isAdminAccent = true,
                        isLoading = isBusy,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val startMs = existing?.startTimeMillis
                                ?: (System.currentTimeMillis() + (hoursFromNow.toLongOrNull() ?: 2L).coerceAtLeast(1L) * 3600_000L)
                            onSave(
                                title,
                                mapName,
                                mode,
                                entryFee.toLongOrNull() ?: 0L,
                                prizePool.toLongOrNull() ?: 0L,
                                perKill.toLongOrNull() ?: 0L,
                                startMs,
                                status
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminPublishRoomDialog(
    tournament: Tournament,
    currentAccess: com.example.data.model.RoomAccess?,
    isBusy: Boolean,
    onPublish: (String, String, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    var roomId by remember(currentAccess) { mutableStateOf(currentAccess?.roomId ?: "") }
    var roomPass by remember(currentAccess) { mutableStateOf(currentAccess?.roomPassword ?: "") }
    var published by remember(tournament, currentAccess) { mutableStateOf(currentAccess?.roomPublished ?: tournament.roomPublished) }
    var forceVisible by remember(tournament) { mutableStateOf(tournament.roomForceVisible) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep.copy(alpha = 0.9f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        FrostedGlassCard(borderColor = PurpleButtonBorder) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Publish Room ID & Password", style = MaterialTheme.typography.titleLarge, color = NeonPurple)
                Text(tournament.title, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                DarkInsetTextField(roomId, { roomId = it }, "e.g. 8492014", label = "BGMI Room ID", focusColor = NeonPurple)
                DarkInsetTextField(roomPass, { roomPass = it }, "e.g. NAX25", label = "Room Password", focusColor = NeonPurple)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Publish to Joined Teams", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(
                        checked = published,
                        onCheckedChange = { published = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonPurple, checkedTrackColor = PurpleButtonBg)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Immediately (Skip 15m Lock)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Switch(
                        checked = forceVisible,
                        onCheckedChange = { forceVisible = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonPurple, checkedTrackColor = PurpleButtonBg)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NeonTranslucentButton("Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                    NeonTranslucentButton(
                        text = "Broadcast Room",
                        isAdminAccent = true,
                        isLoading = isBusy,
                        modifier = Modifier.weight(1f),
                        onClick = { onPublish(roomId, roomPass, published, forceVisible) }
                    )
                }
            }
        }
    }
}
