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
import org.goodmath.simplex.runtime.SimplexAnalysisError
import org.goodmath.simplex.runtime.SimplexError
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.AnyType
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.AbstractFunctionValue
import org.goodmath.simplex.runtime.values.primitives.ArrayValue
import org.goodmath.simplex.runtime.values.primitives.ArrayValueType
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.FunctionValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.StringValue
import org.goodmath.simplex.runtime.values.primitives.TupleValue
import org.goodmath.simplex.runtime.values.primitives.TupleValueType
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable

abstract class Expr(loc: Location) : AstNode(loc) {
    abstract fun evaluateIn(env: Env): Value
    abstract fun resultType(env: Env): Type
    abstract fun validate(env: Env)
}

class BlockExpr(val body: List<Expr>, loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "Block",
            Twist.array("body", body)
        )

    override fun evaluateIn(env: Env): Value {
        val localEnv = Env(emptyList(), env)
        var result: Value = IntegerValue(0)
        for (expr in body) {
            result = expr.evaluateIn(localEnv)
        }
        return result
    }

    override fun resultType(env: Env): Type {
        return body.last().resultType(env)
    }

    override fun validate(env: Env) {
        for (expr in body) {
            expr.validate(env)
        }
    }

}

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
            throw SimplexAnalysisError("Cond clauses return different types: $condTypes", loc=loc)
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

class FieldRefExpr(val tupleExpr: Expr, val fieldName: String, loc: Location) :
    Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "FieldRefExpr",
            Twist.value("tupleExpr", tupleExpr),
            Twist.attr("fieldName", fieldName)
        )

    override fun evaluateIn(env: Env): Value {
        val target = tupleExpr.evaluateIn(env)
        if (target.valueType !is TupleValueType) {
            throw SimplexEvaluationError("Expected a tuple value, found ${target.valueType.name}")
        }
        val tupleVal = target as TupleValue
        val fieldIdx = tupleVal.valueType.tupleDef.indexOf(fieldName)
        return tupleVal.fields[fieldIdx]
    }

    override fun resultType(env: Env): Type {
        val tupleTypeMaybe = tupleExpr.resultType(env)
        if (tupleTypeMaybe is SimpleType) {
            val def = env.getDef(tupleTypeMaybe.name)
            if (def is TupleDefinition) {
                val field = def.fields.firstOrNull { it.name == fieldName }
                if (field != null) {
                    return field.type
                } else {
                    throw SimplexUndefinedError(fieldName, "tuple field name", loc=loc)
                }
            } else {
                throw SimplexAnalysisError(
                    "Tuple field target is not a tuple",
                    loc = loc
                )
            }
        } else {
            throw SimplexAnalysisError(
                "Tuple field target is not a tuple",
                loc = loc
            )
        }
    }

    override fun validate(env: Env) {
        val tupleType = tupleExpr.resultType(env)
        if (tupleType !is SimpleType) {
            throw SimplexAnalysisError("Field reference target must have a simple tuple type, not $tupleType", loc=loc)
        }
        val tupleDef = env.getDef(tupleType.name)
        if (tupleDef !is TupleDefinition) {
            throw SimplexAnalysisError("Field reference target must be a tuple type, not $tupleDef", loc=loc)
        }
        tupleDef.fields.firstOrNull { it.name == fieldName } ?: throw SimplexUndefinedError(fieldName, "tuple field", loc = loc)
    }
}

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
            throw SimplexEvaluationError("Only a function can be invoked, not ${funVal.valueType.name}", loc=loc)
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
                loc = loc)
        }
        if (funType.args.size != argExprs.size) {
            throw SimplexAnalysisError("Function call expected ${funType.args.size} args, but received ${argExprs.size}", loc=loc)
        }
        funType.args.zip(argExprs).forEach { (type, expr) ->
            if (!type.matchedBy(expr.resultType(env))) {
                throw SimplexTypeError(type.toString(), expr.resultType(env).toString(), location=expr.loc)
            }
        }

    }
}


data class Binding(val name: String, val type: Type, val value: Expr, override val loc: Location) : AstNode(loc) {
    override fun twist(): Twist {
        return Twist.obj(
            "binding",
            Twist.attr("name", name),
            Twist.value("type", type),
            Twist.value("value", value)
        )
    }
}

class LetExpr(val bindings: List<Binding>, val body: List<Expr>, loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "LetExpr",
            Twist.array("bindings", bindings),
            Twist.array("body", body)
        )

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

    override fun resultType(env: Env): Type {
        val localEnv = Env(emptyList(), env)
        for ((name, type, _) in bindings) {
            localEnv.declareTypeOf(name, type)
        }
        return body.last().resultType(localEnv)
    }

    override fun validate(env: Env) {
        val localEnv = Env(emptyList(), env)
        for ((name, type, expr) in bindings) {
            localEnv.declareTypeOf(name, type)
            if (!type.matchedBy(expr.resultType(localEnv))) {
                throw SimplexTypeError(type.toString(), expr.resultType(localEnv).toString(), location = expr.loc)
            }
        }
        body.forEach { expr -> expr.validate(localEnv) }
    }
}


class LiteralExpr<T>(val v: T, loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "LiteralExpr",
            Twist.attr("value", v.toString())
        )

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
            throw SimplexEvaluationError("Invalid literal value $v", loc=loc)
        }
    }

    override fun resultType(env: Env): Type {
        return if (v is Int) {
            Type.IntType
        } else if (v is Double) {
           Type.FloatType
        } else if (v is String) {
            Type.StringType
        } else if (v is Boolean) {
            Type.BooleanType
        } else {
            throw SimplexEvaluationError("Invalid literal value $v", loc=loc)
        }
    }

    override fun validate(env: Env) {
    }
}

class LoopExpr(val idxVar: String, val collExpr: Expr, val body: List<Expr>, loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "LoopExpr",
            Twist.attr("idxVar", idxVar),
            Twist.value("collection", collExpr),
            Twist.array("body", body)
        )

    override fun evaluateIn(env: Env): Value {
        val collValue = collExpr.evaluateIn(env)
        if (collValue.valueType != ArrayValueType) {
            throw SimplexEvaluationError("Loops can only iterate over arrays, not ${collValue.valueType.name}", loc=loc)
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
        // TODO this should be better.
        return ArrayValue(
            ArrayValueType.of(result[0].valueType), result
        )
    }

    override fun resultType(env: Env): Type {
        val collectionType = collExpr.resultType(env)
        if (collectionType !is ArrayType) {
            throw SimplexAnalysisError("Target of a loop must be an array, not $collectionType", loc=loc)
        }
        val elementType = collectionType.elementType
        val localEnv = Env(emptyList(), env)
        localEnv.declareTypeOf(idxVar, elementType)
        return Type.array(body.last().resultType(localEnv))
    }

    override fun validate(env: Env) {
        val collectionType = collExpr.resultType(env)
        if (collectionType !is ArrayType) {
            throw SimplexAnalysisError("Target of a loop must be an array, not $collectionType", loc=loc)
        }
        val elementType = collectionType.elementType
        val localEnv = Env(emptyList(), env)
        localEnv.declareTypeOf(idxVar, elementType)
        for (e in body) {
            e.validate(localEnv)
        }
    }
}

enum class Operator {
    Plus, Minus, Times, Div, Mod, Pow,
    Eq, Neq, Gt, Ge, Lt, Le,
    Not, And, Or,
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
            val e = SimplexEvaluationError("Error evaluating expression", cause = t)
            e.location = loc
            throw e
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
            else -> throw SimplexEvaluationError("undefined operator $op", loc=loc)
        })
        if (methodName == "isTruthy") {
            if (args.size != 1) {
                throw SimplexEvaluationError("Operator $op expected 1 arg, received ${args.size}", loc=loc)
            }
        } else {
            val methodType = target.getMethod(methodName) ?: throw SimplexUndefinedError(methodName, "method", loc=loc)
            val realArgs = args.drop(1)
            if (methodType.args.size != realArgs.size) {
                throw SimplexEvaluationError("Method $methodName expected ${methodType.args.size} arguments, but received ${realArgs.size}",
                    loc=loc)
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
        } ?: throw SimplexUnsupportedOperation(targetType.toString(), op.toString(), loc=loc)
    }
}

class TupleExpr(val tupleType: String, val args: List<Expr>, loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "TupleExpr",
            Twist.attr("tupleType", tupleType),
            Twist.array("args", args)
        )

    override fun evaluateIn(env: Env): Value {
        val tupleDef = env.getDef(tupleType)
        if (tupleDef !is TupleDefinition) {
            throw SimplexEvaluationError("Cannot create a non-tuple type like $tupleType with a tuple expression", loc=loc)
        }
        if (args.size != tupleDef.fields.size) {
            throw SimplexEvaluationError("Invalid expression: tuple takes ${tupleDef.fields.size} fields, but only given ${args.size}", loc=loc)
        }
        val fieldValues = args.map { it.evaluateIn(env) }
        return TupleValue(tupleDef.valueType, fieldValues)
    }

    override fun resultType(env: Env): Type {
        return Type.simple(tupleType)
    }

    override fun validate(env: Env) {
        val tupleDef = env.getDef(tupleType)
        if (tupleDef !is TupleDefinition) {
            throw SimplexEvaluationError("Cannot create a non-tuple type like $tupleType with a tuple expression", loc=loc)
        }
        val fieldTypes = tupleDef.fields.map { it.type }
        if (fieldTypes.size != args.size) {
            throw SimplexEvaluationError("Tuple type ${tupleDef.name} expects ${fieldTypes.size} field values, but received ${args.size}", loc=loc)
        }
        fieldTypes.zip(args).forEach { (t, a) ->
            val argType = a.resultType(env)
            if (!t.matchedBy(argType)) {
                throw SimplexTypeError(t.toString(), argType.toString(), location = a.loc)
            }
        }
    }
}

class AssignmentExpr(val target: String, val expr: Expr, loc: Location) : Expr(loc) {
    override fun evaluateIn(env: Env): Value {
        val value = expr.evaluateIn(env)
        env.updateVariable(target, value)
        return value
    }

    override fun resultType(env: Env): Type {
        return expr.resultType(env)
    }

    override fun validate(env: Env) {
        val expected = env.getDeclaredTypeOf(target)
        val actual = expr.resultType(env)
        if (!expected.matchedBy(actual)) {
            throw SimplexTypeError(expected.toString(), actual.toString(), location=loc)
        }
    }

    override fun twist(): Twist =
        Twist.obj(
            "AssignmentExpr",
            Twist.attr("variable", target),
            Twist.value("value", expr)
        )

}

class MethodCallExpr(
    val target: Expr, val name: String, val args: List<Expr>,
    loc: Location
) : Expr(loc) {
    override fun evaluateIn(env: Env): Value {
        val targetValue = target.evaluateIn(env)
        val argValues = args.map { it.evaluateIn(env) }
        return if (targetValue.valueType.hasPrimitiveMethod(name)) {
            val meth = targetValue.valueType.getPrimitiveMethod(name)
            return meth.execute(targetValue, argValues, env)
        } else {
            val meth = targetValue.valueType.getMethod(name)
            return meth.applyTo(targetValue, argValues, env)
        }
    }

    override fun resultType(env: Env): Type {
        val targetType = target.resultType(env)
        return targetType.getMethod(name)?.returnType ?: throw SimplexUndefinedError(name, "method of ${this.name}", loc=loc)
    }

    override fun validate(env: Env) {
        val targetType = target.resultType(env)
        val methodType = targetType.getMethod(name) ?: throw SimplexUndefinedError(name, "method of $targetType", loc=loc)
        if (args.size != methodType.args.size) {
            throw SimplexAnalysisError("Method $name expected ${methodType.args.size} arguments, but received ${args.size}",
                loc = loc)
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

class VarRefExpr(val name: String, loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "VariableExpr",
            Twist.attr("name", name)
        )

    override fun evaluateIn(env: Env): Value {
        try {
            return env.getValue(name)
        } catch (e: SimplexError) {
            e.location = loc
            throw e
        }
    }

    override fun resultType(env: Env): Type {
        try {
            return env.getDeclaredTypeOf(name)
        } catch (e: Throwable) {
            if (e is SimplexError) {
                if (e.location == null) {
                    e.location = loc
                }
                throw e
            } else {
                throw SimplexAnalysisError("Internal error", cause = e, loc=loc)
            }
        }
    }

    override fun validate(env: Env) {
        env.getDeclaredTypeOf(name)
    }
}

class ArrayExpr(val elements: List<Expr>, loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "Array",
            Twist.array("elements", elements)
        )

    override fun evaluateIn(env: Env): Value {
        val elementValues = elements.map { it.evaluateIn(env) }
        val elementTypes = elementValues.map { it.valueType }.toSet()
        val elementType = if (elementTypes.size > 1) {
            AnyType
        } else {
            elementTypes.first()
        }
        return ArrayValue(ArrayValueType.of(elementType), elementValues)
    }

    override fun resultType(env: Env): Type {
        val elementTypes = elements.map { it.resultType(env) }.toSet()
        return if (elementTypes.size > 1) {
            Type.array(Type.simple("Any"))
        } else {
            Type.array(elementTypes.first())
        }
    }

    override fun validate(env: Env) {
        resultType(env)
        for (e in elements) {
            e.validate(env)
        }
    }

}

class WithExpr(val focus: Expr, val body: List<Expr>, loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "WithExpr",
            Twist.value("focus", focus),
            Twist.array("body", body)
        )

    override fun evaluateIn(env: Env): Value {
        val focusVal = focus.evaluateIn(env)
        if (focusVal !is TupleValue) {
            throw SimplexTypeError("Tuple", focusVal.valueType.name, location=loc)
        }
        val def = focusVal.valueType.tupleDef
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

    override fun resultType(env: Env): Type {
        val focusType = focus.resultType(env)
        if (focusType !is SimpleType) {
            throw SimplexAnalysisError("With expression focus must be a simple type, not $focusType", loc=loc)
        }
        val focusDef = env.getDef(focusType.name)
        if (focusDef !is TupleDefinition) {
            throw SimplexAnalysisError("With expression focus must be a tuple type, not $focusDef", loc=loc)
        }
        val localEnv = Env(emptyList(), env)
        for (field in focusDef.fields) {
            localEnv.declareTypeOf(field.name, field.type)
        }
        return body.last().resultType(localEnv)
    }

    override fun validate(env: Env) {
        val focusType = focus.resultType(env)
        if (focusType !is SimpleType) {
            throw SimplexAnalysisError("With expression focus must be a simple type, not $focusType", loc=loc)
        }
        val focusDef = env.getDef(focusType.name)
        if (focusDef !is TupleDefinition) {
            throw SimplexAnalysisError("With expression focus must be a tuple type, not $focusDef", loc=loc)
        }
        val localEnv = Env(emptyList(), env)
        for (field in focusDef.fields) {
            localEnv.declareTypeOf(field.name, field.type)
        }
        for (e in body) {
            e.validate(localEnv)
        }
    }
}

class LambdaExpr(
    val declaredResultType: Type,
    val params: List<TypedName>,
    val body: List<Expr>,
    loc: Location
) : Expr(loc) {
    override fun evaluateIn(env: Env): Value {
        return FunctionValue(
            declaredResultType, params, emptyList(),
            body, env
        )
    }

    override fun resultType(env: Env): Type {
        return Type.function(params.map { it.type }, declaredResultType)
    }

    override fun validate(env: Env) {
        val localEnv = Env(emptyList(), env)
        for (param in params) {
            localEnv.declareTypeOf(param.name, param.type)
        }
        for (b in body) {
            b.validate(localEnv)
        }
        val actualResultType = body.last().resultType(localEnv)
        if (!declaredResultType.matchedBy(actualResultType)) {
            throw SimplexTypeError(declaredResultType.toString(), actualResultType.toString(), location = body.last().loc)
        }
    }

    override fun twist(): Twist =
        Twist.obj(
            "LambdaExpr",
            Twist.value("resultType", declaredResultType),
            Twist.array("params", params),
            Twist.array("body", body)
        )

}


