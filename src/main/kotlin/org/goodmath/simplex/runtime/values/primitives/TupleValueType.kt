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

class TupleValueType(val tupleDef: TupleDefinition) : ValueType<TupleValue>() {
    override val name: String = tupleDef.name

    override val supportsText: Boolean = true

    override fun toText(v: Value): String {
        val tup = assertIs(v)
        val sb = StringBuilder()
        sb.append("#${tup.valueType.name}(")

        sb.append(tup.valueType.tupleDef.fields.map { field ->
            val fieldValue = tup.fields[tup.valueType.tupleDef.indexOf(field.name)]
            if (fieldValue.valueType.supportsText) {
                "${field.name}=${fieldValue.valueType.toText(fieldValue)}"
            } else {
                "${field.name}:${fieldValue.valueType.name}"
            }
        }.joinToString(", "))
        sb.append(")")
        return sb.toString()
    }

    override fun isTruthy(v: Value): Boolean {
        assertIs(v)
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
        val t1 = assertIs(v1)
        val t2 = assertIs(v2)
        if (t1.valueType != t2.valueType) {
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
        val t1 = assertIs(v1)
        val t2 = assertIs(v2)
        if (t1.valueType.tupleDef != t2.valueType.tupleDef) {
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

    override val providesOperations: List<PrimitiveMethod<TupleValue>> = emptyList()
}

class TupleValue(override val valueType: TupleValueType, val fields: List<Value>): Value {

    override fun twist(): Twist =
        Twist.obj("TupleValue",
            Twist.attr("name", valueType.name),
            Twist.array("fields", fields))

}
