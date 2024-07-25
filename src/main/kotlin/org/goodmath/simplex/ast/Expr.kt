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

import org.goodmath.simplex.runtime.values.primitives.AbstractFunctionValue
import org.goodmath.simplex.runtime.values.primitives.ArrayValue
import org.goodmath.simplex.runtime.values.primitives.ArrayValueType
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexParameterCountError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.values.primitives.TupleValue
import org.goodmath.simplex.runtime.values.primitives.TupleValueType
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.StringValue
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable

abstract class Expr(loc: Location): AstNode(loc) {
    abstract fun evaluateIn(env: Env): Value
}

class BlockExpr(val body: List<Expr>, loc: Location): Expr(loc) {
    override fun twist(): Twist =
        Twist.obj("Block",
            Twist.array("body", body))

    override fun evaluateIn(env: Env): Value {
        val localEnv = Env(emptyList(), env)
        var result: Value = IntegerValue(0)
        for (expr in body) {
            result = expr.evaluateIn(localEnv)
        }
        return result
    }

}

data class Condition(val cond: Expr, val value: Expr): Twistable {
    override fun twist(): Twist =
        Twist.obj("Condition",
            Twist.value("if", cond),
            Twist.value("then", value))
}

class CondExpr(val conds: List<Condition>, val elseClause: Expr,
               loc: Location): Expr(loc) {

    override fun twist(): Twist =
        Twist.obj("IfExpr",
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
}

class FieldRefExpr(val tupleExpr: Expr, val fieldName: String, loc: Location):
        Expr(loc) {
    override fun twist(): Twist =
        Twist.obj("FieldRefExpr",
            Twist.value("tupleExpr", tupleExpr),
            Twist.attr("fieldName", fieldName))

    override fun evaluateIn(env: Env): Value {
        val target = tupleExpr.evaluateIn(env)
        if (target.valueType != TupleValueType) {
            throw SimplexEvaluationError("Expected a tuple value, found ${target.valueType.name}")
        }
        val tupleVal = target as TupleValue
        val fieldIdx = tupleVal.tupleDef.indexOf(fieldName)
        return tupleVal.fields[fieldIdx]
    }
}

class FunCallExpr(val funExpr: Expr, val argExprs: List<Expr>, loc: Location): Expr(loc) {
    override fun twist(): Twist =
        Twist.obj("FunCallExpr",
            Twist.value("functionExpr", funExpr),
            Twist.array("args", argExprs))

    override fun evaluateIn(env: Env): Value {
        val funVal = funExpr.evaluateIn(env)
        if (funVal !is AbstractFunctionValue) {
            throw SimplexEvaluationError("Only a function can be invoked, not ${funVal.valueType.name}")
        }
        val args = argExprs.map { it.evaluateIn(env) }
        if (funVal is PrimitiveFunctionValue &&
                    !funVal.signatures.any { sig -> sig.validateCall(args) }) {
            throw SimplexParameterCountError(funVal.signatures.map { it.params.size },
                args.size)
        }
        return funVal.applyTo(args)
    }
}


data class Binding(val name: String, val type: Type?, val value: Expr, override val loc: Location): AstNode(loc) {
    override fun twist(): Twist {
        return Twist.obj("binding",
            Twist.attr("name", name),
            Twist.value("type", type),
            Twist.value("value", value))
    }
}

class LetExpr(val bindings: List<Binding>, val body: List<Expr>, loc: Location): Expr(loc) {
    override fun twist(): Twist =
        Twist.obj("LetExpr",
            Twist.array("bindings", bindings),
            Twist.array("body", body))

    override fun evaluateIn(env: Env): Value {
        val localEnv = Env(emptyList(), env)
        for ((name, _, expr) in bindings) {
            val v = expr.evaluateIn(localEnv)
            localEnv.addVariable(name, v)
        }
        var result: Value = IntegerValue(0)
        for (e in body) {
            result = e.evaluateIn(localEnv)
        }
        return result
    }
}


class LiteralExpr<T>(val v: T, loc: Location): Expr(loc) {
    override fun twist(): Twist =
        Twist.obj("LiteralExpr",
            Twist.attr("value", v.toString()))

    override fun evaluateIn(env: Env): Value {
        return if (v is Int) {
            IntegerValue(v)
        } else if (v is Double) {
            FloatValue(v)
        } else if (v is String) {
            StringValue(v)
        } else if (v is Boolean) {
            BooleanValue(v)
        } else {
            throw SimplexEvaluationError("Invalid literal value $v")
        }
    }
}

class LoopExpr(val idxVar: String, val collExpr: Expr, val body: List<Expr>, loc: Location): Expr(loc) {
    override fun twist(): Twist =
        Twist.obj("LoopExpr",
            Twist.attr("idxVar", idxVar),
            Twist.value("collection", collExpr),
            Twist.array("body", body))

    override fun evaluateIn(env: Env): Value {
        val collValue = collExpr.evaluateIn(env)
        if (collValue.valueType != ArrayValueType) {
            throw SimplexEvaluationError("Loops can only iterate over arrays, not ${collValue.valueType.name}")
        }
        collValue as ArrayValue
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
        return ArrayValue(result)
    }
}

enum class Operator {
    Plus, Minus, Times, Div, Mod, Pow,
    Eq, Neq, Gt, Ge, Lt, Le,
    Not, And, Or,
    Subscript
}

class OperatorExpr(val op: Operator, val args: List<Expr>, loc: Location): Expr(loc) {
    override fun twist(): Twist =
        Twist.obj("OperatorExpr",
            Twist.attr("op", op.toString()),
            Twist.array("args", args)
            )

    fun l(args: List<Expr>, env: Env): Value = args.first().evaluateIn(env)
    fun r(args: List<Expr>, env: Env): Value = if (args.size > 1) {
        args[1].evaluateIn(env)
    } else {
        throw SimplexEvaluationError("Incorrect number of args for operator $op")
    }

    override fun evaluateIn(env: Env): Value {
        val left = l(args, env)
        return when(op) {
            Operator.Plus -> left.valueType.add(left, r(args, env))
            Operator.Minus -> if (args.size == 1) {
                left.valueType.neg(left)
            } else {
                left.valueType.subtract(left, r(args, env))
            }
            Operator.Times ->  left.valueType.mult(left, r(args, env))
            Operator.Div -> left.valueType.div(left, r(args, env))
            Operator.Mod -> left.valueType.mod(left, r(args, env))
            Operator.Pow -> left.valueType.pow(left, r(args, env))
            Operator.Eq -> BooleanValue(left.valueType.equals(left, r(args, env)))
            Operator.Neq -> BooleanValue(!left.valueType.equals(left, r(args, env)))
            Operator.Gt -> BooleanValue(left.valueType.compare(left, r(args, env)) > 0)
            Operator.Ge -> BooleanValue(left.valueType.compare(left, r(args, env)) >= 0)
            Operator.Lt -> BooleanValue(left.valueType.compare(left, r(args, env)) < 0)
            Operator.Le -> BooleanValue(left.valueType.compare(left, r(args, env)) <= 0)
            Operator.Not -> BooleanValue(left.valueType.isTruthy(left))
            Operator.And -> if (left.valueType.isTruthy(left)) {
                val right = r(args, env)
                right
            } else {
                left
            }
            Operator.Or -> if (left.valueType.isTruthy(left)) {
                    left
                } else {
                r(args, env)
            }
            Operator.Subscript -> left.valueType.subscript(left, r(args, env))
        }
    }
}

class TupleExpr(val tupleType: String, val args: List<Expr>, loc: Location): Expr(loc) {
    override fun twist(): Twist =
        Twist.obj("TupleExpr",
            Twist.attr("tupleType", tupleType),
            Twist.array("args", args))

    override fun evaluateIn(env: Env): Value {
        val tupleDef = env.getDef(tupleType)
        if (tupleDef !is TupleDefinition) {
            throw SimplexEvaluationError("Cannot create a non-tuple type like $tupleType with a tuple expression")
        }
        if (args.size != tupleDef.fields.size) {
            throw SimplexEvaluationError("Invalid expression: tuple takes ${tupleDef.fields.size} fields, but only given ${args.size}")
        }
        val fieldValues = args.map { it.evaluateIn(env) }
        return TupleValue(tupleDef, fieldValues)
    }
}


data class Update(val name: String, val value: Expr): Twistable {
    override fun twist(): Twist =
        Twist.obj(
            "update",
            Twist.attr("name", name),
            Twist.value("value", value)
        )
}

class UpdateExpr(val tupleExpr: Expr, val updates: List<Update>, loc: Location): Expr(loc) {
    override fun twist(): Twist =
        Twist.obj("UpdateExpr",
            Twist.value("target", tupleExpr),
            Twist.array("updates", updates))

    override fun evaluateIn(env: Env): Value {
        val target = tupleExpr.evaluateIn(env)
        if (target !is TupleValue) {
            throw SimplexTypeError("Tuple", target.valueType.name)
        }
        val def = target.tupleDef
        val fields = ArrayList(target.fields)
        for ((n, v) in updates) {
            val idx = def.indexOf(n)
            fields[idx] = v.evaluateIn(env)
        }
        return TupleValue(def, fields)
    }
}

class MethodCallExpr(val target: Expr, val name: String, val args: List<Expr>,
    loc: Location): Expr(loc) {
    override fun evaluateIn(env: Env): Value {
        val targetValue = target.evaluateIn(env)
        val meth = targetValue.valueType.getOperation(name)
        val argValues = args.map { it.evaluateIn(env) }
        val resultType = meth.signatures.firstNotNullOfOrNull { it.validateCall(targetValue, argValues) }
        if (resultType == null) {
            val sig = "(${argValues.map { it.valueType.name }.joinToString(",")})"
            throw SimplexEvaluationError("Could not find function signature variant matching $sig")
        }
        val result = meth.execute(targetValue, argValues)
        if (result.valueType != resultType) {
            throw SimplexTypeError(resultType.name, result.valueType.name)
        }
        return result
    }

    override fun twist(): Twist =
        Twist.obj("MethodExpr",
            Twist.value("target", target),
            Twist.attr("name", name),
            Twist.array("args", args))

}

class VarRefExpr(val name: String, loc: Location): Expr(loc) {
    override fun twist(): Twist =
        Twist.obj("VariableExpr",
            Twist.attr("name", name))

    override fun evaluateIn(env: Env): Value {
        return env.getValue(name)
    }
}

class ArrayExpr(val elements: List<Expr>, loc: Location): Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "Array",
            Twist.array("elements", elements)
        )

    override fun evaluateIn(env: Env): Value {
        return ArrayValue(elements.map { it.evaluateIn(env) })
    }

}

class WithExpr(val focus: Expr, val body: List<Expr>, loc: Location): Expr(loc) {
    override fun twist(): Twist =
        Twist.obj("WithExpr",
            Twist.value("focus", focus),
            Twist.array("body", body))

    override fun evaluateIn(env: Env): Value {
        val focusVal = focus.evaluateIn(env)
        if (focusVal !is TupleValue) {
            throw SimplexTypeError("Tuple", focusVal.valueType.name)
        }
        val def = focusVal.tupleDef
        val localEnv = Env(emptyList(), env)
        for ((name, idx) in def.fields.map { Pair(it.name, def.indexOf(it.name)) }) {
            localEnv.addVariable(name, focusVal.fields[idx])
        }
        var result: Value = IntegerValue(0)
        for (b in body) {
            result = b.evaluateIn(localEnv)
        }
        return result
    }
}
