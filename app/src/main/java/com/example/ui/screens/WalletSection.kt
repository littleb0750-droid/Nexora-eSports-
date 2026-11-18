package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PlatformConfig
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WalletTxStatus
import com.example.data.model.WalletTxType
import com.example.ui.theme.AmberBg
import com.example.ui.theme.AmberGold
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
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonTranslucentButton
import com.example.ui.theme.ObsidianInset
import com.example.ui.theme.StatusPill
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UpiQrMatrixGraphic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class WalletActionMode {
    ADD_CASH,
    WITHDRAW
}

@Composable
fun WalletSection(
    userProfile: UserProfile,
    platformConfig: PlatformConfig,
    transactionsState: UiState<List<WalletTransaction>>,
    isBusy: Boolean,
    onSubmitDeposit: (amountText: String, utr: String, onClear: () -> Unit) -> Unit,
    onSubmitWithdrawal: (amountText: String, upiId: String, onClear: () -> Unit) -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var actionMode by remember { mutableStateOf(WalletActionMode.ADD_CASH) }

    var depositAmount by remember { mutableStateOf("") }
    var depositUtr by remember { mutableStateOf("") }

    var withdrawAmount by remember { mutableStateOf("") }
    var withdrawUpi by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FrostedGlassCard(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    borderColor = CyanButtonBorder.copy(alpha = 0.5f)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AccountBalanceWallet, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Deposit Cash", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${userProfile.depositBalance}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonCyan
                        )
                        Text("Used for match entry", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }

                FrostedGlassCard(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    borderColor = EmeraldWin.copy(alpha = 0.45f)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Payments, null, tint = EmeraldWin, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Winning Cash", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${userProfile.winningBalance}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldWin
                        )
                        Text("Instant UPI withdrawable", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }
        }

        // Mode Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("wallet_mode_add")
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            actionMode = WalletActionMode.ADD_CASH
                            formError = null
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (actionMode == WalletActionMode.ADD_CASH) CyanButtonBg else ObsidianInset,
                    border = BorderStroke(1.dp, if (actionMode == WalletActionMode.ADD_CASH) CyanButtonBorder else GlassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.AddCard, null, tint = if (actionMode == WalletActionMode.ADD_CASH) NeonCyan else TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Cash (UPI)", style = MaterialTheme.typography.labelLarge, color = if (actionMode == WalletActionMode.ADD_CASH) NeonCyan else TextSecondary)
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("wallet_mode_withdraw")
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            actionMode = WalletActionMode.WITHDRAW
                            formError = null
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (actionMode == WalletActionMode.WITHDRAW) EmeraldBg else ObsidianInset,
                    border = BorderStroke(1.dp, if (actionMode == WalletActionMode.WITHDRAW) EmeraldWin else GlassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, null, tint = if (actionMode == WalletActionMode.WITHDRAW) EmeraldWin else TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Withdraw UPI", style = MaterialTheme.typography.labelLarge, color = if (actionMode == WalletActionMode.WITHDRAW) EmeraldWin else TextSecondary)
                    }
                }
            }
        }

        // Action Card
        item {
            FrostedGlassCard {
                if (actionMode == WalletActionMode.ADD_CASH) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scan Admin UPI QR or Pay via UPI ID",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        UpiQrMatrixGraphic(upiId = platformConfig.adminUpiId)

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = platformConfig.adminUpiName,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianInset)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = platformConfig.adminUpiId,
                                style = MaterialTheme.typography.titleSmall,
                                color = NeonCyan
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(platformConfig.adminUpiId))
                                    onCopiedNotice("Admin UPI ID copied!")
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, "Copy UPI ID", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        DarkInsetTextField(
                            value = depositAmount,
                            onValueChange = { depositAmount = it.filter { ch -> ch.isDigit() }.take(6) },
                            placeholder = "Amount in ₹ (e.g. 100)",
                            label = "Deposit Amount (₹)",
                            tag = "deposit_amount_input",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Rounded.CurrencyRupee, null, tint = NeonCyan, modifier = Modifier.size(18.dp)) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DarkInsetTextField(
                            value = depositUtr,
                            onValueChange = { depositUtr = it.filter { ch -> ch.isDigit() }.take(12) },
                            placeholder = "Enter 12-digit UTR / Ref Number",
                            label = "12-Digit UPI UTR Number",
                            tag = "deposit_utr_input",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        if (formError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = formError!!, style = MaterialTheme.typography.bodySmall, color = CrimsonDanger)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        NeonTranslucentButton(
                            text = "Submit UTR for Verification",
                            isLoading = isBusy,
                            tag = "submit_deposit_button",
                            onClick = {
                                val amt = depositAmount.toLongOrNull()
                                if (amt == null || amt < 10L) {
                                    formError = "Minimum deposit is ₹10."
                                } else if (!depositUtr.matches(Regex("^[0-9]{12}$"))) {
                                    formError = "UTR must be exactly 12 numeric digits."
                                } else {
                                    formError = null
                                    onSubmitDeposit(depositAmount, depositUtr) {
                                        depositAmount = ""
                                        depositUtr = ""
                                    }
                                }
                            }
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Withdraw Winning Cash via UPI",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Available Winning Cash: ₹${userProfile.winningBalance}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldWin
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        DarkInsetTextField(
                            value = withdrawAmount,
                            onValueChange = { withdrawAmount = it.filter { ch -> ch.isDigit() }.take(6) },
                            placeholder = "Amount in ₹ (min 20)",
                            label = "Withdrawal Amount (₹)",
                            tag = "withdraw_amount_input",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Rounded.CurrencyRupee, null, tint = EmeraldWin, modifier = Modifier.size(18.dp)) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DarkInsetTextField(
                            value = withdrawUpi,
                            onValueChange = { withdrawUpi = it },
                            placeholder = "yourname@okaxis / phone@ybl",
                            label = "Your UPI ID",
                            tag = "withdraw_upi_input"
                        )

                        if (formError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = formError!!, style = MaterialTheme.typography.bodySmall, color = CrimsonDanger)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        NeonTranslucentButton(
                            text = "Request UPI Payout",
                            isLoading = isBusy,
                            tag = "submit_withdraw_button",
                            onClick = {
                                val amt = withdrawAmount.toLongOrNull()
                                if (amt == null || amt < 20L) {
                                    formError = "Minimum withdrawal is ₹20."
                                } else if (amt > userProfile.winningBalance) {
                                    formError = "Exceeds Winning Cash (₹${userProfile.winningBalance})."
                                } else if (!withdrawUpi.contains("@") || withdrawUpi.length < 4) {
                                    formError = "Enter a valid UPI ID."
                                } else {
                                    formError = null
                                    onSubmitWithdrawal(withdrawAmount, withdrawUpi) {
                                        withdrawAmount = ""
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Transaction History Header
        item {
            Text(
                text = "Live Transaction Status",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        when (transactionsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    FrostedGlassCard {
                        Text(transactionsState.message, color = CrimsonDanger, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            is UiState.Success -> {
                val txList = transactionsState.data
                if (txList.isEmpty()) {
                    item {
                        FrostedGlassCard {
                            Text(
                                text = "No wallet transactions yet. Add cash via UPI or win matches to see live status here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(txList, key = { it.id }) { tx ->
                        WalletTransactionRow(tx = tx)
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletTransactionRow(tx: WalletTransaction) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val dateStr = remember(tx.createdAt) {
        val ts = tx.updatedAt ?: tx.createdAt
        if (ts != null) dateFormat.format(Date(ts.seconds * 1000L)) else "Just now"
    }

    val (statusColor, statusBg) = when (tx.statusEnum) {
        WalletTxStatus.PENDING -> AmberGold to AmberBg
        WalletTxStatus.APPROVED -> EmeraldWin to EmeraldBg
        WalletTxStatus.REJECTED -> CrimsonDanger to CrimsonBg
    }

    val isCredit = tx.typeEnum == WalletTxType.DEPOSIT || tx.typeEnum == WalletTxType.PRIZE
    val amountPrefix = if (isCredit) "+" else "-"
    val amountColor = if (isCredit) EmeraldWin else TextPrimary

    FrostedGlassCard(contentPadding = PaddingValues(14.dp), cornerRadius = 18.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianInset),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        contentDescription = null,
                        tint = if (isCredit) EmeraldWin else NeonPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(tx.typeEnum.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(tx.note, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix₹${tx.amount}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusPill(text = tx.statusEnum.label, color = statusColor, bgColor = statusBg)
            }
        }
    }
}
