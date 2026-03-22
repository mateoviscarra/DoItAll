package com.mateoviscarra.doitall.ui

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mateoviscarra.doitall.calendar.CalendarManager
import com.mateoviscarra.doitall.data.persist.WeightUnit
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CustomExercise(
    val name: String,
    val sets: Int,
    val reps: Int,
    val weight: String,
    val isBodyweight: Boolean,
    val weightUnit: WeightUnit,
    val isDone: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomWorkoutScreen(
    calendarManager: CalendarManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val exercises = remember { mutableStateListOf<CustomExercise>() }
    var isLoading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    
    val completedCount = exercises.count { it.isDone }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Custom Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (exercises.isNotEmpty()) {
                        IconButton(
                            onClick = { showScheduleDialog = true },
                            enabled = !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Add to Calendar"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (exercises.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No exercises yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap the button below to add exercises",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            itemsIndexed(exercises) { index, exercise ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (exercise.isDone) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = {
                                exercises[index] = exercise.copy(isDone = !exercise.isDone)
                            }) {
                                Icon(
                                    imageVector = if (exercise.isDone) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Outlined.CheckCircle
                                    },
                                    contentDescription = if (exercise.isDone) "Mark as not done" else "Mark as done",
                                    tint = if (exercise.isDone) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = if (exercise.isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.alpha(if (exercise.isDone) 0.6f else 1f)
                                )
                                Text(
                                    text = "${exercise.sets} sets × ${exercise.reps} reps" +
                                            if (exercise.weight.isNotEmpty()) {
                                                " @ ${exercise.weight}" +
                                                if (exercise.isBodyweight) " Bodyweight" else " ${exercise.weightUnit.name.lowercase()}"
                                            } else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.alpha(if (exercise.isDone) 0.6f else 1f)
                                )
                            }
                        }
                        IconButton(onClick = { exercises.removeAt(index) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove exercise",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            item {
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Add Exercise")
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { exercise ->
                exercises.add(exercise)
                showAddDialog = false
            }
        )
    }
    
    if (showScheduleDialog) {
        WorkoutScheduleDialog(
            onDismiss = { showScheduleDialog = false },
            onConfirm = { config ->
                showScheduleDialog = false
                isLoading = true
                scope.launch {
                    val title = "Custom Workout"
                    val description = buildString {
                        exercises.forEachIndexed { index, exercise ->
                            append("${exercise.name}: ${exercise.sets} sets × ${exercise.reps} reps")
                            if (exercise.weight.isNotEmpty()) {
                                append(" @ ${exercise.weight}")
                                if (exercise.isBodyweight) append(" Bodyweight")
                            }
                            if (index < exercises.size - 1) append("\n")
                        }
                    }
                    
                    val (date, startHour, startMinute) = if (config.useCustomTime) {
                        Triple(config.customDate, config.customStartTime.hour, config.customStartTime.minute)
                    } else {
                        val now = LocalTime.now()
                        if (config.useCurrentTimeAs == CurrentTimeReference.END) {
                            val startTime = now.minusMinutes(config.durationMinutes.toLong())
                            Triple(LocalDate.now(), startTime.hour, startTime.minute)
                        } else {
                            Triple(LocalDate.now(), now.hour, now.minute)
                        }
                    }
                    
                    val result = calendarManager.createWorkoutEvent(
                        title = title,
                        date = date,
                        startHour = startHour,
                        startMinute = startMinute,
                        durationMinutes = config.durationMinutes,
                        description = description
                    )
                    result.fold(
                        onSuccess = {
                            snackbarHostState.showSnackbar("Added to Google Calendar!")
                        },
                        onFailure = {
                            snackbarHostState.showSnackbar("Failed: ${it.message}")
                        }
                    )
                    isLoading = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onAdd: (CustomExercise) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var sets by remember { mutableIntStateOf(3) }
    var reps by remember { mutableIntStateOf(10) }
    var weight by remember { mutableStateOf("") }
    var isBodyweight by remember { mutableStateOf(false) }
    var weightUnit by remember { mutableStateOf(WeightUnit.KG) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exercise") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    text = "Sets: $sets",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = sets.toFloat(),
                    onValueChange = { sets = it.toInt() },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Reps: $reps",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = reps.toFloat(),
                    onValueChange = { reps = it.toInt() },
                    valueRange = 1f..30f,
                    steps = 28,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isBodyweight,
                        onCheckedChange = { isBodyweight = it }
                    )
                    Text("Bodyweight exercise")
                }
                
                if (!isBodyweight) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = weightUnit == WeightUnit.KG,
                            onClick = { weightUnit = WeightUnit.KG },
                            label = { Text("kg") }
                        )
                        FilterChip(
                            selected = weightUnit == WeightUnit.LBS,
                            onClick = { weightUnit = WeightUnit.LBS },
                            label = { Text("lbs") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            CustomExercise(
                                name = name.trim(),
                                sets = sets,
                                reps = reps,
                                weight = weight.trim(),
                                isBodyweight = isBodyweight,
                                weightUnit = weightUnit
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
