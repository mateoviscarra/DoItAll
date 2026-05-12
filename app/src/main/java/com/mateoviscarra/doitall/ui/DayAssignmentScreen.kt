package com.mateoviscarra.doitall.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mateoviscarra.doitall.data.CategoryExercise
import com.mateoviscarra.doitall.data.DayExercise
import com.mateoviscarra.doitall.data.NewWorkoutDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayAssignmentScreen(
    days: List<NewWorkoutDay>,
    allExercises: List<CategoryExercise>,
    onBack: () -> Unit,
    onUpdateDay: (NewWorkoutDay) -> Unit
) {
    var selectedDay by remember { mutableStateOf<NewWorkoutDay?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Day Assignments") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(days) { day ->
                DayCard(
                    day = day,
                    allExercises = allExercises,
                    onClick = { selectedDay = day }
                )
            }
        }
    }
    
    selectedDay?.let { day ->
        EditDayDialog(
            day = day,
            allExercises = allExercises,
            onDismiss = { selectedDay = null },
            onSave = { updatedDay ->
                onUpdateDay(updatedDay)
                selectedDay = null
            }
        )
    }
}

@Composable
private fun DayCard(
    day: NewWorkoutDay,
    allExercises: List<CategoryExercise>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.day,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (day.isRestDay) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Rest Day",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            if (day.muscleGroups.isNotEmpty()) {
                Text(
                    text = day.muscleGroups.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "${day.exercises.size} exercises",
                style = MaterialTheme.typography.bodyMedium
            )
            
            day.exercises.forEach { dayEx ->
                val exercise = allExercises.find { it.id == dayEx.exerciseId }
                Text(
                    text = "• ${exercise?.name ?: dayEx.exerciseId}" +
                            if (dayEx.alternatives.isNotEmpty()) " (${dayEx.alternatives.size} alternatives)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EditDayDialog(
    day: NewWorkoutDay,
    allExercises: List<CategoryExercise>,
    onDismiss: () -> Unit,
    onSave: (NewWorkoutDay) -> Unit
) {
    var exercises by remember { mutableStateOf(day.exercises) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var editingExerciseIndex by remember { mutableStateOf<Int?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit: ${day.day}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exercises.forEachIndexed { index, dayEx ->
                    val exercise = allExercises.find { it.id == dayEx.exerciseId }
                    Surface(
                        onClick = { editingExerciseIndex = index },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise?.name ?: dayEx.exerciseId,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (dayEx.alternatives.isNotEmpty()) {
                                    Text(
                                        text = "Alts: ${dayEx.alternatives.joinToString { altId -> 
                                            allExercises.find { it.id == altId }?.name ?: altId 
                                        }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row {
                                IconButton(onClick = { 
                                    exercises = exercises.toMutableList().also { it.removeAt(index) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                
                TextButton(onClick = { showAddExerciseDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Exercise")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                onSave(day.copy(exercises = exercises)) 
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    if (showAddExerciseDialog) {
        SelectExerciseDialog(
            exercises = allExercises,
            onSelect = { exerciseId ->
                exercises = exercises + DayExercise(exerciseId = exerciseId)
                showAddExerciseDialog = false
            },
            onDismiss = { showAddExerciseDialog = false }
        )
    }
    
    editingExerciseIndex?.let { index ->
        val dayEx = exercises[index]
        val mainExercise = allExercises.find { it.id == dayEx.exerciseId }
        ManageAlternativesDialog(
            mainExerciseName = mainExercise?.name ?: dayEx.exerciseId,
            alternatives = dayEx.alternatives,
            allExercises = allExercises,
            onAlternativesChanged = { newAlternatives ->
                exercises = exercises.toMutableList().also {
                    it[index] = dayEx.copy(alternatives = newAlternatives)
                }
            },
            onDismiss = { editingExerciseIndex = null }
        )
    }
}

@Composable
private fun SelectExerciseDialog(
    exercises: List<CategoryExercise>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredExercises = exercises.filter { 
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(filteredExercises) { exercise ->
                        TextButton(
                            onClick = { onSelect(exercise.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(exercise.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ManageAlternativesDialog(
    mainExerciseName: String,
    alternatives: List<String>,
    allExercises: List<CategoryExercise>,
    onAlternativesChanged: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddAlternativeDialog by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alternatives: $mainExerciseName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (alternatives.isEmpty()) {
                    Text(
                        text = "No alternatives added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    alternatives.forEach { altId ->
                        val altExercise = allExercises.find { it.id == altId }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = altExercise?.name ?: altId,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { 
                                onAlternativesChanged(alternatives - altId)
                            }) {
                                Icon(
                                    Icons.Default.Delete, 
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                Divider()
                
                TextButton(
                    onClick = { showAddAlternativeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Alternative")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {}
    )
    
    if (showAddAlternativeDialog) {
        SelectExerciseDialog(
            exercises = allExercises.filter { ex -> 
                ex.name != mainExerciseName && ex.id !in alternatives 
            },
            onSelect = { exerciseId ->
                onAlternativesChanged(alternatives + exerciseId)
                showAddAlternativeDialog = false
            },
            onDismiss = { showAddAlternativeDialog = false }
        )
    }
}