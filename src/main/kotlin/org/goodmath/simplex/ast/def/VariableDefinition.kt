package org.goodmath.simplex.ast.def

import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.expr.Expr
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexError
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.twist.Twist

class VariableDefinition(name: String, val type: Type,
                         val initialValue: Expr, loc: Location
):
        Definition(name, loc) {
    override fun twist(): Twist =
        Twist.obj("VariableDefinition",
            Twist.attr("name", name),
            Twist.value("type", type),
            Twist.value("value", initialValue))

    override fun installStatic(env: Env) {
        env.declareTypeOf(name, type)
    }

    override fun installValues(env: Env) {
        try {
            val v = initialValue.evaluateIn(env)
            env.addVariable(name, v)
        } catch (e: Exception) {
            if (e is SimplexError) {
                if (e.location == null) {
                    e.location = loc
                }
                throw e
            } else {
                throw SimplexEvaluationError(
                    "Evaluation error in variable definition", cause = e,
                    loc = loc
                )
            }
        }

    }

    override fun validate(env: Env) {
        val actualType = initialValue.resultType(env)
        if (!type.matchedBy(actualType)) {
            throw SimplexTypeError(type.toString(), actualType.toString(), location = loc)
        }
    }
}
