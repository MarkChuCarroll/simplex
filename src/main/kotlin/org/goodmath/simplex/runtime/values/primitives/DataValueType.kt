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
package org.goodmath.simplex.runtime.values.primitives

import org.goodmath.simplex.ast.def.DataDefinition
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.types.Parameter
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.ParameterSignature
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist

class DataValueType(val dataDef: DataDefinition) : ValueType() {
    override val name: String = dataDef.name

    override val asType: Type by lazy {
        val t = Type.simple(dataDef.name)
        Type.registerValueType(t, this)
        t
    }

    override val supportsText: Boolean = true

    override fun toText(v: Value): String {
        val tup = assertIs(v)
        val sb = StringBuilder()
        sb.append("#${tup.valueType.name}(")

        sb.append(
            tup.valueType.dataDef.fields
                .map { field ->
                    val fieldValue = tup.fields[tup.valueType.dataDef.indexOf(field.name)]
                    if (fieldValue.valueType.supportsText) {
                        "${field.name}=${fieldValue.valueType.toText(fieldValue)}"
                    } else {
                        "${field.name}:${fieldValue.valueType.name}"
                    }
                }
                .joinToString(", ")
        )
        sb.append(")")
        return sb.toString()
    }

    override fun isTruthy(v: Value): Boolean {
        assertIs(v)
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object :
                PrimitiveMethod(
                    "eq",
                    MethodSignature.simple(asType, ParameterSignature(listOf(Parameter("other", asType))), BooleanValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>,
                                     kwArgs: Map<String, Value>, env: Env): Value {
                    val self = assertIs(target)
                    return if (args[0].valueType != self.valueType) {
                        BooleanValue(false)
                    } else {
                        val other = args[0] as DataValue
                        if (self.fields.size != other.fields.size) {
                            BooleanValue(false)
                        } else {
                            BooleanValue(
                                self.fields.zip(other.fields).all { (l, r) ->
                                    val result = l.valueType.applyMethod(l, "eq", listOf(r), emptyMap(), env)
                                    result.valueType.isTruthy(result)
                                }
                            )
                        }
                    }
                }
            }
        )
    }
    override val providesVariables: Map<String, Value> = emptyMap()

    override fun assertIs(v: Value): DataValue {
        if (v.valueType is DataValueType) {
            return v as DataValue
        } else {
            throw SimplexTypeError(v.valueType.asType.toString(), this.toString())
        }
    }
}

class DataValue(override val valueType: DataValueType, val fields: MutableList<Value>) : Value {

    override fun twist(): Twist =
        Twist.obj("DataValue", Twist.attr("name", valueType.dataDef.name), Twist.array("fields", fields))
}
