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

import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.expr.Expr
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexError
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.twist.Twist

class VariableDefinition(name: String, val type: Type?, val initialValue: Expr, loc: Location) :
    Definition(name, loc) {
    override fun twist(): Twist =
        Twist.obj(
            "VariableDefinition",
            Twist.attr("name", name),
            Twist.value("type", type),
            Twist.value("value", initialValue),
        )

    override fun installStatic(env: Env) {
        val declareType = type ?: initialValue.resultType(env)
        env.declareTypeOf(name, declareType)
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
                throw SimplexEvaluationError(
                    "Evaluation error in variable definition",
                    cause = e,
                    loc = loc,
                )
            }
        }
    }

    override fun validate(env: Env) {
        initialValue.validate(env)
        val actualType = initialValue.resultType(env)
        if (type != null && !type.matchedBy(actualType)) {
            throw SimplexTypeError(initialValue.toString(), type.toString(), actualType.toString(), location = loc)
        }
    }
}
