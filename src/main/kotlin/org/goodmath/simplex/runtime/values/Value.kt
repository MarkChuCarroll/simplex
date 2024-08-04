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

import org.goodmath.simplex.ast.expr.Expr
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

/**
 * The abstract supertype of all values.
 *
 * All values in the Simplex runtime can report their own value type.
 */
interface Value: Twistable {
    val valueType: ValueType
}

/**
 * The type of a value. Values generally store as little information and behavior
 * as possible. The ability to do things like compare them, render them into text,
 * and so on, are all done by their value type.
 *
 * Value types are _not_ the same thing as static types. They're a runtime
 * encapsulation of behavior - sort of like the runtime value of a class object
 * in some object-oriented languages.
 *
 * A value type provides the implementations of the basic operations for
 * its values. It does this mainly by providing functions and methods
 * for the object.
 */
abstract class ValueType: Twistable {
    abstract val name: String

    /**
     * a utility function to avoid needing to write the same error
     * expression in every value type's assertIs method.
     */
    fun throwTypeError(v: Value): Nothing {
        throw SimplexTypeError(v.valueType.asType.toString(), this.toString())
    }

    /**
     * Get the static type of values of this value type. It's possible
     * that multiple value types will map onto one static type.
     */
    abstract val asType: Type

    /**
     * A flag used for choosing the output format of non-CSG values. If
     * the ValueType says supportsText=true, then a value will be
     * pretty-printed using the toText method; otherwise, it will be
     * written as twist or just its typename.
     */
    open val supportsText: Boolean = false

    /**
     * Convert a value to formatted text.
     */
    open fun toText(v: Value): String {
        throw SimplexUnsupportedOperation(name, "render_pretty")
    }

    /**
     * Convert the object to a twist.
     */
    override fun twist(): Twist {
        return Twist.attr("ValueType", name)
    }

    /**
     * Should a value of the type be considered true for conditionals?
     */
    abstract fun isTruthy(v: Value): Boolean

    /**
     * A list of primitive functions provided by this value type.
     * These will be installed into the root environment of the
     * model.
     */
    abstract val providesFunctions: List<PrimitiveFunctionValue>

    /**
     * A list of builtin methods that can be invoked on values of
     * this value type. These will be installed into this value
     * type, and also into the corresponding static type.
     */
    abstract val providesPrimitiveMethods: List<PrimitiveMethod>

    /**
     * A collection of all methods - both the built-in primitives,
     * and any implemented by the user as part of a model.
     */
    protected val methods: HashMap<String, AbstractMethod> = HashMap()

    /**
     * Get a method for a value, throwing an exception if no method
     * with the name exists.
     */
    fun getMethod(name: String): AbstractMethod {
        return methods[name] ?: throw SimplexUndefinedError(name, "method of type $asType")
    }

    /**
     * Register a method with the value type of the values that
     * it should operate on.
     */
    fun addMethod(method: AbstractMethod) {
        methods[method.name] = method
    }

    val primitiveMethods by lazy {
        providesPrimitiveMethods.associateBy { it.name }
    }

    /**
     * Execute a method on a value.
     * @param target the "self" object receiving the method call.
     * @param args a list of argument values for the call.
     * @param env the environment to use for retrieving variables.
     */
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

/**
 * The abstract superclass of both primitive and source methods
 */
abstract class AbstractMethod(val name: String,
    val sig: MethodSignature): Value {
    abstract fun applyTo(target: Value, args: List<Value>, env: Env): Value
}

/**
 * The parent class of built-in method implementations. Each
 * built-in primitive method is implemented as an object that
 * inherits from this class.
 * @param name the name of the method.
 * @param signature the call signature of the method, including its
 *   target object type, its arguments, and its return type.
 */
abstract class PrimitiveMethod(
    name: String,
    signature: MethodSignature): AbstractMethod(name, signature) {

    /**
     * The implementation of the primitive method. This is the
     * only method that needs to be implemented for a new
     * primitive method.
     * @param target the self object
     * @param args the argument values.
     * @param env the execution environment.
     */
    abstract fun execute(target: Value, args: List<Value>,
                         env: Env): Value

    override fun twist(): Twist =
        Twist.obj("PrimitiveMethod",
            Twist.attr("name", name),
            Twist.attr("sig", sig.toString()))

    override val valueType = MethodValueType(Type.method(sig.self, sig.params.map { it.type }, sig.returnType))

    override fun applyTo(target: Value, args: List<Value>, env: Env): Value {
        return execute(target, args, env)
    }
}

/**
 * The value type of methods defined by source code in a model.
 */
class MethodValue(
    val targetType: Type,
    val returnType: Type,
    val params: List<TypedName>,
    val body: List<Expr>,
    val def: MethodDefinition
): AbstractMethod(def.methodName, MethodSignature(targetType, params.map { Param(it.name, it.type)}, returnType)) {
    override val valueType: ValueType = MethodValueType(Type.method(targetType, params.map { it.type }, returnType))

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

/**
 * The value type for all methods - both primitive and source code.
 */
class MethodValueType(val methodType: MethodType): ValueType() {
    override val name: String = "Method($methodType)"

    override val asType: Type = methodType

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()
    override val providesPrimitiveMethods: List<PrimitiveMethod> = emptyList()
    override fun assertIs(v: Value): AbstractMethod {
        if (v is AbstractMethod) {
            return v
        } else {
            throw SimplexTypeError(v.valueType.asType.toString(), this.toString())
        }
    }
}

/**
 * The "any" type. This should be very rarely used, but it's a
 * type that allows a value of any type to be passed. This is mainly
 * for use in built-in debugging primitives, like "print". Once
 * an object has been passed into something as an "Any", there's
 * virtually nothing you can do with it. There's no downcast from
 * any!
 */
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

    override val providesPrimitiveMethods: List<PrimitiveMethod> = emptyList()

}
