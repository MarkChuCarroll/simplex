package org.goodmath.simplex.ast.types

import org.goodmath.simplex.ast.AstNode
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.twist.Twist

/**
 * A name with an optional type declaration, used in several places
 * in the code.
 */
class TypedName(val name: String, val type: Type, loc: Location): AstNode(loc) {
    override fun twist(): Twist =
        Twist.obj("TypedName",
            Twist.attr("name", name),
            Twist.value("type", type))

    override fun toString(): String {
        return "TypedName(${name}:${type})"
    }
}
