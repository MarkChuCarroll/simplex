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

import java.util.ArrayList
import kotlin.collections.first
import kotlin.collections.last
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.types.VectorType
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexAnalysisError
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.VectorValue
import org.goodmath.simplex.runtime.values.primitives.VectorValueType
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.twist.Twist

class LoopExpr(val idxVar: String, val collExpr: Expr, val body: List<Expr>, loc: Location) :
    Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "LoopExpr",
            Twist.attr("idxVar", idxVar),
            Twist.value("collection", collExpr),
            Twist.array("body", body),
        )

    override fun evaluateIn(env: Env): Value {
        val collValue = collExpr.evaluateIn(env)
        if (collValue.valueType !is VectorValueType) {
            throw SimplexEvaluationError(
                "Loops can only iterate over arrays, not ${collValue.valueType.name}",
                loc = loc,
            )
        }
        collValue as VectorValue
        if (collValue.isEmpty()) {
            return collValue
        }

        val localEnv = Env(emptyList(), env)
        localEnv.addVariable(idxVar, collValue.elements.first())
        val result = ArrayList<Value>()
        for (e in collValue.elements) {
            localEnv.updateVariable(idxVar, e)
            var iterationResult: Value = IntegerValue(0)
            for (expr in body) {
                iterationResult = expr.evaluateIn(localEnv)
            }
            result.add(iterationResult)
        }
        return VectorValue(result[0].valueType, result)
    }

    override fun resultType(env: Env): Type {
        val collectionType = collExpr.resultType(env) as VectorType
        val elementType = collectionType.elementType
        val localEnv = Env(emptyList(), env)
        localEnv.declareTypeOf(idxVar, elementType)
        return Type.vector(body.last().resultType(localEnv))
    }

    override fun validate(env: Env) {
        val collectionType = collExpr.resultType(env)
        if (collectionType !is VectorType) {
            throw SimplexAnalysisError(
                "Target of a loop must be an array, not $collectionType",
                loc = loc,
            )
        }
        val elementType = collectionType.elementType
        val localEnv = Env(emptyList(), env)
        localEnv.declareTypeOf(idxVar, elementType)
        for (e in body) {
            e.validate(localEnv)
        }
    }
}

class WhileExpr(val cond: Expr, val body: List<Expr>, loc: Location) : Expr(loc) {
    override fun evaluateIn(env: Env): Value {
        var condValue = cond.evaluateIn(env)
        var result: Value = BooleanValue(false)
        while (condValue.valueType.isTruthy(condValue)) {
            for (expr in body) {
                result = expr.evaluateIn(env)
            }
            condValue = cond.evaluateIn(env)
        }
        return result
    }

    override fun resultType(env: Env): Type {
        return body.last().resultType(env)
    }

    override fun validate(env: Env) {
        cond.validate(env)
        for (expr in body) {
            expr.validate(env)
        }
    }

    override fun twist(): Twist =
        Twist.obj("WhileStmt", Twist.value("condition", cond), Twist.array("body", body))
}
