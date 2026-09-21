package andersonstacy.workout.tools

import andersonstacy.workout.tools.WorkoutCatalogBuilder.Result
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** The checked-in `workouts/workouts.yaml` has to compile, so a bad pull request fails CI. */
class ValidateWorkoutsTest {

    @Test
    fun `the checked-in workouts are valid`() {
        val file = workoutsFile()
        assertTrue("no workout file at $file", file.isFile)

        val result = WorkoutCatalogBuilder.build(file, displayName = { "workouts/${it.name}" })

        if (result is Result.Failure) {
            throw AssertionError(result.problems.joinToString("\n", prefix = "invalid workout file:\n"))
        }
    }

    /** Bazel runs the test from the runfiles root, where `//workouts` is a data dependency. */
    private fun workoutsFile(): File {
        val runfiles = System.getenv("TEST_SRCDIR")
        val workspace = System.getenv("TEST_WORKSPACE")
        val dir = if (runfiles != null && workspace != null) File(File(runfiles, workspace), "workouts") else File("workouts")
        return File(dir, WorkoutsFile.FILE_NAME)
    }
}
