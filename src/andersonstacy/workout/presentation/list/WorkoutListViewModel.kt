package andersonstacy.workout.presentation.list

import andersonstacy.workout.data.CatalogState
import andersonstacy.workout.data.WorkoutRepository
import andersonstacy.workout.presentation.appContainer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.StateFlow

class WorkoutListViewModel(private val repository: WorkoutRepository) : ViewModel() {

    val state: StateFlow<CatalogState> = repository.state

    fun refresh() = repository.refresh()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { WorkoutListViewModel(appContainer().repository) }
        }
    }
}
