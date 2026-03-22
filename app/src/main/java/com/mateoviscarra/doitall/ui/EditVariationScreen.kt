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
import com.mateoviscarra.doitall.data.WorkoutExercise
import com.mateoviscarra.doitall.data.catalogOptionNames
import com.mateoviscarra.doitall.data.persist.PageLogState
import com.mateoviscarra.doitall.data.persist.WorkoutStateStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVariationScreen(
    dayKey: String,
    exercises: List<WorkoutExercise>,
    slotIndex: Int,
    pageIndex: Int,
    store: WorkoutStateStore,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val template = exercises.getOrNull(slotIndex) ?: run {
        onBack()
        return
    }
    val catalog = template.catalogOptionNames()

    val scope = rememberCoroutineScope()

    var pageState by remember(dayKey, slotIndex, pageIndex) {
        mutableStateOf<PageLogState?>(null)
    }

    LaunchedEffect(dayKey, slotIndex, pageIndex, exercises) {
        val day = store.dayLogFlow(dayKey, exercises).first()
        pageState = day.slots[slotIndex.toString()]?.pages?.getOrNull(pageIndex)
    }

    if (pageState == null) {
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
                    val toSave = pageState ?: return@Button
                    if (!validate(toSave)) return@Button
                    scope.launch {
                        store.updatePageLog(
                            dayKey = dayKey,
                            exercises = exercises,
                            slotIndex = slotIndex,
                            pageIndex = pageIndex,
                            page = sanitize(toSave)
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
        EditVariationForm(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            catalog = catalog,
            state = pageState!!,
            onStateChange = { pageState = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditVariationForm(
    modifier: Modifier,
    catalog: List<String>,
    state: PageLogState,
    onStateChange: (PageLogState) -> Unit
) {
    LaunchedEffect(catalog, state.selectedExerciseName) {
        if (state.selectedExerciseName !in catalog && catalog.isNotEmpty()) {
            onStateChange(state.copy(selectedExerciseName = catalog.first()))
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Exercise",
            style = MaterialTheme.typography.labelLarge
        )
        ExerciseDropdown(
            catalog = catalog,
            selected = state.selectedExerciseName,
            onSelected = { name ->
                onStateChange(state.copy(selectedExerciseName = name))
            }
        )

        if (state.isCardio) {
            OutlinedTextField(
                value = state.duration ?: "",
                onValueChange = { v ->
                    onStateChange(state.copy(duration = v.ifBlank { null }))
                },
                label = { Text("Duration") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.intensity ?: "",
                onValueChange = { v ->
                    onStateChange(state.copy(intensity = v.ifBlank { null }))
                },
                label = { Text("Intensity") },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            SetsWeightRepsSection(
                state = state,
                onChange = onStateChange
            )
        }

        OutlinedTextField(
            value = state.comment,
            onValueChange = { v ->
                val clipped = v.take(PageLogState.MAX_COMMENT_LENGTH)
                onStateChange(state.copy(comment = clipped))
            },
            label = { Text("Comment (max ${PageLogState.MAX_COMMENT_LENGTH} chars)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            supportingText = {
                Text("${state.comment.length}/${PageLogState.MAX_COMMENT_LENGTH}")
            }
        )

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDropdown(
    catalog: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
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
            catalog.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelected(name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SetsWeightRepsSection(
    state: PageLogState,
    onChange: (PageLogState) -> Unit
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

    OutlinedTextField(
        value = weightText,
        onValueChange = { v ->
            weightText = v
            onChange(state.copy(weight = v))
        },
        label = { Text("Weight (one value for all sets)") },
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
                val n = digits.toIntOrNull() ?: 0
                onChange(state.copy(repsSingle = n, repsPerSet = null))
            },
            label = { Text("Reps") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RowCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun validate(page: PageLogState): Boolean {
    if (page.isCardio) return true
    if (page.usePerSetReps) {
        val list = page.repsPerSet
        return list != null && list.size == page.sets
    }
    return true
}

private fun sanitize(page: PageLogState): PageLogState {
    val comment = page.comment.take(PageLogState.MAX_COMMENT_LENGTH)
    return if (page.isCardio) {
        page.copy(comment = comment)
    } else if (page.usePerSetReps) {
        page.copy(
            comment = comment,
            repsSingle = null
        )
    } else {
        page.copy(
            comment = comment,
            repsPerSet = null
        )
    }
}
