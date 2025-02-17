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

import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.assertEquals
import org.antlr.v4.runtime.CharStreams
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.expr.FunCallExpr
import org.goodmath.simplex.ast.expr.LiteralExpr
import org.goodmath.simplex.ast.expr.VarRefExpr
import org.goodmath.simplex.parser.SimplexParseListener
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.manifold.SMaterial
import org.goodmath.simplex.runtime.values.manifold.Solid
import org.junit.jupiter.api.Test

class SolidTest {
    val solidTestProgram =
        """
   fun cone(radius: Float, height: Float): Solid {
         cylinder(height + 2.0, radius, 0.1)
   }

   fun compoundShape(size: Float): Solid {
      let cyl: Solid = cylinder(size * 3.0, size, size)
      let br: Solid = cuboid(size*2.0, size*3.0, size*4.0)->rotate(90.0, 45.0, 0.0)
      br + cyl + cone(size*2.0, size*3.0)->move(0.0, 20.0, 0.0) -
          cone(size, size)->move(30.0, 20.0, 10.0)->rotate(90.0, 90.0, 45.0)
   }

produce("shape") {
  compoundShape(30.0)
}
"""
            .trimIndent()

    var locIdx = 0

    fun mockLoc(): Location {
        locIdx++
        return Location("CsgTest", locIdx, 0)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testCsgProducingProgram() {
        val stream = CharStreams.fromString(solidTestProgram)
        val prog = SimplexParseListener().parse("test", stream) { i, a, b -> System.err.println(a) }
        val root = Env.createRootEnv()
        val env = Env(prog.defs, root)
        env.installStaticDefinitions()
        env.installDefinitionValues()

        val runner =
            FunCallExpr(
                VarRefExpr("compoundShape", mockLoc()),
                listOf(LiteralExpr(30.0, mockLoc())),
                mockLoc(),
            )
        val result = runner.evaluateIn(env) as Solid
        val bounds = result.boundingBox()
        result.export("/tmp/mtest.stl",  SMaterial.roughAqua)

        val digest = MessageDigest.getInstance("SHA3-256")
        val expected = Path("src/test/resources/csg-out.stl").readText()
        val expectedDigest = digest.digest(expected.toByteArray()).toHexString()

        val stlStr = Path("/tmp/mtest.stl").readText()
        val actualDigest = digest.digest(stlStr.toByteArray()).toHexString()
        assertEquals(expectedDigest, actualDigest)
        val min = bounds.low
        val max = bounds.high
        assertEquals(-60.0, min.x,  0.1)
        assertEquals(-60.0, min.y, 0.1)
        assertEquals(-92.0, min.z, 0.1)
        assertEquals(60.0, max.x, 0.1)
        assertEquals(80.0, max.y, 0.1)
        assertEquals(53.00, max.z, 0.1)
    }
}
