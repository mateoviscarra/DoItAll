package com.mateoviscarra.doitall.data.persist

import android.content.Context
import android.content.SharedPreferences
import com.mateoviscarra.doitall.data.Category
import com.mateoviscarra.doitall.data.CategoryExercise
import com.mateoviscarra.doitall.data.DayExercise
import com.mateoviscarra.doitall.data.NewWorkoutDay
import com.mateoviscarra.doitall.data.NewWorkoutPlan
import org.json.JSONArray
import org.json.JSONObject

class NewWorkoutStore(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_CATEGORIES = "categories"
        private const val KEY_DAYS = "days"
    }
    
    val planFlow: kotlinx.coroutines.flow.Flow<NewWorkoutPlan?>
        get() = kotlinx.coroutines.flow.flow {
            emit(loadPlan())
        }
    
    fun loadPlanOnce(): NewWorkoutPlan? = loadPlan()
    
    private fun loadPlan(): NewWorkoutPlan? {
        val categoriesJson = prefs.getString(KEY_CATEGORIES, null)
        val daysJson = prefs.getString(KEY_DAYS, null)
        
        if (categoriesJson != null && daysJson != null) {
            return try {
                parsePlan(categoriesJson, daysJson)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }
    
    suspend fun savePlan(plan: NewWorkoutPlan) {
        prefs.edit()
            .putString(KEY_CATEGORIES, categoriesToJson(plan.categories))
            .putString(KEY_DAYS, daysToJson(plan.days))
            .apply()
    }
    
    private fun categoriesToJson(categories: List<Category>): String {
        val arr = JSONArray()
        categories.forEach { cat ->
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            val exArr = JSONArray()
            cat.exercises.forEach { ex ->
                val exObj = JSONObject()
                exObj.put("id", ex.id)
                exObj.put("name", ex.name)
                exObj.put("defaultSets", ex.defaultSets)
                exObj.put("defaultReps", ex.defaultReps)
                exObj.put("defaultLoad", ex.defaultLoad)
                exArr.put(exObj)
            }
            obj.put("exercises", exArr)
            arr.put(obj)
        }
        return arr.toString()
    }
    
    private fun daysToJson(days: List<NewWorkoutDay>): String {
        val arr = JSONArray()
        days.forEach { day ->
            val obj = JSONObject()
            obj.put("day", day.day)
            obj.put("muscleGroups", JSONArray(day.muscleGroups))
            obj.put("isRestDay", day.isRestDay)
            obj.put("notes", JSONArray(day.notes))
            val exArr = JSONArray()
            day.exercises.forEach { ex ->
                val exObj = JSONObject()
                exObj.put("exerciseId", ex.exerciseId)
                if (ex.alternatives.isNotEmpty()) {
                    exObj.put("alternatives", JSONArray(ex.alternatives))
                }
                exArr.put(exObj)
            }
            obj.put("exercises", exArr)
            arr.put(obj)
        }
        return arr.toString()
    }
    
    private fun parsePlan(categoriesJson: String, daysJson: String): NewWorkoutPlan {
        val categories = parseCategories(JSONArray(categoriesJson))
        val days = parseDays(JSONArray(daysJson))
        return NewWorkoutPlan(categories, days)
    }
    
    private fun parseCategories(arr: JSONArray): List<Category> {
        val list = mutableListOf<Category>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val exArr = obj.getJSONArray("exercises")
            val exercises = parseExercises(exArr)
            list.add(Category(
                id = obj.getString("id"),
                name = obj.getString("name"),
                exercises = exercises
            ))
        }
        return list
    }
    
    private fun parseExercises(arr: JSONArray): List<CategoryExercise> {
        val list = mutableListOf<CategoryExercise>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(CategoryExercise(
                id = obj.getString("id"),
                name = obj.getString("name"),
                defaultSets = obj.optInt("defaultSets", 3),
                defaultReps = obj.optString("defaultReps", "10"),
                defaultLoad = obj.optString("defaultLoad", "")
            ))
        }
        return list
    }
    
    private fun parseDays(arr: JSONArray): List<NewWorkoutDay> {
        val list = mutableListOf<NewWorkoutDay>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val mgArr = obj.getJSONArray("muscleGroups")
            val muscleGroups = (0 until mgArr.length()).map { mgArr.getString(it) }
            val notesArr = obj.getJSONArray("notes")
            val notes = (0 until notesArr.length()).map { notesArr.getString(it) }
            val exArr = obj.getJSONArray("exercises")
            val exercises = parseDayExercises(exArr)
            list.add(NewWorkoutDay(
                day = obj.getString("day"),
                muscleGroups = muscleGroups,
                isRestDay = obj.getBoolean("isRestDay"),
                notes = notes,
                exercises = exercises
            ))
        }
        return list
    }
    
    private fun parseDayExercises(arr: JSONArray): List<DayExercise> {
        val list = mutableListOf<DayExercise>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val alternatives = if (obj.has("alternatives")) {
                val altArr = obj.getJSONArray("alternatives")
                (0 until altArr.length()).map { altArr.getString(it) }
            } else {
                emptyList()
            }
            list.add(DayExercise(
                exerciseId = obj.getString("exerciseId"),
                alternatives = alternatives
            ))
        }
        return list
    }
}