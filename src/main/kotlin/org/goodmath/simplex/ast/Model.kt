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
import com.github.ajalt.mordant.rendering.TextColors.*
import org.goodmath.simplex.ast.def.Definition
import org.goodmath.simplex.ast.expr.Expr
import org.goodmath.simplex.runtime.RootEnv
import org.goodmath.simplex.runtime.SimplexError
import org.goodmath.simplex.runtime.SimplexEvaluationError

/**
 * A 3d model for execution in simplex.
 * @param defs the list of all top-level definitions in the model.
 * @param loc the location of the model declaration in the source file.
 */
class Model(val defs: List<Definition>,
            val products: List<Product>,
            loc: Location): AstNode(loc) {
    override fun twist(): Twist =
        Twist.obj("Model",
            Twist.array("defs", defs))

    fun analyze() {
        for (d in defs) {
            RootEnv.addDefinition(d)
        }
        RootEnv.installStaticDefinitions()
        for (d in defs) {
            d.validate(RootEnv)
        }
    }

    fun execute(renderNames: Set<String>?,
                outputPrefix: String,
                echo: (Int, Any?, Boolean) -> Unit) {
        val rootEnv = Env.createRootEnv()

        analyze()
        val executionEnv = Env(defs,
            rootEnv)
        executionEnv.installDefinitionValues()
        val toRender = if (renderNames == null) {
            products
        } else {
            products.filter { renderNames.contains(it.name) }
        }
        for (product in toRender) {
            echo(1, cyan("Rendering ${product.name}"), false)
            product.execute(executionEnv, echo, outputPrefix)
        }
    }
    companion object {
        var output: (Int, Any?, Boolean) -> Unit = { i: Int, s: Any?, err: Boolean ->
            System.out.println(s)
        }
    }

}

/**
 * A product block, which specifies a list of expressions to evaluate,
 * and then render the outputs.
 * @param name the name of the product block
 * @param body a list of the expressions to evaluate and render.
 * @param loc the source location of the render block.
 */
class Product(val name: String?, val body: List<Expr>, loc: Location): AstNode(loc) {
    override fun twist(): Twist =
        Twist.obj("Product",
            Twist.attr("name", name),
            Twist.array("body", body))

    fun execute(env: Env, echo: (Int, Any?, Boolean) -> Unit,
                prefix: String) {

        val results = try {
            body.map { it.evaluateIn(env) }
        } catch (e: Exception) {
            if (e is SimplexError) {
                if (e.location == null) {
                    e.location = loc
                }
                throw e
            } else {
                throw SimplexEvaluationError("Error evaluating model", cause=e, loc=loc)
            }
        }
        val bodies = results.filter { it is CsgValue }.map { it as CsgValue }
            .map { it.csgValue }
        if (bodies.isNotEmpty()) {
            var combined = bodies.first()
            for (body in bodies.drop(0)) {
                combined = combined.union(body)
            }
            echo(1, cyan("Rendering 3d model of ${bodies.size} bodies to $prefix-$name.stl"), false)
            Path("$prefix-$name.stl").writeText(combined.toStlString())
        }
        val others = results.filter { it.valueType != CsgValueType }
        if (others.isNotEmpty()) {
            val text = StringBuilder()
            val twists = StringBuilder()
            for (other in others) {
                if (other.valueType.supportsText) {
                    text + other.valueType.toText(other) + "\n"
                } else {
                    twists + other.twist().consStr() + "\n\n"
                }
            }
            val textOut = text.toString()
            if (textOut.isNotEmpty()) {
                echo(1, cyan("Writing text products to $prefix-$name.txt"), false)
                Path("$prefix-$name.txt").writeText(textOut)
            }
            val twistOut = twists.toString()
            if (twistOut.isNotEmpty()) {
                echo(1, cyan("Writing twisted products to $prefix-$name.twist"), false)
                Path("$prefix-$name.twist").writeText(twistOut)
            }
        }
    }
}
