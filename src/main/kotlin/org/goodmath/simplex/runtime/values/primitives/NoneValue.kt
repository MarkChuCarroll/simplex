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
package org.goodmath.simplex.runtime.values.primitives

import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist

object NoneValue : Value {
    override val valueType: ValueType = NoneValueType

    override fun twist(): Twist {
        return Twist.obj("None")
    }
}

object NoneValueType : ValueType() {
    override val name: String = "None"

    override val asType: Type by lazy {
        Type.simple(name)
    }

    override fun isTruthy(v: Value): Boolean {
        return false
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesPrimitiveMethods: List<PrimitiveMethod> = emptyList()
    override val providesVariables: Map<String, Value> by lazy { mapOf("none" to NoneValue) }

    override fun assertIs(v: Value): Value {
        return if (v == NoneValue) {
            v
        } else {
            throwTypeError(v)
        }
    }
}
