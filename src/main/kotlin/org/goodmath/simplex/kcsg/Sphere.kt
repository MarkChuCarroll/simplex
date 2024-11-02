package org.goodmath.simplex.kcsg

import org.goodmath.simplex.vvecmath.Vector3d
import kotlin.math.cos
import kotlin.math.sin

/**
 * A solid sphere.
 *
 * Tthe tessellation along the longitude and latitude directions can be
 * controlled via the {@link #numSlices} and {@link #numStacks} parameters.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 * Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
class Sphere(
    val center: Vector3d = Vector3d.xyz(0.0, 0.0, 0.0),
    val radius: Double = 1.0,
    val numSlices: Int = 16,
    val numStacks: Int = 8): PrimitiveShape {

    constructor(radius: Double, numSlices: Int = 16, numStacks: Int = 8) : this(
        center=Vector3d.xyz(0.0, 0.0, 0.0),
        radius = radius,
        numSlices = numSlices,
        numStacks = numStacks
    )

    override val properties = PropertyStorage()

    private fun sphereVertex(c: Vector3d, r: Double, theta: Double, phi: Double):  Vertex {
        val thetaRads = theta * Math.PI * 2
        val phiRads = phi * Math.PI;
        val dir = Vector3d.xyz(
                cos(thetaRads) * sin(phiRads),
            cos(phiRads),
            sin(thetaRads) * sin(phiRads))
        return Vertex(c.plus(dir.times(r)), dir)
    }

    override fun toPolygons(): List<Polygon> {
        val polygons = ArrayList<Polygon>()
        for (i in 0 until numSlices) {
            for (j in 0 until numStacks) {
                val vertices = ArrayList<Vertex>()
                vertices.add(
                    sphereVertex(
                        center, radius, i / numSlices.toDouble(),
                        j / numStacks.toDouble()
                    )
                )
                if (j > 0) {
                    vertices.add(
                        sphereVertex(
                            center, radius, (i + 1) / numSlices.toDouble(),
                            j / numStacks.toDouble()
                        )
                    )
                }
                if (j < numStacks - 1) {
                    vertices.add(
                        sphereVertex(
                            center, radius, (i + 1) / numSlices.toDouble(),
                            (j + 1) / numStacks.toDouble()
                        )
                    )
                }
                vertices.add(
                    sphereVertex(
                        center, radius, i / numSlices.toDouble(),
                        (j + 1) / numStacks.toDouble()
                    )
                )
                polygons.add(Polygon(vertices, properties))
            }
        }
        return polygons
    }
}
