package org.goodmath.simplex.kcsg

import org.goodmath.simplex.kcsg.vvecmath.Vector3d

/**
 * Polyhedron.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
class Polyhedron(val points: ArrayList<Vector3d>,
    val faces: ArrayList<ArrayList<Int>>): Primitive {

    override val properties = PropertyStorage()

    override fun toPolygons(): List<Polygon> {

        val indexToPoint = { i:Int ->
            points[i].clone()
        }

        val faceListToPolygon = { faceList: List<Int> ->
            Polygon.fromPoints(faceList.map(indexToPoint), properties)
        }

        return faces.map(faceListToPolygon)
    }

}
