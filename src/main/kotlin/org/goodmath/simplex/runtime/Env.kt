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
package org.goodmath.simplex.runtime

import org.goodmath.simplex.ast.Definition
import org.goodmath.simplex.ast.Model
import org.goodmath.simplex.runtime.values.csg.CsgValueType
import org.goodmath.simplex.runtime.csg.TwoDPointValueType
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.csg.ThreeDPointValueType
import org.goodmath.simplex.runtime.values.csg.PolygonValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.runtime.values.primitives.ArrayValueType
import org.goodmath.simplex.runtime.values.primitives.BooleanValueType
import org.goodmath.simplex.runtime.values.primitives.FunctionValueType
import org.goodmath.simplex.runtime.values.primitives.StringValueType
import org.goodmath.simplex.runtime.values.primitives.TupleValueType
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable
import java.util.UUID

class Env(defList: List<Definition>,
    val parentEnv: Env?): Twistable {
    val defs = defList.associateBy { it.name }
    val vars = HashMap<String, Value>()

    val id: String = UUID.randomUUID().toString()

    fun getValue(name: String): Value {
        return if (vars.containsKey(name)) {
            vars[name]!!
        } else if (parentEnv != null) {
            parentEnv.getValue(name)
        } else {
            throw SimplexUndefinedError(name, "variable")
        }
    }

    fun getDef(name: String): Definition {
        return defs[name] ?: throw SimplexUndefinedError(name, "definition")
    }

    fun installDefinitionValues() {
        for (d in defs.values) {
            d.installInEnv(this)
        }
    }

    fun addVariable(name: String, value: Value) {
        vars[name] = value
    }

    fun updateVariable(name: String, value: Value) {
        if (vars.containsKey(name)) {
            vars[name] = value
        } else {
            throw SimplexUndefinedError(name, "variable")
        }
    }

    override fun twist(): Twist =
        Twist.obj("Environment",
            Twist.attr("id", id),
            Twist.attr("parent", parentEnv?.id),
            Twist.array("definitions", defs.values.toList()),
            Twist.array("variables",
                vars.map { (k, v) -> Twist.value(k, v) }
            ))

    companion object {
        val valueTypes: List<ValueType<*>> = listOf(
            IntegerValueType,
            TupleValueType,
            FunctionValueType,
            FloatValueType,
            StringValueType,
            BooleanValueType,
            ArrayValueType,
            ThreeDPointValueType,
            TwoDPointValueType,
            PolygonValueType,
            CsgValueType
        )

        fun createRootEnv(model: Model): Env {
            val env = Env(model.defs, null)
            for (t in valueTypes) {
                for (t in t.providesFunctions) {
                    env.addVariable(t.name, t)
                }
            }
            return env
        }
    }


}
