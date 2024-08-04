package org.goodmath.simplex.ast.def

import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.expr.Expr
import org.goodmath.simplex.ast.types.TypedName
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.values.primitives.FunctionValue
import org.goodmath.simplex.twist.Twist
import kotlin.collections.last
import kotlin.collections.map

/**
 * A function definition.
 * @param name
 * @param params a list of the function's parameters, with optional types.
 * @param localDefs a list of local definitions declared within the function.
 * @param body the function body.
 * @param loc the source location.
 */
class FunctionDefinition(name: String,
                         val returnType: Type,
                         val params: List<TypedName>,
                         val localDefs: List<Definition>,
                         val body: List<Expr>,
                         loc: Location
): Definition(name, loc) {

    val type = Type.function(params.map { it.type }, returnType)

    override fun twist(): Twist =
        Twist.obj(
            "FunctionDefinition",
            Twist.attr("name", name),
            Twist.array("params", params),
            Twist.array("localDefs", localDefs),
            Twist.array("body", body)
        )

    override fun installStatic(env: Env) {
        env.declareTypeOf(name, type)
    }

    override fun installValues(env: Env) {
        val funValue = FunctionValue(
            returnType,
            params,
            localDefs,
            body,
            env,
            this
        )
        env.addVariable(name, funValue)
    }

    override fun validate(env: Env) {
        val functionEnv = Env(localDefs, env)
        functionEnv.installStaticDefinitions()
        for (p in params) {
            functionEnv.declareTypeOf(p.name, p.type)
        }
        for (b in body) {
            b.validate(functionEnv)
        }
        val actualReturnType = body.last().resultType(functionEnv)
        if (!returnType.matchedBy(actualReturnType)) {
            throw SimplexTypeError(
                returnType.toString(), actualReturnType.toString(),
                location = loc
            )
        }
    }
}
