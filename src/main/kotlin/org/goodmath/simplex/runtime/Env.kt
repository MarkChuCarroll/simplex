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
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.StringValueType
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable
import java.util.UUID
import com.github.ajalt.mordant.rendering.TextColors.*
import org.goodmath.simplex.ast.ArrayType
import org.goodmath.simplex.ast.MethodType
import org.goodmath.simplex.ast.Type
import org.goodmath.simplex.runtime.values.AnyType
import org.goodmath.simplex.runtime.values.primitives.StringValue


open class Env(defList: List<Definition>,
    val parentEnv: Env?): Twistable {
    val defs = defList.associateBy { it.name }.toMutableMap()
    val vars = HashMap<String, Value>()
    val declaredTypes = HashMap<String, Type>()
    open val types = HashMap<String, ValueType>()

    open val id: String = UUID.randomUUID().toString()

    fun registerType(name: String, valueType: ValueType) {
        types[name] = valueType
    }

    fun getType(name: String): ValueType {
        return types[name] ?: throw SimplexUndefinedError(name,  "type")
    }


    fun declareTypeOf(name: String, t: Type) {
        if (declaredTypes.containsKey(name) && declaredTypes[name] != t) {
            throw SimplexAnalysisError("Type of $name is already defined")
        }
        declaredTypes[name] = t
    }

    fun getDeclaredTypeOf(name: String): Type {
        return if (declaredTypes.containsKey(name)) {
            declaredTypes[name]!!
        } else if (parentEnv != null) {
            parentEnv.getDeclaredTypeOf(name)
        } else {
            throw SimplexUndefinedError(name, "symbol")
        }
    }

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

    fun installStaticDefinitions() {
        for (t in valueTypes) {
            registerType(t.name, t)
            for (m in t.providesOperations) {
                t.asType.registerMethod(m.name, Type.method(m.sig.self, m.sig.params.map { it.type }, m.sig.returnType) as MethodType)
            }
            for (f in t.providesFunctions) {
                val sig = f.signatures[0]
                declareTypeOf(f.name, Type.function(sig.params.map { it.type }, sig.returnType))
            }
        }
        for (f in functions) {
            val sig = f.signatures[0]
            val funType = Type.function(sig.params.map { it.type }, sig.returnType)
            declareTypeOf(f.name, funType)
        }
        for (d in defs.values) {
            d.installStatic(this)
        }
    }

    fun installDefinitionValues() {
        for (t in valueTypes) {
            registerType(t.name, t)
            for (m in t.providesOperations) {
                t.addMethod(m)
                t.asType.registerMethod(m.name, Type.method(m.sig.self, m.sig.params.map { it.type }, m.sig.returnType) as MethodType)
            }
        }
        for (d in defs.values) {
            d.installValues(this)
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
        val valueTypes: List<ValueType> by lazy {
            listOf(IntegerValueType,
            FloatValueType,
            StringValueType,
            BooleanValueType,
            ThreeDPointValueType,
            TwoDPointValueType,
            PolygonValueType,
            CsgValueType)
        }

        val functions: List<PrimitiveFunctionValue> by lazy {
            listOf(
                object: PrimitiveFunctionValue("print",
                    FunctionSignature(listOf(Param("values", ArrayType(AnyType.asType))), StringValueType.asType)) {
                    override fun execute(args: List<Value>): Value {
                        val arr = ArrayValueType.of(AnyType).assertIsArray(args[0])
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
                })
        }


        fun createRootEnv(): Env {
            for (t in valueTypes) {
                for (t in t.providesFunctions) {
                    RootEnv.addVariable(t.name, t)
                }
            }
            for (f in functions) {
                RootEnv.addVariable(f.name, f)
            }
            return RootEnv
        }
    }
}

object RootEnv: Env(emptyList(), null) {


    fun addDefinition(def: Definition) {
        defs[def.name] = def
    }

    fun getDefinition(name: String): Definition {
        return defs[name] ?: throw SimplexUndefinedError(name, "definition")
    }

    override val types by lazy {
        hashMapOf(
            "Int" to IntegerValueType,
            "Float" to FloatValueType,
            "String" to StringValueType,
            "CSG" to CsgValueType,
            "TwoDPoint" to TwoDPointValueType,
            "ThreeDPoint" to ThreeDPointValueType,
            "Polygon" to PolygonValueType,
            "Any" to AnyType,
        )
    }

    override val id: String = "Root"
}
