package org.goodmath.simplex.kcsg

/**
 * Fork of
 * https://github.com/fiji/fiji/blob/master/src-plugins/3D_Viewer/src/main/java/customnode/STLLoader.java
 *
 * TODO: license unclear
 */

import org.goodmath.simplex.vvecmath.Vector3d
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.lang.Double.parseDouble
import java.lang.Float.intBitsToFloat
import java.util.ArrayList
import kotlin.experimental.and

class STLLoader() {
    var line: String = ""

    // attributes of the currently read mesh
    private val vertices = ArrayList<Vector3d>()
    private val normal = Vector3d.zero().asModifiable() //to be used for file checking
    var triangles: Int = 0


    fun parse(f: File): ArrayList<Vector3d> {
        vertices.clear()

        // determine if this is a binary or ASCII STL
        // and send to the appropriate parsing method
        // Hypothesis 1: this is an ASCII STL
        val br = BufferedReader(FileReader(f))
        val line = br.readLine()
        val words = line.trim().split("\\s+")
        if (line.indexOf(0.toChar()) < 0 && words[0].lowercase() == "solid") {
            println("Looks like an ASCII STL")
            parseAscii(f)
            return vertices
        }

        // Hypothesis 2: this is a binary STL
        val fs = FileInputStream(f)

        // bytes 80, 81, 82 and 83 form a little-endian int
        // that contains the number of triangles
        val buffer = ByteArray(84)
        fs.read(buffer, 0, 84)
        val triangles =
            buffer[83].and(0xff.toByte()).toInt().shl(24).or(
                buffer[82].and(0xff.toByte()).toInt().shl(16)
            ).or(
                (buffer[81].and(0xff.toByte())).toInt().shl(8)
            ).or(
                buffer[80].and(0xff.toByte()).toInt()
            )
        if (((f.length() - 84) / 50).toInt() == triangles) {
            println("Looks like a binary STL")
            parseBinary(f)
            return vertices
        }
        println("File is not a valid STL")
        return vertices
    }

    fun parseAscii(f: File) {
        val input = BufferedReader(FileReader(f))
        val vertices = ArrayList<Vector3d>()
        val lines = input.readLines()
        for (line in lines) {
            val numbers = line.trim().split("\\s+")
            if (numbers[0] == "vertex") {
                val x = parseDouble(numbers[1])
                val y = parseDouble(numbers[2])
                val z = parseDouble(numbers[3])
                val vertex = Vector3d.xyz(x, y, z)
                vertices.add(vertex)
            } else if (numbers[0] == "facet" && numbers[1] == "normal") {
                normal.x = parseDouble(numbers[2])
                normal.y = parseDouble(numbers[3])
                normal.z = parseDouble(numbers[4])
            }
        }
        input.close()
    }


    fun parseBinary(f: File) {
        val vertices = ArrayList<Vector3d>()
        try {
            val fis = FileInputStream(f)
            for (h in 0 until 84) {
                fis.read()// skip the header bytes
            }
            for (t in 0 until triangles) {
                val tri = ByteArray(50)
                for (tb in 0 until 50) {
                    tri[tb] = fis.read().toByte()
                }
                normal.x = leBytesToFloat(tri[0], tri[1], tri[2], tri[3]).toDouble()
                normal.y = leBytesToFloat(tri[4], tri[5], tri[6], tri[7]).toDouble()
                normal.z = leBytesToFloat(tri[8], tri[9], tri[10], tri[11]).toDouble()
                for (i in 0 until 3) {
                    val j = i * 12 + 12
                    val px = leBytesToFloat(
                        tri[j], tri[j + 1], tri[j + 2],
                        tri[j + 3]
                    ).toDouble()
                    val py = leBytesToFloat(
                        tri[j + 4], tri[j + 5],
                        tri[j + 6], tri[j + 7]
                    ).toDouble()
                    val pz = leBytesToFloat(
                        tri[j + 8], tri[j + 9],
                        tri[j + 10], tri[j + 11]
                    ).toDouble()
                    val p = Vector3d.xyz(px, py, pz)
                    vertices.add(p)
                }
            }
            fis.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun leBytesToFloat(b0: Byte, b1: Byte, b2: Byte, b3: Byte): Float {
        return intBitsToFloat(
            b3.and(0xff.toByte()).toInt().shl(24).or(
                b2.and(0xff.toByte()).toInt().shl(16)
            ).or(
                b1.and(0xff.toByte()).toInt().shl(8)
            ).or(
                b0.and(0xff.toByte()).toInt()
            )
        )
    }
}
