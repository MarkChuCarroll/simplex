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
package org.goodmath.simplex.runtime.values.solid

import org.goodmath.simplex.runtime.values.manifold.Slice
import org.goodmath.simplex.runtime.values.manifold.SliceValueType
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import kotlin.test.assertEquals
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.junit.jupiter.api.Test

class PolygonTest {
    @Test
    fun testPolygonCreators() {
        val rect = SliceValueType.providesFunctions.first { it.name == "rectangle" }
        val r = rect.execute(listOf(FloatValue(20.0) ,FloatValue(10.0), BooleanValue(true)), emptyMap())
        r as Slice
        assertEquals(
            """(obj Slice
               |   [array points
               |      (obj Vec2
               |         (x '0.0')
               |         (y '0.0'))
               |      (obj Vec2
               |         (x '20.0')
               |         (y '0.0'))
               |      (obj Vec2
               |         (x '20.0')
               |         (y '10.0'))
               |      (obj Vec2
               |         (x '0.0')
               |         (y '10.0'))])
               |"""
                .trimMargin(),
            r.twist().consStr(),
        )
    }
}
