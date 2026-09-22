package andersonstacy.workout.tools

import andersonstacy.workout.data.WorkoutCatalog
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Turns the one workout file into the catalog the watch downloads. */
object WorkoutCatalogBuilder {

    sealed interface Result {
        data class Success(val catalog: WorkoutCatalog) : Result

        /** One line per problem, each prefixed with the file it was found in. */
        data class Failure(val problems: List<String>) : Result
    }

    /**
     * @param file the single YAML file; its workouts keep the order they are listed in.
     * @param displayName how the file is named in problem reports, e.g. relative to the repo root.
     */
    fun build(
        file: File,
        now: Instant = Instant.now(),
        displayName: (File) -> String = File::getPath,
    ): Result {
        val label = displayName(file)
        if (!file.isFile) return Result.Failure(listOf("$label: no such file"))

        return when (val parsed = WorkoutsFile.read(file)) {
            is WorkoutsFile.Parsed.Invalid -> Result.Failure(parsed.problems.map { "$label: $it" })
            is WorkoutsFile.Parsed.Valid -> Result.Success(
                WorkoutCatalog(
                    version = WorkoutCatalog.CURRENT_VERSION,
                    generatedAt = now.truncatedTo(ChronoUnit.SECONDS).toString(),
                    workouts = parsed.workouts,
                ),
            )
        }
    }
}
