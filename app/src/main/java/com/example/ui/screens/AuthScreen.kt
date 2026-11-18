package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.repository.attemptAutoSignIn
import com.example.data.repository.onGoogleSignInClicked
import com.example.ui.theme.DarkInsetTextField
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonTranslucentButton
import com.example.ui.theme.ObsidianArenaBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class PendingSignupProfile(
    val displayName: String = "",
    val phone: String = "",
    val bgmiIgn: String = "",
    val bgmiUid: String = ""
)

@Composable
fun AuthScreen(
    onAuthSuccess: (PendingSignupProfile) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isSignUpMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var bgmiIgnInput by remember { mutableStateOf("") }
    var bgmiUidInput by remember { mutableStateOf("") }

    var titleTapCount by remember { mutableIntStateOf(0) }
    var lastTapMs by remember { mutableLongStateOf(0L) }
    var adminHintVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { onAuthSuccess(PendingSignupProfile()) },
            onUnauthenticated = {},
            scope = scope
        )
    }

    ObsidianArenaBackground {
        // Subtle atmospheric hero art in upper backdrop
        Image(
            painter = painterResource(id = R.drawable.img_arena_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .alpha(0.16f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            FrostedGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Arena Crest Badge
                    Icon(
                        imageVector = Icons.Rounded.SportsEsports,
                        contentDescription = "Naxora eSports emblem",
                        tint = NeonCyan,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Title "Naxora" matching exact reference style (also responds to 5-7 taps)
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary,
                        modifier = Modifier
                            .testTag("auth_brand_title")
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val now = System.currentTimeMillis()
                                titleTapCount = if (now - lastTapMs > 3000L) 1 else titleTapCount + 1
                                lastTapMs = now
                                if (titleTapCount >= 5) {
                                    titleTapCount = 0
                                    adminHintVisible = true
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSignUpMode) stringResource(R.string.signup_subtitle)
                        else stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (adminHintVisible) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Master Control Portal detected: Sign in with your verified Admin Google account first, then tap \"Naxora eSports\" 5× on the dashboard.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    AnimatedVisibility(visible = isSignUpMode) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            DarkInsetTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                placeholder = "Enter your full name",
                                label = "Player Name",
                                tag = "signup_name_input",
                                leadingIcon = {
                                    Icon(Icons.Rounded.Person, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            )
                            DarkInsetTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
                                placeholder = "10-digit mobile number",
                                label = "Phone Number",
                                tag = "signup_phone_input",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = {
                                    Icon(Icons.Rounded.Phone, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            )
                            DarkInsetTextField(
                                value = bgmiIgnInput,
                                onValueChange = { bgmiIgnInput = it },
                                placeholder = "Exact in-game name (IGN)",
                                label = "Primary BGMI IGN",
                                tag = "signup_ign_input",
                                leadingIcon = {
                                    Icon(Icons.Rounded.SportsEsports, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            )
                            DarkInsetTextField(
                                value = bgmiUidInput,
                                onValueChange = { bgmiUidInput = it.filter { ch -> ch.isDigit() }.take(15) },
                                placeholder = "10-digit Character UID (e.g. 5123456789)",
                                label = "BGMI Character UID",
                                tag = "signup_uid_input",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = {
                                    Icon(Icons.Rounded.Badge, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            )
                        }
                    }

                    if (!isSignUpMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Email,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Secured by Google Sign-In • 25-Slot BGMI Rooms",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    NeonTranslucentButton(
                        text = if (isSignUpMode) "Sign Up with Google" else "Log In with Google",
                        tag = "google_signin_button",
                        isLoading = isLoading,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Login,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            errorMessage = null
                            if (isSignUpMode && nameInput.isBlank()) {
                                errorMessage = "Please enter your Player Name or switch to Log In."
                                return@NeonTranslucentButton
                            }
                            isLoading = true
                            val pending = if (isSignUpMode) {
                                PendingSignupProfile(
                                    displayName = nameInput,
                                    phone = phoneInput,
                                    bgmiIgn = bgmiIgnInput,
                                    bgmiUid = bgmiUidInput
                                )
                            } else {
                                PendingSignupProfile()
                            }
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess(pending)
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Toggle Footer: "Don't have an account? Sign up"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .testTag("toggle_auth_mode_button")
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                isSignUpMode = !isSignUpMode
                                errorMessage = null
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = if (isSignUpMode) "Log in" else "Sign up",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Naxora eSports • Founded by Amreshwar Maravi",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
