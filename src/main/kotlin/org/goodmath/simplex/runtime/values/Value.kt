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

import org.goodmath.simplex.ast.ArrayType
import org.goodmath.simplex.ast.MethodDefinition
import org.goodmath.simplex.ast.SimpleType
import org.goodmath.simplex.ast.Type
import org.goodmath.simplex.runtime.RootEnv
import org.goodmath.simplex.runtime.SimplexInvalidParameterError
import org.goodmath.simplex.runtime.SimplexParameterCountError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.primitives.ArrayValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.StringValue
import org.goodmath.simplex.twist.Twist
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
abstract class ValueType<T: Value>: Twistable {
    abstract val name: String

    open val asType: Type by lazy {
        SimpleType(name)
    }

    fun satisfiesConstraint(t: Type): Boolean {
        val vt = RootEnv.getType(t)
        return if (vt == AnyType) {
            true
        } else if (t is ArrayType && this is ArrayValueType) {
            elementType.satisfiesConstraint(t.elementType)
        } else {
            vt == this
        }
    }

    open val supportsText: Boolean = false

    open fun toText(v: Value): String {
        throw SimplexUnsupportedOperation(name, "render_pretty")
    }

    override fun twist(): Twist {
        return Twist.attr("ValueType", name)
    }

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
    abstract val providesOperations: List<PrimitiveMethod<T>>

    protected val methods: HashMap<String, MethodDefinition> = HashMap()

    fun getMethod(name: String): MethodDefinition {
        return methods[name] ?: throw SimplexUndefinedError(name, "method")
    }

    fun addMethod(method: MethodDefinition) {
        methods[method.methodName] = method
    }

    val primitiveMethods by lazy {
        providesOperations.associateBy { it.name }
    }

    fun hasPrimitiveMethod(name: String): Boolean {
        return primitiveMethods.containsKey(name)
    }

    fun getPrimitiveMethod(name: String): PrimitiveMethod<T> {
        return primitiveMethods[name] ?: throw SimplexUndefinedError(name, "method")
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
    open fun assertIs(v: Value): T {
        if (v.valueType != this) {
            throw SimplexTypeError(name, v.valueType.name)
        } else {
            return v as T
        }
    }
}

data class FunctionSignature(
    val params: List<Param>,
    val returnType: ValueType<*>
): Twistable {
    override fun twist(): Twist =
        Twist.obj("FunctionSignature",
            Twist.array("params", params),
            Twist.value("resultType", returnType)
        )

}

data class Param(val name: String, val type: ValueType<*>): Twistable {
    override fun twist(): Twist =
        Twist.obj("Param",
            Twist.attr("name", name),
            Twist.attr("type", type.name))
}

data class MethodSignature<T: Value>(
    val self: ValueType<T>,
    val params: List<Param>,
    val returnType: ValueType<*>
): Twistable {
    override fun twist(): Twist =
        Twist.obj("MethodSig",
            Twist.value("selfType", self),
            Twist.array("params", params),
            Twist.value("returnType", returnType))

}

abstract class PrimitiveMethod<T: Value>(
    val name: String,
    vararg val signatures: MethodSignature<T>) {
    abstract fun execute(target: Value, args: List<Value>): Value
    fun validateCall(selfValue: Value,
                     argValues: List<Value>): ValueType<*> {
        for (sig in signatures) {
            if (selfValue.valueType != sig.self || argValues.size != sig.params.size) {
                continue
            } else { // if the arity is correct, then the parameter types
                // must match.
                for ((param, arg) in sig.params.zip(argValues)) {
                    if (param.type != arg.valueType) {
                        throw SimplexInvalidParameterError(
                            "method $name", param.name, param.type,
                            arg.valueType
                        )
                    }
                }
                return sig.returnType
            }
        }
        throw SimplexParameterCountError("method $name",
            signatures.map { it.params.size },
            argValues.size)
    }
}

object AnyType: ValueType<Value>() {
    override val name: String = "Any"
    override fun assertIs(v: Value): Value {
        return v
    }

    override fun isTruthy(v: Value): Boolean {
        return v.valueType.isTruthy(v)
    }

    override fun add(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("Any", "addition")
    }

    override fun subtract(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("Any", "subtraction")
    }

    override fun mult(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("Any", "multiplication")
    }

    override fun div(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("Any", "division")
    }

    override fun mod(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("Any", "modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("Any", "exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value
    ): Boolean {
        return v1 == v2
    }

    override fun neg(v1: Value): Value {
        throw SimplexUnsupportedOperation("Any", "negation")
    }

    override fun compare(
        v1: Value,
        v2: Value
    ): Int {
        throw SimplexUnsupportedOperation("Any", "ordering")
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveMethod<Value>> = emptyList()

}
