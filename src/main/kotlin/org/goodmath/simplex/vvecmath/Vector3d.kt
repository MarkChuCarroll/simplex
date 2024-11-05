package org.goodmath.simplex.vvecmath

import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
interface Vector3d {

    /**
     * Returns a clone of this vector.
     *
     * @return a clone of this vector
     */
    fun clone(): Vector3d


    //////////////////////////////////
    // Cloning and mutabilitization
    //////////////////////////////////

    val x: Double
    val y: Double
    val z: Double

    /**
     * Clones the specified vector.
     *
     * @param source vector toclone
     * @return cloned vector
     */
    fun clone(source: Vector3d): Vector3d {
        return Vector3dImpl(source.x, source.y, source.z)
    }

    /**
     * Returns a modifiable copy of this vector.
     *
     * @return a modifiable copy of this vector
     */
    fun asModifiable(): ModifiableVector3d =
          ModifiableVector3dImpl(x, y, z)



    /**
     * Returns the components {code x,y,z} as double array.
     *
     * @return the components {code x,y,z} as double array
     */
    fun get(): Array<Double> {
        return arrayOf(x, y, z)
    }

    /**
     * Returns the i-th component of this vector.
     *
     * @param i component index
     * @return the i-th component of this vector
     */
    operator fun get(i: Int): Double {
        return when(i) {
            0 -> x
            1 -> y
            2 -> z
            else ->
            throw RuntimeException("Illegal index: $i")
        }
    }

    //////////////////////////////////
    // Vectorspace addition
    //////////////////////////////////

    /**
     * Returns the sum of this vector and the specified vector.
     *
     * @param v the vector to add
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the sum of this vector and the specified vector
     */
    operator fun plus(v: Vector3d): Vector3d {
        return plus(v.x, v.y, v.z)
    }

    fun added(v: Vector3d): Vector3d {
        return clone() + v
    }

    /**
     * Returns the sum of this vector and the specified vector.
     *
     * @param x x coordinate of the vector to add
     * @param y y coordinate of the vector to add
     * @param z z coordinate of the vector to add
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the sum of this vector and the specified vector
     */
    fun plus(ox: Double, oy: Double, oz: Double): Vector3d {
        return Vector3d.xyz(x + ox, y + oy, z + oz)
    }

    /**
     * Returns the difference of this vector and the specified vector.
     *
     * @param v the vector to subtract
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the difference of this vector and the specified vector
     */
    operator fun minus(v: Vector3d): Vector3d {
        return minus(v.x, v.y, v.z)
    }

    /**
     * Returns the difference of this vector and the specified vector.
     *
     * @param x x coordinate of the vector to subtract
     * @param y y coordinate of the vector to subtract
     * @param z z coordinate of the vector to subtract
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the difference of this vector and the specified vector
     */
    fun minus(ox: Double, oy: Double, oz: Double): Vector3d {
        return Vector3d.xyz(x - ox, y - oy, z - oz)
    }

    fun subtracted(v: Vector3d): Vector3d {
        return clone() - v
    }

    //////////////////////////////////
    // Vectorspace scalar mul.
    //////////////////////////////////


    /**
     * Returns the product of this vector and the specified value.
     *
     * @param a the value
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the product of this vector and the specified value
     */
    operator fun times(a: Double): Vector3d {
        return Vector3dImpl(x * a, y * a, z * a)
    }

    fun multiplied(a: Double): Vector3d {
        return clone() * a
    }

    fun multiplied(v: Vector3d): Vector3d {
        return clone() * v
    }

    /**
     * Returns this vector divided by the specified value.
     *
     * @param a the value
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return this vector divided by the specified value
     */
    operator fun div(a: Double): Vector3d {
        return Vector3dImpl(x / a, y / a, z / a)
    }

    operator fun div(v: Vector3d): Vector3d {
        return Vector3dImpl(x / v.x, y / v.y, z / v.z)
    }

    fun divided(a: Double): Vector3d {
        return clone() / a
    }


    //////////////////////////////////
    // Hadamard product
    //////////////////////////////////

    /**
     * Returns the product of this vector and the specified vector.
     *
     * @param a the vector
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the product of this vector and the specified vector
     */
    operator fun times(a: Vector3d): Vector3d {
        return times(a.x, a.y, a.z)
    }


    /**
     * Returns the product of this vector and the specified vector.
     *
     * @param x x coordinate of the vector to multiply
     * @param y y coordinate of the vector to multiply
     * @param z z coordinate of the vector to multiply
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the product of this vector and the specified vector
     */
    fun times(ox: Double, oy: Double, oz: Double): Vector3d {
        return Vector3d.xyz(x * ox, y * oy, z * oz)
    }


    //////////////////////////////////
    // Inner product
    //////////////////////////////////

    /**
     * Returns the dot product of this vector and the specified vector.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @param a the second vector
     *
     * @return the dot product of this vector and the specified vector
     */
    fun dot(a: Vector3d): Double {
        return x * a.x + y * a.y + z * a.z
    }

    //////////////////////////////////
    // Cross product
    //////////////////////////////////

    fun cross(a: Vector3d): Vector3d {
        return Vector3dImpl(
            y * a.z - z * a.y,
            z * a.x - x * a.z,
            x * a.y - y * a.x)
    }

    /**
     * Returns the cross product of this vector and the specified vector.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @param a the vector
     *
     * @return the cross product of this vector and the specified vector.
     */
    fun crossed(a: Vector3d): Vector3d {
        return Vector3dImpl(
                y * a.z - z * a.y,
            z * a.x - x * a.z,
            x * a.y - y * a.x)
    }


    //////////////////////////////////
    // Misc
    //////////////////////////////////

    /**
     * Returns the magnitude of this vector.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the magnitude of this vector
     */
    fun magnitude(): Double {
        return sqrt(this.dot(this))
    }

    /**
     * Returns the squared magnitude of this vector
     * (<code>this.dot(this)</code>).
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the squared magnitude of this vector
     */
    fun magnitudeSq(): Double {
        return this.dot(this)
    }

    /**
     * Returns the angle between this and the specified vector.
     *
     * @param v vector
     * @return angle in degrees
     */
    fun angle(v: Vector3d): Double {
        val v = dot(v) / (magnitude() * v.magnitude())
        return acos(max(min(v, 1.0), -1.0)) * 180.0 / PI // compensate rounding errors
    }

    /**
     * Returns the distance between the specified point and this point.
     *
     * @param p point
     * @return the distance between the specified point and this point
     */
    fun distance(p: Vector3d): Double {
        return minus(p).magnitude()
    }
    /**
     * Returns a new vector which is orthogonal to this vector.
     * @return a new vector which is orthogonal to this vector
     */
    fun orthogonal(): Vector3d {
        return if (z.absoluteValue < x.absoluteValue) {
            Vector3d.xy(y, -x)
        } else {
            Vector3d.yz(-z, y)
        }
    }

    /**
     * Returns a normalized copy of this vector with length {@code 1}.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return a normalized copy of this vector with length {@code 1}
     */
    fun normalized(): Vector3d {
        return this / magnitude()
    }

    fun normalize(): Vector3d {
        return this / magnitude()
    }

    /**
     * Returns a negated copy of this vector.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return a negated copy of this vector
     */
    operator fun unaryMinus(): Vector3d {
        return Vector3dImpl(-x, -y, -z)
    }

    fun negated(): Vector3d {
        return multiplied(-1.0)
    }

    /**
     * Linearly interpolates between this and the specified vector.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @param a vector
     * @param t interpolation value
     *
     * @return copy of this vector if {@code t = 0}; copy of a if {@code t = 1};
     * the point midway between this and the specified vector if {@code t = 0.5}
     */
    fun lerp(a: Vector3d, t: Double): Vector3d {
        return plus(a.minus(this).times(t))
    }


    /**
     * Projects the specified vector onto this vector.
     *
     * @param v vector to project onto this vector
     * @return the projection of the specified vector onto this vector
     */
    fun project(v: Vector3d): Vector3d {
        val pScale = v.dot(this) / magnitudeSq()
        return times(pScale)
    }

    fun projected(v: Vector3d): Vector3d {
        return clone().project(v)
    }

    /**
     * Indicates whether the two given points are collinear with this
     * vector/point.
     *
     * @param p2 second point
     * @param p3 third point
     *
     * @return {@code true} if all three points are collinear;
     *         {@code false} otherwise
     */
    fun collinear(p2: Vector3d, p3: Vector3d): Boolean {
        // The points p1, p2, p3 are collinear (are on the same line segment)
        // if and only if the largest of the lenghts of
        //
        //   a = P1P2,
        //   b = P1P2
        //   c = P2P3
        //
        // is equal to the sum of the other two.
        //
        // Explanation:
        //
        // If p1, p2 and p3 are on the same line then the point in the 'middle'
        // cuts the line segment into two smaller pieces. That is the sum of the
        // lengths of the smaller pieces is equal to the largest one.
        //
        // That is, the following expression determines whether the three points
        // are collinear
        //
        //   boolean collinear = largest > (second + third)

        val a = distance(p2)
        val b = distance(p3)
        val c = p2.distance(p3)

        var largest: Double = 0.0
        var second: Double = 0.0
        var third: Double = 0.0

        if (a > b && a > c) {
            // a is largest
            largest = a
            second = b
            third = c
        } else if (b > a && b > c) {
            // b is largest
            largest = b
            second = a
            third = c
        } else if (c > a && c > b) {
            // c is largest
            largest = c
            second = a
            third = b
        } else {
            // lengths are not distinct.
            //
            // there are two possibilities:
            //
            //   a: they are vertices of a equilateral triangle   -> false
            //   b: they are zero, i.e., all points are identical -> true
            //
            return a == 0.0 && b == 0.0 && c == 0.0
        }

        return (largest  - (second + third)).absoluteValue < Plane.TOL
    }

    //////////////////////////////////
    // Transformation
    //////////////////////////////////


    /**
     * Returns a transformed copy of this vector.
     *
     * @param transform the transform to apply
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return a transformed copy of this vector
     */
    fun transformed(transform: Transform): Vector3d {
        return transform.transform(this.asModifiable())
    }

    /**
     * Returns a transformed copy of this vector.
     *
     * @param transform the transform to apply
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @param amount
     *
     * @return a transformed copy of this vector
     */
    fun transformed(transform: Transform, amount: Double): Vector3d {
        return transform.transform(this.asModifiable(), amount)
    }



    //////////////////////////////////
    // String
    //////////////////////////////////

    /**
     * Returns this vector in STL string format.
     *
     * @return this vector in STL string format
     */
    fun toStlString(): String {
        return toStlString(StringBuilder()).toString()
    }

    /**
     * Returns this vector in STL string format.
     *
     * @param sb string builder
     * @return the specified string builder
     */
    fun toStlString(sb: StringBuilder): StringBuilder {
        return sb.append(x).append(" ").
        append(y).append(" ").
        append(z)
    }

    /**
     * Returns this vector in OBJ string format.
     *
     * @return this vector in OBJ string format
     */
    fun toObjString(): String {
        return toObjString(StringBuilder()).toString();
    }

    /**
     * Returns this vector in OBJ string format.
     *
     * @param sb string builder
     * @return the specified string builder
     */
    fun toObjString(sb: StringBuilder): StringBuilder {
        return sb.append(x).append(" ").
        append(y).append(" ").
        append(z)
    }

    companion object {
        /**
         * Unity vector {@code (1, 1, 1)}.
         */
        val UNITY = Vector3dImpl(1.0, 1.0, 1.0)

        /**
         * Vector {@code (1, 0, 0)}.
         */
        val X_ONE = Vector3dImpl(1.0, 0.0, 0.0)

        /**
         * Vector {@code (0, 1, 0)}.
         */
        val Y_ONE = Vector3dImpl(0.0, 1.0, 0.0)

        /**
         * Vector {@code (0, 0, 0)}.
         */
        val ZERO = Vector3dImpl(0.0, 0.0, 0.0)

        /**
         * Vector {@code (0, 0, 1)}.
         */
        val Z_ONE = Vector3dImpl(0.0, 0.0, 1.0)


        //////////////////////////////////
        // Factory methods
        //////////////////////////////////

        /**
         * Creates a new vector with specified {@code x}
         *
         * @param x x value
         * @return a new vector {@code [x,0,0]}
         *
         */
        fun x(x: Double): Vector3d = Vector3dImpl(x, 0.0, 0.0)


        /**
         * Creates a new vector with specified {@code y}
         *
         * @param y y value
         * @return a new vector {@code [0,y,0]}
         *
         */
        fun y(y: Double): Vector3d =
            Vector3dImpl(0.0, y, 0.0)

        /**
         * Creates a new vector with specified {@code z}
         *
         * @param z z value
         * @return a new vector {@code [0,0,z]}
         *
         */
        fun z(z: Double): Vector3d =
            Vector3dImpl(0.0, 0.0, z)


        /**
         * Creates a new vector with specified {@code x}, {@code y} and
         * {@code z = 0}.
         *
         * @param x x value
         * @param y y value
         * @return
         */
        fun xy(x: Double, y: Double): Vector3d =
            Vector3dImpl(x, y)


        /**
         * Creates a new vector with specified {@code x}, {@code y} and {@code z}.
         *
         * @param x x value
         * @param y y value
         * @param z z value
         * @return a new vector
         */
        fun xyz(x: Double, y: Double, z: Double): Vector3d =
            Vector3dImpl(x, y, z)

        /**
         * Creates a new vector with specified {@code y} and {@code z}.
         *
         * @param y y value
         * @param z z value
         * @return a new vector
         */
        fun yz(y: Double, z: Double) =
            Vector3dImpl(0.0, y, z)

        /**
         * Creates a new vector with specified {@code x} and {@code z}.
         *
         * @param x x value
         * @param z z value
         * @return a new vector
         */
        fun xz(x: Double, z: Double): Vector3d =
            Vector3dImpl(x, 0.0, z)


        /**
         * Creates a new vector {@code (0,0,0)}.
         *
         * @return a new vector
         */
        fun zero(): Vector3d =
            Vector3dImpl(0.0, 0.0, 0.0)


        /**
         * Creates a new vector {@code (1,1,1)}.
         *
         * @return a new vector
         */
        fun unity(): Vector3d = Vector3dImpl(1.0, 1.0, 1.0)


        fun toString(v: Vector3d): String {
            return "[${v.x}, ${v.y}, ${v.z}]"
        }

        fun equals(thisV: Vector3d, obj: Any?): Boolean {
            if (obj == null) {
                return false
            }
            if (thisV.javaClass != obj.javaClass) {
                return false
            }
            val other =  obj as Vector3d
            if ((thisV.x - other.x).absoluteValue > Plane.TOL) {
                return false
            }
            if ((thisV.y - other.y).absoluteValue > Plane.TOL) {
                return false
            }
            if ((thisV.z - other.z).absoluteValue > Plane.TOL) {
                return false
            }
            return true
        }

        fun getHashCode(v: Vector3d): Int {
            var hash = 7
            hash = 67 * hash + v.x.toRawBits().xor(v.x.toRawBits().ushr(32)).toInt()
            hash = 67 * hash + v.y.toRawBits().xor(v.y.toRawBits().ushr(32)).toInt()
            hash = 67 * hash + v.z.toRawBits().xor(v.z.toRawBits().ushr(32)).toInt()
            return hash
        }
    }
}
