package org.goodmath.simplex.runtime.values.manifold

import manifold3d.pub.Smoothness
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.twist.Twist

class SSmoothness(var smoothness: Float, var halfEdge: Int): Value {
    override val valueType: ValueType = SSmoothnessType

    fun toSmoothness(): Smoothness {
        val result = Smoothness()
        result.smoothness(smoothness)
        result.halfedge(halfEdge)
        return result
    }

    override fun twist(): Twist =
        Twist.obj("Smoothness",
            Twist.attr("smoothness", smoothness.toString()),
            Twist.attr("halfEdge", halfEdge.toString()))

    companion object {
        fun fromSmoothness(s: Smoothness): SSmoothness =
            SSmoothness(s.smoothness(), s.halfedge())
    }

}

object SSmoothnessType: ValueType() {
    override val name: String = "Smoothness"

    override val asType: Type by lazy { Type.simple("Smoothness") }

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object: PrimitiveFunctionValue("smoothness",
            FunctionSignature.simple(listOf(Param("smoothness", FloatValueType.asType),
                Param("halfEdge", IntegerValueType.asType)),
                asType)
            ) {
                override fun execute(args: List<Value>): Value {
                    val smoothness = assertIsFloat(args[0])
                    val halfEdge = assertIsInt(args[1])
                    return SSmoothness(smoothness.toFloat(), halfEdge)
                }
            })
    }

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object: PrimitiveMethod("set_smoothness",
                MethodSignature.simple(asType, listOf(Param("smoothness", FloatValueType.asType)),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIs(target)
                    val sm = assertIsFloat(args[0]).toFloat()
                    self.smoothness = sm
                    return self
                }
            },
            object: PrimitiveMethod("set_halfedge",
                MethodSignature.simple(asType, listOf(Param("halfedge", IntegerValueType.asType)),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIs(target)
                    val he = assertIsInt(args[0])
                    self.halfEdge = he
                    return self
                }
            },
            object: PrimitiveMethod("set_smoothness",
                MethodSignature.simple(asType, emptyList<Param>(),
                    FloatValueType.asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIs(target)
                    return FloatValue(self.smoothness.toDouble())
                }
            },
            object: PrimitiveMethod("halfedge",
                MethodSignature.simple(asType, emptyList<Param>(),
                    FloatValueType.asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIs(target)
                    return FloatValue(self.halfEdge.toDouble())
                }
            },
        )
    }

    override val providesVariables: Map<String, Value> = emptyMap()

    override fun assertIs(v: Value): SSmoothness {
        return v as? SSmoothness ?: throwTypeError(v)
    }

}
