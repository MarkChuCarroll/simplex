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
package org.goodmath.simplex.runtime.values.primitives

import kotlin.collections.all
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.zip
import kotlin.math.min
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexEvaluationError
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist

class ArrayValueType(val elementType: ValueType) : ValueType() {
    override fun twist(): Twist {
        return Twist.obj("ArrayValueType", Twist.value("elementType", elementType))
    }

    override val name: String = "[${elementType.name}]"

    override val asType: Type by lazy {
        Type.array(elementType.asType)
    }

    fun assertIsArray(v: Value): List<Value> {
        if (v is ArrayValue) {
            return v.elements
        } else {
            throw SimplexTypeError("Array", v.valueType.name)
        }
    }
    override val supportsText: Boolean = elementType.supportsText

    override fun toText(v: Value): String {
        val array = assertIs(v).elements
        val rendered =
            array
                .map {
                    if (it.valueType.supportsText) {
                        it.valueType.toText(it)
                    } else {
                        "<<${it.valueType.name}>>"
                    }
                }
                .joinToString(", ")
        return "[$rendered]"
    }

    override fun isTruthy(v: Value): Boolean {
        val a = assertIsArray(v)
        return a.isNotEmpty()
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = emptyList()

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object :
                PrimitiveMethod(
                    "sub",
                    MethodSignature.simple(
                        asType,
                        listOf(Param("subscript", IntegerValueType.asType)),
                        elementType.asType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val arr = assertIsArray(target)
                    val idx = assertIsInt(args[0])
                    return arr[idx]
                }
            },
            object :
                PrimitiveMethod(
                    "length",
                    MethodSignature.simple(asType, emptyList<Param>(), IntegerValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val a = this@ArrayValueType.assertIs(target).elements
                    return IntegerValue(a.size)
                }
            },
            object :
                PrimitiveMethod(
                    "plus",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val a1 = assertIsArray(target)
                    val a2 = assertIsArray(args[0])
                    if (a1.size != a2.size) {
                        throw SimplexEvaluationError("Cannot add arrays of different lengths")
                    }
                    return ArrayValue(
                        this@ArrayValueType.elementType,
                        a1.zip(a2).map { (l, r) ->
                            l.valueType.applyMethod(l, "plus", listOf(r), env)
                        },
                    )
                }
            },
            object :
                PrimitiveMethod(
                    "minus",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val a1 = assertIsArray(target)
                    val a2 = assertIsArray(args[0])
                    if (a1.size != a2.size) {
                        throw SimplexEvaluationError("Cannot add arrays of different lengths")
                    }
                    return ArrayValue(
                        this@ArrayValueType.elementType,
                        a1.zip(a2).map { (l, r) ->
                            l.valueType.applyMethod(l, "minus", listOf(r), env)
                        },
                    )
                }
            },
            object :
                PrimitiveMethod(
                    "eq",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), BooleanValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val a1 = assertIsArray(target)
                    val a2 = assertIsArray(args[0])
                    if (a1.size != a2.size) {
                        return BooleanValue(false)
                    }
                    return BooleanValue(
                        a1.zip(a2).all { (l, r) ->
                            val e = l.valueType.applyMethod(l, "eq", listOf(r), env)
                            e.valueType.isTruthy(e)
                        }
                    )
                }
            },
            object : PrimitiveMethod("neg", MethodSignature.simple(asType, emptyList<Param>(), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val a1 = assertIsArray(target)
                    return ArrayValue(
                        this@ArrayValueType.elementType,
                        a1.map { it.valueType.applyMethod(it, "neg", emptyList(), env) },
                    )
                }
            },
            object :
                PrimitiveMethod(
                    "compare",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), IntegerValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    // Basically doing a lexicographic ordering.
                    val a1 = assertIsArray(target)
                    val a2 = assertIsArray(args[0])
                    val commonLength = min(a1.size, a2.size)
                    for (i in 0..<commonLength) {
                        val c =
                            assertIsInt(
                                a1[i].valueType.applyMethod(target, "compare", listOf(a2[i]), env)
                            )
                        if (c != 0) {
                            return IntegerValue(c)
                        }
                    }
                    // If the elements up to the common length were equal, then the longer
                    // list is greater.
                    return if (a1.size > a2.size) {
                        IntegerValue(1)
                    } else if (a1.size < a2.size) {
                        IntegerValue(-1)
                    } else {
                        IntegerValue(0)
                    }
                }
            },
        )
    }
    override val providesVariables: Map<String, Value> = emptyMap()

    override fun assertIs(v: Value): ArrayValue {
        if (v is ArrayValue) {
            return v
        } else {
            throwTypeError(v)
        }
    }

    companion object {
        val arrayTypes = HashMap<ValueType, ArrayValueType>()

        fun of(t: ValueType): ArrayValueType {
            return arrayTypes.computeIfAbsent(t) { t -> ArrayValueType(t) }
        }
    }
}

class ArrayValue(val elementType: ValueType, val elements: List<Value>) : Value {
    fun isEmpty(): Boolean = elements.isEmpty()

    override val valueType: ValueType = ArrayValueType(elementType)

    override fun twist(): Twist = Twist.obj("ArrayValue", Twist.array("elements", elements))
}
