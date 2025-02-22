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
import org.goodmath.simplex.runtime.values.Signature
import org.goodmath.simplex.runtime.values.ValueType

open class SimplexError(
    val kind: Kind,
    val detail: String,
    var location: Location? = null,
    cause: Throwable? = null,
) : Exception(cause) {
    enum class Kind {
        UndefinedSymbol,
        UndefinedVariable,
        UndefinedMethod,
        InvalidMethodSignature,
        UnsupportedOperation,
        InvalidParameter,
        InvalidIndex,
        IncorrectType,
        ParameterCount,
        InvalidValue,
        Evaluation,
        Internal,
        Parser,
        Analysis;

        override fun toString(): String {
            return when (this) {
                UndefinedSymbol -> "Undefined"
                UndefinedVariable -> "Undefined variable"
                UndefinedMethod -> "Undefined method"
                UnsupportedOperation -> "Operation not supported by type"
                InvalidMethodSignature -> "Invalid parameter error:"
                InvalidParameter -> "Invalid parameter"
                InvalidIndex -> "Invalid index"
                IncorrectType -> "Incorrect type"
                ParameterCount -> "Incorrect number of parameters."
                InvalidValue -> "Invalid value"
                Internal -> "Internal execution error"
                Evaluation -> "Evaluation error"
                Parser -> "Parsing errors"
                Analysis -> "Analysis error"
            }
        }
    }

    override val message: String
        get() {
            val prefix =
                if (location != null) {
                    val l = location!!
                    "At ${l.file}(${l.line}, ${l.col-1}): "
                } else {
                    "Unknown location: "
                }
            return if (cause != null) {
                "$prefix $kind $detail; caused by:\n $cause"
            } else {
                "$prefix $kind $detail"
            }
        }
}

class SimplexParameterCountError(
    val callable: String,
    val expected: String,
    val actual: String,
    location: Location? = null,
) :
    SimplexError(
        Kind.ParameterCount,
        "$callable expected one of $expected, but received $actual",
        location,
    )

class SimplexInvalidMethodSignature(
    val type: String,
    val method: String,
    val methodSig: String,
    val argTypes: String,
    loc: Location? = null
): SimplexError(
    Kind.InvalidMethodSignature,
    "method $method of type $type can take one of $methodSig as a parameter list, but received ${argTypes}",
    loc
)

class SimplexInvalidParameterError(
    val callable: String,
    val name: String,
    val expected: ValueType,
    val actual: ValueType,
    val expr: String,
    location: Location? = null,
) :
    SimplexError(
        Kind.InvalidParameter,
        "Parameter $name of $callable expects a value of type ${expected.name}, but received $expr, which has type ${actual.name}",
        location = location,
    )

open class SimplexUndefinedError(val name: String, val symbolKind: String, loc: Location? = null) :
    SimplexError(Kind.UndefinedSymbol, "symbol '$name' of kind $symbolKind", loc)

class SimplexUndefinedMethodError(name: String, ofType: String, loc: Location?):
        SimplexError(Kind.UndefinedMethod, "'$name' of type '$ofType'")

class SimplexUndefinedVariableError(name: String, loc: Location? = null):
        SimplexError(SimplexError.Kind.UndefinedVariable, name, loc)

class SimplexUnsupportedOperation(val type: String, val op: String, loc: Location? = null) :
    SimplexError(Kind.UnsupportedOperation, "$op in type $type", location = loc)

class SimplexEvaluationError(msg: String, cause: Throwable? = null, loc: Location? = null) :

    SimplexError(Kind.Evaluation, msg, cause = cause, location = loc)

class SimplexInternalError(msg: String, cause: Throwable? = null) :
    SimplexError(Kind.Internal, msg, cause = cause)

class SimplexTypeError(value: String, expected: String, actual: String, location: Location? = null) :
    SimplexError(
        Kind.IncorrectType,
        "expected a $expected, but received '$value', which is type $actual",
        location = location,
    )

class SimplexAnalysisError(msg: String, cause: Throwable? = null, loc: Location? = null) :
    SimplexError(Kind.Analysis, msg, cause = cause, location = loc)
