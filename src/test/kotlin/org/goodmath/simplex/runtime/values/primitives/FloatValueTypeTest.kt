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

import org.goodmath.simplex.runtime.RootEnv
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FloatValueTypeTest {
    @Test
    fun testFloatOperations() {
        val v1 = FloatValue(3.14159)
        val v2 = FloatValue(2.718281828)
        val v3 = FloatValue(sqrt(2.0))
        RootEnv.installStaticDefinitions()
        RootEnv.installDefinitionValues()
        val a = FloatValueType.applyMethod(v1, "plus", listOf(v2), RootEnv)
        assertEquals(FloatValueType, a.valueType)
        a as FloatValue
        assertEquals(5.859871827999999, a.d, 0.0001)
        val b = FloatValueType.applyMethod(a, "minus", listOf(v3), RootEnv)
        b as FloatValue
        assertEquals(4.445658265626904, b.d, 0.0001)
        val c = FloatValueType.applyMethod(b, "pow", listOf(FloatValue(0.5)), RootEnv)
        c as FloatValue
        assertEquals(2.1084729700963454, c.d, 0.0001)
        val d = FloatValueType.applyMethod(FloatValue(2.1), "times", listOf(c), RootEnv)
        d as FloatValue
        assertEquals(4.427793237202326, d.d)
        val e = FloatValueType.applyMethod(d, "div", listOf(FloatValue(sqrt(3.0))), RootEnv)
        e as FloatValue
        assertEquals(2.5563876174147673, e.d)

        val eq1 = FloatValueType.applyMethod(e, "eq", listOf(d), RootEnv) as BooleanValue
        assertFalse(eq1.b)
        val eq2 = FloatValueType.applyMethod(d, "eq", listOf(d), RootEnv) as BooleanValue
        assertTrue(eq2.b)

        val cmp1 = FloatValueType.applyMethod(c, "compare", listOf(d), RootEnv) as IntegerValue
        val cmp2 = FloatValueType.applyMethod(d, "compare", listOf(c), RootEnv) as IntegerValue
        val cmp3 = FloatValueType.applyMethod(c, "compare", listOf(c), RootEnv) as IntegerValue
        assertEquals(-1, cmp1.i)
        assertEquals(1, cmp2.i)
        assertEquals(0, cmp3.i)

        assertTrue(FloatValueType.isTruthy(c))
        assertFalse(FloatValueType.isTruthy(FloatValueType.applyMethod(c, "minus", listOf(c), RootEnv)))

    }
}
