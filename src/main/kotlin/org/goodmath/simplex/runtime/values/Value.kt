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

import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.RootEnv
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUndefinedError
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.primitives.AbstractMethod
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.NoneValue
import org.goodmath.simplex.runtime.values.primitives.NoneValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.runtime.values.primitives.StringValue
import org.goodmath.simplex.runtime.values.primitives.VectorValue
import org.goodmath.simplex.runtime.values.primitives.VectorValueType
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable

/**
 * The abstract supertype of all values.
 *
 * All values in the Simplex runtime can report their own value type,
 * which is used to provide all of their behaviors and validations.
 */
interface Value : Twistable {
    val valueType: ValueType
}

/**
 * The type of a value. Values generally store as little information and behavior as possible. The
 * ability to do things like compare them, render them into text, and so on, are all done by their
 * value type.
 *
 * Value types are _not_ the same thing as static types. They're a runtime encapsulation of
 * behavior - sort of like the runtime value of a class object in some object-oriented languages.
 *
 * A value type provides the implementations of the basic operations for its values. It does this
 * mainly by providing functions, methods, and variables for the object/type.
 */
abstract class ValueType : Twistable {
    abstract val name: String

    /**
     * a utility function to avoid needing to write the same error expression in every value type's
     * assertIs method.
     */
    fun throwTypeError(v: Value): Nothing {
        throw SimplexTypeError(v.valueType.asType.toString() + "_G", this.toString())
    }

    /**
     * Get the static type of values of this value type.
     *
     * Subclass implementors: the implementation of this should be
     * lazy; and if you're adding a new primitive type, you should
     * make sure that you add it to the primitive type list in
     * Type.kt.
     */
    abstract val asType: Type

    /**
     * A flag used for choosing the output format of non-CSG values. If the ValueType says
     * supportsText=true, then a value will be pretty-printed using the toText method; otherwise, it
     * will be written as twist or just its typename.
     */
    open val supportsText: Boolean = false

    /** Convert a value to formatted text. */
    open fun toText(v: Value): String {
        throw SimplexUnsupportedOperation(name, "render_pretty")
    }

    /** Convert the object to a twist. */
    override fun twist(): Twist {
        return Twist.attr("ValueType", name)
    }

    /** Should a value of the type be considered true for conditionals? */
    abstract fun isTruthy(v: Value): Boolean

    /**
     * A list of primitive functions provided by this value type. These will be installed into the
     * root environment of the model.
     */
    abstract val providesFunctions: List<PrimitiveFunctionValue>

    /**
     * A list of builtin methods that can be invoked on values of this value type. These will be
     * installed into this value type, and also into the corresponding static type.
     */
    abstract val providesPrimitiveMethods: List<PrimitiveMethod>

    /**
     * A list of global variables provided by this type, which will
     * be installed into the root environment.
     */
    abstract val providesVariables: Map<String, Value>

    /**
     * A collection of all methods - both the built-in primitives, and any implemented by the user
     * as part of a model.
     */
    val methods: HashMap<String, AbstractMethod> by lazy {
        val result = HashMap<String, AbstractMethod>()
        for (m in providesPrimitiveMethods) {
            asType.registerMethod(m.name, m.sig.toStaticType())
            result[m.name] = m
        }
        result
    }

    /** Get a method for a value of this type, throwing an exception if no method with the name exists. */
    fun getMethod(name: String): AbstractMethod {
        return methods[name] ?: throw SimplexUndefinedError(name, "method of type $asType")
    }

    /** Register a method with the value type of the values that it should operate on. */
    fun addMethod(method: AbstractMethod) {
        methods[method.name] = method
    }

    /**
     * Execute a method on a value.
     *
     * @param target the "self" object receiving the method call.
     * @param args a list of argument values for the call.
     * @param env the environment to use for retrieving variables.
     */
    fun applyMethod(target: Value, name: String, args: List<Value>, env: Env): Value {
        val meth = target.valueType.getMethod(name)
        return meth.applyTo(target, args, env)
    }

    /**
     * Utility method for implementing primitive functions
     * and methods. This checks that the type of a runtime value
     * is a string, and if so, unwraps the string and returns it.
     * If the runtime value is not a string, then this throws an
     * exception.
     */
    fun assertIsString(v: Value): String {
        if (v !is StringValue) {
            throw SimplexTypeError("String_H", v.valueType.name)
        } else {
            return v.s
        }
    }

    /**
     * Utility method for implementing primitive functions
     * and methods. This checks that the type of a runtime value
     * is an integer, and if so, unwraps the int and returns it.
     * If the runtime value is not an int, then this throws an
     * exception.
     */
    fun assertIsInt(v: Value): Int {
        if (v !is IntegerValue) {
            throw SimplexTypeError("Int_I", v.valueType.name)
        } else {
            return v.i
        }
    }

    /**
     * Utility method for implementing primitive functions
     * and methods. This checks that the type of a runtime value
     * is a Double, and if so, unwraps the Double and returns it.
     * If the runtime value is not a Double, then this throws an
     * exception.
     */
    fun assertIsFloat(v: Value): Double {
        return if (v is FloatValue) {
            v.d
        } else if (v is IntegerValue) {
            v.i.toDouble()
        } else {
            System.err.println("Invalid value $v")
            throw SimplexTypeError("Float_FF", v.valueType.name)
        }
    }

    /**
     * Utility method for implementing primitive functions
     * and methods. This checks that the type of a runtime value
     * is a Boolean, and if so, unwraps the Boolean and returns it.
     * If the runtime value is not a Boolean, then this throws an
     * exception.
     */
    fun assertIsBoolean(v: Value): Boolean {
        if (v !is BooleanValue) {
            throw SimplexTypeError("Boolean", v.valueType.name)
        } else {
            return v.b
        }
    }

    /**
     * Utility method for implementing primitive functions
     * and methods. This checks that the type of a runtime value
     * as a value of this value type, and if so, returns it.
     * If the runtime value is not of this type, then this throws an
     * exception.
     *
     * Note to implementors: you should change the return type of
     * this to match the correct ValueType.
     */
    abstract fun assertIs(v: Value): Value

}


/**
 * The "any" type. This should be very rarely used, but it's a type that allows a value of any type
 * to be passed. This is mainly for use in built-in debugging primitives, like "print". Once an
 * object has been passed into something as an "Any", there's virtually nothing you can do with it.
 * There's no downcast from any!
 */
object AnyValueType : ValueType() {
    override val name: String = "Any"
    override val asType: Type by lazy {
        Type.simple(name)
    }


    override fun assertIs(v: Value): Value {
        return v
    }

    override fun isTruthy(v: Value): Boolean {
        return v.valueType.isTruthy(v)
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object: PrimitiveFunctionValue("print",
                FunctionSignature.simple(listOf(
                    Param("args", VectorValueType.of(AnyValueType).asType)),
                    NoneValueType.asType)) {
                override fun execute(args: List<Value>): Value {
                    val v = VectorValueType.of(AnyValueType).assertIsVector(args[0])
                    RootEnv.echo(0,
                        "${
                            v.map {
                                if (it.valueType.supportsText) {
                                    it.valueType.toText(it)
                                } else {
                                    it.toString()
                                }
                            }.joinToString()
                        }\n",
                        false
                    )
                    return NoneValue
                }
            }
        )
    }

    override val providesPrimitiveMethods: List<PrimitiveMethod> = emptyList()

    override val providesVariables: Map<String, Value> = emptyMap()
}
