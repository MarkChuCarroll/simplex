package org.goodmath.simplex.runtime.values.primitives

import org.goodmath.simplex.ast.def.MethodDefinition
import org.goodmath.simplex.ast.expr.Expr
import org.goodmath.simplex.ast.types.MethodType
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.types.TypedName
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist

/** The abstract superclass of both primitive and source methods */
abstract class AbstractMethod(val name: String, val sig: MethodSignature) : Value {
    abstract fun applyTo(target: Value, args: List<Value>, env: Env): Value
}

/**
 * The parent class of built-in method implementations. Each built-in primitive method is
 * implemented as an object that inherits from this class.
 *
 * @param name the name of the method.
 * @param signature the call signature of the method, including its target object type, its
 *   arguments, and its return type.
 */
abstract class PrimitiveMethod(name: String, signature: MethodSignature) :
    AbstractMethod(name, signature) {

    /**
     * The implementation of the primitive method. This is the only method that needs to be
     * implemented for a new primitive method.
     *
     * @param target the self object
     * @param args the argument values.
     * @param env the execution environment.
     */
    abstract fun execute(target: Value, args: List<Value>, env: Env): Value

    override fun twist(): Twist =
        Twist.obj("PrimitiveMethod", Twist.attr("name", name), Twist.attr("sig", sig.toString()))

    override val valueType =
        MethodValueType(Type.multiMethod(sig.self, sig.paramSets.map { paramSet ->
            paramSet.map { it.type  } }, sig.returnType))

    override fun applyTo(target: Value, args: List<Value>, env: Env): Value {
        return execute(target, args, env)
    }
}

/** The value type of methods defined by source code in a model. */
class MethodValue(
    val targetType: Type,
    val returnType: Type,
    val params: List<TypedName>,
    val body: List<Expr>,
    val def: MethodDefinition,
    val staticScope: Env,
) :
    AbstractMethod(
        def.methodName,
        MethodSignature.simple(targetType, params.map { Param(it.name, it.type) }, returnType),
    ) {
    override val valueType: ValueType =
        MethodValueType(Type.multiMethod(targetType, listOf(params.map { it.type }), returnType))

    override fun twist(): Twist =
        Twist.obj(
            "MethodValue",
            Twist.attr("target", targetType.toString()),
            Twist.attr("name", def.methodName),
            Twist.array("params", params),
            Twist.array("body", body),
            Twist.value("def", def),
        )

    override fun applyTo(target: Value, args: List<Value>, env: Env): Value {
        return def.applyTo(target, args, staticScope)
    }
}

/** The value type for all methods - both primitive and source code. */
class MethodValueType(val methodType: MethodType) : ValueType() {
    override val name: String = "Method($methodType)"

    override val asType: Type = methodType

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()
    override val providesPrimitiveMethods: List<PrimitiveMethod> = emptyList()
    override val providesVariables: Map<String, Value> = emptyMap()


    override fun assertIs(v: Value): AbstractMethod {
        if (v is AbstractMethod) {
            return v
        } else {
            throw SimplexTypeError(v.valueType.asType.toString(), this.toString())
        }
    }
}
