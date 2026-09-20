package andersonstacy.workout.tools

import java.io.File
import kotlin.system.exitProcess
import kotlinx.serialization.json.Json

/**
 * Compiles workout YAML files into the workouts.json the watch fetches from GitHub Pages, plus
 * an index.html so the Pages root is readable. Normally run by Bazel (see //workouts:dist).
 *
 * Usage:
 *   build_workouts --out DIR FILE-OR-DIR...   compile into DIR/workouts.json and DIR/index.html
 *   build_workouts --check FILE-OR-DIR...     validate only, write nothing
 */
fun main(args: Array<String>) {
    var checkOnly = false
    var outputDir: File? = null
    val inputs = mutableListOf<File>()

    var index = 0
    while (index < args.size) {
        when (val arg = args[index]) {
            "--check" -> checkOnly = true
            "--out" -> outputDir = File(args.getOrNull(++index) ?: usage("--out needs a directory"))
            else -> if (arg.startsWith("--")) usage("unknown option $arg") else inputs += File(arg)
        }
        index++
    }
    if (!checkOnly && outputDir == null) usage("either --check or --out DIR is required")

    // A directory stands for every workout file in it.
    val files = inputs.flatMap { input ->
        if (input.isDirectory) {
            input.listFiles().orEmpty().filter(WorkoutFile::isWorkoutFile).sortedBy(File::getName)
        } else {
            listOf(input)
        }
    }

    val catalog = when (val result = WorkoutCatalogBuilder.build(files)) {
        is WorkoutCatalogBuilder.Result.Failure -> {
            System.err.println("${result.problems.size} problem(s) found:\n")
            result.problems.forEach { System.err.println("  $it") }
            System.err.println("\nSee the README for the expected shape of a workout file.")
            exitProcess(1)
        }
        is WorkoutCatalogBuilder.Result.Success -> result.catalog
    }

    val summary = "${catalog.workouts.size} workout(s), ${catalog.workouts.sumOf { it.steps.size }} step(s)"
    if (checkOnly) {
        println("OK: $summary.")
        return
    }

    val out = checkNotNull(outputDir)
    out.mkdirs()
    File(out, "workouts.json").writeText(CatalogJson.encodeToString(catalog) + "\n")
    File(out, "index.html").writeText(IndexHtml.render(catalog))
    println("Wrote ${File(out, "workouts.json")}: $summary.")
}

/** Human-readable, and leaves optional fields out rather than writing `null`. */
val CatalogJson: Json = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
    explicitNulls = false
}

private fun usage(problem: String): Nothing {
    System.err.println("build_workouts: $problem")
    System.err.println("usage: build_workouts (--check | --out DIR) FILE-OR-DIR...")
    exitProcess(2)
}
