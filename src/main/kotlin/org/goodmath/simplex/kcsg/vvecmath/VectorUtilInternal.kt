package org.goodmath.simplex.kcsg.vvecmath

import kotlin.math.absoluteValue

/**
 * Internal utility class.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
object VectorUtilInternal {

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
        val other = obj as Vector3d
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
        var hash: Long = 7
        hash = 67 * hash + v.x.toRawBits() xor (v.x.toRawBits() ushr 32)
        hash = 67 * hash + v.y.toRawBits() xor (v.y.toRawBits() ushr 32)
        hash = 67 * hash + v.z.toRawBits() xor (v.z.toRawBits() ushr 32)
        return hash.toInt()
    }
}
