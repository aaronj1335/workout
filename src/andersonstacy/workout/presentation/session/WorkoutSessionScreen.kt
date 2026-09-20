package andersonstacy.workout.presentation.session

import andersonstacy.workout.R
import andersonstacy.workout.domain.SessionState
import andersonstacy.workout.presentation.FixedScreenScaffold
import andersonstacy.workout.presentation.KeepScreenOn
import andersonstacy.workout.presentation.MessageScreen
import andersonstacy.workout.presentation.PreviewData
import andersonstacy.workout.presentation.theme.WorkoutTheme
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import java.util.Locale

@Composable
fun WorkoutSessionRoute(
    onFinished: () -> Unit,
    viewModel: WorkoutSessionViewModel = viewModel(factory = WorkoutSessionViewModel.Factory),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()

    when (val current = session) {
        null -> MessageScreen(title = stringResource(R.string.workout_missing))
        else -> {
            KeepScreenOn()
            WorkoutSessionScreen(
                session = current,
                elapsedMillis = viewModel.elapsedMillis,
                onAdvance = viewModel::advance,
                onBack = viewModel::back,
                onFinished = onFinished,
            )
        }
    }
}

@Composable
fun WorkoutSessionScreen(
    session: SessionState,
    elapsedMillis: Long?,
    onAdvance: () -> Unit,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val animatedProgress by animateFloatAsState(
        targetValue = session.progress,
        label = "workoutProgress",
    )

    LaunchedEffect(session.isComplete) {
        if (session.isComplete) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    FixedScreenScaffold(
        behindContent = {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize().padding(2.dp),
            )
        },
        edgeButton = {
            EdgeButton(
                onClick = {
                    if (session.isComplete) {
                        onFinished()
                    } else {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAdvance()
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text(
                    when {
                        session.isComplete -> stringResource(R.string.done)
                        session.isLastStep -> stringResource(R.string.finish)
                        else -> stringResource(R.string.done)
                    },
                )
            }
        },
    ) {
        if (session.isComplete) {
            CompleteContent(session = session, elapsedMillis = elapsedMillis)
        } else {
            StepContent(session = session, onBack = onBack)
        }
    }
}

@Composable
private fun StepContent(session: SessionState, onBack: () -> Unit) {
    val step = session.currentStep ?: return
    val haptics = LocalHapticFeedback.current
    val animatedProgress by animateFloatAsState(
        targetValue = session.progress,
        label = "workoutProgress",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.step_progress, session.stepNumber, session.totalSteps),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = step.name,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = stringResource(R.string.reps, step.reps),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp),
        )
        // Held in a local: `notes` belongs to another module, so it cannot be smart cast.
        val notes = step.notes
        if (notes != null) {
            Text(
                text = notes,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (session.canGoBack) {
            FilledTonalIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBack()
                },
                modifier = Modifier.padding(top = 8.dp).size(IconButtonDefaults.ExtraSmallButtonSize),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_step),
                )
            }
        }
    }
}

@Composable
private fun CompleteContent(session: SessionState, elapsedMillis: Long?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = stringResource(R.string.complete_title, session.workout.name),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        val steps = pluralStringResource(R.plurals.step_count, session.totalSteps, session.totalSteps)
        Text(
            text = if (elapsedMillis != null) {
                stringResource(R.string.complete_summary, steps, formatDuration(elapsedMillis))
            } else {
                steps
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** m:ss, or h:mm:ss for the rare long session. */
internal fun formatDuration(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

@WearPreviewDevices
@Composable
private fun WorkoutSessionPreview() {
    WorkoutTheme {
        AppScaffold {
            WorkoutSessionScreen(
                session = SessionState.of(PreviewData.soccerLadder, 1),
                elapsedMillis = null,
                onAdvance = {},
                onBack = {},
                onFinished = {},
            )
        }
    }
}

@WearPreviewDevices
@Composable
private fun WorkoutSessionCompletePreview() {
    WorkoutTheme {
        AppScaffold {
            WorkoutSessionScreen(
                session = SessionState.of(PreviewData.soccerLadder, PreviewData.soccerLadder.steps.size),
                elapsedMillis = 872_000,
                onAdvance = {},
                onBack = {},
                onFinished = {},
            )
        }
    }
}
