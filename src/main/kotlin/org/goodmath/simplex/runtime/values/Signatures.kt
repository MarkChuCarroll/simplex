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

import org.goodmath.simplex.ast.types.ArgumentListSpec
import org.goodmath.simplex.ast.types.KwParameter
import org.goodmath.simplex.ast.types.MethodType
import org.goodmath.simplex.ast.types.Parameter
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable

data class ParameterSignature(
    val positionalParameters: List<Parameter>,
    val keywordParameters: List<KwParameter> = emptyList()
): Twistable {
    override fun twist(): Twist =
        Twist.obj("ParameterSignature",
            Twist.array("positionalParameters", positionalParameters),
            Twist.array("keywordParameters", keywordParameters))

    override fun toString(): String {
        val posStr = positionalParameters.map { it.type.toString() }.joinToString(",")
        return if (keywordParameters.isNotEmpty()) {
            val kwStr = keywordParameters.map { "${it.name}:${it.type}" }.joinToString(",")
            "$posStr;$kwStr"
        } else {
            posStr
        }
    }

    companion object {
        val empty: ParameterSignature = ParameterSignature(emptyList(), emptyList())
    }

}

/**
 * Signatures for invokable values.
 *
 * These are almost the same as the static types - except that they carry a bit of extra information
 * that helps generate better error messages when there's a type error in the model.
 */
abstract class Signature : Twistable {
    abstract fun toStaticType(): Type
}

class FunctionSignature private constructor(val paramSigs: List<ParameterSignature>, val returnType: Type) : Signature() {

    override fun twist(): Twist =
        Twist.obj(
            "FunctionSignature",
            Twist.array("paramSigs", paramSigs),
            Twist.value("resultType", returnType),
        )

    override fun toString(): String {
        val paramStr = paramSigs.map { it.toString() }.joinToString("|")
        return "($paramStr):$returnType"
    }

    override fun toStaticType(): Type {
        val argSpecs = paramSigs.map { paramSig ->
            ArgumentListSpec(
                paramSig.positionalParameters.map { it.type },
                paramSig.keywordParameters.associate { it.name to it.type }
            )
        }
        return Type.function(argSpecs, returnType)
    }

    companion object {
        fun simple(params: ParameterSignature, returnType: Type): FunctionSignature {
            return FunctionSignature(listOf(params), returnType)
        }

        fun multi(params: List<ParameterSignature>, returnType: Type): FunctionSignature {
            return FunctionSignature(params, returnType)
        }
    }
}


class MethodSignature private constructor(val self: Type,
                                          val paramSigs: List<ParameterSignature>,
                                          val returnType: Type) :
    Signature() {

    override fun twist(): Twist =
        Twist.obj(
            "MethodSig",
            Twist.value("selfType", self),
            Twist.array("params", paramSigs),
            Twist.value("returnType", returnType),
        )

    override fun toStaticType(): MethodType {
        val argSpecs = paramSigs.map { paramSig ->
            ArgumentListSpec(
                paramSig.positionalParameters.map { it.type },
                paramSig.keywordParameters.associate { it.name to it.type }) }
        return Type.multiMethod(self, argSpecs, returnType)
    }

    companion object {
        fun simple(target: Type, params: ParameterSignature, returnType: Type): MethodSignature {
            return MethodSignature(target, listOf(params), returnType)
        }

        fun multi(target: Type, paramSigs: List<ParameterSignature>, returnType: Type): MethodSignature {
            return MethodSignature(target, paramSigs, returnType)
        }
    }
}
