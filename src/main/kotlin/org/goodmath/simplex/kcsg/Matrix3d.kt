package org.goodmath.simplex.kcsg

import org.goodmath.simplex.kcsg.vvecmath.Vector3d

/**
 * 3D Matrix3d
 *
 * @author cpoliwoda
 */
class Matrix3d(
    var m11: Double, var m12: Double, var m13: Double,
    var m21: Double, var m22: Double, var m23: Double,
    var m31: Double, var m32: Double, var m33: Double) {


    override fun toString(): String {
        return "[$m11, $m12, $m13]\n[$m21, $m22, $m23]\n[$m31, $m32, $m33]";
    }

    /**
     * Returns the product of this matrix and the specified value.
     *
     * @param a the value
     *
     * <b>Note:</b> this matrix is not modified.
     *
     * @return the product of this matrix and the specified value
     */
    fun times(a: Double): Matrix3d {
        return Matrix3d(
            m11 * a, m12 * a, m13 * a,
            m21 * a, m22 * a, m23 * a,
            m31 * a, m32 * a, m33 * a)
    }

    /**
     * Returns the product of this matrix and the specified vector.
     *
     * @param a the vector
     *
     * <b>Note:</b> the vector is not modified.
     *
     * @return the product of this matrix and the specified vector
     */
    fun times(a: Vector3d): Vector3d {
        return Vector3d.xyz(
            m11 * a.x + m12 * a.y + m13 * a.z,
            m21 * a.x + m22 * a.y + m23 * a.z,
            m31 * a.x + m32 * a.y + m33 * a.z)
    }


    companion object {
        val ZERO = Matrix3d(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val UNITY = Matrix3d(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
    }
}
