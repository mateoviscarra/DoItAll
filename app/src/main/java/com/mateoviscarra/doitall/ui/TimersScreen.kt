package com.mateoviscarra.doitall.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mateoviscarra.doitall.calendar.CalendarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class TimerType(
    val name: String,
    val defaultMinutes: Int,
    val quickDescriptions: List<String>? = null
)

data class ActiveTimer(
    val id: Long,
    val name: String,
    val description: String,
    val remainingMs: Long,
    val job: Job
)

val timerTypes = listOf(
    TimerType("Productivity", 60, listOf("Guitar", "Programming", "Applying")),
    TimerType("Reading", 60),
    TimerType("Rest", 2)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimersScreen(
    onBack: () -> Unit,
    calendarManager: CalendarManager
) {
    val context = LocalContext.current
    
    var longVibration by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var customTimeEnabled by remember { mutableStateOf(false) }
    
    var showTimerDialog by remember { mutableStateOf(false) }
    var selectedTimerType by remember { mutableStateOf<TimerType?>(null) }
    var timerDurationMs by remember { mutableLongStateOf(0L) }
    
    var hasNotificationPermission by remember { mutableStateOf(true) }
    var activeTimers by remember { mutableStateOf(listOf<ActiveTimer>()) }
    var timerCounter by remember { mutableLongStateOf(0L) }
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    LaunchedEffect(activeTimers) {
        while (activeTimers.isNotEmpty()) {
            delay(1000)
            activeTimers = activeTimers.map { timer ->
                timer.copy(remainingMs = maxOf(0L, timer.remainingMs - 1000))
            }.filter { it.remainingMs > 0 }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timers & Tools") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Long Vibration")
                        }
                        Switch(
                            checked = longVibration,
                            onCheckedChange = { longVibration = it }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Notification")
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Custom Time")
                        }
                        Switch(
                            checked = customTimeEnabled,
                            onCheckedChange = { customTimeEnabled = it }
                        )
                    }
                    
                    if (!hasNotificationPermission && notificationsEnabled) {
                        OutlinedButton(
                            onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Notification Permission")
                        }
                    }
                }
            }

            if (activeTimers.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Timers",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Button(
                                onClick = { 
                                    activeTimers.forEach { it.job.cancel() }
                                    activeTimers = emptyList()
                                }
                            ) {
                                Text("Cancel All")
                            }
                        }
                        activeTimers.forEach { timer ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = timer.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${timer.description} - ${(timer.remainingMs / 1000 / 60)}m ${(timer.remainingMs / 1000 % 60)}s",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                TextButton(onClick = { 
                                    timer.job.cancel()
                                    activeTimers = activeTimers.filter { it.id != timer.id }
                                }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "Timers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(count = timerTypes.size) { index ->
                    val timerType = timerTypes[index]
                    Card(
                        onClick = {
                            selectedTimerType = timerType
                            timerDurationMs = timerType.defaultMinutes * 60 * 1000L
                            showTimerDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = timerType.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${timerType.defaultMinutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showTimerDialog && selectedTimerType != null) {
        TimerDialog(
            timerType = selectedTimerType!!,
            customTimeEnabled = customTimeEnabled,
            onDismiss = {
                showTimerDialog = false
                selectedTimerType = null
            },
            onStart = { description, customDurationMs ->
                showTimerDialog = false
                val duration = if (customTimeEnabled && customDurationMs > 0) customDurationMs else timerDurationMs
                val timerId = timerCounter++
                val job = startTimer(
                    context = context,
                    durationMs = duration,
                    timerName = selectedTimerType!!.name,
                    description = description,
                    longVibration = longVibration,
                    notificationsEnabled = notificationsEnabled,
                    calendarManager = calendarManager,
                    onComplete = {
                        activeTimers = activeTimers.filter { it.id != timerId }
                    }
                )
                activeTimers = activeTimers + ActiveTimer(
                    id = timerId,
                    name = selectedTimerType!!.name,
                    description = description.ifBlank { "Running" },
                    remainingMs = duration,
                    job = job
                )
                selectedTimerType = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerDialog(
    timerType: TimerType,
    customTimeEnabled: Boolean,
    onDismiss: () -> Unit,
    onStart: (description: String, customDurationMs: Long) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var customMinutes by remember { mutableStateOf(timerType.defaultMinutes.toString()) }
    var customMinutesFocused by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${timerType.name} Timer") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (timerType.quickDescriptions != null) {
                    Text(
                        text = "Quick options:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        timerType.quickDescriptions.chunked(2).forEach { rowItems ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { quickDesc ->
                                    FilterChip(
                                        selected = description == quickDesc,
                                        onClick = { description = quickDesc },
                                        label = { Text(quickDesc) }
                                    )
                                }
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                if (customTimeEnabled) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                customMinutes = newValue
                            }
                        },
                        label = { Text("Minutes") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = customMinutesFocused && (customMinutes.isEmpty() || customMinutes.toIntOrNull() == null || customMinutes.toInt() <= 0)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val minutes = if (customTimeEnabled) customMinutes.toIntOrNull() ?: timerType.defaultMinutes else timerType.defaultMinutes
                    onStart(description, minutes * 60 * 1000L)
                },
                enabled = !customTimeEnabled || (customMinutes.isNotEmpty() && (customMinutes.toIntOrNull() ?: 0) > 0)
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun startTimer(
    context: Context,
    durationMs: Long,
    timerName: String,
    description: String,
    longVibration: Boolean,
    notificationsEnabled: Boolean,
    calendarManager: CalendarManager,
    onComplete: () -> Unit
): Job {
    val scope = CoroutineScope(Dispatchers.Default)
    
    return scope.launch {
        var remainingMs = durationMs
        while (remainingMs > 0) {
            delay(1000)
            remainingMs -= 1000
        }
        
        val vibrator = getVibrator(context)
        val vibrationDuration = if (longVibration) 2000L else 500L
        vibrate(vibrator, vibrationDuration)
        
        if (notificationsEnabled) {
            showNotification(context, timerName, description)
        }
        
        val finalDescription = if (description.isNotBlank()) "$timerName: $description" else timerName
        calendarManager.createWorkoutEvent(
            title = finalDescription,
            date = LocalDate.now(),
            startHour = LocalTime.now().hour,
            startMinute = LocalTime.now().minute,
            durationMinutes = (durationMs / 60000).toInt(),
            description = "Timer completed via DoItAll"
        )
        
        onComplete()
    }
}

private fun getVibrator(context: Context): Vibrator {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
}

private fun vibrate(vibrator: Vibrator, durationMs: Long) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs)
    }
}

private fun showNotification(context: Context, timerName: String, description: String) {
    val channelId = "doitall_timers"
    val channelName = "DoItAll Timers"
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(
            channelId,
            channelName,
            android.app.NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    val contentText = if (description.isNotBlank()) "$timerName: $description" else "$timerName completed"
    
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Timer Complete")
        .setContentText(contentText)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    
    val notificationManager = NotificationManagerCompat.from(context)
    try {
        notificationManager.notify(1001, notification)
    } catch (e: SecurityException) {
    }
}