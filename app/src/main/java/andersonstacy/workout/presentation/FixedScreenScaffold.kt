package andersonstacy.workout.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ScreenScaffold

/**
 * A screen whose content does not scroll, with an optional [edgeButton] pinned to the bottom.
 *
 * ScreenScaffold only accepts an edge button alongside a ScrollInfoProvider, and a provider built
 * from a state that cannot scroll reports `isScrollable = false`, which collapses the button to
 * nothing — it stays clickable but never draws. So these screens place the button themselves.
 */
@Composable
fun FixedScreenScaffold(
    modifier: Modifier = Modifier,
    behindContent: @Composable BoxScope.() -> Unit = {},
    edgeButton: @Composable (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    ScreenScaffold(modifier = modifier) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            behindContent()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(
                        top = ContentInset,
                        bottom = if (edgeButton == null) ContentInset else EdgeButtonInset,
                    ),
                contentAlignment = Alignment.Center,
                content = content,
            )
            if (edgeButton != null) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    edgeButton()
                }
            }
        }
    }
}

/** Keeps content clear of the curve at the top and bottom of a round display. */
private val ContentInset = 28.dp

/** Room for the edge button at the bottom of the screen. */
private val EdgeButtonInset = 56.dp
