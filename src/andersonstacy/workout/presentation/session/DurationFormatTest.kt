package andersonstacy.workout.presentation.session

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    @Test
    fun `formats minutes and seconds`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:09", formatDuration(9_500))
        assertEquals("14:32", formatDuration(872_000))
    }

    @Test
    fun `formats hours when a session runs long`() {
        assertEquals("1:02:03", formatDuration(3_723_000))
    }

    @Test
    fun `a negative clock adjustment reads as zero`() {
        assertEquals("0:00", formatDuration(-5_000))
    }
}
