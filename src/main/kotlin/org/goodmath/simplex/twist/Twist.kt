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

/** The interface used to declare that a type can be rendered as a twist. */
interface Twistable {
    fun twist(): Twist
}

/**
 * Twists are a simple tree structured data type that's useful for rendering complex values in a
 * readable form, and for being able to easily see the differences between similar values. They're
 * particularly useful when you're debugging, and trying to see subtle differences between values in
 * a test.
 *
 * They're similar to cons lists in Lisp, except that they have a little bit more structure, and
 * they're set up to be more natural for an object-oriented language like Kotlin.
 *
 * A twist is a tree of nodes. Every node has a name, which is most often used as a way of declaring
 * the type of the value defined by the node.
 *
 * Twist nodes come in four types:
 * * Object nodes, which contain a heterogeneous list of children.
 * * Array nodes, which contain a homogeneous list of children.
 * * Attribute nodes, which contain a single string value.
 * * Value nodes, which contain a single twistable value.
 */
abstract class Twist : Twistable {
    abstract fun render(b: StringBuilder, indent: Int)

    abstract fun cons(indent: Int): String?

    /** Render a twist in a compact cons-list like syntax. */
    fun consStr(): String {
        return cons(0) + "\n"
    }

    /**
     * Render a twist in a slightly more verbose syntax, where it's easier to see differences
     * between similar values.
     */
    override fun toString(): String {
        val sb = StringBuilder()
        render(sb, 0)
        return sb.toString()
    }

    companion object {
        /**
         * Create a twist object node.
         *
         * @param name the name of the node.
         * @param children a list of the children of the node.
         */
        fun obj(name: String, vararg children: Twist): Twist {
            return TwistObj(name, children.toList())
        }

        /**
         * Create a twist attribute node.
         *
         * @param name the name of the node.
         * @param value the value of the node. If the value is null, then this node will not be
         *   rendered.
         */
        fun attr(name: String, value: String?): Twist = TwistAttr(name, value)

        /**
         * Create an array node.
         *
         * @param name the name of the node.
         * @param children a list containing (usually) a list of the same type of twistable object.
         */
        fun array(name: String, children: List<Twistable>): Twist =
            TwistArray(name, children.map { it.twist() })

        /**
         * Create a value node.
         *
         * @param name the name of the node.
         * @param value a single twistable child. If this is null, then the node will not be
         *   rendered.
         */
        fun value(name: String, value: Twistable?): Twist = TwistVal(name, value?.twist())
    }

    override fun twist(): Twist = this
}

class TwistObj(val name: String, val children: List<Twist>) : Twist() {
    override fun render(b: StringBuilder, indent: Int) {
        b * indent + "obj $name {\n"
        for (c in children) {
            c.render(b, indent + 1)
        }
        b * indent + "}\n"
    }

    override fun cons(indent: Int): String {
        var result = "   ".repeat(indent)
        result += "(obj $name\n"
        result +=
            children.mapNotNull { c -> c.cons(indent + 1) }.joinToString(("\n")) + ")"
        return result
    }
}

class TwistAttr(val name: String, val v: String?) : Twist() {
    override fun render(b: StringBuilder, indent: Int) {
        if (v != null) {
            b * indent + "attr $name='${v}'\n"
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

class TwistArray(val name: String, val children: List<Twist>) : Twist() {
    override fun render(b: StringBuilder, indent: Int) {
        b * indent + "array $name [\n"
        for (c in children) {
            c.render(b, indent + 1)
        }
        b * indent + "]\n"
    }

    override fun cons(indent: Int): String {
        if (children.isEmpty()) {
            return "   ".repeat(indent) + "[array $name]"
        } else {
            var result = "   ".repeat(indent)
            result += "[array $name\n"
            result += children.mapNotNull { it.cons(indent + 1) }.joinToString("\n")
            result += "]"
            return result
        }
    }
}

class TwistVal(val name: String, val value: Twist?) : Twist() {
    override fun render(b: StringBuilder, indent: Int) {
        if (value != null) {
            b * indent + "val $name=(\n"
            value.render(b, indent + 1)
            b * indent + ")\n"
        }
    }

    override fun cons(indent: Int): String? {
        if (value != null) {
            var result = "   ".repeat(indent)
            result += "(val $name\n"
            result += value.cons(indent + 1)
            return "$result)"
        } else {
            return null
        }
    }
}

operator fun StringBuilder.times(indent: Int): StringBuilder {
    this.append("  ".repeat(indent))
    return this
}

operator fun StringBuilder.plus(s: String): StringBuilder {
    this.append(s)
    return this
}
