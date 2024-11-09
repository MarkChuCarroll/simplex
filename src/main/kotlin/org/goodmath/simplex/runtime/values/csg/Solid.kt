package org.goodmath.simplex.runtime.values.csg

import eu.mihosoft.jcsg.CSG
import eu.mihosoft.jcsg.Cube
import eu.mihosoft.jcsg.Cylinder
import eu.mihosoft.jcsg.Primitive
import eu.mihosoft.jcsg.RoundedCube
import eu.mihosoft.jcsg.Sphere
import eu.mihosoft.vvecmath.Plane
import eu.mihosoft.vvecmath.Transform
import org.goodmath.simplex.ast.types.KwParameter
import org.goodmath.simplex.ast.types.Parameter

import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.ParameterSignature
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.csg.Solid.Companion.cylinder
import org.goodmath.simplex.runtime.values.primitives.BooleanValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.runtime.values.primitives.Vec3
import org.goodmath.simplex.runtime.values.primitives.Vec3ValueType
import org.goodmath.simplex.twist.Twist

class Solid(val csg: CSG): Value {
    override val valueType: ValueType = SolidValueType


    override fun twist(): Twist {
        return Twist.obj("CSG")
    }

    fun bounds(): BoundingBox {
        return BoundingBox(csg.bounds)
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
        return Solid(csg.difference(other.csg))
    }

    fun intersection(other: Solid): Solid {
        return Solid(csg.intersect(other.csg))
    }

    fun hull(): Solid {
        return Solid(csg.hull())
    }

    fun mirror(point: Vec3, normal: Vec3): Solid {
        return Solid(csg.transformed(Transform().mirror(Plane.fromPointAndNormal(point.v3, normal.v3))))
    }

    companion object {
        fun sphere(radius: Double): Solid {
            return Solid(Sphere().toCSG().transformed(Transform().scale(radius)))
        }

        fun spheroid(x: Double, y: Double, z: Double): Solid {
            return Solid(Sphere().toCSG().transformed(Transform().scale(x, y, z)))
        }

        fun spheroid(v: Vec3): Solid {
            return spheroid(v.x, v.y, v.z)
        }

        fun cube(size: Double): Solid {
            return Solid(Cube().toCSG().transformed(Transform().scale(size)))
        }

        fun cuboid(x: Double, y: Double, z: Double): Solid {
            return Solid(Cube().toCSG().transformed(Transform().scale(x, y, z)))
        }

        fun cuboid(v: Vec3): Solid {
            return cuboid(v.x, v.y, v.z)
        }

        fun cylinder(height: Double, radLow: Double, radHigh: Double=radLow, slices: Int = 16): Solid {
            return Solid(Cylinder(radLow, radHigh, height, slices).toCSG())
        }

        fun roundedCube(length: Double, width: Double, height: Double, radius: Double=0.1): Solid {
            val c = RoundedCube(length, width, height)
            c.cornerRadius = radius
            return Solid(c.toCSG())
        }
    }
}

object SolidValueType: ValueType() {
    override val name: String = "Solid"

    override val asType: Type = Type.simple(name)

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object : PrimitiveFunctionValue(
                "sphere",
                FunctionSignature.simple(
                    ParameterSignature(listOf(Parameter("radius", FloatValueType.asType))),
                    asType
                )
            ) {
                override fun execute(
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                ): Value {
                    val radius = assertIsFloat(args[0])
                    return Solid.sphere(radius)
                }
            },
            object : PrimitiveFunctionValue(
                "spheroid",
                FunctionSignature.multi(
                    listOf(
                        ParameterSignature(
                            listOf(
                                Parameter("xRadius", FloatValueType.asType),
                                Parameter("yRadius", FloatValueType.asType),
                                Parameter("zRadius", FloatValueType.asType)
                            )
                        ),
                        ParameterSignature(
                            listOf(
                                Parameter("v", Vec3ValueType.asType)
                            )
                        )),
                    asType
                )
            ) {
                override fun execute(
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                ): Value {
                    if (args.size == 1) {
                        val v = Vec3ValueType.assertIs(args[0])
                        return Solid.spheroid(v)
                    } else {
                        val xRadius = assertIsFloat(args[0])
                        val yRadius = assertIsFloat(args[1])
                        val zRadius = assertIsFloat(args[2])
                        return Solid.spheroid(xRadius, yRadius, zRadius)
                    }
                }
            },
            object : PrimitiveFunctionValue(
                "cube",
                FunctionSignature.simple(
                    ParameterSignature(
                        listOf(
                            Parameter("size", FloatValueType.asType)
                        )
                    ),
                    asType
                )
            ) {
                override fun execute(
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                ): Value {
                    val size = assertIsFloat(args[0])
                    return Solid(Cube().toCSG().transformed(Transform().scale(size)))
                }
            },
            object : PrimitiveFunctionValue(
                "cuboid",
                FunctionSignature.simple(
                        ParameterSignature(
                            listOf(
                                Parameter("length", FloatValueType.asType),
                                Parameter("width", FloatValueType.asType),
                                Parameter("depth", FloatValueType.asType))),
                    asType))  {
                override fun execute(
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                ): Value {
                    val length = assertIsFloat(args[0])
                    val width = assertIsFloat(args[1])
                    val depth = assertIsFloat(args[2])
                    return Solid(Cube().toCSG().transformed(Transform().scale(length, width, depth)))
                }
            },
            object : PrimitiveFunctionValue("rounded_cube",
                FunctionSignature.simple(
                    ParameterSignature(
                        listOf(
                            Parameter("length", FloatValueType.asType),
                            Parameter("width", FloatValueType.asType),
                            Parameter("depth", FloatValueType.asType),
                            Parameter("radius", FloatValueType.asType))),
                    asType)) {
                override fun execute(
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                ): Value {
                    val length = assertIsFloat(args[0])
                    val width = assertIsFloat(args[1])
                    val depth = assertIsFloat(args[2])
                    val radius = assertIsFloat(args[3])
                    return Solid.roundedCube(length, width, depth, radius)
                }
            },
            object: PrimitiveFunctionValue("cylinder",
                FunctionSignature.simple(
                    ParameterSignature(
                        listOf(
                            Parameter("height", FloatValueType.asType),
                            Parameter("low", FloatValueType.asType),
                            Parameter("high", FloatValueType.asType)),
                        listOf(KwParameter("slices", IntegerValueType.asType, IntegerValue(16)))),
                    asType)) {
                override fun execute(
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                ): Value {
                    val height = assertIsFloat(args[0])
                    val low = assertIsFloat(args[1])
                    val high = assertIsFloat(args[2])

                    val slices = assertIsInt(kwArgs["slices"] ?: IntegerValue(16))
                    return cylinder(low, high, height, slices)
                }
            })
    }


    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object: PrimitiveMethod("scale",
                MethodSignature.multi(asType,
                    listOf(
                        ParameterSignature(
                            listOf(
                                Parameter("x", FloatValueType.asType),
                                Parameter("y", FloatValueType.asType),
                                Parameter("z", FloatValueType.asType))),
                        ParameterSignature(listOf(Parameter("v", Vec3ValueType.asType)))),
                        asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    if (args.size == 1) {
                        val v = Vec3ValueType.assertIs(args[0])
                        return self.scale(v)
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        val z = assertIsFloat(args[2])
                        return self.scale(x, y, z)
                    }
                }
                },
                    object: PrimitiveMethod("rotate",
                        MethodSignature.multi(asType,
                            listOf(
                                ParameterSignature(
                                    listOf(
                                        Parameter("x", FloatValueType.asType),
                                        Parameter("y", FloatValueType.asType),
                                        Parameter("z", FloatValueType.asType))),
                                ParameterSignature(listOf(Parameter("v", Vec3ValueType.asType)))),
                            asType)) {
                        override fun execute(
                            target: Value,
                            args: List<Value>,
                            kwArgs: Map<String, Value>,
                            env: Env,
                        ): Value {
                            val self = assertIs(target)
                            if (args.size == 1) {
                                val v = Vec3ValueType.assertIs(args[0])
                                return self.rotate(v.x, v.y, v.z)
                            } else {
                                val x = assertIsFloat(args[0])
                                val y = assertIsFloat(args[1])
                                val z = assertIsFloat(args[2])
                                return self.rotate(x, y, z)
                            }
                        }
                    },
                        object: PrimitiveMethod("move",
                            MethodSignature.multi(asType,
                                listOf(
                                    ParameterSignature(
                                        listOf(
                                            Parameter("x", FloatValueType.asType),
                                            Parameter("y", FloatValueType.asType),
                                            Parameter("z", FloatValueType.asType))),
                                    ParameterSignature(listOf(Parameter("v", Vec3ValueType.asType)))),
                                asType)) {
                            override fun execute(
                                target: Value,
                                args: List<Value>,
                                kwArgs: Map<String, Value>,
                                env: Env,
                            ): Value {
                                val self = assertIs(target)
                                if (args.size == 1) {
                                    val v = Vec3ValueType.assertIs(args[0])
                                    return self.move(v)
                                } else {
                                    val x = assertIsFloat(args[0])
                                    val y = assertIsFloat(args[1])
                                    val z = assertIsFloat(args[2])
                                    return self.move(x, y, z)
                                }
                            }
                        },
            object: PrimitiveMethod("plus",
                MethodSignature.simple(asType,
                    ParameterSignature(listOf(Parameter("other", asType))),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return self + other
                }
            },
            object: PrimitiveMethod("minus",
                MethodSignature.simple(asType,
                    ParameterSignature(listOf(Parameter("other", asType))),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return self - other
                }
            },
            object: PrimitiveMethod("intersect",
                MethodSignature.simple(asType,
                    ParameterSignature(listOf(Parameter("other", asType))),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return self.intersection(other)
                }
            },
            object: PrimitiveMethod("hull",
                MethodSignature.simple(asType,
                    ParameterSignature(emptyList()),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    return self.hull()
                }
            },
            object: PrimitiveMethod("bounds",
                MethodSignature.simple(asType,
                    ParameterSignature(emptyList()),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    return BoundingBox(self.csg.bounds)
                }
            },
            object: PrimitiveMethod("mirror",
                MethodSignature.simple(asType,
                    ParameterSignature(
                        listOf(
                            Parameter("point", Vec3ValueType.asType),
                            Parameter("normal", Vec3ValueType.asType))),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    val point = Vec3ValueType.assertIs(args[0])
                    val norm = Vec3ValueType.assertIs(args[1])
                    return self.mirror(point, norm)
                }
            },
        )
    }


    override val providesVariables: Map<String, Value> = emptyMap()



    override fun assertIs(v: Value): Solid {
        if (v is Solid) {
            return v
        } else {
            throwTypeError(v)
        }
    }

}
