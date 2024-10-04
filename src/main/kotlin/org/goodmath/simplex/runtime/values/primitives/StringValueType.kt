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

import kotlin.text.indexOf
import kotlin.text.isNotEmpty
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist

object StringValueType : ValueType() {
    override val name: String = "String"
    override val asType: Type by lazy {
        Type.simple(name)
    }

    override fun isTruthy(v: Value): Boolean {
        return assertIsString(v).isNotEmpty()
    }

    override val supportsText: Boolean = true

    override fun toText(v: Value): String {
        return assertIs(v).s
    }

    override fun assertIs(v: Value): StringValue {
        return if (v is StringValue) {
            v
        } else {
            throwTypeError(v)
        }
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()
    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object :
                PrimitiveMethod(
                    "length",
                    MethodSignature.simple(asType, emptyList<Param>(), IntegerValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    return IntegerValue(assertIsString(target).length)
                }
            },
            object :
                PrimitiveMethod(
                    "find",
                    MethodSignature.simple(asType, listOf(Param("s", asType)), IntegerValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val pat = assertIsString(args[0])
                    return IntegerValue(assertIsString(target).indexOf(pat))
                }
            },
            object :
                PrimitiveMethod(
                    "plus",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIs(target).s
                    val r = assertIs(args[0]).s
                    return StringValue(l + r)
                }
            },
            object :
                PrimitiveMethod(
                    "eq",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), BooleanValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIs(target).s
                    val r = assertIs(args[0]).s
                    return BooleanValue(l == r)
                }
            },
            object :
                PrimitiveMethod(
                    "compare",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), IntegerValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val l = assertIs(target).s
                    val r = assertIs(args[0]).s
                    return IntegerValue(l.compareTo(r))
                }
            },
        )
    }
    override val providesVariables: Map<String, Value> = emptyMap()
}

class StringValue(val s: String) : Value {
    override val valueType: ValueType = StringValueType

    override fun twist(): Twist {
        return Twist.obj("StringValue", Twist.attr("value", s))
    }
}
