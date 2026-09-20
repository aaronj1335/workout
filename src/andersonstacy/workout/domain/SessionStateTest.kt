package andersonstacy.workout.domain

import andersonstacy.workout.data.Workout
import andersonstacy.workout.data.WorkoutStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStateTest {

    private val workout = Workout(
        id = "ladder",
        name = "Soccer Ladder",
        steps = listOf(
            WorkoutStep("Icky shuffle", 2),
            WorkoutStep("Two feet in each square", 4),
            WorkoutStep("Sprint out", 1),
        ),
    )

    @Test
    fun `starts on the first step`() {
        val session = SessionState.of(workout, 0)

        assertEquals(workout.steps[0], session.currentStep)
        assertEquals(1, session.stepNumber)
        assertEquals(3, session.totalSteps)
        assertFalse(session.isComplete)
        assertFalse(session.canGoBack)
        assertEquals(0f, session.progress, 0f)
    }

    @Test
    fun `advances through every step and then completes`() {
        var session = SessionState.of(workout, 0)

        session = session.next()
        assertEquals(workout.steps[1], session.currentStep)
        assertFalse(session.isLastStep)

        session = session.next()
        assertTrue(session.isLastStep)
        assertFalse(session.isComplete)

        session = session.next()
        assertTrue(session.isComplete)
        assertNull(session.currentStep)
        assertEquals(1f, session.progress, 0f)
    }

    @Test
    fun `next stops at the end and previous stops at the start`() {
        val completed = SessionState.of(workout, 3)
        assertEquals(3, completed.next().stepIndex)

        val first = SessionState.of(workout, 0)
        assertEquals(0, first.previous().stepIndex)
    }

    @Test
    fun `previous goes back a step`() {
        val session = SessionState.of(workout, 2)

        assertTrue(session.canGoBack)
        assertEquals(1, session.previous().stepIndex)
    }

    @Test
    fun `progress is the fraction of steps finished`() {
        assertEquals(1f / 3f, SessionState.of(workout, 1).progress, 0.0001f)
        assertEquals(2f / 3f, SessionState.of(workout, 2).progress, 0.0001f)
    }

    @Test
    fun `out of range indexes are clamped instead of crashing`() {
        // A catalog refresh can shorten a workout while a session for it is open.
        assertEquals(3, SessionState.of(workout, 99).stepIndex)
        assertEquals(0, SessionState.of(workout, -5).stepIndex)
    }

    @Test
    fun `a workout with no steps is already complete`() {
        val empty = SessionState.of(Workout(id = "empty", name = "Empty", steps = emptyList()), 0)

        assertTrue(empty.isComplete)
        assertEquals(1f, empty.progress, 0f)
        assertEquals(0, empty.stepNumber)
    }
}
