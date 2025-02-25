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
package org.goodmath.simplex.runtime.values.primitives

import kotlin.math.sqrt
import manifold3d.glm.DoubleVec2
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist

class Vec2(val x: Double, val y: Double) : Value {
    override val valueType: ValueType = Vec2ValueType

    override fun twist(): Twist {
        return Twist.obj("Vec2", Twist.attr("x", x.toString()), Twist.attr("y", y.toString()))
    }

    fun toDoubleVec2(): DoubleVec2 {
        return DoubleVec2(x, y)
    }

    operator fun plus(other: Vec2): Vec2 {
        return Vec2(x + other.x, y + other.y)
    }

    operator fun minus(other: Vec2): Vec2 {
        return Vec2(x - other.x, y - other.y)
    }

    fun dot(other: Vec2): Double {
        return x * other.x + y * other.y
    }

    fun magnitude(): Double {
        return sqrt(x * x + y * y)
    }

    fun compareTo(other: Vec2): Int {
        return when {
            x < other.x -> -1
            x > other.x -> 1
            y < other.y -> -1
            y > other.y -> 1
            else -> 0
        }
    }

    fun eq(other: Any?): Boolean {
        return other is Vec2 && x == other.x && y == other.y
    }

    operator fun times(other: Double): Vec2 {
        return Vec2(x * other, y * other)
    }

    operator fun div(other: Double): Vec2 {
        return Vec2(x / other, y / other)
    }

    operator fun unaryMinus(): Vec2 = Vec2(-x, -y)

    fun at(z: Double): Vec3 {
        return Vec3(x, y, z)
    }

    override fun toString(): String {
      return "v2($x, $y)"
    }

    companion object {
        fun fromDoubleVec2(doubleVec2: DoubleVec2): Vec2 {
            return Vec2(doubleVec2.x(), doubleVec2.y())
        }
    }
}

object Vec2ValueType : ValueType() {
    override val name: String = "Vec2"

    override val asType: Type by lazy {
        Type.simple(name)
    }

    override fun isTruthy(v: Value): Boolean {
        return if (v is Vec3) {
            v.x != 0.0 || v.y != 0.0 || v.z != 0.0
        } else {
            false
        }
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object :
                PrimitiveFunctionValue(
                    "v2",
                    FunctionSignature.simple(
                        listOf(Param("x", FloatValueType.asType), Param("y", FloatValueType.asType)),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    return Vec2(
                        assertIsFloat(args[0]),
                        assertIsFloat(args[1]),
                    )
                }
            }
        )
    }

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
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
                    "times",
                    MethodSignature.simple(asType, listOf(Param("other", FloatValueType.asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIsFloat(args[0])
                    return self * other
                }
            },
            object :
                PrimitiveMethod(
                    "div",
                    MethodSignature.simple(asType, listOf(Param("other", FloatValueType.asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIsFloat(args[0])
                    return self / other
                }
            },
            object :
                PrimitiveMethod("neg", MethodSignature.simple(asType, emptyList<Param>(), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return -self
                }
            },
            object :
                PrimitiveMethod(
                    "eq",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), BooleanValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return BooleanValue(self.eq(other))
                }
            },
            object :
                PrimitiveMethod(
                    "compare",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), IntegerValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return IntegerValue(self.compareTo(other))
                }
            },
            object :
                PrimitiveMethod(
                    "dot",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), FloatValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return FloatValue(self.dot(other))
                }
            },
            object :
                PrimitiveMethod(
                    "at",
                    MethodSignature.simple(asType, listOf(Param("z", FloatValueType.asType)), Vec3ValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val z = assertIsFloat(args[0])
                    return self.at(z)
                }
            },
        )
    }
    override val providesVariables: Map<String, Value> by lazy {
        mapOf("zero_v2" to Vec2(0.0, 0.0))
    }

    override fun assertIs(v: Value): Vec2 {
        return v as? Vec2 ?: throwTypeError(v)
    }
}
