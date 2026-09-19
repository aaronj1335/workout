package andersonstacy.workout.tools

import andersonstacy.workout.tools.WorkoutCatalogBuilder.Result
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every file checked in under `workouts/` has to compile, so a bad pull request fails CI. */
class ValidateWorkoutsTest {

    @Test
    fun `the checked-in workouts are valid`() {
        val files = workoutsDir().listFiles().orEmpty().filter(WorkoutFile::isWorkoutFile).sortedBy(File::getName)
        assertTrue("no workout files found in ${workoutsDir()}", files.isNotEmpty())

        val result = WorkoutCatalogBuilder.build(files, displayName = { "workouts/${it.name}" })

        if (result is Result.Failure) {
            throw AssertionError(result.problems.joinToString("\n", prefix = "invalid workout files:\n"))
        }
    }

    /** Bazel runs the test from the runfiles root, where `//workouts` is a data dependency. */
    private fun workoutsDir(): File {
        val runfiles = System.getenv("TEST_SRCDIR")
        val workspace = System.getenv("TEST_WORKSPACE")
        return if (runfiles != null && workspace != null) File(File(runfiles, workspace), "workouts") else File("workouts")
    }
}
