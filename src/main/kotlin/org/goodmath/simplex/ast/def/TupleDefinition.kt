package org.goodmath.simplex.ast.def

import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.types.TypedName
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.values.primitives.TupleValueType
import org.goodmath.simplex.twist.Twist
import kotlin.collections.indexOfFirst

class TupleDefinition(name: String, val fields: List<TypedName>,
                      loc: Location
): Definition(name, loc) {

    val valueType = TupleValueType(this)
    override fun twist(): Twist =
        Twist.obj("TupleDefinition",
            Twist.attr("name", name),
            Twist.array("fields", fields))

    override fun installValues(env: Env) {
        env.registerType(name, valueType)
    }

    override fun validate(env: Env) {
        // Nothing to check here.
    }

    fun indexOf(fieldName: String): Int {
        val idx = fields.indexOfFirst { it.name == fieldName }
        if (idx < 0) {
            throw SimplexUndefinedError(fieldName, "tuple field of $name", loc = loc)
        } else {
            return idx
        }
    }

    override fun installStatic(env: Env) {
        val tupleType = TupleValueType(this)
        env.registerType(name, tupleType)
        Type.simple(name)
    }

}
