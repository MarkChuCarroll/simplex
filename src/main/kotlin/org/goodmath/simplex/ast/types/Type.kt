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
package org.goodmath.simplex.ast.types

import org.goodmath.simplex.ast.expr.Arguments
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.RootEnv
import org.goodmath.simplex.runtime.SimplexAnalysisError
import org.goodmath.simplex.runtime.values.AnyValueType
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.ParameterSignature
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.manifold.BoundingBoxValueType
import org.goodmath.simplex.runtime.values.manifold.BoundingRectValueType
import org.goodmath.simplex.runtime.values.manifold.SMeshGLType
import org.goodmath.simplex.runtime.values.manifold.SPolygonType
import org.goodmath.simplex.runtime.values.manifold.SSmoothnessType
import org.goodmath.simplex.runtime.values.manifold.SliceValueType
import org.goodmath.simplex.runtime.values.manifold.SolidValueType
import org.goodmath.simplex.runtime.values.primitives.VectorValueType
import org.goodmath.simplex.runtime.values.primitives.BooleanValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.FunctionValueType
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.runtime.values.primitives.MethodValueType
import org.goodmath.simplex.runtime.values.primitives.NoneValueType
import org.goodmath.simplex.runtime.values.primitives.StringValueType
import org.goodmath.simplex.runtime.values.primitives.Vec2ValueType
import org.goodmath.simplex.runtime.values.primitives.Vec3ValueType
import java.util.HashMap
import kotlin.collections.all
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.collections.zip
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable

/**
 * Static types. These types are used for analysis and typechecking.
 */
abstract class Type : Twistable {
    abstract fun matchedBy(t: Type): Boolean

    companion object {

        private val types: HashMap<String, Type> = HashMap()

        val valueTypes: HashMap<Type, ValueType> = HashMap()

        val builtinValueTypes = listOf(
            IntegerValueType, FloatValueType, StringValueType,
            BooleanValueType,
            Vec2ValueType, Vec3ValueType,
            BoundingBoxValueType,
            SolidValueType,
            BoundingRectValueType,
            SPolygonType,
            SliceValueType,
            SMeshGLType, SSmoothnessType,
            NoneValueType, AnyValueType
        )

        init {
            builtinValueTypes.forEach {
                val simp = SimpleType(it.name)
                registerValueType(simp, it)
            }
        }

        operator fun get(name: String): Type? = types[name]

        fun getValueType(type: Type): ValueType {
            return valueTypes[type] ?: throw SimplexAnalysisError("Unknown value type $type")
        }

        fun all(): List<Type> {
            return types.values.toList()
        }

        fun registerValueType(type: Type, valueType: ValueType) {
            if (!valueTypes.containsKey(type)) {
                valueTypes[type] = valueType
                valueType.methods
                for (f in valueType.providesFunctions) {
                    RootEnv.declareTypeOf(f.name, f.valueType.asType)
                    RootEnv.addVariable(f.name, f)
                }
            }
        }

        fun simple(name: String): SimpleType {
            return Type.types.computeIfAbsent(name) { n ->
             SimpleType(n)
            } as SimpleType
        }

        fun vector(baseType: Type): VectorType {
            val name = "[$baseType]"
            val result = Type.types.computeIfAbsent(name) { n ->
                VectorType(baseType)
            } as VectorType
            if (!valueTypes.containsKey(result)) {
                registerValueType(result, VectorValueType(valueTypes[baseType]!!))
            }
            return result
        }

        fun simpleMethod(target: Type, argSpecs: ArgumentListSpec, result: Type): MethodType {
            val result = multiMethod(target, listOf(argSpecs), result)
            return result
        }

        fun multiMethod(target: Type, argSets: List<ArgumentListSpec>, result: Type): MethodType {
            val argsStr = argSets.map { it.toString() }.joinToString("|")
            val name = "$target->($argsStr):$result"
            val result = Type.types.computeIfAbsent(name) { _ ->
                 MethodType(target, argSets, result)
            } as MethodType
            if (!valueTypes.containsKey(result)) {
                valueTypes[result] = MethodValueType(result)
            }
            return result
        }

        fun function(sig: FunctionSignature): FunctionType {
            val result = Type.types.computeIfAbsent(sig.toString()) { _ ->
                FunctionType(sig.paramSigs.map { ArgumentListSpec(it) }, sig.returnType)
            } as FunctionType
            if (!valueTypes.containsKey(result)) {
                valueTypes[result] = FunctionValueType(result)
            }
            return result
        }

        fun function(argOptions: List<ArgumentListSpec>, result: Type): FunctionType {
            val argOptStrings =
                argOptions.map { it.toString() }.joinToString("|")
            val name = "($argOptStrings):$result"
            val result = Type.types.computeIfAbsent(name) { _ -> FunctionType(argOptions, result) }
                as FunctionType
            if (!valueTypes.containsKey(result)) {
                valueTypes[result] = FunctionValueType(result)
            }
            return result
        }

        val IntType = simple("Int")
        val FloatType = simple("Float")
        val StringType = simple("String")
        val BooleanType = simple("Boolean")
        val Vec2Type = simple("Vec2")
        val Vec3Type = simple("Vec3")
        val BoundingBoxType = simple("BoundingBox")
        val SolidType = simple("Solid")
        val BoundingRectType = simple("BoundingRect")
        val PolygonType = simple("Polygon")
        val SliceType = simple("Slice")
        val MeshType = simple("Mesh")
        val SmoothnessType = simple("Smoothness")
        val NoneType = simple("None")
        val AnyType = simple("Any")
    }

    fun registerMethod(name: String, type: MethodType) {
        methods[name] = type
    }

    fun getMethod(name: String): MethodType? {
        return methods[name]
    }

    val methods = HashMap<String, MethodType>()
}

data class SimpleType constructor(val name: String) : Type() {
    override fun twist(): Twist = Twist.obj("SimpleType", Twist.attr("name", name))

    override fun toString(): String {
        return name
    }

    override fun matchedBy(t: Type): Boolean {
        return if (t is SimpleType) {
            t.name == name
        } else {
            false
        }
    }
}

class VectorType internal constructor(val elementType: Type) : Type() {
    override fun twist(): Twist = Twist.obj("VectorType", Twist.value("elementType", elementType))

    override fun toString(): String {
        return "[$elementType]"
    }

    override fun matchedBy(t: Type): Boolean {
        return if (t is VectorType) {
            elementType.matchedBy(t.elementType)
        } else {
            false
        }
    }
}

data class ArgumentListSpec(val positional: List<Type>, val keyword: Map<String, Type>): Twistable {
    constructor(params: ParameterSignature): this(params.positionalParameters.map { it.type },
        params.keywordParameters.associate { it.name to it.type })

    override fun twist(): Twist = Twist.obj("ArgumentListSpec",
        Twist.array("positional", positional),
        Twist.array("keyword", keyword.map { (k, v) -> Twist.value(k, v) }))

    override fun toString(): String {
        val pos = positional.map { it.toString() }.joinToString(",")
        return if (keyword.isEmpty()) {
            pos
        } else {
            val kw = keyword.map { (k, v) -> "$k:$v" }.joinToString(",")
            return "$pos;$kw"
        }
    }

    fun matchedBy(args: Arguments, env: Env): Boolean {
        System.err.println("Expected count = ${positional.size}, actual = ${args.positionalArgs.size}")
        if (args.positionalArgs.size != positional.size) {
            return false
        }
        if (!positional.zip(args.positionalArgs).all { (spec, arg) ->
                val argType = arg.resultType(env)
                spec.matchedBy(argType)
            }) {
            System.err.println("Mismatch in positionals")
            return false
        }
        if (!args.kwArgs.all { (kwArgName, kwArgExpr) ->
                val kwSpec = keyword[kwArgName]
                kwSpec != null && kwSpec.matchedBy(kwArgExpr.resultType(env))
            }) {
            System.err.println("Mismatch in KW")
            return false
        }
        return true
    }
}


class FunctionType internal constructor(
    val argOptions: List<ArgumentListSpec>, val returnType: Type
) : Type() {
    override fun twist(): Twist =
        Twist.obj(
            "FunctionType",
            Twist.value("return", returnType),
            Twist.array("args", argOptions)
        )

    override fun toString(): String {
        val argOptStrings =
            argOptions.map { it.toString() }.joinToString(",")
        return "($argOptStrings):$returnType"
    }

    override fun matchedBy(t: Type): Boolean {
        // function type A is matched by function type B if:
        // - A and B  have the same return type
        // - Every possible argument list in A has a matching list in B.
        //
        // Argument lists match if:
        // - for every positional argument a_i in A and b_i in B, a_i is matched by b_i;
        // - for every keyword argument k_a in A, B contains a keyword argument k_b with
        //   the same name, and the type of k_a is matched by the type of k_b.
        return if (t !is FunctionType ||  t.returnType != returnType) {
            false
        } else {
            argOptions.all { myArgOpt ->
                t.argOptions.any { theirArgOpt ->
                    myArgOpt.positional.size == theirArgOpt.positional.size &&
                            myArgOpt.positional.zip(theirArgOpt.positional).all { (l, r) -> l.matchedBy(r) } &&
                            myArgOpt.keyword.all { (k, v) ->
                                theirArgOpt.keyword.containsKey(k) &&
                                        v.matchedBy(theirArgOpt.keyword[k]!!)
                            }
                }
            }
        }
    }
}

class MethodType
internal constructor(val target: Type, val argSets: List<ArgumentListSpec>, val returnType: Type) : Type() {
    override fun twist(): Twist =
        Twist.obj(
            "MethodType",
            Twist.value("target", target),
            Twist.value("return", returnType),
            Twist.array("args", argSets)
        )

    override fun toString(): String {
        val argStr = argSets.map { argSet ->
            argSet.toString()
        }.joinToString("|")
        val resultStr = returnType.toString()
        val targetStr = target.toString()
        return "$targetStr->($argStr):$resultStr"
    }

    override fun matchedBy(t: Type): Boolean {
        return (t is MethodType) &&
            target.matchedBy(t.target) &&
                returnType.matchedBy(t.returnType) &&
                argSets.all { myArgs ->
                    t.argSets.any { theirArgs ->
                            (myArgs.positional.size == theirArgs.positional.size&&
                                    myArgs.positional.zip(theirArgs.positional).all { (l, r) ->
                                        l.matchedBy(r)
                                    }) &&
                                    (myArgs.keyword.all { (kwName, kwType) ->
                                        theirArgs.keyword.containsKey(kwName) &&
                                                kwType.matchedBy(theirArgs.keyword[kwName]!!)
                                    })
                    }
                }
    }
}
