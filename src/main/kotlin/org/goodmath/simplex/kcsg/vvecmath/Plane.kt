package org.goodmath.simplex.kcsg.vvecmath



/**
 * Represents a plane in 3D space.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
class Plane(val anchor: Vector3d, val normal: Vector3d) {



    fun copy(): Plane {
        return Plane (anchor, normal)
    }

    /**
     * Returns a flipped copy of this plane.
     * @return flipped copy of this plane
     */
    fun flipped(): Plane {
        return Plane (anchor, normal.negated())
    }

    /**
     * Return the distance of this plane to the origin.
     *
     * @return distance of this plane to the origin
     */
    fun getDist(): Double {
        return anchor.magnitude
    }

    /**
     * Projects the specified point onto this plane.
     *
     * @param p point to project
     * @return projection of p onto this plane
     */
    fun project(p: Vector3d): Vector3d {
        // dist:   the distance of this plane to the origin
        // anchor: is the anchor point of the plane (closest point to origin)
        // n:      the plane normal
        //
        // a) project (p-anchor) onto n
        val projV = normal.project (p.minus(anchor))

        // b) subtract projection from p to get projP
        val projP = p.minus (projV)

        return projP
    }

    /**
     * Returns the shortest distance between the specified point and this plane.
     *
     * @param p point
     * @return the shortest distance between the specified point and this plane
     */
    fun distance(p: Vector3d): Double {
        return p.minus(project(p)).magnitude
    }

    /**
     * Determines whether the specified point is in front of, in back of or on
     * this plane.
     *
     * @param p point to check
     * @param TOL tolerance
     * @return {@code 1}, if p is in front of the plane, {@code -1}, if the
     * point is in the back of this plane and {@code 0} if the point is on this
     * plane
     */
    fun compare(p: Vector3d, tol: Double): Int {
        // angle between vector n and vector (p-anchor)
        val t = this.normal.dot(p.minus(anchor))
        return if (t < -tol) {
            -1
        } else if (t > TOL) {
            1
        } else {
            0
        }
    }

    /**
     * Determines whether the specified point is in front of, in back of or on
     * this plane.
     *
     * @param p point to check
     *
     * @return {@code 1}, if p is in front of the plane, {@code -1}, if the
     * point is in the back of this plane and {@code 0} if the point is on this
     * plane
     */
    fun compare(p: Vector3d): Int {

        // angle between vector n and vector (p-anchor)
        val t = normal.dot(p.minus(anchor))
        return if (t < -TOL) {
            -1
        } else if (t > TOL) {
            1
        } else {
            0
        }
    }

    companion object {

        const val TOL: Double = 1e-12

        /**
         * XY plane.
         */
        val XY_PLANE = Plane(Vector3d.ZERO, Vector3d.Z_ONE)

        /**
         * XZ plane.
         */
        val XZ_PLANE = Plane(Vector3d.ZERO, Vector3d.Y_ONE)
        /**
         * YZ plane.
         */
        val YZ_PLANE = Plane(Vector3d.ZERO, Vector3d.X_ONE)


        /**
         * Creates a plane defined by the the specified points. The anchor point of
         * the plane is the centroid of the triangle (a,b,c).
         *
         * @param a first point
         * @param b second point
         * @param c third point
         * @return a plane
         */
        fun fromPoints(a: Vector3d, b: Vector3d, c: Vector3d): Plane {
            val n = b.minus(a).crossed(c.minus(a)).normalized()
            var center: Vector3d = Vector3d.ZERO

            center = center.plus(a)
            center = center.plus(b)
            center = center.plus(c)

            center = center.times(1.0 / 3.0)

            return Plane(n, center)
        }

        /**
         * Creates a plane defined by an anchor point and a normal vector.
         *
         * @param p anchor point
         * @param n plane normal
         * @return a plane
         */
        fun fromPointAndNormal(p: Vector3d, n: Vector3d): Plane {
            return Plane(p, n)
        }

    }

}
