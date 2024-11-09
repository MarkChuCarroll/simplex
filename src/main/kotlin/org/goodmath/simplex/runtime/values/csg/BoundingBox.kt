package org.goodmath.simplex.runtime.values.csg

import eu.mihosoft.jcsg.Bounds
import eu.mihosoft.vvecmath.Vector3d
import org.goodmath.simplex.ast.types.Parameter
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.ParameterSignature
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.runtime.values.primitives.Vec3
import org.goodmath.simplex.runtime.values.primitives.Vec3ValueType
import org.goodmath.simplex.twist.Twist

class BoundingBox(val bb: Bounds): Value {
    constructor(min: Vector3d, max: Vector3d): this(Bounds(min, max))

    val center: Vec3 by lazy { Vec3(bb.center) }
    val max: Vec3
        get() = Vec3(bb.max)

    val min: Vec3
        get() = Vec3(bb.min)

    val size: Vec3
        get() = Vec3(bb.bounds)

    fun containsPoint(pt: Vector3d): Boolean {
        return bb.contains(pt)
    }

    fun containsPolygon(p: Polygon): Boolean {
        return bb.contains(p.poly)
    }

    override val valueType: ValueType =
        BoundingBoxValueType

    override fun twist(): Twist =
        Twist.obj("BoundingBox",
            Twist.attr("min", min.toString()),
            Twist.attr("max", max.toString()))
}

object BoundingBoxValueType: ValueType() {
    override val name: String = "BoundingBox"

    override val asType: Type by lazy { Type.simple(name) }

    override val supportsText = true

    override fun toText(v: Value): String {
        val bb = assertIs(v)
        return "[min=${bb.min}, max=${bb.max}]"
    }

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object: PrimitiveFunctionValue(
                "bounds",
                FunctionSignature.simple(
                    ParameterSignature(listOf(Parameter("min", Vec3ValueType.asType),
                                                Parameter("max", Vec3ValueType.asType)),
                        emptyList()),
                    asType)) {
                override fun execute(
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                ): Value {
                    val min = Vec3ValueType.assertIs(args[0]).v3
                    val max = Vec3ValueType.assertIs(args[1]).v3
                    return BoundingBox(min, max)
                }
            })

    }

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object: PrimitiveMethod("min",
                MethodSignature.simple(
                    asType,
                    ParameterSignature(
                        emptyList(), emptyList()),
                    Vec3ValueType.asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    return self.min
                }
            },
            object: PrimitiveMethod("max",
                MethodSignature.simple(
                    asType,
                    ParameterSignature(
                        emptyList(), emptyList()),
                    Vec3ValueType.asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    return self.max
                }
            },
            object: PrimitiveMethod("center",
                MethodSignature.simple(
                    asType,
                    ParameterSignature(
                        emptyList(), emptyList()),
                    Vec3ValueType.asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    return self.center
                }
            },
            object: PrimitiveMethod("size",
                MethodSignature.simple(
                    asType,
                    ParameterSignature(
                        emptyList(), emptyList()),
                    Vec3ValueType.asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    return self.size
                }
            },
            object: PrimitiveMethod("containsPoint",
                MethodSignature.simple(
                    asType,
                    ParameterSignature(
                        listOf(Parameter("point", Vec3ValueType.asType)), emptyList()),
                    Vec3ValueType.asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    val pt = Vec3ValueType.assertIs(args[0]).v3
                    return BooleanValue(self.containsPoint(pt))
                }
            },
            object: PrimitiveMethod("move",
                MethodSignature.multi(
                    asType,
                    listOf(ParameterSignature(
                        listOf(Parameter("offset", Vec3ValueType.asType))),
                        ParameterSignature(listOf(Parameter("x", FloatValueType.asType),
                            Parameter("y", FloatValueType.asType),
                            Parameter("z", FloatValueType.asType)))),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    if (args.size == 1) {
                        val offset = Vec3ValueType.assertIs(args[0]).v3
                        return BoundingBox(self.bb.min.plus(offset), self.bb.max.plus(offset))
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        val z = assertIsFloat(args[2])
                        val offset = Vec3(x, y, z)
                        return BoundingBox(self.bb.min.plus(offset.v3), self.bb.max.plus(offset.v3))
                    }
                }
            })
    }

    override val providesVariables: Map<String, Value> = emptyMap()

    override fun assertIs(v: Value): BoundingBox {
        if (v is BoundingBox) {
            return v
        } else {
            throwTypeError(v)
        }
    }
}
