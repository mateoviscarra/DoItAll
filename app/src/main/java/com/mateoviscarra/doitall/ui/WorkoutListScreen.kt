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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mateoviscarra.doitall.calendar.CalendarManager
import com.mateoviscarra.doitall.data.WorkoutDay
import com.mateoviscarra.doitall.data.persist.TimerStateStore
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
    val startedAtEpochMs: Long,
    val job: Job? = null
)

val timerTypes = listOf(
    TimerType("Productivity", 60, listOf("Guitar", "Programming", "Applying")),
    TimerType("Reading", 60),
    TimerType("Rest", 2)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutListScreen(
    workoutDays: List<WorkoutDay>,
    onDaySelected: (index: Int) -> Unit,
    onSettingsClick: () -> Unit,
    onCustomClick: () -> Unit,
    timerStore: TimerStateStore,
    calendarManager: CalendarManager
) {
    val context = LocalContext.current
    
    var longVibration by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var customTimeEnabled by remember { mutableStateOf(false) }
    var onlyToCalendar by remember { mutableStateOf(false) }
    
    var showTimerDialog by remember { mutableStateOf(false) }
    var selectedTimerType by remember { mutableStateOf<TimerType?>(null) }
    var timerDurationMs by remember { mutableLongStateOf(0L) }
    
    var hasNotificationPermission by remember { mutableStateOf(true) }
    var activeTimers by remember { mutableStateOf(listOf<ActiveTimer>()) }
    var timerCounter by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }
    
    // Load persisted timers on first composition
    LaunchedEffect(Unit) {
        val (persistedTimers, counter) = timerStore.loadActiveTimers()
        timerCounter = counter
        
        // Recalculate remaining time based on how much has passed
        val now = System.currentTimeMillis()
        activeTimers = persistedTimers.mapNotNull { persisted ->
            val elapsed = now - persisted.startedAtEpochMs
            val remaining = persisted.remainingMs - elapsed
            if (remaining > 0) {
                ActiveTimer(
                    id = persisted.id,
                    name = persisted.name,
                    description = persisted.description,
                    remainingMs = remaining,
                    startedAtEpochMs = persisted.startedAtEpochMs
                )
            } else null
        }
    }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    // Timer countdown and persistence
    LaunchedEffect(activeTimers) {
        while (activeTimers.isNotEmpty()) {
            delay(1000)
            val now = System.currentTimeMillis()
            activeTimers = activeTimers.map { timer ->
                val newRemaining = maxOf(0L, timer.remainingMs - 1000)
                timer.copy(remainingMs = newRemaining)
            }.filter { it.remainingMs > 0 }
            
            // Persist active timers
            val toPersist = activeTimers.map { timer ->
                TimerStateStore.PersistedTimer(
                    id = timer.id,
                    name = timer.name,
                    description = timer.description,
                    remainingMs = timer.remainingMs,
                    startedAtEpochMs = timer.startedAtEpochMs
                )
            }
            timerStore.saveActiveTimers(toPersist, timerCounter)
        }
        
        // Clear persisted when all done
        if (activeTimers.isEmpty()) {
            scope.launch { timerStore.clearTimers() }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Workout Tracker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hardcoded plan — V1",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Button(
                    onClick = onCustomClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Custom Workout",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Push & Pull",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val pushDays = listOf("Push A", "Push B")
                        val pullDays = listOf("Pull A", "Pull B")
                        val legDays = listOf("Legs A", "Legs B")
                        val accessoryDays = listOf("Abs", "Cardio", "Stretches")

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pushDays.forEach { dayName ->
                                workoutDays.find { it.day == dayName }?.let { day ->
                                    val index = workoutDays.indexOf(day)
                                    WorkoutButton(
                                        day = day,
                                        onClick = { onDaySelected(index) }
                                    )
                                }
                            }
                            accessoryDays.getOrNull(0)?.let { dayName ->
                                workoutDays.find { it.day == dayName }?.let { day ->
                                    val index = workoutDays.indexOf(day)
                                    WorkoutButton(
                                        day = day,
                                        onClick = { onDaySelected(index) },
                                        isAccent = true
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pullDays.forEach { dayName ->
                                workoutDays.find { it.day == dayName }?.let { day ->
                                    val index = workoutDays.indexOf(day)
                                    WorkoutButton(
                                        day = day,
                                        onClick = { onDaySelected(index) }
                                    )
                                }
                            }
                            accessoryDays.getOrNull(1)?.let { dayName ->
                                workoutDays.find { it.day == dayName }?.let { day ->
                                    val index = workoutDays.indexOf(day)
                                    WorkoutButton(
                                        day = day,
                                        onClick = { onDaySelected(index) },
                                        isAccent = true
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            legDays.forEach { dayName ->
                                workoutDays.find { it.day == dayName }?.let { day ->
                                    val index = workoutDays.indexOf(day)
                                    WorkoutButton(
                                        day = day,
                                        onClick = { onDaySelected(index) }
                                    )
                                }
                            }
                            accessoryDays.getOrNull(2)?.let { dayName ->
                                workoutDays.find { it.day == dayName }?.let { day ->
                                    val index = workoutDays.indexOf(day)
                                    WorkoutButton(
                                        day = day,
                                        onClick = { onDaySelected(index) },
                                        isAccent = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Active timers section - now below the buttons
            if (activeTimers.isNotEmpty()) {
                item {
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
                                        activeTimers.forEach { it.job?.cancel() }
                                        activeTimers = emptyList()
                                        scope.launch { timerStore.clearTimers() }
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
                                        timer.job?.cancel()
                                        activeTimers = activeTimers.filter { it.id != timer.id }
                                    }) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Timers & Tools title and buttons
            item {
                Text(
                    text = "Timers & Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Timer settings card
            item {
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Only to Calendar")
                            }
                            Switch(
                                checked = onlyToCalendar,
                                onCheckedChange = { onlyToCalendar = it }
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
            }

            // Timer buttons grid
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(220.dp)
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
    }
    
    // Timer dialog
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
                
                if (onlyToCalendar) {
                    val timerName = selectedTimerType!!.name
                    if (timerName == "Rest") {
                        // Rest timers don't create calendar events
                    } else {
                        val finalDescription = if (description.isNotBlank()) "$timerName: $description" else timerName
                        scope.launch {
                            val result = calendarManager.createWorkoutEvent(
                                title = finalDescription,
                                date = LocalDate.now(),
                                startHour = LocalTime.now().hour,
                                startMinute = LocalTime.now().minute,
                                durationMinutes = (duration / 60000).toInt(),
                                description = "Logged via DoItAll"
                            )
                            result.onFailure { e ->
                                android.util.Log.e("WorkoutList", "Calendar event failed: ${e.message}")
                            }
                        }
                    }
                } else {
                    val timerId = timerCounter++
                    val timerName = selectedTimerType!!.name
                    val timerDescription = description.ifBlank { "Running" }
                    
                    // Create calendar event immediately when timer starts (except for Rest)
                    if (timerName != "Rest") {
                        val finalDescription = if (description.isNotBlank()) "$timerName: $description" else timerName
                        scope.launch {
                            val result = calendarManager.createWorkoutEvent(
                                title = finalDescription,
                                date = LocalDate.now(),
                                startHour = LocalTime.now().hour,
                                startMinute = LocalTime.now().minute,
                                durationMinutes = (duration / 60000).toInt(),
                                description = "Timer started via DoItAll"
                            )
                            result.onFailure { e ->
                                android.util.Log.e("WorkoutList", "Calendar event failed: ${e.message}")
                            }
                        }
                    }
                    
                    val job = startTimer(
                        context = context,
                        durationMs = duration,
                        timerName = timerName,
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
                        name = timerName,
                        description = timerDescription,
                        remainingMs = duration,
                        startedAtEpochMs = System.currentTimeMillis(),
                        job = job
                    )
                }
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
        
        onComplete()
    }
}

private fun startTimerNoAlert(
    context: Context,
    durationMs: Long,
    timerName: String,
    description: String,
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

@Composable
private fun WorkoutButton(
    day: WorkoutDay,
    onClick: () -> Unit,
    isAccent: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAccent) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Text(
            text = day.day,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            color = if (isAccent) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}