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

import com.sun.org.apache.xalan.internal.xsltc.compiler.sym
import org.goodmath.simplex.ast.Expr
import org.goodmath.simplex.ast.Location

open class SimplexError(
    val kind: Kind,
    val detail: String,
    val location: Location? = null,
    cause: Throwable? = null): Exception(cause) {
    enum class Kind {
        UndefinedSymbol, UnsupportedOperation,
        InvalidParameter, InvalidIndex,
        IncorrectType, ParameterCount,
        InvalidValue, Internal, Parser;

        override fun toString(): String {
            return when (this) {
                Kind.UndefinedSymbol -> "Undefined symbol"
                Kind.UnsupportedOperation -> "Operation not supported by type"
                Kind.InvalidParameter -> "Invalid parameter"
                Kind.InvalidIndex -> "Invalid index"
                Kind.IncorrectType -> "Incorrect type"
                Kind.ParameterCount -> "Incorrect number of parameters"
                Kind.InvalidValue -> "Invalid value"
                Kind.Internal -> "Internal execution error"
                Kind.Parser -> "Parsing errors"
            }
        }
    }

    override val message: String
        get() {
            val prefix = if (location != null) {
                "@(${location.line}, ${location.col}): "
            } else {
                "@?: "
            }
            return "$prefix $kind: $detail"
        }
}

class SimplexIndexError(val expr: Expr, msg: String) : SimplexError(Kind.InvalidIndex, msg, expr.loc)

class SimplexParameterCountError(val expected: Int, val actual: Int,
                                 location: Location?=null):
        SimplexError(SimplexError.Kind.ParameterCount, "Expected $expected, but received $actual",
            location)

class SimplexUndefinedError(val name: String, val symbolKind: String) : SimplexError(Kind.UndefinedSymbol,
    "$name: $symbolKind")


class SimplexUnsupportedOperation(val type: String, val op: String) :
    SimplexError(Kind.UnsupportedOperation, "$op in type $type")

class SimplexEvaluationError(msg: String) : SimplexError(Kind.Internal,
    msg)

class SimplexTypeError(expected: String, actual: String,
    location: Location? = null): SimplexError(Kind.IncorrectType, "expected a $expected, but received a $actual")

