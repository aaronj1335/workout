package andersonstacy.workout.data

import kotlinx.serialization.json.Json

/**
 * Lenient on purpose: the published catalog can gain fields (a step duration, say) without
 * breaking watches running an older build of the app.
 */
val WorkoutJson: Json = Json { ignoreUnknownKeys = true }
