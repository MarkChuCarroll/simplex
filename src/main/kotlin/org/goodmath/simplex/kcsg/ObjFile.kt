package org.goodmath.simplex.kcsg

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths


/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
class ObjFile(var obj: String, var mtl: String) {

    var storedObjStream: InputStream? = null
    var storedMtlStream: InputStream? = null

    fun toFiles(p: Path) {

        val parent = p.parent

        var fileName = p.fileName.toString()

        if (fileName.lowercase().endsWith(".obj")
            || fileName.lowercase().endsWith(".mtl")) {
            fileName = fileName.substring(0, fileName.length - 4)
        }

        val objName = "$fileName.obj"
        val mtlName = "$fileName.mtl"

        obj = obj.replace(MTL_NAME, mtlName)
        storedObjStream = null

        if (parent == null) {
            FileUtil.write(Paths.get(objName), obj)
            FileUtil.write(Paths.get(mtlName), mtl)
        } else {
            FileUtil.write(Paths.get(parent.toString(), objName), obj)
            FileUtil.write(Paths.get(parent.toString(), mtlName), mtl)
        }

    }

    val objStream: InputStream
        get() {
            if (storedObjStream == null) {
                storedObjStream = obj.byteInputStream(StandardCharsets.UTF_8)
            }
            return storedObjStream!!
        }


    val mtlStream: InputStream
        get() {
            if (storedMtlStream == null) {
                storedMtlStream = mtl.byteInputStream(StandardCharsets.UTF_8)
            }
            return storedMtlStream!!
        }

    companion object {
        const val MTL_NAME = "${'$'}JCSG_MTL_NAME${'$'}"
    }
}
