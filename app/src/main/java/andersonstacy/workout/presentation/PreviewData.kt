package andersonstacy.workout.presentation

import andersonstacy.workout.data.Workout
import andersonstacy.workout.data.WorkoutCatalog
import andersonstacy.workout.data.WorkoutStep

/** Stand-in catalog for @Preview functions, shaped like the published one. */
internal object PreviewData {

    val soccerLadder = Workout(
        id = "soccer-ladder",
        name = "Soccer Ladder",
        description = "Agility-ladder footwork for soccer",
        steps = listOf(
            WorkoutStep("Two feet in each square", 2),
            WorkoutStep("Icky shuffle", 2, notes = "Lead with the outside foot each pass"),
            WorkoutStep("In-in-out-out", 2),
            WorkoutStep("Lateral shuffle", 2),
            WorkoutStep("Sprint out", 4, notes = "10 yards past the last rung"),
        ),
    )

    val catalog = WorkoutCatalog(
        workouts = listOf(
            soccerLadder,
            Workout(
                id = "dumbbell",
                name = "Dumbbell",
                steps = List(6) { WorkoutStep("Move ${it + 1}", 10) },
            ),
            Workout(
                id = "agility",
                name = "Agility",
                steps = List(7) { WorkoutStep("Drill ${it + 1}", 2) },
            ),
        ),
    )
}
