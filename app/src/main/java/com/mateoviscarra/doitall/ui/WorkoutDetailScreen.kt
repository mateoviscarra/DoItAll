package com.mateoviscarra.doitall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mateoviscarra.doitall.data.WorkoutDay
import com.mateoviscarra.doitall.data.WorkoutExercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutDay: WorkoutDay,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = workoutDay.day,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val muscleText = if (workoutDay.muscleGroups.isEmpty()) {
                    if (workoutDay.isRestDay) "Rest day" else "General"
                } else {
                    workoutDay.muscleGroups.joinToString()
                }
                Text(
                    text = "Muscle groups: $muscleText",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (workoutDay.notes.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        workoutDay.notes.forEach { note ->
                            Text(
                                text = "• $note",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (workoutDay.exercises.isEmpty()) {
                item {
                    Text(
                        text = if (workoutDay.isRestDay) {
                            "No exercises — enjoy your rest."
                        } else {
                            "No exercises listed for this day."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                itemsIndexed(workoutDay.exercises) { i, exercise ->
                    ExerciseCard(index = i + 1, exercise = exercise)
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(index: Int, exercise: WorkoutExercise) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$index. ${exercise.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (exercise.duration != null) {
                Text(
                    text = "Duration: ${exercise.duration}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (exercise.intensity != null) {
                    Text(
                        text = "Intensity: ${exercise.intensity}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = "${exercise.sets} sets × ${exercise.reps} reps",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Load: ${exercise.load}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (exercise.alternatives.isNotEmpty()) {
                Text(
                    text = "Alternatives:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                exercise.alternatives.forEach { alt ->
                    Text(
                        text = "• $alt",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
