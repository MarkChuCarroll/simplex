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
package org.goodmath.simplex.ast

import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable

abstract class Type: Twistable


class SimpleType(val name: String): Type() {
    override fun twist(): Twist =
        Twist.obj("SimpleType",
            Twist.attr("name", name)
            )
}

class ArrayType(val elementType: Type): Type() {
    override fun twist(): Twist =
        Twist.obj("ArrayType",
            Twist.value("elementType", elementType))

}

