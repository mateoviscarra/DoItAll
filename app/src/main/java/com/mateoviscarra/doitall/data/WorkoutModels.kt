package com.mateoviscarra.doitall.data

/**
 * Exercise defined in a category (default template)
 */
data class CategoryExercise(
    val id: String,
    val name: String,
    val defaultSets: Int,
    val defaultReps: String,
    val defaultLoad: String,
    val categories: List<String> = listOf()
)

/**
 * Category containing a library of exercises
 */
data class Category(
    val id: String,
    val name: String,
    val exercises: List<CategoryExercise>
)

/**
 * An exercise assigned to a day with optional alternatives (new format)
 */
data class DayExercise(
    val exerciseId: String,
    val alternatives: List<String> = emptyList()
)

/**
 * A workout day with flexible exercise assignments (new format)
 */
data class NewWorkoutDay(
    val day: String,
    val muscleGroups: List<String>,
    val isRestDay: Boolean,
    val notes: List<String> = emptyList(),
    val exercises: List<DayExercise>
)

/**
 * Complete workout plan with categories and day assignments (new format)
 */
data class NewWorkoutPlan(
    val categories: List<Category>,
    val days: List<NewWorkoutDay>
) {
    fun allExercises(): List<CategoryExercise> = categories.flatMap { it.exercises }
    fun findExercise(id: String): CategoryExercise? = allExercises().find { it.id == id }
    fun categoriesForExercise(exerciseId: String): List<Category> = 
        categories.filter { cat -> cat.exercises.any { it.id == exerciseId } }
}

/**
 * Canonical exercise (derived from category exercise for logging)
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
    fun optionIds(): List<String> = exerciseIds
}

/**
 * Workout day (legacy format for UI compatibility)
 */
data class LegacyWorkoutDay(
    val day: String,
    val muscleGroups: List<String>,
    val isRestDay: Boolean,
    val notes: List<String> = emptyList(),
    val slots: List<WorkoutSlot>
)

/**
 * Workout plan (legacy format for UI compatibility)
 */
data class LegacyWorkoutPlan(
    val catalog: Map<String, ExerciseDefinition>,
    val schedule: List<LegacyWorkoutDay>
) {
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

// Type aliases for backward compatibility with existing UI code
typealias WorkoutDay = LegacyWorkoutDay
typealias WorkoutPlan = LegacyWorkoutPlan