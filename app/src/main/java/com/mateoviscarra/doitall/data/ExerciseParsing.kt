package com.mateoviscarra.doitall.data

import org.json.JSONObject

/**
 * Best-effort defaults from JSON strings for initial log state.
 */
fun ExerciseDefinition.defaultSetsInt(): Int {
    return sets.trim().toIntOrNull()
        ?: Regex("\\d+").find(sets)?.value?.toIntOrNull()
        ?: 1
}

fun ExerciseDefinition.defaultRepsInt(): Int {
    val r = reps.trim()
    if (r.contains("failure", ignoreCase = true)) return 0
    return r.toIntOrNull()
        ?: Regex("\\d+").find(r)?.value?.toIntOrNull()
        ?: 0
}

fun placeholderExerciseDefinition(id: String, displayName: String): ExerciseDefinition =
    ExerciseDefinition(
        id = id,
        name = displayName,
        sets = "1",
        reps = "0",
        load = "-",
        duration = null,
        intensity = null
    )

fun exerciseDefinitionFromWorkoutJson(id: String, displayName: String, obj: JSONObject): ExerciseDefinition =
    ExerciseDefinition(
        id = id,
        name = displayName,
        sets = obj.opt("sets")?.toString() ?: "-",
        reps = obj.optString("reps", "-"),
        load = obj.optString("load", "-"),
        duration = obj.optString("duration").takeIf { it.isNotEmpty() },
        intensity = obj.optString("intensity").takeIf { it.isNotEmpty() }
    )
