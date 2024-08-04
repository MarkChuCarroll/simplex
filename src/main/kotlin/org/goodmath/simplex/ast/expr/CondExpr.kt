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
package org.goodmath.simplex.ast.expr

import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexAnalysisError
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.twist.Twistable
import org.goodmath.simplex.twist.Twist
import kotlin.collections.first
import kotlin.collections.map
import kotlin.collections.toMutableSet

data class Condition(val cond: Expr, val value: Expr) : Twistable {
    override fun twist(): Twist =
        Twist.obj(
            "Condition",
            Twist.value("if", cond),
            Twist.value("then", value)
        )
}

class CondExpr(
    val conds: List<Condition>, val elseClause: Expr,
    loc: Location
) : Expr(loc) {

    override fun twist(): Twist =
        Twist.obj(
            "IfExpr",
            Twist.array("cond_clauses", conds),
            Twist.value("else", elseClause)
        )

    override fun evaluateIn(env: Env): Value {
        for (cond in conds) {
            val v = cond.cond.evaluateIn(env)
            if (v.valueType.isTruthy(v)) {
                return cond.value.evaluateIn(env)
            }
        }
        return elseClause.evaluateIn(env)
    }

    override fun resultType(env: Env): Type {
        val condTypes = conds.map { cond -> cond.value.resultType(env) }.toMutableSet()
        condTypes.add(elseClause.resultType(env))
        return if (condTypes.size > 1) {
            throw SimplexAnalysisError("Cond clauses return different types: $condTypes", loc = loc)
        } else {
            condTypes.first()
        }
    }

    override fun validate(env: Env) {
        for (cond in conds){
            cond.cond.validate(env)
            cond.cond.validate(env)
        }
        elseClause.validate(env)
        val resultTypes = conds.map { it.value.resultType(env) }.toMutableSet()
        resultTypes.add(elseClause.resultType(env))
        if (resultTypes.size > 1) {
            throw SimplexAnalysisError("Cond clauses do not return the same type: $resultTypes", loc = loc)
        }
    }
}
