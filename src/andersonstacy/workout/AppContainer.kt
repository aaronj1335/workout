package andersonstacy.workout

import android.content.Context
import andersonstacy.workout.data.FileWorkoutCache
import andersonstacy.workout.data.HttpWorkoutRemoteSource
import andersonstacy.workout.data.WorkoutRepository
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

/** Hand-rolled dependency graph; the app is too small to earn a DI framework. */
class AppContainer(context: Context) {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // A watch on a flaky connection should fall back to the cache quickly.
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val repository: WorkoutRepository by lazy {
        WorkoutRepository(
            remote = HttpWorkoutRemoteSource(
                client = httpClient,
                url = BuildConfig.WORKOUTS_URL,
                ioDispatcher = Dispatchers.IO,
            ),
            cache = FileWorkoutCache(File(context.filesDir, CACHE_FILE_NAME)),
            scope = applicationScope,
            ioDispatcher = Dispatchers.IO,
        )
    }

    private companion object {
        const val CACHE_FILE_NAME = "workouts.json"
    }
}
