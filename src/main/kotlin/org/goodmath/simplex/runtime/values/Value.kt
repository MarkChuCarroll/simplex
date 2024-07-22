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
package org.goodmath.simplex.runtime.values

import org.goodmath.simplex.runtime.SimplexParameterCountError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.StringValue
import org.goodmath.simplex.twist.Twistable
import kotlin.collections.associateBy

/**
 * The abstract supertype of all values.
 * The only thing that all values need to be able to do
 * is report their value type.
 */
interface Value: Twistable {
    val valueType: ValueType<*>
}

/**
 * A value type. This provides implementations of
 * the basic arithmetic and comparison operations
 * for the type's values.
 */
abstract class ValueType<T: Value> {
    abstract val name: String

    /**
     * Should a value of the type be considered true for
     * conditionals?
     */
    abstract fun isTruthy(v: Value): Boolean

    abstract fun add(v1: Value, v2: Value): Value
    abstract fun subtract(v1: Value, v2: Value): Value
    abstract fun mult(v1: Value, v2: Value): Value
    abstract fun div(v1: Value, v2: Value): Value
    abstract fun mod(v1: Value, v2: Value): Value
    abstract fun pow(v1: Value, v2: Value): Value
    abstract fun equals(v1: Value, v2: Value): Boolean
    abstract fun neg(v1: Value): Value
    open fun subscript(v1: Value, v2: Value): Value {
        throw SimplexUnsupportedOperation(name, "subscripting")
    }
    abstract fun compare(v1: Value, v2: Value): Int
    abstract val providesFunctions: List<PrimitiveFunctionValue>
    abstract val providesOperations: List<PrimitiveMethod>

    val operations by lazy {
        providesOperations.associateBy { it.name }
    }

    fun getOperation(name: String): PrimitiveMethod {
        return operations[name] ?: throw SimplexUndefinedError(name, "method")
    }

    fun assertIsString(v: Value): String {
        if (v !is StringValue) {
            throw SimplexTypeError("String", v.valueType.name)
        } else {
            return v.s
        }
    }

    fun assertIsInt(v: Value): Int {
        if (v !is IntegerValue) {
            throw SimplexTypeError("Int", v.valueType.name)
        } else {
            return v.i
        }
    }

    fun assertArity(args: List<Value>, n: Int) {
        if (args.size != n) {
            throw SimplexParameterCountError(n, args.size)
        }
    }

    fun assertIsFloat(v: Value): Double {
        if (v !is FloatValue) {
            throw SimplexTypeError("Float", v.valueType.name)
        } else {
            return v.d
        }
    }

    fun assertIsBoolean(v: Value): Boolean {
        if (v !is BooleanValue) {
            throw SimplexTypeError("Boolean", v.valueType.name)
        } else {
            return v.b
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun assertIs(v: Value, t: ValueType<*>): T {
        if (v.valueType != t) {
            throw SimplexTypeError(t.name, v.valueType.name)
        } else {
            return v as T
        }
    }
}

class PrimitiveMethod(
    val name: String,
    val args: List<ValueType<*>>,
    val result: ValueType<*>,
    val execute: (target: Value, args: List<Value>) -> Value)

