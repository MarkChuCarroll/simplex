package org.goodmath.simplex.kcsg

import org.goodmath.simplex.vvecmath.Vector3d

/**
 * 3D Matrix3d
 *
 * @author cpoliwoda
 * Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
data class Matrix3d(
    val m11: Double, val m12: Double, val m13: Double,
    val m21: Double, val m22: Double, val m23: Double,
    val m31: Double, val m32: Double, val m33: Double) {

    override fun toString(): String {
        return "[$m11, $m12, $m13]\n[$m21, $m22, $m23]\n[$m31, $m32, $m33]"
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
    operator fun times(a: Double): Matrix3d {
        return  Matrix3d(
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
    operator fun times(a: Vector3d): Vector3d {
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
