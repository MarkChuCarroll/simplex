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
import org.goodmath.simplex.ast.types.ArgumentListSpec
import org.goodmath.simplex.ast.types.FunctionType
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexAnalysisError
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexParameterCountError
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.AbstractFunctionValue
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable

data class Arguments(val positionalArgs: List<Expr>,
                     val kwArgs: Map<String, Expr>): Twistable {
    override fun twist(): Twist =
        Twist.obj("ArgumentList",
            Twist.array("positional", positionalArgs),
            Twist.array("kw",
                kwArgs.map { Twist.value(it.key, it.value)})
            )
}

class FunCallExpr(val funExpr: Expr,
                  val arguments: Arguments,
                  loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "FunCallExpr",
            Twist.value("functionExpr", funExpr),
            Twist.value("args", arguments),
        )

    override fun evaluateIn(env: Env): Value {
        val funVal = funExpr.evaluateIn(env)
        if (funVal !is AbstractFunctionValue) {
            throw SimplexEvaluationError(
                "Only a function can be invoked, not ${funVal.valueType.name}",
                loc = loc,
            )
        }
        val positionalArgs =
            arguments.positionalArgs.map { it.evaluateIn(env) }
        val kwArgs = arguments.kwArgs.map { (name, expr) ->  name to expr.evaluateIn(env) }.associate { it.first to it.second }
        return funVal.applyTo(positionalArgs, kwArgs)
    }

    override fun resultType(env: Env): Type {
        val funType = funExpr.resultType(env)
        if (funType !is FunctionType) {
            throw SimplexAnalysisError("Function expression isn't a function", loc = loc)
        } else {
            return funType.returnType
        }
    }

    fun findMatchingArgumentSpec(args: Arguments, env: Env): ArgumentListSpec {
        val funType = funExpr.resultType(env)
        if (funType !is FunctionType) {
            throw SimplexAnalysisError("Function expression isn't a function", loc = loc)
        }
        return funType.argOptions.firstOrNull {
            it.matchedBy(args, env)
        } ?: throw SimplexAnalysisError("No matching parameter spec found", loc = loc)
    }

    override fun validate(env: Env) {
        val funType = funExpr.resultType(env)
        if (funType !is FunctionType) {
            throw SimplexAnalysisError("Function expression isn't a function", loc = loc)
        }
        findMatchingArgumentSpec(arguments, env)
    }
}

class MethodCallExpr(val target: Expr, val name: String, val args: Arguments, loc: Location) :
    Expr(loc) {
    override fun evaluateIn(env: Env): Value {
        val targetValue = target.evaluateIn(env)
        val meth = targetValue.valueType.getMethod(name)
        val positionalArgs = args.positionalArgs.map { it.evaluateIn(env) }
        val kwArgs = args.kwArgs.map { (name, valueExpr) -> name to valueExpr.evaluateIn(env) }.associate { it.first to it.second }
        return meth.applyTo(targetValue, positionalArgs, kwArgs, env)
    }

    fun findMatchingArgumentSpec(args: Arguments, env: Env): ArgumentListSpec {
        val argSpecs = target.resultType(env).getMethod(name)?.argSets ?: throw SimplexAnalysisError("No matching method found", loc = loc)
        return argSpecs.firstOrNull {
            it.matchedBy(args, env)
        } ?: throw SimplexAnalysisError("No matching parameter spec found", loc = loc)
    }


    override fun resultType(env: Env): Type {
        val targetType = target.resultType(env)
        return targetType.getMethod(name)?.returnType
            ?: throw SimplexUndefinedError(name, "method of ${this.name}", loc = loc)
    }

    override fun validate(env: Env) {
        val targetType = target.resultType(env)
        val methodType =
            targetType.getMethod(name)
                ?: throw SimplexUndefinedError(name, "method of $targetType", loc = loc)
        findMatchingArgumentSpec(args, env)
    }


    override fun twist(): Twist =
        Twist.obj(
            "MethodExpr",
            Twist.value("target", target),
            Twist.attr("name", name),
            Twist.value("args", args),
        )
}
