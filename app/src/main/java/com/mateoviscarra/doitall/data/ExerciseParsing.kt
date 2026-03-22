package com.mateoviscarra.doitall.data

/**
 * Best-effort defaults from JSON strings for initial log state.
 */
fun WorkoutExercise.defaultSetsInt(): Int {
    return sets.trim().toIntOrNull()
        ?: Regex("\\d+").find(sets)?.value?.toIntOrNull()
        ?: 1
}

fun WorkoutExercise.defaultRepsInt(): Int {
    val r = reps.trim()
    if (r.contains("failure", ignoreCase = true)) return 0
    return r.toIntOrNull()
        ?: Regex("\\d+").find(r)?.value?.toIntOrNull()
        ?: 0
}
