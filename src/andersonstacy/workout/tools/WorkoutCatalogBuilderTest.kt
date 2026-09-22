package andersonstacy.workout.tools

import andersonstacy.workout.data.WorkoutCatalog
import andersonstacy.workout.data.WorkoutJson
import andersonstacy.workout.tools.WorkoutCatalogBuilder.Result
import java.io.File
import java.nio.file.Files
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutCatalogBuilderTest {

    private val dir: File = Files.createTempDirectory("workouts").toFile()

    @Test
    fun `keeps the order the file lists`() {
        val file = write(
            """
            workouts:
              - {id: agility, name: Agility, steps: [{name: Sprint, reps: 4}]}
              - {id: plank, name: Plank, steps: [{name: Hold, reps: 3, notes: 30s}]}
            """.trimIndent(),
        )

        val catalog = success(WorkoutCatalogBuilder.build(file, now = Instant.parse("2026-09-19T20:19:37.123Z")))

        assertEquals(WorkoutCatalog.CURRENT_VERSION, catalog.version)
        assertEquals("2026-09-19T20:19:37Z", catalog.generatedAt)
        assertEquals(listOf("agility", "plank"), catalog.workouts.map { it.id })
    }

    @Test
    fun `problems are prefixed with the file`() {
        val file = write("workouts:\n  - {id: b, name: B, steps: [{name: Run, reps: 0}]}")

        val problems = failure(WorkoutCatalogBuilder.build(file, displayName = { "workouts/${it.name}" }))

        assertEquals(listOf("workouts/workouts.yaml: /workouts/0/steps/0/reps must be between 1 and 999"), problems)
    }

    @Test
    fun `a missing file is a failure rather than an empty catalog`() {
        val missing = File(dir, "nope.yaml")

        assertEquals(listOf("${missing.path}: no such file"), failure(WorkoutCatalogBuilder.build(missing)))
    }

    @Test
    fun `the written JSON is what the watch reads`() {
        val file = write("workouts:\n  - {id: plank, name: Plank, steps: [{name: Hold, reps: 3}]}")
        val catalog = success(WorkoutCatalogBuilder.build(file))

        val json = CatalogJson.encodeToString(catalog)

        assertTrue("version should be written explicitly:\n$json", "\"version\": 1" in json)
        assertFalse("absent optional fields should be omitted, not null:\n$json", "null" in json)
        assertEquals(catalog, WorkoutJson.decodeFromString<WorkoutCatalog>(json))
    }

    @After
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun write(yaml: String): File = File(dir, WorkoutsFile.FILE_NAME).apply { writeText(yaml) }

    private fun success(result: Result) = (result as? Result.Success)?.catalog
        ?: throw AssertionError("expected a catalog, got $result")

    private fun failure(result: Result) = (result as? Result.Failure)?.problems
        ?: throw AssertionError("expected problems, got $result")
}
