package com.mateoviscarra.doitall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mateoviscarra.doitall.data.WorkoutDay
import com.mateoviscarra.doitall.data.WorkoutPlan
import com.mateoviscarra.doitall.data.persist.ExerciseLogState
import com.mateoviscarra.doitall.data.persist.WeightUnit
import com.mateoviscarra.doitall.data.persist.WorkoutStateStore
import com.mateoviscarra.doitall.data.persist.defaultExerciseLog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVariationScreen(
    workoutPlan: WorkoutPlan,
    dayKey: String,
    workoutDay: WorkoutDay,
    slotIndex: Int,
    pageIndex: Int,
    store: WorkoutStateStore,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val slot = workoutDay.slots.getOrNull(slotIndex) ?: run {
        onBack()
        return
    }
    val optionIds = slot.optionIds()
    val scope = rememberCoroutineScope()

    var selectedExerciseId by remember { mutableStateOf<String?>(null) }
    var draftLog by remember { mutableStateOf<ExerciseLogState?>(null) }

    val locationsById = remember(workoutPlan) { workoutPlan.exerciseLocationsById() }

    LaunchedEffect(dayKey, slotIndex, pageIndex, workoutDay, workoutPlan) {
        val resolved = store.loadResolvedDay(dayKey, workoutDay, workoutPlan)
        val bindings = resolved.dayLog.slots[slotIndex.toString()]?.pageBindings ?: return@LaunchedEffect
        val id = bindings.getOrNull(pageIndex) ?: return@LaunchedEffect
        selectedExerciseId = id
    }

    LaunchedEffect(selectedExerciseId, dayKey, workoutDay, workoutPlan) {
        val id = selectedExerciseId ?: return@LaunchedEffect
        val resolved = store.loadResolvedDay(dayKey, workoutDay, workoutPlan)
        draftLog = resolved.exerciseLogs[id]
            ?: workoutPlan.catalog[id]?.let { defaultExerciseLog(it) }
    }

    if (selectedExerciseId == null || draftLog == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit variation") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Text("Loading…", modifier = Modifier.padding(padding))
        }
        return
    }

    val exerciseId = selectedExerciseId!!
    val def = workoutPlan.catalog[exerciseId]
    val isCardio = def?.duration != null

    val linkedOtherDays = locationsById[exerciseId]
        ?.filter { it.dayKey != dayKey }
        ?.map { it.dayKey }
        ?.distinct()
        ?.sorted()
        .orEmpty()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Edit variation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val toSave = draftLog ?: return@Button
                    if (!validate(toSave, isCardio)) return@Button
                    scope.launch {
                        store.updateExerciseLogAndBinding(
                            dayKey = dayKey,
                            day = workoutDay,
                            slotIndex = slotIndex,
                            pageIndex = pageIndex,
                            exerciseId = exerciseId,
                            log = sanitizeForSave(toSave, isCardio)
                        )
                        onSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Save")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (linkedOtherDays.isNotEmpty()) {
                Text(
                    text = "This exercise is also on: ${linkedOtherDays.joinToString()}. Edits update every day that uses it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Text(text = "Exercise", style = MaterialTheme.typography.labelLarge)
            ExerciseIdDropdown(
                optionIds = optionIds,
                selectedId = exerciseId,
                workoutPlan = workoutPlan,
                onSelectedId = { newId -> selectedExerciseId = newId }
            )

            val log = draftLog!!

            if (isCardio) {
                OutlinedTextField(
                    value = log.duration ?: "",
                    onValueChange = { v ->
                        draftLog = log.copy(duration = v.ifBlank { null })
                    },
                    label = { Text("Duration") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = log.intensity ?: "",
                    onValueChange = { v ->
                        draftLog = log.copy(intensity = v.ifBlank { null })
                    },
                    label = { Text("Intensity") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                SetsWeightRepsSection(
                    state = log,
                    onChange = { draftLog = it }
                )
            }

            OutlinedTextField(
                value = draftLog!!.comment,
                onValueChange = { v ->
                    draftLog = draftLog!!.copy(comment = v.take(ExerciseLogState.MAX_COMMENT_LENGTH))
                },
                label = { Text("Comment (max ${ExerciseLogState.MAX_COMMENT_LENGTH} chars)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                supportingText = {
                    Text("${draftLog!!.comment.length}/${ExerciseLogState.MAX_COMMENT_LENGTH}")
                }
            )

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseIdDropdown(
    optionIds: List<String>,
    selectedId: String,
    workoutPlan: WorkoutPlan,
    onSelectedId: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = workoutPlan.catalog[selectedId]?.name ?: selectedId
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            optionIds.forEach { id ->
                val name = workoutPlan.catalog[id]?.name ?: id
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelectedId(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SetsWeightRepsSection(
    state: ExerciseLogState,
    onChange: (ExerciseLogState) -> Unit
) {
    var setsText by remember(state.sets) { mutableStateOf(state.sets.toString()) }
    var weightText by remember(state.weight) { mutableStateOf(state.weight) }
    var singleRepText by remember(state.repsSingle) {
        mutableStateOf(state.repsSingle?.toString() ?: "")
    }

    OutlinedTextField(
        value = setsText,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }
            setsText = digits
            val n = digits.toIntOrNull()?.coerceIn(1, 99) ?: 1
            val newReps = if (state.usePerSetReps) {
                val old = state.repsPerSet ?: List(state.sets) { state.repsSingle ?: 0 }
                List(n) { i -> old.getOrElse(i) { old.lastOrNull() ?: 0 } }
            } else {
                state.repsPerSet
            }
            onChange(
                state.copy(
                    sets = n,
                    repsPerSet = newReps
                )
            )
        },
        label = { Text("Sets") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )

    RowCheckbox(
        label = "One rep value per set",
        checked = state.usePerSetReps,
        onCheckedChange = { checked ->
            val sets = state.sets
            val next = if (checked) {
                val base = state.repsSingle ?: 0
                val list = List(sets) { i -> state.repsPerSet?.getOrNull(i) ?: base }
                state.copy(
                    usePerSetReps = true,
                    repsPerSet = list,
                    repsSingle = null
                )
            } else {
                val single = state.repsPerSet?.firstOrNull() ?: state.repsSingle ?: 0
                state.copy(
                    usePerSetReps = false,
                    repsSingle = single,
                    repsPerSet = null
                )
            }
            onChange(next)
        }
    )

    if (state.usePerSetReps) {
        Text("Reps per set", style = MaterialTheme.typography.labelLarge)
        repeat(state.sets) { i ->
            val repVal = state.repsPerSet?.getOrNull(i) ?: 0
            var repI by remember(state.sets, state.usePerSetReps, i, repVal) {
                mutableIntStateOf(repVal)
            }
            OutlinedTextField(
                value = repI.toString(),
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(4)
                    repI = digits.toIntOrNull() ?: 0
                    val list = (state.repsPerSet ?: List(state.sets) { 0 }).toMutableList()
                    while (list.size < state.sets) list.add(0)
                    if (i < list.size) list[i] = repI
                    onChange(state.copy(repsPerSet = list))
                },
                label = { Text("Set ${i + 1} reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        OutlinedTextField(
            value = singleRepText,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(4)
                singleRepText = digits
                val n = if (digits.isEmpty()) null else digits.toIntOrNull()
                onChange(state.copy(repsSingle = n, repsPerSet = null))
            },
            label = { Text("Reps") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Feature 1: Weight field — label changes based on bodyweight
    OutlinedTextField(
        value = weightText,
        onValueChange = { v ->
            weightText = v
            onChange(state.copy(weight = v))
        },
        label = {
            Text(
                if (state.isBodyweight) "Added weight (leave blank for bodyweight only)"
                else "Weight"
            )
        },
        modifier = Modifier.fillMaxWidth()
    )

    // Feature 1: Bodyweight checkbox
    RowCheckbox(
        label = "Bodyweight exercise",
        checked = state.isBodyweight,
        onCheckedChange = { checked ->
            onChange(state.copy(isBodyweight = checked))
        }
    )

    // Feature 1: kg / lbs toggle using FilterChip — only show if weight doesn't already have a unit
    val hasWeightUnit = weightText.contains(Regex("""(?i)(kg|kgs|lbs?|each|per ?side)"""))
    if (!hasWeightUnit) {
        Text(text = "Unit", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.weightUnit == WeightUnit.KG,
                onClick = { onChange(state.copy(weightUnit = WeightUnit.KG)) },
                label = { Text("kg") }
            )
            FilterChip(
                selected = state.weightUnit == WeightUnit.LBS,
                onClick = { onChange(state.copy(weightUnit = WeightUnit.LBS)) },
                label = { Text("lbs") }
            )
        }
    }
}

@Composable
private fun RowCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun validate(log: ExerciseLogState, isCardio: Boolean): Boolean {
    if (isCardio) return true
    if (log.usePerSetReps) {
        val list = log.repsPerSet
        return list != null && list.size == log.sets
    }
    return true
}

private fun sanitizeForSave(log: ExerciseLogState, isCardio: Boolean): ExerciseLogState {
    val comment = log.comment.take(ExerciseLogState.MAX_COMMENT_LENGTH)
    return if (isCardio) {
        log.copy(comment = comment)
    } else if (log.usePerSetReps) {
        log.copy(comment = comment, repsSingle = null)
    } else {
        log.copy(comment = comment, repsPerSet = null)
    }
}
