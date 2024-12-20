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
import org.goodmath.simplex.ast.types.ArgumentListSpec
import org.goodmath.simplex.ast.types.KwParameter
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.types.Parameter
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexAnalysisError
import org.goodmath.simplex.runtime.SimplexError
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.values.AnyValueType
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.VectorValue
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.BooleanValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.FunctionValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.runtime.values.primitives.StringValue
import org.goodmath.simplex.runtime.values.primitives.StringValueType
import org.goodmath.simplex.twist.Twist

abstract class Expr(loc: Location?) : AstNode(loc) {
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

class LiteralExpr<T>(val v: T, loc: Location?) : Expr(loc) {
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
            IntegerValueType.asType
        } else if (v is Double) {
            FloatValueType.asType
        } else if (v is String) {
            StringValueType.asType
        } else if (v is Boolean) {
            BooleanValueType.asType
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

class VectorExpr(val elements: List<Expr>, loc: Location) : Expr(loc) {
    override fun twist(): Twist = Twist.obj("Vector", Twist.array("elements", elements))

    override fun evaluateIn(env: Env): Value {
        val elementValues = elements.map { it.evaluateIn(env) }
        val elementTypes = elementValues.map { it.valueType }.toSet()
        val elementType =
            if (elementTypes.size > 1) {
                AnyValueType
            } else {
                elementTypes.first()
            }
        return VectorValue(elementType, elementValues)
    }

    override fun resultType(env: Env): Type {
        val elementTypes = elements.map { it.resultType(env) }.toSet()
        return if (elementTypes.size > 1) {
            throw SimplexAnalysisError("No resolved type: [${elementTypes}]", loc = loc)
        } else {
            Type.vector(elementTypes.first())
        }
    }

    override fun validate(env: Env) {
        resultType(env)
        for (e in elements) {
            e.validate(env)
        }
    }
}

class LambdaExpr(
    val declaredResultType: Type,
    val positionalParams: List<Parameter>,
    val keywordParams: List<KwParameter>,
    val body: List<Expr>,
    loc: Location,
) : Expr(loc) {

    override fun evaluateIn(env: Env): Value {
        return FunctionValue(declaredResultType, positionalParams, keywordParams, emptyList(), body, env)
    }

    override fun resultType(env: Env): Type {
        return Type.function(
            listOf(ArgumentListSpec(
                positionalParams.map { it.type },
                keywordParams.associate { it.name to it.type }
            )), declaredResultType)
    }

    override fun validate(env: Env) {
        val localEnv = Env(emptyList(), env)
        for (param in positionalParams) {
            localEnv.declareTypeOf(param.name, param.type)
        }
        for (kwParam in keywordParams) {
            localEnv.declareTypeOf(kwParam.name, kwParam.type)
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
            Twist.array("positionalParams", positionalParams),
            Twist.array("keywordParams", keywordParams),
            Twist.array("body", body),
        )
}
