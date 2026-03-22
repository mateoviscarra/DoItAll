package com.mateoviscarra.doitall.data

/**
 * Hardcoded exercise options for one row in the plan: main first, then alternatives.
 */
fun WorkoutExercise.catalogOptionNames(): List<String> {
    return buildList {
        add(name)
        addAll(alternatives)
    }
}
