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

import org.goodmath.simplex.ast.TupleDefinition
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist
import kotlin.collections.all
import kotlin.collections.zip

object TupleValueType: ValueType<TupleValue>() {
    fun assertIsTuple(v: Value): TupleValue {
        if (v !is TupleValue) {
            throw SimplexTypeError("Tuple", v.valueType.name)
        } else {
            return v
        }
    }

    override val name: String = "Tuple"

    override fun isTruthy(v: Value): Boolean {
        assertIsTuple(v)
        return true
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
        val t1 = assertIsTuple(v1)
        val t2 = assertIsTuple(v2)
        if (t1.tupleDef != t2.tupleDef) {
            return false
        }
        return t1.fields.zip(t2.fields).all { (l, r) -> l == r }

    }

    override fun neg(v1: Value): Value {
        throw SimplexUnsupportedOperation(name, "negation")
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        val t1 = assertIsTuple(v1)
        val t2 = assertIsTuple(v2)
        if (t1.tupleDef != t2.tupleDef) {
            throw SimplexEvaluationError("Cannot compare different tuple types")
        }
        for ((l, r) in t1.fields.zip(t2.fields)) {
            val c = l.valueType.compare(l, r)
            if (c != 0) {
                return c
            }
        }
        return 0
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveMethod> = emptyList()
}

class TupleValue(val tupleDef: TupleDefinition, val fields: List<Value>): Value {
    override val valueType: ValueType<*> = TupleValueType


    override fun twist(): Twist =
        Twist.obj("TupleValue",
            Twist.attr("name", tupleDef.name),
            Twist.array("fields", fields))

}
