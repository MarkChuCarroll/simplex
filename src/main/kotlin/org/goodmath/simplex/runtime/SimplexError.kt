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

import org.goodmath.simplex.ast.Expr

open class SimplexError(val msg: String, cause: Throwable? = null): Throwable(msg, cause)

class SimplexIndexError(val expr: Expr, msg: String) : SimplexError(msg)

class SimplexUndefinedError(val name: String, val kind: String) : SimplexError("$kind with name $name not found")

class SimplexUnsupportedOperation(val type: String, val op: String) :
    SimplexError("Type $type does not support $op")

class SimplexEvaluationError(msg: String) : SimplexError(msg)

class SimplexTypeError(msg: String): SimplexError(msg)
