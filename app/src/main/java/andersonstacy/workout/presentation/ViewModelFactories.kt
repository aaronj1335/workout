package andersonstacy.workout.presentation

import andersonstacy.workout.AppContainer
import andersonstacy.workout.WorkoutApplication
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

/** The app's dependency graph, reachable from a [androidx.lifecycle.ViewModel] factory. */
fun CreationExtras.appContainer(): AppContainer {
    val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
    return (application as WorkoutApplication).container
}
