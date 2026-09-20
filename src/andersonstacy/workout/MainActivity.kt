package andersonstacy.workout

import android.os.Bundle
import andersonstacy.workout.presentation.WorkoutNavHost
import andersonstacy.workout.presentation.theme.WorkoutTheme
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    WorkoutTheme {
        AppScaffold {
            WorkoutNavHost()
        }
    }
}
