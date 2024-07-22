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

import org.goodmath.simplex.runtime.values.csg.CsgValue
import org.goodmath.simplex.runtime.values.csg.CsgValueType
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.plus
import kotlin.io.path.Path
import kotlin.io.path.writeText

/**
 * A 3d model for execution in simplex.
 * @param name the name of the model.
 * @param defs the list of all top-level definitions in the model.
 * @param renders a list of the render declarations in the model.
 * @param loc the location of the model declaration in the source file.a
 */
class Model(val name: String, val defs: List<Definition>, val renders: List<Render>,
            loc: Location): AstNode(loc) {
    override fun twist(): Twist =
        Twist.obj("Model",
            Twist.attr("name", name),
            Twist.array("defs", defs),
            Twist.array("renders", renders))

    val renderMap = renders.associateBy { it.name }

    fun execute(renderNames: Set<String>?,
                outputPrefix: String,
                echo: (String, Boolean) -> Unit) {
        val rootEnv = Env.createRootEnv(this)
        val executionEnv = Env(defs, rootEnv)
        executionEnv.installDefinitionValues()
        val toRender = renderNames ?: renders.map { it.name }.toSet()
        for (renderName in toRender) {
            echo("Rendering $renderName", false)
            val render = renderMap[renderName]!!
            render.execute(executionEnv, echo, outputPrefix)
        }
    }
}

/**
 * A render block, which specifies a list of expressions to evaluate,
 * and then render the outputs.
 * @param name the name of the render block
 * @param body a list of the expressions to evaluate and render.
 * @param loc the source location of the render block.
 */
class Render(val name: String, val body: List<Expr>, loc: Location): AstNode(loc) {
    override fun twist(): Twist =
        Twist.obj("Render",
            Twist.attr("name", name),
            Twist.array("body", body))

    fun execute(env: Env, echo: (String, Boolean) -> Unit,
                prefix: String) {
        val results = body.map { it.evaluateIn(env) }
        val bodies = results.filter { it is CsgValue }.map { it as CsgValue }
            .map { it.csgValue }
        if (bodies.isNotEmpty()) {
            var combined = bodies.first()
            for (body in bodies.drop(0)) {
                combined = combined.union(body)
            }
            echo("Rendering 3d model of ${bodies.size} bodies to $prefix-$name.stl", false)
            Path("$prefix-$name.stl").writeText(combined.toStlString())
        }
        val others = results.filter { it.valueType != CsgValueType }
        if (others.isNotEmpty()) {
            val sb = StringBuilder()
            for (other in others) {
                sb + other.twist().toString() + "\n\n"
            }
            echo("Writing ${others.size} other products to $prefix-$name.txt", false)
            Path("$prefix-$name.txt").writeText(sb.toString())
        }
    }

}
