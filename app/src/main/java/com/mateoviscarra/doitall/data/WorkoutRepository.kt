package com.mateoviscarra.doitall.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object WorkoutRepository {

    fun loadWorkoutPlan(context: Context): WorkoutPlan {
        val jsonText = context.assets.open("workout_plan.json")
            .bufferedReader()
            .use { it.readText() }

        val root = JSONObject(jsonText)
        val scheduleArray = root.optJSONArray("schedule") ?: JSONArray()

        val nameToId = linkedMapOf<String, String>()
        val catalog = linkedMapOf<String, ExerciseDefinition>()
        val usedIds = mutableSetOf<String>()

        fun registerExercise(rawName: String, jsonTemplate: JSONObject?, forceUnique: Boolean = false): String {
            val key = rawName.trim().lowercase(Locale.US)
            val displayName = rawName.trim()

            if (!forceUnique) {
                val existing = nameToId[key]
                if (existing != null) return existing
            }

            val newId = buildStableExerciseId(displayName, usedIds)
            usedIds.add(newId)
            nameToId[key] = newId
            catalog[newId] = if (jsonTemplate != null) {
                exerciseDefinitionFromWorkoutJson(newId, displayName, jsonTemplate)
            } else {
                placeholderExerciseDefinition(newId, displayName)
            }
            return newId
        }

        fun parseSlot(exerciseObj: JSONObject): WorkoutSlot {
            val mainName = exerciseObj.optString("name", "Unknown")
            val mainId = registerExercise(mainName, exerciseObj)
            val altNames = jsonStringArray(exerciseObj.optJSONArray("alternatives"))
            val altIds = altNames.map { registerExercise(it, null, forceUnique = true) }
            return WorkoutSlot(exerciseIds = listOf(mainId) + altIds)
        }

        val schedule = buildList {
            for (i in 0 until scheduleArray.length()) {
                val dayObj = scheduleArray.optJSONObject(i) ?: continue
                val day = dayObj.optString("day", "Unknown Day")
                val isRestDay = dayObj.optBoolean("is_rest_day", false)
                val muscleGroups = jsonStringArray(dayObj.optJSONArray("muscle_groups"))
                val notes = jsonStringArray(dayObj.optJSONArray("notes"))

                val exercisesArray = dayObj.optJSONArray("exercises") ?: JSONArray()
                val slots = buildList {
                    for (j in 0 until exercisesArray.length()) {
                        val exerciseObj = exercisesArray.optJSONObject(j) ?: continue
                        add(parseSlot(exerciseObj))
                    }
                }

                add(
                    WorkoutDay(
                        day = day,
                        muscleGroups = muscleGroups,
                        isRestDay = isRestDay,
                        notes = notes,
                        slots = slots
                    )
                )
            }
        }

        return WorkoutPlan(
            catalog = catalog.toMap(),
            schedule = schedule
        )
    }

    private fun jsonStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                add(array.optString(i))
            }
        }
    }
}

private fun buildStableExerciseId(displayName: String, taken: MutableSet<String>): String {
    val base = displayName.lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifEmpty { "exercise" }
    if (base !in taken) return base
    val suffix = displayName.hashCode().toUInt().toString(16)
    var candidate = "${base}_$suffix"
    var n = 1
    while (candidate in taken) {
        candidate = "${base}_${suffix}_$n"
        n++
    }
    return candidate
}
