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

import kotlin.collections.forEach
import kotlin.collections.zip
import org.goodmath.simplex.ast.def.Definition
import org.goodmath.simplex.ast.def.FunctionDefinition
import org.goodmath.simplex.ast.expr.Expr
import org.goodmath.simplex.ast.types.FunctionType
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.types.TypedName
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist

abstract class AbstractFunctionValue : Value {
    abstract fun applyTo(args: List<Value>): Value
}

class FunctionValue(
    val returnType: Type,
    val params: List<TypedName>,
    val localDefs: List<Definition>,
    val body: List<Expr>,
    val staticScope: Env,
    val def: FunctionDefinition? = null,
) : AbstractFunctionValue() {
    override val valueType: ValueType =
        FunctionValueType(Type.function(listOf(params.map { it.type }), returnType))

    override fun applyTo(args: List<Value>): Value {
        val localEnv = Env(localDefs, staticScope)
        localEnv.installStaticDefinitions()
        localEnv.installDefinitionValues()
        if (params.size != args.size) {
            throw SimplexEvaluationError(
                "Incorrect number of args: expected ${params.size}, but found ${args.size}"
            )
        }
        params.zip(args).forEach { (param, value) -> localEnv.addVariable(param.name, value) }
        var result: Value = IntegerValue(0)
        for (b in body) {
            result = b.evaluateIn(localEnv)
        }
        return result
    }

    override fun twist(): Twist =
        Twist.obj("FunctionValue", Twist.value("def", def), Twist.value("scope", staticScope))
}

open class FunctionValueType(val type: FunctionType) : ValueType() {
    override val name: String = "Function($type)"

    override val asType: Type = type

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesPrimitiveMethods: List<PrimitiveMethod> = emptyList()
    override val providesVariables: Map<String, Value> = emptyMap()

    override fun assertIs(v: Value): AbstractFunctionValue {
        if (v is FunctionValue) {
            return v
        } else {
            throw SimplexTypeError(v.valueType.asType.toString(), this.toString())
        }
    }
}

class PrimitiveFunctionValueType(ft: FunctionType) : FunctionValueType(ft) {
    override val name: String = "PrimitiveFunction($type)"
    override val asType: Type by lazy {
        ft
    }

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()
    override val providesPrimitiveMethods: List<PrimitiveMethod> = emptyList()
    override val providesVariables: Map<String, Value> = emptyMap()

    override fun assertIs(v: Value): PrimitiveFunctionValue {
        if (v is PrimitiveFunctionValue) {
            return v
        } else {
            throw SimplexTypeError(v.valueType.asType.toString(), this.toString())
        }
    }
}

/**
 * To keep the syntax of the manifold bindings clean, Simplex needs to support some form of
 * overloading on function signatures. To keep them as simple as possible, I'm opting to allow a
 * primitive function to support multiple parameter lists, but it must always return the same value
 * type.
 */
abstract class PrimitiveFunctionValue(val name: String, val signature: FunctionSignature) :
    AbstractFunctionValue() {

    val resultType: Type = signature.returnType

    abstract fun execute(args: List<Value>): Value

    override val valueType by lazy {
        PrimitiveFunctionValueType(Type.function(signature.params.map {
            pOpt -> pOpt.map { it.type} },
            signature.returnType))
    }

    override fun twist(): Twist =
        Twist.obj(
            "PrimitiveFunctionValue",
            Twist.attr("name", name),
            Twist.attr("resultType", name),
            Twist.value("signature", signature),
        )

    override fun applyTo(args: List<Value>): Value {
        return execute(args)
    }
}
