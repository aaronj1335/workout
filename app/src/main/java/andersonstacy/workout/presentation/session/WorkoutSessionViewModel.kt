package andersonstacy.workout.presentation.session

import andersonstacy.workout.data.CatalogState
import andersonstacy.workout.data.WorkoutRepository
import andersonstacy.workout.domain.SessionState
import andersonstacy.workout.presentation.ARG_WORKOUT_ID
import andersonstacy.workout.presentation.appContainer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Holds the position in a workout. The index lives in [SavedStateHandle] so a session survives
 * the watch killing the process mid-workout.
 */
class WorkoutSessionViewModel(
    repository: WorkoutRepository,
    private val savedStateHandle: SavedStateHandle,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val workoutId: String = checkNotNull(savedStateHandle[ARG_WORKOUT_ID])

    private val stepIndex: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_STEP_INDEX, 0)

    val session: StateFlow<SessionState?> =
        combine(repository.state, stepIndex) { catalogState, index ->
            val workout = (catalogState as? CatalogState.Ready)
                ?.catalog?.workouts?.firstOrNull { it.id == workoutId }
            workout?.let { SessionState.of(it, index) }
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            repository.workout(workoutId)?.let { SessionState.of(it, 0) },
        )

    init {
        if (savedStateHandle.get<Long>(KEY_STARTED_AT) == null) {
            savedStateHandle[KEY_STARTED_AT] = clock()
        }
    }

    /** How long the workout took, available once it is finished. */
    val elapsedMillis: Long?
        get() {
            val startedAt = savedStateHandle.get<Long>(KEY_STARTED_AT) ?: return null
            val finishedAt = savedStateHandle.get<Long>(KEY_FINISHED_AT) ?: return null
            return finishedAt - startedAt
        }

    fun advance() {
        val next = (session.value ?: return).next()
        savedStateHandle[KEY_STEP_INDEX] = next.stepIndex
        if (next.isComplete && savedStateHandle.get<Long>(KEY_FINISHED_AT) == null) {
            savedStateHandle[KEY_FINISHED_AT] = clock()
        }
    }

    fun back() {
        val previous = (session.value ?: return).previous()
        savedStateHandle[KEY_STEP_INDEX] = previous.stepIndex
    }

    companion object {
        private const val KEY_STEP_INDEX = "stepIndex"
        private const val KEY_STARTED_AT = "startedAt"
        private const val KEY_FINISHED_AT = "finishedAt"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { WorkoutSessionViewModel(appContainer().repository, createSavedStateHandle()) }
        }
    }
}
