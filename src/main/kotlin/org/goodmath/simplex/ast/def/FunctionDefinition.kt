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
package org.goodmath.simplex.ast.def

import kotlin.collections.last
import kotlin.collections.map
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.expr.Expr
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.types.TypedName
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.values.primitives.FunctionValue
import org.goodmath.simplex.twist.Twist

sealed class InvokableDefinition(
    name: String,
    val returnType: Type,
    val params: List<TypedName>,
    val body: List<Expr>,
    loc: Location,
    val localDefs: List<FunctionDefinition> = emptyList()
) : Definition(name, loc) {

    fun validateParamsAndBody(localEnv: Env) {
        for (p in params) {
            localEnv.declareTypeOf(p.name, p.type)
        }
        for (l in localDefs) {
            localEnv.declareTypeOf(l.name, l.type)
        }
        for (b in body) {
            b.validate(localEnv)
        }
        val actualReturnType = body.last().resultType(localEnv)
        if (!returnType.matchedBy(actualReturnType)) {
            throw SimplexTypeError(
                body.last().toString(),
                returnType.toString(),
                actualReturnType.toString(),
                location = loc,
            )
        }
    }
}

/**
 * A function definition.
 *
 * @param name
 * @param params a list of the function's parameters, with optional types.
 * @param localDefs a list of local definitions declared within the function.
 * @param body the function body.
 * @param loc the source location.
 */
class FunctionDefinition(
    name: String,
    returnType: Type,
    params: List<TypedName>,
    localDefs: List<FunctionDefinition>,
    body: List<Expr>,
    loc: Location,
) : InvokableDefinition(name, returnType, params, body, loc, localDefs) {

    val type = Type.function(listOf(params.map { it.type }), returnType)

    override fun twist(): Twist =
        Twist.obj(
            "FunctionDefinition",
            Twist.attr("name", name),
            Twist.array("params", params),
            Twist.array("localDefs", localDefs),
            Twist.array("body", body),
        )

    override fun installStatic(env: Env) {
        env.declareTypeOf(name, type)
    }

    override fun installValues(env: Env) {
        val funValue = FunctionValue(returnType, params, localDefs, body, env, this)
        env.addVariable(name, funValue)
    }

    override fun validate(env: Env) {
        val functionEnv = Env(localDefs, env)
        functionEnv.installStaticDefinitions()
        validateParamsAndBody(functionEnv)
    }
}
