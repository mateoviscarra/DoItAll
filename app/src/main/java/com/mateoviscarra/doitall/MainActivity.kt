package com.mateoviscarra.doitall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import com.mateoviscarra.doitall.data.WorkoutRepository
import com.mateoviscarra.doitall.ui.WorkoutApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val workoutPlan = remember { WorkoutRepository.loadWorkoutPlan(this) }

            MaterialTheme {
                Surface {
                    WorkoutApp(workoutPlan = workoutPlan)
                }
            }
        }
    }
}
