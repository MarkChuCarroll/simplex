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
package org.goodmath.simplex.ast.def

import org.goodmath.simplex.ast.AstNode
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.runtime.Env

/**
 * The supertype of all declarations.
 * @param name the name of the declarations.
 * @param loc the source location
 */
abstract class Definition(val name: String, loc: Location): AstNode(loc) {
    abstract fun installStatic(env: Env)
    abstract fun installValues(env: Env)
    abstract fun validate(env: Env)
}


