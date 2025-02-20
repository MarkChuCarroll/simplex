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

import org.goodmath.simplex.ast.types.FunctionType
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

class VectorValueType(val elementType: ValueType) : ValueType() {
    override fun twist(): Twist {
        return Twist.obj("VectorValueType", Twist.value("elementType", elementType))
    }

    override val name: String = "[${elementType.name}]"

    override val asType: Type by lazy {
        Type.vector(elementType.asType)
    }

    fun assertIsVector(v: Value): List<Value> {
        if (v is VectorValue) {
            return v.elements
        } else {
            throw SimplexTypeError(v.toString(), "Vector", v.valueType.name)
        }
    }
    override val supportsText: Boolean = elementType.supportsText

    override fun toText(v: Value): String {
        val vec = assertIs(v).elements
        val rendered =
            vec.joinToString(", ") {
                if (it.valueType.supportsText) {
                    it.valueType.toText(it)
                } else {
                    "<<${it.valueType.name}>>"
                }
            }
        return "[$rendered]"
    }

    override fun isTruthy(v: Value): Boolean {
        val a = assertIsVector(v)
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
                    val arr = assertIsVector(target)
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
                    val a = this@VectorValueType.assertIs(target).elements
                    return IntegerValue(a.size)
                }
            },
            object :
                PrimitiveMethod(
                    "plus",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val a1 = assertIsVector(target)
                    val a2 = assertIsVector(args[0])
                    if (a1.size != a2.size) {
                        throw SimplexEvaluationError("Cannot add vectors of different lengths")
                    }
                    return VectorValue(
                        this@VectorValueType.elementType,
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
                    val a1 = assertIsVector(target)
                    val a2 = assertIsVector(args[0])
                    if (a1.size != a2.size) {
                        throw SimplexEvaluationError("Cannot subtract vectors of different lengths")
                    }
                    return VectorValue(
                        this@VectorValueType.elementType,
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
                    val a1 = assertIsVector(target)
                    val a2 = assertIsVector(args[0])
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
                    val a1 = assertIsVector(target)
                    return VectorValue(
                        this@VectorValueType.elementType,
                        a1.map { it.valueType.applyMethod(it, "neg", emptyList(), env) },
                    )
                }
            },
            object: PrimitiveMethod("map",
                MethodSignature.simple(asType,
                    listOf(Param("f", Type.function(
                        listOf(
                            listOf(elementType.asType)),
                        elementType.asType))), asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIsVector(target)
                    val function = FunctionValueType(Type.function(
                        listOf(
                            listOf(elementType.asType)),
                        elementType.asType)).assertIs(args[0])
                    val result = ArrayList<Value>()
                    for (v in self) {
                        result.add(function.applyTo(listOf(v)))
                    }
                    return VectorValue(elementType, result)

                }

            },
            object: PrimitiveMethod("append",
                MethodSignature.simple(asType, listOf(Param("other", asType)), asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIsVector(target)
                    val other = assertIsVector(args[0])
                    val result = ArrayList<Value>()
                    result.addAll(self)
                    result.addAll(other)
                    return VectorValue(elementType, result)
                }

            },
            object: PrimitiveMethod("push",
                MethodSignature.simple(asType, listOf(Param("el", elementType.asType)), asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIsVector(target)
                    val el = args[0]
                    val result = ArrayList<Value>()
                    result.addAll(self)
                    result.add(el)
                    return VectorValue(elementType, result)
                }
            },
            object: PrimitiveMethod("insert",
                MethodSignature.simple(asType, listOf(Param("el", elementType.asType)), asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIsVector(target)
                    val el = args[0]
                    val result = ArrayList<Value>()
                    result.addAll(self)
                    result.add(0, el)
                    return VectorValue(elementType, result)
                }
            },
            object: PrimitiveMethod("insert_at",
                MethodSignature.simple(asType, listOf(Param("el", elementType.asType),
                    Param("idx", IntegerValueType.asType)), asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIsVector(target)
                    val el = args[0]
                    val idx = assertIsInt(args[1])
                    val result = ArrayList<Value>()
                    result.addAll(self)
                    result.add(idx, el)
                    return VectorValue(elementType, result)
                }
            },
            object: PrimitiveMethod("filter",
                MethodSignature.simple(asType,
                    listOf(Param("f", Type.function(
                        listOf(
                            listOf(elementType.asType)),
                        BooleanValueType.asType))), asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIsVector(target)
                    val function = FunctionValueType(Type.function(
                        listOf(
                            listOf(elementType.asType)),
                        BooleanValueType.asType)).assertIs(args[0])
                    val result = ArrayList<Value>()
                    for (v in self) {
                        val maybe = assertIsBoolean(function.applyTo(listOf(v)))
                        if (maybe) {
                            result.add(v)
                        }
                    }
                    return VectorValue(elementType, result)
                }
            },
            object: PrimitiveMethod("sort",
                MethodSignature.simple(asType, emptyList(), asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIsVector(target)
                    return VectorValue(elementType, self.sortedWith { l, r ->
                        assertIsInt(l.valueType.applyMethod(l, "compare", listOf(r), env))
                    })
                }
            },
            object :
                PrimitiveMethod(
                    "compare",
                    MethodSignature.simple(asType, listOf(Param("r", asType)), IntegerValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    // Basically doing a lexicographic ordering.
                    val a1 = assertIsVector(target)
                    val a2 = assertIsVector(args[0])
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

    override fun assertIs(v: Value): VectorValue {
        if (v is VectorValue) {
            return v
        } else {
            throwTypeError(v)
        }
    }

    companion object {
        val vectorTypes = HashMap<ValueType, VectorValueType>()

        fun of(t: ValueType): VectorValueType {
            return vectorTypes.computeIfAbsent(t) { t -> VectorValueType(t) }
        }
    }
}

class VectorValue(val elementType: ValueType, val elements: List<Value>) : Value {
    fun isEmpty(): Boolean = elements.isEmpty()

    override val valueType: ValueType = VectorValueType(elementType)

    override fun twist(): Twist = Twist.obj("VectorValue", Twist.array("elements", elements))
}
