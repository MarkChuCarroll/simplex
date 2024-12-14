package org.goodmath.simplex.kcsg

import org.goodmath.simplex.kcsg.vvecmath.Vector3d
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

/**
 * A solid cylinder.
 *
 * The tessellation can be controlled via the {@link #numSlices} parameter.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
class Cylinder(var start: Vector3d = Vector3d.xyz(0.0, -0.5, 0.0),
               var end: Vector3d = Vector3d.xyz(0.0, 0.5, 0.0),
               var startRadius: Double = 1.0,
               var endRadius: Double = startRadius,
               var numSlices: Int = 16): Primitive {
    constructor(): this(Vector3d.xyz(0.0, -0.5, 0.0),
        Vector3d.xyz(0.0, 0.5, 0.0),
        1.0,
        1.0,
        16)

    constructor(radius: Double, height: Double, numSlices: Int = 16):
            this(Vector3d.ZERO, end = Vector3d.Z_ONE.times(height),
                radius, radius, numSlices)




    /**
     * Constructor. Creates a cylinder ranging from {@code [0,0,0]} to
     * {@code [0,0,height]} with the specified {@code radius} and
     * {@code height}. The resolution of the tessellation can be controlled with
     * {@code numSlices}.
     *
     * @param startRadius cylinder start radius
     * @param endRadius cylinder end radius
     * @param height cylinder height
     * @param numSlices number of slices (used for tessellation)
     */
    constructor(startRadius: Double, endRadius: Double, height: Double,
                numSlices: Int = 16): this(
        Vector3d.ZERO,
        Vector3d.Z_ONE.times(height),
        startRadius,
        endRadius,
        numSlices)

    override val properties = PropertyStorage()

    override fun toPolygons(): List<Polygon> {
        val s = start
        val e = end
        val ray = e.minus(s)
        val axisZ = ray.normalized()
        val isY = (axisZ.y.absoluteValue > 0.5)
        val axisX = Vector3d.xyz(if (isY) { 1.0 } else { 0.0 },
            if (!isY) { 1.0 } else { 0.0 },
            0.0).crossed(axisZ).normalized()

        val axisY = axisX.crossed(axisZ).normalized()
        val startV = Vertex(s, axisZ.negated())
        val endV = Vertex(e, axisZ.normalized())
        val polygons = ArrayList<Polygon>()

        for (i in 0 until numSlices) {
            val t0 = i.toDouble() / numSlices.toDouble()
            val t1 = (i + 1).toDouble() / numSlices.toDouble()
            polygons.add(Polygon(arrayListOf(
                startV,
                cylPoint(axisX, axisY, axisZ, ray, s, startRadius, 0.0, t0, -1.0),
                cylPoint(axisX, axisY, axisZ, ray, s, startRadius, 0.0, t1, -1.0)),
                properties))
            polygons.add(Polygon(arrayListOf(
                cylPoint(axisX, axisY, axisZ, ray, s, startRadius, 0.0, t1, 0.0),
                cylPoint(axisX, axisY, axisZ, ray, s, startRadius, 0.0, t0, 0.0),
                cylPoint(axisX, axisY, axisZ, ray, s, endRadius, 1.0, t0, 0.0),
                cylPoint(axisX, axisY, axisZ, ray, s, endRadius, 1.0, t1, 0.0)),
                properties))
            polygons.add(Polygon(
                arrayListOf(
                    endV,
                    cylPoint(axisX, axisY, axisZ, ray, s, endRadius, 1.0, t1, 1.0),
                    cylPoint(axisX, axisY, axisZ, ray, s, endRadius, 1.0, t0, 1.0)),
                properties))
        }

        return polygons
    }

    private fun cylPoint(
        axisX: Vector3d, axisY: Vector3d, axisZ: Vector3d,
        ray: Vector3d, s: Vector3d,
        r: Double, stack: Double, slice: Double, normalBlend: Double): Vertex {
        val angle = slice * PI * 2
        val out = axisX.times(cos(angle)).plus(axisY.times(sin(angle)))
        val pos = s.plus(ray.times(stack)).plus(out.times(r))
        val normal = out.times(1.0 - normalBlend.absoluteValue).plus(axisZ.times(normalBlend))
        return Vertex(pos, normal)
    }

}
