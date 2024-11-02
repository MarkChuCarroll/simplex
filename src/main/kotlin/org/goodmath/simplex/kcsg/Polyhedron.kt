package org.goodmath.simplex.kcsg

import org.goodmath.simplex.vvecmath.Vector3d


/**
 * Polyhedron.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 * Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
class Polyhedron(
    val points: List<Vector3d>,
    val faces: List<List<Int>>): PrimitiveShape {
    override val properties = PropertyStorage()

    override fun toPolygons(): List<Polygon> {
        return faces.map {
            Polygon.fromPoints(it.map { i -> points[i].clone() })
        }
    }
}
