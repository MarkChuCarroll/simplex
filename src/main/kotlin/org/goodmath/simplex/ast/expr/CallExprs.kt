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
import org.goodmath.simplex.ast.types.FunctionType
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexAnalysisError
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexParameterCountError
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.SimplexUndefinedMethodError
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.AbstractFunctionValue
import org.goodmath.simplex.twist.Twist

class FunCallExpr(val funExpr: Expr, val argExprs: List<Expr>, loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "FunCallExpr",
            Twist.value("functionExpr", funExpr),
            Twist.array("args", argExprs),
        )

    override fun evaluateIn(env: Env): Value {
        val funVal = funExpr.evaluateIn(env)
        if (funVal !is AbstractFunctionValue) {
            throw SimplexEvaluationError(
                "Only a function can be invoked, not ${funVal.valueType.name}",
                loc = loc,
            )
        }
        val args = argExprs.map { it.evaluateIn(env) }
        return funVal.applyTo(args)
    }

    override fun resultType(env: Env): Type {
        val funType = funExpr.resultType(env)
        if (funType !is FunctionType) {
            throw SimplexAnalysisError("Function expression isn't a function", loc = loc)
        } else {
            return funType.returnType
        }
    }

    override fun validate(env: Env) {
        val funType = funExpr.resultType(env)
        if (funType !is FunctionType) {
            throw SimplexAnalysisError("Function expression isn't a function", loc = loc)
        }
        val expectedArgs =  funType.argLists.firstOrNull { it.size == argExprs.size }
        if (expectedArgs == null) {
            throw SimplexParameterCountError(
                "Function call",
                funType.argLists.joinToString(" | ") { args -> args.toString() },
                        argExprs.map { it.resultType(env) }.joinToString(),
                loc,
            )
        }
        if (!expectedArgs.zip(argExprs).all { (type, expr) ->
                    type.matchedBy(expr.resultType(env))
                }) {

            throw SimplexAnalysisError("No function signatures matched", loc = loc)
        }
    }
}

class MethodCallExpr(val target: Expr, val name: String, val args: List<Expr>, loc: Location) :
    Expr(loc) {
    override fun evaluateIn(env: Env): Value {
        val targetValue = target.evaluateIn(env)
        val argValues = args.map { it.evaluateIn(env) }
        val meth = targetValue.valueType.getMethod(name)
        return meth.applyTo(targetValue, argValues, env)
    }

    override fun resultType(env: Env): Type {
        val targetType = target.resultType(env)
        return targetType.getMethod(name)?.returnType
            ?: throw SimplexUndefinedMethodError(name,  targetType.toString(), loc = loc)
    }

    override fun validate(env: Env) {
        val targetType = target.resultType(env)
        val methodType =
            targetType.getMethod(name)
                ?: throw SimplexUndefinedMethodError(name, targetType.toString(), loc = loc)

        val expectedArgs = methodType.argSets.firstOrNull { it.size == args.size }
        if (expectedArgs == null) {
            throw SimplexParameterCountError("Method $name",
                methodType.argSets.joinToString(" | ") { args ->
                    args.toString()
                },
                args.joinToString { it.resultType(env).toString() },
                location = loc)
        }

        if (!expectedArgs.zip(args).all { (expected, expr) ->
                            expected.matchedBy(expr.resultType(env))
                        }) {
            throw SimplexAnalysisError("Method $name does not accept an argument list of ${
                args.joinToString(", ") {
                    it.resultType(
                        env
                    ).toString()
                }
            }")
        }
    }


    override fun twist(): Twist =
        Twist.obj(
            "MethodExpr",
            Twist.value("target", target),
            Twist.attr("name", name),
            Twist.array("args", args),
        )
}
