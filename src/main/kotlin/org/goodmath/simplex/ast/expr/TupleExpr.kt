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
import org.goodmath.simplex.ast.types.SimpleType
import org.goodmath.simplex.ast.def.TupleDefinition
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexAnalysisError
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.TupleValue
import org.goodmath.simplex.runtime.values.primitives.TupleValueType
import org.goodmath.simplex.twist.Twist
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.zip

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
            throw SimplexEvaluationError(
                "Cannot create a non-tuple type like $tupleType with a tuple expression",
                loc = loc
            )
        }
        if (args.size != tupleDef.fields.size) {
            throw SimplexEvaluationError(
                "Invalid expression: tuple takes ${tupleDef.fields.size} fields, but only given ${args.size}",
                loc = loc
            )
        }
        val fieldValues = args.map { it.evaluateIn(env) }.toMutableList()
        return TupleValue(tupleDef.valueType, fieldValues)
    }

    override fun resultType(env: Env): Type {
        return Type.simple(tupleType)
    }

    override fun validate(env: Env) {
        val tupleDef = env.getDef(tupleType)
        if (tupleDef !is TupleDefinition) {
            throw SimplexEvaluationError(
                "Cannot create a non-tuple type like $tupleType with a tuple expression",
                loc = loc
            )
        }
        val fieldTypes = tupleDef.fields.map { it.type }
        if (fieldTypes.size != args.size) {
            throw SimplexEvaluationError(
                "Tuple type ${tupleDef.name} expects ${fieldTypes.size} field values, but received ${args.size}",
                loc = loc
            )
        }
        fieldTypes.zip(args).forEach { (t, a) ->
            val argType = a.resultType(env)
            if (!t.matchedBy(argType)) {
                throw SimplexTypeError(t.toString(), argType.toString(), location = a.loc)
            }
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
                    throw SimplexUndefinedError(fieldName, "tuple field name", loc = loc)
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
            throw SimplexAnalysisError(
                "Field reference target must have a simple tuple type, not $tupleType",
                loc = loc
            )
        }
        val tupleDef = env.getDef(tupleType.name)
        if (tupleDef !is TupleDefinition) {
            throw SimplexAnalysisError("Field reference target must be a tuple type, not $tupleDef", loc = loc)
        }
        tupleDef.fields.firstOrNull { it.name == fieldName } ?: throw SimplexUndefinedError(
            fieldName,
            "tuple field",
            loc = loc
        )
    }
}

class TupleFieldUpdateExpr(val tupleExpr: Expr, val field: String, val value: Expr, loc: Location): Expr(loc) {
    override fun evaluateIn(env: Env): Value {
        val tuple = tupleExpr.evaluateIn(env)
        if (tuple !is TupleValue) {
            throw SimplexEvaluationError("Target of a tuple field update must be a tuple, not ${tuple.valueType.name}", loc = loc)
        }
        val idx = tuple.valueType.tupleDef.indexOf(field)
        val newValue = value.evaluateIn(env)
        tuple.fields[idx] = newValue
        return tuple
    }

    override fun resultType(env: Env): Type {
        return tupleExpr.resultType(env)
    }

    override fun validate(env: Env) {
        tupleExpr.validate(env)
        val targetType = tupleExpr.resultType(env)
        if (targetType !is SimpleType) {
            throw SimplexAnalysisError("The type of the target of a tuple field update expr must be a simple type, but received ${targetType}",
                loc=loc)
        }
        val def = env.getDef(targetType.name)
        if (def !is TupleDefinition) {
            throw SimplexAnalysisError("The type of the target of a tuple field update must be a tuple type, but no tuple def found for ${targetType}",
                loc=loc)
        }
        val tupleFieldDef = def.fields.firstOrNull { it.name == field }
        if (tupleFieldDef == null) {
            throw SimplexUndefinedError(field, "tuple field of  ${def.name}")
        }
        value.validate(env)
        val newValueType = value.resultType(env)
        if (!tupleFieldDef.type.matchedBy(newValueType)) {
            throw SimplexTypeError(tupleFieldDef.type.toString(), newValueType.toString(),
                location = value.loc)
        }
    }

    override fun twist(): Twist =
        Twist.obj("TupleFieldUpdateExpr",
            Twist.value("target", tupleExpr),
            Twist.attr("field", field),
            Twist.value("newValue", value))

}
