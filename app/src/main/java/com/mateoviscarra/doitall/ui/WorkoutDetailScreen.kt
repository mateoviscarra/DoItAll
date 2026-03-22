package com.mateoviscarra.doitall.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mateoviscarra.doitall.data.ExerciseLocation
import com.mateoviscarra.doitall.data.WorkoutDay
import com.mateoviscarra.doitall.data.WorkoutPlan
import com.mateoviscarra.doitall.data.WorkoutSlot
import com.mateoviscarra.doitall.data.persist.ExerciseLogState
import com.mateoviscarra.doitall.data.persist.SlotLogState
import com.mateoviscarra.doitall.data.persist.WorkoutStateStore
import com.mateoviscarra.doitall.data.persist.defaultDayLog
import com.mateoviscarra.doitall.data.persist.defaultExerciseLog
import com.mateoviscarra.doitall.data.persist.mergedExerciseLogs
import com.mateoviscarra.doitall.data.persist.PersistRootV2
import com.mateoviscarra.doitall.data.persist.ResolvedDayUi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkoutDetailScreen(
    workoutPlan: WorkoutPlan,
    dayKey: String,
    workoutDay: WorkoutDay,
    onBack: () -> Unit,
    onEditVariation: (slotIndex: Int, pageIndex: Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(dayKey) { WorkoutStateStore(context.applicationContext) }
    val scope = rememberCoroutineScope()

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
                        }
                    )
                }
            }
        }
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
    onPageSelected: (page: Int) -> Unit,
    onEdit: (pageIndex: Int) -> Unit
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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
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

            VariationCard(
                displayName = displayName,
                log = log,
                isMainVariation = isMainVariation,
                linkedOtherDayNames = otherDayNames,
                onEditClick = { onEdit(pageIndex) }
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
    onEditClick: () -> Unit
) {
    val colors = CardDefaults.cardColors(
        containerColor = if (isMainVariation) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (isMainVariation) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

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
                Text(
                    text = "Weight: ${log.weight}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (log.comment.isNotBlank()) {
                Text(
                    text = log.comment,
                    style = MaterialTheme.typography.bodySmall,
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

private fun formatReps(log: ExerciseLogState): String {
    return if (log.usePerSetReps) {
        log.repsPerSet?.joinToString(", ") ?: "—"
    } else {
        log.repsSingle?.toString() ?: "—"
    }
}
