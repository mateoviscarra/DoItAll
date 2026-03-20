package com.mateoviscarra.doitall.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object WorkoutRepository {

    fun loadWorkoutPlan(context: Context): WorkoutPlan {
        val jsonText = context.assets.open("workout_plan.json")
            .bufferedReader()
            .use { it.readText() }

        val root = JSONObject(jsonText)
        val scheduleArray = root.optJSONArray("schedule") ?: JSONArray()

        val schedule = buildList {
            for (i in 0 until scheduleArray.length()) {
                val dayObj = scheduleArray.optJSONObject(i) ?: continue
                add(parseWorkoutDay(dayObj))
            }
        }

        return WorkoutPlan(schedule = schedule)
    }

    private fun parseWorkoutDay(dayObj: JSONObject): WorkoutDay {
        val day = dayObj.optString("day", "Unknown Day")
        val isRestDay = dayObj.optBoolean("is_rest_day", false)
        val muscleGroups = jsonStringArray(dayObj.optJSONArray("muscle_groups"))

        val exercisesArray = dayObj.optJSONArray("exercises") ?: JSONArray()
        val exercises = buildList {
            for (i in 0 until exercisesArray.length()) {
                val exerciseObj = exercisesArray.optJSONObject(i) ?: continue
                add(parseExercise(exerciseObj))
            }
        }

        return WorkoutDay(
            day = day,
            muscleGroups = muscleGroups,
            isRestDay = isRestDay,
            exercises = exercises
        )
    }

    private fun parseExercise(exerciseObj: JSONObject): WorkoutExercise {
        val name = exerciseObj.optString("name", "Unknown Exercise")
        val sets = exerciseObj.opt("sets")?.toString() ?: "-"
        val reps = exerciseObj.optString("reps", "-")
        val load = exerciseObj.optString("load", "-")
        val alternatives = jsonStringArray(exerciseObj.optJSONArray("alternatives"))

        return WorkoutExercise(
            name = name,
            sets = sets,
            reps = reps,
            load = load,
            alternatives = alternatives
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
