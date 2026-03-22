package com.mateoviscarra.doitall.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mateoviscarra.doitall.data.WorkoutPlan

private const val ROUTE_LIST = "workout_list"
private const val ROUTE_DETAIL = "workout_detail"

fun workoutDetailRoute(index: Int) = "$ROUTE_DETAIL/$index"

@Composable
fun WorkoutApp(workoutPlan: WorkoutPlan) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ROUTE_LIST
    ) {
        composable(ROUTE_LIST) {
            WorkoutListScreen(
                workoutDays = workoutPlan.schedule,
                onDaySelected = { index ->
                    navController.navigate(workoutDetailRoute(index))
                }
            )
        }
        composable(
            route = "$ROUTE_DETAIL/{index}",
            arguments = listOf(
                navArgument("index") { type = NavType.IntType }
            )
        ) { entry ->
            val index = entry.arguments?.getInt("index") ?: return@composable
            val day = workoutPlan.schedule.getOrNull(index) ?: return@composable
            WorkoutDetailScreen(
                workoutDay = day,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
