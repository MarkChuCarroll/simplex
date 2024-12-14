package org.goodmath.simplex.kcsg


import javafx.scene.Group
import javafx.scene.SubScene
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.paint.Material
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.Mesh
import javafx.scene.shape.MeshView
import org.goodmath.simplex.kcsg.vvecmath.Vector3d

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
class MeshContainer(min: Vector3d, max: Vector3d, val meshes: ArrayList<Mesh>,
                    val materials: ArrayList<Material> = ArrayList<Material>()) {

    val bounds: Bounds = Bounds(min, max)
    val width: Double = bounds.bounds.x
    val height: Double = bounds.bounds.y
    val depth: Double = bounds.bounds.z

    val root = Group()
    val viewContainer: Pane? = null
    val subScene: SubScene? = null
    init {
        if (materials.isEmpty()) {
            val material = PhongMaterial(Color.RED)
            for (mesh in meshes) {
                materials.add(material)
            }
        } else if(materials.size != meshes.size) {
            throw IllegalArgumentException("Mesh list and Material list must not differ in size!")
        }
    }


    override fun toString(): String {
        return bounds.toString()
    }

    fun  getAsMeshViews(): List<MeshView> {
        val result = ArrayList<MeshView>(meshes.size)

        for (i in 0 until meshes.size) {
            val mesh = meshes[i]
            val mat = materials[i]

            val view = MeshView(mesh)
            view.setMaterial(mat)
            view.setCullFace(CullFace.NONE)

            result.add(view)
        }

        return result
    }

}
