package andersonstacy.workout.data

import kotlinx.serialization.Serializable

/**
 * The catalog published at [andersonstacy.workout.BuildConfig.WORKOUTS_URL], compiled from
 * `workouts/workouts.yaml` by `//tools:build_workouts`.
 */
@Serializable
data class WorkoutCatalog(
    val version: Int = CURRENT_VERSION,
    val generatedAt: String? = null,
    val workouts: List<Workout> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class Workout(
    val id: String,
    val name: String,
    val description: String? = null,
    val steps: List<WorkoutStep> = emptyList(),
)

/** One move in a workout, performed [reps] times. */
@Serializable
data class WorkoutStep(
    val name: String,
    val reps: Int,
    val notes: String? = null,
)
