package andersonstacy.workout.tools

import andersonstacy.workout.data.Workout
import andersonstacy.workout.data.WorkoutStep
import java.io.File
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.MarkedYAMLException
import org.yaml.snakeyaml.error.YAMLException

/**
 * Reads `workouts/workouts.yaml` into the app's [Workout] model, checking the same rules the
 * README documents: which keys are allowed, which are required, and how long text may be so it
 * still fits on a watch face.
 *
 * The workouts are kept in one file, in one list, because the order of that list is the order
 * they appear on the watch.
 *
 * Every problem in the file is reported, not just the first, so a contributor can fix a pull
 * request in one round.
 */
object WorkoutsFile {

    /** The one file the catalog is compiled from, relative to `workouts/`. */
    const val FILE_NAME = "workouts.yaml"

    const val MAX_NAME_LENGTH = 40
    const val MAX_DESCRIPTION_LENGTH = 120
    const val MAX_STEP_NAME_LENGTH = 60
    const val MAX_NOTES_LENGTH = 120
    const val MAX_REPS = 999

    /** Ids are lowercase slugs: they end up in URLs and in the watch's saved session. */
    val ID_PATTERN = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

    private val ROOT_KEYS = setOf("workouts")
    private val WORKOUT_KEYS = setOf("id", "name", "description", "steps")
    private val STEP_KEYS = setOf("name", "reps", "notes")

    fun read(file: File): Parsed = parse(file.readText())

    fun parse(yaml: String): Parsed {
        val document = try {
            Yaml(LoaderOptions()).load<Any?>(yaml)
        } catch (error: MarkedYAMLException) {
            val mark = error.problemMark
            val where = if (mark != null) "line ${mark.line + 1}: " else ""
            return Parsed.Invalid(listOf("$where${error.problem ?: error.message}"))
        } catch (error: YAMLException) {
            return Parsed.Invalid(listOf(error.message ?: "could not parse YAML"))
        }

        val root = document as? Map<*, *>
            ?: return Parsed.Invalid(listOf("expected a YAML mapping with a workouts list"))

        val problems = mutableListOf<String>()
        Fields(root, path = "", problems).rejectUnknownKeys(ROOT_KEYS)
        val workouts = workouts(root["workouts"], problems)

        if (problems.isNotEmpty()) return Parsed.Invalid(problems)
        return Parsed.Valid(checkNotNull(workouts))
    }

    private fun workouts(value: Any?, problems: MutableList<String>): List<Workout>? {
        if (value == null) {
            problems += "/workouts is required"
            return null
        }
        if (value !is List<*>) {
            problems += "/workouts must be a list"
            return null
        }
        if (value.isEmpty()) {
            problems += "/workouts must have at least one workout"
            return null
        }

        val seenIds = mutableMapOf<String, Int>()
        val workouts = value.mapIndexedNotNull { index, item ->
            workout(item, "/workouts/$index", problems)?.also {
                val duplicate = seenIds.put(it.id, index)
                if (duplicate != null) {
                    problems += "/workouts/$index/id \"${it.id}\" is already used by /workouts/$duplicate"
                }
            }
        }
        return workouts.takeIf { problems.isEmpty() }
    }

    private fun workout(item: Any?, path: String, problems: MutableList<String>): Workout? {
        val map = item as? Map<*, *>
        if (map == null) {
            problems += "$path must be a mapping with id, name and steps"
            return null
        }
        val fields = Fields(map, path, problems)
        fields.rejectUnknownKeys(WORKOUT_KEYS)

        val id = fields.requiredString("id", MAX_NAME_LENGTH)?.takeIf { slug ->
            ID_PATTERN.matches(slug).also {
                if (!it) problems += "$path/id must be a lowercase slug like \"soccer-ladder\" (got \"$slug\")"
            }
        }
        val name = fields.requiredString("name", MAX_NAME_LENGTH)
        val description = fields.optionalString("description", MAX_DESCRIPTION_LENGTH)
        val steps = fields.steps()

        if (id == null || name == null || steps == null) return null
        return Workout(id = id, name = name, description = description, steps = steps)
    }

    sealed interface Parsed {
        /** The workouts in the order the file lists them, which is the order on the watch. */
        data class Valid(val workouts: List<Workout>) : Parsed

        /** Each problem is `<json-pointer> <message>`, e.g. `/workouts/2/steps/0/reps must be an integer`. */
        data class Invalid(val problems: List<String>) : Parsed
    }

    private class Fields(
        private val map: Map<*, *>,
        private val path: String,
        private val problems: MutableList<String>,
    ) {
        fun rejectUnknownKeys(allowed: Set<String>) {
            for (key in map.keys) {
                if (key !in allowed) {
                    problems += "$path/ unknown key \"$key\"; allowed: ${allowed.joinToString()}"
                }
            }
        }

        fun requiredString(key: String, maxLength: Int): String? {
            if (key !in map) {
                problems += "$path/$key is required"
                return null
            }
            return optionalString(key, maxLength)
        }

        fun optionalString(key: String, maxLength: Int = Int.MAX_VALUE): String? {
            val value = map[key] ?: return null
            if (value !is String) {
                problems += "$path/$key must be a string"
                return null
            }
            if (value.isEmpty()) {
                problems += "$path/$key must not be empty"
                return null
            }
            if (value.length > maxLength) {
                problems += "$path/$key must be at most $maxLength characters (got ${value.length})"
                return null
            }
            return value
        }

        fun steps(): List<WorkoutStep>? {
            val value = map["steps"]
            if (value == null) {
                problems += "$path/steps is required"
                return null
            }
            if (value !is List<*>) {
                problems += "$path/steps must be a list"
                return null
            }
            if (value.isEmpty()) {
                problems += "$path/steps must have at least one step"
                return null
            }
            val steps = value.mapIndexedNotNull { index, item -> step(item, "$path/steps/$index") }
            return steps.takeIf { it.size == value.size }
        }

        private fun step(item: Any?, stepPath: String): WorkoutStep? {
            val map = item as? Map<*, *>
            if (map == null) {
                problems += "$stepPath must be a mapping with name and reps"
                return null
            }
            val fields = Fields(map, stepPath, problems)
            fields.rejectUnknownKeys(STEP_KEYS)
            val name = fields.requiredString("name", MAX_STEP_NAME_LENGTH)
            val reps = fields.reps()
            val notes = fields.optionalString("notes", MAX_NOTES_LENGTH)
            if (name == null || reps == null) return null
            return WorkoutStep(name = name, reps = reps, notes = notes)
        }

        private fun reps(): Int? {
            val value = map["reps"]
            if (value == null) {
                problems += "$path/reps is required"
                return null
            }
            // SnakeYAML widens large numbers to Long/BigInteger; those are out of range anyway.
            if (value !is Int) {
                problems += "$path/reps must be an integer"
                return null
            }
            if (value !in 1..MAX_REPS) {
                problems += "$path/reps must be between 1 and $MAX_REPS"
                return null
            }
            return value
        }
    }
}
