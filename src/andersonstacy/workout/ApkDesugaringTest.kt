package andersonstacy.workout

import java.io.File
import java.util.zip.ZipFile
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards against shipping Java 8 `invokedynamic` to the watch.
 *
 * Lambdas compiled for the JVM are bootstrapped by `LambdaMetafactory`, which ART blocks, so the
 * build has to desugar them away before dexing (the dexer itself is dx-compatible and leaves
 * `invokedynamic` alone). A jar that slips past the desugaring step still builds and installs
 * fine, then throws `NoSuchMethodError` on the device the moment one of its lambdas runs, so
 * check the dex files instead: a desugared APK never mentions `LambdaMetafactory`.
 */
class ApkDesugaringTest {

    @Test
    fun `no dex file in the APK references LambdaMetafactory`() {
        val apk = apkFile()
        assertTrue("APK not found at $apk", apk.isFile)

        val offenders = ZipFile(apk).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.endsWith(".dex") }
                .filter { entry -> zip.getInputStream(entry).use { it.readBytes() }.contains(MARKER) }
                .map { it.name }
                .toList()
        }

        assertTrue(
            "undesugared invokedynamic in ${offenders.joinToString()}: some jar in the APK is " +
                "reachable only through `associates`, which the dex/desugar aspect does not walk",
            offenders.isEmpty(),
        )
    }

    /** Bazel runs the test from the runfiles root, where `//src:app` is a data dependency. */
    private fun apkFile(): File {
        val runfiles = System.getenv("TEST_SRCDIR")
        val workspace = System.getenv("TEST_WORKSPACE")
        return if (runfiles != null && workspace != null) {
            File(File(runfiles, workspace), "src/app.apk")
        } else {
            File("bazel-bin/src/app.apk")
        }
    }

    private companion object {
        val MARKER = "LambdaMetafactory".toByteArray(Charsets.UTF_8)
    }
}

/** Naive byte-sequence search; the dex files are a few megabytes, so this runs in no time. */
private fun ByteArray.contains(needle: ByteArray): Boolean {
    outer@ for (start in 0..size - needle.size) {
        for (i in needle.indices) {
            if (this[start + i] != needle[i]) continue@outer
        }
        return true
    }
    return false
}
