package com.mateoviscarra.doitall.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mateoviscarra.doitall.calendar.CalendarManager
import com.mateoviscarra.doitall.data.Category
import com.mateoviscarra.doitall.data.CategoryExercise
import com.mateoviscarra.doitall.data.NewWorkoutDay
import com.mateoviscarra.doitall.data.NewWorkoutPlan
import com.mateoviscarra.doitall.data.WorkoutPlan
import com.mateoviscarra.doitall.data.persist.NewWorkoutStore
import com.mateoviscarra.doitall.data.persist.WorkoutStateStore
import com.mateoviscarra.doitall.data.persist.TimerStateStore
import kotlinx.coroutines.flow.collectLatest

private const val ROUTE_LIST = "workout_list"
private const val ROUTE_DETAIL = "workout_detail"
private const val ROUTE_EDIT = "workout_edit"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_CALENDAR_SETTINGS = "calendar_settings"
private const val ROUTE_CUSTOM = "custom_workout"
private const val ROUTE_CATEGORY_MANAGE = "category_manage"
private const val ROUTE_DAY_ASSIGN = "day_assign"

fun workoutDetailRoute(index: Int) = "$ROUTE_DETAIL/$index"

private fun workoutEditRoute(dayIndex: Int, slotIndex: Int, pageIndex: Int) =
    "$ROUTE_EDIT/$dayIndex/$slotIndex/$pageIndex"

@Composable
fun WorkoutApp(
    workoutPlan: WorkoutPlan, 
    newPlan: NewWorkoutPlan,
    newWorkoutStore: NewWorkoutStore? = null,
    onSavePlan: ((NewWorkoutPlan) -> Unit)? = null
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val store = remember { WorkoutStateStore(context.applicationContext) }
    val calendarManager = remember { CalendarManager(context.applicationContext) }
    val timerStore = remember { TimerStateStore(context.applicationContext) }
    
    // Local state for the mutable workout plan
    var mutablePlan by remember { mutableStateOf(newPlan) }
    var isLoaded by remember { mutableStateOf(false) }
    
    // Load persisted data if store exists
    LaunchedEffect(newWorkoutStore) {
        newWorkoutStore?.planFlow?.collectLatest { persistedPlan ->
            if (persistedPlan != null && !isLoaded) {
                mutablePlan = persistedPlan
                isLoaded = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_LIST
    ) {
        composable(ROUTE_LIST) {
            val convertedSchedule = workoutPlan.schedule.map { legacyDay ->
                val newDay = mutablePlan.days.find { it.day == legacyDay.day }
                if (newDay != null) {
                    com.mateoviscarra.doitall.data.LegacyWorkoutDay(
                        day = newDay.day,
                        muscleGroups = newDay.muscleGroups,
                        isRestDay = newDay.isRestDay,
                        notes = newDay.notes,
                        slots = newDay.exercises.map { ex ->
                            com.mateoviscarra.doitall.data.WorkoutSlot(
                                exerciseIds = listOf(ex.exerciseId) + ex.alternatives
                            )
                        }
                    )
                } else legacyDay
            }
            
            WorkoutListScreen(
                workoutDays = convertedSchedule,
                onDaySelected = { index ->
                    navController.navigate(workoutDetailRoute(index))
                },
                onSettingsClick = {
                    navController.navigate(ROUTE_SETTINGS)
                },
                onCustomClick = {
                    navController.navigate(ROUTE_CUSTOM)
                },
                onCategoryManageClick = {
                    navController.navigate(ROUTE_CATEGORY_MANAGE)
                },
                onDayAssignClick = {
                    navController.navigate(ROUTE_DAY_ASSIGN)
                },
                timerStore = timerStore,
                calendarManager = calendarManager
            )
        }
        composable(
            route = "$ROUTE_DETAIL/{index}",
            arguments = listOf(
                navArgument("index") { type = NavType.IntType }
            )
        ) { entry ->
            val index = entry.arguments?.getInt("index") ?: return@composable
            
            // Rebuild legacy plan from mutablePlan to ensure exercise IDs match catalog
            val rebuiltPlan = com.mateoviscarra.doitall.data.WorkoutRepository.convertToLegacyFormat(mutablePlan)
            
            val day = rebuiltPlan.schedule.getOrNull(index) ?: return@composable
            WorkoutDetailScreen(
                workoutPlan = rebuiltPlan,
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
            
            val legacySchedule = workoutPlan.schedule.map { legacyDay ->
                val newDay = mutablePlan.days.find { it.day == legacyDay.day }
                if (newDay != null) {
                    com.mateoviscarra.doitall.data.LegacyWorkoutDay(
                        day = newDay.day,
                        muscleGroups = newDay.muscleGroups,
                        isRestDay = newDay.isRestDay,
                        notes = newDay.notes,
                        slots = newDay.exercises.map { ex ->
                            com.mateoviscarra.doitall.data.WorkoutSlot(
                                exerciseIds = listOf(ex.exerciseId) + ex.alternatives
                            )
                        }
                    )
                } else legacyDay
            }
            
            val legacyPlan = com.mateoviscarra.doitall.data.WorkoutPlan(
                catalog = workoutPlan.catalog,
                schedule = legacySchedule
            )
            
            val day = legacyPlan.schedule.getOrNull(dayIndex) ?: return@composable
            EditVariationScreen(
                workoutPlan = legacyPlan,
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
        composable(ROUTE_CUSTOM) {
            CustomWorkoutScreen(
                calendarManager = calendarManager,
                onBack = { navController.popBackStack() }
            )
        }
        composable(ROUTE_CATEGORY_MANAGE) {
            CategoryManagementScreen(
                categories = mutablePlan.categories,
                onBack = { navController.popBackStack() },
                onAddCategory = { name ->
                    val newCategory = Category(
                        id = name.lowercase().replace(" ", "_"),
                        name = name,
                        exercises = emptyList()
                    )
                    mutablePlan = mutablePlan.copy(
                        categories = mutablePlan.categories + newCategory
                    )
                    onSavePlan?.invoke(mutablePlan)
                },
                onDeleteCategory = { id ->
                    mutablePlan = mutablePlan.copy(
                        categories = mutablePlan.categories.filter { it.id != id }
                    )
                    onSavePlan?.invoke(mutablePlan)
                },
                onAddExercise = { categoryId, exercise ->
                    mutablePlan = mutablePlan.copy(
                        categories = mutablePlan.categories.map { cat ->
                            if (cat.id == categoryId) {
                                cat.copy(exercises = cat.exercises + exercise)
                            } else {
                                cat
                            }
                        }
                    )
                    onSavePlan?.invoke(mutablePlan)
                },
                onDeleteExercise = { categoryId, exerciseId ->
                    mutablePlan = mutablePlan.copy(
                        categories = mutablePlan.categories.map { cat ->
                            if (cat.id == categoryId) {
                                cat.copy(exercises = cat.exercises.filter { it.id != exerciseId })
                            } else {
                                cat
                            }
                        },
                        days = mutablePlan.days.map { day ->
                            day.copy(exercises = day.exercises.filter { it.exerciseId != exerciseId })
                        }
                    )
                    onSavePlan?.invoke(mutablePlan)
                }
            )
        }
        composable(ROUTE_DAY_ASSIGN) {
            DayAssignmentScreen(
                days = mutablePlan.days,
                allExercises = mutablePlan.allExercises(),
                onBack = { navController.popBackStack() },
                onUpdateDay = { updatedDay ->
                    mutablePlan = mutablePlan.copy(
                        days = mutablePlan.days.map { day ->
                            if (day.day == updatedDay.day) updatedDay else day
                        }
                    )
                    onSavePlan?.invoke(mutablePlan)
                }
            )
        }
    }
}