package andersonstacy.workout.data

import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** Fetches the published workout catalog. */
fun interface WorkoutRemoteSource {
    suspend fun fetch(): WorkoutCatalog
}

class HttpWorkoutRemoteSource(
    private val client: OkHttpClient,
    private val url: String,
    private val ioDispatcher: CoroutineDispatcher,
    private val json: Json = WorkoutJson,
) : WorkoutRemoteSource {

    override suspend fun fetch(): WorkoutCatalog = withContext(ioDispatcher) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Server returned ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response")
            json.decodeFromString<WorkoutCatalog>(body)
        }
    }
}
