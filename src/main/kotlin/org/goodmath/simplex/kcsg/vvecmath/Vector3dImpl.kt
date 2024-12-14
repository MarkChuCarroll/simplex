package org.goodmath.simplex.kcsg.vvecmath

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
open class Vector3dImpl(override var x: Double, override var y: Double, override var z: Double): Vector3d, Cloneable {
    override fun clone(): Vector3d {
        return Vector3dImpl(x, y, z)
    }


    open fun set(vararg xyz: Double): Vector3d {
        if(xyz.size > 3) {
            throw IllegalArgumentException(
                    "Wrong number of components. "
                            + "Expected number of components <= 3, got: ${xyz.size}, but found ${xyz.size}")
        }

        for (i in 0 until xyz.size) {
            set(i, xyz[i])
        }
        return this
    }

    open fun set(i: Int, value: Double): Vector3d {
        when (i) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else ->
                throw InvalidParameterException("Illegal index: $i")
        }
        return this
    }

    override fun toString(): String {
        return VectorUtilInternal.toString(this)
    }

    override fun hashCode(): Int {
        return VectorUtilInternal.getHashCode(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vector3dImpl

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

}


/**
 * Modifiable 3d vector.
 *
 * @author Michael Hoffer (info@michaelhoffer.de)
 */
open class ModifiableVector3dImpl(x: Double, y: Double, z: Double): Vector3dImpl(x, y, z), ModifiableVector3d {

    override fun set(vararg xyz: Double): Vector3d {
        return super.set(*xyz)
    }

    override fun set(i: Int, value: Double): Vector3d {
        return super.set(i, value)
    }
}

