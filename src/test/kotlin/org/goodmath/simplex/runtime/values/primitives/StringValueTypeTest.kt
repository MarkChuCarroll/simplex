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

import kotlin.math.PI
import kotlin.test.assertEquals
import org.goodmath.simplex.runtime.RootEnv
import org.goodmath.simplex.runtime.SimplexError
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StringValueTypeTest {

    @Test
    fun testOperations() {
        RootEnv.installStaticDefinitions()
        RootEnv.installDefinitionValues()

        val s1 = StringValue("string one")
        val s2 = StringValue("STRING TWO")
        val s3 = StringValue(PI.toString())

        val a = StringValueType.applyMethod(s1, "plus", listOf(s2), emptyMap(), RootEnv)
        a as StringValue
        assertEquals("string oneSTRING TWO", a.s)

        assertThrows<SimplexError> { StringValueType.applyMethod(s3, "minus", listOf(s2), emptyMap(), RootEnv) }

        val cmp1 = StringValueType.applyMethod(s1, "compare", listOf(s2), emptyMap(), RootEnv) as IntegerValue
        assertTrue(cmp1.i > 0)
        val cmp2 = StringValueType.applyMethod(s2, "compare", listOf(s1), emptyMap(), RootEnv) as IntegerValue
        assertTrue(cmp2.i < 0)
        val cmp3 = StringValueType.applyMethod(s2, "compare", listOf(s2), emptyMap(), RootEnv) as IntegerValue
        assertTrue(cmp3.i == 0)
        val i = StringValueType.applyMethod(s1, "length", emptyList(), emptyMap(), RootEnv) as IntegerValue
        assertEquals(10, i.i)
    }
}
