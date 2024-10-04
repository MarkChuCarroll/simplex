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
package org.goodmath.simplex.ast.expr

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.def.DataDefinition
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.types.TypedName
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.StringValue
import org.goodmath.simplex.runtime.values.primitives.DataValue
import org.junit.jupiter.api.BeforeEach

/** Some basic tests of tuple types. */
class TupleTests {

    val rootEnv = Env.createRootEnv()
    lateinit var env: Env

    var idx = 0

    fun mockLoc(): Location {
        idx++
        return Location("test", idx, 0)
    }

    @BeforeEach
    fun setupEnvironment() {
        val tupleTypeOne =
            DataDefinition(
                "TestTupleOne",
                listOf(
                    TypedName("iFoo", Type.simple("Int"), mockLoc()),
                    TypedName("sBar", Type.simple("String"), mockLoc()),
                    TypedName("fBaz", Type.simple("Float"), mockLoc()),
                ),
                mockLoc(),
            )
        val tupleTypeTwo =
            DataDefinition(
                "TestTupleTwo",
                listOf(
                    TypedName("tOne", Type.simple("TestTupleOne"), mockLoc()),
                    TypedName("oops", Type.simple("String"), mockLoc()),
                ),
                mockLoc(),
            )

        env = Env(listOf(tupleTypeTwo, tupleTypeOne), rootEnv)
        env.installStaticDefinitions()
        env.installDefinitionValues()
    }

    @Test
    fun testCreateTuple() {
        val createOne =
            DataExpr(
                "TestTupleOne",
                listOf(
                    LiteralExpr(11, mockLoc()),
                    LiteralExpr("garble", mockLoc()),
                    LiteralExpr(PI, mockLoc()),
                ),
                mockLoc(),
            )

        createOne.validate(env)
        val v = createOne.evaluateIn(env)
        assertEquals("TestTupleOne", v.valueType.name)
        v as DataValue
        assertEquals(IntegerValue(11), v.fields[0])

        val createTwo =
            DataExpr("TestTupleTwo", listOf(createOne, LiteralExpr("oops", mockLoc())), mockLoc())
        val w = createTwo.evaluateIn(env)
        assertIs<DataValue>(w)
        val eq = v.valueType.applyMethod(v, "eq", listOf(w.fields[0]), env)
        assertTrue(eq is BooleanValue && eq.b)
        val wOne = w.fields[1] as StringValue
        assertEquals("oops", wOne.s)
        assertNotEquals(v.valueType, w.valueType)
    }

    @Test
    fun testFieldAccess() {
        val createOne =
            DataExpr(
                "TestTupleOne",
                listOf(
                    LiteralExpr(11, mockLoc()),
                    LiteralExpr("garble", mockLoc()),
                    LiteralExpr(PI, mockLoc()),
                ),
                mockLoc(),
            )
        val createTwo =
            DataExpr("TestTupleTwo", listOf(createOne, LiteralExpr("oops", mockLoc())), mockLoc())

        val getExpr = FieldRefExpr(FieldRefExpr(createTwo, "tOne", mockLoc()), "iFoo", mockLoc())
        val result = getExpr.evaluateIn(env)
        assertIs<IntegerValue>(result)
        assertEquals(11, result.i)
    }

    @Test
    fun testFieldUpdate() {
        val createOne =
            DataExpr(
                "TestTupleOne",
                listOf(
                    LiteralExpr(11, mockLoc()),
                    LiteralExpr("garble", mockLoc()),
                    LiteralExpr(PI, mockLoc()),
                ),
                mockLoc(),
            )
        val createTwo =
            DataExpr("TestTupleTwo", listOf(createOne, LiteralExpr("oops", mockLoc())), mockLoc())

        val letExpr = LetExpr("two", Type.simple("TestTupleTwo"), createTwo, mockLoc())
        val update =
            DataFieldUpdateExpr(
                VarRefExpr("two", mockLoc()),
                "oops",
                LiteralExpr("yikes", mockLoc()),
                mockLoc(),
            )
        val block = BlockExpr(listOf(letExpr, update), mockLoc())

        val result = block.evaluateIn(env)

        assertIs<DataValue>(result)
        assertEquals("yikes", (result.fields[1] as StringValue).s)
    }
}
