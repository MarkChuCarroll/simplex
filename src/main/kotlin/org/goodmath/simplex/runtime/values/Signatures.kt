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

import org.goodmath.simplex.ast.MethodType
import org.goodmath.simplex.ast.Type
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable

/**
 * Signatures for invokable values.
 *
 * These are almost the same as the static types - except that they
 * carry a bit of extra information that helps generate better error
 * messages when there's a type error in the model.
 */
abstract class Signature: Twistable {
    abstract fun toStaticType(): Type
}

data class FunctionSignature(
    val params: List<Param>,
    val returnType: Type
): Signature() {
    override fun twist(): Twist =
        Twist.obj("FunctionSignature",
            Twist.array("params", params),
            Twist.value("resultType", returnType)
        )

    override fun toStaticType(): Type =
        Type.function(params.map { it.type }, returnType)

}

data class Param(val name: String, val type: Type): Twistable {
    override fun twist(): Twist =
        Twist.obj("Param",
            Twist.attr("name", name),
            Twist.attr("type", type.toString()))
}

data class MethodSignature(
    val self: Type,
    val params: List<Param>,
    val returnType: Type,
): Signature() {
    override fun twist(): Twist =
        Twist.obj("MethodSig",
            Twist.value("selfType", self),
            Twist.array("params", params),
            Twist.value("returnType", returnType))

    override fun toStaticType(): MethodType =
        Type.method(self, params.map { it.type }, returnType)

}
