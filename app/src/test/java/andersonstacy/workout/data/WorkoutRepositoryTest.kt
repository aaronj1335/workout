package andersonstacy.workout.data

import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutRepositoryTest {

    private val cached = catalogOf("cached-workout")
    private val fresh = catalogOf("fresh-workout")

    @Test
    fun `network catalog replaces the cached one and is written back`() = runTest {
        val cache = FakeCache(stored = cached)
        val repository = repository(remote = { fresh }, cache = cache)

        repository.refresh()
        runCurrent()

        assertEquals(CatalogState.Ready(fresh), repository.state.value)
        assertEquals(fresh, cache.stored)
    }

    @Test
    fun `a failed refresh keeps the cached catalog and reports the problem`() = runTest {
        val cache = FakeCache(stored = cached)
        val repository = repository(remote = { throw IOException("Network unavailable") }, cache = cache)

        repository.refresh()
        runCurrent()

        assertEquals(
            CatalogState.Ready(cached, fromCache = true, refreshError = CatalogError.NETWORK),
            repository.state.value,
        )
    }

    @Test
    fun `a failed refresh with nothing cached is an error`() = runTest {
        val repository = repository(
            remote = { throw IOException("Network unavailable") },
            cache = FakeCache(stored = null),
        )

        repository.refresh()
        runCurrent()

        assertEquals(CatalogState.Error(CatalogError.NETWORK), repository.state.value)
    }

    @Test
    fun `retrying after a failure recovers`() = runTest {
        var attempt = 0
        val repository = repository(
            remote = {
                if (attempt++ == 0) throw IOException("Network unavailable") else fresh
            },
            cache = FakeCache(stored = null),
        )

        repository.refresh()
        runCurrent()
        assertEquals(CatalogState.Error(CatalogError.NETWORK), repository.state.value)

        repository.refresh()
        runCurrent()
        assertEquals(CatalogState.Ready(fresh), repository.state.value)
    }

    @Test
    fun `a catalog we cannot parse is a data error, not a network one`() = runTest {
        val repository = repository(
            remote = { throw IllegalStateException("bad json") },
            cache = FakeCache(stored = null),
        )

        repository.refresh()
        runCurrent()

        assertEquals(CatalogState.Error(CatalogError.DATA), repository.state.value)
    }

    @Test
    fun `workout looks up by id`() = runTest {
        val repository = repository(remote = { fresh }, cache = FakeCache(stored = null))

        repository.refresh()
        runCurrent()

        assertEquals("fresh-workout", repository.workout("fresh-workout")?.id)
        assertEquals(null, repository.workout("nope"))
    }

    private fun kotlinx.coroutines.test.TestScope.repository(
        remote: WorkoutRemoteSource,
        cache: WorkoutCache,
    ) = WorkoutRepository(
        remote = remote,
        cache = cache,
        scope = backgroundScope,
        ioDispatcher = UnconfinedTestDispatcher(testScheduler),
    )

    private class FakeCache(var stored: WorkoutCatalog?) : WorkoutCache {
        override fun read(): WorkoutCatalog? = stored

        override fun write(catalog: WorkoutCatalog) {
            stored = catalog
        }
    }

    private fun catalogOf(workoutId: String) = WorkoutCatalog(
        workouts = listOf(
            Workout(
                id = workoutId,
                name = workoutId,
                steps = listOf(WorkoutStep(name = "Step", reps = 1)),
            ),
        ),
    )
}
