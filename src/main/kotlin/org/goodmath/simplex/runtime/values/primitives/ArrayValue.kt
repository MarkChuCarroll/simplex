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
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist
import kotlin.collections.all
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.zip
import kotlin.math.min

object ArrayValueType: ValueType<ArrayValue>() {
    override val name: String = "Array"

    fun assertIsArray(v: Value): List<Value> {
        if (v is ArrayValue) {
            return v.elements
        } else {
            throw SimplexTypeError("Array", v.valueType.name)
        }
    }

    override val supportsText: Boolean = true

    override fun toText(v: Value): String {
        val array = assertIs(v).elements
        val rendered = array.map {
            if (it.valueType.supportsText) {
                it.valueType.toText(it)
            } else {
                "<<${it.valueType.name}>>"
            }
        }.joinToString(", ")
        return "[$rendered]"
    }


    override fun subscript(v1: Value, v2: Value): Value {
        val arr = assertIsArray(v1)
        val idx = assertIsInt(v2)
        return arr[idx]
    }

    override fun isTruthy(v: Value): Boolean {
        val a = assertIsArray(v)
        return a.isNotEmpty()
    }

    override fun add(
        v1: Value,
        v2: Value,
    ): Value {
        val a1 = assertIsArray(v1)
        val a2 = assertIsArray(v2)
        if (a1.size != a2.size) {
            throw SimplexEvaluationError("Cannot add arrays of different lengths")
        }

        return ArrayValue(a1.zip(a2).map { (l, r) -> l.valueType.add(l, r) })
    }

    override fun subtract(
        v1: Value,
        v2: Value,
    ): Value {
        val a1 = assertIsArray(v1)
        val a2 = assertIsArray(v2)
        if (a1.size != a2.size) {
            throw SimplexEvaluationError("Cannot add arrays of different lengths")
        }

        return ArrayValue(a1.zip(a2).map { (l, r) -> l.valueType.subtract(l, r) })
    }

    override fun mult(
        v1: Value,
        v2: Value,
    ): Value {
        val arr = assertIsArray(v1)
        return ArrayValue(arr.map { it.valueType.mult(it, v2) })
    }

    override fun div(
        v1: Value,
        v2: Value,
    ): Value {
        val arr = assertIsArray(v1)
        return ArrayValue(arr.map { it.valueType.div(it, v2) })
    }

    override fun mod(
        v1: Value,
        v2: Value,
    ): Value {
        val arr = assertIsArray(v1)
        return ArrayValue(arr.map { it.valueType.mod(it, v2) })
    }

    override fun pow(
        v1: Value,
        v2: Value,
    ): Value {
        val arr = assertIsArray(v1)
        return ArrayValue(arr.map { it.valueType.pow(it, v2) })

    }

    override fun equals(
        v1: Value,
        v2: Value,
    ): Boolean {
        val a1 = assertIsArray(v1)
        val a2 = assertIsArray(v2)
        if (a1.size != a2.size) {
            return false
        }
        return a1.zip(a2).all { (l, r) -> l.valueType.equals(l, r)}

    }

    override fun neg(v1: Value): Value {
        val a1 = assertIsArray(v1)
        return ArrayValue(a1.map { it.valueType.neg(it) })
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        // Basically doing a lexicographic ordering.
        val a1 = assertIsArray(v1)
        val a2 = assertIsArray(v2)
        val commonLength = min(a1.size, a2.size)
        for (i in 0..<commonLength) {
            val c = a1[i].valueType.compare(a1[i], a2[i])
            if (c != 0) {
                return c
            }
        }
        // If the elements up to the common length were equal, then the longer
        // list is greater.
        return if (a1.size > a2.size) {
            1
        } else if (a1.size < a2.size) {
            -1
        } else {
            0
        }
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveMethod<ArrayValue>> by lazy {
        listOf(
            ArrayLength
        )
    }
}

object ArrayLength: PrimitiveMethod<ArrayValue>("length",
    MethodSignature<ArrayValue>(ArrayValueType, emptyList(), IntegerValueType)) {

    override fun execute(target: Value, args: List<Value>): Value {
        val a = ArrayValueType.assertIsArray(target)
        return IntegerValue(a.size)
    }
}


class ArrayValue(val elements: List<Value>): Value {
    fun isEmpty(): Boolean = elements.isEmpty()
    override val valueType: ValueType<ArrayValue> = ArrayValueType


    override fun twist(): Twist =
        Twist.obj("ArrayValue",
            Twist.array("elements", elements))


}
