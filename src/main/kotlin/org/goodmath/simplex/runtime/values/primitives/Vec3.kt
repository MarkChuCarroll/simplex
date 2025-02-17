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
import manifold3d.glm.DoubleVec3
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist

class Vec3(val x: Double, val y: Double, val z: Double) : Value {
    override val valueType: ValueType = Vec3ValueType

    override fun twist(): Twist {
        return Twist.obj(
            "Vec3",
            Twist.attr("x", x.toString()),
            Twist.attr("y", y.toString()),
            Twist.attr("z", z.toString()),
        )
    }

    override fun toString(): String {
        return "(x=$x, y=$y, z=$z)"
    }

    fun toDoubleVec3(): DoubleVec3 {
        return DoubleVec3(x, y, z)
    }

    operator fun plus(other: Vec3): Vec3 {
        return Vec3(x + other.x, y + other.y, z + other.z)
    }

    operator fun minus(other: Vec3): Vec3 {
        return Vec3(x - other.x, y - other.y, z - other.z)
    }

    fun dot(other: Vec3): Double {
        return x * other.x + y * other.y + z * other.z
    }

    fun magnitude(): Double {
        return sqrt(x * x + y * y + z * z)
    }

    fun compareTo(other: Vec3): Int {
        return when {
            x < other.x -> -1
            x > other.x -> 1
            y < other.y -> -1
            y > other.y -> 1
            z < other.z -> -1
            z > other.z -> 1
            else -> 0
        }
    }

    fun eq(other: Any?): Boolean {
        return other is Vec3 && x == other.x && y == other.y && z == other.z
    }

    operator fun times(other: Double): Vec3 {
        return Vec3(x * other, y * other, z * other)
    }

    operator fun div(other: Double): Vec3 {
        return Vec3(x / other, y / other, z / other)
    }

    operator fun unaryMinus(): Vec3 = Vec3(-x, -y, -z)

    companion object {
        fun fromDoubleVec3(doubleVec3: DoubleVec3): Vec3 {
            return Vec3(doubleVec3.x(), doubleVec3.y(), doubleVec3.z())
        }
    }
}

object Vec3ValueType : ValueType() {
    override val name: String = "Vec3"

    override val asType: Type by lazy {
        Type.simple(name)
    }

    override val supportsText: Boolean = true

    override fun toText(v: Value): String {
        return v.toString()
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
                    "v3",
                    FunctionSignature.simple(
                        listOf(
                            Param("x", FloatValueType.asType),
                            Param("y", FloatValueType.asType),
                            Param("z", FloatValueType.asType),
                        ),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    return Vec3(
                        assertIsFloat(args[0]),
                        assertIsFloat(args[1]),
                        assertIsFloat(args[2]),
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
                PrimitiveMethod("negate", MethodSignature.simple(asType, emptyList<Param>(), asType)) {
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
            object: PrimitiveMethod(
                "x",
                MethodSignature.simple(asType, emptyList(), FloatValueType.asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return FloatValue(self.x)
                }
            },
            object: PrimitiveMethod(
                "y",
                MethodSignature.simple(asType, emptyList(), FloatValueType.asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return FloatValue(self.y)
                }
            },
            object: PrimitiveMethod(
                "z",
                MethodSignature.simple(asType, emptyList(), FloatValueType.asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return FloatValue(self.z)
                }
            }
        )
    }
    override val providesVariables: Map<String, Value> by lazy {
        mapOf("zero_v3" to Vec3(0.0, 0.0, 0.0), "unit_v3" to Vec3(1.0, 1.0, 1.0))
    }

    override fun assertIs(v: Value): Vec3 {
        return if (v is Vec3) {
            v
        } else {
            throwTypeError(v)
        }
    }
}
