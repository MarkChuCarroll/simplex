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

import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist

object IntegerValueType : ValueType() {
    override val name: String = "Int"

    override val asType: Type by lazy {
        Type.simple(name)
    }

    override val supportsText: Boolean = true

    override fun toText(v: Value): String {
        val i = assertIsInt(v)
        return i.toString()
    }

    override fun isTruthy(v: Value): Boolean {
        v as IntegerValue
        return v.i != 0
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object :
                PrimitiveMethod(
                    "to",
                    MethodSignature.simple(asType, listOf(Param("max", asType)), Type.array(asType)),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val lower = assertIs(target).i
                    val upper = assertIs(args[0]).i
                    return ArrayValue(IntegerValueType, (lower..upper).map { IntegerValue(it) })
                }
            },
            object :
                PrimitiveMethod(
                    "float",
                    MethodSignature.simple(asType, emptyList<Param>(), FloatValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    return FloatValue(assertIsInt(target).toDouble())
                }
            },
            object :
                PrimitiveMethod(
                    "plus",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsInt(target)
                    val r = assertIsInt(args[0])
                    return IntegerValue(l + r)
                }
            },
            object :
                PrimitiveMethod(
                    "minus",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsInt(target)
                    val r = assertIsInt(args[0])
                    return IntegerValue(l - r)
                }
            },
            object :
                PrimitiveMethod(
                    "times",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsInt(target)
                    val r = assertIsInt(args[0])
                    return IntegerValue(l * r)
                }
            },
            object :
                PrimitiveMethod(
                    "div",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsInt(target)
                    val r = assertIsInt(args[0])
                    return IntegerValue(l / r)
                }
            },
            object :
                PrimitiveMethod(
                    "mod",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsInt(target)
                    val r = assertIsInt(args[0])
                    return IntegerValue(l % r)
                }
            },
            object :
                PrimitiveMethod(
                    "pow",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsInt(target)
                    val r = assertIsInt(args[0])
                    var result = 1
                    if (r >= 0) {
                        repeat(r) { result *= l }
                        return IntegerValue(result)
                    } else {
                        throw SimplexEvaluationError("Cannot raise an integer to a negative power")
                    }
                }
            },
            object :
                PrimitiveMethod(
                    "eq",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), BooleanValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsInt(target)
                    val r = assertIsInt(args[0])
                    return BooleanValue(l == r)
                }
            },
            object :
                PrimitiveMethod(
                    "compare",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsInt(target)
                    val r = assertIsInt(args[0])
                    return IntegerValue(l.compareTo(r))
                }
            },
            object : PrimitiveMethod("neg", MethodSignature.simple(asType, emptyList<Param>(), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIsInt(target)
                    return IntegerValue(-l)
                }
            },
        )
    }
    override val providesVariables: Map<String, Value> by lazy {
        mapOf("MAXINT" to IntegerValue(Int.MAX_VALUE), "MININT" to IntegerValue(Int.MIN_VALUE))
    }

    override fun assertIs(v: Value): IntegerValue {
        return if (v is IntegerValue) {
            v
        } else {
            throwTypeError(v)
        }
    }
}

class IntegerValue(val i: Int) : Value {
    override fun twist(): Twist = Twist.obj("IntegerValue", Twist.attr("value", i.toString()))

    override fun equals(other: Any?): Boolean {
        return other is IntegerValue && other.i == i
    }

    override fun hashCode(): Int {
        return i.hashCode()
    }

    override val valueType: ValueType = IntegerValueType
}
