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

import kotlin.collections.indexOfFirst
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.types.Parameter
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.RootEnv
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.values.primitives.DataValueType
import org.goodmath.simplex.twist.Twist

class DataDefinition(name: String, val fields: List<Parameter>, loc: Location) :
    Definition(name, loc) {

    val valueType = DataValueType(this)

    override fun twist(): Twist =
        Twist.obj("DataDefinition", Twist.attr("name", name), Twist.array("fields", fields))

    override fun installValues(env: Env) {}

    override fun validate(env: Env) {
        // Nothing to check here.
    }

    fun indexOf(fieldName: String): Int {
        val idx = fields.indexOfFirst { it.name == fieldName }
        if (idx < 0) {
            throw SimplexUndefinedError(fieldName, "data field of $name", loc = loc)
        } else {
            return idx
        }
    }

    override fun installStatic(env: Env) {
        val dataType = DataValueType(this)
        val tt = Type.simple(name)
        Type.registerValueType(RootEnv, tt, dataType)
    }
}
