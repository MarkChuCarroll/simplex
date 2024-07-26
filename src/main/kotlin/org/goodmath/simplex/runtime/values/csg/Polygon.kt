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
package org.goodmath.simplex.runtime.values.csg

import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.csg.TwoDPointValueType
import org.goodmath.simplex.runtime.csg.TwoDPoint
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.primitives.ArrayValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.twist.Twist
import kotlin.collections.map
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


/*
 * This code is derived from the loop package in chalumier,
 * which in turn is largely a Kotlin Port of the Loop code
 * written by Paul Harrison for Demakein.
 */
const val QUALITY = 128

data class Limits2(val xMin: Double, val xMax: Double, val yMin: Double, val yMax: Double)

class Polygon(inputs: List<TwoDPoint>): Value {
    override val valueType: ValueType<Polygon> = PolygonValueType


    val points: ArrayList<TwoDPoint> = ArrayList(inputs)

    override fun twist(): Twist =
        Twist.obj("PolygonValue",
            Twist.array("points", points))


    fun len(): Int = points.size

    operator fun get(i: Int): TwoDPoint = points[i]

    operator fun set(i: Int, value: TwoDPoint) {
        points[i] = value
    }

    fun fromEnd(i: Int): TwoDPoint = points[points.size - i - 1]


    val circumference: Double by lazy {
        var total = 0.0
        var last = fromEnd(1)
        var dx: Double
        var dy: Double
        for (point in points) {
            dx = last.x - point.x
            dy = last.y - point.y
            total += sqrt(dx * dx + dy * dy)
            last = point
        }
        total
    }

    val area: Double by lazy {
        var total = 0.0
        var last = fromEnd(1)
        for (point in points) {
            total += (last.x * point.y - last.y * point.x)
            last = point
        }
        0.5 * total
    }

    val centroid: TwoDPoint by lazy {
        var sumX = 0.0
        var sumY = 0.0
        var div = 0.0
        var last = fromEnd(1)
        for (point in points) {
            val value = last.x * point.y - point.x * last.y
            div += value
            sumX += (last.x + point.x) * value
            sumY += (last.y + point.y) * value
            last = point
        }
        div *= 3.0
        if (div == 0.0) {
            // ph: # Probably a TwoDPoint, possibly a line, do something vaguely sensible
            div = points.size.toDouble()
            sumX = points.sumOf { point -> point.x }
            sumY = points.sumOf { point -> point.y }
        }
        TwoDPoint(sumX / div, sumY / div)
    }

    fun extent(): Limits2 {
        val xs =points.map { it.x }
        val ys =points.map { it.y }
        return Limits2(xs.min(), xs.max(), ys.min(), ys.max())
    }

    fun scale(factor: Double): Polygon {
        return Polygon(points.map { TwoDPoint(it.x * factor, it.y * factor) })
    }

    fun scale2(x: Double, y: Double): Polygon {
        return Polygon(points.map { TwoDPoint(it.x * x, it.y * y) })
    }

    fun withArea(area: Double): Polygon {
        return scale(sqrt(area / this.area))
    }

    fun withEffectiveDiameter(diameter: Double): Polygon {
        return withArea(PI * 0.25 * diameter * diameter)
    }

    fun withCircumference(circumference: Double): Polygon {
        return scale(circumference / this.circumference)
    }

    fun offset(dx: Double, dy: Double): Polygon {
        return Polygon(points.map { TwoDPoint(it.x + dx, it.y + dy) })
    }

    fun flipX(): Polygon {
        return Polygon(points.reversed().map { TwoDPoint(-it.x, it.y) })
    }

    fun flipY(): Polygon {
        return Polygon(points.reversed().map { TwoDPoint(it.x, -it.y) })
    }
}

object PolygonValueType: ValueType<Polygon>() {
    override val name: String = "Polygon"

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    fun assertIsPolygon(v: Value): Polygon {
        if (v is Polygon) {
            return v
        } else {
            throw SimplexTypeError(name, v.valueType.name)
        }
    }

    override fun add(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "addition")
    }

    override fun subtract(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "subtraction")
    }

    override fun mult(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "multiplication")
    }

    override fun div(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "division")
    }

    override fun mod(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value
    ): Boolean {
        val p1 = assertIsPolygon(v1)
        val p2 = assertIsPolygon(v2)
        return p1 == p2
    }

    override fun neg(v1: Value): Value {
        throw SimplexUnsupportedOperation(name, "negation")
    }

    override fun compare(v1: Value, v2: Value): Int {
        throw SimplexUnsupportedOperation(name, "ordering")
    }

    fun circle(diameter: Double = 1.0, n: Int = QUALITY): Polygon {
        val radius = diameter * 0.5
        return Polygon((0 until n).map { i ->
            val a = (i + 0.5) * PI * 2.0 / (n.toDouble())
            TwoDPoint(cos(a) * radius, sin(a) * radius)
        })
    }

    fun chordedCircle(amount: Double = 0.5): Polygon {
        // ph: semi-circle and the like
        val a1 = PI * (0.5 + amount)
        val a2 = PI * (2.5 - amount)
        return Polygon((0 until QUALITY).map { i ->
            val a = a1 + (i.toDouble() + 0.5) * (a2 - a1) / QUALITY.toDouble()
            TwoDPoint(cos(a), sin(a))
        })
    }

    fun squaredCircle(xPad: Double, yPad: Double, diameter: Double = 1.0): Polygon {
        // ph: Squared circle with same area as circle of specified diameter
        var result = (0 until QUALITY).map { i ->
            val a = (i.toDouble() + 0.5) * PI * 2.0 / (QUALITY.toDouble())
            var x = cos(a)
            if (x < 0) {
                x -= xPad * 0.5
            } else {
                x += xPad * 0.5
            }
            var y = sin(a)
            if (y < 0) {
                y -= yPad * 0.5
            } else {
                y += yPad * 0.5
            }
            Pair(x, y)
        }
        val area = PI + xPad * yPad + xPad * 2 + yPad * 2
        val want = PI * (diameter * 0.5).pow(2)
        val scale = sqrt(want / area)
        return Polygon(result.map { (x, y) -> TwoDPoint(x * scale, y * scale) })
    }

    fun rectangle(p0: TwoDPoint, p1: TwoDPoint): Polygon {
        return Polygon(
            listOf(
                p0, TwoDPoint(p1.x, p0.y), p1, TwoDPoint(p0.x, p1.y)
            )
        )
    }

    fun roundedRectangle(p0: TwoDPoint, p1: TwoDPoint, diameter: Double): Polygon {
        val radius = listOf(diameter, p1.x - p0.x, p1.y - p0.y).min() * 0.5
        val result = (0 until QUALITY).map { i ->
            val a = (i.toDouble() + 0.5) * PI * 2.0 / QUALITY.toDouble()
            var x = cos(a) * radius
            x += if (x < 0.0) {
                (p0.x + radius)
            } else {
                (p1.x - radius)
            }
            var y = sin(a) * radius
            y += if (y < 0) {
                p0.y + radius
            } else {
                p1.y - radius
            }
            TwoDPoint(x, y)
        }
        return Polygon(result)
    }

    fun halfRoundedRectangle(p0: TwoDPoint, p1: TwoDPoint): Polygon {
        val radius = p1.x - p0.x
        val result = (0 until QUALITY).map { i -> i.toDouble() }.map { i ->
            val a = ((i + 0.5) / QUALITY - 0.5) * PI
            val x = cos(a) * radius + p0.x
            var y = sin(a) * radius
            if (y < 0) y += (p0.y + radius) else y += (p1.y - radius)
            TwoDPoint(x, y)
        }.toMutableList()
        result.add(TwoDPoint(p0.x, p1.y))
        result.add(p0)
        return Polygon(result)
    }

    fun lens(amount: Double, circumference: Double = PI): Polygon {
        val turn = asin(amount)
        val turn2 = PI - turn * 2
        val shift = sin(turn)
        val result = (0 until QUALITY / 2).map { i -> i.toDouble() }.map { i ->
            val a = (i + 0.5) / QUALITY.toDouble() * 2 * turn2 + turn
            TwoDPoint(cos(a), sin(a) - shift)
        }.toMutableList()
        return Polygon(result + result.map { (x, y) -> TwoDPoint(-x, -y) }).withCircumference(circumference)
    }

    fun lens2(amount: Double, circumference: Double = PI): Polygon {
        val turn = PI * 0.5 * amount
        val turn2 = PI - turn * 2
        val shift = sin(turn)
        val result = (0 until QUALITY / 2).map { i -> i.toDouble() }.map { i ->
            val a = (i + 0.5) / QUALITY.toDouble() * 2 * turn2 + turn
            TwoDPoint(cos(a), sin(a) - shift)
        }.toMutableList()
        return Polygon(result + result.map { (x, y) -> TwoDPoint(-x, -y) }).withCircumference(circumference)
    }


    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object : PrimitiveFunctionValue("circle",
                FunctionSignature(listOf(Param("radius", FloatValueType)), PolygonValueType),
                FunctionSignature(listOf(Param("radius", FloatValueType),
                    Param("quality", IntegerValueType)),PolygonValueType)) {
                override fun execute(args: List<Value>): Value {
                    val radius = assertIsFloat(args[0])
                    val quality = if (args.size > 1) {
                        assertIsInt(args[1])
                    } else {
                        QUALITY
                    }
                    return circle(radius, quality)
                }
            },
            object : PrimitiveFunctionValue(
                "squared_circle",
                FunctionSignature(
                listOf(Param("diameter", FloatValueType),
                    Param("xPad", FloatValueType),
                    Param("yPad", FloatValueType)),
                PolygonValueType)
            ) {
                override fun execute(args: List<Value>): Value {
                    val diam = assertIsFloat(args[0])
                    val xPad = assertIsFloat(args[1])
                    val yPad = assertIsFloat(args[2])
                    return squaredCircle(xPad, yPad, diam)
                }
            },
            object : PrimitiveFunctionValue(
                "rectangle",
                FunctionSignature(
                    listOf(Param("center", TwoDPointValueType),
                        Param("width", FloatValueType),
                        Param("height", FloatValueType)),
                    PolygonValueType)) {
                override fun execute(args: List<Value>): Value {
                    val center = TwoDPointValueType.assertIs(args[0])
                    val width = assertIsFloat(args[1])
                    val height = assertIsFloat(args[2])
                    val ll = TwoDPoint(center.x - width / 2, center.y - height / 2)
                    val ur = TwoDPoint(center.x + width / 2, center.y + height / 2)
                    return rectangle(ll, ur)
                }
            })
    }


    override val providesOperations: List<PrimitiveMethod<Polygon>> by lazy {
        listOf(
            object : PrimitiveMethod<Polygon>("area",
                MethodSignature(PolygonValueType,
                    emptyList(), FloatValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                    val p = assertIsPolygon(target)
                    return FloatValue(p.area)
                }
            },
            object : PrimitiveMethod<Polygon>("centroid",
                MethodSignature(PolygonValueType, emptyList(), TwoDPointValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                    val p = assertIsPolygon(target)
                    return p.centroid
                }
            },
            object : PrimitiveMethod<Polygon>("scale",
                MethodSignature(PolygonValueType,
                    listOf(Param("factor", FloatValueType)), PolygonValueType),
                MethodSignature(PolygonValueType,
                    listOf(Param("xFactor", FloatValueType),
                        Param("yFactor", FloatValueType)),
                    PolygonValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                    val poly = assertIsPolygon(target)
                    if (args.size == 1) {
                        val factor = assertIsFloat(args[0])
                        return poly.scale(factor)
                    } else {
                        val xFactor = assertIsFloat(args[0])
                        val yFactor = assertIsFloat(args[1])
                        return poly.scale2(xFactor, yFactor)
                    }
                }
            },
            object : PrimitiveMethod<Polygon>(
                "with_area",
                MethodSignature(PolygonValueType,
                listOf(Param("area", FloatValueType)),
                PolygonValueType
            )) {
                override fun execute(target: Value, args: List<Value>): Value {
                    val poly = assertIsPolygon(target)
                    val area = assertIsFloat(args[0])
                    return poly.withArea(area)
                }
            },
            object : PrimitiveMethod<Polygon>("with_diam",
                MethodSignature(PolygonValueType,
                    listOf(Param("diam", FloatValueType)), PolygonValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                    val poly = assertIsPolygon(target)
                    val diam = assertIsFloat(args[0])
                    return poly.withEffectiveDiameter(diam)
                }
            },
            object : PrimitiveMethod<Polygon>("with_circumference",
                MethodSignature(
                    PolygonValueType,
                    listOf(Param("circ", FloatValueType)),
                    PolygonValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                    val poly = assertIsPolygon(target)
                    val circ = assertIsFloat(args[0])
                    return poly.withCircumference(circ)
                }
            },
            object : PrimitiveMethod<Polygon>("move",
                MethodSignature(
                    PolygonValueType,
                    listOf(Param("x", FloatValueType), Param("y", FloatValueType)),
                    PolygonValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                    val poly = assertIsPolygon(target)
                    val x = assertIsFloat(args[0])
                    val y = assertIsFloat(args[1])
                    return poly.offset(x, y)
                }
            },
            object : PrimitiveMethod<Polygon>("flipx",
                MethodSignature(PolygonValueType, emptyList(), PolygonValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                    val poly = assertIsPolygon(target)
                    return poly.flipX()
                }
            },
            object : PrimitiveMethod<Polygon>("flipy",
                MethodSignature(PolygonValueType, emptyList(), PolygonValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                                        val poly = assertIsPolygon(target)
                    return poly.flipY()
                }
            },
            object: PrimitiveMethod<Polygon>("extrude",
                MethodSignature(PolygonValueType,
                    listOf(Param("profile", ExtrusionProfileType)), CsgValueType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>
                ): Value {
                    val poly = assertIsPolygon(target)
                    val profile = ExtrusionProfileType.assertIs(args[0])
                    return CsgValue(
                        extrudeProfile(
                            listOf(profile),
                            crossSection = { scale -> poly.withEffectiveDiameter(1.0).scale(scale[0]) })
                    )
                }
            },
            object: PrimitiveMethod<Polygon>("extrude_series",
                MethodSignature<Polygon>(
                    PolygonValueType,
                    listOf(Param("profiles", ArrayValueType)), CsgValueType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>
                ): Value {
                    val poly = assertIsPolygon(target)
                    val profiles = ArrayValueType.assertIsArray(args[0]).map {
                        ExtrusionProfileType.assertIs(it)
                    }
                    return CsgValue(
                        extrudeProfile(
                            profiles,
                            crossSection = { scale -> poly.withEffectiveDiameter(1.0).scale(scale[0]) })
                    )
                }
            })
    }

    fun getFunction(name: String): PrimitiveFunctionValue {
        return providesFunctions.first { it.name == name}
    }

}

