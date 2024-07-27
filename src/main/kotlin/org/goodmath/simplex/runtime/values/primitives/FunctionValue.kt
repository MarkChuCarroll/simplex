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

import org.goodmath.simplex.ast.Definition
import org.goodmath.simplex.ast.Expr
import org.goodmath.simplex.ast.FunctionDefinition
import org.goodmath.simplex.ast.Type
import org.goodmath.simplex.ast.TypedName
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexInvalidParameterError
import org.goodmath.simplex.runtime.SimplexParameterCountError
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist
import kotlin.collections.forEach
import kotlin.collections.zip

abstract class AbstractFunctionValue: Value {
    abstract fun applyTo(args: List<Value>): Value
}

class FunctionValue(
    val returnType: Type?,
    val params: List<TypedName>,
    val localDefs: List<Definition>,
    val body: List<Expr>,
    val staticScope: Env,
    val def: FunctionDefinition? = null
): AbstractFunctionValue() {
    override val valueType: ValueType<FunctionValue> = FunctionValueType

    override fun applyTo(args: List<Value>): Value {
        val localEnv = Env(localDefs, staticScope)
        if (params.size != args.size) {
            throw SimplexEvaluationError("Incorrect number of args: expected ${params.size}, but found ${args.size}")
        }
        params.zip(args).forEach { (param, value) ->
            localEnv.addVariable(param.name, value)
        }
        var result: Value = IntegerValue(0)
        for (b in body) {
            result = b.evaluateIn(localEnv)
        }
        return result
    }

    override fun twist(): Twist =
        Twist.obj("FunctionValue",
            Twist.value("def", def),
            Twist.value("scope", staticScope))
}

object FunctionValueType: ValueType<FunctionValue>() {
    override val name: String = "Function"

    override fun isTruthy(v: Value): Boolean {
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
        val f2 = assertIs(v2)
        return v1 == f2
    }

    override fun neg(v1: Value): Value {
        throw SimplexUnsupportedOperation(name, "negation")
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        throw SimplexUnsupportedOperation(name, "ordering")
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveMethod<FunctionValue>> = emptyList()
}

object PrimitiveFunctionValueType: ValueType<PrimitiveFunctionValue>() {
    override val name: String = "PrimitiveFunction"

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override fun add(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("PrimitiveFunction", "addition")
    }

    override fun subtract(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("PrimitiveFunction", "subtraction")
    }

    override fun mult(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("PrimitiveFunction", "multiplication")
    }

    override fun div(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("PrimitiveFunction", "division")
    }

    override fun mod(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("PrimitiveFunction", "modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("PrimitiveFunction", "exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value
    ): Boolean {
        throw SimplexUnsupportedOperation("PrimitiveFunction", "comparison")
    }

    override fun neg(v1: Value): Value {
        throw SimplexUnsupportedOperation("PrimitiveFunction", "negation")
    }

    override fun compare(v1: Value, v2: Value): Int {
        throw SimplexUnsupportedOperation("PrimitiveFunction", "ordering")
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()
    override val providesOperations: List<PrimitiveMethod<PrimitiveFunctionValue>> = emptyList()
}

abstract class PrimitiveFunctionValue(
    val name: String,
    vararg val signatures: FunctionSignature
): AbstractFunctionValue() {
    abstract fun execute(args: List<Value>): Value

    override val valueType = PrimitiveFunctionValueType

    fun validateCall(args: List<Value>): ValueType<*> {
        for (sig in signatures) {
            if (sig.params.size != args.size) {
                continue
            } else {
                for ((param, arg) in sig.params.zip(args)) {
                    if (param.type != arg.valueType) {
                        throw SimplexInvalidParameterError(
                            "function $name", param.name, param.type,
                            arg.valueType
                        )
                    }
                }
                return sig.returnType
            }
        }
        throw SimplexParameterCountError(
            "function $name",
            signatures.map { it.params.size },
            args.size
        )
    }

    override fun twist(): Twist =
        Twist.obj("PrimitiveFunctionValue",
            Twist.attr("name", name),
            Twist.attr("resultType", name),
            Twist.array("signatures", signatures.toList()))

    override fun applyTo(args: List<Value>): Value {
        return execute(args)
    }
}
