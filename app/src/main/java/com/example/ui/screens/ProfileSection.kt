package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PlatformConfig
import com.example.data.model.UserProfile
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CrimsonBg
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.DarkInsetTextField
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonTranslucentButton
import com.example.ui.theme.ObsidianCardSolid
import com.example.ui.theme.ObsidianInset
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProfileSection(
    userProfile: UserProfile,
    platformConfig: PlatformConfig,
    isBusy: Boolean,
    onSaveProfile: (name: String, phone: String, bgmiIgn: String, bgmiUid: String) -> Unit,
    onSignOutConfirmed: () -> Unit
) {
    val context = LocalContext.current

    var displayName by remember(userProfile.userId, userProfile.displayName) { mutableStateOf(userProfile.displayName) }
    var phone by remember(userProfile.userId, userProfile.phone) { mutableStateOf(userProfile.phone) }
    var bgmiIgn by remember(userProfile.userId, userProfile.bgmiIgn) { mutableStateOf(userProfile.bgmiIgn) }
    var bgmiUid by remember(userProfile.userId, userProfile.bgmiUid) { mutableStateOf(userProfile.bgmiUid) }

    var rulesExpanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    fun openExternalUrl(url: String) {
        val safeUrl = url.trim().ifBlank { "https://www.youtube.com/@Itachiplays" }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Edit Account & Saved BGMI IGN/UID
        item {
            FrostedGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Player Profile & BGMI ID", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Text(userProfile.email, style = MaterialTheme.typography.bodySmall, color = NeonCyan)
                        }
                        Icon(Icons.Rounded.Person, null, tint = NeonCyan, modifier = Modifier.size(28.dp))
                    }

                    DarkInsetTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        placeholder = "Your Name",
                        label = "Player Name",
                        tag = "profile_name_input",
                        leadingIcon = { Icon(Icons.Rounded.Person, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) }
                    )
                    DarkInsetTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
                        placeholder = "Phone Number",
                        label = "Mobile Number",
                        tag = "profile_phone_input",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Rounded.Phone, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) }
                    )
                    DarkInsetTextField(
                        value = bgmiIgn,
                        onValueChange = { bgmiIgn = it },
                        placeholder = "Exact BGMI IGN",
                        label = "Saved Primary BGMI IGN",
                        tag = "profile_ign_input",
                        leadingIcon = { Icon(Icons.Rounded.SportsEsports, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) }
                    )
                    DarkInsetTextField(
                        value = bgmiUid,
                        onValueChange = { bgmiUid = it.filter { ch -> ch.isDigit() }.take(15) },
                        placeholder = "10-digit Character UID",
                        label = "Saved BGMI Character UID",
                        tag = "profile_uid_input",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Rounded.Badge, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    NeonTranslucentButton(
                        text = "Save Profile Changes",
                        isLoading = isBusy,
                        tag = "save_profile_button",
                        onClick = { onSaveProfile(displayName, phone, bgmiIgn, bgmiUid) }
                    )
                }
            }
        }

        // Official Support & Social Channels
        item {
            FrostedGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Official Naxora Support & Channels", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    SupportLinkRow(
                        title = "WhatsApp Support Desk",
                        subtitle = "Instant lobby & UTR payout help",
                        tint = EmeraldWin,
                        icon = { Icon(Icons.Rounded.SupportAgent, null, tint = EmeraldWin, modifier = Modifier.size(20.dp)) },
                        tag = "support_whatsapp_button",
                        onClick = { openExternalUrl(platformConfig.whatsappSupportUrl) }
                    )
                    SupportLinkRow(
                        title = "YouTube — Itachiplays",
                        subtitle = "Live tournament streams & highlights",
                        tint = CrimsonDanger,
                        icon = { Icon(Icons.Rounded.PlayCircle, null, tint = CrimsonDanger, modifier = Modifier.size(20.dp)) },
                        tag = "support_youtube_button",
                        onClick = { openExternalUrl(platformConfig.youtubeUrl) }
                    )
                    SupportLinkRow(
                        title = "Instagram — @ig.amreshwar",
                        subtitle = "Founder updates & match schedules",
                        tint = NeonPurple,
                        icon = { Icon(Icons.Rounded.Public, null, tint = NeonPurple, modifier = Modifier.size(20.dp)) },
                        tag = "support_instagram_button",
                        onClick = { openExternalUrl(platformConfig.instagramUrl) }
                    )
                }
            }
        }

        // Expandable T&C + Security Rules Guidelines
        item {
            FrostedGlassCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_rules_accordion")
                            .clickable { rulesExpanded = !rulesExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Gavel, null, tint = AmberGold, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("T&C Apply + Security Rules Guidelines", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Tap to read BGMI 25-slot anti-cheat & slot rules", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(
                            imageVector = if (rulesExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = "Expand rules",
                            tint = NeonCyan
                        )
                    }

                    AnimatedVisibility(visible = rulesExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RuleBullet("1. Assigned BGMI Slot Only:", "Strictly sit in your assigned BGMI Slot (#1–#25 for Squad, #1–#50 for Duo, #1–#100 for Solo). Sitting in another team's slot leads to immediate kick.")
                            RuleBullet("2. Verified IGN/UID Only:", "Only registered IGNs & Character UIDs submitted in your squad roster are permitted in the lobby.")
                            RuleBullet("3. Zero Emulators or Hacks:", "No emulators, GFX scripts, ESP, or config modifications. Caught players face permanent ban and prize forfeiture.")
                            RuleBullet("4. No Room ID Sharing:", "Never share Room ID or Password outside your registered team.")
                            RuleBullet("5. Screen Recording / POV:", "Players must record gameplay POV when requested by Naxora Admin for suspicious-kill verification.")
                            RuleBullet("6. 20–25% Penalty Policy:", "Wrong-slot sitting or late cancellation incurs a 20–25% entry fee / payout penalty.")
                        }
                    }
                }
            }
        }

        // Founder Card + Logout
        item {
            FrostedGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shield, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("About Naxora eSports", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    }
                    Text(
                        text = stringResource(R.string.platform_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Founder: ${stringResource(R.string.founder_name)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CrimsonBg)
                            .border(1.dp, CrimsonDanger.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .testTag("logout_button")
                            .clickable { showLogoutDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Logout, null, tint = CrimsonDanger, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = CrimsonDanger)
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = ObsidianCardSolid,
            title = { Text("Leave the Arena?", color = TextPrimary) },
            text = { Text("Are you sure you want to sign out of Naxora eSports?", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onSignOutConfirmed()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CrimsonDanger),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = NeonCyan)
                }
            }
        )
    }
}

@Composable
private fun SupportLinkRow(
    title: String,
    subtitle: String,
    tint: Color,
    icon: @Composable () -> Unit,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianInset)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .testTag(tag)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Text("Open", style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
private fun RuleBullet(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianInset)
            .padding(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = AmberGold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}
