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
import org.goodmath.simplex.ast.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.twist.Twist
import kotlin.math.sqrt

data class TwoDPoint(val x: Double, val y: Double): Value {
    override val valueType: ValueType = TwoDPointValueType

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


object TwoDPointValueType: ValueType() {
    override val name: String = "TwoDPoint"
    override val asType: Type = Type.TwoDPointType


    override val supportsText: Boolean = true

    override fun toText(v: Value): String {
        val point = assertIs(v)
        return "(x=${point.x}, y=${point.y})"
    }

    override fun isTruthy(v: Value): Boolean {
        return true
    }


    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object : PrimitiveFunctionValue(
                "p2d",
                FunctionSignature(listOf(
                    Param("x", Type.FloatType),
                    Param("y", Type.FloatType)),
                    asType)
            ) {
                override fun execute(args: List<Value>): Value {
                    val x = assertIsFloat(args[0])
                    val y = assertIsFloat(args[1])
                    return TwoDPoint(x, y)
                }
            })
    }

    override val providesOperations: List<PrimitiveMethod> by lazy {
        listOf(
            object: PrimitiveMethod("mag",
                MethodSignature(asType, emptyList(), Type.FloatType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val p = assertIs(args[0])
                    return FloatValue(sqrt(p.x * p.x + p.y * p.y))
                }
            },
            object: PrimitiveMethod("plus",
                MethodSignature(asType, listOf(Param("r", asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val p1 = assertIs(target)
                    val p2 = assertIs(args[0])
                    return p1 + p2
                }
            },
            object: PrimitiveMethod("minus",
                MethodSignature(asType, listOf(Param("r", asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val p1 = assertIs(target)
                    val p2 = assertIs(args[0])
                    return p1 - p2
                }
            },
            object: PrimitiveMethod("multBy",
                MethodSignature(asType, listOf(Param("r", Type.FloatType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val p1 = assertIs(target)
                    val v = assertIsFloat(args[0])
                    return TwoDPoint(p1.x * v, p1.y * v)
                }
            },
            object: PrimitiveMethod("eq",
                MethodSignature(asType, listOf(Param("r", asType)), Type.BooleanType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val p1 = assertIs(target)
                    val p2 = assertIs(args[0])
                    return BooleanValue(p1 == p2)
                }
            },
            object: PrimitiveMethod("neg",
                MethodSignature(asType, emptyList(), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val p1 = assertIs(target)
                    return TwoDPoint(-p1.x, -p1.y)
                }
            },
            object: PrimitiveMethod("compare",
                MethodSignature(asType, listOf(Param("r", asType)), Type.IntType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val p1 = assertIs(target)
                    val p2 = assertIs(args[0])
                    return IntegerValue(p1.compareTo(p2))
                }
            })
    }

    override fun assertIs(v: Value): TwoDPoint {
        if (v is TwoDPoint) {
            return v
        } else {
            throwTypeError(v)
        }
    }
}
