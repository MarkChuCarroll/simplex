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

import java.util.HashMap
import kotlin.collections.all
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.collections.zip
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable

abstract class Type : Twistable {

    abstract fun matchedBy(t: Type): Boolean

    companion object {
        private val types = HashMap<String, Type>()
        val IntType = simple("Int")
        val FloatType = simple("Float")
        val StringType = simple("String")
        val PolygonType = simple("Polygon")
        val BooleanType = simple("Boolean")
        val AnyType = simple("Any")
        val SolidType = simple("Solid")
        val SliceType = simple("Slice")
        val Vec2Type = simple("Vec2")
        val Vec3Type = simple("Vec3")
        val RGBAType = simple("RGBA")
        val BoundingRectType = simple("BoundingRect")
        val BoundingBoxType = simple("BoundingBox")
        val MeshType = simple("Mesh")
        val MaterialType = simple("Material")
        val NoneType = simple("None")

        operator fun get(name: String): Type? = types[name]

        fun all(): List<Type> {
            return types.values.toList()
        }

        fun simple(name: String): SimpleType {
            return types.computeIfAbsent(name) { n -> SimpleType(n) } as SimpleType
        }

        fun array(baseType: Type): ArrayType {
            val name = "[$baseType]"
            return types.computeIfAbsent(name) { n -> ArrayType(baseType) } as ArrayType
        }

        fun simpleMethod(target: Type, args: List<Type>, result: Type): MethodType {
            return multiMethod(target, listOf(args), result)
        }

        fun multiMethod(target: Type, argSets: List<List<Type>>, result: Type): MethodType {
            val argsStr = argSets.map { argSet ->
                argSet.map { arg -> arg.toString() }.joinToString(", ")
            }.joinToString("|")
            val name = "$target->($argsStr):$result"
            return types.computeIfAbsent(name) { _ -> MethodType(target, argSets, result) }
                    as MethodType
        }

        fun function(argOptions: List<List<Type>>, result: Type): FunctionType {
            val argOptStrings =
                argOptions
                    .map { args -> args.map { it.toString() }.joinToString(",") }
                    .joinToString("|")
            val name = "($argOptStrings):$result"
            return types.computeIfAbsent(name) { _ -> FunctionType(argOptions, result) }
                as FunctionType
        }
    }

    fun registerMethod(name: String, type: MethodType) {
        methods[name] = type
    }

    fun getMethod(name: String): MethodType? {
        return methods[name]
    }

    val methods = HashMap<String, MethodType>()
}

class SimpleType internal constructor(val name: String) : Type() {
    override fun twist(): Twist = Twist.obj("SimpleType", Twist.attr("name", name))

    override fun toString(): String {
        return name
    }

    override fun matchedBy(t: Type): Boolean {
        return if (this == Type.AnyType) {
            true
        } else if (t is SimpleType) {
            t.name == name
        } else {
            false
        }
    }
}

class ArrayType internal constructor(val elementType: Type) : Type() {
    override fun twist(): Twist = Twist.obj("ArrayType", Twist.value("elementType", elementType))

    override fun toString(): String {
        return "[$elementType]"
    }

    override fun matchedBy(t: Type): Boolean {
        return if (t is ArrayType) {
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
