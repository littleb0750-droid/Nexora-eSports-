package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.NaxoraRepository
import com.example.data.repository.authStateFlow
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.PendingSignupProfile
import com.example.ui.screens.UserDashboardScreen
import com.example.ui.theme.NaxoraTheme
import com.example.ui.viewmodel.NaxoraViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NaxoraTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var pendingSignup by remember { mutableStateOf<PendingSignupProfile?>(null) }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { signupInfo ->
                pendingSignup = signupInfo
            }
        )
    } else {
        val uid = user.uid
        key(uid) {
            val viewModel: NaxoraViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        NaxoraViewModel(NaxoraRepository(db), uid)
                    }
                }
            )

            // Apply any Sign-Up profile fields entered prior to Google Sign-In
            LaunchedEffect(uid, pendingSignup) {
                val pending = pendingSignup
                if (pending != null) {
                    viewModel.applyPendingSignupProfile(
                        displayName = pending.displayName,
                        phone = pending.phone,
                        bgmiIgn = pending.bgmiIgn,
                        bgmiUid = pending.bgmiUid
                    )
                    pendingSignup = null
                }
            }

            UserDashboardScreen(viewModel = viewModel)
        }
    }
}
