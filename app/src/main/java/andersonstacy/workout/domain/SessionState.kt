package andersonstacy.workout.domain

import andersonstacy.workout.data.Workout
import andersonstacy.workout.data.WorkoutStep

/**
 * Where you are in a workout. [stepIndex] runs from 0 to the step count; landing on the count
 * itself means the workout is finished.
 */
data class SessionState(
    val workout: Workout,
    val stepIndex: Int,
) {
    val totalSteps: Int = workout.steps.size

    val isComplete: Boolean
        get() = stepIndex >= totalSteps

    val currentStep: WorkoutStep?
        get() = workout.steps.getOrNull(stepIndex)

    /** 1-based position for display, clamped to the last step once complete. */
    val stepNumber: Int
        get() = (stepIndex + 1).coerceAtMost(totalSteps)

    val progress: Float
        get() = if (totalSteps == 0) 1f else stepIndex.toFloat() / totalSteps

    val canGoBack: Boolean
        get() = stepIndex > 0

    val isLastStep: Boolean
        get() = totalSteps > 0 && stepIndex == totalSteps - 1

    fun next(): SessionState = of(workout, stepIndex + 1)

    fun previous(): SessionState = of(workout, stepIndex - 1)

    companion object {
        /**
         * Clamps rather than throws: a catalog refresh can shorten a workout while a session for
         * it is open, and dropping the user on the completion screen beats crashing.
         */
        fun of(workout: Workout, stepIndex: Int): SessionState =
            SessionState(workout, stepIndex.coerceIn(0, workout.steps.size))
    }
}
