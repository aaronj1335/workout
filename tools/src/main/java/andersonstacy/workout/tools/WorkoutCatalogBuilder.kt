package andersonstacy.workout.tools

import andersonstacy.workout.data.Workout
import andersonstacy.workout.data.WorkoutCatalog
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Combines the workout files into the one catalog the watch downloads. */
object WorkoutCatalogBuilder {

    sealed interface Result {
        data class Success(val catalog: WorkoutCatalog) : Result

        /** One line per problem, each prefixed with the file it was found in. */
        data class Failure(val problems: List<String>) : Result
    }

    /**
     * @param files read in the order given; sort them first for a stable catalog.
     * @param displayName how a file is named in problem reports, e.g. relative to the repo root.
     */
    fun build(
        files: List<File>,
        now: Instant = Instant.now(),
        displayName: (File) -> String = File::getPath,
    ): Result {
        val problems = mutableListOf<String>()
        val workouts = mutableListOf<Workout>()
        val seenIds = mutableMapOf<String, String>()

        if (files.isEmpty()) problems += "no workout files given"

        for (file in files) {
            val label = displayName(file)
            when (val parsed = WorkoutFile.read(file)) {
                is WorkoutFile.Parsed.Invalid -> parsed.problems.mapTo(problems) { "$label: $it" }
                is WorkoutFile.Parsed.Valid -> {
                    val workout = parsed.workout
                    val duplicate = seenIds.put(workout.id, label)
                    if (duplicate != null) {
                        problems += "$label: id \"${workout.id}\" is already used by $duplicate"
                    } else {
                        workouts += workout
                    }
                }
            }
        }

        if (problems.isNotEmpty()) return Result.Failure(problems)
        return Result.Success(
            WorkoutCatalog(
                version = WorkoutCatalog.CURRENT_VERSION,
                generatedAt = now.truncatedTo(ChronoUnit.SECONDS).toString(),
                workouts = workouts,
            ),
        )
    }
}
