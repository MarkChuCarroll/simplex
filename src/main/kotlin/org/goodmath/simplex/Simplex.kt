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
package org.goodmath.simplex

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import org.antlr.v4.runtime.CharStreams
import org.goodmath.simplex.parser.SimplexParseListener
import org.goodmath.simplex.runtime.SimplexError
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.system.exitProcess

class Simplex: CliktCommand(help="Evaluate a Simplex model") {
    val input: String by argument(help="The path to the input file. The pathname must end in .smp3d")
    val prefix: String? by option("--prefix", help="Prefix for all output files")
    val renders: List<String>? by option("--render", help="The names of render blocks to evaluate").split(Regex(","))

    override fun run() {
        if (!input.endsWith(".smp3d")) {
            echo("input must be an smp3d file", err=true)
            exitProcess(1)
        }
        val inputPath = Path(input)
        if (!inputPath.exists()) {
            echo("input file $input doesn't exist", err=true)
        }

        val pre = prefix ?: "${input.toString().dropLast(6)}-out"
        val stream = CharStreams.fromFileName(input)
        val captiveEcho = { s: String, err: Boolean -> echo(s, err=err) }
        try {
            echo("Loading model from $inputPath")
            val result = SimplexParseListener().parse(stream, captiveEcho)
            result.execute(renders?.toSet(), pre, captiveEcho)
        } catch (e: SimplexError) {
            echo(e.toString(), err=true)
        }

    }
}

fun main(args: Array<String>) = Simplex().main(args)
