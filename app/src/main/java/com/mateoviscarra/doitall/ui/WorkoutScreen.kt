package com.mateoviscarra.doitall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mateoviscarra.doitall.data.WorkoutDay

@Composable
fun WorkoutScreen(workoutDays: List<WorkoutDay>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Workout Tracker",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Hardcoded plan - V1",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(workoutDays) { day ->
            WorkoutDayCard(day)
        }
    }
}

@Composable
private fun WorkoutDayCard(day: WorkoutDay) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = day.day,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            val muscleText = if (day.muscleGroups.isEmpty()) {
                if (day.isRestDay) "Rest day" else "General"
            } else {
                day.muscleGroups.joinToString()
            }

            Text(
                text = "Muscles: $muscleText",
                style = MaterialTheme.typography.bodyMedium
            )

            if (day.exercises.isEmpty()) {
                Text(
                    text = "No exercises for this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                day.exercises.take(3).forEach { exercise ->
                    Text(
                        text = "- ${exercise.name}: ${exercise.sets} sets x ${exercise.reps} reps (${exercise.load})",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (day.exercises.size > 3) {
                    Text(
                        text = "+ ${day.exercises.size - 3} more exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
