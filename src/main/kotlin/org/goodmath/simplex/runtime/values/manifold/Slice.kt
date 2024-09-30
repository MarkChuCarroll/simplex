/*
 * Copyright 2024 Mark C. Chu-Carroll
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.goodmath.simplex.runtime.values.manifold

import manifold3d.Manifold
import manifold3d.glm.DoubleVec2
import manifold3d.manifold.CrossSection
import manifold3d.manifold.CrossSectionVector
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.ArrayValue
import org.goodmath.simplex.runtime.values.primitives.ArrayValueType
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.runtime.values.primitives.Vec2
import org.goodmath.simplex.runtime.values.primitives.Vec2Type
import org.goodmath.simplex.twist.Twist

/** The Simplex wrapper for the Manifold CrossSection type. */
class Slice(val cross: CrossSection) : Value {
    override val valueType: ValueType = SliceType

    override fun twist(): Twist =
        Twist.obj(
            "CrossSection",
            Twist.attr("area", cross.area().toString()),
            Twist.attr("verts", cross.numVert().toString()),
            Twist.attr("contours", cross.numContour().toString()),
        )

    val area = FloatValue(cross.area())

    val numVert = IntegerValue(cross.numVert())

    val contours = IntegerValue(cross.numContour())

    fun isEmpty(): BooleanValue = BooleanValue(cross.isEmpty)

    val bounds = BoundingRect(cross.bounds())

    fun translate(x: Double, y: Double): Slice = Slice(cross.translate(x, y))

    fun translate(v: Vec2): Slice = translate(v.x, v.y)

    fun rotate(angle: Double): Slice = Slice(cross.rotate(angle.toFloat()))

    fun scale(x: Double, y: Double): Slice = Slice(cross.scale(DoubleVec2(x, y)))

    fun scale(v: Vec2): Slice = Slice(cross.scale(v.toDoubleVec2()))

    fun mirror(norm: Vec2): Slice = Slice(cross.mirror(norm.toDoubleVec2()))

    fun simplify(epsilon: Double): Slice = Slice(cross.simplify(epsilon))

    fun offset(offset: Double, join: Int, segments: Int, miter: Double): Slice =
        Slice(cross.offset(offset, join, miter, segments))

    fun convexHull(): Slice = Slice(cross.convexHull())

    operator fun plus(other: Slice): Slice = Slice(cross.add(other.cross))

    operator fun minus(other: Slice): Slice =
        Slice(cross.subtract(other.cross))

    fun intersect(other: Slice): Slice = Slice(cross.intersect(other.cross))

    fun toPolygons(): ArrayValue = ArrayValue(SPolygonType, cross.toPolygons().map { SPolygon(it) })

    fun decompose(): ArrayValue {
        val css = cross.decompose()
        return ArrayValue(SliceType, css.map { Slice(it) })
    }

    fun extrude(height: Double, steps: Int, scale: Vec2, twist: Double): Solid {
        return Solid(
            Manifold.Extrude(cross, height.toFloat(), steps, twist.toFloat(), scale.toDoubleVec2())
        )
    }

    fun revolve(segments: Int, degrees: Float): Solid =
        Solid(Manifold.Revolve(cross, segments, degrees))

    companion object {
        fun rectangle(x: Double, y: Double): Slice {
            return Slice(CrossSection.Square(x, y))
        }

        fun circle(x: Double, facets: Int): Slice =
            Slice(CrossSection.Circle(x.toFloat(), facets))

        fun oval(x: Double, y: Double, facets: Int): Slice {
            return Slice(CrossSection.Circle(x.toFloat(), facets).scale(
                Vec2(1.0,  y/x).toDoubleVec2()))
        }
    }
}

object SliceType : ValueType() {
    override val name: String = "Slice"

    override val asType: Type = Type.simple(name)

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesVariables: Map<String, Value> by lazy {
        hashMapOf(
            "JoinSquare" to IntegerValue(CrossSection.JoinType.Square.ordinal),
            "JoinRound" to IntegerValue(CrossSection.JoinType.Round.ordinal),
            "JoinMiter" to IntegerValue(CrossSection.JoinType.Miter.ordinal),
            "FillEvenOdd" to IntegerValue(CrossSection.FillRule.EvenOdd.ordinal),
            "FillNonZero" to IntegerValue(CrossSection.FillRule.NonZero.ordinal),
            "FillPositive" to IntegerValue(CrossSection.FillRule.Positive.ordinal),
            "FillNegative" to IntegerValue(CrossSection.FillRule.Negative.ordinal),
        )
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object: PrimitiveFunctionValue("circle",
                FunctionSignature.multi(
                    listOf(
                        listOf(Param("radius", Type.FloatType)),
                        listOf(Param("radius", Type.FloatType), Param("facets", Type.IntType))),
                    Type.SliceType)) {
                override fun execute(args: List<Value>): Value {
                    val radius = assertIsFloat(args[0])
                    val facets = if (args.size > 1) {
                        assertIsInt(args[1])
                    } else {
                        0
                    }
                    return Slice.circle(radius, facets)
                }
            },
            object: PrimitiveFunctionValue("oval",
                FunctionSignature.multi(
                    listOf(
                        listOf(
                            Param("x", Type.FloatType),
                            Param("y", Type.FloatType),
                        ),
                        listOf(
                            Param("x", Type.FloatType),
                            Param("y", Type.FloatType),
                            Param("facets", Type.IntType))
                        ),
                    Type.SliceType)) {
                override fun execute(args: List<Value>): Value {
                    val x = assertIsFloat(args[0])
                    val y = assertIsFloat(args[1])
                    val facets = if (args.size > 2) {
                        assertIsInt(args[2])
                    } else {
                        0
                    }
                    return Slice.oval(x, y, facets)
                }
            },
            object: PrimitiveFunctionValue("rectangle",
                FunctionSignature.simple(
                        listOf(Param("x", Type.FloatType),
                            Param("y", Type.FloatType)),
                    Type.SliceType)) {
                override fun execute(args: List<Value>): Value {
                    val x = assertIsFloat(args[0])
                    val y = assertIsFloat(args[1])
                    return Slice.rectangle(x, y)
                }
            },
            object :
                PrimitiveFunctionValue(
                    "batch_hull",
                    FunctionSignature.simple(
                        listOf(Param("css", Type.array(asType))), asType),
                ) {
                override fun execute(args: List<Value>): Value {
                    val css =
                        ArrayList(
                            ArrayValueType.of(SliceType).assertIs(args[0]).elements.map {
                                assertIs(it).cross
                            }
                        )
                    return Slice(CrossSection.ConvexHull(CrossSectionVector(css)))
                }
            },
            object :
                PrimitiveFunctionValue(
                    "cross_section_compose",
                    FunctionSignature.simple(listOf(Param("css", Type.array(asType))), asType),
                ) {
                override fun execute(args: List<Value>): Value {
                    val css =
                        ArrayList(
                            ArrayValueType.of(SliceType).assertIs(args[0]).elements.map {
                                assertIs(it).cross
                            }
                        )
                    return Slice(CrossSection.Compose(CrossSectionVector(css)))
                }
            },
            object :
                PrimitiveFunctionValue(
                    "text_cross_section",
                    FunctionSignature.simple(
                        listOf(
                            Param("font_file", Type.StringType),
                            Param("text", Type.StringType),
                            Param("pixelHeight", Type.IntType),
                            Param("interpRes", Type.IntType),
                            Param("fillRule", Type.IntType),
                        ),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    return Slice(
                        CrossSection.Text(
                            assertIsString(args[0]),
                            assertIsString(args[1]),
                            assertIsInt(args[2]),
                            assertIsInt(args[3]),
                            assertIsInt(args[4]),
                        )
                    )
                }
            },
            object :
                PrimitiveFunctionValue(
                    "circle_cross_section",
                    FunctionSignature.simple(
                        listOf(
                            Param("height", Type.FloatType),
                            Param("circle_segmets", Type.IntType),
                        ),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    return Slice(
                        CrossSection.Circle(assertIsFloat(args[0]).toFloat(), assertIsInt(args[1]))
                    )
                }
            },
            object :
                PrimitiveFunctionValue(
                    "square_cross_section",
                    FunctionSignature.simple(
                        listOf(
                            Param("width", Type.FloatType),
                            Param("height", Type.FloatType),
                            Param("center", Type.BooleanType),
                        ),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    return Slice(
                        CrossSection.Square(
                            assertIsFloat(args[0]),
                            assertIsFloat(args[1]),
                            assertIsBoolean(args[2]),
                        )
                    )
                }
            },
        )
    }

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object :
                PrimitiveMethod(
                    "area",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.FloatType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.area
                }
            },
            object :
                PrimitiveMethod(
                    "num_vert",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.IntType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.numVert
                }
            },
            object :
                PrimitiveMethod(
                    "contours",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.FloatType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.contours
                }
            },
            object :
                PrimitiveMethod(
                    "is_empty",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.BooleanType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.isEmpty()
                }
            },
            object :
                PrimitiveMethod(
                    "bounds",
                    MethodSignature.simple(asType, emptyList<Param>(),
                        Type.BoundingRectType)
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.bounds
                }
            },
            object :
                PrimitiveMethod(
                    "translate",
                    MethodSignature.multi(
                        asType,
                        listOf(
                            listOf(Param("x", Type.FloatType), Param("y", Type.FloatType)),
                            listOf(Param("v", Vec2Type.asType)),
                        ),
                        asType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return if (args.size == 1) {
                        val v = Vec2Type.assertIs(args[0])
                        self.translate(v)
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        self.translate(x, y)
                    }
                }
            },
            object :
                PrimitiveMethod(
                    "rotate",
                    MethodSignature.simple(asType, listOf(Param("angle", Type.FloatType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val angle = assertIsFloat(args[0])
                    return self.rotate(angle)
                }
            },
            object :
                PrimitiveMethod(
                    "scale",
                    MethodSignature.multi(
                        asType,
                        listOf(
                            listOf(Param("x", Type.FloatType), Param("y", Type.FloatType)),
                            listOf(Param("factor", Vec2Type.asType)),
                        ),
                        asType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    if (args.size == 1) {
                        return self.scale(Vec2Type.assertIs(args[0]))
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        return self.scale(x, y)
                    }
                }
            },
            object :
                PrimitiveMethod(
                    "mirror",
                    MethodSignature.simple(asType, listOf(Param("norm", Vec2Type.asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val norm = Vec2Type.assertIs(args[0])
                    return self.mirror(norm)
                }
            },
            object :
                PrimitiveMethod(
                    "simplify",
                    MethodSignature.simple(asType, listOf(Param("epsilon", Type.FloatType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val epsilon = assertIsFloat(args[0])
                    return self.simplify(epsilon)
                }
            },
            object :
                PrimitiveMethod(
                    "offset",
                    MethodSignature.multi(
                        asType,
                        listOf(
                            listOf(Param("offset", FloatValueType.asType)),
                            listOf(
                                Param("offset", FloatValueType.asType),
                                Param("join_type", IntegerValueType.asType),
                            ),
                            listOf(
                                Param("offset", Type.FloatType),
                                Param("join_type", Type.IntType),
                                Param("circle_segments", Type.IntType),
                            ),
                            listOf(
                                Param("offset", Type.FloatType),
                                Param("join_type", Type.IntType),
                                Param("circle_segments", Type.IntType),
                                Param("miter_limit", Type.FloatType),
                            ),
                        ),
                        asType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val offset = assertIsFloat(args[0])
                    val joinType =
                        if (args.size > 1) {
                            assertIsInt(args[1])
                        } else {
                            CrossSection.JoinType.Round.ordinal
                        }
                    val circleSegments =
                        if (args.size > 2) {
                            assertIsInt(args[2])
                        } else {
                            0
                        }
                    val miterLimit =
                        if (args.size > 3) {
                            assertIsFloat(args[3])
                        } else {
                            0.0
                        }
                    return self.offset(offset, joinType, circleSegments, miterLimit)
                }
            },
            object :
                PrimitiveMethod(
                    "convex_hull",
                    MethodSignature.simple(asType, emptyList<Param>(), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.convexHull()
                }
            },
            object :
                PrimitiveMethod(
                    "plus",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return self + other
                }
            },
            object :
                PrimitiveMethod(
                    "minus",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return self - other
                }
            },
            object :
                PrimitiveMethod(
                    "intersect",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return self.intersect(other)
                }
            },
            object :
                PrimitiveMethod(
                    "to_polygons",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.array(Type.PolygonType)),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.toPolygons()
                }
            },
            object :
                PrimitiveMethod(
                    "decompose",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.array(Type.SliceType)),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.decompose()
                }
            },
            object :
                PrimitiveMethod(
                    "extrude",
                    MethodSignature.multi(
                        SolidType.asType,
                        listOf(
                            listOf(Param("height", Type.FloatType), Param("steps", Type.IntType)),
                            listOf(Param("height", Type.FloatType), Param("steps", Type.IntType)),
                            listOf(
                                Param("height", Type.FloatType),
                                Param("steps", Type.IntType),
                                Param("scaleTop", Type.Vec2Type),
                            ),
                            listOf(
                                Param("height", Type.FloatType),
                                Param("n_divisions", Type.IntType),
                                Param("twist_degrees", Type.FloatType),
                                Param("scaleTop", Type.Vec2Type),
                            ),
                        ),
                        SolidType.asType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val height = assertIsFloat(args[0])
                    val steps =
                        if (args.size > 1) {
                            assertIsInt(args[1])
                        } else {
                            20
                        }
                    val scaleTop =
                        if (args.size > 2) {
                            Vec2Type.assertIs(args[3])
                        } else {
                            Vec2(1.0, 1.0)
                        }
                    val twist =
                        if (args.size > 3) {
                            assertIsFloat(args[2])
                        } else {
                            0.0
                        }
                    return self.extrude(height, steps, scaleTop, twist)
                }
            },
            object :
                PrimitiveMethod(
                    "revolve",
                    MethodSignature.multi(
                        asType,
                        listOf(
                            listOf(Param("segments", Type.IntType)),
                            listOf(
                                Param("segments", Type.IntType),
                                Param("degrees", Type.FloatType),
                            ),
                        ),
                        Type.SolidType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val segments = assertIsInt(args[0])
                    val degrees =
                        if (args.size == 2) {
                            assertIsFloat(args[1])
                        } else {
                            360.0
                        }
                    return self.revolve(segments, degrees.toFloat())
                }
            },
        )
    }

    override fun assertIs(v: Value): Slice {
        return if (v is Slice) {
            v
        } else {
            throwTypeError(v)
        }
    }
}