package org.goodmath.simplex.kcsg

import org.goodmath.simplex.vvecmath.Vector3d

/**
 * Represents a plane in 3D space.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 * Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
class Plane(normalish: Vector3d, var dist: Double): Cloneable {
    var normal = normalish.normalized()

    public override fun clone(): Plane {
        return Plane(normal.clone(), dist)
    }

    /**
     * Flips this plane.
     */
    fun flip(): Plane {
        return Plane(normal.negated(), -dist)
    }

    /**
     * Splits a {@link Polygon} by this plane if needed. After that it puts the
     * polygons or the polygon fragments in the appropriate lists
     * ({@code front}, {@code back}). Coplanar polygons go into either
     * {@code coplanarFront}, {@code coplanarBack} depending on their
     * orientation with respect to this plane. Polygons in front or back of this
     * plane go into either {@code front} or {@code back}.
     *
     * @param polygon polygon to split
     * @param coplanarFront "coplanar front" polygons
     * @param coplanarBack "coplanar back" polygons
     * @param front front polygons
     * @param back back polygons
     */
    fun splitPolygon(
        polygon: Polygon,
        coplanarFront: MutableList<Polygon>,
        coplanarBack: MutableList<Polygon>,
        front: MutableList<Polygon>,
        back: MutableList<Polygon>) {

        // Classify each point as well as the entire polygon into one of the
        // above four classes.
        var polygonType = 0
        val types: ArrayList<Int> = ArrayList(polygon.vertices.size)
        for (i in 0 until polygon.vertices.size) {
            val t = normal.dot(polygon.vertices[i].pos) - dist
            val type = if (t < -EPSILON) {
                BACK
            } else if (t > EPSILON) {
                FRONT
            } else {
                COPLANAR
            }
            polygonType = polygonType.or(type)
            types.add(type)
        }

        // Put the polygon in the correct list, splitting it when necessary.
        when (polygonType) {
            COPLANAR ->
                if (normal.dot(polygon.csgPlane.normal) > 0) {
                    coplanarFront
                } else {
                    coplanarBack
                }.add(polygon)

            FRONT ->
                front.add(polygon)

            BACK ->
                back.add(polygon)

            SPANNING -> {
                val f = ArrayList<Vertex>()
                val b = ArrayList<Vertex>()
                for (i in polygon.vertices.indices) {
                    val j = (i + 1) % polygon.vertices.size
                    val ti = types[i]
                    val tj = types[j]
                    val vi = polygon.vertices[i]
                    val vj = polygon.vertices[j]
                    if (ti != BACK) {
                        f.add(vi)
                    }
                    if (ti != FRONT) {
                        b.add(
                            if (ti != BACK) {
                                vi.copy()
                            } else {
                                vi
                            }
                        )
                        if (ti.or(tj) == SPANNING) {
                            val t = dist - normal.dot(vi.pos) / normal.dot(vj.pos.minus(vi.pos))
                            val v = vi.interpolate(vj, t)
                            f.add(v)
                            b.add(v.copy())
                        }
                    }
                    if (f.size >= 3) {
                        front.add(Polygon(f, polygon.storage))
                    }
                    if (b.size >= 3) {
                        back.add(Polygon(b, polygon.storage))
                    }
                }
            }
        }
    }

    companion object {
        const val COPLANAR = 0
        const val FRONT = 1
        const val BACK = 2
        const val SPANNING = 3 // == some in the FRONT + some in the BACK


        /**
         * EPSILON is the tolerance used by {@link #splitPolygon(Polygon, java.util.List, java.util.List, java.util.List, java.util.List)
         * } to decide if a point is on the plane.
         */
        const val EPSILON: Double = 1e-8

        /**
         * XY plane.
         */
        val XY_PLANE = Plane(Vector3d.Z_ONE, 1.0)

        /**
         * XZ plane.
         */
        val XZ_PLANE = Plane(Vector3d.Y_ONE, 1.0)

        /**
         * YZ plane.
         */
        val YZ_PLANE = Plane(Vector3d.X_ONE, 1.0)


        /**
         * Creates a plane defined by the the specified points.
         *
         * @param a first point
         * @param b second point
         * @param c third point
         * @return a plane
         */
        fun createFromPoints(a: Vector3d, b: Vector3d, c: Vector3d): Plane {
            val n = b.minus(a).crossed(c.minus(a)).normalized()
            return Plane(n, n.dot(a))
        }
    }
}
