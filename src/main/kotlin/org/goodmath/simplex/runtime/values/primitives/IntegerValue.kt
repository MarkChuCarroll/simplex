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

import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist
import kotlin.math.pow

object IntegerValueType: ValueType<IntegerValue>() {
    override val name: String = "Int"

    override val supportsText: Boolean = true

    override fun toText(v: Value): String {
        val i = assertIsInt(v)
        return i.toString()
    }

    override fun isTruthy(v: Value): Boolean {
        v as IntegerValue
        return v.i != 0
    }

    override fun add(
        v1: Value,
        v2: Value,
    ): Value {
        v1 as IntegerValue
        return IntegerValue(v1.i + assertIsInt(v2))
    }

    override fun subtract(
        v1: Value,
        v2: Value,
    ): Value {
        v1 as IntegerValue
        return IntegerValue(v1.i - assertIsInt(v2))
    }

    override fun mult(
        v1: Value,
        v2: Value,
    ): Value {
        v1 as IntegerValue
        return IntegerValue(v1.i * assertIsInt(v2))
    }

    override fun div(
        v1: Value,
        v2: Value,
    ): Value {
        v1 as IntegerValue
        return IntegerValue(v1.i / assertIsInt(v2))
    }

    override fun mod(
        v1: Value,
        v2: Value,
    ): Value {
        v1 as IntegerValue
        return IntegerValue(v1.i % assertIsInt(v2))
    }

    override fun pow(
        v1: Value,
        v2: Value,
    ): Value {
        v1 as IntegerValue
        return FloatValue(v1.i.toDouble().pow(assertIsInt(v2)))
    }

    override fun equals(
        v1: Value,
        v2: Value,
    ): Boolean {
        v1 as IntegerValue
        return v1.i == assertIsInt(v2)
    }

    override fun neg(v1: Value): Value {
        v1 as IntegerValue
        return IntegerValue(-v1.i)
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        v1 as IntegerValue
        v2 as IntegerValue
        return v1.i.compareTo(v2.i)
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveMethod<IntegerValue>> by lazy {
        listOf(
            object : PrimitiveMethod<IntegerValue>("float",
                MethodSignature(
                    IntegerValueType,
                    emptyList(), FloatValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                    return FloatValue(assertIsInt(target).toDouble())
                }
            }
        )
    }
}

class IntegerValue(val i: Int): Value {
    override fun twist(): Twist =
        Twist.obj("IntegerValue",
            Twist.attr("value", i.toString()))

    override fun equals(other: Any?): Boolean {
        return other is IntegerValue &&
                other.i == i
    }

    override fun hashCode(): Int {
        return i.hashCode()
    }

    override val valueType: ValueType<IntegerValue> = IntegerValueType
}
