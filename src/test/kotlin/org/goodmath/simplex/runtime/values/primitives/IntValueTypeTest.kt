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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.goodmath.simplex.runtime.RootEnv
import org.junit.jupiter.api.Assertions.assertTrue

class IntValueTypeTest {
    @Test
    fun testOperations() {
        RootEnv.installStaticDefinitions()
        RootEnv.installDefinitionValues()

        val v1 = IntegerValue(3)
        val v2 = IntegerValue(4)
        val v3 = IntegerValue(5)

        val sum = IntegerValueType.applyMethod(v1, "plus", listOf(v2), emptyMap(), RootEnv)
        sum as IntegerValue

        val prod = IntegerValueType.applyMethod(v2, "times", listOf(v3), emptyMap(), RootEnv)
        prod as IntegerValue
        val divI = IntegerValueType.applyMethod(v3, "div", listOf(v1), emptyMap(), RootEnv)
        divI as IntegerValue
        val modI = IntegerValueType.applyMethod(prod, "mod", listOf(sum), emptyMap(), RootEnv)
        modI as IntegerValue

        assertEquals(7, sum.i)
        assertEquals(20, prod.i)
        assertEquals(1, divI.i)
        assertEquals(6, modI.i)

        val eq1 =
            IntegerValueType.applyMethod(divI, "eq", listOf(IntegerValue(1)), emptyMap(), RootEnv)
                as BooleanValue
        assertTrue(eq1.b)
        val eq2 =
            IntegerValueType.applyMethod(prod, "eq", listOf(IntegerValue(6)), emptyMap(), RootEnv)
                as BooleanValue
        assertFalse(eq2.b)

        val eq3 =
            IntegerValueType.applyMethod(
                IntegerValueType.applyMethod(modI, "neg", emptyList(), emptyMap(), RootEnv),
                "eq",
                listOf(
                    IntegerValueType.applyMethod(IntegerValue(0), "minus", listOf(modI), emptyMap(), RootEnv)
                ), emptyMap(),
                RootEnv,
            ) as BooleanValue

        assertTrue(eq3.b)

        val cmp1 =
            IntegerValueType.applyMethod(
                IntegerValue(-1),
                "compare",
                listOf(IntegerValue(-2)), emptyMap(),
                RootEnv,
            ) as IntegerValue
        val cmp2 =
            IntegerValueType.applyMethod(
                IntegerValue(-1),
                "compare",
                listOf(IntegerValue(2)), emptyMap(),
                RootEnv,
            ) as IntegerValue
        val cmp3 =
            IntegerValueType.applyMethod(
                IntegerValue(13),
                "compare",
                listOf(IntegerValue(13)), emptyMap(),
                RootEnv,
            ) as IntegerValue
        assertEquals(1, cmp1.i)
        assertEquals(-1, cmp2.i)
        assertEquals(0, cmp3.i)
        val p =
            IntegerValueType.applyMethod(IntegerValue(3), "pow", listOf(IntegerValue(4)), emptyMap(), RootEnv)
                as IntegerValue

        assertEquals(81, p.i)
    }
}
