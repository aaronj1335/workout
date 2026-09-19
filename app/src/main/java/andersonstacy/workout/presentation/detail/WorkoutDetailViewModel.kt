package andersonstacy.workout.presentation.detail

import andersonstacy.workout.data.CatalogState
import andersonstacy.workout.data.Workout
import andersonstacy.workout.data.WorkoutRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WorkoutDetailViewModel(
    repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val workoutId: String = checkNotNull(savedStateHandle[ARG_WORKOUT_ID])

    /** Null while the catalog is still loading, or if this workout has since been removed. */
    val workout: StateFlow<Workout?> = repository.state
        .map { state -> (state as? CatalogState.Ready)?.catalog?.workouts?.firstOrNull { it.id == workoutId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.workout(workoutId))

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { WorkoutDetailViewModel(appContainer().repository, createSavedStateHandle()) }
        }
    }
}
