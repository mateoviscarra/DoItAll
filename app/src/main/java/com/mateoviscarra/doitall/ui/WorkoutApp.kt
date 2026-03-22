package com.mateoviscarra.doitall.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mateoviscarra.doitall.calendar.CalendarManager
import com.mateoviscarra.doitall.data.WorkoutPlan
import com.mateoviscarra.doitall.data.persist.WorkoutStateStore

private const val ROUTE_LIST = "workout_list"
private const val ROUTE_DETAIL = "workout_detail"
private const val ROUTE_EDIT = "workout_edit"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_CALENDAR_SETTINGS = "calendar_settings"

fun workoutDetailRoute(index: Int) = "$ROUTE_DETAIL/$index"

private fun workoutEditRoute(dayIndex: Int, slotIndex: Int, pageIndex: Int) =
    "$ROUTE_EDIT/$dayIndex/$slotIndex/$pageIndex"

@Composable
fun WorkoutApp(workoutPlan: WorkoutPlan) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val store = remember { WorkoutStateStore(context.applicationContext) }
    val calendarManager = remember { CalendarManager(context.applicationContext) }

    NavHost(
        navController = navController,
        startDestination = ROUTE_LIST
    ) {
        composable(ROUTE_LIST) {
            WorkoutListScreen(
                workoutDays = workoutPlan.schedule,
                onDaySelected = { index ->
                    navController.navigate(workoutDetailRoute(index))
                },
                onSettingsClick = {
                    navController.navigate(ROUTE_SETTINGS)
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
                workoutPlan = workoutPlan,
                dayKey = day.day,
                workoutDay = day,
                onBack = { navController.popBackStack() },
                onEditVariation = { slotIndex, pageIndex ->
                    navController.navigate(workoutEditRoute(index, slotIndex, pageIndex))
                },
                calendarManager = calendarManager
            )
        }
        composable(
            route = "$ROUTE_EDIT/{dayIndex}/{slotIndex}/{pageIndex}",
            arguments = listOf(
                navArgument("dayIndex") { type = NavType.IntType },
                navArgument("slotIndex") { type = NavType.IntType },
                navArgument("pageIndex") { type = NavType.IntType }
            )
        ) { entry ->
            val dayIndex = entry.arguments?.getInt("dayIndex") ?: return@composable
            val slotIndex = entry.arguments?.getInt("slotIndex") ?: return@composable
            val pageIndex = entry.arguments?.getInt("pageIndex") ?: return@composable
            val day = workoutPlan.schedule.getOrNull(dayIndex) ?: return@composable
            EditVariationScreen(
                workoutPlan = workoutPlan,
                dayKey = day.day,
                workoutDay = day,
                slotIndex = slotIndex,
                pageIndex = pageIndex,
                store = store,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                workoutPlan = workoutPlan,
                calendarManager = calendarManager,
                onBack = { navController.popBackStack() },
                onCalendarSettingsClick = {
                    navController.navigate(ROUTE_CALENDAR_SETTINGS)
                }
            )
        }
        composable(ROUTE_CALENDAR_SETTINGS) {
            com.mateoviscarra.doitall.calendar.CalendarSettingsScreen(
                onBack = { navController.popBackStack() },
                calendarManager = calendarManager
            )
        }
    }
}
