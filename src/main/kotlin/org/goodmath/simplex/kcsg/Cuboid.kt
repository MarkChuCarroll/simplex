package org.goodmath.simplex.kcsg

import org.goodmath.simplex.vvecmath.Vector3d
import kotlin.math.min


/**
 * An axis-aligned solid cuboid defined by {@code center} and
 * {@code dimensions}.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 * Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
class Cuboid(val x: Double = 1.0,
             val y: Double = x,
             val z: Double = x,
             var center: Vector3d = Vector3d.xyz(0.0, 0.0, 0.0))
    : PrimitiveShape {
    constructor(center: Vector3d, bounds: Vector3d): this(bounds.x, bounds.y, bounds.z, center)

    val dimensions: Vector3d
        get() = Vector3d.xyz(x, y, z)

    override val properties = PropertyStorage()

    override fun toPolygons(): ArrayList<Polygon>  {
        val a = arrayOf(
            //             position              normal
            arrayOf(arrayOf(0, 4, 6, 2), arrayOf(-1, 0, 0)),
            arrayOf(arrayOf(1, 3, 7, 5), arrayOf(+1, 0, 0)),
            arrayOf(arrayOf(0, 1, 5, 4), arrayOf(0, -1, 0)),
            arrayOf(arrayOf(2, 6, 7, 3), arrayOf(0, +1, 0)),
            arrayOf(arrayOf(0, 2, 3, 1), arrayOf(0, 0, -1)),
            arrayOf(arrayOf(4, 5, 7, 6), arrayOf(0, 0, +1)))

        val polygons = ArrayList<Polygon>()
        for (info in a) {
            val vertices = ArrayList<Vertex>()
            for (i in info[0]) {
                val pos = Vector3d.xyz(
                    center.x + dimensions.x * ((1 * min(1, i.and(1)).toDouble() - 0.5)),
                    center.y + dimensions.y * (1 * (min(1, i.and(2)).toDouble() - 0.5)),
                    center.z + dimensions.z * (1 * (min(1, i.and(4)).toDouble() - 0.5)))
                vertices.add(Vertex(pos, Vector3d.xyz(
                    info[1][0].toDouble(),
                    info[1][1].toDouble(),
                    info[1][2].toDouble())))

            }
            polygons.add(Polygon(vertices, properties))
        }
        return polygons
    }
}
