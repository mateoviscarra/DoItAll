package com.mateoviscarra.doitall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class CurrentTimeReference {
    START, END
}

data class WorkoutScheduleConfig(
    val useCurrentTimeAs: CurrentTimeReference,
    val durationMinutes: Int,
    val useCustomTime: Boolean,
    val customDate: LocalDate,
    val customStartTime: LocalTime
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScheduleDialog(
    onDismiss: () -> Unit,
    onConfirm: (WorkoutScheduleConfig) -> Unit
) {
    var useCustomTime by remember { mutableStateOf(false) }
    var currentTimeReference by remember { mutableStateOf(CurrentTimeReference.END) }
    var durationMinutes by remember { mutableIntStateOf(60) }

    var customDate by remember { mutableStateOf(LocalDate.now()) }
    var customStartTime by remember { mutableStateOf(LocalTime.now()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Workout") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Duration: $durationMinutes minutes",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = durationMinutes.toFloat(),
                    onValueChange = { durationMinutes = it.toInt() },
                    valueRange = 15f..180f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Set custom time/date",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Checkbox(
                        checked = useCustomTime,
                        onCheckedChange = { useCustomTime = it }
                    )
                }

                if (useCustomTime) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Date: ${customDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = { showDatePicker = true }) {
                                Text("Change Date")
                            }

                            Text(
                                text = "Start Time: ${customStartTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = { showTimePicker = true }) {
                                Text("Change Time")
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Current time is the:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Column(Modifier.selectableGroup()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = currentTimeReference == CurrentTimeReference.START,
                                        onClick = { currentTimeReference = CurrentTimeReference.START },
                                        role = Role.RadioButton
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentTimeReference == CurrentTimeReference.START,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start of workout")
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = currentTimeReference == CurrentTimeReference.END,
                                        onClick = { currentTimeReference = CurrentTimeReference.END },
                                        role = Role.RadioButton
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentTimeReference == CurrentTimeReference.END,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("End of workout")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        WorkoutScheduleConfig(
                            useCurrentTimeAs = currentTimeReference,
                            durationMinutes = durationMinutes,
                            useCustomTime = useCustomTime,
                            customDate = customDate,
                            customStartTime = customStartTime
                        )
                    )
                }
            ) {
                Text("Add to Calendar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            customDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = customStartTime.hour,
            initialMinute = customStartTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        customStartTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
