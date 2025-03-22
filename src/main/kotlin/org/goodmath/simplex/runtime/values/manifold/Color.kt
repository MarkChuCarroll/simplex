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

import manifold3d.linalg.DoubleVec3
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.twist.Twist

class Color(val r: Double, val g: Double, val b: Double, val a: Int) : Value {
    constructor(r: Double, g: Double, b: Double) : this(r, g, b, 1)

    override val valueType: ValueType = ColorValueType

    override fun twist(): Twist =
        Twist.obj(
            "RGBA",
            Twist.attr("red", r.toString()),
            Twist.attr("green", g.toString()),
            Twist.attr("blue", b.toString()),
            Twist.attr("alpha", g.toString()),
        )

    fun toDVec3(): DoubleVec3 = DoubleVec3(r, g, b)

    val alpha: Int = a

    fun dim(factor: Double): Color = Color(r * factor, g * factor, b * factor, a)

    fun blend(other: Color): Color = Color((r + other.r) / 2.0, (g + other.g) / 2.0, b + other.b, a)

    fun fade(factor: Double): Color = Color(r, g, b, (a * factor).toInt())

    companion object {
        val red = Color(1.0, 0.0, 0.0)
        val green = Color(0.0, 1.0, 0.0)
        val blue = Color(0.0, 0.0, 1.0)
        val yellow = Color(1.0, 1.0, 0.0)
        val purple = Color(1.0, 0.0, 1.0)
        val aqua = Color(0.0, 1.0, 1.0)
        val white = Color(1.0, 1.0, 1.0)
        val black = Color(0.0, 0.0, 0.0)
        val gray = Color(0.5, 0.5, 0.5)

    }
}

object ColorValueType : ValueType() {
    override val name: String = "RGBA"

    override val asType: Type by lazy {
        Type.simple(name)
    }

    override fun isTruthy(v: Value): Boolean {
        return if (v is Color) {
            v.r != 0.0 || v.g != 0.0 || v.b != 0.0
        } else {
            false
        }
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object :
                PrimitiveFunctionValue(
                    "rgb",
                    FunctionSignature.simple(
                    listOf(
                            Param("red", FloatValueType.asType),
                            Param("green", FloatValueType.asType),
                            Param("blue", FloatValueType.asType),
                    ),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    val r = assertIsFloat(args[0])
                    val g = assertIsFloat(args[1])
                    val b = assertIsFloat(args[2])
                    return Color(r, g, b)
                }
            },
            object :
                PrimitiveFunctionValue(
                    "rgba",
                    FunctionSignature.simple(
                        listOf(
                            Param("red", FloatValueType.asType),
                            Param("green", FloatValueType.asType),
                            Param("blue", FloatValueType.asType),
                            Param("alpha", FloatValueType.asType),
                        ),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    val r = assertIsFloat(args[0])
                    val g = assertIsFloat(args[1])
                    val b = assertIsFloat(args[2])
                    val a = assertIsFloat(args[3])
                    return Color(r, g, a)
                }
            },
        )
    }

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object :
                PrimitiveMethod(
                    "dim",
                    MethodSignature.simple(asType, listOf(Param("factor", FloatValueType.asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val factor = assertIsFloat(args[0])
                    return self.dim(factor)
                }
            },
            object :
                PrimitiveMethod(
                    "fade",
                    MethodSignature.simple(asType, listOf(Param("factor", FloatValueType.asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val factor = assertIsFloat(args[0])
                    return self.fade(factor)
                }
            },
            object :
                PrimitiveMethod(
                    "blend",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return self.blend(other)
                }
            },
            object: PrimitiveMethod(
                "with_alpha",
                MethodSignature.simple(asType, listOf(Param("alpha", IntegerValueType.asType)), asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self =  assertIs(target)
                    val alpha = assertIsInt(args[0])
                    return Color(self.r, self.g, self.b, alpha)
                }
            }
        )
    }

    override val providesVariables: Map<String, Value> by lazy {
        mapOf(
            "red" to Color(1.0, 0.0, 0.0),
            "green" to Color(0.0, 1.0, 0.0),
            "blue" to Color(0.0, 0.0, 1.0),
            "yellow" to Color(1.0, 1.0, 0.0),
            "purple" to Color(1.0, 0.0, 1.0),
            "aqua" to Color(0.0, 1.0, 1.0),
            "white" to Color(1.0, 1.0, 1.0),
            "black" to Color(0.0, 0.0, 0.0),
        )
    }

    override fun assertIs(v: Value): Color {
        return v as? Color ?: throwTypeError(v)
    }
}
