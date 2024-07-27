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
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.csg.ThreeDPointValueType
import org.goodmath.simplex.runtime.values.csg.PolygonValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.runtime.values.primitives.ArrayValueType
import org.goodmath.simplex.runtime.values.primitives.BooleanValueType
import org.goodmath.simplex.runtime.values.primitives.FunctionValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.StringValueType
import org.goodmath.simplex.runtime.values.primitives.TupleValueType
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable
import java.util.UUID
import com.github.ajalt.mordant.rendering.TextColors.*
import org.goodmath.simplex.runtime.values.primitives.ArrayValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.StringValue

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
                vars.map { (k, _) -> Twist.attr("name", k) }
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

        val functions: List<PrimitiveFunctionValue> = listOf(
            object: PrimitiveFunctionValue("print",
                FunctionSignature(listOf(Param("values", ArrayValueType)), StringValueType)) {
                override fun execute(args: List<Value>): Value {
                    val arr = ArrayValueType.assertIs(args[0]).elements
                    val result = arr.map {
                        if (it.valueType.supportsText) {
                            it.valueType.toText(it)
                        } else {
                            it.valueType.name
                        }
                    }.joinToString("")
                    Model.output(0, brightWhite(result), false)
                    return StringValue(result)
                }
            },
            object: PrimitiveFunctionValue("range",
                FunctionSignature(
                    listOf(
                        Param("from", IntegerValueType),
                        Param("to", IntegerValueType)),
                    ArrayValueType),
                FunctionSignature(
                    listOf(Param("to", IntegerValueType)),
                    ArrayValueType)) {
                override fun execute(args: List<Value>): Value {
                    val l = IntegerValueType.assertIs(args[0]).i
                    if (args.size == 1) {
                        return ArrayValue((0..<l).map { IntegerValue(it) }.toList())
                    } else {
                        val r = IntegerValueType.assertIs(args[1]).i
                        return ArrayValue((l..<r).map { IntegerValue(it) }.toList())
                    }
                }

            })

        fun createRootEnv(model: Model): Env {
            val env = Env(model.defs, null)
            for (t in valueTypes) {
                for (t in t.providesFunctions) {
                    env.addVariable(t.name, t)
                }
            }
            for (f in functions) {
                env.addVariable(f.name, f)
            }
            return env
        }
    }


}
