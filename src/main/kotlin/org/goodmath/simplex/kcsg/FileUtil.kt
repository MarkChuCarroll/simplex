package org.goodmath.simplex.kcsg

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * File util class.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
object FileUtil {

    /**
     * Writes the specified string to a file.
     *
     * @param p file destination (existing files will be overwritten)
     * @param s string to save
     *
     * @throws IOException if writing to file fails
     */
    fun write(p: Path, s: String) {
        val writer = Files.newBufferedWriter(
            p, Charset.forName("UTF-8"),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
        )
        writer.write(s, 0, s.length)
    }

    /**
     * Reads the specified file to a string.
     *
     * @param p file to read
     * @return the content of the file
     *
     * @throws IOException if reading from file failed
     */
    fun read(p: Path): String {
        return String(Files.readAllBytes(p), Charset.forName("UTF-8"))
    }


    /**
     * Saves the specified csg using STL ASCII format.
     *
     * @param path destination path
     * @param csg csg to save
     * @throws java.io.IOException
     */
    fun toStlFile(path: Path, csg: CSG) {
        val out = Files.newBufferedWriter(
            path, Charset.forName("UTF-8"),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
        )
        out.append("solid v3d.csg\n")
        csg.polygons.forEach { p ->
            try {
                out.append(p.toStlString())
            } catch (ex: IOException) {
                logger.error(ex.toString())
                throw RuntimeException(ex);
            }
        }
        out.append("endsolid v3d.csg\n")
    }

   val logger: Logger = LoggerFactory.getLogger(FileUtil::class.java)
}
