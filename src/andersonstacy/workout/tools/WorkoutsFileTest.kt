package andersonstacy.workout.tools

import andersonstacy.workout.tools.WorkoutsFile.Parsed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutsFileTest {

    @Test
    fun `reads a complete workout`() {
        val yaml = """
            workouts:
              - id: soccer-ladder
                name: Soccer Ladder
                description: Agility-ladder footwork for soccer
                steps:
                  - name: Icky shuffle
                    reps: 2
                    notes: Lead with the outside foot
                  - name: Hopscotch
                    reps: 4
        """.trimIndent()

        val workout = valid(WorkoutsFile.parse(yaml)).single()

        assertEquals("soccer-ladder", workout.id)
        assertEquals("Soccer Ladder", workout.name)
        assertEquals("Agility-ladder footwork for soccer", workout.description)
        assertEquals(2, workout.steps.size)
        assertEquals("Lead with the outside foot", workout.steps[0].notes)
        assertEquals(4, workout.steps[1].reps)
        assertNull(workout.steps[1].notes)
    }

    @Test
    fun `workouts keep the order they are listed in`() {
        val yaml = """
            workouts:
              - {id: plank, name: Plank, steps: [{name: Hold, reps: 3}]}
              - {id: agility, name: Agility, steps: [{name: Sprint, reps: 4}]}
              - {id: ladder, name: Ladder, steps: [{name: Run, reps: 1}]}
        """.trimIndent()

        assertEquals(listOf("plank", "agility", "ladder"), valid(WorkoutsFile.parse(yaml)).map { it.id })
    }

    @Test
    fun `optional fields may be left out`() {
        val yaml = "workouts:\n  - {id: plank, name: Plank, steps: [{name: Front plank, reps: 3}]}\n"

        val workout = valid(WorkoutsFile.parse(yaml)).single()

        assertNull(workout.description)
        assertNull(workout.steps.single().notes)
    }

    @Test
    fun `ids are required and must be lowercase slugs`() {
        assertProblem("/workouts/0/id is required", WorkoutsFile.parse("workouts:\n  - {name: Minimal, steps: [{name: Run, reps: 1}]}"))
        assertProblem("/workouts/0/id must be a lowercase slug", WorkoutsFile.parse(workout("Soccer Ladder")))
        assertProblem("/workouts/0/id must be a lowercase slug", WorkoutsFile.parse(workout("-ladder")))
    }

    @Test
    fun `two workouts cannot claim the same id`() {
        val yaml = """
            workouts:
              - {id: ladder, name: Ladder, steps: [{name: Run, reps: 1}]}
              - {id: agility, name: Agility, steps: [{name: Run, reps: 1}]}
              - {id: ladder, name: Other, steps: [{name: Run, reps: 1}]}
        """.trimIndent()

        assertEquals(
            listOf("/workouts/2/id \"ladder\" is already used by /workouts/0"),
            invalid(WorkoutsFile.parse(yaml)),
        )
    }

    @Test
    fun `required fields are reported by path`() {
        val problems = invalid(WorkoutsFile.parse("workouts:\n  - {id: empty, description: nothing else}"))

        assertEquals(listOf("/workouts/0/name is required", "/workouts/0/steps is required"), problems)
    }

    @Test
    fun `problems from every workout are reported together`() {
        val yaml = """
            workouts:
              - {id: a, steps: []}
              - {id: b, name: B, steps: [{name: Run, reps: 0}]}
        """.trimIndent()

        assertEquals(
            listOf(
                "/workouts/0/name is required",
                "/workouts/0/steps must have at least one step",
                "/workouts/1/steps/0/reps must be between 1 and 999",
            ),
            invalid(WorkoutsFile.parse(yaml)),
        )
    }

    @Test
    fun `steps need a name and integer reps`() {
        val yaml = """
            workouts:
              - id: broken
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

        val problems = invalid(WorkoutsFile.parse(yaml))

        assertEquals(
            listOf(
                "/workouts/0/steps/0/name is required",
                "/workouts/0/steps/1/reps must be an integer",
                "/workouts/0/steps/2/reps must be an integer",
                "/workouts/0/steps/3/reps must be between 1 and 999",
                "/workouts/0/steps/4/reps must be between 1 and 999",
                "/workouts/0/steps/5 must be a mapping with name and reps",
            ),
            problems,
        )
    }

    @Test
    fun `text has to fit on a watch`() {
        val yaml = """
            workouts:
              - id: long
                name: ${"x".repeat(41)}
                description: ${"x".repeat(121)}
                steps:
                  - name: ${"x".repeat(61)}
                    reps: 1
                    notes: ${"x".repeat(121)}
        """.trimIndent()

        val problems = invalid(WorkoutsFile.parse(yaml))

        assertEquals(
            listOf(
                "/workouts/0/name must be at most 40 characters (got 41)",
                "/workouts/0/description must be at most 120 characters (got 121)",
                "/workouts/0/steps/0/name must be at most 60 characters (got 61)",
                "/workouts/0/steps/0/notes must be at most 120 characters (got 121)",
            ),
            problems,
        )
    }

    @Test
    fun `empty strings are rejected`() {
        val yaml = "workouts:\n  - {id: x, name: \"\", steps: [{name: Run, reps: 1}]}"

        assertProblem("/workouts/0/name must not be empty", WorkoutsFile.parse(yaml))
    }

    @Test
    fun `workouts must be a non-empty list`() {
        assertProblem("/workouts is required", WorkoutsFile.parse("version: 1\n"))
        assertProblem("/workouts must be a list", WorkoutsFile.parse("workouts: ladder\n"))
        assertProblem("/workouts must have at least one workout", WorkoutsFile.parse("workouts: []\n"))
        assertProblem("/workouts/0 must be a mapping", WorkoutsFile.parse("workouts:\n  - just a string\n"))
    }

    @Test
    fun `steps must be a non-empty list`() {
        assertProblem("/workouts/0/steps must have at least one step", WorkoutsFile.parse("workouts:\n  - {id: none, name: None, steps: []}"))
        assertProblem("/workouts/0/steps must be a list", WorkoutsFile.parse("workouts:\n  - {id: none, name: None, steps: run}"))
    }

    @Test
    fun `unknown keys are rejected so typos do not silently drop data`() {
        val yaml = """
            workout:
              - id: typo
            workouts:
              - id: typo
                name: Typo
                descripton: oops
                steps:
                  - name: Run
                    reps: 1
                    note: oops
        """.trimIndent()

        val problems = invalid(WorkoutsFile.parse(yaml))

        assertEquals(
            listOf(
                "/ unknown key \"workout\"; allowed: workouts",
                "/workouts/0/ unknown key \"descripton\"; allowed: id, name, description, steps",
                "/workouts/0/steps/0/ unknown key \"note\"; allowed: name, reps, notes",
            ),
            problems,
        )
    }

    @Test
    fun `a document that is not a mapping is rejected`() {
        assertProblem("expected a YAML mapping", WorkoutsFile.parse("- just\n- a list\n"))
        assertProblem("expected a YAML mapping", WorkoutsFile.parse(""))
    }

    @Test
    fun `syntax errors name the line`() {
        val problems = invalid(WorkoutsFile.parse("workouts:\n  - name: [unclosed\n"))

        assertEquals(listOf("line 3: expected ',' or ']', but got <stream end>"), problems)
    }

    private fun workout(id: String) = "workouts:\n  - {id: $id, name: Minimal, steps: [{name: Run, reps: 1}]}"

    private fun valid(parsed: Parsed) = (parsed as? Parsed.Valid)?.workouts
        ?: throw AssertionError("expected valid workouts, got $parsed")

    private fun invalid(parsed: Parsed) = (parsed as? Parsed.Invalid)?.problems
        ?: throw AssertionError("expected problems, got $parsed")

    private fun assertProblem(expected: String, parsed: Parsed) {
        val problems = invalid(parsed)
        assertTrue("expected a problem containing \"$expected\" in $problems", problems.any { expected in it })
    }
}
