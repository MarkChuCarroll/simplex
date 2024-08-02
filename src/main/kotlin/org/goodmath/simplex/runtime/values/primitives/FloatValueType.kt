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

import org.goodmath.simplex.ast.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.RootEnv
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist
import kotlin.isNaN
import kotlin.math.pow

object FloatValueType: ValueType() {
    override val name: String = "Float"

    override val asType: Type = Type.FloatType

    init {
        RootEnv.registerType(name, this)
    }


    override fun isTruthy(v: Value): Boolean {
        return assertIsFloat(v) != 0.0
    }

    override val supportsText: Boolean = true

    override fun toText(v: Value): String {
        val f = assertIs(v).d
        return f.toString()
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveMethod> by lazy {
        listOf(
            object : PrimitiveMethod("isNaN",
                MethodSignature(asType, emptyList(), Type.BooleanType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    return BooleanValue(assertIsFloat(target).isNaN())
                }
            },
            object : PrimitiveMethod("truncate",
                MethodSignature(asType, emptyList(), Type.IntType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    return IntegerValue(assertIsFloat(target).toInt())
                }
            },
            object: PrimitiveMethod("plus",
                MethodSignature(asType, listOf(Param("r", asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsFloat(target)
                    val r = assertIsFloat(args[0])
                    return FloatValue(l + r)
                }
            },
            object: PrimitiveMethod("minus",
                MethodSignature(asType, listOf(Param("r", asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsFloat(target)
                    val r = assertIsFloat(args[0])
                    return FloatValue(l - r)
                }
            },
            object: PrimitiveMethod("times",
                MethodSignature(asType, listOf(Param("r", asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsFloat(target)
                    val r = assertIsFloat(args[0])
                    return FloatValue(l * r)
                }
            },
            object: PrimitiveMethod("div",
                MethodSignature(asType, listOf(Param("r", asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsFloat(target)
                    val r = assertIsFloat(args[0])
                    return FloatValue(l / r)
                }
            },
            object: PrimitiveMethod("mod",
                MethodSignature(asType, listOf(Param("r", asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsFloat(target)
                    val r = assertIsFloat(args[0])
                    return FloatValue(l % r)
                }
            },
            object: PrimitiveMethod("pow",
                MethodSignature(asType, listOf(Param("r", asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsFloat(target)
                    val r = assertIsFloat(args[0])
                    return FloatValue(l.pow(r))
                }
            },
            object: PrimitiveMethod("eq",
                MethodSignature(asType, listOf(Param("r", asType)), Type.BooleanType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsFloat(target)
                    val r = assertIsFloat(args[0])
                    return BooleanValue(l == r)
                }
            },
            object: PrimitiveMethod("compare",
                MethodSignature(asType, listOf(Param("r", asType)), Type.IntType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsFloat(target)
                    val r = assertIsFloat(args[0])
                    return IntegerValue(l.compareTo(r))
                }
            },
            object: PrimitiveMethod("neg",
                MethodSignature(asType, emptyList(), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsFloat(target)
                    return  FloatValue(-l)
                }
            }
        )
    }

    override fun assertIs(v: Value): FloatValue {
        return if (v is FloatValue) {
            v
        } else {
            throwTypeError(v)
        }
    }
}

class FloatValue(val d: Double): Value {
    override val valueType: ValueType = FloatValueType

    override fun twist(): Twist =
        Twist.obj("FloatValue",
            Twist.attr("value", d.toString()))

}
