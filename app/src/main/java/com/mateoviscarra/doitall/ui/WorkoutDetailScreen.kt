package com.mateoviscarra.doitall.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mateoviscarra.doitall.data.WorkoutDay
import com.mateoviscarra.doitall.data.WorkoutExercise
import com.mateoviscarra.doitall.data.persist.PageLogState
import com.mateoviscarra.doitall.data.persist.SlotLogState
import com.mateoviscarra.doitall.data.persist.WorkoutStateStore
import com.mateoviscarra.doitall.data.persist.defaultDayLog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WorkoutDetailScreen(
    dayKey: String,
    workoutDay: WorkoutDay,
    onBack: () -> Unit,
    onEditVariation: (slotIndex: Int, pageIndex: Int) -> Unit
) {
    val context = LocalContext.current
    val store = remember(dayKey) { WorkoutStateStore(context.applicationContext) }
    val scope = rememberCoroutineScope()

    val dayLog by store.dayLogFlow(dayKey, workoutDay.exercises).collectAsStateWithLifecycle(
        initialValue = defaultDayLog(workoutDay.exercises)
    )

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
                itemsIndexed(workoutDay.exercises) { slotIndex, template ->
                    val slotState = dayLog.slots[slotIndex.toString()]
                        ?: return@itemsIndexed
                    ExerciseSlotRow(
                        slotLabel = "${slotIndex + 1}. ${template.name}",
                        slotState = slotState,
                        onPageSelected = { page ->
                            scope.launch {
                                store.updateSelectedPage(
                                    dayKey = dayKey,
                                    exercises = workoutDay.exercises,
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
    slotLabel: String,
    slotState: SlotLogState,
    onPageSelected: (page: Int) -> Unit,
    onEdit: (pageIndex: Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = slotLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        val pageCount = slotState.pages.size
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
            val page = slotState.pages[pageIndex]
            VariationCard(
                pageIndex = pageIndex,
                page = page,
                onEditClick = { onEdit(pageIndex) }
            )
        }
    }
}

@Composable
private fun VariationCard(
    pageIndex: Int,
    page: PageLogState,
    onEditClick: () -> Unit
) {
    val isPrimarySlot = pageIndex == 0
    val colors = CardDefaults.cardColors(
        containerColor = if (isPrimarySlot) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (isPrimarySlot) {
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
                text = page.selectedExerciseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (page.isCardio) {
                page.duration?.let {
                    Text(text = "Duration: $it", style = MaterialTheme.typography.bodyMedium)
                }
                page.intensity?.let {
                    Text(text = "Intensity: $it", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(
                    text = "${page.sets} sets × ${formatReps(page)} reps",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Weight: ${page.weight}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (page.comment.isNotBlank()) {
                Text(
                    text = page.comment,
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

private fun formatReps(page: PageLogState): String {
    return if (page.usePerSetReps) {
        page.repsPerSet?.joinToString(", ") ?: "—"
    } else {
        page.repsSingle?.toString() ?: "—"
    }
}
