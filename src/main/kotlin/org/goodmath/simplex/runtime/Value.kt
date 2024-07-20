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
package org.goodmath.simplex.runtime

import org.goodmath.simplex.ast.Definition
import org.goodmath.simplex.ast.Expr
import org.goodmath.simplex.ast.FunctionDefinition
import org.goodmath.simplex.ast.TupleDefinition
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.twist.Twistable
import kotlin.math.min
import kotlin.math.pow


class PrimitiveOperation(
    val name: String,
    val args: List<ValueType<*>>,
    val result: ValueType<*>,
    val execute: (target: Value, args: List<Value>) -> Value)


abstract class ValueType<T: Value> {
    abstract val name: String
    abstract fun isTruthy(v: Value): Boolean
    abstract fun add(v1: Value, v2: Value): Value
    abstract fun subtract(v1: Value, v2: Value): Value
    abstract fun mult(v1: Value, v2: Value): Value
    abstract fun div(v1: Value, v2: Value): Value
    abstract fun mod(v1: Value, v2: Value): Value
    abstract fun pow(v1: Value, v2: Value): Value
    abstract fun equals(v1: Value, v2: Value): Boolean
    abstract fun neg(v1: Value): Value
    open fun subscript(v1: Value, v2: Value): Value {
        throw SimplexTypeError("Type ${v1.valueType.name} does not support subscripting")
    }
    abstract fun compare(v1: Value, v2: Value): Int
    abstract val providesFunctions: List<PrimitiveFunctionValue>
    abstract val providesOperations: List<PrimitiveOperation>
    val operations = providesOperations.associateBy { it.name }

    fun getOperation(name: String): PrimitiveOperation {
        return operations[name] ?: throw SimplexUndefinedError(name, "method")
    }

    fun assertIsString(v: Value): String {
        if (v !is StringValue) {
            throw SimplexTypeError("Expected a string, but found ${v.valueType.name}")
        } else {
            return v.s
        }
    }

    fun assertIsInt(v: Value): Int {
        if (v !is IntegerValue) {
            throw SimplexTypeError("Expected an Int, but found ${v.valueType.name}")
        } else {
            return v.i
        }
    }

    fun assertArity(args: List<Value>, n: Int) {
        if (args.size != n) {
            throw SimplexEvaluationError("Expected $n args,  but received ${args.size}")
        }
    }

    fun assertIsFloat(v: Value): Double {
        if (v !is FloatValue) {
            throw SimplexTypeError("Expected a Float, but found ${v.valueType.name}")
        } else {
            return v.d
        }
    }

    fun assertIsBoolean(v: Value): Boolean {
        if (v !is BooleanValue) {
            throw SimplexTypeError("Expected a Boolean, but found ${v.valueType.name}")
        } else {
            return v.b
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun assertIs(v: Value, t: ValueType<*>): T {
        if (v.valueType != t) {
            throw SimplexTypeError("Expected a ${t.name}, but found ${v.valueType.name}")
        } else {
            return v as T
        }
    }

}

interface Value: Twistable {
    val valueType: ValueType<*>
}

object IntegerValueType: ValueType<IntegerValue>() {
    override val name: String = "Int"


    override fun isTruthy(v: Value): Boolean {
        v as IntegerValue
        return v.i != 0
    }

    override fun add(
        v1: Value,
        v2: Value,
    ): Value {
        v1 as IntegerValue
        return IntegerValue(v1.i + assertIsInt(v2))
    }

    override fun subtract(
        v1: Value,
        v2: Value,
    ): Value {
        v1 as IntegerValue
        return IntegerValue(v1.i - assertIsInt(v2))
    }

    override fun mult(
        v1: Value,
        v2: Value,
    ): Value {
        v1 as IntegerValue
        return IntegerValue(v1.i * assertIsInt(v2))
    }

    override fun div(
        v1: Value,
        v2: Value,
    ): Value {
        v1 as IntegerValue
        return IntegerValue(v1.i / assertIsInt(v2))
    }

    override fun mod(
        v1: Value,
        v2: Value,
    ): Value {
        v1 as IntegerValue
        return IntegerValue(v1.i % assertIsInt(v2))
    }

    override fun pow(
        v1: Value,
        v2: Value,
    ): Value {
        v1 as IntegerValue
        return FloatValue(v1.i.toDouble().pow(assertIsInt(v2)))
    }

    override fun equals(
        v1: Value,
        v2: Value,
    ): Boolean {
        v1 as IntegerValue
        return v1.i == assertIsInt(v2)
    }

    override fun neg(v1: Value): Value {
        v1 as IntegerValue
        return IntegerValue(-v1.i)
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        v1 as IntegerValue
        v2 as IntegerValue
        return v1.i.compareTo(v2.i)
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveOperation> = listOf(
        PrimitiveOperation("toFloat", emptyList(), FloatValueType) { target: Value, args: List<Value> ->
            FloatValue(assertIsInt(target).toDouble())
        }
    )
}

class IntegerValue(val i: Int): Value {
    override fun twist(): Twist =
        Twist.obj("IntegerValue",
            Twist.attr("value", i.toString()))

    override val valueType: ValueType<IntegerValue> = IntegerValueType
}

object FloatValueType: ValueType<FloatValue>() {
    override val name: String = "Float"

    override fun isTruthy(v: Value): Boolean {

        return assertIsFloat(v) != 0.0
    }

    override fun add(
        v1: Value,
        v2: Value,
    ): Value {
        return FloatValue(assertIsFloat(v1) + assertIsFloat(v2))
    }

    override fun subtract(
        v1: Value,
        v2: Value,
    ): Value {
        return FloatValue(assertIsFloat(v1) - assertIsFloat(v2))
    }

    override fun mult(
        v1: Value,
        v2: Value,
    ): Value {
        return FloatValue(assertIsFloat(v1) * assertIsFloat(v2))
    }

    override fun div(
        v1: Value,
        v2: Value,
    ): Value {
        return FloatValue(assertIsFloat(v1) / assertIsFloat(v2))
    }

    override fun mod(
        v1: Value,
        v2: Value,
    ): Value {
        return FloatValue(assertIsFloat(v1) % assertIsFloat(v2))
    }

    override fun pow(
        v1: Value,
        v2: Value,
    ): Value {
        return FloatValue(assertIsFloat(v1).pow(assertIsFloat(v2)))
    }

    override fun equals(
        v1: Value,
        v2: Value,
    ): Boolean {
        return assertIsFloat(v1) == assertIsFloat(v2)
    }

    override fun neg(v1: Value): Value {
        return FloatValue(-assertIsFloat(v1))
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        return assertIsFloat(v1).compareTo(assertIsFloat(v2))
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveOperation> = listOf(
        PrimitiveOperation("isNaN", emptyList(), BooleanValueType) { target: Value, args: List<Value> ->
            BooleanValue(assertIsFloat(target).isNaN())
        }
    )
}

class FloatValue(val d: Double): Value {
    override val valueType: ValueType<FloatValue> = FloatValueType

    override fun twist(): Twist =
        Twist.obj("FloatValue",
            Twist.attr("value", d.toString()))

}

object BooleanValueType: ValueType<BooleanValue>() {
    override val name: String = "Boolean"

    override fun isTruthy(v: Value): Boolean {
        return assertIsBoolean(v)
    }

    override fun add(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "addition")
    }

    override fun subtract(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "subtraction")
    }

    override fun mult(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "multiplication")
    }

    override fun div(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "division")
    }

    override fun mod(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value,
    ): Boolean {
        return assertIsBoolean(v1) == assertIsBoolean(v2)
    }

    override fun neg(v1: Value): Value {
        return BooleanValue(!assertIsBoolean(v1))
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        throw SimplexUnsupportedOperation(name, "ordering")
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveOperation> = emptyList()
}

class BooleanValue(val b: Boolean): Value {
    override val valueType: ValueType<BooleanValue> = BooleanValueType

    override fun twist(): Twist =
        Twist.obj("BooleanValue",
            Twist.attr("value", b.toString())
            )
}

object StringValueType: ValueType<StringValue>() {
    override val name: String = "String"

    override fun isTruthy(v: Value): Boolean {
        return assertIsString(v).isNotEmpty()
    }

    override fun add(
        v1: Value,
        v2: Value,
    ): Value {
        v2 as StringValue
        return StringValue(assertIsString(v1) + v2.s)
    }

    override fun subtract(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "subtraction")
    }

    override fun mult(
        v1: Value,
        v2: Value,
    ): Value {
        v2 as IntegerValue
        return StringValue(assertIsString(v1).repeat(v2.i))
    }

    override fun div(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "division")
    }

    override fun mod(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value,
    ): Boolean {
        v2 as StringValue
        return assertIsString(v1) == v2.s
    }

    override fun neg(v1: Value): Value {
        throw SimplexUnsupportedOperation(name, "negation")
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        return assertIsString(v1).compareTo(assertIsString(v2))
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()
    override val providesOperations: List<PrimitiveOperation> = listOf(
        PrimitiveOperation("length", emptyList(), IntegerValueType) { target, args ->
            IntegerValue(assertIsString(target).length)
        },
        PrimitiveOperation("find", listOf(StringValueType), IntegerValueType) { target, args ->
            val pat = assertIsString(args[0])
            IntegerValue(assertIsString(target).indexOf(pat))
        })
}

class StringValue(val s: String): Value {
    override val valueType: ValueType<StringValue> = StringValueType

    override fun twist(): Twist {
        return Twist.obj("StringValue",
            Twist.attr("value", s))
    }

}

abstract class AbstractFunctionValue: Value {
    abstract fun applyTo(args: List<Value>): Value
}

class FunctionValue(
    val argNames: List<String>,
    val localDefs: List<Definition>,
    val body: List<Expr>,
    val staticScope: Env,
    val def: FunctionDefinition? = null
): AbstractFunctionValue() {
    override val valueType: ValueType<FunctionValue> = FunctionValueType

    override fun applyTo(args: List<Value>): Value {
        val localEnv = Env(localDefs, staticScope)
        if (argNames.size != args.size) {
            throw SimplexEvaluationError("Incorrect number of args: expected ${argNames.size}, but found ${args.size}")
        }
        argNames.zip(args).forEach { (name, value) ->
            localEnv.addVariable(name, value)
        }
        var result: Value = IntegerValue(0)
        for (b in body) {
            result = b.evaluateIn(localEnv)
        }
        return result
    }

    override fun twist(): Twist =
        Twist.obj("FunctionValue",
            Twist.value("def", def),
            Twist.value("scope", staticScope))
}

object FunctionValueType: ValueType<FunctionValue>() {
    override val name: String = "Function"

    fun assertIsFunction(v: Value): FunctionValue {
        if (v is FunctionValue) {
            return v
        } else {
            throw SimplexTypeError("Expected a function value, recieved ${v.valueType.name}")
        }
    }


    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override fun add(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "addition")
    }

    override fun subtract(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "subtraction")
    }

    override fun mult(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "multiplication")
    }

    override fun div(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "division")
    }

    override fun mod(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value,
    ): Boolean {
        val f2 = assertIs(v2, this)
        return v1 == f2
    }

    override fun neg(v1: Value): Value {
        throw SimplexUnsupportedOperation(name, "negation")
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        throw SimplexUnsupportedOperation(name, "ordering")
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveOperation> = emptyList()

}

object TupleValueType: ValueType<TupleValue>() {
    fun assertIsTuple(v: Value): TupleValue {
        if (v !is TupleValue) {
            throw SimplexTypeError("Expected a tuple, not ${v.valueType.name}")
        } else {
            return v
        }
    }

    override val name: String = "Tuple"

    override fun isTruthy(v: Value): Boolean {
        assertIsTuple(v)
        return true
    }

    override fun add(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "addition")
    }

    override fun subtract(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "subtraction")
    }

    override fun mult(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "multiplication")
    }

    override fun div(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "division")
    }

    override fun mod(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value,
    ): Value {
        throw SimplexUnsupportedOperation(name, "exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value,
    ): Boolean {
        val t1 = assertIsTuple(v1)
        val t2 = assertIsTuple(v2)
        if (t1.tupleDef != t2.tupleDef) {
            return false
        }
        return t1.fields.zip(t2.fields).all { (l, r) -> l == r }

    }

    override fun neg(v1: Value): Value {
        throw SimplexUnsupportedOperation(name, "negation")
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        val t1 = assertIsTuple(v1)
        val t2 = assertIsTuple(v2)
        if (t1.tupleDef != t2.tupleDef) {
            throw SimplexTypeError("Cannot compare different tuple types")
        }
        for ((l, r) in t1.fields.zip(t2.fields)) {
            val c = l.valueType.compare(l, r)
            if (c != 0) {
                return c
            }
        }
        return 0
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveOperation> = emptyList()
}

class TupleValue(val tupleDef: TupleDefinition, val fields: List<Value>): Value {
    override val valueType: ValueType<*> = TupleValueType


    override fun twist(): Twist =
        Twist.obj("TupleValue",
            Twist.attr("name", tupleDef.name),
            Twist.array("fields", fields))

}

object ArrayValueType: ValueType<ArrayValue>() {
    override val name: String = "Array"

    fun assertIsArray(v: Value): List<Value> {
        if(v is ArrayValue) {
            return v.elements
        } else {
            throw SimplexTypeError("Expected an array, not a ${v.valueType.name}")
        }
    }

    override fun subscript(v1: Value, v2: Value): Value {
        val arr = assertIsArray(v1)
        val idx = assertIsInt(v2)
        return arr[idx]
    }

    override fun isTruthy(v: Value): Boolean {
        val a = assertIsArray(v)
        return a.isNotEmpty()

    }

    override fun add(
        v1: Value,
        v2: Value,
    ): Value {
        val a1 = assertIsArray(v1)
        val a2 = assertIsArray(v2)
        if (a1.size != a2.size) {
            throw SimplexEvaluationError("Cannot add arrays of different lengths")
        }

        return ArrayValue(a1.zip(a2).map { (l, r) -> l.valueType.add(l, r) })
    }

    override fun subtract(
        v1: Value,
        v2: Value,
    ): Value {
        val a1 = assertIsArray(v1)
        val a2 = assertIsArray(v2)
        if (a1.size != a2.size) {
            throw SimplexEvaluationError("Cannot add arrays of different lengths")
        }

        return ArrayValue(a1.zip(a2).map { (l, r) -> l.valueType.subtract(l, r) })
    }

    override fun mult(
        v1: Value,
        v2: Value,
    ): Value {
        val arr = assertIsArray(v1)
        return ArrayValue(arr.map { it.valueType.mult(it, v2) })
    }

    override fun div(
        v1: Value,
        v2: Value,
    ): Value {
        val arr = assertIsArray(v1)
        return ArrayValue(arr.map { it.valueType.div(it, v2) })
    }

    override fun mod(
        v1: Value,
        v2: Value,
    ): Value {
        val arr = assertIsArray(v1)
        return ArrayValue(arr.map { it.valueType.mod(it, v2) })
    }

    override fun pow(
        v1: Value,
        v2: Value,
    ): Value {
        val arr = assertIsArray(v1)
        return ArrayValue(arr.map { it.valueType.pow(it, v2) })

    }

    override fun equals(
        v1: Value,
        v2: Value,
    ): Boolean {
        val a1 = assertIsArray(v1)
        val a2 = assertIsArray(v2)
        if (a1.size != a2.size) {
            return false
        }
        return a1.zip(a2).all { (l, r) -> l.valueType.equals(l, r)}

    }

    override fun neg(v1: Value): Value {
        val a1 = assertIsArray(v1)
        return ArrayValue(a1.map { it.valueType.neg(it)})
    }

    override fun compare(
        v1: Value,
        v2: Value,
    ): Int {
        // Basically doing a lexicographic ordering.
        val a1 = assertIsArray(v1)
        val a2 = assertIsArray(v2)
        val commonLength = min(a1.size, a2.size)
        for (i in 0..<commonLength) {
            val c = a1[i].valueType.compare(a1[i], a2[i])
            if (c != 0) {
                return c
            }
        }
        // If the elements up to the common length were equal, then the longer
        // list is greater.
        if (a1.size > a2.size) {
            return 1
        } else if (a1.size < a2.size) {
            return -1
        } else {
            return 0
        }
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesOperations: List<PrimitiveOperation> = listOf(
        PrimitiveOperation("length", emptyList(), IntegerValueType) { target: Value, args: List<Value> ->
            val a = assertIsArray(target)
            IntegerValue(a.size)
        })

}

class ArrayValue(val elements: List<Value>): Value {
    fun isEmpty(): Boolean = elements.isEmpty()
    override val valueType: ValueType<ArrayValue> = ArrayValueType


    override fun twist(): Twist =
        Twist.obj("ArrayValue",
            Twist.array("elements", elements))


}

object PrimitiveFunctionValueType: ValueType<PrimitiveFunctionValue>() {
    override val name: String = "PrimitiveFunction"

    fun assertIsPrim(v: Value): PrimitiveFunctionValue {
        if (v !is PrimitiveFunctionValue) {
            throw SimplexTypeError("Expected a primitive function, not ${v.valueType.name}")
        } else {
            return v
        }
    }

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override fun add(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("Primitive functions don't support any operations")
    }

    override fun subtract(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("Primitive functions don't support any operations")
    }

    override fun mult(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("Primitive functions don't support any operations")    }

    override fun div(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("Primitive functions don't support any operations")    }

    override fun mod(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("Primitive functions don't support any operations")    }

    override fun pow(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("Primitive functions don't support any operations")    }

    override fun equals(
        v1: Value,
        v2: Value
    ): Boolean {
        throw SimplexTypeError("Primitive functions don't support any operations")    }

    override fun neg(v1: Value): Value {
        throw SimplexTypeError("Primitive functions don't support any operations")    }

    override fun compare(v1: Value, v2: Value): Int {
        throw SimplexTypeError("Primitive functions don't support any operations")    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()
    override val providesOperations: List<PrimitiveOperation> = emptyList()


}

class PrimitiveFunctionValue(
    val name: String,
    val args: List<ValueType<*>>,
    val result: ValueType<*>,
    val execute: (args: List<Value>) -> Value): AbstractFunctionValue() {
    override val valueType = PrimitiveFunctionValueType
    override fun twist(): Twist =
        Twist.obj("PrimitiveFunctionValue",
            Twist.attr("name", name),
            Twist.attr("resultType", name),
            Twist.array("argTypes", args.map { Twist.attr("type", it.name) }))

    override fun applyTo(args: List<Value>): Value {
        return execute(args)
    }

}


