package com.mateoviscarra.doitall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.mateoviscarra.doitall.data.WorkoutRepository
import com.mateoviscarra.doitall.data.persist.NewWorkoutStore
import com.mateoviscarra.doitall.ui.WorkoutApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.parseColor("#EEEEEE")
        
        val newWorkoutStore = NewWorkoutStore(this)
        val defaultPlan = WorkoutRepository.loadWorkoutPlan(this)
        
        setContent {
            val scope = rememberCoroutineScope()
            var plan by remember { mutableStateOf(defaultPlan) }
            var isInitialLoad by remember { mutableStateOf(true) }
            
            // Background load persisted data after UI is shown
            LaunchedEffect(Unit) {
                // Small delay to let UI render first
                delay(500)
                
                // Load directly from SharedPreferences
                try {
                    val persisted = newWorkoutStore.loadPlanOnce()
                    android.util.Log.d("DoItAll", "Loaded persisted: ${persisted?.categories?.sumOf { it.exercises.size }} exercises")
                    if (persisted != null) {
                        plan = persisted
                        android.util.Log.d("DoItAll", "Plan updated from persistence")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DoItAll", "Load failed: $e")
                }
                isInitialLoad = false
            }
            
            val legacyPlan = remember(plan) { WorkoutRepository.convertToLegacyFormat(plan) }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WorkoutApp(
                        workoutPlan = legacyPlan, 
                        newPlan = plan,
                        newWorkoutStore = newWorkoutStore,
                        onSavePlan = { newPlan ->
                            scope.launch {
                                try {
                                    newWorkoutStore.savePlan(newPlan)
                                    plan = newPlan
                                } catch (e: Exception) {
                                    // Silent fail
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}