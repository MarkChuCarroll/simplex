package org.goodmath.simplex.kcsg

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.writeText

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 * Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
 class ObjFile(var obj: String, var mtl: String) {
    companion object {
        val MTL_NAME = "\$JCSG_MTL_NAME\$"
    }

    fun toFiles(p: Path) {
        val parent = p.parent
        var fileName = p.fileName.toString()
        if (fileName.lowercase().endsWith(".obj")
            || fileName.lowercase().endsWith(".mtl")) {
            fileName = fileName.substring(0, fileName.length - 4)
        }
        var objName = fileName + ".obj"
        var mtlName = fileName + ".mtl"

        obj = obj.replace(MTL_NAME, mtlName)
        if (parent == null) {
            Path("$objName.obj").writeText(obj)
            Path(mtlName).writeText(mtl)
        } else {
            (parent / objName).writeText(obj)
            (parent / mtlName).writeText(mtl)
        }
    }

    val objStream: InputStream by lazy {
        ByteArrayInputStream(obj.toByteArray(StandardCharsets.UTF_8))
    }

    val mtlStream: InputStream by lazy {
        ByteArrayInputStream(mtl.toByteArray(StandardCharsets.UTF_8))
    }
}
