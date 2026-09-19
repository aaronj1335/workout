package andersonstacy.workout.data

import java.io.File
import kotlinx.serialization.json.Json

/** The last catalog we fetched, so the app opens instantly and works out of signal. */
interface WorkoutCache {
    fun read(): WorkoutCatalog?

    fun write(catalog: WorkoutCatalog)
}

class FileWorkoutCache(
    private val file: File,
    private val json: Json = WorkoutJson,
) : WorkoutCache {

    override fun read(): WorkoutCatalog? {
        if (!file.exists()) return null
        // A truncated or stale-format file is not worth crashing over: refetch instead.
        return runCatching { json.decodeFromString<WorkoutCatalog>(file.readText()) }.getOrNull()
    }

    override fun write(catalog: WorkoutCatalog) {
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(json.encodeToString(catalog))
        temporary.renameTo(file)
    }
}
