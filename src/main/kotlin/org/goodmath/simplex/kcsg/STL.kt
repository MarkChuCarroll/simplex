package org.goodmath.simplex.kcsg

import org.goodmath.simplex.kcsg.vvecmath.Vector3d
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.nio.file.Path
import java.text.ParseException


class STLLoader {

//        /**
//         * Load the specified stl file and returns the result as a hash map, mapping
//         * the object names to the corresponding <code>CustomMesh</code> objects.
//         */
//        public static Map<String, CustomMesh> load(String stlfile)
//                        throws IOException {
//                STLLoader sl = new STLLoader();
//                try {
//                        sl.parse(stlfile);
//                } catch (RuntimeException e) {
//                        System.out.println("error reading " + sl.name);
//                        throw e;
//                }
//                return sl.meshes;
//        }
//
//        private HashMap<String, CustomMesh> meshes;

    var line: String = ""

    // attributes of the currently read mesh
    var vertices = ArrayList<Vector3d>()
    val normal = Vector3d.zero().asModifiable() //to be used for file checking
    var triangles: Int = 0



    fun parse(f: File): ArrayList<Vector3d> {
        vertices.clear()
        // determine if this is a binary or ASCII STL
        // and send to the appropriate parsing method
        // Hypothesis 1: this is an ASCII STL
        var br = BufferedReader(FileReader(f))
        val line = br.readLine()
        val words = line.trim().split("\\s+")
        if (line.indexOf(0.toChar()) < 0 && words[0].lowercase() == "solid") {
            println("Looks like an ASCII STL")
            parseAscii(f)
            return vertices
        }
        br.close()

        // Hypothesis 2: this is a binary STL
        val fs = FileInputStream(f)

        // bytes 80, 81, 82 and 83 form a little-endian int
        // that contains the number of triangles
        val buffer = ByteArray(84)
        fs.read(buffer, 0, 84)
        triangles = ((buffer[83].toInt() and 0xff) shl 24) or
                ((buffer[82].toInt() and 0xff) shl 16) or
                ((buffer[81].toInt() and 0xff) shl 8) or
                (buffer[80].toInt() and 0xff)
        if (((f.length() - 84) / 50).toInt() == triangles) {
            println("Looks like a binary STL")
            parseBinary(f)
            return vertices
        }
        System.err.println("File is not a valid STL")
        return vertices
    }

    fun parseAscii(f: File) {
        val input = try {
            BufferedReader( FileReader(f))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            throw e
        }
        vertices = ArrayList<Vector3d>()
        try {
            var lines = input.readLines()
            input.close()
            for (line in lines) {
                val numbers = line.trim().split("\\s+")
                if (numbers[0] == "vertex") {
                    val x = numbers[1].toDouble()
                    val y = numbers[2].toDouble()
                    val z = numbers[3].toDouble()
                    val vertex = Vector3d.xyz(x, y, z)
                    vertices.add(vertex)
                } else if (numbers[0] == "facet" && numbers[1] == "normal") {
                    normal.x = numbers[2].toDouble()
                    normal.y = numbers[3].toDouble()
                    normal.z = numbers[4].toDouble()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    fun parseBinary(f: File) {
        vertices = ArrayList<Vector3d>()
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
                    val px = leBytesToFloat(tri[j], tri[j + 1], tri[j + 2],
                        tri[j + 3]).toDouble()
                    val py = leBytesToFloat(tri[j + 4], tri[j + 5],
                        tri[j + 6], tri[j + 7]).toDouble()
                    val pz = leBytesToFloat(tri[j + 8], tri[j + 9],
                        tri[j + 10], tri[j + 11]).toDouble()
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

//    private float parseFloat(String string) throws ParseException {
//        //E+05 -> E05, e+05 -> E05
//        string = string.replaceFirst("[eE]\\+", "E");
//        //E-05 -> E-05, e-05 -> E-05
//        string = string.replaceFirst("e\\-", "E-");
//        return decimalFormat.parse(string).floatValue();
//    }

    fun leBytesToFloat(b0: Byte, b1: Byte, b2: Byte, b3: Byte):  Float {
        return Float.fromBits((((b3.toInt() and 0xff) shl 24) or
                ((b2.toInt() and 0xff) shl 16) or
                ((b1.toInt() and 0xff) shl 8) or
                (b0.toInt() and 0xff)))
    }

}


/**
 * Loads a CSG from stl.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
object STL {
    /**
     * Loads a CSG from stl.
     * @param path file path
     * @return CSG
     * @throws IOException if loading failed
     */
    fun file(path: Path): CSG  {
        val loader = STLLoader()
        val polygons =  ArrayList<Polygon>()
        var vertices =  ArrayList<Vector3d>()
        for (p in loader.parse(path.toFile())) {
            vertices.add(p.clone())
            if (vertices.size == 3) {
                polygons.add(Polygon.fromPoints(vertices))
                vertices = ArrayList()
            }
        }

        return CSG.fromPolygons(PropertyStorage(), polygons)
    }
}



