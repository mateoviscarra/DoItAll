package com.mateoviscarra.doitall.data

data class WorkoutPlan(
    val schedule: List<WorkoutDay>
)

data class WorkoutDay(
    val day: String,
    val muscleGroups: List<String>,
    val isRestDay: Boolean,
    val notes: List<String> = emptyList(),
    val exercises: List<WorkoutExercise>
)

data class WorkoutExercise(
    val name: String,
    val sets: String,
    val reps: String,
    val load: String,
    val alternatives: List<String>,
    /** Present for cardio-style entries in JSON. */
    val duration: String? = null,
    val intensity: String? = null
)
