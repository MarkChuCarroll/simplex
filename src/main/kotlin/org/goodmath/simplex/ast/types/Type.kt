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

import org.goodmath.simplex.runtime.RootEnv
import org.goodmath.simplex.runtime.SimplexAnalysisError
import org.goodmath.simplex.runtime.values.AnyValueType
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

        fun simpleMethod(target: Type, args: List<Type>, result: Type): MethodType {
            val result = multiMethod(target, listOf(args), result)
            return result
        }

        fun multiMethod(target: Type, argSets: List<List<Type>>, result: Type): MethodType {
            val argsStr = argSets.map { argSet ->
                argSet.map { arg -> arg.toString() }.joinToString(", ")
            }.joinToString("|")
            val name = "$target->($argsStr):$result"
            val result = Type.types.computeIfAbsent(name) { _ ->
                 MethodType(target, argSets, result)
            } as MethodType
            if (!valueTypes.contains(result)) {
                valueTypes[result] = MethodValueType(result)
            }
            return result
        }

        fun function(argOptions: List<List<Type>>, result: Type): FunctionType {
            val argOptStrings =
                argOptions
                    .map { args -> args.map { it.toString() }.joinToString(",") }
                    .joinToString("|")
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
            t.name == name || name == "Any"
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

class FunctionType internal constructor(val argLists: List<List<Type>>, val returnType: Type) :
    Type() {

    private data class ArgTypeList(val argList: List<Type>) : Twistable {
        override fun twist(): Twist {
            return Twist.array("ArgList", argList)
        }
    }

    override fun twist(): Twist =
        Twist.obj(
            "FunctionType",
            Twist.value("return", returnType),
            Twist.array("args", argLists.map { ArgTypeList(it) }),
        )

    override fun toString(): String {
        val argOptStrings =
            argLists.map { args -> args.map { it.toString() }.joinToString(",") }.joinToString("|")
        return "($argOptStrings):$returnType"
    }

    override fun matchedBy(t: Type): Boolean {
        return if (t is FunctionType) {
            returnType == t.returnType &&
                argLists.all { myArgOption ->
                    t.argLists.any { theirArgOption ->
                        myArgOption.size == theirArgOption.size &&
                            myArgOption.zip(theirArgOption).all { (l, r) -> l.matchedBy(r) }
                    }
                }
        } else {
            false
        }
    }
}

class MethodType
internal constructor(val target: Type, val argSets: List<List<Type>>, val returnType: Type) : Type() {
    override fun twist(): Twist =
        Twist.obj(
            "MethodType",
            Twist.value("target", target),
            Twist.value("return", returnType),
            Twist.array("args", argSets.map { Twist.array("option", it) }),
        )

    override fun toString(): String {
        val argStr = argSets.map { argSet ->
            argSet.map { arg -> arg.toString() }.joinToString(", ")
        }.joinToString("|")
        val resultStr = returnType.toString()
        val targetStr = target.toString()
        return "$targetStr->($argStr):$resultStr"
    }

    override fun matchedBy(t: Type): Boolean {
        return if (t is MethodType) {
            target.matchedBy(t.target) &&
                    argSets.all { args ->
                        t.argSets.any { tArgs ->
                            (args.size == tArgs.size) &&
                                    args.zip(tArgs).all { (l, r) ->
                                        l.matchedBy(r)
                                    }
                        }
                    } &&
                returnType.matchedBy(t.returnType)
        } else {
            false
        }
    }
}
