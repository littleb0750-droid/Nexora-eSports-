package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RoomAccess
import com.example.data.model.Tournament
import com.example.data.model.TournamentMode
import com.example.data.model.TournamentRegistration
import com.example.data.model.TournamentStatus
import com.example.data.model.UiState
import com.example.ui.theme.AmberBg
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CyanButtonBg
import com.example.ui.theme.CyanButtonBorder
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonTranslucentButton
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianInset
import com.example.ui.theme.PurpleButtonBg
import com.example.ui.theme.PurpleSoftText
import com.example.ui.theme.StatusPill
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TournamentsSection(
    tournamentsState: UiState<List<Tournament>>,
    myRegistrations: Map<String, TournamentRegistration>,
    roomAccessByTournament: Map<String, RoomAccess>,
    onObserveRegistration: (String) -> Unit,
    onRequestRoomAccess: (Tournament) -> Unit,
    onOpenJoinModal: (Tournament) -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    var selectedModeFilter by remember { mutableStateOf<TournamentMode?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Mode Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeFilterChip(
                label = "All Lobbies",
                selected = selectedModeFilter == null,
                onClick = { selectedModeFilter = null },
                tag = "filter_all"
            )
            TournamentMode.entries.forEach { mode ->
                ModeFilterChip(
                    label = mode.label,
                    selected = selectedModeFilter == mode,
                    onClick = { selectedModeFilter = mode },
                    tag = "filter_${mode.name.lowercase()}"
                )
            }
        }

        when (tournamentsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    FrostedGlassCard {
                        Text(
                            text = tournamentsState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            is UiState.Success -> {
                val filtered = remember(tournamentsState.data, selectedModeFilter) {
                    tournamentsState.data.filter {
                        selectedModeFilter == null || it.modeEnum == selectedModeFilter
                    }
                }
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        FrostedGlassCard {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SportsEsports,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Custom Rooms Scheduled Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "New 25-slot BGMI matches are hosted daily by Naxora eSports.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filtered, key = { it.id }) { tournament ->
                            LaunchedEffect(tournament.id) {
                                onObserveRegistration(tournament.id)
                            }
                            val registration = myRegistrations[tournament.id]
                            LaunchedEffect(tournament.id, registration != null, tournament.roomPublished) {
                                if (registration != null && tournament.roomPublished) {
                                    onRequestRoomAccess(tournament)
                                }
                            }
                            TournamentMatchCard(
                                tournament = tournament,
                                myRegistration = registration,
                                roomAccess = roomAccessByTournament[tournament.id],
                                onOpenJoinModal = { onOpenJoinModal(tournament) },
                                onCopiedNotice = onCopiedNotice
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        modifier = Modifier
            .testTag(tag)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (selected) CyanButtonBg else ObsidianInset,
        border = BorderStroke(1.dp, if (selected) CyanButtonBorder else GlassBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) NeonCyan else TextSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun TournamentMatchCard(
    tournament: Tournament,
    myRegistration: TournamentRegistration?,
    roomAccess: RoomAccess?,
    onOpenJoinModal: () -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val dateFormat = remember { SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(tournament.startTimeMillis) {
        dateFormat.format(Date(tournament.startTimeMillis))
    }

    val mapDrawable = when {
        tournament.mapName.contains("Miramar", ignoreCase = true) -> R.drawable.img_map_miramar
        tournament.mapName.contains("Erangel", ignoreCase = true) -> R.drawable.img_map_erangel
        else -> R.drawable.img_arena_hero
    }

    val slotsUsed = tournament.bookedSlotNumbers.size
    val slotProgress = (slotsUsed.toFloat() / tournament.maxSlots.coerceAtLeast(1L)).coerceIn(0f, 1f)
    val isFull = slotsUsed >= tournament.maxSlots

    val now = System.currentTimeMillis()
    val within15Min = (tournament.startTimeMillis - now) <= 15L * 60L * 1000L
    val roomUnlockedForUser = myRegistration != null &&
        tournament.roomPublished &&
        (tournament.roomForceVisible || within15Min)

    FrostedGlassCard(
        contentPadding = PaddingValues(0.dp),
        borderColor = if (myRegistration != null) CyanButtonBorder else GlassBorder
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Map Artwork Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                Image(
                    painter = painterResource(id = mapDrawable),
                    contentDescription = "${tournament.mapName} map banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, ObsidianDeep.copy(alpha = 0.92f))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    StatusPill(
                        text = tournament.modeEnum.label,
                        color = PurpleSoftText,
                        bgColor = PurpleButtonBg
                    )
                    val (statusColor, statusBg) = when (tournament.statusEnum) {
                        TournamentStatus.LIVE -> EmeraldWin to EmeraldBg
                        TournamentStatus.UPCOMING -> NeonCyan to CyanButtonBg
                        TournamentStatus.COMPLETED -> TextSecondary to ObsidianInset
                    }
                    StatusPill(
                        text = tournament.statusEnum.label,
                        color = statusColor,
                        bgColor = statusBg
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = tournament.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${tournament.mapName} • $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Metrics & Slots Progress
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        label = "Prize Pool",
                        value = "₹${tournament.prizePool}",
                        accent = AmberGold,
                        icon = { Icon(Icons.Rounded.EmojiEvents, null, tint = AmberGold, modifier = Modifier.size(15.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Per Kill",
                        value = "₹${tournament.perKillPrize}",
                        accent = NeonPurple,
                        icon = { Icon(Icons.Rounded.MilitaryTech, null, tint = NeonPurple, modifier = Modifier.size(15.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Entry Fee",
                        value = if (tournament.entryFee == 0L) "FREE" else "₹${tournament.entryFee}",
                        accent = EmeraldWin,
                        icon = { Icon(Icons.Rounded.Groups, null, tint = EmeraldWin, modifier = Modifier.size(15.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 25-Slot / 100-Player Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$slotsUsed/${tournament.maxSlots} ${tournament.modeEnum.name} Slots • ${tournament.bookedPlayerCount}/${tournament.totalPlayerCapacity} Players",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${tournament.maxSlots - slotsUsed} left",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFull) AmberGold else NeonCyan
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { slotProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (isFull) AmberGold else NeonCyan,
                    trackColor = ObsidianInset
                )

                // Room ID & Password Box
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianInset)
                        .border(
                            1.dp,
                            if (roomUnlockedForUser) EmeraldWin.copy(alpha = 0.45f) else GlassBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    if (myRegistration == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Room ID & Password visible to joined teams 15 mins before match start.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    } else if (roomUnlockedForUser && roomAccess != null && roomAccess.roomId.isNotBlank()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.LockOpen, null, tint = EmeraldWin, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE ROOM • SLOT #${myRegistration.slotNumber}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldWin
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString("Room ID: ${roomAccess.roomId} | Pass: ${roomAccess.roomPassword}"))
                                        onCopiedNotice("Room ID & Password copied!")
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, "Copy room credentials", tint = NeonCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Room ID: ${roomAccess.roomId}   •   Pass: ${roomAccess.roomPassword}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                text = "Team: ${myRegistration.teamName} — Sit strictly in Slot #${myRegistration.slotNumber}!",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberGold
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Lock, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Joined Slot #${myRegistration.slotNumber} (${myRegistration.teamName})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonCyan
                                )
                                Text(
                                    text = "Room ID & Password unlocks 15 mins before match start.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (myRegistration != null) {
                    NeonTranslucentButton(
                        text = "Registered in Slot #${myRegistration.slotNumber} • View Roster",
                        tag = "view_slot_${tournament.id}",
                        onClick = onOpenJoinModal
                    )
                } else {
                    NeonTranslucentButton(
                        text = if (isFull) "Lobby Full (${tournament.maxSlots}/${tournament.maxSlots} Slots)" else "Select Slot & Join Match",
                        enabled = !isFull && tournament.statusEnum == TournamentStatus.UPCOMING,
                        tag = "join_match_${tournament.id}",
                        onClick = onOpenJoinModal
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accent: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianInset)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = accent
        )
    }
}
