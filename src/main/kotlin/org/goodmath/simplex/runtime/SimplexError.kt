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

import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.runtime.values.ValueType

open class SimplexError(
    val kind: Kind,
    val detail: String,
    var location: Location? = null,
    cause: Throwable? = null): Exception(cause) {
    enum class Kind {
        UndefinedSymbol, UnsupportedOperation,
        InvalidParameter, InvalidIndex,
        IncorrectType, ParameterCount,
        InvalidValue, Internal, Parser, Analysis;

        override fun toString(): String {
            return when (this) {
                UndefinedSymbol -> "Undefined "
                UnsupportedOperation -> "Operation not supported by type"
                InvalidParameter -> "Invalid parameter"
                InvalidIndex -> "Invalid index"
                IncorrectType -> "Incorrect type"
                ParameterCount -> "Incorrect number of parameters"
                InvalidValue -> "Invalid value"
                Internal -> "Internal execution error"
                Parser -> "Parsing errors"
                Analysis -> "Analysis error"
            }
        }
    }

    override val message: String
        get() {
            val prefix = if (location != null) {
                val l = location!!
                "${l.file}(${l.line}, ${l.col}): "
            } else {
                "@?: "
            }
            if (cause != null) {
                return "$prefix $kind: $detail; caused by:\n $cause"
            } else {
                return "$prefix $kind: $detail"
            }
        }
}

class SimplexParameterCountError(
    val callable: String,
    val expected: List<Int>,
    val actual: Int,
    location: Location?=null):
        SimplexError(Kind.ParameterCount, "$callable expected one of $expected, but received $actual",
            location)

class SimplexInvalidParameterError(val callable: String,
                                   val name: String,
                                   val expected: ValueType,
                                   val actual: ValueType, location: Location? = null):
        SimplexError(Kind.InvalidParameter,
            "Parameter $name of $callable expects a value of type ${expected.name}, but received a ${actual.name}",
            location=location
        )


class SimplexUndefinedError(val name: String, val symbolKind: String,
    loc: Location? = null) : SimplexError(Kind.UndefinedSymbol,
    "$symbolKind '$name'", loc)


class SimplexUnsupportedOperation(val type: String, val op: String, loc: Location? = null) :
    SimplexError(Kind.UnsupportedOperation, "$op in type $type", location=loc)

class SimplexEvaluationError(msg: String, cause: Throwable? = null,
    loc: Location?=null) : SimplexError(Kind.Internal,
    msg, cause=cause, location = loc)

class SimplexTypeError(expected: String, actual: String,
    location: Location? = null): SimplexError(Kind.IncorrectType, "expected a $expected, but received a $actual",
    location=location)

class SimplexAnalysisError(msg: String, cause: Throwable? = null,
    loc: Location?= null): SimplexError(Kind.Analysis, msg, cause=cause, location=loc)
