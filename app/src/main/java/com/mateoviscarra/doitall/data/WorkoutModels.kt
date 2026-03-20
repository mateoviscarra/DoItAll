package com.mateoviscarra.doitall.data

data class WorkoutPlan(
    val schedule: List<WorkoutDay>
)

data class WorkoutDay(
    val day: String,
    val muscleGroups: List<String>,
    val isRestDay: Boolean,
    val exercises: List<WorkoutExercise>
)

data class WorkoutExercise(
    val name: String,
    val sets: String,
    val reps: String,
    val load: String,
    val alternatives: List<String>
)
