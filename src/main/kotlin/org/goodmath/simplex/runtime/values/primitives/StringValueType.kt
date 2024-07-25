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

import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist
import kotlin.text.indexOf
import kotlin.text.isNotEmpty
import kotlin.text.repeat

object StringValueType: ValueType<StringValue>() {
    override val name: String = "String"

    override fun isTruthy(v: Value): Boolean {
        return assertIsString(v).isNotEmpty()
    }

    override fun add(
        v1: Value,
        v2: Value,
    ): Value {
        v2 as StringValue
        return StringValue(assertIsString(v1) + v2.s)
    }

    override fun subtract(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "subtraction")
    }

    override fun mult(
        v1: Value,
        v2: Value,
    ): Value {
        v2 as IntegerValue
        return StringValue(assertIsString(v1).repeat(v2.i))
    }

    override fun div(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "division")
    }

    override fun mod(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value,
    ): Boolean {
        v2 as StringValue
        return assertIsString(v1) == v2.s
    }

    override fun neg(v1: Value): Value {
        throw SimplexUnsupportedOperation(name, "negation")
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        return assertIsString(v1).compareTo(assertIsString(v2))
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()
    override val providesOperations: List<PrimitiveMethod<StringValue>> by lazy {
        listOf(
            object : PrimitiveMethod<StringValue>("length",
                MethodSignature(StringValueType, emptyList(), IntegerValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                    return IntegerValue(assertIsString(target).length)
                }
            },
            object : PrimitiveMethod<StringValue>("find",
                MethodSignature(
                    StringValueType,
                    listOf(StringValueType), IntegerValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                    val pat = assertIsString(args[0])
                    return IntegerValue(assertIsString(target).indexOf(pat))
                }
            })
    }
}

class StringValue(val s: String): Value {
    override val valueType: ValueType<StringValue> = StringValueType

    override fun twist(): Twist {
        return Twist.obj("StringValue",
            Twist.attr("value", s))
    }

}
