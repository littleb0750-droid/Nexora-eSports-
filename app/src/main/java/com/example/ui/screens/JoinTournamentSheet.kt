package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.SquadPlayerInput
import com.example.data.model.Tournament
import com.example.data.model.TournamentRegistration
import com.example.data.model.UserProfile
import com.example.data.model.parseSquadRosterText
import com.example.ui.theme.CrimsonBg
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyanButtonBg
import com.example.ui.theme.CyanButtonBorder
import com.example.ui.theme.DarkInsetTextField
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonTranslucentButton
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianInset
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JoinTournamentSheet(
    tournament: Tournament,
    userProfile: UserProfile,
    existingRegistration: TournamentRegistration?,
    isBusy: Boolean,
    onConfirmJoin: (slotNumber: Long, teamName: String, igns: List<String>, uids: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val requiredPlayers = tournament.playersPerSlot.toInt().coerceIn(1, 4)
    val isViewOnly = existingRegistration != null

    var selectedSlot by remember(tournament.id, existingRegistration) {
        val firstAvailable = (1L..tournament.maxSlots).firstOrNull { !tournament.bookedSlotNumbers.contains(it) }
        mutableStateOf(existingRegistration?.slotNumber ?: firstAvailable)
    }

    var teamName by remember(existingRegistration) {
        mutableStateOf(
            existingRegistration?.teamName ?: if (requiredPlayers == 1) userProfile.bgmiIgn.ifBlank { userProfile.displayName } else ""
        )
    }

    val playerInputs = remember(tournament.id, existingRegistration) {
        mutableStateListOf<SquadPlayerInput>().apply {
            for (i in 0 until requiredPlayers) {
                if (existingRegistration != null) {
                    add(
                        SquadPlayerInput(
                            ign = existingRegistration.playerIgns.getOrElse(i) { "" },
                            uid = existingRegistration.playerUids.getOrElse(i) { "" }
                        )
                    )
                } else if (i == 0) {
                    add(SquadPlayerInput(ign = userProfile.bgmiIgn, uid = userProfile.bgmiUid))
                } else {
                    add(SquadPlayerInput())
                }
            }
        }
    }

    var showPasteBox by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep.copy(alpha = 0.90f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        FrostedGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isViewOnly) "Your Assigned Slot" else "Select BGMI Slot & Roster",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "${tournament.title} • ${tournament.modeEnum.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_join_sheet")) {
                        Icon(Icons.Rounded.Close, "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 25-Slot (or 50/100) Grid
                Text(
                    text = "BGMI Custom Room Slots (#1 – #${tournament.maxSlots})",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (slot in 1L..tournament.maxSlots) {
                        val isBooked = tournament.bookedSlotNumbers.contains(slot)
                        val isMySlot = existingRegistration?.slotNumber == slot
                        val isSelected = selectedSlot == slot

                        val bg = when {
                            isMySlot -> EmeraldBg
                            isSelected -> CyanButtonBg
                            isBooked -> CrimsonBg
                            else -> ObsidianInset
                        }
                        val border = when {
                            isMySlot -> EmeraldWin
                            isSelected -> NeonCyan
                            isBooked -> CrimsonDanger.copy(alpha = 0.35f)
                            else -> GlassBorder
                        }
                        val textCol = when {
                            isMySlot -> EmeraldWin
                            isSelected -> NeonCyan
                            isBooked -> TextMuted
                            else -> TextPrimary
                        }

                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(10.dp))
                                .testTag("slot_tile_$slot")
                                .clickable(enabled = !isViewOnly && !isBooked) {
                                    selectedSlot = slot
                                    validationError = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$slot",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = textCol
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Team Name
                DarkInsetTextField(
                    value = teamName,
                    onValueChange = { if (!isViewOnly) teamName = it },
                    placeholder = if (requiredPlayers == 1) "Solo Player Tag" else "Enter Squad / Team Name",
                    label = if (requiredPlayers == 1) "Player Tag" else "Team / Clan Name",
                    tag = "join_team_name_input",
                    leadingIcon = { Icon(Icons.Rounded.Groups, null, tint = NeonCyan, modifier = Modifier.size(18.dp)) }
                )

                if (!isViewOnly && requiredPlayers > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .testTag("toggle_paste_squad_box")
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyanButtonBg)
                            .border(1.dp, CyanButtonBorder, RoundedCornerShape(10.dp))
                            .clickable { showPasteBox = !showPasteBox }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.ContentPaste, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (showPasteBox) "Hide Quick Squad Paste Box" else "Paste Full Squad IGN/UID List",
                            style = MaterialTheme.typography.labelMedium,
                            color = NeonCyan
                        )
                    }

                    AnimatedVisibility(visible = showPasteBox) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            DarkInsetTextField(
                                value = pasteText,
                                onValueChange = { pasteText = it },
                                placeholder = "Player1 - 5123456789\nPlayer2 - 5987654321\nPlayer3 - 5456789123\nPlayer4 - 5321654987",
                                label = "Paste 1 player per line (IGN + UID)",
                                singleLine = false,
                                maxLines = 5,
                                tag = "paste_squad_textarea"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            NeonTranslucentButton(
                                text = "Auto-Fill Roster from Paste",
                                tag = "parse_squad_button",
                                leadingIcon = {
                                    Icon(Icons.Rounded.AutoAwesome, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    val parsed = parseSquadRosterText(pasteText, requiredPlayers)
                                    for (i in 0 until requiredPlayers) {
                                        playerInputs[i] = parsed[i]
                                    }
                                    showPasteBox = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Verified Player IGN & Character UID Roster",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                for (idx in 0 until requiredPlayers) {
                    val roleTitle = if (idx == 0) "Player 1 (Leader)" else "Player ${idx + 1}"
                    val current = playerInputs[idx]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DarkInsetTextField(
                            value = current.ign,
                            onValueChange = { newVal ->
                                if (!isViewOnly) playerInputs[idx] = current.copy(ign = newVal)
                            },
                            placeholder = "$roleTitle IGN",
                            label = "$roleTitle IGN",
                            tag = "player_${idx + 1}_ign",
                            modifier = Modifier.weight(1f)
                        )
                        DarkInsetTextField(
                            value = current.uid,
                            onValueChange = { newVal ->
                                if (!isViewOnly) {
                                    playerInputs[idx] = current.copy(uid = newVal.filter { ch -> ch.isDigit() }.take(15))
                                }
                            },
                            placeholder = "BGMI UID",
                            label = "Character UID",
                            tag = "player_${idx + 1}_uid",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                if (!isViewOnly) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Wallet Balance: ₹${userProfile.totalBalance} • Entry Fee: ${if (tournament.entryFee == 0L) "FREE" else "₹${tournament.entryFee}"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (userProfile.totalBalance >= tournament.entryFee) EmeraldWin else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NeonTranslucentButton(
                        text = "Confirm Slot #${selectedSlot ?: "-"} • Pay ₹${tournament.entryFee}",
                        isLoading = isBusy,
                        tag = "confirm_join_slot_button",
                        onClick = {
                            val slot = selectedSlot
                            if (slot == null) {
                                validationError = "No available slot selected."
                                return@NeonTranslucentButton
                            }
                            if (teamName.isBlank()) {
                                validationError = "Enter your Team / Player Name."
                                return@NeonTranslucentButton
                            }
                            val incomplete = playerInputs.any { it.ign.isBlank() || it.uid.length < 6 }
                            if (incomplete) {
                                validationError = "Enter valid IGN and Character UID (min 6 digits) for all $requiredPlayers player(s)."
                                return@NeonTranslucentButton
                            }
                            if (userProfile.totalBalance < tournament.entryFee) {
                                validationError = "Insufficient Wallet balance. Please add funds in Wallet tab."
                                return@NeonTranslucentButton
                            }
                            validationError = null
                            onConfirmJoin(
                                slot,
                                teamName,
                                playerInputs.map { it.ign.trim() },
                                playerInputs.map { it.uid.trim() }
                            )
                        }
                    )
                } else {
                    NeonTranslucentButton(
                        text = "Close Roster",
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}
