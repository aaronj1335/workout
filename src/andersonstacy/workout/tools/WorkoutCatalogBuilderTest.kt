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

    @After
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `combines files in the order given`() {
        val agility = write("agility.yaml", "name: Agility\nsteps: [{name: Sprint, reps: 4}]")
        val plank = write("plank.yaml", "name: Plank\nsteps: [{name: Hold, reps: 3, notes: 30s}]")

        val catalog = success(WorkoutCatalogBuilder.build(listOf(agility, plank), now = Instant.parse("2026-09-19T20:19:37.123Z")))

        assertEquals(WorkoutCatalog.CURRENT_VERSION, catalog.version)
        assertEquals("2026-09-19T20:19:37Z", catalog.generatedAt)
        assertEquals(listOf("agility", "plank"), catalog.workouts.map { it.id })
    }

    @Test
    fun `problems from every file are reported together, prefixed with the file`() {
        val a = write("a.yaml", "steps: []")
        val b = write("b.yaml", "name: B\nsteps: [{name: Run, reps: 0}]")

        val problems = failure(WorkoutCatalogBuilder.build(listOf(a, b), displayName = { "workouts/${it.name}" }))

        assertEquals(
            listOf(
                "workouts/a.yaml: /name is required",
                "workouts/a.yaml: /steps must have at least one step",
                "workouts/b.yaml: /steps/0/reps must be between 1 and 999",
            ),
            problems,
        )
    }

    @Test
    fun `two files cannot claim the same id`() {
        val first = write("ladder.yaml", "name: Ladder\nsteps: [{name: Run, reps: 1}]")
        val second = write("other.yaml", "id: ladder\nname: Other\nsteps: [{name: Run, reps: 1}]")

        val problems = failure(WorkoutCatalogBuilder.build(listOf(first, second), displayName = File::getName))

        assertEquals(listOf("other.yaml: id \"ladder\" is already used by ladder.yaml"), problems)
    }

    @Test
    fun `no files is a failure rather than an empty catalog`() {
        assertEquals(listOf("no workout files given"), failure(WorkoutCatalogBuilder.build(emptyList())))
    }

    @Test
    fun `the written JSON is what the watch reads`() {
        val file = write("plank.yaml", "name: Plank\nsteps: [{name: Hold, reps: 3}]")
        val catalog = success(WorkoutCatalogBuilder.build(listOf(file)))

        val json = CatalogJson.encodeToString(catalog)

        assertTrue("version should be written explicitly:\n$json", "\"version\": 1" in json)
        assertFalse("absent optional fields should be omitted, not null:\n$json", "null" in json)
        assertEquals(catalog, WorkoutJson.decodeFromString<WorkoutCatalog>(json))
    }

    private fun write(name: String, yaml: String): File = File(dir, name).apply { writeText(yaml) }

    private fun success(result: Result) = (result as? Result.Success)?.catalog
        ?: throw AssertionError("expected a catalog, got $result")

    private fun failure(result: Result) = (result as? Result.Failure)?.problems
        ?: throw AssertionError("expected problems, got $result")
}
