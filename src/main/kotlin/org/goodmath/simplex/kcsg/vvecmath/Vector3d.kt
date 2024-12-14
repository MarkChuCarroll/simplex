package org.goodmath.simplex.kcsg.vvecmath

import java.util.Random
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


interface Vector3d {

    fun clone(): Vector3d

    /**
     * Returns the angle between this and the specified vector.
     *
     * @param v vector
     * @return angle in degrees
     */
    fun angle(v: Vector3d): Double {
        val vee = dot(v) / (magnitude * v.magnitude)
        return acos(max(min(vee, 1.0), -1.0)) * 180.0 / PI // compensate rounding errors
    }

    /**
     * Returns the distance between the specified point and this point.
     *
     * @param p point
     * @return the distance between the specified point and this point
     */
    fun distance(p: Vector3d): Double {
        return minus(p).magnitude
    }

    /**
     * Returns the {@code x} component of this vector.
     *
     * @return the {@code x} component of this vector
     */
    val x: Double

    /**
     * Returns the {@code y} component of this vector.
     *
     * @return the {@code y} component of this vector
     */
    val y: Double

    /**
     * Returns the {@code z} component of this vector.
     *
     * @return the {@code z} component of this vector
     */
    val z: Double

    /**
     * Returns the components {code x,y,z} as double array.
     *
     * @return the components {code x,y,z} as double array
     */
    fun get(): List<Double> {
        return listOf(x, y, z)
    }

    /**
     * Returns the i-th component of this vector.
     *
     * @param i component index
     * @return the i-th component of this vector
     */
    fun get(i: Int): Double {
        return when (i) {
            0 -> x
            1 -> y
            2 -> z
            else -> throw RuntimeException("Illegal index: $i")
        }
    }

    /**
     * Returns a negated copy of this vector.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return a negated copy of this vector
     */
    fun negated(): Vector3d {
        return Vector3dImpl(-x, -y, -z)
    }

    /**
     * Returns the sum of this vector and the specified vector.
     *
     * @param v the vector to add
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the sum of this vector and the specified vector
     */
    fun plus(v: Vector3d): Vector3d {
        return Vector3dImpl(x + v.x, y + v.y, z + v.z)
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
    fun minus(v: Vector3d): Vector3d {
        return Vector3dImpl(x - v.x, y - v.y, z - v.z)
    }

    /**
     * Returns the product of this vector and the specified value.
     *
     * @param a the value
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the product of this vector and the specified value
     */
    fun times(a: Double): Vector3d {
        return Vector3dImpl(x * a, y * a, z * a)
    }

    /**
     * Returns the product of this vector and the specified vector.
     *
     * @param a the vector
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the product of this vector and the specified vector
     */
    fun times(a: Vector3d): Vector3d {
        return Vector3dImpl(x * a.x, y * a.y, z * a.z)
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
    fun divided(a: Double): Vector3d {
        return Vector3dImpl(x / a, y / a, z / a)
    }

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
        return this.x * a.x + this.y * a.y + this.z * a.z
    }

    /**
     * Linearly interpolates between this and the specified vector.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @param a vector
     * @param t interpolation value
     *
     * @return copy of this vector if {@code t = 0}; copy of "a" if {@code t = 1};
     * the point midway between this and the specified vector if {@code t = 0.5}
     */
    fun linearInterpolation(a: Vector3d, t: Double): Vector3d {
        return plus(a.minus(this).times(t))
    }

    /**
     * Returns the magnitude of this vector.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the magnitude of this vector
     */
    val magnitude: Double
        get() = sqrt(this.dot(this))

    /**
     * Returns the squared magnitude of this vector
     * (<code>this.dot(this)</code>).
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return the squared magnitude of this vector
     */
    val magnitudeSq: Double
        get() = this.dot(this)

    /**
     * Returns a normalized copy of this vector with length {@code 1}.
     *
     * <b>Note:</b> this vector is not modified.
     *
     * @return a normalized copy of this vector with length {@code 1}
     */
    fun normalized(): Vector3d {
        return divided(magnitude)
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
        return Vector3dImpl(y * a.z - z * a.y, z * a.x - x * a.z, x * a.y - y * a.x)
    }

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
        return sb.append(x).append(" ").append(y).append(" ").append(z)
    }

    /**
     * Returns this vector in OBJ string format.
     *
     * @return this vector in OBJ string format
     */
    fun toObjString(): String {
        return toObjString(StringBuilder()).toString()
    }

    /**
     * Returns this vector in OBJ string format.
     *
     * @param sb string builder
     * @return the specified string builder
     */
    fun toObjString(sb: StringBuilder): StringBuilder {
        return sb.append(this.x).append(" ").append(this.y).append(" ").append(this.z)
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

    /**
     * Creates a new vector which is orthogonal to this.
     *
     * this_i , this_j , this_k => i,j,k € {1,2,3} permutation
     *
     * looking for orthogonal vector o to vector this: this_i * o_i + this_j *
     * o_j + this_k * o_k = 0
     *
     * @return a new vector which is orthogonal to this
     */
    fun orthogonal(): Vector3d {

//        if ((this.x == Double.NaN) || (this.y == Double.NaN) || (this.z == Double.NaN)) {
//            throw new IllegalStateException("NaN is not a valid entry for a vector.");
//        }
        var o1 = 0.0
        var o2 = 0.0
        var o3 = 0.0

        val r = Random()

        var numberOfZeroEntries = 0

        if (x == 0.0) {
            numberOfZeroEntries++
            o1 = r.nextDouble()
        }

        if (y == 0.0) {
            numberOfZeroEntries++
            o2 = r.nextDouble()
        }

        if (z == 0.0) {
            numberOfZeroEntries++
            o3 = r.nextDouble()
        }

        when (numberOfZeroEntries) {
            0 -> {
                // all this_i != 0
                //
                //we do not want o3 to be zero
                while (o3 == 0.0) {
                    o3 = r.nextDouble()
                }

                //we do not want o2 to be zero
                while (o2 == 0.0) {
                    o2 = r.nextDouble()
                }
                // calculate or choose randomly ??
                // o2 = -this.z * o3 / this.y;
                o1 = (-this.y * o2 - this.z * o3) / this.x
            }

            1 -> {

                // this_i = 0 , i € {1,2,3}
                // this_j != 0 != this_k , j,k € {1,2,3}\{i}
                //
                // choose one none zero randomly and calculate the other one

                if (this.x == 0.0) {
                    //we do not want o3 to be zero
                    while (o3 == 0.0) {
                        o3 = r.nextDouble()
                    }

                    o2 = -this.z * o3 / this.y

                } else if (this.y == 0.0) {

                    //we do not want o3 to be zero
                    while (o3 == 0.0) {
                        o3 = r.nextDouble()
                    }

                    o1 = -this.z * o3 / this.x

                } else if (this.z == 0.0) {

                    //we do not want o1 to be zero
                    while (o1 == 0.0) {
                        o1 = r.nextDouble()
                    }

                    o2 = -this.z * o1 / this.y
                }

            }

            2 -> {
                // if two parts of this are 0 we can achieve orthogonality
                // via setting the corresponding part of the orthogonal vector
                // to zero this is ALREADY DONE in the init (o_i = 0.0)
                // NO CODE NEEDED
            }

            3 -> {
                System.err.println("This vector is equal to (0,0,0). ")
            }

            else -> {
                System.err.println("The orthogonal one is set randomly.")
                o1 = r.nextDouble()
                o2 = r.nextDouble()
                o3 = r.nextDouble()
            }
        }

        var result: Vector3d = Vector3dImpl(o1, o2, o3)

        // check if the created vector is really orthogonal to this
        // if not try one more time
        while (this.dot(result) != 0.0) {
            result = orthogonal()
        }

        return result
    }

    /**
     * Returns a modifiable copy of this vector.
     *
     * @return a modifiable copy of this vector
     */
    fun asModifiable(): ModifiableVector3d {
        return ModifiableVector3dImpl(x, y, z)
    }

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
    fun plus(x: Double, y: Double, z: Double): Vector3d {
        return xyz(this.x + x, this.y + y, this.z + z)
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
    fun minus(x: Double, y: Double, z: Double): Vector3d {
        return xyz(this.x - x, this.y - y, this.z - z)
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
    fun times(x: Double, y: Double, z: Double): Vector3d {
        return xyz(this.x * x, this.y * y, this.z * z)
    }

    /**
     * Projects the specified vector onto this vector.
     *
     * @param v vector to project onto this vector
     * @return the projection of the specified vector onto this vector
     */
    fun project(v: Vector3d): Vector3d {
        val pScale = v.dot(this) / magnitudeSq
        return this.times(pScale)
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

        /**
         * Creates a new vector with specified {@code x}
         *
         * @param x x value
         * @return a new vector {@code [x,0,0]}
         *
         */
        fun x(x: Double): Vector3d {
            return Vector3dImpl(x, 0.0, 0.0)
        }

        /**
         * Creates a new vector with specified {@code y}
         *
         * @param y y value
         * @return a new vector {@code [0,y,0]}
         *
         */
        fun y(y: Double): Vector3d {
            return Vector3dImpl(0.0, y, 0.0)
        }

        /**
         * Creates a new vector with specified {@code z}
         *
         * @param z z value
         * @return a new vector {@code [0,0,z]}
         *
         */
        fun z(z: Double): Vector3d {
            return Vector3dImpl(0.0, 0.0, z)
        }

        /**
         * Creates a new vector with specified {@code x}, {@code y} and
         * {@code z = 0}.
         *
         * @param x x value
         * @param y y value
         * @return
         */
        fun xy(x: Double, y: Double): Vector3d {
            return Vector3dImpl(x, y, 0.0)
        }

        /**
         * Creates a new vector with specified {@code x}, {@code y} and {@code z}.
         *
         * @param x x value
         * @param y y value
         * @param z z value
         * @return a new vector
         */
        fun xyz(x: Double, y: Double, z: Double): Vector3d {
            return Vector3dImpl(x, y, z)
        }

        /**
         * Creates a new vector with specified {@code y} and {@code z}.
         *
         * @param y y value
         * @param z z value
         * @return a new vector
         */
        fun yz(y: Double, z: Double): Vector3d {
            return Vector3dImpl(0.0, y, z)
        }

        /**
         * Creates a new vector with specified {@code x} and {@code z}.
         *
         * @param x x value
         * @param z z value
         * @return a new vector
         */
        fun xz(x: Double, z: Double): Vector3d {
            return Vector3dImpl(x, 0.0, z)
        }

        /**
         * Creates a new vector {@code (0,0,0)}.
         *
         * @return a new vector
         */
        fun zero(): Vector3d {
            return Vector3dImpl(0.0, 0.0, 0.0)
        }

        /**
         * Creates a new vector {@code (1,1,1)}.
         *
         * @return a new vector
         */
        fun unity(): Vector3d {
            return Vector3dImpl(0.0, 0.0, 0.0)
        }
    }
}


/**
 * Modifiable 3d vector.
 *
 * @author Michael Hoffer (info@michaelhoffer.de)
 */
interface ModifiableVector3d : Vector3d {

    fun scale(s: Double): Vector3d {
        x *= s
        y *= s
        z *= s
        return this
    }

    /**
     * Sets the specified vector components.
     *
     * @param xyz vector components to set (number of components {@code <= 3} are valid)
     * @return this vector
     */
    fun set(vararg xyz: Double): Vector3d

    /**
     * Sets the i-th component of this vector.
     *
     * @param i component index
     * @param value value to set
     * @return this vector
     */
    fun set(i: Int, value: Double): Vector3d

    /**
     * Sets the {@code x} component of this vector.
     *
     * @param x component to set
     */
    override var x: Double

    /**
     * Sets the {@code y} component of this vector.
     *
     * @param y component to set
     */
    override var y: Double


    /**
     * Sets the {@code z} component of this vector.
     *
     * @param z component to set
     */
    override var z: Double

    /**
     * Adds the specified vector to this vector.
     *
     * @param v the vector to add
     *
     * <b>Note:</b> this vector <b>is</b> not modified.
     *
     * @return this vector
     */
    fun add(v: Vector3d): Vector3d {
        x += v.x
        y += v.y
        z += v.z
        return this
    }

    /**
     * Adds the specified vector to this vector.
     *
     * @param x x coordinate of the vector to add
     * @param y y coordinate of the vector to add
     * @param z z coordinate of the vector to add
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    fun add(x: Double, y: Double, z: Double): Vector3d {
        this.x += x
        this.y += y
        this.z += z
        return this
    }

    /**
     * Subtracts the specified vector from this vector.
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @param v vector to subtract
     * @return this vector
     */
    fun subtract(v: Vector3d): Vector3d {
        x -= v.x
        y -= v.y
        z -= v.z

        return this
    }

    /**
     * Subtracts the specified vector from this vector.
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @param x x coordinate of the vector to subtract
     * @param y y coordinate of the vector to subtract
     * @param z z coordinate of the vector to subtract
     *
     * @return this vector
     */
    fun subtract(x: Double, y: Double, z: Double): Vector3d {
        this.x -= x
        this.y -= y
        this.z -= z
        return this
    }

    /**
     * Multiplies this vector with the specified value.
     *
     * @param a the value
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    fun multiply(a: Double): Vector3d {
        x *= a
        y *= a
        z *= a
        return this
    }

    /**
     * Multiplies this vector with the specified vector.
     *
     * @param a the vector
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    fun multiply(a: Vector3d): Vector3d {
        x *= a.x
        y *= a.y
        z *= a.z

        return this
    }

    /**
     * Divides this vector with the specified value.
     *
     * @param a the value
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    fun divide(a: Double): Vector3d {
        x /= a
        y /= a
        z /= a
        return this
    }

    /**
     * Divides this vector with the specified vector.
     *
     * @param v the vector
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    fun divide(v: Vector3d): Vector3d {
        x /= v.x
        y /= v.y
        z /= v.z
        return this
    }

    /**
     * Stores the cross product of this vector and the specified vector in this
     * vector.
     *
     * <b>Note:</b> this vector <b>is</b>modified.
     *
     * @param a the vector
     *
     * @return this vector
     */
    fun cross(a: Vector3d): Vector3d {
        x = y * a.z - z * a.y
        y = z * a.x - x * a.z
        z = x * a.y - y * a.x
        return this
    }

    /**
     * Negates this vector.
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    fun negate(): Vector3d {
        return multiply(-1.0)
    }

    /**
     * Normalizes this vector with length {@code 1}.
     *
     * <b>Note:</b> this vector <b>is</b> modified.
     *
     * @return this vector
     */
    fun normalize(): Vector3d {
        return this.divide(this.magnitude)
    }

}
