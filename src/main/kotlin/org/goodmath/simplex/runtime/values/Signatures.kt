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
package org.goodmath.simplex.runtime.values

import org.goodmath.simplex.ast.types.MethodType
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable

/**
 * Signatures for invokable values.
 *
 * These are almost the same as the static types - except that they carry a bit of extra information
 * that helps generate better error messages when there's a type error in the model.
 */
abstract class Signature : Twistable {
    abstract fun toStaticType(): Type
}

class FunctionSignature private constructor(val params: List<List<Param>>, val returnType: Type) : Signature() {

    override fun twist(): Twist =
        Twist.obj(
            "FunctionSignature",
            Twist.array("params", params.map { Twist.array("option", it) }),
            Twist.value("resultType", returnType),
        )

    override fun toStaticType(): Type = Type.function(params.map { it.map { it.type } }, returnType)

    companion object {
        fun simple(params: List<Param>, returnType: Type): FunctionSignature {
            return FunctionSignature(listOf(params), returnType)
        }

        fun multi(params: List<List<Param>>, returnType: Type): FunctionSignature {
            return FunctionSignature(params, returnType)
        }
    }
}

data class Param(val name: String, val type: Type) : Twistable {
    override fun twist(): Twist =
        Twist.obj("Param", Twist.attr("name", name), Twist.attr("type", type.toString()))
}

class MethodSignature private constructor(val self: Type, val paramSets: List<List<Param>>, val returnType: Type) :
    Signature() {

    override fun twist(): Twist =
        Twist.obj(
            "MethodSig",
            Twist.value("selfType", self),
            Twist.array("params", paramSets.map { Twist.array("option", it) }),
            Twist.value("returnType", returnType),
        )

    override fun toStaticType(): MethodType = Type.multiMethod(self,
        paramSets.map { alt -> alt.map { it.type } }, returnType)

    companion object {
        fun simple(target: Type, params: List<Param>, returnType: Type): MethodSignature {
            return MethodSignature(target, listOf(params), returnType)
        }

        fun multi(target: Type, paramSets: List<List<Param>>, returnType: Type): MethodSignature {
            return MethodSignature(target, paramSets, returnType)
        }
    }
}
