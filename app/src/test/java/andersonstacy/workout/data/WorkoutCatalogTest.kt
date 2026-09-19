package andersonstacy.workout.data

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** The shape here has to match what `tools/build-workouts.mjs` emits. */
class WorkoutCatalogTest {

    @Test
    fun `decodes a published catalog`() {
        val json = """
            {
              "version": 1,
              "generatedAt": "2026-09-18T22:10:03Z",
              "workouts": [
                {
                  "id": "soccer-ladder",
                  "name": "Soccer Ladder",
                  "description": "Agility-ladder footwork for soccer",
                  "steps": [
                    { "name": "Icky shuffle", "reps": 2, "notes": "Lead with the outside foot" },
                    { "name": "Hopscotch", "reps": 4 }
                  ]
                }
              ]
            }
        """.trimIndent()

        val catalog = WorkoutJson.decodeFromString<WorkoutCatalog>(json)

        assertEquals(1, catalog.version)
        assertEquals("2026-09-18T22:10:03Z", catalog.generatedAt)
        val workout = catalog.workouts.single()
        assertEquals("soccer-ladder", workout.id)
        assertEquals("Agility-ladder footwork for soccer", workout.description)
        assertEquals(2, workout.steps.size)
        assertEquals("Lead with the outside foot", workout.steps[0].notes)
        assertNull(workout.steps[1].notes)
        assertEquals(4, workout.steps[1].reps)
    }

    @Test
    fun `ignores fields added by a newer publisher`() {
        val json = """
            {
              "version": 2,
              "workouts": [
                {
                  "id": "plank",
                  "name": "Plank",
                  "equipment": ["mat"],
                  "steps": [ { "name": "Front plank", "reps": 3, "seconds": 30 } ]
                }
              ]
            }
        """.trimIndent()

        val catalog = WorkoutJson.decodeFromString<WorkoutCatalog>(json)

        assertEquals("plank", catalog.workouts.single().id)
        assertEquals(3, catalog.workouts.single().steps.single().reps)
    }

    @Test
    fun `rejects a step with no reps`() {
        val json = """
            { "workouts": [ { "id": "x", "name": "X", "steps": [ { "name": "No reps" } ] } ] }
        """.trimIndent()

        assertThrows(SerializationException::class.java) {
            WorkoutJson.decodeFromString<WorkoutCatalog>(json)
        }
    }
}
