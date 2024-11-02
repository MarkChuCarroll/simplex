package org.goodmath.simplex.kcsg

import javafx.scene.Group
import javafx.scene.paint.Color
import javafx.scene.paint.Material
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.Mesh
import javafx.scene.shape.MeshView
import org.goodmath.simplex.vvecmath.Vector3d

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 * Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
class MeshContainer(val min: Vector3d, val max: Vector3d, val meshes: List<Mesh>) {

    val materials = ArrayList<Material>()
    val bounds: Bounds = Bounds(min, max)
    val width = bounds.bounds.x
    val height = bounds.bounds.y
    val depth = bounds.bounds.z
    val root: Group = Group()

    init {
        val material = PhongMaterial(Color.RED)
        repeat(meshes.size) { _ ->
            materials.add(material)
        }
    }

    override fun toString(): String {
        return bounds.toString()
    }

    fun getAsMeshViews(): List<MeshView> {
        val  result = ArrayList<MeshView>(meshes.size)

        for (i in meshes.indices) {
            val mesh = meshes[i]
            val mat = materials[i]

            val view = MeshView(mesh)
            view.material = mat
            view.cullFace = CullFace.NONE
            result.add(view)
        }
        return result
    }
}
