package org.goodmath.simplex.ast.def

import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.expr.Expr
import org.goodmath.simplex.ast.types.TypedName
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexParameterCountError
import org.goodmath.simplex.runtime.values.MethodValue
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.twist.Twist
import kotlin.collections.map
import kotlin.collections.zip

class MethodDefinition(
    val targetType: Type,
    val methodName: String,
    params: List<TypedName>,
    resultType: Type,
    body: List<Expr>,
    loc: Location
): InvokableDefinition("${targetType}->${methodName}", resultType, params, body, loc) {
    override fun installValues(env: Env) {
        val valueType = env.getType(targetType.toString())
        valueType.addMethod(MethodValue(targetType, returnType, params, body, this, env))

    }

    override fun validate(env: Env) {
        val methodEnv = Env(emptyList(), env)
        methodEnv.declareTypeOf("self", targetType)
        validateParamsAndBody(methodEnv)
    }

    override fun installStatic(env: Env) {
        targetType.registerMethod(name,
            Type.method(targetType, params.map { it.type }, returnType))
    }

    override fun twist(): Twist =
        Twist.obj("MethodDefinition",
            Twist.value("targetType", targetType),
            Twist.attr("name", methodName),
            Twist.array("params", params),
            Twist.value("resultType", returnType),
            Twist.array("body", body))

    fun applyTo(target: Value, args: List<Value>, env: Env): Value {
        val localEnv = Env(emptyList(), env)
        localEnv.addVariable("self", target)
        if (params.size != args.size) {
            throw SimplexParameterCountError(
                "method ${targetType}.${methodName}", listOf(params.size), args.size,
                loc
            )
        }
        params.zip(args).map { (param, arg) ->
            localEnv.addVariable(param.name, arg)
        }
        var result: Value = IntegerValue(0)
        for (expr in body) {
            result = expr.evaluateIn(localEnv)
        }
        return result
    }

}
