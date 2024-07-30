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
package org.goodmath.simplex.twist

operator fun StringBuilder.times(indent: Int): StringBuilder {
    this.append("  ".repeat(indent))
    return this
}

operator fun StringBuilder.plus(s: String): StringBuilder {
    this.append(s)
    return this
}

interface Twistable {
    fun twist(): Twist
}

abstract class Twist: Twistable {
    abstract fun render(b: StringBuilder, indent: Int)

    abstract fun cons(indent: Int): String?

    fun consStr(): String {
        return cons(0) + "\n"
    }

    override fun toString(): String {
        val sb = StringBuilder()
        render(sb, 0)
        return sb.toString()
    }

    companion object {
        fun obj(name: String, vararg children: Twist): Twist {
            return TwistObj(name, children.toList())
        }

        fun attr(name: String, value: String?): Twist = TwistAttr(name, value)

        fun array(name: String, children: List<Twistable>): Twist =
            TwistArray(name, children.map { it.twist() })

        fun value(name: String, value: Twistable?): Twist =
            TwistVal(name, value?.twist())
    }

    override fun twist(): Twist = this
}

class TwistObj(val name: String, val children: List<Twist>): Twist() {
    override fun render(b: StringBuilder, indent: Int) {
        b * indent + "=== $name === {\n"
        for (c in children) {
            c.render(b, indent+1)
        }
        b * indent + "}\n"
    }

    override fun cons(indent: Int): String {
        var result = "   ".repeat(indent)
        result +=  "(obj $name\n"
        result += children.map { c -> c.cons(indent+1) }.filterNotNull().joinToString(("\n")) + ")"
        return result
    }
}

class TwistAttr(val name: String, val v: String?): Twist() {
    override fun render(b: StringBuilder, indent: Int) {
        if (v != null) {
            b * indent + "$name='${v}'\n"
        }
    }

    override fun cons(indent: Int): String? {
        if (v != null) {
            var result = "   ".repeat(indent)
            result += "($name '$v')"
            return result
        } else {
            return null
        }
    }
}

class TwistArray(val name: String, val children: List<Twist>): Twist() {
    override fun render(b: StringBuilder, indent: Int) {
        b * indent + "array $name [\n"
        for (c in children) {
            c.render(b, indent+1)
        }
        b * indent + "]\n"
    }

    override fun cons(indent: Int): String {
        if (children.size == 0) {
            return "   ".repeat(indent) + "(array $name)"
        } else {
            var result = "   ".repeat(indent)
            result += "(array $name\n"
            result += children.map { it.cons(indent + 1) }.filterNotNull().joinToString("\n")
            result += ")"
            return result
        }
    }
}

class TwistVal(val name: String, val value: Twist?): Twist() {
    override fun render(b: StringBuilder, indent: Int) {
        if (value != null) {
            b * indent + "$name=(\n"
            value.render(b, indent + 1)
            b * indent + ")\n"
        }
    }

    override fun cons(indent: Int): String? {
        if (value != null) {
            var result = "   ".repeat(indent)
            result += "(val $name\n"
            result += value.cons(indent + 1)
            return result + ")"
        } else {
            return null
        }
    }
}
