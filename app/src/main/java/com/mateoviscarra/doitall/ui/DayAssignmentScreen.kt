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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit: ${day.day}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exercises.forEachIndexed { index, dayEx ->
                    val exercise = allExercises.find { it.id == dayEx.exerciseId }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                        IconButton(onClick = { 
                            exercises = exercises.toMutableList().also { it.removeAt(index) }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
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