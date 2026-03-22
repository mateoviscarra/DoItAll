package com.mateoviscarra.doitall.data.persist

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mateoviscarra.doitall.data.WorkoutExercise
import com.mateoviscarra.doitall.data.catalogOptionNames
import com.mateoviscarra.doitall.data.defaultRepsInt
import com.mateoviscarra.doitall.data.defaultSetsInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.workoutLogDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "workout_log"
)

private val JSON_KEY = stringPreferencesKey("workout_log_json_v1")

class WorkoutStateStore(private val context: Context) {

    /**
     * Emits merged [DayLogState] whenever preferences change.
     */
    fun dayLogFlow(dayKey: String, exercises: List<WorkoutExercise>): Flow<DayLogState> {
        return context.workoutLogDataStore.data.map { prefs ->
            val root = prefs[JSON_KEY]?.let { JSONObject(it) } ?: JSONObject()
            parseDayFromRoot(root, dayKey, exercises)
        }
    }

    suspend fun saveDay(dayKey: String, day: DayLogState) {
        context.workoutLogDataStore.edit { prefs ->
            val root = prefs[JSON_KEY]?.let { JSONObject(it) } ?: JSONObject()
            val days = root.optJSONObject("days") ?: JSONObject()
            days.put(dayKey, day.toDayJson())
            root.put("days", days)
            prefs[JSON_KEY] = root.toString()
        }
    }

    suspend fun updateDay(dayKey: String, exercises: List<WorkoutExercise>, transform: (DayLogState) -> DayLogState) {
        context.workoutLogDataStore.edit { prefs ->
            val root = prefs[JSON_KEY]?.let { JSONObject(it) } ?: JSONObject()
            val current = parseDayFromRoot(root, dayKey, exercises)
            val next = transform(current)
            val days = root.optJSONObject("days") ?: JSONObject()
            days.put(dayKey, next.toDayJson())
            root.put("days", days)
            prefs[JSON_KEY] = root.toString()
        }
    }

    suspend fun updateSelectedPage(
        dayKey: String,
        exercises: List<WorkoutExercise>,
        slotIndex: Int,
        page: Int
    ) {
        updateDay(dayKey, exercises) { day ->
            day.updateSlot(slotIndex) { slot ->
                val max = (slot.pages.size - 1).coerceAtLeast(0)
                slot.copy(selectedPage = page.coerceIn(0, max))
            }
        }
    }

    suspend fun updatePageLog(
        dayKey: String,
        exercises: List<WorkoutExercise>,
        slotIndex: Int,
        pageIndex: Int,
        page: PageLogState
    ) {
        updateDay(dayKey, exercises) { day ->
            day.updateSlot(slotIndex) { slot ->
                if (pageIndex !in slot.pages.indices) return@updateSlot slot
                val pages = slot.pages.toMutableList()
                pages[pageIndex] = page
                slot.copy(pages = pages)
            }
        }
    }
}

private fun parseDayFromRoot(root: JSONObject, dayKey: String, exercises: List<WorkoutExercise>): DayLogState {
    val days = root.optJSONObject("days") ?: return defaultDayLog(exercises)
    val dayJson = days.optJSONObject(dayKey) ?: return defaultDayLog(exercises)
    return parseDayJson(dayJson, exercises)
}

fun defaultDayLog(exercises: List<WorkoutExercise>): DayLogState {
    val slots = exercises.mapIndexed { index, ex ->
        index.toString() to defaultSlotLog(ex)
    }.toMap()
    return DayLogState(slots = slots)
}

fun defaultSlotLog(exercise: WorkoutExercise): SlotLogState {
    val names = exercise.catalogOptionNames()
    val pages = names.map { name ->
        defaultPageForCatalog(exercise, initialName = name)
    }
    return SlotLogState(
        selectedPage = 0,
        pages = pages
    )
}

private fun defaultPageForCatalog(template: WorkoutExercise, initialName: String): PageLogState {
    val cardio = template.duration != null
    val sets = if (cardio) 1 else template.defaultSetsInt().coerceAtLeast(1)
    val repsSingle = if (cardio) null else template.defaultRepsInt()
    return PageLogState(
        selectedExerciseName = initialName,
        sets = sets,
        weight = if (cardio) "" else template.load,
        usePerSetReps = false,
        repsSingle = repsSingle,
        repsPerSet = null,
        comment = "",
        isCardio = cardio,
        duration = template.duration,
        intensity = template.intensity
    )
}

private fun DayLogState.toDayJson(): JSONObject {
    val slotsObj = JSONObject()
    slots.forEach { (k, v) ->
        slotsObj.put(k, v.toSlotJson())
    }
    return JSONObject().put("slots", slotsObj)
}

private fun SlotLogState.toSlotJson(): JSONObject {
    val arr = JSONArray()
    pages.forEach { arr.put(it.toPageJson()) }
    return JSONObject()
        .put("selectedPage", selectedPage)
        .put("pages", arr)
}

private fun PageLogState.toPageJson(): JSONObject {
    val o = JSONObject()
        .put("selectedExerciseName", selectedExerciseName)
        .put("sets", sets)
        .put("weight", weight)
        .put("usePerSetReps", usePerSetReps)
        .put("comment", comment.take(PageLogState.MAX_COMMENT_LENGTH))
        .put("isCardio", isCardio)
    if (repsSingle != null) o.put("repsSingle", repsSingle)
    if (repsPerSet != null) {
        val a = JSONArray()
        repsPerSet.forEach { a.put(it) }
        o.put("repsPerSet", a)
    }
    if (duration != null) o.put("duration", duration)
    if (intensity != null) o.put("intensity", intensity)
    return o
}

private fun parseDayJson(dayJson: JSONObject, exercises: List<WorkoutExercise>): DayLogState {
    val slotsObj = dayJson.optJSONObject("slots") ?: JSONObject()
    val out = mutableMapOf<String, SlotLogState>()
    exercises.forEachIndexed { index, ex ->
        val key = index.toString()
        val slotJson = slotsObj.optJSONObject(key)
        val default = defaultSlotLog(ex)
        out[key] = if (slotJson == null) {
            default
        } else {
            parseSlotJson(slotJson, ex, default)
        }
    }
    return DayLogState(slots = out)
}

private fun parseSlotJson(
    json: JSONObject,
    template: WorkoutExercise,
    default: SlotLogState
): SlotLogState {
    val names = template.catalogOptionNames()
    val expectedPages = names.size
    val selectedPage = json.optInt("selectedPage", default.selectedPage)
        .coerceIn(0, (expectedPages - 1).coerceAtLeast(0))
    val pagesArr = json.optJSONArray("pages")
    val pages = if (pagesArr == null || pagesArr.length() != expectedPages) {
        default.pages
    } else {
        List(expectedPages) { i ->
            parsePageLog(
                pagesArr.optJSONObject(i) ?: JSONObject(),
                default.pages[i]
            )
        }
    }
    return SlotLogState(
        selectedPage = selectedPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
        pages = pages
    )
}

private fun parsePageLog(json: JSONObject, default: PageLogState): PageLogState {
    val repsArr = json.optJSONArray("repsPerSet")
    val repsPerSet = if (repsArr == null) {
        null
    } else {
        List(repsArr.length()) { repsArr.optInt(it) }
    }
    return PageLogState(
        selectedExerciseName = json.optString("selectedExerciseName", default.selectedExerciseName),
        sets = json.optInt("sets", default.sets).coerceAtLeast(1),
        weight = json.optString("weight", default.weight),
        usePerSetReps = json.optBoolean("usePerSetReps", default.usePerSetReps),
        repsSingle = if (json.has("repsSingle")) json.optInt("repsSingle") else default.repsSingle,
        repsPerSet = repsPerSet ?: default.repsPerSet,
        comment = json.optString("comment", default.comment).take(PageLogState.MAX_COMMENT_LENGTH),
        isCardio = json.optBoolean("isCardio", default.isCardio),
        duration = if (json.has("duration")) json.optString("duration").ifBlank { null } else default.duration,
        intensity = if (json.has("intensity")) json.optString("intensity").ifBlank { null } else default.intensity
    )
}
