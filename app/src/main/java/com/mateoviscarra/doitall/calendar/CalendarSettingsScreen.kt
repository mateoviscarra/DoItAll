package com.mateoviscarra.doitall.calendar

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSettingsScreen(
    onBack: () -> Unit,
    calendarManager: CalendarManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var showCredentialsDialog by remember { mutableStateOf(false) }
    var clientId by remember { mutableStateOf("") }
    var clientSecret by remember { mutableStateOf("") }
    var showAuthCodeDialog by remember { mutableStateOf(false) }
    var authUrl by remember { mutableStateOf("") }
    var authCode by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf(calendarManager.getAuthState()) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

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
                        text = "Setup Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "1. Go to console.cloud.google.com\n" +
                               "2. Create a new project or select existing\n" +
                               "3. Enable Google Calendar API\n" +
                               "4. Go to Credentials → Create Credentials → OAuth Client ID\n" +
                               "5. Choose \"Desktop app\" type\n" +
                               "6. Copy the Client ID and Client Secret\n" +
                               "7. Enter them below",
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
                    onClick = { showCredentialsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Google Calendar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "All credentials are stored securely on your device and never leave your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showCredentialsDialog) {
        AlertDialog(
            onDismissRequest = { showCredentialsDialog = false },
            title = { Text("Enter Google OAuth Credentials") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { clientId = it },
                        label = { Text("Client ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientSecret,
                        onValueChange = { clientSecret = it },
                        label = { Text("Client Secret") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clientId.isNotBlank() && clientSecret.isNotBlank()) {
                            calendarManager.saveCredentials(CalendarCredentials(clientId, clientSecret))
                            try {
                                authUrl = calendarManager.getAuthorizationUrl()
                                showCredentialsDialog = false
                                showAuthCodeDialog = true
                            } catch (e: Exception) {
                                errorMessage = "Failed to generate auth URL: ${e.message}"
                            }
                        }
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCredentialsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAuthCodeDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAuthCodeDialog = false
                isConnecting = false
            },
            title = { Text("Authorize DoItAll") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Open this URL in your browser:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    OutlinedTextField(
                        value = authUrl,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(authUrl))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                        }
                    )
                    
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Authorization Page")
                    }
                    
                    OutlinedTextField(
                        value = authCode,
                        onValueChange = { authCode = it },
                        label = { Text("Authorization Code") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        supportingText = { Text("After authorizing, copy the code from the URL (after 'code=') and paste it here") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (authCode.isNotBlank()) {
                            isConnecting = true
                            scope.launch {
                                val result = calendarManager.exchangeCodeForTokens(authCode)
                                result.fold(
                                    onSuccess = {
                                        connectionStatus = calendarManager.getAuthState()
                                        showAuthCodeDialog = false
                                        isConnecting = false
                                        authCode = ""
                                        successMessage = "Successfully connected to Google Calendar!"
                                    },
                                    onFailure = { error ->
                                        errorMessage = "Failed to connect: ${error.message}"
                                        isConnecting = false
                                    }
                                )
                            }
                        }
                    },
                    enabled = authCode.isNotBlank() && !isConnecting
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAuthCodeDialog = false
                    isConnecting = false
                    authCode = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect Calendar?") },
            text = { Text("This will remove your Google Calendar connection. You can reconnect anytime.") },
            confirmButton = {
                Button(
                    onClick = {
                        calendarManager.disconnect()
                        connectionStatus = CalendarAuthState(isConnected = false)
                        showDisconnectDialog = false
                        successMessage = "Disconnected from Google Calendar."
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
