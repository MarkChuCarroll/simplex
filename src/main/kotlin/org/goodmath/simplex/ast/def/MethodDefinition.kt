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
package org.goodmath.simplex.ast.def

import kotlin.collections.map
import kotlin.collections.zip
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.expr.Expr
import org.goodmath.simplex.ast.types.ArgumentListSpec
import org.goodmath.simplex.ast.types.KwParameter
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.types.Parameter
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.MethodValue
import org.goodmath.simplex.runtime.values.primitives.NoneValue
import org.goodmath.simplex.twist.Twist

class MethodDefinition(
    val targetType: Type,
    val methodName: String,
    positionalParams: List<Parameter>,
    kwParams: List<KwParameter>,
    resultType: Type,
    body: List<Expr>,
    loc: Location,
) : InvokableDefinition("${targetType}->${methodName}", resultType, positionalParams, kwParams, body, loc) {
    override fun installValues(env: Env) {
        val valueType = Type.getValueType(targetType)
        valueType.addMethod(MethodValue(targetType, returnType, this@MethodDefinition.positionalParams,
            this@MethodDefinition.kwParams, body, this, env))
    }

    override fun validate(env: Env) {
        val methodEnv = Env(emptyList(), env)
        methodEnv.declareTypeOf("self", targetType)
        validateParamsAndBody(methodEnv)
    }

    override fun installStatic(env: Env) {
        targetType.registerMethod(name, Type.multiMethod(targetType,
            listOf(
                ArgumentListSpec(this@MethodDefinition.positionalParams.map { it.type },
                    this@MethodDefinition.kwParams.associate { it.name to it.type })),
            returnType))
    }

    override fun twist(): Twist =
        Twist.obj(
            "MethodDefinition",
            Twist.value("targetType", targetType),
            Twist.attr("name", methodName),
            Twist.array("params", this@MethodDefinition.positionalParams),
            Twist.value("resultType", returnType),
            Twist.array("body", body),
        )

    fun applyTo(target: Value, args: List<Value>,
                kwArgs: Map<String, Value>,
                env: Env): Value {
        val localEnv = Env(emptyList(), env)
        localEnv.addVariable("self", target)
        this@MethodDefinition.positionalParams.zip(args).map { (param, arg) -> localEnv.addVariable(param.name, arg) }
        for (kwParam in kwParams) {
            val paramValue = kwArgs[kwParam.name] ?: kwParam.defaultValue.evaluateIn(env)
            localEnv.addVariable(kwParam.name, paramValue)
        }
        var result: Value = NoneValue
        for (expr in body) {
            result = expr.evaluateIn(localEnv)
        }
        return result
    }
}
