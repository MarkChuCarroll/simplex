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

import org.goodmath.simplex.runtime.CsgValue
import org.goodmath.simplex.runtime.CsgValueType
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.plus
import kotlin.io.path.Path
import kotlin.io.path.writeText

class Model(val name: String, val defs: List<Definition>, val renders: List<Render>,
            loc: Location): AstNode(loc) {
    override fun twist(): Twist =
        Twist.obj("Model",
            Twist.attr("name", name),
            Twist.array("defs", defs),
            Twist.array("renders", renders))

    val renderMap = renders.associateBy { it.name }

    fun execute(renderNames: Set<String>?,
                outputPrefix: String) {
        val rootEnv = Env.createRootEnv(this)
        val toRender = renderNames ?: renders.map { it.name }.toSet()
        for (renderName in toRender) {
            val render = renderMap[renderName]!!
            render.evaluateIn(rootEnv, outputPrefix)
        }
    }

}

class Render(val name: String, val body: List<Expr>, loc: Location): AstNode(loc) {
    override fun twist(): Twist =
        Twist.obj("Render",
            Twist.attr("name", name),
            Twist.array("body", body))

    fun evaluateIn(env: Env, prefix: String) {
        val results = body.map { it.evaluateIn(env) }
        val bodies = results.filter { it is CsgValue }.map { it as CsgValue }
            .map { it.csgValue }
        var combined = bodies.first()
        for (body in bodies.drop(0)) {
            combined = combined.union(body)
        }
        Path("$prefix-$name.stl").writeText(combined.toStlString())
        val sb = StringBuilder()
        for (other in results.filter { it.valueType != CsgValueType }) {
            sb + other.toString() + "\n\n"
        }
        Path("$prefix-$name.txt").writeText(sb.toString())
    }

}
