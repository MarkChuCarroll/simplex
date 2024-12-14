package org.goodmath.simplex.kcsg

import org.goodmath.simplex.kcsg.vvecmath.Vector3d
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A solid sphere.
 *
 * Tthe tessellation along the longitude and latitude directions can be
 * controlled via the {@link #numSlices} and {@link #numStacks} parameters.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
class Sphere(var center: Vector3d,
             var radius: Double, var numSlices: Int = 16, var numStacks: Int = 8): Primitive {

    override var properties = PropertyStorage()

    /**
     * Constructor. Creates a sphere with the specified radius, 16 slices and 8
     * stacks and center [0,0,0].
     *
     * @param radius sphere radius
     */
    constructor(radius: Double): this(Vector3d.xyz(0.0, 0.0, 0.0), radius)

    constructor(): this(Vector3d.xyz(0.0, 0.0, 0.0), 1.0)


    private fun sphereVertex(c: Vector3d, r: Double, inTheta: Double, inPhi: Double): Vertex {
        val theta = inTheta * PI * 2
        val phi = inPhi * PI
        val dir = Vector3d.xyz(
                cos(theta) * sin(phi),
            cos(phi),
            sin(theta) * sin(phi))
        return Vertex(c.plus(dir.times(r)), dir)
    }

    override fun toPolygons(): List<Polygon> {
        val polygons = ArrayList<Polygon>()
        for (i in 0 until numSlices) {
            for (j in 0 until numStacks) {
                val vertices = ArrayList<Vertex>()
                vertices.add(
                    sphereVertex(center, radius, i.toDouble() / numSlices.toDouble(),
                        j.toDouble() / numStacks.toDouble()))
                if (j > 0) {
                    vertices.add(
                        sphereVertex(center, radius, (i + 1).toDouble() / numSlices.toDouble(),
                            j.toDouble() / numStacks.toDouble()))
                }
                if (j < numStacks - 1) {
                    vertices.add(
                        sphereVertex(center, radius, (i + 1).toDouble() / numSlices.toDouble(),
                            (j + 1).toDouble() / numStacks.toDouble()))
                }
                vertices.add(
                    sphereVertex(center, radius, i.toDouble() / numSlices.toDouble(),
                        (j + 1).toDouble() / numStacks.toDouble()));
                polygons.add(Polygon(vertices, properties))
            }
        }
        return polygons
    }

}
