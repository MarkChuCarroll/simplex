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

import manifold3d.manifold.CrossSection
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.def.VariableDefinition
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.StringValue
import org.junit.jupiter.api.BeforeEach

class VarTests {
    val rootEnv = Env.createRootEnv()
    lateinit var env: Env

    var idx = 0

    fun mockLoc(): Location {
        idx++
        return Location("test", idx, 0)
    }

    @BeforeEach
    fun setupEnvironment() {
        var varDefOne =
            VariableDefinition("one", Type.FloatType, LiteralExpr(PI, mockLoc()), mockLoc())
        var varDefTwo =
            VariableDefinition("two", Type.StringType, LiteralExpr("PI", mockLoc()), mockLoc())
        env = Env(listOf(varDefOne, varDefTwo), rootEnv)
        env.installStaticDefinitions()
        env.installDefinitionValues()
        val c = CrossSection.Square(10.0, 20.0)
    }

    @Test
    fun testVarRef() {
        val v = VarRefExpr("one", mockLoc()).evaluateIn(env)
        assertEquals(PI, (v as FloatValue).d)
        val w = VarRefExpr("two", mockLoc()).evaluateIn(env)
        assertEquals("PI", (w as StringValue).s)
    }

    @Test
    fun testVarUpdate() {
        val v = VarRefExpr("one", mockLoc()).evaluateIn(env)
        assertEquals(PI, (v as FloatValue).d)
        val a =
            AssignmentExpr(
                "one",
                OperatorExpr(
                    Operator.Times,
                    listOf(VarRefExpr("one", mockLoc()), LiteralExpr(sqrt(2.0), mockLoc())),
                    mockLoc(),
                ),
                mockLoc(),
            )
        val r1 = a.evaluateIn(env)
        assertEquals(4.443, (r1 as FloatValue).d, 0.01)
        val r2 = VarRefExpr("one", mockLoc()).evaluateIn(env)
        assertEquals(4.443, (r2 as FloatValue).d, 0.01)
    }

    @Test
    fun testUpdateInLet() {
        val letExpr = LetExpr("a", Type.IntType, LiteralExpr(31415, mockLoc()), mockLoc())
        val block =
            BlockExpr(
                listOf(
                    letExpr,
                    AssignmentExpr(
                        "a",
                        OperatorExpr(
                            Operator.Times,
                            listOf(VarRefExpr("a", mockLoc()), VarRefExpr("a", mockLoc())),
                            mockLoc(),
                        ),
                        mockLoc(),
                    ),
                    VarRefExpr("a", mockLoc()),
                ),
                mockLoc(),
            )
        val result = block.evaluateIn(env)
        assertEquals(31415 * 31415, (result as IntegerValue).i)
    }
}
