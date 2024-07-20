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
import org.goodmath.simplex.runtime.FunctionValue
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.twist.Twist

abstract class Definition(val name: String, loc: Location): AstNode(loc) {
    abstract fun installInEnv(env: Env)
}

class TypedName(val name: String, val type: Type?, loc: Location): AstNode(loc) {
    override fun twist(): Twist =
        Twist.obj("TypedName",
            Twist.attr("name", name),
            Twist.value("type", type))
}

class FunctionDefinition(name: String, val params: List<TypedName>,
    val localDefs: List<Definition>,
    val body: List<Expr>,
    loc: Location): Definition(name, loc) {
    override fun twist(): Twist =
        Twist.obj(
            "FunctionDefinition",
            Twist.attr("name", name),
            Twist.array("params", params),
            Twist.array("localDefs", localDefs),
            Twist.array("body", body)
        )

    override fun installInEnv(env: Env) {
        val funValue = FunctionValue(params.map { it.name }.toList(),
            localDefs,
            body,
            env,
            this)
        env.addVariable(name, funValue)
    }
}

class TupleDefinition(name: String, val fields: List<TypedName>,
    loc: Location): Definition(name, loc) {
    override fun twist(): Twist =
        Twist.obj("TupleDefinition",
            Twist.attr("name", name),
            Twist.array("fields", fields)
            )

    override fun installInEnv(env: Env) {
        // No need to do anything
    }

    fun indexOf(fieldName: String): Int {
        val idx = fields.indexOfFirst { it.name == fieldName }
        if (idx < 0) {
            throw SimplexUndefinedError(fieldName, "tuple field of ${name}")
        } else {
            return idx
        }
    }
}

class VariableDefinition(name: String, val type: Type?,
                         val initialValue: Expr, loc: Location):
        Definition(name, loc) {
    override fun twist(): Twist =
        Twist.obj("VariableDefinition",
            Twist.attr("name", name),
            Twist.value("type", type),
            Twist.value("value", initialValue))

    override fun installInEnv(env: Env) {
        val v = initialValue.evaluateIn(env)
        env.addVariable(name, v)

    }
}
