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

import eu.mihosoft.vvecmath.Vector3d
import kotlin.math.sqrt
import manifold3d.glm.DoubleVec3
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.types.Parameter
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.ParameterSignature
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist

class Vec3(val v3: Vector3d): Value {
    constructor(x: Double, y: Double, z: Double): this(Vector3d.xyz(x, y, z))
    override val valueType: ValueType = Vec3ValueType

    override fun twist(): Twist {
        return Twist.obj(
            "Vec3",
            Twist.attr("x", v3.x.toString()),
            Twist.attr("y", v3.y.toString()),
            Twist.attr("z", v3.z.toString()),
        )
    }

    override fun toString(): String {
        return "(x=${v3.x}, y=${v3.y}, z=${v3.z})"
    }


    fun compareTo(other: Vec3): Int {
        return when {
            v3.x < other.v3.x -> -1
            v3.x > other.v3.x -> 1
            v3.y < other.v3.y -> -1
            v3.y > other.v3.y -> 1
            v3.z < other.v3.z -> -1
            v3.z > other.v3.z -> 1
            else -> 0
        }
    }

    fun eq(other: Any?): Boolean {
        return other is Vec3 && v3 == other.v3
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
            v.v3.x != 0.0 || v.v3.y != 0.0 || v.v3.z != 0.0
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
                        ParameterSignature(listOf(
                            Parameter("x", FloatValueType.asType),
                            Parameter("y", FloatValueType.asType),
                            Parameter("z", FloatValueType.asType)),
                        ),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>, kwArgs: Map<String, Value>): Value {
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
                    MethodSignature.simple(asType, ParameterSignature(listOf(Parameter("other", asType))), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, kwArgs: Map<String, Value>, env: Env): Value {
                    val self = assertIs(target).v3
                    val other = assertIs(args[0]).v3
                    return Vec3(self.plus(other))
                }
            },
            object :
                PrimitiveMethod(
                    "minus",
                    MethodSignature.simple(asType,
                        ParameterSignature(listOf(Parameter("other", asType))), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, kwArgs: Map<String, Value>, env: Env): Value {
                    val self = assertIs(target).v3
                    val other = assertIs(args[0]).v3
                    return Vec3(self - other)
                }
            },
            object :
                PrimitiveMethod(
                    "times",
                    MethodSignature.simple(asType,
                        ParameterSignature(listOf(Parameter("other", FloatValueType.asType))), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, kwArgs: Map<String, Value>, env: Env): Value {
                    val self = assertIs(target).v3
                    val other = assertIsFloat(args[0])
                    return Vec3(self.times(other))
                }
            },
            object :
                PrimitiveMethod(
                    "div",
                    MethodSignature.simple(asType,
                        ParameterSignature(listOf(Parameter("other", FloatValueType.asType))), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, kwArgs: Map<String, Value>, env: Env): Value {
                    val self = assertIs(target).v3
                    val other = assertIsFloat(args[0])
                    return Vec3(self.divided(other))
                }
            },
            object :
                PrimitiveMethod("negate", MethodSignature.simple(asType, ParameterSignature.empty, asType)) {
                override fun execute(target: Value, args: List<Value>, kwArgs: Map<String, Value>, env: Env): Value {
                    val self = assertIs(target).v3
                    return Vec3(self.negated())
                }
            },
            object :
                PrimitiveMethod(
                    "eq",
                    MethodSignature.simple(asType, ParameterSignature(listOf(Parameter("other", asType))), BooleanValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, kwArgs: Map<String, Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return BooleanValue(self.eq(other))
                }
            },
            object :
                PrimitiveMethod(
                    "compare",
                    MethodSignature.simple(asType, ParameterSignature(listOf(Parameter("other", asType))), IntegerValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, kwArgs: Map<String, Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return IntegerValue(self.compareTo(other))
                }
            },
            object :
                PrimitiveMethod(
                    "dot",
                    MethodSignature.simple(asType, ParameterSignature(listOf(Parameter("other", asType))), FloatValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, kwArgs: Map<String, Value>, env: Env): Value {
                    val self = assertIs(target).v3
                    val other = assertIs(args[0]).v3
                    return FloatValue(self.dot(other))
                }
            },
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
