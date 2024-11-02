package org.goodmath.simplex.runtime.values.csg

import eu.mihosoft.vvecmath.Transform

import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.runtime.values.primitives.Vec3
import org.goodmath.simplex.twist.Twist

class Solid(val csg: CSG): Value {
    override val valueType: ValueType
        get() = TODO("Not yet implemented")

    override fun twist(): Twist {
        TODO("Not yet implemented")
    }

    fun move(x: Double, y: Double, z: Double): Solid {
        return Solid(csg.transformed(Transform().translate(x, y, z)))
    }

    fun move(offset: Vec3): Solid {
        return Solid(csg.transformed(Transform().translate(offset.v3)))
    }

    fun rotate(xAngle: Double, yAngle: Double, zAngle: Double): Solid {
        return Solid(csg.transformed(Transform().rot(xAngle, yAngle, zAngle)))
    }

    fun scale(x: Double, y: Double, z: Double): Solid {
        return Solid(csg.transformed(Transform().scale(x, y, z)))
    }

    fun scale(factor: Vec3): Solid {
        return Solid(csg.transformed(Transform().scale(factor.v3)))
    }

    operator fun plus(other: Solid): Solid {
        return Solid(csg.union(other.csg))
    }

    operator fun minus(other: Solid): Solid {
        return Solid(csg.union(other.csg))
    }

    fun intersection(other: Solid): Solid {
        return Solid(csg.intersect(other.csg))
    }

    fun hull(): Solid {
        return Solid(csg.hull())
    }

    fun volume(): Double {
        return csg.computeVolume()
    }
}

object SolidValueType: ValueType() {
    override val name: String = "Solid"

    override val asType: Type = Type.simple(name)

    override fun isTruthy(v: Value): Boolean {
        return true
    }
3
    override val providesFunctions: List<PrimitiveFunctionValue>
        get() = TODO("Not yet implemented")

    override val providesPrimitiveMethods: List<PrimitiveMethod>
        get() = TODO("Not yet implemented")
    override val providesVariables: Map<String, Value>
        get() = TODO("Not yet implemented")

    override fun assertIs(v: Value): Value {
        TODO("Not yet implemented")
    }

}
