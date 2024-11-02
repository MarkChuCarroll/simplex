package org.goodmath.simplex.vvecmath

import java.io.Closeable

/*
 * Copyright 2017-2019 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * If you use this software for scientific research then please cite the following publication(s):
 *
 * M. Hoffer, C. Poliwoda, & G. Wittum. (2013). Visual reflection library:
 * a framework for declarative GUI programming on the Java platform.
 * Computing and Visualization in Science, 2013, 16(4),
 * 181–192. http://doi.org/10.1007/s00791-014-0230-y
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Michael Hoffer <info@michaelhoffer.de>.
 */

/**
 * Represents a plane in 3D space.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
public class Plane(val anchor: Vector3d, val normal: Vector3d): Cloneable {
    public override fun clone(): Plane {
        return Plane(anchor, normal)
    }

    /**
     * Returns a flipped copy of this plane.
     * @return flipped coppy of this plane
     */
    fun flipped(): Plane {
        return  Plane(anchor, - normal)
    }

    /**
     * Return the distance of this plane to the origin.
     *
     * @return distance of this plane to the origin
     */
    val dist: Double = anchor.magnitude()


    /**
     * Projects the specified point onto this plane.
     *
     * @param p point to project
     * @return projection of p onto this plane
     */
    public fun project(p: Vector3d): Vector3d {

        // dist:   the distance of this plane to the origin
        // anchor: is the anchor point of the plane (closest point to origin)
        // n:      the plane normal
        //
        // a) project (p-anchor) onto n
        val projV = normal.project(p.minus(anchor))

        // b) subtract projection from p to get projP
        val projP = p.minus(projV)

        return projP
    }

    /**
     * Returns the shortest distance between the specified point and this plane.
     *
     * @param p point
     * @return the shortest distance between the specified point and this plane
     */
    fun distance(p: Vector3d): Double {
        return p.minus(project(p)).magnitude()
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
    public fun compare(p: Vector3d, tol: Double): Int {
        // angle between vector n and vector (p-anchor)
        val t = this.normal.dot(p.minus(anchor))
        return if (t < -tol) {
            -1
        } else if (t > tol) {
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
        val TOL = 1e-12

        /**
         * XY plane.
         */
        val XY_PLANE =  Plane(Vector3d.ZERO, Vector3d.Z_ONE)
        /**
         * XZ plane.
         */
        val XZ_PLANE =  Plane(Vector3d.ZERO, Vector3d.Y_ONE)
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
            val normal = b.minus(a).crossed(c.minus(a)).normalized()

            var anchor = Vector3d.zero()

            anchor = anchor.plus(a)
            anchor = anchor.plus(b)
            anchor = anchor.plus(c)

            anchor = anchor.times(1.0 / 3.0)

            return Plane(anchor, normal)
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
