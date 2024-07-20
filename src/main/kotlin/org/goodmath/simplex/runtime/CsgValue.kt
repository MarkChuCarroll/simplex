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
package org.goodmath.simplex.runtime

import eu.mihosoft.jcsg.CSG
import eu.mihosoft.jcsg.Cube
import eu.mihosoft.jcsg.Extrude
import eu.mihosoft.jcsg.Polygon
import eu.mihosoft.jcsg.Sphere
import eu.mihosoft.jcsg.Vertex
import eu.mihosoft.vvecmath.Vector3d
import org.goodmath.simplex.twist.Twist
import kotlin.math.sqrt


object CsgValueType: ValueType<CsgValue>() {
    override val name: String = "CSG"

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    fun assertIsCsg(v: Value): CSG {
        if (v is CsgValue) {
            return v.csgValue
        } else {
            throw SimplexTypeError("Expected a CSG, found ${v.valueType.name}")
        }
    }

    override fun add(
        v1: Value,
        v2: Value
    ): Value {
        val c1 = assertIsCsg(v1)
        val c2 = assertIsCsg(v2)
        return CsgValue(c1.union(c2))
    }

    override fun subtract(
        v1: Value,
        v2: Value
    ): Value {
        val c1 = assertIsCsg(v1)
        val c2 = assertIsCsg(v2)
        return CsgValue(c1.difference(c2))
    }

    override fun mult(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("CSG does not support multiplication")
    }

    override fun div(
        v1: Value,
        v2: Value
    ): Value {
        val c1 = assertIsCsg(v1)
        val c2 = assertIsCsg(v2)
        return CsgValue(c1.intersect(c2))
    }

    override fun mod(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("CSG does not support modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("CSG does not support exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value
    ): Boolean {
        val c1 = assertIsCsg(v1)
        val c2 = assertIsCsg(v2)
        return c1 == c2
    }

    override fun neg(v1: Value): Value {
        throw SimplexTypeError("CSG does not support negation")
    }

    override fun compare(v1: Value, v2: Value): Int {
        throw SimplexTypeError("CSG does not support exponentiation")
    }

    override val providesFunctions: List<PrimitiveFunctionValue> =
        listOf(
            PrimitiveFunctionValue("block", listOf(PointValueType, PointValueType),  CsgValueType) { args: List<Value> ->
                val center = PointValueType.assertIsPoint(args[0])
                val size = PointValueType.assertIsPoint(args[1])
                CsgValue(Cube(center,size).toCSG())
            },
            PrimitiveFunctionValue("sphere", listOf(PointValueType, FloatValueType),
                CsgValueType) { args: List<Value> ->
                val center = PointValueType.assertIsPoint(args[0])
                val radius = assertIsFloat(args[1])
                CsgValue(Sphere(center, radius, 16, 8).toCSG())
            },

        )


    override val providesOperations: List<PrimitiveOperation> = emptyList()

}

class CsgValue(val csgValue: CSG): Value {
    override val valueType: ValueType<CsgValue> = CsgValueType

    override fun twist(): Twist =
        Twist.obj(
            "CSGValue",
            Twist.attr("csg", csgValue.toObjString())
        )

}


object PointValueType: ValueType<PointValue>() {
    override val name: String = "Point"

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    fun assertIsPoint(v: Value): Vector3d {
        if (v is PointValue) {
            return v.xyz
        } else {
            throw SimplexTypeError("Expected a Point, but found ${v.valueType.name}")
        }
    }

    override fun add(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIsPoint(v1)
        val p2 = assertIsPoint(v2)
        return PointValue(p1.plus(p2))
    }

    override fun subtract(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIsPoint(v1)
        val p2 = assertIsPoint(v2)
        return PointValue(p1.minus(p2))
    }

    override fun mult(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIsPoint(v1)
        val d = assertIsFloat(v2)
        return PointValue(p1.times(d))
    }

    override fun div(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIsPoint(v1)
        val d = assertIsFloat(v2)
        return PointValue(p1.divided(d))
    }

    override fun mod(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("Point does not support modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("Point does not support exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value
    ): Boolean {
        val p1 = assertIsPoint(v1)
        val p2 = assertIsPoint(v2)
        return p1 == p2
    }

    override fun neg(v1: Value): Value {
        val p1 = assertIsPoint(v1)
        return PointValue(p1.negated())
    }

    override fun compare(v1: Value, v2: Value): Int {
        throw SimplexTypeError("Point does not support ordering")
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = listOf(
        PrimitiveFunctionValue("point", listOf(FloatValueType,
            FloatValueType, FloatValueType), PointValueType) { args: List<Value> ->
            val x = assertIsFloat(args[0])
            val y = assertIsFloat(args[1])
            val z = assertIsFloat(args[2])
            PointValue(Vector3d.xyz(x, y, z))
        }
    )

    override val providesOperations: List<PrimitiveOperation> = emptyList()
}

class PointValue(val xyz: Vector3d): Value {
    override val valueType: ValueType<PointValue> = PointValueType


    override fun twist(): Twist =
        Twist.obj("PointValue",
            Twist.attr("x", xyz.x.toString()),
            Twist.attr("y", xyz.y.toString()),
            Twist.attr("z", xyz.z.toString()),
            )


}

object Point2DValueType: ValueType<Point2DValue>() {
    override val name: String = "Point2D"

    fun assertIsPoint2D(v: Value): Point2DValue {
        if (v is Point2DValue) {
            return v
        } else {
            throw SimplexTypeError("Expected a $name, but received a ${v.valueType.name}")
        }
    }

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override fun add(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIsPoint2D(v1)
        val p2 = assertIsPoint2D(v2)
        return Point2DValue(p1.x + p2.x, p1.y + p2.y)
    }

    override fun subtract(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIsPoint2D(v1)
        val p2 = assertIsPoint2D(v2)
        return Point2DValue(p1.x + p2.x, p1.y + p2.y)
    }

    override fun mult(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIsPoint2D(v1)
        val p2 = assertIsFloat(v2)
        return Point2DValue(p1.x*p2, p1.y*p2)
    }

    override fun div(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIsPoint2D(v1)
        val p2 = assertIsFloat(v2)
        return Point2DValue(p1.x/p2, p1.y/p2)
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
        val p1 = assertIsPoint2D(v1)
        val p2 = assertIsPoint2D(v2)
        return p1 == p2
    }

    override fun neg(v1: Value): Value {
        val p1 = assertIsPoint2D(v1)
        return Point2DValue(-p1.x, -p1.y)
    }

    override fun compare(v1: Value, v2: Value): Int {
        throw SimplexUnsupportedOperation(name, "ordering")
    }

    override val providesFunctions: List<PrimitiveFunctionValue> =
        listOf(
            PrimitiveFunctionValue("twod", listOf(FloatValueType, FloatValueType),
                Point2DValueType) { args: List<Value> ->
                assertArity(args, 2)
                val x = assertIsFloat(args[0])
                val y = assertIsFloat(args[1])
                Point2DValue(x, y)
            }
        )

    override val providesOperations: List<PrimitiveOperation> = listOf(
        PrimitiveOperation("mag", emptyList(), FloatValueType) { target: Value, args: List<Value> ->
            assertArity(args, 0)
            val p = assertIsPoint2D(args[0])
            FloatValue(sqrt(p.x*p.x + p.y*p.y))
        }
    )
}

class Point2DValue(val x: Double, val y: Double): Value {
    override val valueType: ValueType<Point2DValue> = Point2DValueType

    override fun twist(): Twist =
        Twist.obj("Point2D",
            Twist.attr("x", x.toString()),
            Twist.attr("y", y.toString()))
}


object PolygonValueType: ValueType<PolygonValue>() {
    override val name: String = "Polygon"

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    fun assertIsPolygon(v: Value): Polygon {
        if (v is PolygonValue) {
            return v.polygon
        } else {
            throw SimplexTypeError("Expected a $name, but received ${v.valueType.name}")
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

    override val providesFunctions: List<PrimitiveFunctionValue> = listOf(
        PrimitiveFunctionValue("polygon", listOf(ArrayValueType), PolygonValueType) { args: List<Value> ->
            assertArity(args, 1)
            val p2ds = ArrayValueType.assertIsArray(args[0])
            PolygonValue(Polygon(p2ds.map {
                val p = Point2DValueType.assertIsPoint2D(it)
                Vertex(Vector3d.xyz(p.x, p.y, 0.0), Vector3d.xyz(0.0, 0.0, 1.0))
            }))
        })

    override val providesOperations: List<PrimitiveOperation> = listOf(
        PrimitiveOperation("linear_extrude", listOf(PointValueType), CsgValueType) {
            target: Value, args: List<Value> ->
            val poly = assertIsPolygon(target)
            val vec = PointValueType.assertIsPoint(args[0])
            val shape = Extrude.points(vec, poly.vertices.map { it.pos })
            CsgValue(shape)
        }
    )
}

class PolygonValue(val polygon: Polygon): Value {
    override val valueType: ValueType<PolygonValue> = PolygonValueType

    override fun twist(): Twist =
        Twist.obj("PolygonValue",
            Twist.attr("value", polygon.toString()))
}
