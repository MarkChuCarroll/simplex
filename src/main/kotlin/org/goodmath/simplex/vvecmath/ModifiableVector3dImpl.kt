package org.goodmath.simplex.vvecmath

class ModifiableVector3dImpl(
    override var x: Double,
    override var y: Double,
    override var z: Double
): ModifiableVector3d {
    override fun set(xyz: List<Double>): Vector3d {
        x = xyz[0]
        y = xyz[1]
        z = xyz[2]
        return this
    }

    override fun set(i: Int, value: Double): Vector3d {
        when(i) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> throw IndexOutOfBoundsException()
        }
        return this
    }

    override fun clone(): Vector3d {
        return ModifiableVector3dImpl(x, y, z)
    }
}
