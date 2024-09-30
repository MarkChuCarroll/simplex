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

import kotlin.collections.first
import kotlin.collections.last
import kotlin.collections.map
import kotlin.collections.toSet
import kotlin.toString
import org.goodmath.simplex.ast.AstNode
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.def.TupleDefinition
import org.goodmath.simplex.ast.types.SimpleType
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.types.TypedName
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexAnalysisError
import org.goodmath.simplex.runtime.SimplexError
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.values.AnyType
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.ArrayValue
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.FunctionValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.StringValue
import org.goodmath.simplex.runtime.values.primitives.TupleValue
import org.goodmath.simplex.twist.Twist

abstract class Expr(loc: Location) : AstNode(loc) {
    abstract fun evaluateIn(env: Env): Value

    abstract fun resultType(env: Env): Type

    abstract fun validate(env: Env)
}

class BlockExpr(val body: List<Expr>, loc: Location) : Expr(loc) {
    override fun twist(): Twist = Twist.obj("Block", Twist.array("body", body))

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

data class Binding(val name: String, val type: Type, val value: Expr, override val loc: Location) :
    AstNode(loc) {
    override fun twist(): Twist {
        return Twist.obj(
            "binding",
            Twist.attr("name", name),
            Twist.value("type", type),
            Twist.value("value", value),
        )
    }
}

class LetExpr(val name: String, val type: Type?, val value: Expr, loc: Location) : Expr(loc) {

    override fun twist(): Twist =
        Twist.obj(
            "LetExpr",
            Twist.attr("name", name),
            Twist.attr("type", type.toString()),
            Twist.value("value", value),
        )

    override fun evaluateIn(env: Env): Value {
        val result = value.evaluateIn(env)
        env.addVariable(name, result)
        return result
    }

    override fun resultType(env: Env): Type {
        return value.resultType(env)
    }

    override fun validate(env: Env) {
        value.validate(env)
        val declareType = type ?: value.resultType(env)
        env.declareTypeOf(name, declareType)

        if (type != null && !type.matchedBy(value.resultType(env))) {
            throw SimplexTypeError(
                type.toString(),
                value.resultType(env).toString(),
                location = loc,
            )
        }
        value.validate(env)
    }
}

class LiteralExpr<T>(val v: T, loc: Location) : Expr(loc) {
    override fun twist(): Twist = Twist.obj("LiteralExpr", Twist.attr("value", v.toString()))

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
            throw SimplexEvaluationError("Invalid literal value $v", loc = loc)
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
            throw SimplexEvaluationError("Invalid literal value $v", loc = loc)
        }
    }

    override fun validate(env: Env) {}
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
            throw SimplexTypeError(expected.toString(), actual.toString(), location = loc)
        }
    }

    override fun twist(): Twist =
        Twist.obj("AssignmentExpr", Twist.attr("variable", target), Twist.value("value", expr))
}

class VarRefExpr(val name: String, loc: Location) : Expr(loc) {
    override fun twist(): Twist = Twist.obj("VariableExpr", Twist.attr("name", name))

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
                throw SimplexAnalysisError("Internal error", cause = e, loc = loc)
            }
        }
    }

    override fun validate(env: Env) {
        env.getDeclaredTypeOf(name)
    }
}

class ArrayExpr(val elements: List<Expr>, loc: Location) : Expr(loc) {
    override fun twist(): Twist = Twist.obj("Array", Twist.array("elements", elements))

    override fun evaluateIn(env: Env): Value {
        val elementValues = elements.map { it.evaluateIn(env) }
        val elementTypes = elementValues.map { it.valueType }.toSet()
        val elementType =
            if (elementTypes.size > 1) {
                AnyType
            } else {
                elementTypes.first()
            }
        return ArrayValue(elementType, elementValues)
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
        Twist.obj("WithExpr", Twist.value("focus", focus), Twist.array("body", body))

    override fun evaluateIn(env: Env): Value {
        val focusVal = focus.evaluateIn(env)
        if (focusVal !is TupleValue) {
            throw SimplexTypeError("Tuple", focusVal.valueType.name, location = loc)
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
        // We know it's a simple type, because validation would have failed otherwise.
        val focusType = focus.resultType(env) as SimpleType
        // similarly, we know it's a tuple-def
        val focusDef = env.getDef(focusType.name) as TupleDefinition
        val localEnv = Env(emptyList(), env)
        for (field in focusDef.fields) {
            localEnv.declareTypeOf(field.name, field.type)
        }
        return body.last().resultType(localEnv)
    }

    override fun validate(env: Env) {
        val focusType = focus.resultType(env)
        if (focusType !is SimpleType) {
            throw SimplexAnalysisError(
                "With expression focus must be a simple type, not $focusType",
                loc = loc,
            )
        }
        val focusDef = env.getDef(focusType.name)
        if (focusDef !is TupleDefinition) {
            throw SimplexAnalysisError(
                "With expression focus must be a tuple type, not $focusDef",
                loc = loc,
            )
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
    loc: Location,
) : Expr(loc) {
    override fun evaluateIn(env: Env): Value {
        return FunctionValue(declaredResultType, params, emptyList(), body, env)
    }

    override fun resultType(env: Env): Type {
        return Type.function(listOf(params.map { it.type }), declaredResultType)
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
            throw SimplexTypeError(
                declaredResultType.toString(),
                actualResultType.toString(),
                location = body.last().loc,
            )
        }
    }

    override fun twist(): Twist =
        Twist.obj(
            "LambdaExpr",
            Twist.value("resultType", declaredResultType),
            Twist.array("params", params),
            Twist.array("body", body),
        )
}
