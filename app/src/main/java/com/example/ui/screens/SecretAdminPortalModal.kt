package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.AdminPortalConfig
import com.example.data.repository.onGoogleSignInClicked
import com.example.ui.theme.DarkInsetTextField
import com.example.ui.theme.EmeraldBg
import com.example.ui.theme.EmeraldWin
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonTranslucentButton
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.PurpleButtonBorder
import com.example.ui.theme.PurpleSoftText
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun SecretAdminPortalModal(
    isAuthorizedAdmin: Boolean,
    adminPortalConfig: AdminPortalConfig?,
    verifiedRecoveryEmail: String?,
    recoveryError: String?,
    isBusy: Boolean,
    defaultEmail: String,
    onLoginSubmit: (String, String) -> Unit,
    onVerifyRecoveryEmail: (String) -> Unit,
    onReauthGoogleAccount: () -> Unit,
    onRecoverCredentials: (
        newAdminEmail: String,
        newRecoveryEmail: String,
        newPassword: String,
        recoveryHint: String,
        onResetSuccess: () -> Unit
    ) -> Unit,
    onClearRecoveryState: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isRecoveryMode by remember { mutableStateOf(false) }
    var reauthLoading by remember { mutableStateOf(false) }

    var adminEmail by remember(adminPortalConfig) {
        mutableStateOf(adminPortalConfig?.adminEmail?.ifBlank { defaultEmail } ?: defaultEmail)
    }
    var adminPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Step 1 Recovery: Verify Registered Recovery Email
    var enteredRecoveryEmail by remember(adminPortalConfig) {
        mutableStateOf(
            adminPortalConfig?.recoveryEmail?.ifBlank { adminPortalConfig.adminEmail }
                ?: defaultEmail
        )
    }

    // Step 2 Recovery: Reset Admin Credentials
    var newAdminEmail by remember(adminPortalConfig) {
        mutableStateOf(adminPortalConfig?.adminEmail?.ifBlank { defaultEmail } ?: defaultEmail)
    }
    var updatedRecoveryEmail by remember(verifiedRecoveryEmail, adminPortalConfig) {
        mutableStateOf(
            verifiedRecoveryEmail
                ?: adminPortalConfig?.recoveryEmail?.ifBlank { defaultEmail }
                ?: defaultEmail
        )
    }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showResetPassword by remember { mutableStateOf(false) }
    var recoveryHint by remember(adminPortalConfig) {
        mutableStateOf(adminPortalConfig?.recoveryHint ?: "")
    }
    var localError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep.copy(alpha = 0.88f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        FrostedGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            borderColor = PurpleButtonBorder.copy(alpha = 0.45f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AdminPanelSettings,
                        contentDescription = "Master Control Portal",
                        tint = NeonPurple,
                        modifier = Modifier.size(30.dp)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_admin_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Admin Portal",
                            tint = TextSecondary
                        )
                    }
                }

                // Exact reference styling: Neon purple title "Naxora Admin" (#C060FF)
                Text(
                    text = stringResource(R.string.admin_portal_title),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = NeonPurple,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        !isRecoveryMode -> stringResource(R.string.admin_portal_subtitle)
                        verifiedRecoveryEmail == null -> "Step 1/2 • Verify Registered Recovery Email"
                        else -> "Step 2/2 • Reset Master Portal Credentials"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (!isAuthorizedAdmin) {
                    Text(
                        text = "Restricted Zone: Only verified Naxora Master Admin accounts can unlock or recover the Control Portal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else if (!isRecoveryMode) {
                    if (adminPortalConfig == null) {
                        Text(
                            text = "First-time setup: Set your Master Admin Email & Portal Password below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PurpleSoftText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    DarkInsetTextField(
                        value = adminEmail,
                        onValueChange = { adminEmail = it },
                        placeholder = "admin@naxora.esports",
                        label = "Admin Email",
                        focusColor = NeonPurple,
                        tag = "admin_email_input",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(Icons.Rounded.Email, null, tint = PurpleSoftText, modifier = Modifier.size(18.dp))
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DarkInsetTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        placeholder = "Enter Master Password",
                        label = "Admin Password",
                        focusColor = NeonPurple,
                        tag = "admin_password_input",
                        visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Rounded.Lock, null, tint = PurpleSoftText, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = if (showLoginPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { showLoginPassword = !showLoginPassword }
                            )
                        }
                    )

                    if (localError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = localError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    NeonTranslucentButton(
                        text = if (adminPortalConfig == null) "Initialize & Access Portal" else "Access Portal",
                        isAdminAccent = true,
                        isLoading = isBusy,
                        tag = "admin_access_portal_button",
                        onClick = {
                            localError = null
                            if (adminEmail.isBlank() || adminPassword.length < 4) {
                                localError = "Enter valid Admin Email and Password (min 4 chars)."
                            } else {
                                onLoginSubmit(adminEmail, adminPassword)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .testTag("admin_forgot_credentials_button")
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                localError = null
                                onClearRecoveryState()
                                isRecoveryMode = true
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LockReset,
                            contentDescription = null,
                            tint = PurpleSoftText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Forgot Admin ID / Password?",
                            style = MaterialTheme.typography.labelMedium,
                            color = PurpleSoftText
                        )
                    }
                } else if (verifiedRecoveryEmail == null) {
                    // STEP 1: Verify Registered Recovery Email via Firebase Auth + Firestore
                    Text(
                        text = "Enter the registered recovery email linked to your Master Control Portal. Firebase Auth & Firestore will verify your identity before allowing a reset.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PurpleSoftText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    DarkInsetTextField(
                        value = enteredRecoveryEmail,
                        onValueChange = { enteredRecoveryEmail = it },
                        placeholder = "Registered recovery email",
                        label = "Registered Recovery Email",
                        focusColor = NeonPurple,
                        tag = "admin_recovery_verify_email_input",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(Icons.Rounded.MarkEmailRead, null, tint = PurpleSoftText, modifier = Modifier.size(18.dp))
                        }
                    )

                    val displayErr = localError ?: recoveryError
                    if (displayErr != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = displayErr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    NeonTranslucentButton(
                        text = "Verify Recovery Email",
                        isAdminAccent = true,
                        isLoading = isBusy,
                        tag = "admin_verify_recovery_button",
                        leadingIcon = {
                            Icon(Icons.Rounded.VerifiedUser, null, tint = PurpleSoftText, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            localError = null
                            if (!enteredRecoveryEmail.contains("@")) {
                                localError = "Enter a valid recovery email address."
                            } else {
                                onVerifyRecoveryEmail(enteredRecoveryEmail)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    NeonTranslucentButton(
                        text = "Verify with Google Account",
                        isAdminAccent = false,
                        isLoading = reauthLoading,
                        tag = "admin_reauth_google_button",
                        onClick = {
                            localError = null
                            reauthLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    reauthLoading = false
                                    onReauthGoogleAccount()
                                },
                                onAuthError = { msg ->
                                    reauthLoading = false
                                    localError = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    reauthLoading = false
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Back to Admin Login",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted,
                        modifier = Modifier
                            .clickable {
                                localError = null
                                onClearRecoveryState()
                                isRecoveryMode = false
                            }
                            .padding(8.dp)
                    )
                } else {
                    // STEP 2: Verified Recovery Email -> Reset Admin Credentials
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(EmeraldBg)
                            .border(1.dp, EmeraldWin.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = EmeraldWin, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Verified: $verifiedRecoveryEmail",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldWin
                            )
                            if (!adminPortalConfig?.recoveryHint.isNullOrBlank()) {
                                Text(
                                    text = "Saved Hint: ${adminPortalConfig?.recoveryHint}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PurpleSoftText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    DarkInsetTextField(
                        value = newAdminEmail,
                        onValueChange = { newAdminEmail = it },
                        placeholder = "Admin login email",
                        label = "Admin Portal Email / ID",
                        focusColor = NeonPurple,
                        tag = "admin_recovery_email_input",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(Icons.Rounded.Email, null, tint = PurpleSoftText, modifier = Modifier.size(18.dp))
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DarkInsetTextField(
                        value = updatedRecoveryEmail,
                        onValueChange = { updatedRecoveryEmail = it },
                        placeholder = "Registered recovery email",
                        label = "Registered Recovery Email",
                        focusColor = NeonPurple,
                        tag = "admin_recovery_registered_email_input",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(Icons.Rounded.MarkEmailRead, null, tint = PurpleSoftText, modifier = Modifier.size(18.dp))
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DarkInsetTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = "Enter new Admin Password (min 4 chars)",
                        label = "New Admin Password",
                        focusColor = NeonPurple,
                        tag = "admin_recovery_password_input",
                        visualTransformation = if (showResetPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Rounded.Key, null, tint = PurpleSoftText, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = if (showResetPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { showResetPassword = !showResetPassword }
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DarkInsetTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Confirm new Admin Password",
                        label = "Confirm Password",
                        focusColor = NeonPurple,
                        tag = "admin_recovery_confirm_password_input",
                        visualTransformation = if (showResetPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Rounded.Lock, null, tint = PurpleSoftText, modifier = Modifier.size(18.dp))
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DarkInsetTextField(
                        value = recoveryHint,
                        onValueChange = { recoveryHint = it },
                        placeholder = "Optional recovery hint",
                        label = "Recovery Hint",
                        focusColor = NeonPurple,
                        tag = "admin_recovery_hint_input"
                    )

                    val displayErr = localError ?: recoveryError
                    if (displayErr != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = displayErr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    NeonTranslucentButton(
                        text = "Reset & Save Admin Credentials",
                        isAdminAccent = true,
                        isLoading = isBusy,
                        tag = "admin_save_recovery_button",
                        onClick = {
                            localError = null
                            if (!newAdminEmail.contains("@") || !updatedRecoveryEmail.contains("@")) {
                                localError = "Enter valid Admin and Recovery email addresses."
                            } else if (newPassword.trim().length < 4) {
                                localError = "New password must be at least 4 characters."
                            } else if (newPassword.trim() != confirmPassword.trim()) {
                                localError = "Passwords do not match."
                            } else {
                                onRecoverCredentials(
                                    newAdminEmail,
                                    updatedRecoveryEmail,
                                    newPassword,
                                    recoveryHint
                                ) {
                                    adminEmail = newAdminEmail
                                    adminPassword = ""
                                    newPassword = ""
                                    confirmPassword = ""
                                    isRecoveryMode = false
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Cancel Recovery",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextMuted,
                        modifier = Modifier
                            .clickable {
                                localError = null
                                onClearRecoveryState()
                                isRecoveryMode = false
                            }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
