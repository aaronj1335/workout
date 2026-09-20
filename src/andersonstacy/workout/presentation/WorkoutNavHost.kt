package andersonstacy.workout.presentation

import andersonstacy.workout.presentation.detail.WorkoutDetailRoute
import andersonstacy.workout.presentation.list.WorkoutListRoute
import andersonstacy.workout.presentation.session.WorkoutSessionRoute
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

const val ARG_WORKOUT_ID = "workoutId"

object Routes {
    const val LIST = "list"
    const val DETAIL = "workout/{$ARG_WORKOUT_ID}"
    const val SESSION = "session/{$ARG_WORKOUT_ID}"

    fun detail(workoutId: String) = "workout/$workoutId"

    fun session(workoutId: String) = "session/$workoutId"
}

@Composable
fun WorkoutNavHost(
    navController: NavHostController = rememberSwipeDismissableNavController(),
) {
    SwipeDismissableNavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            WorkoutListRoute(
                onWorkoutClick = { workoutId -> navController.navigate(Routes.detail(workoutId)) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(ARG_WORKOUT_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString(ARG_WORKOUT_ID).orEmpty()
            WorkoutDetailRoute(onStart = { navController.navigate(Routes.session(workoutId)) })
        }
        composable(
            route = Routes.SESSION,
            arguments = listOf(navArgument(ARG_WORKOUT_ID) { type = NavType.StringType }),
        ) {
            WorkoutSessionRoute(
                onFinished = { navController.popBackStack(Routes.LIST, inclusive = false) },
            )
        }
    }
}
