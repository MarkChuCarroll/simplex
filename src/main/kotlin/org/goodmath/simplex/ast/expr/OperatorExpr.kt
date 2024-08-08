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
import org.goodmath.simplex.runtime.SimplexError
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.twist.Twist
import kotlin.collections.drop
import kotlin.collections.forEach
import kotlin.collections.zip
import kotlin.let

enum class Operator {
    Plus, Minus, Times, Div, Mod, Pow,
    Eq, Neq, Gt, Ge, Lt, Le,
    Uminus, Not, And, Or,
    Subscript;

    fun toMethod(): String? {
        return when (this) {
            Plus -> "plus"
            Minus -> "minus"
            Times -> "times"
            Div -> "div"
            Mod -> "mod"
            Pow -> "pow"
            Eq -> "eq"
            Neq -> null
            Gt -> null
            Ge -> null
            Lt -> null
            Le -> null
            Not -> "not"
            And -> null
            Or -> null
            Uminus -> "neg"
            Subscript -> "sub"
        }
    }
}

class OperatorExpr(val op: Operator, val args: List<Expr>, loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "OperatorExpr",
            Twist.attr("op", op.toString()),
            Twist.array("args", args)
        )

    override fun evaluateIn(env: Env): Value {
        try {
            val target = args[0].evaluateIn(env)
            val methodName = op.toMethod()
            if (methodName != null) {
                if (methodName == "neg") {
                    return target.valueType.applyMethod(target, methodName, emptyList(), env)
                }
                return target.valueType.applyMethod(target, methodName, listOf(args[1].evaluateIn(env)), env)
            } else {
                return when (op) {
                    Operator.Neq -> {
                        val truthy = target.valueType.applyMethod(
                            target,
                            "eq",
                            listOf(args[1].evaluateIn(env)),
                            env
                        ) as BooleanValue
                        BooleanValue(!truthy.b)
                    }
                    Operator.Gt -> {
                        val c = target.valueType.applyMethod(target, "compare", listOf(args[1].evaluateIn(env)), env)
                        BooleanValue((c as IntegerValue).i > 0)
                    }

                    Operator.Ge -> {
                        val c = target.valueType.applyMethod(target, "compare", listOf(args[1].evaluateIn(env)), env)
                        BooleanValue((c as IntegerValue).i >= 0)
                    }

                    Operator.Lt -> {
                        val c = target.valueType.applyMethod(target, "compare", listOf(args[1].evaluateIn(env)), env)
                        BooleanValue((c as IntegerValue).i < 0)
                    }

                    Operator.Le -> {
                        val c = target.valueType.applyMethod(target, "compare", listOf(args[1].evaluateIn(env)), env)
                        BooleanValue((c as IntegerValue).i < 0)
                    }

                    Operator.Not -> BooleanValue(!target.valueType.isTruthy(target))
                    Operator.And -> {
                        if (target.valueType.isTruthy(target)) {
                            val r = args[1].evaluateIn(env)
                            BooleanValue(r.valueType.isTruthy(r))
                        } else {
                            BooleanValue(false)
                        }
                    }

                    Operator.Or -> {
                        if (!target.valueType.isTruthy(target)) {
                            val r = args[1].evaluateIn(env)
                            BooleanValue(r.valueType.isTruthy(r))
                        } else {
                            BooleanValue(true)
                        }
                    }

                    else -> throw SimplexUnsupportedOperation(
                        target.valueType.asType.toString(),
                        op.toString(),
                        loc = args[0].loc
                    )
                }
            }
        } catch (t: Throwable) {
            if (t is SimplexError) {
                if (t.location == null) {
                    t.location = loc
                }
                throw t
            } else {
                throw SimplexEvaluationError(
                    "Error evaluating expression", cause = t,
                    loc = loc
                )
            }
        }
    }

    override fun validate(env: Env) {
        val target = args[0].resultType(env)
        for (arg in args) {
            arg.validate(env)
        }
        val methodName = op.toMethod() ?: (when (op) {
            Operator.Neq -> "eq"
            Operator.Gt -> "compare"
            Operator.Ge -> "compare"
            Operator.Lt -> "compare"
            Operator.Le -> "compare"
            Operator.Not -> "isTruthy"
            Operator.And -> "isTruthy"
            Operator.Or -> "isTruthy"
            else -> throw SimplexEvaluationError("undefined operator $op", loc = loc)
        })
        if (methodName == "isTruthy") {
            if (args.size != 1) {
                throw SimplexEvaluationError("Operator $op expected 1 arg, received ${args.size}", loc = loc)
            }
        } else {
            val methodType = target.getMethod(methodName) ?: throw SimplexUndefinedError(
                methodName,
                "method",
                loc = loc
            )
            val realArgs = args.drop(1)
            if (methodType.args.size != realArgs.size) {
                throw SimplexEvaluationError(
                    "Method $methodName expected ${methodType.args.size} arguments, but received ${realArgs.size}",
                    loc = loc
                )
            }
            methodType.args.zip(realArgs).forEach { (t, a) ->
                if (!t.matchedBy(a.resultType(env))) {
                    throw SimplexTypeError(t.toString(), a.resultType(env).toString(), location = a.loc)
                }
            }
        }
    }

    override fun resultType(env: Env): Type {
        val targetType = args[0].resultType(env)
        return when (op) {
            Operator.Plus -> targetType.getMethod("plus")?.returnType
            Operator.Minus -> targetType.getMethod("minus")?.returnType
            Operator.Times -> targetType.getMethod("times")?.returnType
            Operator.Div -> targetType.getMethod("div")?.returnType
            Operator.Mod -> targetType.getMethod("mod")?.returnType
            Operator.Pow -> targetType.getMethod("pow")?.returnType
            Operator.Eq -> Type.simple("Boolean")
            Operator.Neq -> Type.simple("Boolean")
            Operator.Gt -> targetType.getMethod("compare")?.let { Type.simple("Boolean") }
            Operator.Ge -> targetType.getMethod("compare")?.let { Type.simple("Boolean") }
            Operator.Lt -> targetType.getMethod("compare")?.let { Type.simple("Boolean") }
            Operator.Le -> targetType.getMethod("compare")?.let { Type.simple("Boolean") }
            Operator.Not -> Type.simple("Boolean")
            Operator.And -> Type.simple("Boolean")
            Operator.Or -> Type.simple("Boolean")
            Operator.Subscript -> targetType.getMethod("sub")?.returnType
            Operator.Uminus -> targetType.getMethod("neg")?.returnType
        } ?: throw SimplexUnsupportedOperation(targetType.toString(), op.toString(), loc = loc)
    }
}
