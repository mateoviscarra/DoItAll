package com.mateoviscarra.doitall.data.persist

import com.mateoviscarra.doitall.data.ExerciseDefinition
import com.mateoviscarra.doitall.data.WorkoutDay
import com.mateoviscarra.doitall.data.WorkoutPlan
import com.mateoviscarra.doitall.data.WorkoutSlot
import com.mateoviscarra.doitall.data.defaultRepsInt
import com.mateoviscarra.doitall.data.defaultSetsInt

fun defaultExerciseLog(def: ExerciseDefinition): ExerciseLogState {
    val cardio = def.duration != null
    val sets = if (cardio) 1 else def.defaultSetsInt().coerceAtLeast(1)
    val repsSingle = if (cardio) null else def.defaultRepsInt()
    return ExerciseLogState(
        sets = sets,
        weight = if (cardio) "" else def.load,
        usePerSetReps = false,
        repsSingle = repsSingle,
        repsPerSet = null,
        comment = "",
        isCardio = cardio,
        duration = def.duration,
        intensity = def.intensity
    )
}

fun defaultDayLog(day: WorkoutDay): DayLogState {
    val slots = day.slots.mapIndexed { index, slot ->
        index.toString() to SlotLogState(
            selectedPage = 0,
            pageBindings = slot.exerciseIds.toList()
        )
    }.toMap()
    return DayLogState(slots = slots)
}

fun normalizeSlotState(slot: WorkoutSlot, persisted: SlotLogState?): SlotLogState {
    val size = slot.exerciseIds.size
    val defaultBindings = slot.exerciseIds.toList()
    if (persisted == null) {
        return SlotLogState(selectedPage = 0, pageBindings = defaultBindings)
    }
    val allowed = slot.exerciseIds.toSet()
    val bindings = List(size) { i ->
        val p = persisted.pageBindings.getOrNull(i)
        if (p != null && p in allowed) p else defaultBindings[i]
    }
    return SlotLogState(
        selectedPage = persisted.selectedPage.coerceIn(0, (size - 1).coerceAtLeast(0)),
        pageBindings = bindings
    )
}

fun dayLogFromPersist(root: PersistRootV2, dayKey: String, day: WorkoutDay): DayLogState {
    val persistedDay = root.days[dayKey]
    return DayLogState(
        slots = day.slots.mapIndexed { index, slot ->
            val key = index.toString()
            val slotPersisted = persistedDay?.slots?.get(key)
            key to normalizeSlotState(slot, slotPersisted)
        }.toMap()
    )
}

fun mergedExerciseLogs(plan: WorkoutPlan, root: PersistRootV2): Map<String, ExerciseLogState> {
    val out = linkedMapOf<String, ExerciseLogState>()
    for (def in plan.catalog.values) {
        out[def.id] = root.logs[def.id] ?: defaultExerciseLog(def)
    }
    return out
}
