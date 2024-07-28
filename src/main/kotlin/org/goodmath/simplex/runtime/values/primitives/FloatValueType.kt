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
import kotlin.isNaN
import kotlin.math.pow

object FloatValueType: ValueType<FloatValue>() {
    override val name: String = "Float"

    override fun isTruthy(v: Value): Boolean {
        return assertIsFloat(v) != 0.0
    }

    override val supportsText: Boolean = true

    override fun toText(v: Value): String {
        val f = assertIs(v).d
        return f.toString()
    }

    override fun add(
        v1: Value,
        v2: Value,
    ): Value {
        return FloatValue(assertIsFloat(v1) + assertIsFloat(v2))
    }

    override fun subtract(
        v1: Value,
        v2: Value,
    ): Value {
        return FloatValue(assertIsFloat(v1) - assertIsFloat(v2))
    }

    override fun mult(
        v1: Value,
        v2: Value,
    ): Value {
        return FloatValue(assertIsFloat(v1) * assertIsFloat(v2))
    }

    override fun div(
        v1: Value,
        v2: Value,
    ): Value {
        return FloatValue(assertIsFloat(v1) / assertIsFloat(v2))
    }

    override fun mod(
        v1: Value,
        v2: Value,
    ): Value {
        return FloatValue(assertIsFloat(v1) % assertIsFloat(v2))
    }

    override fun pow(
        v1: Value,
        v2: Value,
    ): Value {
        return FloatValue(assertIsFloat(v1).pow(assertIsFloat(v2)))
    }

    override fun equals(
        v1: Value,
        v2: Value,
    ): Boolean {
        return assertIsFloat(v1) == assertIsFloat(v2)
    }

    override fun neg(v1: Value): Value {
        return FloatValue(-assertIsFloat(v1))
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        return assertIsFloat(v1).compareTo(assertIsFloat(v2))
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveMethod<FloatValue>> by lazy {
        listOf(
            object : PrimitiveMethod<FloatValue>("isNaN",
                MethodSignature<FloatValue>(FloatValueType, emptyList(), BooleanValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                    return BooleanValue(assertIsFloat(target).isNaN())
                }
            },
            object : PrimitiveMethod<FloatValue>("truncate",
                MethodSignature<FloatValue>(FloatValueType, emptyList(), IntegerValueType)) {
                override fun execute(target: Value, args: List<Value>): Value {
                    return IntegerValue(assertIsFloat(target).toInt())
                }
            }
        )
    }
}

class FloatValue(val d: Double): Value {
    override val valueType: ValueType<FloatValue> = FloatValueType

    override fun twist(): Twist =
        Twist.obj("FloatValue",
            Twist.attr("value", d.toString()))

}
