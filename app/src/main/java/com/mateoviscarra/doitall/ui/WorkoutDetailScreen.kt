package com.mateoviscarra.doitall.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mateoviscarra.doitall.calendar.CalendarManager
import com.mateoviscarra.doitall.data.ExerciseLocation
import com.mateoviscarra.doitall.data.WorkoutDay
import com.mateoviscarra.doitall.data.WorkoutPlan
import com.mateoviscarra.doitall.data.WorkoutSlot
import com.mateoviscarra.doitall.data.persist.ExerciseLogState
import com.mateoviscarra.doitall.data.persist.SlotLogState
import com.mateoviscarra.doitall.data.persist.WeightUnit
import com.mateoviscarra.doitall.data.persist.WorkoutStateStore
import com.mateoviscarra.doitall.data.persist.defaultDayLog
import com.mateoviscarra.doitall.data.persist.defaultExerciseLog
import com.mateoviscarra.doitall.data.persist.mergedExerciseLogs
import com.mateoviscarra.doitall.data.persist.PersistRootV2
import com.mateoviscarra.doitall.data.persist.ResolvedDayUi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkoutDetailScreen(
    workoutPlan: WorkoutPlan,
    dayKey: String,
    workoutDay: WorkoutDay,
    onBack: () -> Unit,
    onEditVariation: (slotIndex: Int, pageIndex: Int) -> Unit,
    calendarManager: CalendarManager
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(dayKey) { WorkoutStateStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isSyncing by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    val initialResolved = remember(workoutDay, workoutPlan) {
        ResolvedDayUi(
            dayLog = defaultDayLog(workoutDay),
            exerciseLogs = mergedExerciseLogs(workoutPlan, PersistRootV2(emptyMap(), emptyMap()))
        )
    }

    val resolved by store.resolvedDayFlow(dayKey, workoutDay, workoutPlan).collectAsStateWithLifecycle(
        initialValue = initialResolved
    )

    val locationsById = remember(workoutPlan) { workoutPlan.exerciseLocationsById() }

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
                },
                actions = {
                    // Calendar sync button
                    if (workoutDay.slots.isNotEmpty() && !workoutDay.isRestDay) {
                        IconButton(
                            onClick = {
                                if (!calendarManager.isConnected()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please connect Google Calendar in Settings first")
                                    }
                                    return@IconButton
                                }
                                showScheduleDialog = true
                            },
                            enabled = !isSyncing
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Add to Calendar"
                            )
                        }
                    }
                    // Uncheck All button — only shown when there are done exercises
                    if (resolved.dayLog.doneExercises.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    store.uncheckAllForDay(dayKey, workoutDay)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Uncheck all exercises"
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

            if (workoutDay.slots.isEmpty()) {
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
                itemsIndexed(workoutDay.slots) { slotIndex, slot ->
                    val slotState = resolved.dayLog.slots[slotIndex.toString()]
                        ?: return@itemsIndexed
                    ExerciseSlotRow(
                        workoutPlan = workoutPlan,
                        dayKey = dayKey,
                        slot = slot,
                        slotState = slotState,
                        exerciseLogs = resolved.exerciseLogs,
                        locationsById = locationsById,
                        doneExercises = resolved.dayLog.doneExercises,
                        onPageSelected = { page ->
                            scope.launch {
                                store.updateSelectedPage(
                                    dayKey = dayKey,
                                    day = workoutDay,
                                    slotIndex = slotIndex,
                                    page = page
                                )
                            }
                        },
                        onEdit = { pageIndex ->
                            onEditVariation(slotIndex, pageIndex)
                        },
                        onToggleDone = { exerciseId ->
                            scope.launch {
                                store.toggleExerciseDone(dayKey, workoutDay, exerciseId)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showScheduleDialog) {
        WorkoutScheduleDialog(
            onDismiss = { showScheduleDialog = false },
            onConfirm = { config ->
                showScheduleDialog = false
                isSyncing = true
                scope.launch {
                    val exerciseNames = workoutDay.slots.mapNotNull { slot ->
                        val exerciseId = slot.exerciseIds.firstOrNull()
                        val def = exerciseId?.let { workoutPlan.catalog[it] }
                        def?.name
                    }.take(3)
                    val title = "$dayKey: ${exerciseNames.joinToString(", ")}"
                    val description = "${workoutDay.slots.size} exercises"

                    val (date, startHour, startMinute) = if (config.useCustomTime) {
                        Triple(config.customDate, config.customStartTime.hour, config.customStartTime.minute)
                    } else {
                        val now = LocalTime.now()
                        if (config.useCurrentTimeAs == CurrentTimeReference.END) {
                            val endTime = now
                            val startTime = endTime.minusMinutes(config.durationMinutes.toLong())
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
                            snackbarHostState.showSnackbar("Failed to sync: ${it.message}")
                        }
                    )
                    isSyncing = false
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseSlotRow(
    workoutPlan: WorkoutPlan,
    dayKey: String,
    slot: WorkoutSlot,
    slotState: SlotLogState,
    exerciseLogs: Map<String, ExerciseLogState>,
    locationsById: Map<String, List<ExerciseLocation>>,
    doneExercises: Set<String>,
    onPageSelected: (page: Int) -> Unit,
    onEdit: (pageIndex: Int) -> Unit,
    onToggleDone: (exerciseId: String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val pageCount = slot.exerciseIds.size
        if (pageCount == 0) return

        val pagerState = rememberPagerState(
            initialPage = slotState.selectedPage.coerceIn(0, pageCount - 1),
            pageCount = { pageCount }
        )

        LaunchedEffect(slotState.selectedPage, pageCount) {
            val target = slotState.selectedPage.coerceIn(0, pageCount - 1)
            if (pagerState.currentPage != target) {
                pagerState.scrollToPage(target)
            }
        }

        LaunchedEffect(pagerState, slotState.selectedPage) {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { page ->
                    if (page != slotState.selectedPage) {
                        onPageSelected(page)
                    }
                }
        }

        // Feature 2+3: Equal-height cards + edge peek for adjacent items
        // contentPadding creates the peek gap; height(IntrinsicSize.Max) equalises card heights
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 12.dp
        ) { pageIndex ->
            val exerciseId = slotState.pageBindings.getOrNull(pageIndex) ?: slot.exerciseIds[pageIndex]
            val def = workoutPlan.catalog[exerciseId]
            val displayName = def?.name ?: exerciseId
            val log = exerciseLogs[exerciseId]
                ?: (def?.let { defaultExerciseLog(it) }
                    ?: return@HorizontalPager)
            val isMainVariation = exerciseId == slot.mainExerciseId
            val otherDayNames = locationsById[exerciseId]
                ?.filter { it.dayKey != dayKey }
                ?.map { it.dayKey }
                ?.distinct()
                ?.sorted()
                .orEmpty()
            val isDone = exerciseId in doneExercises

            VariationCard(
                displayName = displayName,
                log = log,
                isMainVariation = isMainVariation,
                linkedOtherDayNames = otherDayNames,
                isDone = isDone,
                onEditClick = { onEdit(pageIndex) },
                onToggleDone = { onToggleDone(exerciseId) },
                modifier = Modifier.height(200.dp)
            )
        }
    }
}

@Composable
private fun VariationCard(
    displayName: String,
    log: ExerciseLogState,
    isMainVariation: Boolean,
    linkedOtherDayNames: List<String>,
    isDone: Boolean,
    onEditClick: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = if (isMainVariation) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        colors = colors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isMainVariation) 4.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title row with done checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Feature 4: "Alt" badge for secondary variations
                    if (!isMainVariation) {
                        Text(
                            text = "ALTERNATIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        // Feature 5: Strikethrough when done
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.alpha(if (isDone) 0.6f else 1f)
                    )
                }
                // Feature 5: Done toggle button
                IconButton(onClick = onToggleDone) {
                    Icon(
                        imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = if (isDone) "Mark as not done" else "Mark as done",
                        tint = if (isDone) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                }
            }

            if (linkedOtherDayNames.isNotEmpty()) {
                Text(
                    text = "Also used on: ${linkedOtherDayNames.joinToString()} — changes apply everywhere.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (log.isCardio) {
                log.duration?.let {
                    Text(text = "Duration: $it", style = MaterialTheme.typography.bodyMedium)
                }
                log.intensity?.let {
                    Text(text = "Intensity: $it", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(
                    text = "${log.sets} sets × ${formatReps(log)} reps",
                    style = MaterialTheme.typography.bodyMedium
                )
                // Feature 1: formatted weight with unit and bodyweight support
                Text(
                    text = "Weight: ${formatWeight(log)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (log.comment.isNotBlank()) {
                Text(
                    text = log.comment,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(onClick = onEditClick) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Text(text = "Edit variation")
                }
            }
        }
    }
}

/** Feature 1: Format weight string with unit and bodyweight support. */
private fun formatWeight(log: ExerciseLogState): String {
    val weightValue = log.weight.trim()

    return when {
        log.isBodyweight && weightValue.isEmpty() -> "Bodyweight"
        log.isBodyweight -> {
            val num = weightValue.toDoubleOrNull()
            val unitLabel = if (log.weightUnit == WeightUnit.LBS) {
                if (num != null && num == 1.0) "lb" else "lbs"
            } else {
                if (num != null && num == 1.0) "kg" else "kgs"
            }
            "Bodyweight + $weightValue $unitLabel"
        }
        weightValue.isEmpty() -> "—"
        else -> {
            // If weight already contains a unit label (from old freeform data), show as-is
            val hasUnit = weightValue.contains(Regex("""(?i)(kg|kgs|lbs?|lb)"""))
            if (hasUnit) weightValue
            else {
                val num = weightValue.toDoubleOrNull()
                val unitLabel = if (log.weightUnit == WeightUnit.LBS) {
                    if (num != null && num == 1.0) "lb" else "lbs"
                } else {
                    if (num != null && num == 1.0) "kg" else "kgs"
                }
                "$weightValue $unitLabel"
            }
        }
    }
}

private fun formatReps(log: ExerciseLogState): String {
    return if (log.usePerSetReps) {
        log.repsPerSet?.joinToString(", ") ?: "—"
    } else {
        log.repsSingle?.toString() ?: "—"
    }
}
