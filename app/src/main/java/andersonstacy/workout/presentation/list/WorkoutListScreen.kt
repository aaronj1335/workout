package andersonstacy.workout.presentation.list

import andersonstacy.workout.R
import andersonstacy.workout.data.CatalogError
import andersonstacy.workout.data.CatalogState
import andersonstacy.workout.data.Workout
import andersonstacy.workout.presentation.MessageScreen
import andersonstacy.workout.presentation.PreviewData
import andersonstacy.workout.presentation.theme.WorkoutTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
fun WorkoutListRoute(
    onWorkoutClick: (String) -> Unit,
    viewModel: WorkoutListViewModel = viewModel(factory = WorkoutListViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Pick up workouts published since the app was last opened.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.refresh() }

    WorkoutListScreen(
        state = state,
        onWorkoutClick = onWorkoutClick,
        onRefresh = viewModel::refresh,
    )
}

@Composable
fun WorkoutListScreen(
    state: CatalogState,
    onWorkoutClick: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    when (state) {
        is CatalogState.Loading -> LoadingScreen()

        is CatalogState.Error -> MessageScreen(
            title = stringResource(R.string.load_error_title),
            body = stringResource(
                when (state.reason) {
                    CatalogError.NETWORK -> R.string.load_error_network
                    CatalogError.DATA -> R.string.load_error_data
                },
            ),
            actionLabel = stringResource(R.string.retry),
            onAction = onRefresh,
        )

        is CatalogState.Ready ->
            if (state.catalog.workouts.isEmpty()) {
                MessageScreen(
                    title = stringResource(R.string.empty_title),
                    body = stringResource(R.string.empty_body),
                    actionLabel = stringResource(R.string.refresh),
                    onAction = onRefresh,
                )
            } else {
                WorkoutList(
                    workouts = state.catalog.workouts,
                    showOfflineNotice = state.refreshError != null || state.fromCache,
                    onWorkoutClick = onWorkoutClick,
                    onRefresh = onRefresh,
                )
            }
    }
}

@Composable
private fun LoadingScreen() {
    ScreenScaffold {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun WorkoutList(
    workouts: List<Workout>,
    showOfflineNotice: Boolean,
    onWorkoutClick: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(stringResource(R.string.workouts_title))
                }
            }

            if (showOfflineNotice) {
                item {
                    Text(
                        text = stringResource(R.string.offline_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                }
            }

            items(workouts, key = { it.id }) { workout ->
                Button(
                    onClick = { onWorkoutClick(workout.id) },
                    label = { Text(workout.name) },
                    secondaryLabel = {
                        Text(pluralStringResource(R.plurals.step_count, workout.steps.size, workout.steps.size))
                    },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }

            // A list item rather than an EdgeButton so it scrolls away with the content.
            item {
                Button(
                    onClick = onRefresh,
                    label = { Text(stringResource(R.string.refresh)) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
        }
    }
}

@WearPreviewDevices
@Composable
private fun WorkoutListPreview() {
    WorkoutTheme {
        AppScaffold {
            WorkoutListScreen(
                state = CatalogState.Ready(PreviewData.catalog),
                onWorkoutClick = {},
                onRefresh = {},
            )
        }
    }
}

@WearPreviewDevices
@Composable
private fun WorkoutListOfflinePreview() {
    WorkoutTheme {
        AppScaffold {
            WorkoutListScreen(
                state = CatalogState.Ready(
                    PreviewData.catalog,
                    fromCache = true,
                    refreshError = CatalogError.NETWORK,
                ),
                onWorkoutClick = {},
                onRefresh = {},
            )
        }
    }
}

@WearPreviewDevices
@Composable
private fun WorkoutListErrorPreview() {
    WorkoutTheme {
        AppScaffold {
            WorkoutListScreen(
                state = CatalogState.Error(CatalogError.NETWORK),
                onWorkoutClick = {},
                onRefresh = {},
            )
        }
    }
}
