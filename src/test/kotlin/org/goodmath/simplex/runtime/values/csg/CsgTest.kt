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
package org.goodmath.simplex.runtime.values.csg

import org.antlr.v4.runtime.CharStreams
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.expr.FunCallExpr
import org.goodmath.simplex.ast.expr.LiteralExpr
import org.goodmath.simplex.ast.expr.VarRefExpr
import org.goodmath.simplex.parser.SimplexParseListener
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.primitives.ArrayValue
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.assertEquals

class CsgTest {
    val csgTestProgram = """
   fun cone(radius: Float, height: Float): Csg {
         cylinder(height + 2.0, radius, 0.1)
   }

   fun compoundShape(size: Float): Csg {
      let cyl: Csg = cylinder(size * 3.0, size, size)
      let brick: Csg = block(size*2.0, size*3.0, size*4.0)->rot(90.0, 45.0, 0.0)
      brick + cyl + cone(size*2.0, size*3.0)->move(0.0, 20.0, 0.0) -
          cone(size, size)
   }

produce("shape") {
  compoundShape(30.0)
}
""".trimIndent()

    var locIdx = 0
    fun mockLoc(): Location {
        locIdx++
        return Location("CsgTest", locIdx, 0)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testCsgProducingProgram() {
        val stream = CharStreams.fromString(csgTestProgram)
        val prog = SimplexParseListener().parse("test", stream) { i, a, b -> System.err.println(a) }
        val root = Env.createRootEnv()
        val env = Env(prog.defs, root)
        env.installStaticDefinitions()
        env.installDefinitionValues()

        val runner =
            FunCallExpr(VarRefExpr("compoundShape", mockLoc()), listOf(LiteralExpr(30.0, mockLoc())), mockLoc())
        val result = runner.evaluateIn(env)
        val bounds = result.valueType.applyMethod(result, "bounds", emptyList(), env)
        val csgStr = (result as CsgValue).csgValue.toStlString()
        val digest = MessageDigest.getInstance("SHA3-256")
        val expected = Path("src/test/resources/csg-out.stl").readText()
        val expectedDigest = digest.digest(expected.toByteArray()).toHexString()
        val actualDigest = digest.digest(csgStr.toByteArray()).toHexString()
        assertEquals(expectedDigest, actualDigest)
        val min = (bounds as ArrayValue).elements[0] as ThreeDPoint
        val max = bounds.elements[1] as ThreeDPoint
        assertEquals(-63.63, min.xyz.x, 0.1)
        assertEquals(-63.63, min.xyz.y, 0.1)
        assertEquals(-45.00, min.xyz.z, 0.1)
        assertEquals(63.63, max.xyz.x, 0.1)
        assertEquals(63.63, max.xyz.y, 0.1)
        assertEquals(92.00, max.xyz.z, 0.1)
    }

}
