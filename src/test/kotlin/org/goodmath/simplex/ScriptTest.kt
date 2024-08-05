package org.goodmath.simplex

import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.assertEquals

class TestScript(val prefix: String, val name: String) {
    fun program(): Path {
        return Path("$prefix/$name/$name.s3d")
    }

    fun expected(): Map<String, Path> {
        val dir = Path("$prefix/$name")
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
        System.out.println("Testing script '$name.s3d'")
        val cmd = Simplex()

        val tmpDir = Files.createTempDirectory("test-$name")
        val out = cmd.test("--prefix=$tmpDir/$name-out", "--verbosity=2", program().toString())
        val exp = expected()
        val act = actual(tmpDir)
        val stdout = Path("$prefix/$name/stdout.txt").readText()
        assertEquals(stdout, out.stdout)
        val stderr = Path("$prefix/$name/stderr.txt").readText()
        assertEquals(stderr, out.stderr)
        assertEquals(exp.keys, act.keys)
        for (k in exp.keys) {
            val expTxt = exp[k]?.readText()!!
            val actTxt =  act[k]?.readText()!!
            assertEquals(expTxt, actTxt, "Output $k")
        }
    }

}

class ScriptTest {
    val prefix = "./src/test/resources/scripts"
    @Test
    fun run() {
        val tests = File(prefix).listFiles()!!.map { it.name }
        for (test in tests) {
            val tc = TestScript(prefix, test)
            tc.run()
        }
    }
}
