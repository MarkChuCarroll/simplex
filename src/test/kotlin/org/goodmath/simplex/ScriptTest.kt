package org.goodmath.simplex

import org.antlr.v4.runtime.CharStreams
import org.goodmath.simplex.parser.SimplexParseListener
import org.junit.jupiter.api.DisplayNameGenerator
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.math.exp
import kotlin.test.assertEquals


class TestCase(val name: String) {
    fun program(): Path {
        return Path("./src/test/resources/scripts/$name/$name.s3d")
    }

    fun expected(): Map<String, Path> {
        val dir = Path("./src/test/resources/scripts/$name")
        val result = HashMap<String, Path>()
        for (p in dir.toFile().listFiles()!!) {
            if (p.name.startsWith("$name-out-")) {
                val key = p.name.toString().substring("$name-out-".length)
                result[key] = p.toPath()
            }
        }
        return result
    }

    fun actual(testDir: Path): Map<String, Path> {
        val result = HashMap<String, Path>()
        for (p in testDir.toFile().listFiles()!!) {
            if (p.name.startsWith("$name-out-")) {
                val key = p.name.toString().substring("$name-out-".length)
                result[key] = p.toPath()
            }
        }
        return result
    }

    fun run() {
        val tmpDir = Files.createTempDirectory("test-$name")
        System.err.println("Tmp = $tmpDir")
        val model = SimplexParseListener().parse("$name.s3d", CharStreams.fromPath(program()))
        { a, b, c -> Unit }
        model.execute(null, "$tmpDir/$name-out") { l, st, e ->
            Unit
        }
        val exp = expected()
        val act = actual(tmpDir)
        assertEquals(exp.keys, act.keys)
        for (k in exp.keys) {
            val expTxt = exp[k]?.readText()!!
            val actTxt =  act[k]?.readText()!!
            assertEquals(expTxt, actTxt, "Output $k")
        }
    }

}

class ScriptTest {

    @Test
    fun run() {
        val tests = listOf("lambda", "loop", "method", "test")
        for (test in tests) {
            val tc = TestCase(test)
            tc.run()
        }
    }
}
