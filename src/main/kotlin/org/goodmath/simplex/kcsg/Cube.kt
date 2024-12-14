package org.goodmath.simplex.kcsg

import org.goodmath.simplex.kcsg.vvecmath.Transform
import org.goodmath.simplex.kcsg.vvecmath.Vector3d
import kotlin.math.min
import kotlin.text.get

/**
 * An axis-aligned solid cuboid defined by {@code center} and
 * {@code dimensions}.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
class Cube(val center: Vector3d = Vector3d.ZERO, val dimensions: Vector3d = Vector3d.xyz(1.0, 1.0, 1.0)): Primitive {
    constructor(size: Double): this(dimensions = Vector3d.xyz(size, size, size), center = Vector3d.ZERO)
    constructor(x: Double, y: Double, z: Double): this(dimensions = Vector3d.xyz(x, y, z), center = Vector3d.ZERO)

    var centered = true

    override var properties = PropertyStorage()



    override fun toPolygons(): List<Polygon> {
        val a = listOf(
            //               position          normal
            listOf(listOf(0, 4, 6, 2), listOf(-1, 0, 0)),
            listOf(listOf(1, 3, 7, 5), listOf(+1, 0, 0)),
            listOf(listOf(0, 1, 5, 4), listOf(0, -1, 0)),
            listOf(listOf(2, 6, 7, 3), listOf(0, +1, 0)),
            listOf(listOf(0, 2, 3, 1), listOf(0, 0, -1)),
            listOf(listOf(4, 5, 7, 6), listOf(0, 0, +1)))
        val polygons = ArrayList<Polygon>();
        for (info in a) {
            val vertices = ArrayList<Vertex>()
            for (i in info[0]) {
                val pos = Vector3d.xyz(
                    center.x + dimensions.x * (1 * min(1, i and 1) - 0.5),
                    center.y + dimensions.y * (1 * min(1, i and 2) - 0.5),
                    center.z + dimensions.z * (1 * min(1, i and 4) - 0.5))
                vertices.add(Vertex(pos, Vector3d.Companion.xyz(
                    info[1][0].toDouble(),
                    info[1][1].toDouble(),
                    info[1][2].toDouble())))
            }
            polygons.add(Polygon(vertices, properties))
        }
        if (!centered) {
            val centerTransform = Transform.unity().translate(
                dimensions.x / 2.0,
                dimensions.y / 2.0,
                dimensions.z / 2.0)

            for (p in polygons) {
                p.transform(centerTransform)
            }
        }

        return polygons
    }

    /**
     * Defines that this cube will not be centered.
     *
     * @return this cube
     */
    fun noCenter(): Cube {
        centered = false
        return this
    }

}
