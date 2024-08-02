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
package org.goodmath.simplex.ast

import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexError
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexParameterCountError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.values.MethodValue
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.FunctionValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.TupleValueType
import org.goodmath.simplex.twist.Twist

/**
 * The supertype of all declarations.
 * @param name the name of the declarations.
 * @param loc the source location
 */
abstract class Definition(val name: String, loc: Location): AstNode(loc) {
    abstract fun installStatic(env: Env)
    abstract fun installValues(env: Env)
    abstract fun validate(env: Env)
}

/**
 * A name with an optional type declaration, used in several places
 * in the code.
 */
class TypedName(val name: String, val type: Type, loc: Location): AstNode(loc) {
    override fun twist(): Twist =
        Twist.obj("TypedName",
            Twist.attr("name", name),
            Twist.value("type", type))

    override fun toString(): String {
        return "TypedName(${name}:${type})"
    }
}

/**
 * A function definition.
 * @param name
 * @param params a list of the function's parameters, with optional types.
 * @param localDefs a list of local definitions declared within the function.
 * @param body the function body.
 * @param loc the source location.
 */
class FunctionDefinition(name: String,
                         val returnType: Type,
                         val params: List<TypedName>,
    val localDefs: List<Definition>,
    val body: List<Expr>,
    loc: Location): Definition(name, loc) {

    val type = Type.function(params.map { it.type }, returnType)

    override fun twist(): Twist =
        Twist.obj(
            "FunctionDefinition",
            Twist.attr("name", name),
            Twist.array("params", params),
            Twist.array("localDefs", localDefs),
            Twist.array("body", body)
        )

    override fun installStatic(env: Env) {
        env.declareTypeOf(name, type)
    }

    override fun installValues(env: Env) {
        val funValue = FunctionValue(
            returnType,
            params,
            localDefs,
            body,
            env,
            this
        )
        env.addVariable(name, funValue)
    }

    override fun validate(env: Env) {
        val functionEnv = Env(localDefs, env)
        functionEnv.installStaticDefinitions()
        for (p in params) {
            functionEnv.declareTypeOf(p.name, p.type)
        }
        for (b in body) {
            b.validate(functionEnv)
        }
        val actualReturnType = body.last().resultType(functionEnv)
        if (!returnType.matchedBy(actualReturnType)) {
            throw SimplexTypeError(returnType.toString(), actualReturnType.toString(),
                location = loc)
        }
    }
}

class MethodDefinition(
    val targetType: Type,
    val methodName: String,
    val params: List<TypedName>,
    val resultType: Type,
    val body: List<Expr>,
    loc: Location): Definition("${targetType}->name", loc) {
    override fun installValues(env: Env) {
        val valueType = env.getType(targetType.toString())
        valueType.addMethod(MethodValue(targetType, resultType, params, body, this))

    }

    override fun validate(env: Env) {
        val methodEnv = Env(emptyList(), env)
        methodEnv.declareTypeOf("self", targetType)
        for (p in params) {
            methodEnv.declareTypeOf(p.name, p.type)
        }

        for (b in body) {
            b.validate(methodEnv)
        }
        val actualReturnType = body.last().resultType(methodEnv)
        if (!resultType.matchedBy(actualReturnType)) {
            throw SimplexTypeError(resultType.toString(), actualReturnType.toString(),
                location = loc)
        }
    }

    override fun installStatic(env: Env) {
        targetType.registerMethod(name, Type.method(targetType, params.map { it.type }, resultType) as MethodType)
    }

    override fun twist(): Twist =
        Twist.obj("MethodDefinition",
            Twist.value("targetType", targetType),
            Twist.attr("name", methodName),
            Twist.array("params", params),
            Twist.value("resultType", resultType),
            Twist.array("body", body))

    fun applyTo(target: Value, args: List<Value>, env: Env): Value {
        val localEnv = Env(emptyList(), env)
        localEnv.addVariable("self", target)
        if (params.size != args.size) {
            throw SimplexParameterCountError("method ${targetType}.${methodName}", listOf(params.size), args.size,
                loc)
        }
        params.zip(args).map { (param, arg) ->
            localEnv.addVariable(param.name, arg)
        }
        var result: Value = IntegerValue(0)
        for (expr in body) {
            result = expr.evaluateIn(localEnv)
        }
        return result
    }

}


class TupleDefinition(name: String, val fields: List<TypedName>,
    loc: Location): Definition(name, loc) {

    val valueType =  TupleValueType(this)
    override fun twist(): Twist =
        Twist.obj("TupleDefinition",
            Twist.attr("name", name),
            Twist.array("fields", fields))

    override fun installValues(env: Env) {
    }

    override fun validate(env: Env) {
        // Nothing to check here.
    }

    fun indexOf(fieldName: String): Int {
        val idx = fields.indexOfFirst { it.name == fieldName }
        if (idx < 0) {
            throw SimplexUndefinedError(fieldName, "tuple field of $name", loc=loc)
        } else {
            return idx
        }
    }

    override fun installStatic(env: Env) {
        val tupleType = TupleValueType(this)
        env.registerType(name, tupleType)
        Type.simple(name)
    }

}

class VariableDefinition(name: String, val type: Type,
                         val initialValue: Expr, loc: Location):
        Definition(name, loc) {
    override fun twist(): Twist =
        Twist.obj("VariableDefinition",
            Twist.attr("name", name),
            Twist.value("type", type),
            Twist.value("value", initialValue))

    override fun installStatic(env: Env) {
        env.declareTypeOf(name, type)
    }

    override fun installValues(env: Env) {
        try {
            val v = initialValue.evaluateIn(env)
            env.addVariable(name, v)
        } catch (e: Exception) {
            if (e is SimplexError) {
                if (e.location == null) {
                    e.location = loc
                }
                throw e
            } else {
                throw SimplexEvaluationError("Evaluation error in variable definition", cause=e,
                    loc=loc)
            }
        }

    }

    override fun validate(env: Env) {
        val actualType = initialValue.resultType(env)
        if (!type.matchedBy(actualType)) {
            throw SimplexTypeError(type.toString(), actualType.toString(), location = loc)
        }
    }
}
