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
import manifold3d.linalg.DoubleVec4
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.twist.Twist

class RGBA(val r: Double, val g: Double, val b: Double, val a: Int) : Value {
    constructor(r: Double, g: Double, b: Double) : this(r, g, b, 1)

    override val valueType: ValueType = RGBAValueType

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

    fun dim(factor: Double): RGBA = RGBA(r * factor, g * factor, b * factor, a)

    fun blend(other: RGBA): RGBA = RGBA((r + other.r) / 2.0, (g + other.g) / 2.0, b + other.b, a)

    fun fade(factor: Double): RGBA = RGBA(r, g, b, (a * factor).toInt())

    companion object {
        val red = RGBA(1.0, 0.0, 0.0)
        val green = RGBA(0.0, 1.0, 0.0)
        val blue = RGBA(0.0, 0.0, 1.0)
        val yellow = RGBA(1.0, 1.0, 0.0)
        val purple = RGBA(1.0, 0.0, 1.0)
        val aqua = RGBA(0.0, 1.0, 1.0)
        val white = RGBA(1.0, 1.0, 1.0)
        val black = RGBA(0.0, 0.0, 0.0)
        val gray = RGBA(0.5, 0.5, 0.5)

    }
}

object RGBAValueType : ValueType() {
    override val name: String = "RGBA"

    override val asType: Type by lazy {
        Type.simple(name)
    }

    override fun isTruthy(v: Value): Boolean {
        return if (v is RGBA) {
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
                    return RGBA(r, g, b)
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
                    return RGBA(r, g, a)
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
        )
    }

    override val providesVariables: Map<String, Value> by lazy {
        mapOf(
            "red" to RGBA(1.0, 0.0, 0.0),
            "green" to RGBA(0.0, 1.0, 0.0),
            "blue" to RGBA(0.0, 0.0, 1.0),
            "yellow" to RGBA(1.0, 1.0, 0.0),
            "purple" to RGBA(1.0, 0.0, 1.0),
            "aqua" to RGBA(0.0, 1.0, 1.0),
            "white" to RGBA(1.0, 1.0, 1.0),
            "black" to RGBA(0.0, 0.0, 0.0),
        )
    }

    override fun assertIs(v: Value): RGBA {
        return v as? RGBA ?: throwTypeError(v)
    }
}
