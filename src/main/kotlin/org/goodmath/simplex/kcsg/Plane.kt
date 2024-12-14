package org.goodmath.simplex.kcsg

import org.goodmath.simplex.kcsg.vvecmath.Vector3d

enum class PointClassification {
    COPLANAR,
    FRONT,
    BACK,
    SPANNING; // == some in the FRONT + some in the BACK

    fun leastUpperBound(other: PointClassification): PointClassification {
        return when(this) {
            COPLANAR -> other
            FRONT -> if (other == BACK || other == SPANNING) {
                SPANNING
            } else {
                this
            }
            BACK -> if (other == FRONT || other == SPANNING) {
                SPANNING
            } else {
                this
            }
            SPANNING -> SPANNING
        }
    }
}


/**
 * Represents a plane in 3D space.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
class Plane(n: Vector3d, var dist: Double): Cloneable {

    var normal = n.normalized()

    public override fun clone(): Plane {
        return Plane(normal.clone(), dist)
    }

    /**
     * Flips this plane.
     */
    fun flip() {
        normal = normal.negated()
        dist = -dist
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
     * @param back back polgons
     */
    fun splitPolygon(
        polygon: Polygon,
        coplanarFront: ArrayList<Polygon>,
        coplanarBack: ArrayList<Polygon>,
        front: ArrayList<Polygon>,
        back: ArrayList<Polygon>) {

        // Classify each point as well as the entire polygon into one of the
        // above four classes.
        var polygonType = PointClassification.COPLANAR
        var types = ArrayList<PointClassification>(polygon.vertices.size)
        for (i in 0 until polygon.vertices.size) {
            val t = normal.dot(polygon.vertices[i].pos) - dist
            val type = if  (t < -Plane.EPSILON) {
                PointClassification.BACK
            } else if (t > Plane.EPSILON) {
                PointClassification.FRONT
            } else  {
                PointClassification.COPLANAR
            }

            polygonType = polygonType.leastUpperBound(type)
            types.add(type)
        }

        //System.out.println("> switching");
        // Put the polygon in the correct list, splitting it when necessary.
        when (polygonType) {
            PointClassification.COPLANAR -> {
                val pset = if (normal.dot(polygon.storedCsgPlane.normal) > 0) {
                    coplanarFront
                } else {
                    coplanarBack
                }
                pset.add(polygon)
            }

            PointClassification.FRONT ->
                front.add(polygon)

            PointClassification.BACK ->
                back.add(polygon)

            PointClassification.SPANNING -> {
                val f = ArrayList<Vertex>()
                val b = ArrayList<Vertex>();
                for (i in 0 until polygon.vertices.size) {
                    val j = (i + 1) % polygon.vertices.size
                    val ti = types[i]
                    val tj = types[j]
                    val vi = polygon.vertices[i]
                    val vj = polygon.vertices[j]
                    if (ti != PointClassification.BACK) {
                        f.add(vi)
                    }
                    if (ti != PointClassification.FRONT) {
                        b.add(
                            if (ti != PointClassification.BACK) {
                                vi.clone()
                            } else {
                                vi
                            }
                        )
                    }
                    if ((ti.leastUpperBound(tj)) == PointClassification.SPANNING) {
                        val t = (this.dist - this.normal.dot(vi.pos)) / this.normal.dot(vj.pos.minus(vi.pos))
                        val v = vi.interpolate(vj, t)
                        f.add(v)
                        b.add(v.clone())
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

    companion object {
        /**
         * EPSILON is the tolerance used by {@link #splitPolygon(Polygon, java.util.List, java.util.List, java.util.List, java.util.List)
         * } to decide if a point is on the plane.
         */
        const val EPSILON = 1e-8

        /**
         * XY plane.
         */
        val XY_PLANE =  Plane(Vector3d.Z_ONE, 1.0)

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

