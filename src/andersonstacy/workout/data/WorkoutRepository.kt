package andersonstacy.workout.data

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Why a fetch failed, kept out of the UI's wording so screens can use string resources. */
enum class CatalogError {
    /** The watch could not reach the server. */
    NETWORK,

    /** The server answered with something we could not read. */
    DATA,
}

sealed interface CatalogState {
    data object Loading : CatalogState

    /**
     * @param fromCache the catalog was read from disk and the network copy has not arrived yet.
     * @param refreshError set when we are showing a catalog but the latest refresh failed.
     */
    data class Ready(
        val catalog: WorkoutCatalog,
        val fromCache: Boolean = false,
        val refreshError: CatalogError? = null,
    ) : CatalogState

    data class Error(val reason: CatalogError) : CatalogState
}

/**
 * Cache-first: show whatever we have on disk immediately, then replace it with the network copy.
 * A failed refresh never takes a usable catalog away from the user mid-workout.
 */
class WorkoutRepository(
    private val remote: WorkoutRemoteSource,
    private val cache: WorkoutCache,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val _state = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            if (_state.value is CatalogState.Loading) {
                val cached = withContext(ioDispatcher) { cache.read() }
                if (cached != null) {
                    _state.value = CatalogState.Ready(cached, fromCache = true)
                }
            }
            try {
                val fresh = remote.fetch()
                withContext(ioDispatcher) { runCatching { cache.write(fresh) } }
                _state.value = CatalogState.Ready(fresh)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                val reason = error.toCatalogError()
                _state.value = when (val current = _state.value) {
                    is CatalogState.Ready -> current.copy(refreshError = reason)
                    else -> CatalogState.Error(reason)
                }
            }
        }
    }

    fun workout(id: String): Workout? =
        (_state.value as? CatalogState.Ready)?.catalog?.workouts?.firstOrNull { it.id == id }

    private fun Exception.toCatalogError(): CatalogError = when (this) {
        is IOException -> CatalogError.NETWORK
        else -> CatalogError.DATA
    }
}
