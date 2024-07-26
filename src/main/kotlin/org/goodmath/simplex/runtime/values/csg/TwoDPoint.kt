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
package org.goodmath.simplex.runtime.csg

import eu.mihosoft.vvecmath.Vector3d
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.twist.Twist
import kotlin.math.sqrt

data class TwoDPoint(val x: Double, val y: Double): Value {
    override val valueType: ValueType<TwoDPoint> = TwoDPointValueType

    override fun twist(): Twist =
        Twist.obj("Point2D",
            Twist.attr("x", x.toString()),
            Twist.attr("y", y.toString()))

    fun at(z: Double): Vector3d = Vector3d.xyz(x, y, z)

    operator fun plus(other: TwoDPoint): TwoDPoint {
        return TwoDPoint(x + other.x, y + other.y)
    }

    operator fun minus(other: TwoDPoint): TwoDPoint {
        return TwoDPoint(x - other.x, y - other.y)
    }

    operator fun compareTo(other: TwoDPoint): Int {
        if (x < other.x) {
            return -1
        } else if (x == other.x) {
            if (y < other.y) {
                return -1
            } else if (y == other.y) {
                return 0
            }
        }
        return 1
    }

}


object TwoDPointValueType: ValueType<TwoDPoint>() {
    override val name: String = "Point2D"

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override fun add(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIs(v1)
        val p2 = assertIs(v1)
        return p1 + p2
    }

    override fun subtract(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIs(v1)
        val p2 = assertIs(v1)
        return p1 - p2
    }

    override fun mult(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIs(v1)
        val p2 = assertIsFloat(v2)
        return TwoDPoint(p1.x * p2, p1.y * p2)
    }

    override fun div(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIs(v1)
        val p2 = assertIsFloat(v2)
        return TwoDPoint(p1.x / p2, p1.y / p2)
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
        val p1 = assertIs(v1)
        val p2 = assertIs(v2)
        return p1 == p2
    }

    override fun neg(v1: Value): Value {
        val p1 = assertIs(v1)
        return TwoDPoint(-p1.x, -p1.y)
    }

    override fun compare(v1: Value, v2: Value): Int {
        val p1 = assertIs(v1)
        val p2 = assertIs(v2)
        return p1.compareTo(p2)
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object : PrimitiveFunctionValue(
                "p2d",
                FunctionSignature(listOf(
                    Param("x", FloatValueType),
                    Param("y", FloatValueType)),
                    TwoDPointValueType)
            ) {
                override fun execute(args: List<Value>): Value {
                    val x = assertIsFloat(args[0])
                    val y = assertIsFloat(args[1])
                    return TwoDPoint(x, y)
                }
            })
    }

    override val providesOperations: List<PrimitiveMethod<TwoDPoint>> by lazy {
        listOf(
            object: PrimitiveMethod<TwoDPoint>("mag",
                MethodSignature(TwoDPointValueType, emptyList(), FloatValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                    val p = assertIs(args[0])
                    return FloatValue(sqrt(p.x * p.x + p.y * p.y))
                }
            })
    }
}