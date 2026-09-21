package andersonstacy.workout.tools

import andersonstacy.workout.data.WorkoutCatalog

/** A small page for the GitHub Pages root, so the published data is readable by a human. */
object IndexHtml {

    fun render(catalog: WorkoutCatalog): String = buildString {
        append(
            """
            |<!doctype html>
            |<html lang="en">
            |  <head>
            |    <meta charset="utf-8" />
            |    <meta name="viewport" content="width=device-width, initial-scale=1" />
            |    <title>Workouts</title>
            |    <style>
            |      :root { color-scheme: light dark; }
            |      body { font: 16px/1.5 system-ui, sans-serif; margin: 0 auto; max-width: 42rem; padding: 2rem 1rem; }
            |      h1 { margin-bottom: 0.25rem; }
            |      code { background: color-mix(in srgb, currentColor 12%, transparent); border-radius: 4px; padding: 0 0.3em; font-size: 0.85em; }
            |      section { margin-top: 2rem; }
            |      i { opacity: 0.7; }
            |      footer { margin-top: 3rem; opacity: 0.7; font-size: 0.85em; }
            |    </style>
            |  </head>
            |  <body>
            |    <h1>Workouts</h1>
            |    <p>Data for the Pixel Watch workout app: <a href="workouts.json">workouts.json</a></p>
            |
            """.trimMargin(),
        )
        for (workout in catalog.workouts) {
            append("    <section>\n")
            append("      <h2>${escape(workout.name)} <code>${escape(workout.id)}</code></h2>\n")
            workout.description?.let { append("      <p>${escape(it)}</p>\n") }
            append("      <ol>\n")
            for (step in workout.steps) {
                append("        <li>${escape(step.name)} <b>&times;${step.reps}</b>")
                step.notes?.let { append(" <i>${escape(it)}</i>") }
                append("</li>\n")
            }
            append("      </ol>\n")
            append("    </section>\n")
        }
        append(
            """
            |    <footer>Generated ${escape(catalog.generatedAt.orEmpty())} from <code>workouts/workouts.yaml</code>.</footer>
            |  </body>
            |</html>
            |
            """.trimMargin(),
        )
    }

    private fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
