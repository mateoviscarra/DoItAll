package com.mateoviscarra.doitall.data.persist

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.timerDataStore by preferencesDataStore(name = "timer_state")

class TimerStateStore(private val context: Context) {
    
    private val dataStore get() = context.applicationContext.timerDataStore
    
    companion object {
        private val KEY_ACTIVE_TIMERS = stringPreferencesKey("active_timers_json")
        private val KEY_TIMER_COUNTER = longPreferencesKey("timer_counter")
    }
    
    data class PersistedTimer(
        val id: Long,
        val name: String,
        val description: String,
        val remainingMs: Long,
        val startedAtEpochMs: Long
    )
    
    suspend fun saveActiveTimers(timers: List<PersistedTimer>, counter: Long) {
        dataStore.edit { prefs ->
            val json = timers.joinToString(";") { timer ->
                "${timer.id}|${timer.name}|${timer.description}|${timer.remainingMs}|${timer.startedAtEpochMs}"
            }
            prefs[KEY_ACTIVE_TIMERS] = json
            prefs[KEY_TIMER_COUNTER] = counter
        }
    }
    
    suspend fun loadActiveTimers(): Pair<List<PersistedTimer>, Long> {
        val prefs = dataStore.data.first()
        val json = prefs[KEY_ACTIVE_TIMERS] ?: ""
        val counter = prefs[KEY_TIMER_COUNTER] ?: 0L
        
        if (json.isEmpty()) return Pair(emptyList(), counter)
        
        val timers = json.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 5) {
                try {
                    PersistedTimer(
                        id = parts[0].toLong(),
                        name = parts[1],
                        description = parts[2],
                        remainingMs = parts[3].toLong(),
                        startedAtEpochMs = parts[4].toLong()
                    )
                } catch (e: Exception) {
                    null
                }
            } else null
        }
        
        return Pair(timers, counter)
    }
    
    suspend fun clearTimers() {
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_TIMERS] = ""
        }
    }
}