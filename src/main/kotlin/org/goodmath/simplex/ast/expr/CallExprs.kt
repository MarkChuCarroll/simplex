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

import org.goodmath.simplex.ast.FunctionType
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexAnalysisError
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.AbstractFunctionValue
import org.goodmath.simplex.twist.Twist


class FunCallExpr(val funExpr: Expr, val argExprs: List<Expr>, loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "FunCallExpr",
            Twist.value("functionExpr", funExpr),
            Twist.array("args", argExprs)
        )

    override fun evaluateIn(env: Env): Value {
        val funVal = funExpr.evaluateIn(env)
        if (funVal !is AbstractFunctionValue) {
            throw SimplexEvaluationError("Only a function can be invoked, not ${funVal.valueType.name}", loc = loc)
        }
        val args = argExprs.map { it.evaluateIn(env) }
        return funVal.applyTo(args)
    }

    override fun resultType(env: Env): Type {
        val funType = funExpr.resultType(env)
        if (funType !is FunctionType) {
            throw SimplexAnalysisError(
                "Function expression isn't a function",
                loc = loc
            )
        } else {
            return funType.returnType
        }
    }

    override fun validate(env: Env) {
        val funType = funExpr.resultType(env)
        if (funType !is FunctionType) {
            throw SimplexAnalysisError(
                "Function expression isn't a function",
                loc = loc
            )
        }
        if (funType.args.size != argExprs.size) {
            throw SimplexAnalysisError(
                "Function call expected ${funType.args.size} args, but received ${argExprs.size}",
                loc = loc
            )
        }
        funType.args.zip(argExprs).forEach { (type, expr) ->
            if (!type.matchedBy(expr.resultType(env))) {
                throw SimplexTypeError(type.toString(), expr.resultType(env).toString(), location = expr.loc)
            }
        }

    }
}



class MethodCallExpr(
    val target: Expr, val name: String, val args: List<Expr>,
    loc: Location
) : Expr(loc) {
    override fun evaluateIn(env: Env): Value {
        val targetValue = target.evaluateIn(env)
        val argValues = args.map { it.evaluateIn(env) }
        val meth = targetValue.valueType.getMethod(name)
        return meth.applyTo(targetValue, argValues, env)
    }

    override fun resultType(env: Env): Type {
        val targetType = target.resultType(env)
        return targetType.getMethod(name)?.returnType ?: throw SimplexUndefinedError(
            name,
            "method of ${this.name}",
            loc = loc
        )
    }

    override fun validate(env: Env) {
        val targetType = target.resultType(env)
        val methodType = targetType.getMethod(name) ?: throw SimplexUndefinedError(
            name,
            "method of $targetType",
            loc = loc
        )
        if (args.size != methodType.args.size) {
            throw SimplexAnalysisError(
                "Method $name expected ${methodType.args.size} arguments, but received ${args.size}",
                loc = loc
            )
        }
        methodType.args.zip(args).forEach { (expected, expr) ->
            if (!expected.matchedBy(expr.resultType(env))) {
                throw SimplexTypeError(expected.toString(), expr.resultType(env).toString(), location = expr.loc)
            }
        }
    }

    override fun twist(): Twist =
        Twist.obj(
            "MethodExpr",
            Twist.value("target", target),
            Twist.attr("name", name),
            Twist.array("args", args)
        )
}

