package com.mateoviscarra.doitall.data

/**
 * Canonical exercise (hardcoded). Same [id] is shared across all days — logs apply everywhere.
 */
data class ExerciseDefinition(
    val id: String,
    val name: String,
    val sets: String,
    val reps: String,
    val load: String,
    val duration: String? = null,
    val intensity: String? = null
)

/**
 * One row in a day: carousel order is [exerciseIds] (main first, then alternatives).
 */
data class WorkoutSlot(
    val exerciseIds: List<String>
) {
    init {
        require(exerciseIds.isNotEmpty()) { "Slot must have at least one exercise id" }
    }

    val mainExerciseId: String get() = exerciseIds.first()

    /** Ids the user may bind to any page in this slot (from hardcoded plan). */
    fun optionIds(): List<String> = exerciseIds
}

data class WorkoutPlan(
    /** All exercises keyed by stable id (built when parsing JSON). */
    val catalog: Map<String, ExerciseDefinition>,
    val schedule: List<WorkoutDay>
) {
    /**
     * Where each exercise id appears in the plan (day name + 1-based slot index for display).
     */
    fun exerciseLocationsById(): Map<String, List<ExerciseLocation>> {
        val acc = mutableMapOf<String, MutableList<ExerciseLocation>>()
        for (day in schedule) {
            day.slots.forEachIndexed { slotIndex, slot ->
                slot.exerciseIds.distinct().forEach { id ->
                    acc.getOrPut(id) { mutableListOf() }
                        .add(ExerciseLocation(dayKey = day.day, slotIndex = slotIndex))
                }
            }
        }
        return acc.mapValues { (_, list) -> list.distinct() }
    }
}

data class ExerciseLocation(
    val dayKey: String,
    val slotIndex: Int
)

data class WorkoutDay(
    val day: String,
    val muscleGroups: List<String>,
    val isRestDay: Boolean,
    val notes: List<String> = emptyList(),
    val slots: List<WorkoutSlot>
)
