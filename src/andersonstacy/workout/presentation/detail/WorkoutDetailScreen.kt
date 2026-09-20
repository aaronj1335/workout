package andersonstacy.workout.presentation.detail

import andersonstacy.workout.R
import andersonstacy.workout.data.Workout
import andersonstacy.workout.data.WorkoutStep
import andersonstacy.workout.presentation.MessageScreen
import andersonstacy.workout.presentation.PreviewData
import andersonstacy.workout.presentation.theme.WorkoutTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices

@Composable
fun WorkoutDetailRoute(
    onStart: () -> Unit,
    viewModel: WorkoutDetailViewModel = viewModel(factory = WorkoutDetailViewModel.Factory),
) {
    val workout by viewModel.workout.collectAsStateWithLifecycle()
    WorkoutDetailScreen(workout = workout, onStart = onStart)
}

@Composable
fun WorkoutDetailScreen(workout: Workout?, onStart: () -> Unit) {
    if (workout == null) {
        MessageScreen(title = stringResource(R.string.workout_missing))
        return
    }

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(onClick = onStart) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(stringResource(R.string.start))
            }
        },
    ) { contentPadding ->
        TransformingLazyColumn(contentPadding = contentPadding, state = listState) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(workout.name)
                }
            }
            itemsIndexed(workout.steps) { index, step ->
                StepRow(position = index + 1, step = step)
            }
        }
    }
}

@Composable
private fun StepRow(position: Int, step: WorkoutStep) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = "$position",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp),
        )
        Text(
            text = step.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.reps, step.reps),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@WearPreviewDevices
@Composable
private fun WorkoutDetailPreview() {
    WorkoutTheme {
        AppScaffold {
            WorkoutDetailScreen(workout = PreviewData.soccerLadder, onStart = {})
        }
    }
}
