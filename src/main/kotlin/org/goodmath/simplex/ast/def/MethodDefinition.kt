package org.goodmath.simplex.ast.def

import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.expr.Expr
import org.goodmath.simplex.ast.types.TypedName
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexParameterCountError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.values.MethodValue
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.twist.Twist
import kotlin.collections.last
import kotlin.collections.map
import kotlin.collections.zip

class MethodDefinition(
    val targetType: Type,
    val methodName: String,
    val params: List<TypedName>,
    val resultType: Type,
    val body: List<Expr>,
    loc: Location
): Definition("${targetType}->name", loc) {
    override fun installValues(env: Env) {
        val valueType = env.getType(targetType.toString())
        valueType.addMethod(MethodValue(targetType, resultType, params, body, this, env))

    }

    override fun validate(env: Env) {
        val methodEnv = Env(emptyList(), env)
        methodEnv.declareTypeOf("self", targetType)
        for (p in params) {
            methodEnv.declareTypeOf(p.name, p.type)
        }

        for (b in body) {
            b.validate(methodEnv)
        }
        val actualReturnType = body.last().resultType(methodEnv)
        if (!resultType.matchedBy(actualReturnType)) {
            throw SimplexTypeError(
                resultType.toString(), actualReturnType.toString(),
                location = loc
            )
        }
    }

    override fun installStatic(env: Env) {
        targetType.registerMethod(name,
            Type.method(targetType, params.map { it.type }, resultType))
    }

    override fun twist(): Twist =
        Twist.obj("MethodDefinition",
            Twist.value("targetType", targetType),
            Twist.attr("name", methodName),
            Twist.array("params", params),
            Twist.value("resultType", resultType),
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
