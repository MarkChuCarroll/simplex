package org.goodmath.simplex.runtime.values.csg

import eu.mihosoft.jcsg.Extrude
import eu.mihosoft.jcsg.Primitive
import eu.mihosoft.jcsg.ext.org.poly2tri.PolygonUtil
import eu.mihosoft.vvecmath.Plane
import eu.mihosoft.vvecmath.Transform
import org.goodmath.simplex.ast.expr.LiteralExpr
import org.goodmath.simplex.ast.types.KwParameter
import org.goodmath.simplex.ast.types.Parameter
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.ParameterSignature
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.BooleanValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.runtime.values.primitives.Vec3
import org.goodmath.simplex.runtime.values.primitives.Vec3ValueType
import org.goodmath.simplex.runtime.values.primitives.VectorValueType
import org.goodmath.simplex.twist.Twist
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import eu.mihosoft.jcsg.Polygon as JPolygon

class Polygon(val poly: JPolygon): Value  {
    constructor(points: List<Vec3>): this(
        JPolygon.fromPoints(points.map { it.v3 })
    )

    override val valueType: ValueType = PolygonValueType

    override fun twist(): Twist =
        Twist.obj("Polygon",
            Twist.array("points", poly.vertices.map { Vec3(it.pos) }))

    fun move(x: Double, y: Double, z: Double): Polygon {
        return Polygon(poly.transformed(Transform().translate(x, y, z)))
    }

    fun move(v: Vec3): Polygon {
        return Polygon(poly.transformed(Transform().translate(v.v3)))
    }

    fun rotate(x: Double, y: Double, z: Double): Polygon {
        return Polygon(poly.transformed(Transform().rot(x, y, z)))
    }

    fun rotate(v: Vec3): Polygon {
        return Polygon(poly.transformed(Transform().rot(v.v3)))
    }

    fun scale(x: Double, y: Double, z: Double): Polygon {
        return Polygon(poly.transformed(Transform().scale(x, y, z)))
    }

    fun scale(v: Vec3): Polygon {
        return Polygon(poly.transformed(Transform().scale(v.v3)))
    }

    fun mirror(point: Vec3, normal: Vec3): Polygon {
        return Polygon(
            poly.transformed(
                Transform()
                    .mirror(Plane.fromPointAndNormal(point.v3, normal.v3))
            )
        )
    }

    fun extrude(dir: Vec3, top: Boolean = true, bottom: Boolean = true): Solid {
        return Solid(Extrude.points(dir.v3,
            poly.vertices.map { it.pos }))
    }

    fun join(other: Polygon): Solid {
        return Solid(Extrude.combine(poly, other.poly))
    }

    fun centroid(): Vec3 {
        return Vec3(poly.centroid())
    }

    fun bounds(): BoundingBox {
        return BoundingBox(poly.bounds)
    }

    companion object {
        fun circle(radius: Double, segments: Int = 16): Polygon {
            val points = ArrayList<Vec3>()
            // Arc size to do a CCW sweep over the circle.
            val arc = -2 * PI / (1.0 / segments.toDouble())
            for (i in 0 until segments) {
                val theta = arc * i.toDouble()
                val x = radius * cos(theta)
                val y = radius * sin(theta)
                points.add(Vec3(x, y, 0.0))
            }
            return Polygon(points)
        }

        fun square(edge: Double): Polygon {
            val len = edge / 2.0
            return Polygon(listOf(Vec3(len, len, 0.0), Vec3(-len, len, 0.0 ),
                Vec3(-len, -len, 0.0), Vec3(len, -len, 0.0)))
        }

        fun rect(width: Double, height: Double): Polygon {
            val w = width/2.0
            val h = height/2.0
            return Polygon(listOf(
                Vec3(w, h, 0.0),
                Vec3(-w, h, 0.0),
                Vec3(-w, -h, 0.0),
                Vec3(w, -h, 0.0),
            ))
        }
    }
}

object PolygonValueType: ValueType() {
    override val name: String = "Polygon"

    override val asType: Type by lazy {
        Type.PolygonType
    }

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(object : PrimitiveFunctionValue(
                "polygon",
                FunctionSignature.simple(
                    ParameterSignature(listOf(Parameter("points", VectorValueType(Vec3ValueType).asType))),
                    PolygonValueType.asType
                )
            ) {
                override fun execute(
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                ): Value {
                    val points = VectorValueType(Vec3ValueType).assertIs(args[0]).elements as List<Vec3>
                    return Polygon(points)
                }
            }, object : PrimitiveFunctionValue(
                "circle",
                FunctionSignature.simple(
                    ParameterSignature(
                        listOf(Parameter("radius", FloatValueType.asType)),
                        listOf(KwParameter("segments", IntegerValueType.asType, IntegerValue(16)))
                    ),
                    PolygonValueType.asType
                )
            ) {
                override fun execute(
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                ): Value {
                    val radius = assertIsFloat(args[0])
                    val segments = assertIsInt(kwArgs["segments"] ?: IntegerValue(16))
                    return Polygon.circle(radius, segments)
                }
            },
            object : PrimitiveFunctionValue(
                "square",
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
                return Polygon.square(size)
            }
        },
            object : PrimitiveFunctionValue(
                "rect",
                FunctionSignature.simple(
                    ParameterSignature(
                        listOf(
                            Parameter("width", FloatValueType.asType),
                            Parameter("height", FloatValueType.asType)
                        )
                    ),
                    asType
                )
            ) {
                override fun execute(
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                ): Value {
                    val width = assertIsFloat(args[0])
                    val height = assertIsFloat(args[1])
                    return Polygon.rect(width, height)
                }
            },
        )
    }

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object: PrimitiveMethod("extrude",
                MethodSignature.simple(asType,
                    ParameterSignature(listOf(Parameter("dir", Vec3ValueType.asType)),
                        listOf(KwParameter("top", BooleanValueType.asType, BooleanValue(true)),
                            KwParameter("bottom", BooleanValueType.asType, BooleanValue(true)))),
                    SolidValueType.asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    val dir = Vec3ValueType.assertIs(args[0])
                    val top = kwArgs["top"]?.let { assertIsBoolean(it) } != false
                    val bottom = kwArgs["bottom"]?.let { assertIsBoolean(it) } != false
                    return self.extrude(dir, top, bottom)
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
                        ParameterSignature(
                            listOf(Parameter("dist", Vec3ValueType.asType)))),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    if (args.size == 1) {
                        val dist = Vec3ValueType.assertIs(args[0])
                        return self.move(dist)
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        val z = assertIsFloat(args[2])
                        return self.move(x, y, z)
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
                        ParameterSignature(
                            listOf(Parameter("dist", Vec3ValueType.asType)))),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    if (args.size == 1) {
                        val dist = Vec3ValueType.assertIs(args[0])
                        return self.rotate(dist)
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        val z = assertIsFloat(args[2])
                        return self.rotate(x, y, z)
                    }
                }
            },
            object: PrimitiveMethod("scale",
                MethodSignature.multi(asType,
                    listOf(
                        ParameterSignature(
                            listOf(
                                Parameter("x", FloatValueType.asType),
                                Parameter("y", FloatValueType.asType),
                                Parameter("z", FloatValueType.asType))),
                        ParameterSignature(
                            listOf(Parameter("dist", Vec3ValueType.asType)))),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    if (args.size == 1) {
                        val dist = Vec3ValueType.assertIs(args[0])
                        return self.scale(dist)
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        val z = assertIsFloat(args[2])
                        return self.scale(x, y, z)
                    }
                }
            },
            object: PrimitiveMethod("join",
                MethodSignature.simple(asType,
                    ParameterSignature(listOf(Parameter("other", asType))),
                    SolidValueType.asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return self.join(other)
                }
            },
            object: PrimitiveMethod("bounds",
                MethodSignature.simple(asType,
                    ParameterSignature(emptyList()),
                    BoundingBoxValueType.asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    return self.bounds()
                }
            },
            object: PrimitiveMethod("mirror",
                MethodSignature.simple(asType,
                    ParameterSignature(listOf(Parameter("offset", Vec3ValueType.asType),
                        Parameter("normal", Vec3ValueType.asType))),
                    PolygonValueType.asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    val offset = Vec3ValueType.assertIs(args[0])
                    val normal = Vec3ValueType.assertIs(args[1])
                    return self.mirror(offset, normal)
                }
            },
            object: PrimitiveMethod("centroid",
                MethodSignature.simple(asType,
                    ParameterSignature(emptyList()),
                    PolygonValueType.asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    kwArgs: Map<String, Value>,
                    env: Env,
                ): Value {
                    val self = assertIs(target)
                    return self.centroid()
                }
            })
    }

    override val providesVariables: Map<String, Value> by lazy {
        emptyMap()
    }

    override fun assertIs(v: Value): Polygon {
        if (v is Polygon) {
            return v
        } else {
            throwTypeError(v)
        }
    }
}
