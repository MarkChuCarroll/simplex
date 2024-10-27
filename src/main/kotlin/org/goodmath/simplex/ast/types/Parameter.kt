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

import org.goodmath.simplex.ast.AstNode
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.expr.Expr
import org.goodmath.simplex.twist.Twist

open class Parameter(val name: String, val type: Type, loc: Location? = null) : AstNode(loc) {
    override fun twist(): Twist =
        Twist.obj("Parameter", Twist.attr("name", name), Twist.value("type", type))

    override fun toString(): String {
        return "Parameter(${name}:${type})"
    }
}

class KwParameter(name: String, type: Type, val defaultValue: Expr, loc: Location?): Parameter(name, type, loc) {
    override fun twist(): Twist =
        Twist.obj("KwParameter", Twist.attr("name", name), Twist.value("type", type),
            Twist.value("defaultValue", defaultValue))

    override fun toString(): String {
        return "KwParameter(${name}:${type}=${defaultValue})"
    }
}
