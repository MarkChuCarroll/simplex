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
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist

object BooleanValueType: ValueType<BooleanValue>() {
    override val name: String = "Boolean"

    override fun isTruthy(v: Value): Boolean {
        return assertIsBoolean(v)
    }

    override fun add(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "addition")
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
        throw SimplexUnsupportedOperation(name, "multiplication")
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
        return assertIsBoolean(v1) == assertIsBoolean(v2)
    }

    override fun neg(v1: Value): Value {
        return BooleanValue(!assertIsBoolean(v1))
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        throw SimplexUnsupportedOperation(name, "ordering")
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveMethod> = emptyList()
}

class BooleanValue(val b: Boolean): Value {
    override val valueType: ValueType<BooleanValue> = BooleanValueType

    override fun twist(): Twist =
        Twist.obj("BooleanValue",
            Twist.attr("value", b.toString())
            )
}
