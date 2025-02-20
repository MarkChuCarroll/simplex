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

import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.collections.zip
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.def.DataDefinition
import org.goodmath.simplex.ast.types.SimpleType
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexAnalysisError
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.DataValue
import org.goodmath.simplex.runtime.values.primitives.DataValueType
import org.goodmath.simplex.twist.Twist

class DataExpr(val dataType: String, val args: List<Expr>, loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj("DataExpr", Twist.attr("dataType", dataType), Twist.array("args", args))

    override fun evaluateIn(env: Env): Value {
        val dataDef = env.getDef(dataType)
        if (dataDef !is DataDefinition) {
            throw SimplexEvaluationError(
                "Cannot create a non-data type like $dataType with a data expression",
                loc = loc,
            )
        }
        if (args.size != dataDef.fields.size) {
            throw SimplexEvaluationError(
                "Invalid expression: data value takes ${dataDef.fields.size} fields, but only given ${args.size}",
                loc = loc,
            )
        }
        val fieldValues = args.map { it.evaluateIn(env) }.toMutableList()
        return DataValue(dataDef.valueType, fieldValues)
    }

    override fun resultType(env: Env): Type {
        return Type.simple(dataType)
    }

    override fun validate(env: Env) {
        val dataDef = env.getDef(dataType)
        if (dataDef !is DataDefinition) {
            throw SimplexEvaluationError(
                "Cannot create a non-data type like $dataType with a data expression",
                loc = loc,
            )
        }
        val fieldTypes = dataDef.fields.map { it.type }
        if (fieldTypes.size != args.size) {
            throw SimplexEvaluationError(
                "Data type ${dataDef.name} expects ${fieldTypes.size} field values, but received ${args.size}",
                loc = loc,
            )
        }
        fieldTypes.zip(args).forEach { (t, a) ->
            val argType = a.resultType(env)
            if (!t.matchedBy(argType)) {
                throw SimplexTypeError(a.toString(), t.toString(), argType.toString(), location = a.loc)
            }
        }
    }
}

class FieldRefExpr(val dataExpr: Expr, val fieldName: String, loc: Location) : Expr(loc) {
    override fun twist(): Twist =
        Twist.obj(
            "FieldRefExpr",
            Twist.value("dataExpr", dataExpr),
            Twist.attr("fieldName", fieldName),
        )

    override fun evaluateIn(env: Env): Value {
        val target = dataExpr.evaluateIn(env)
        if (target.valueType !is DataValueType) {
            throw SimplexEvaluationError("Expected a data value, found ${target.valueType.name}")
        }
        val dataVal = target as DataValue
        val fieldIdx = dataVal.valueType.dataDef.indexOf(fieldName)
        return dataVal.fields[fieldIdx]
    }

    override fun resultType(env: Env): Type {
        val dataTypeMaybe = dataExpr.resultType(env)
        if (dataTypeMaybe is SimpleType) {
            val def = env.getDef(dataTypeMaybe.name)
            if (def is DataDefinition) {
                val field = def.fields.firstOrNull { it.name == fieldName }
                if (field != null) {
                    return field.type
                } else {
                    throw SimplexUndefinedError(fieldName, "data field name", loc = loc)
                }
            } else {
                throw SimplexAnalysisError("Field expression target is not a data value", loc = loc)
            }
        } else {
            throw SimplexAnalysisError("Field expression target is not a data value", loc = loc)
        }
    }

    override fun validate(env: Env) {
        val dataType = dataExpr.resultType(env)
        if (dataType !is SimpleType) {
            throw SimplexAnalysisError(
                "Field reference target must have a data type, not $dataType",
                loc = loc,
            )
        }
        val dataTypeDef = env.getDef(dataType.name)
        if (dataTypeDef !is DataDefinition) {
            throw SimplexAnalysisError(
                "Field reference target must be a data type, not $dataTypeDef",
                loc = loc,
            )
        }
        dataTypeDef.fields.firstOrNull { it.name == fieldName }
            ?: throw SimplexUndefinedError(fieldName, "data field", loc = loc)
    }
}

class DataFieldUpdateExpr(val dataExpr: Expr, val field: String, val value: Expr, loc: Location) :
    Expr(loc) {
    override fun evaluateIn(env: Env): Value {
        val dataValue = dataExpr.evaluateIn(env)
        if (dataValue !is DataValue) {
            throw SimplexEvaluationError(
                "Target of a field update must be a data value, not ${dataValue.valueType.name}",
                loc = loc,
            )
        }
        val idx = dataValue.valueType.dataDef.indexOf(field)
        val newValue = value.evaluateIn(env)
        dataValue.fields[idx] = newValue
        return dataValue
    }

    override fun resultType(env: Env): Type {
        return dataExpr.resultType(env)
    }

    override fun validate(env: Env) {
        dataExpr.validate(env)
        val targetType = dataExpr.resultType(env)
        if (targetType !is SimpleType) {
            throw SimplexAnalysisError(
                "The type of the target of a data field update expr must be a data type, but received $targetType",
                loc = loc,
            )
        }
        val def = env.getDef(targetType.name)
        if (def !is DataDefinition) {
            throw SimplexAnalysisError(
                "The type of the target of a data field update must be a data type, but no data type def found for $targetType",
                loc = loc,
            )
        }
        val dataFieldDef = def.fields.firstOrNull { it.name == field }
        if (dataFieldDef == null) { throw SimplexUndefinedError(field, "data field of  ${def.name}")
        }
        value.validate(env)
        val newValueType = value.resultType(env)
        if (!dataFieldDef.type.matchedBy(newValueType)) {
            throw SimplexTypeError(
                value.toString(),
                dataFieldDef.type.toString(),
                newValueType.toString(),
                location = value.loc,
            )
        }
    }

    override fun twist(): Twist =
        Twist.obj(
            "DataFieldUpdateExpr",
            Twist.value("target", dataExpr),
            Twist.attr("field", field),
            Twist.value("newValue", value),
        )
}
