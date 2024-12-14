package org.goodmath.simplex.kcsg.vvecmath

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Transform. Transformations (translation, rotation, scale) can be applied to
 * geometrical objects like {@link CSG}, {@link Polygon}, {@link Vertex} and
 * {@link Vector3d}.
 *
 * This transform class uses the builder pattern to define combined
 * transformations.<br><br>
 *
 * <b>Example:</b>
 *
 * <blockquote><pre>
 * // t applies rotation and translation
 * Transform t = Transform.unity().rotX(45).translate(2,1,0);
 * </pre></blockquote>
 *
 * <b>TODO:</b> use quaternions for rotations.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
class Transform(val m: Matrix4d) {
    constructor() : this(Matrix4d()) {
        m.m00 = 1.0
        m.m11 = 1.0
        m.m22 = 1.0
        m.m33 = 1.0

    }



    /**
     * Applies rotation operation around the x-axis to this transform.
     *
     * @param degrees degrees
     * @return this transform
     */
    fun rotX(degrees: Double): Transform {
        val radians = degrees * Math . PI *(1.0 / 180.0)
        val vCos = cos(radians)
        val vSin = sin(radians)
        val elements = listOf(
            1.0, 0.0, 0.0, 0.0, 0.0,
            vCos, vSin, 0.0, 0.0, -vSin, vCos,
            0.0, 0.0, 0.0, 0.0, 1.0)
        m.mul(Matrix4d (elements))
        return this
    }

    /**
     * Applies rotation operation around the y-axis to this transform.
     *
     * @param degrees degrees
     *
     * @return this transform
     */
    fun rotY(degrees: Double): Transform {
        val radians = degrees * PI *(1.0 / 180.0)
        val vCos = cos(radians)
        val vSin = sin(radians)
        val elements = listOf(
            vCos, 0.0, -vSin, 0.0, 0.0, 1.0,
            0.0, 0.0, vSin, 0.0, vCos, 0.0,
            0.0, 0.0, 0.0, 1.0)
        m.mul(Matrix4d(elements))
        return this
    }

    /**
     * Applies rotation operation around the z axis to this transform.
     *
     * @param degrees degrees
     *
     * @return this transform
     */
    fun rotZ(degrees: Double): Transform {
        val radians = degrees * PI *(1.0 / 180.0)
        val vCos = cos(radians)
        val vSin = sin (radians)
        val elements = listOf(
            vCos, vSin, 0.0, 0.0, -vSin, vCos,
            0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0)

        m.mul(Matrix4d (elements))
        return this
    }

    /**
     * Applies a rotation operation to this transform.
     *
     * @param x x axis rotation (degrees)
     * @param y y axis rotation (degrees)
     * @param z z axis rotation (degrees)
     *
     * @return this transform
     */
    fun rot(x: Double, y: Double, z: Double): Transform {
        return rotX(x).rotY(y).rotZ(z)
    }

    /**
     * Applies a rotation operation to this transform.
     *
     * @param vec axis rotation for x, y, z (degrees)
     *
     * @return this transform
     */
    fun rot(vec: Vector3d): Transform {
        // TODO: use quaternions
        return rotX(vec.x).rotY(vec.y).rotZ(vec.z)
    }

    /**
     * Applies a transformation that rotates one vector into another.
     *
     * @param from source vector
     * @param to target vector
     * @return this transformation
     */
    fun rot(from: Vector3d, to: Vector3d): Transform {
        val a = from.normalized()
        val b = to.normalized()
        val c = a.crossed (b)

        val l = c.magnitude// sine of angle

        if (l > 1e-9) {
            val axis = c.normalized()
            val angle = a.angle(b)
            rot(Vector3d.ZERO, axis, angle)
        }
        return this
    }

    /**
     * Applies a rotation operation about the specified rotation axis.
     *
     * @param axisPos axis point
     * @param inAxisDir axis direction (can be un-normalized)
     * @param degrees rotation angle in degrees
     * @return this transform
     */
    fun rot(axisPos: Vector3d, inAxisDir: Vector3d, degrees: Double): Transform {
        val tmp = unity()
        var axisDir = inAxisDir

        axisDir = axisDir.normalized()

        val dir2 = axisDir.times(axisDir)

        val posX = axisPos.x
        val posY = axisPos.y
        val posZ = axisPos.z

        val dirX = axisDir.x
        val dirY = axisDir.y
        val dirZ = axisDir.z

        val dirXSquare = dir2.x
        val dirYSquare = dir2.y
        val dirZSquare = dir2.z

        val radians = degrees * PI *(1.0 / 180.0)

        val cosOfAngle = cos (radians)
        val oneMinusCosOfAngle = 1-cosOfAngle
        val sinOfAngle = sin (radians)

        tmp.m.m00 = dirXSquare + (dirYSquare + dirZSquare) * cosOfAngle

        tmp.m.m01 = dirX * dirY * oneMinusCosOfAngle - dirZ * sinOfAngle

        tmp.m.m02 = dirX * dirZ * oneMinusCosOfAngle + dirY * sinOfAngle

        tmp.m.m03 = (posX * (dirYSquare + dirZSquare) - dirX * (posY * dirY + posZ * dirZ)) * oneMinusCosOfAngle +(posY * dirZ - posZ * dirY) * sinOfAngle

        tmp.m.m10 = dirX * dirY * oneMinusCosOfAngle + dirZ * sinOfAngle

        tmp.m.m11 = dirYSquare + (dirXSquare + dirZSquare) * cosOfAngle

        tmp.m.m12 = dirY * dirZ * oneMinusCosOfAngle - dirX * sinOfAngle

        tmp.m.m13 = (posY * (dirXSquare + dirZSquare) - dirY * (posX * dirX + posZ * dirZ)) * oneMinusCosOfAngle+(posZ * dirX - posX * dirZ) * sinOfAngle

        tmp.m.m20 = dirX * dirZ * oneMinusCosOfAngle - dirY * sinOfAngle

        tmp.m.m21 = dirY * dirZ * oneMinusCosOfAngle + dirX * sinOfAngle

        tmp.m.m22 = dirZSquare + (dirXSquare + dirYSquare) * cosOfAngle
        tmp.m.m23 = (posZ * (dirXSquare + dirYSquare) - dirZ * (posX * dirX + posY * dirY)) * oneMinusCosOfAngle+(posX * dirY - posY * dirX) * sinOfAngle

        apply(tmp)
        return this
    }

    /**
     * Applies a translation operation to this transform.
     *
     * @param vec translation vector (x,y,z)
     *
     * @return this transform
     */
    fun translate(vec: Vector3d): Transform {
        return translate(vec.x, vec.y, vec.z)
    }

    /**
     * Applies a translation operation to this transform.
     *
     * @param x translation (x-axis)
     * @param y translation (y-axis)
     * @param z translation (z-axis)
     *
     * @return this transform
     */
    fun translate(x: Double, y: Double, z: Double): Transform {
        val elements = listOf(
            1.0, 0.0, 0.0, x,
            0.0, 1.0, 0.0, y,
            0.0, 0.0, 1.0, z,
            0.0, 0.0, 0.0, 1.0)
        m.mul(Matrix4d(elements))
        return this
    }

    /**
     * Applies a translation operation to this transform.
     *
     * @param value translation (x axis)
     *
     * @return this transform
     */
    fun translateX(value: Double): Transform {
        val elements = listOf(
            1.0, 0.0, 0.0, value,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 1.0)
        m.mul(Matrix4d(elements))
        return this
    }

    /**
     * Applies a translation operation to this transform.
     *
     * @param value translation (y-axis)
     *
     * @return this transform
     */
    fun translateY(value: Double): Transform {
        val elements = listOf(
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, value,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 1.0)
        m.mul(Matrix4d(elements))
        return this
    }

    /**
     * Applies a translation operation to this transform.
     *
     * @param value translation (z-axis)
     *
     * @return this transform
     */
    fun translateZ(value: Double): Transform {
        val elements = listOf(
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, value,
            0.0, 0.0, 0.0, 1.0)
        m.mul(Matrix4d(elements))
        return this
    }

    /**
     * Applies a mirror operation to this transform.
     *
     * @param plane the plane that defines the mirror operation
     *
     * @return this transform
     */
    fun mirror(plane: Plane): Transform {
        System.err.println("WARNING: I'm too dumb to implement the mirror() operation correctly. Please fix me!")
        val nx = plane.normal.x
        val ny = plane.normal.y
        val nz = plane.normal.z
        val w = plane.getDist()
        val elements = listOf(
            (1.0 - 2.0 * nx * nx), (-2.0 * ny * nx), (-2.0 * nz * nx), 0.0,
            (-2.0 * nx * ny), (1.0-2.0 * ny * ny), (-2.0 * nz * ny), 0.0,
            (-2.0 * nx * nz), (-2.0 * ny * nz), (1.0-2.0 * nz * nz), 0.0,
            (-2.0 * nx * w), (-2.0 * ny * w), (-2.0 * nz * w), 1.0
        )
        m.mul(Matrix4d(elements))
        return this
    }

    /**
     * Applies a scale operation to this transform.
     *
     * @param vec vector that specifies scale (x,y,z)
     *
     * @return this transform
     */
    fun scale(vec: Vector3d): Transform {

        if (vec.x == 0.0 || vec.y == 0.0 || vec.z == 0.0) {
            throw IllegalArgumentException ("scale by 0 not allowed!")
        }
        val elements = listOf(
            vec.x, 0.0, 0.0, 0.0, 0.0,
            vec.y, 0.0, 0.0, 0.0, 0.0,
            vec.z, 0.0, 0.0, 0.0, 0.0, 1.0)
        m.mul(Matrix4d(elements))
        return this
    }

    /**
     * Applies a scale operation to this transform.
     *
     * @param x x scale value
     * @param y y scale value
     * @param z z scale value
     *
     * @return this transform
     */
    fun scale(x: Double, y: Double, z: Double): Transform {
        if (x == 0.0 || y == 0.0 || z == 0.0) {
            throw IllegalArgumentException ("scale by 0 not allowed!")
        }

        val elements = listOf(x, 0.0, 0.0, 0.0, 0.0,
            y, 0.0, 0.0, 0.0, 0.0,
            z, 0.0, 0.0, 0.0, 0.0, 1.0)
        m.mul(Matrix4d(elements))
        return this
    }

    /**
     * Applies a scale operation to this transform.
     *
     * @param s s scale value (x, y and z)
     *
     * @return this transform
     */
    fun scale(s: Double): Transform {
        if (s == 0.0) {
            throw IllegalArgumentException ("scale by 0 not allowed!")
        }

        val elements = listOf(s, 0.0, 0.0, 0.0, 0.0,
            s, 0.0, 0.0, 0.0, 0.0,
            s, 0.0, 0.0, 0.0, 0.0, 1.0)
        m.mul(Matrix4d(elements))
        return this
    }

    /**
     * Applies a scale operation (x-axis) to this transform.
     *
     * @param s x scale value
     *
     * @return this transform
     */
    fun scaleX(s: Double): Transform {
        if (s == 0.0) {
            throw IllegalArgumentException ("scale by 0 not allowed!")
        }

        val elements = listOf(s, 0.0, 0.0, 0.0, 0.0,
            1.0, 0.0, 0.0, 0.0, 0.0,
            1.0, 0.0, 0.0, 0.0, 0.0, 1.0)
        m.mul(Matrix4d(elements))
        return this
    }

    /**
     * Applies a scale operation (y-axis) to this transform.
     *
     * @param s y scale value
     *
     * @return this transform
     */
    fun scaleY(s: Double): Transform {
        if (s == 0.0) {
            throw IllegalArgumentException ("scale by 0 not allowed!")
        }

        val elements = listOf(
            1.0, 0.0, 0.0, 0.0, 0.0,
            s, 0.0, 0.0, 0.0, 0.0,
            1.0, 0.0, 0.0, 0.0, 0.0, 1.0)
        m.mul(Matrix4d(elements))
        return this
    }

    /**
     * Applies a scale operation (z axis) to this transform.
     *
     * @param s z scale value
     *
     * @return this transform
     */
    fun scaleZ(s: Double): Transform {
        if (s == 0.0) {
            throw IllegalArgumentException("scale by 0 not allowed!")
        }
        val elements = listOf(
            1.0, 0.0, 0.0, 0.0, 0.0,
            1.0, 0.0, 0.0, 0.0, 0.0,
            s, 0.0, 0.0, 0.0, 0.0, 1.0)
        m.mul(Matrix4d(elements))
        return this
    }

    /**
     * Applies this transform to the specified vector.
     *
     * @param vec vector to transform
     *
     * @return the specified vector
     */
     fun transform(vec: ModifiableVector3d): ModifiableVector3d {
        val x = m.m00 * vec.x + m.m01 * vec.y + m.m02 * vec.z + m.m03
        val y = m.m10 * vec.x + m.m11 * vec.y + m.m12 * vec.z + m.m13
        vec.z = m.m20 * vec.x + m.m21 * vec.y + m.m22 * vec.z + m.m23
        vec.x = x
        vec.y = y
        return vec
    }

    /**
     * Applies this transform to the specified vector.
     *
     * @param vec vector to transform
     *
     * @return the specified vector
     */
    fun transform(vec: Vector3d): Vector3d {
        val result = vec.asModifiable()
        val x = m.m00 * vec.x + m.m01 * vec.y + m.m02 * vec.z + m.m03
        val y = m.m10 * vec.x + m.m11 * vec.y + m.m12 * vec.z + m.m13
        result.z = m.m20 * vec.x + m.m21 * vec.y + m.m22 * vec.z + m.m23
        result.x = x
        result.y = y
        return result
    }

    /**
     * Applies this transform to the specified vector.
     *
     * @param vec vector to transform
     * @param amount transform amount (0 = 0 %, 1 = 100%)
     *
     * @return the specified vector
     */
    fun transform(vec: ModifiableVector3d, amount: Double): ModifiableVector3d {
        val prevX = vec.x
        val prevY = vec.y
        val prevZ = vec.z

        val x = m.m00 * vec.x + m.m01 * vec.y + m.m02 * vec.z + m.m03
        val y = m.m10 * vec.x + m.m11 * vec.y + m.m12 * vec.z + m.m13
        vec.z = m.m20 * vec.x + m.m21 * vec.y + m.m22 * vec.z + m.m23
        vec.x = x
        vec.y = y

        val diffX = vec.x - prevX
        val diffY = vec.y - prevY
        val diffZ = vec.z - prevZ

        vec.x = prevX + (diffX) * amount
        vec.y = prevY + (diffY) * amount
        vec.z = prevZ + (diffZ) * amount

        return vec
    }

    /**
     * Indicates whether this transform performs a mirror operation, i.e., flips
     * the orientation.
     *
     * @return <code>true</code> if this transform performs a mirror operation;
     * <code>false</code> otherwise
     */
    fun isMirror(): Boolean {
        return m.determinant() < 0
    }

    /**
     * Applies the specified transform to this transform.
     *
     * @param t transform to apply
     *
     * @return this transform
     */
    fun apply(t: Transform): Transform {
        m.mul(t.m)
        return this
    }

    override fun toString(): String {
        return m.toString()
    }

    companion object {
        /**
         * Returns a new unity transform.
         *
         * @return unity transform
         */
        fun unity(): Transform {
            return Transform()
        }

    }
}
