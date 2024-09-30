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

import com.github.ajalt.mordant.rendering.TextColors.*
import java.util.UUID
import org.goodmath.simplex.ast.def.Definition
import org.goodmath.simplex.ast.types.ArrayType
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.values.AnyType
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.manifold.RGBAType
import org.goodmath.simplex.runtime.values.manifold.BoundingBoxType
import org.goodmath.simplex.runtime.values.manifold.SliceType
import org.goodmath.simplex.runtime.values.manifold.SMaterialType
import org.goodmath.simplex.runtime.values.manifold.SMeshGLType
import org.goodmath.simplex.runtime.values.manifold.SPolygonType
import org.goodmath.simplex.runtime.values.manifold.BoundingRectType
import org.goodmath.simplex.runtime.values.manifold.SolidType
import org.goodmath.simplex.runtime.values.primitives.ArrayValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.runtime.values.primitives.NoneType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.StringValue
import org.goodmath.simplex.runtime.values.primitives.StringValueType
import org.goodmath.simplex.runtime.values.primitives.Vec2Type
import org.goodmath.simplex.runtime.values.primitives.Vec3Type
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable

/**
 * An environment, which contains the definitions, types, variables, functions, and methods
 * available in a static scope of the program.
 *
 * @param defList a list of the definitions local to the scope.
 * @param parentEnv the static scope that encloses this environment, if any.
 */
open class Env(defList: List<Definition>, val parentEnv: Env?) : Twistable {
    val defs = defList.associateBy { it.name }.toMutableMap()
    val vars = HashMap<String, Value>()
    val declaredTypes = HashMap<String, Type>()
    open val valueTypes = HashMap<String, ValueType>()
    val functions = HashMap<String, PrimitiveFunctionValue>()

    /** A unique identifier for a scope. Used only for debugging purposes. */
    open val id: String = UUID.randomUUID().toString()

    /** Register a new type defined within the scope. */
    fun registerType(name: String, valueType: ValueType) {
        if (valueTypes.containsKey(name)) {
            throw SimplexInternalError("Type $name is already registered in env $id")
        }
        valueTypes[name] = valueType
    }

    /**
     * Get a type in the scope. The key is the string returned by a static type's "toString" method.
     */
    fun getType(name: String): ValueType {
        return if (valueTypes.containsKey(name)) {
            valueTypes[name]!!
        } else if (parentEnv != null) {
            parentEnv.getType(name)
        } else {
            throw SimplexUndefinedError(name, "type")
        }
    }

    /** Declare the static type of a new variable name in the scope. */
    fun declareTypeOf(name: String, t: Type) {
        if (declaredTypes.containsKey(name) && declaredTypes[name] != t) {
            throw SimplexAnalysisError("Type of $name is already defined")
        }
        declaredTypes[name] = t
    }

    /** Get the statically declared type of a name in the scope, or any of its parent scopes. */
    fun getDeclaredTypeOf(name: String): Type {
        return if (declaredTypes.containsKey(name)) {
            declaredTypes[name]!!
        } else if (parentEnv != null) {
            parentEnv.getDeclaredTypeOf(name)
        } else {
            throw SimplexUndefinedError(name, "variable")
        }
    }

    /**
     * Get the value associated with a name. Can only be called during execution, not during the
     * analysis phase.
     */
    fun getValue(name: String): Value {
        return if (vars.containsKey(name)) {
            vars[name]!!
        } else if (parentEnv != null) {
            parentEnv.getValue(name)
        } else {
            throw SimplexUndefinedError(name, "variable")
        }
    }

    /** Get a definition declared within the scope. */
    fun getDef(name: String): Definition {
        return if (defs.containsKey(name)) {
            defs[name]!!
        } else if (parentEnv != null) {
            return parentEnv.getDef(name)
        } else {
            throw SimplexUndefinedError(name, "definition")
        }
    }

    /**
     * For use during analysis: Iterate through the definitions in this scope, and register their
     * static types and methods with the environment.
     */
    fun installStaticDefinitions() {
        for (t in valueTypes.values) {
            t.methods
            for (f in t.providesFunctions) {
                val sig = f.signature
                declareTypeOf(f.name, sig.toStaticType())
            }
        }
        for (f in functions.values) {
            val sig = f.signature
            val funType = sig.toStaticType()
            declareTypeOf(f.name, funType)
        }
        for (d in defs.values) {
            d.installStatic(this)
        }
    }

    /**
     * For use during execution when entering a scope: register the runtime values of symbols,
     * functions, and methods provided by the scope's definitions.
     */
    fun installDefinitionValues() {
        for (d in defs.values) {
            d.installValues(this)
        }
        for (t in valueTypes.values) {
            for (f in t.providesFunctions) {
                functions[f.name] = f
            }
        }
        for (f in functions.values) {
            this.addVariable(f.name, f)
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
        Twist.obj(
            "Environment",
            Twist.attr("id", id),
            Twist.attr("parent", parentEnv?.id),
            Twist.array("definitions", defs.values.toList()),
            Twist.array("variables", vars.map { (k, _) -> Twist.attr("name", k) }),
        )

    companion object {

        val rootFunctions: List<PrimitiveFunctionValue> by lazy {
            listOf(
                object :
                    PrimitiveFunctionValue(
                        "print",
                        FunctionSignature.simple(
                            listOf(Param("values", ArrayType(AnyType.asType))),
                            StringValueType.asType,
                        ),
                    ) {
                    override fun execute(args: List<Value>): Value {
                        val arr = ArrayValueType.of(AnyType).assertIsArray(args[0])
                        val result =
                            arr.map {
                                    if (it.valueType.supportsText) {
                                        it.valueType.toText(it)
                                    } else {
                                        it.valueType.name
                                    }
                                }
                                .joinToString("")
                        RootEnv.echo(0, brightWhite(result), false)
                        return StringValue(result)
                    }
                }
            )
        }

        fun createRootEnv(): Env {
            RootEnv.installStaticDefinitions()
            RootEnv.installDefinitionValues()
            for (f in rootFunctions) {
                RootEnv.addVariable(f.name, f)
            }
            return RootEnv
        }
    }
}

/** The root scope of a model. This is the scope where builtins are defined and installed. */
object RootEnv : Env(emptyList(), null) {
    fun addDefinition(def: Definition) {
        defs[def.name] = def
    }

    override val valueTypes by lazy {
        hashMapOf(
            "Int" to IntegerValueType,
            "Float" to FloatValueType,
            "String" to StringValueType,
            "Polygon" to SPolygonType,
            "Solid" to SolidType,
            "CrossSection" to SliceType,
            "Box" to BoundingBoxType,
            "Rect" to BoundingRectType,
            "Material" to SMaterialType,
            "MeshGL" to SMeshGLType,
            "Vec2" to Vec2Type,
            "Vec3" to Vec3Type,
            "RGBA" to RGBAType,
            "None" to NoneType,
            "Any" to AnyType,
        )
    }

    override val id: String = "Root"

    var echo: (level: Int, output: Any?, err: Boolean) -> Unit = { l, o, e ->
        if (e) {
            System.err.println(o)
        } else {
            System.out.println(o)
        }
    }
}
