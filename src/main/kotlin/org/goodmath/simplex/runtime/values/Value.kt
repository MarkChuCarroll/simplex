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
package org.goodmath.simplex.runtime.values

import org.goodmath.simplex.ast.Expr
import org.goodmath.simplex.ast.MethodDefinition
import org.goodmath.simplex.ast.MethodType
import org.goodmath.simplex.ast.Type
import org.goodmath.simplex.ast.TypedName
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.StringValue
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable
import kotlin.collections.associateBy
import kotlin.math.sign

/**
 * The abstract supertype of all values.
 * The only thing that all values need to be able to do
 * is report their value type.
 */
interface Value: Twistable {
    val valueType: ValueType
}

/**
 * A value type. This provides implementations of
 * the basic arithmetic and comparison operations
 * for the type's values.
 */
abstract class ValueType: Twistable {
    abstract val name: String

    fun throwTypeError(v: Value): Nothing {
        throw SimplexTypeError(v.valueType.asType.toString(), this.toString())
    }

    abstract val asType: Type

    open val supportsText: Boolean = false

    open fun toText(v: Value): String {
        throw SimplexUnsupportedOperation(name, "render_pretty")
    }

    override fun twist(): Twist {
        return Twist.attr("ValueType", name)
    }

    /**
     * Should a value of the type be considered true for
     * conditionals?
     */
    abstract fun isTruthy(v: Value): Boolean


    abstract val providesFunctions: List<PrimitiveFunctionValue>
    abstract val providesOperations: List<PrimitiveMethod>

    protected val methods: HashMap<String, AbstractMethod> = HashMap()

    fun hasMethod(name: String): Boolean {
        return methods.containsKey(name)
    }

    fun getMethod(name: String): AbstractMethod {
        return methods[name] ?: throw SimplexUndefinedError(name, "method")
    }

    fun addMethod(method: AbstractMethod) {
        methods[method.name] = method
    }

    val primitiveMethods by lazy {
        providesOperations.associateBy { it.name }
    }

    fun hasPrimitiveMethod(name: String): Boolean {
        return primitiveMethods.containsKey(name)
    }

    fun getPrimitiveMethod(name: String): PrimitiveMethod {
        return primitiveMethods[name] ?: throw SimplexUndefinedError(name, "method")
    }

    fun applyMethod(target: Value, name: String,  args: List<Value>, env: Env): Value {
        val meth = target.valueType.getMethod(name)
        return meth.applyTo(target, args, env)
    }


    fun assertIsString(v: Value): String {
        if (v !is StringValue) {
            throw SimplexTypeError("String", v.valueType.name)
        } else {
            return v.s
        }
    }

    fun assertIsInt(v: Value): Int {
        if (v !is IntegerValue) {
            throw SimplexTypeError("Int", v.valueType.name)
        } else {
            return v.i
        }
    }

    fun assertIsFloat(v: Value): Double {
        if (v !is FloatValue) {
            throw SimplexTypeError("Float", v.valueType.name)
        } else {
            return v.d
        }
    }

    fun assertIsBoolean(v: Value): Boolean {
        if (v !is BooleanValue) {
            throw SimplexTypeError("Boolean", v.valueType.name)
        } else {
            return v.b
        }
    }

    abstract fun assertIs(v: Value):Value
}

data class FunctionSignature(
    val params: List<Param>,
    val returnType: Type
): Twistable {
    override fun twist(): Twist =
        Twist.obj("FunctionSignature",
            Twist.array("params", params),
            Twist.value("resultType", returnType)
        )

}

data class Param(val name: String, val type: Type): Twistable {
    override fun twist(): Twist =
        Twist.obj("Param",
            Twist.attr("name", name),
            Twist.attr("type", type.toString()))
}

data class MethodSignature(
    val self: Type,
    val params: List<Param>,
    val returnType: Type,
): Twistable {
    override fun twist(): Twist =
        Twist.obj("MethodSig",
            Twist.value("selfType", self),
            Twist.array("params", params),
            Twist.value("returnType", returnType))

}

abstract class AbstractMethod(val name: String,
    val sig: MethodSignature): Value {
    abstract fun applyTo(target: Value, args: List<Value>, env: Env): Value
}

abstract class PrimitiveMethod(
    name: String,
    vararg val signatures: MethodSignature): AbstractMethod(name, signatures[0]) {
    abstract fun execute(target: Value, args: List<Value>,
                         env: Env): Value

    override fun twist(): Twist =
        Twist.obj("PrimitiveMethod",
            Twist.attr("name", name),
            Twist.attr("sig", sig.toString()))

    override val valueType = MethodValueType(Type.method(sig.self, sig.params.map { it.type }, sig.returnType) as MethodType)

    override fun applyTo(target: Value, args: List<Value>, env: Env): Value {
        return execute(target, args, env)
    }
}


class MethodValue(
    val targetType: Type,
    val returnType: Type,
    val params: List<TypedName>,
    val body: List<Expr>,
    val def: MethodDefinition
): AbstractMethod(def.methodName, MethodSignature(targetType, params.map { Param(it.name, it.type)}, returnType)) {
    override val valueType: ValueType = MethodValueType(Type.method(targetType, params.map { it.type }, returnType) as MethodType)

    override fun twist(): Twist =
        Twist.obj("MethodValue",
            Twist.attr("target", targetType.toString()),
            Twist.attr("name", def.methodName),
            Twist.array("params", params),
            Twist.array("body", body),
            Twist.value("def", def))

    override fun applyTo(target: Value, args: List<Value>, env: Env): Value {
        return def.applyTo(target, args, env)
    }

}

class MethodValueType(val methodType: MethodType): ValueType() {
    override val name: String = "Method($methodType)"

    override val asType: Type = methodType

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()
    override val providesOperations: List<PrimitiveMethod> = emptyList()
    override fun assertIs(v: Value): AbstractMethod {
        if (v is AbstractMethod) {
            return v
        } else {
            throw SimplexTypeError(v.valueType.asType.toString(), this.toString())
        }
    }
}


object AnyType: ValueType() {
    override val name: String = "Any"
    override val asType: Type = Type.simple("Any")

    override fun assertIs(v: Value): Value {
        return v
    }

    override fun isTruthy(v: Value): Boolean {
        return v.valueType.isTruthy(v)
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveMethod> = emptyList()

}
