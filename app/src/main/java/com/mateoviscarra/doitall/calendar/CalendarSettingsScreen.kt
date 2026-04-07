package com.mateoviscarra.doitall.calendar

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "CalendarSettingsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSettingsScreen(
    onBack: () -> Unit,
    calendarManager: CalendarManager
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var isConnecting by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf(calendarManager.getAuthState()) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Log.d(TAG, "CalendarSettingsScreen init, connectionStatus: $connectionStatus")

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Sign-in result: resultCode=${result.resultCode}, data=${result.data}")
        isConnecting = false
        
        // First try silent sign-in - check if there's already a signed in account
        val silentClient = calendarManager.getGoogleSignInClient(activity!!)
        silentClient.silentSignIn().addOnSuccessListener { account ->
            Log.d(TAG, "Silent sign-in success: ${account.email}")
            connectionStatus = CalendarAuthState(
                isConnected = true,
                email = account.email,
                displayName = account.displayName
            )
            successMessage = "Successfully connected to Google Calendar!"
        }.addOnFailureListener { e ->
            Log.e(TAG, "Silent sign-in failed, trying explicit", e)
            // Try explicit sign-in result
            try {
                val task = result.data?.let { GoogleSignIn.getSignedInAccountFromIntent(it) }
                task?.addOnSuccessListener { account ->
                    Log.d(TAG, "Explicit sign-in success: ${account.email}")
                    connectionStatus = CalendarAuthState(
                        isConnected = true,
                        email = account.email,
                        displayName = account.displayName
                    )
                    successMessage = "Successfully connected to Google Calendar!"
                }?.addOnFailureListener { e2 ->
                    Log.e(TAG, "Explicit sign-in also failed", e2)
            errorMessage = "Sign-in failed (Error 10): App not configured. Add this app's SHA-1 to Google Cloud Console OAuth credentials, or use a release build with proper signing."
        }
            } catch (e2: Exception) {
                Log.e(TAG, "Error handling sign-in", e2)
                errorMessage = "Sign-in error: ${e2.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (connectionStatus.isConnected) Icons.Default.Check else Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (connectionStatus.isConnected) "Google Calendar Connected" else "Google Calendar Not Connected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (connectionStatus.isConnected) {
                        Text(
                            text = "Connected as ${connectionStatus.email ?: "Google account"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Your workouts can be synced to Google Calendar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            text = "Connect your Google Calendar to sync workout schedules.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (successMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = successMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "How it works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Sign in with your Google account to sync workouts. " +
                               "Your credentials stay on your device and Google handles the authentication. " +
                               "This means you won't be logged out unexpectedly.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (connectionStatus.isConnected) {
                OutlinedButton(
                    onClick = { showDisconnectDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LinkOff, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect Calendar")
                }
            } else {
                Button(
                    onClick = {
                        if (activity == null) {
                            errorMessage = "Cannot start sign-in (Activity unavailable)"
                            return@Button
                        }
                        Log.d(TAG, "Starting Google Sign-In...")
                        isConnecting = true
                        errorMessage = null
                        val signInClient = calendarManager.getGoogleSignInClient(activity)
                        signInLauncher.launch(signInClient.signInIntent)
                    },
                    enabled = !isConnecting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(20.dp)
                                .width(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isConnecting) "Connecting..." else "Sign in with Google")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your credentials are stored securely on your device and managed by Google Play Services.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect Calendar?") },
            text = { Text("This will sign you out of Google Calendar. You can sign in again anytime.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            calendarManager.disconnect()
                            connectionStatus = CalendarAuthState(isConnected = false)
                            showDisconnectDialog = false
                            successMessage = "Disconnected from Google Calendar."
                        }
                    }
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(successMessage, errorMessage) {
        if (successMessage != null || errorMessage != null) {
            delay(3000)
            successMessage = null
            errorMessage = null
        }
    }
}