package com.mateoviscarra.doitall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mateoviscarra.doitall.data.WorkoutDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutListScreen(
    workoutDays: List<WorkoutDay>,
    onDaySelected: (index: Int) -> Unit,
    onSettingsClick: () -> Unit,
    onCustomClick: () -> Unit
) {
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
        }
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
