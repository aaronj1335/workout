package andersonstacy.workout.tools

import andersonstacy.workout.tools.WorkoutFile.Parsed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutFileTest {

    @Test
    fun `reads a complete workout`() {
        val yaml = """
            name: Soccer Ladder
            description: Agility-ladder footwork for soccer
            steps:
              - name: Icky shuffle
                reps: 2
                notes: Lead with the outside foot
              - name: Hopscotch
                reps: 4
        """.trimIndent()

        val workout = valid(WorkoutFile.parse(yaml, defaultId = "soccer-ladder"))

        assertEquals("soccer-ladder", workout.id)
        assertEquals("Soccer Ladder", workout.name)
        assertEquals("Agility-ladder footwork for soccer", workout.description)
        assertEquals(2, workout.steps.size)
        assertEquals("Lead with the outside foot", workout.steps[0].notes)
        assertEquals(4, workout.steps[1].reps)
        assertNull(workout.steps[1].notes)
    }

    @Test
    fun `optional fields may be left out`() {
        val workout = valid(WorkoutFile.parse("name: Plank\nsteps:\n  - {name: Front plank, reps: 3}\n", "plank"))

        assertNull(workout.description)
        assertNull(workout.steps.single().notes)
    }

    @Test
    fun `an explicit id overrides the file name`() {
        val workout = valid(WorkoutFile.parse("id: ladder\nname: Ladder\nsteps: [{name: Run, reps: 1}]", "soccer-ladder"))

        assertEquals("ladder", workout.id)
    }

    @Test
    fun `ids must be lowercase slugs`() {
        assertProblem("/id must be a lowercase slug", WorkoutFile.parse(minimal, defaultId = "Soccer Ladder"))
        assertProblem("/id must be a lowercase slug", WorkoutFile.parse(minimal, defaultId = "-ladder"))
    }

    @Test
    fun `required fields are reported by path`() {
        val problems = invalid(WorkoutFile.parse("description: nothing else", "empty"))

        assertEquals(listOf("/name is required", "/steps is required"), problems)
    }

    @Test
    fun `steps need a name and integer reps`() {
        val yaml = """
            name: Broken
            steps:
              - reps: 2
              - name: Text reps
                reps: "2"
              - name: Fractional reps
                reps: 1.5
              - name: Zero reps
                reps: 0
              - name: Too many reps
                reps: 1000
              - just a string
        """.trimIndent()

        val problems = invalid(WorkoutFile.parse(yaml, "broken"))

        assertEquals(
            listOf(
                "/steps/0/name is required",
                "/steps/1/reps must be an integer",
                "/steps/2/reps must be an integer",
                "/steps/3/reps must be between 1 and 999",
                "/steps/4/reps must be between 1 and 999",
                "/steps/5 must be a mapping with name and reps",
            ),
            problems,
        )
    }

    @Test
    fun `text has to fit on a watch`() {
        val yaml = """
            name: ${"x".repeat(41)}
            description: ${"x".repeat(121)}
            steps:
              - name: ${"x".repeat(61)}
                reps: 1
                notes: ${"x".repeat(121)}
        """.trimIndent()

        val problems = invalid(WorkoutFile.parse(yaml, "long"))

        assertEquals(
            listOf(
                "/name must be at most 40 characters (got 41)",
                "/description must be at most 120 characters (got 121)",
                "/steps/0/name must be at most 60 characters (got 61)",
                "/steps/0/notes must be at most 120 characters (got 121)",
            ),
            problems,
        )
    }

    @Test
    fun `empty strings are rejected`() {
        assertProblem("/name must not be empty", WorkoutFile.parse("name: \"\"\nsteps: [{name: Run, reps: 1}]", "x"))
    }

    @Test
    fun `steps must be a non-empty list`() {
        assertProblem("/steps must have at least one step", WorkoutFile.parse("name: None\nsteps: []", "none"))
        assertProblem("/steps must be a list", WorkoutFile.parse("name: None\nsteps: run", "none"))
    }

    @Test
    fun `unknown keys are rejected so typos do not silently drop data`() {
        val yaml = """
            name: Typo
            descripton: oops
            steps:
              - name: Run
                reps: 1
                note: oops
        """.trimIndent()

        val problems = invalid(WorkoutFile.parse(yaml, "typo"))

        assertEquals(
            listOf(
                "/ unknown key \"descripton\"; allowed: id, name, description, steps",
                "/steps/0/ unknown key \"note\"; allowed: name, reps, notes",
            ),
            problems,
        )
    }

    @Test
    fun `a document that is not a mapping is rejected`() {
        assertProblem("expected a YAML mapping", WorkoutFile.parse("- just\n- a list\n", "list"))
        assertProblem("expected a YAML mapping", WorkoutFile.parse("", "blank"))
    }

    @Test
    fun `syntax errors name the line`() {
        val problems = invalid(WorkoutFile.parse("name: Bad\nsteps:\n  - name: [unclosed\n", "bad"))

        assertEquals(listOf("line 4: expected ',' or ']', but got <stream end>"), problems)
    }

    @Test
    fun `file name decides whether a file is a workout`() {
        assertEquals("soccer-ladder", WorkoutFile.idFor(java.io.File("workouts/soccer-ladder.yaml")))
        assertEquals("agility", WorkoutFile.idFor(java.io.File("agility.yml")))
        assertNull(WorkoutFile.idFor(java.io.File("workouts/BUILD.bazel")))
        assertNull(WorkoutFile.idFor(java.io.File("notes.yaml.bak")))
    }

    private val minimal = "name: Minimal\nsteps: [{name: Run, reps: 1}]"

    private fun valid(parsed: Parsed) = (parsed as? Parsed.Valid)?.workout
        ?: throw AssertionError("expected a valid workout, got $parsed")

    private fun invalid(parsed: Parsed) = (parsed as? Parsed.Invalid)?.problems
        ?: throw AssertionError("expected problems, got $parsed")

    private fun assertProblem(expected: String, parsed: Parsed) {
        val problems = invalid(parsed)
        assertTrue("expected a problem containing \"$expected\" in $problems", problems.any { expected in it })
    }
}
