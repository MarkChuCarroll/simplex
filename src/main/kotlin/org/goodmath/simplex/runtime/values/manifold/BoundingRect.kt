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
package org.goodmath.simplex.runtime.values.manifold

import manifold3d.manifold.Rect
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.runtime.values.primitives.Vec2
import org.goodmath.simplex.runtime.values.primitives.Vec2Type
import org.goodmath.simplex.twist.Twist

/** Simplex wrapper for the manifold Rect type. */
class BoundingRect(val rect: Rect) : Value {
    override val valueType: ValueType = BoundingRectType

    override fun twist(): Twist =
        Twist.obj(
            "BoundingRect",
            Twist.value("center", Vec2.fromDoubleVec2(rect.Center())),
            Twist.value("size", Vec2.fromDoubleVec2(rect.Size())),
        )
}

object BoundingRectType : ValueType() {
    override val name: String = "BoundingRect"

    override val asType: Type = Type.simple(name)

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object :
                PrimitiveFunctionValue(
                    "bounding_rect",
                    FunctionSignature.simple(
                        listOf(Param("low", Type.Vec2Type), Param("high", Type.Vec2Type)),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    val low = Vec2Type.assertIs(args[0]).toDoubleVec2()
                    val high = Vec2Type.assertIs(args[1]).toDoubleVec2()
                    return BoundingRect(Rect(low, high))
                }
            }
        )
    }

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object :
                PrimitiveMethod(
                    "size",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.Vec2Type),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).rect
                    return Vec2.fromDoubleVec2(self.Size())
                }
            },
            object :
                PrimitiveMethod(
                    "scale",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.FloatType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).rect
                    return FloatValue(self.Scale().toDouble())
                }
            },
            object :
                PrimitiveMethod(
                    "center",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.Vec2Type),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).rect
                    return Vec2.fromDoubleVec2(self.Center())
                }
            },
            object :
                PrimitiveMethod(
                    "contains_point",
                    MethodSignature.simple(asType, listOf(Param("pt", Type.Vec2Type)), Type.BooleanType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).rect
                    val pt = Vec2Type.assertIs(args[0]).toDoubleVec2()
                    return BooleanValue(self.Contains(pt))
                }
            },
            object :
                PrimitiveMethod(
                    "contains_rect",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), Type.BooleanType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).rect
                    val other = assertIs(args[0]).rect
                    return BooleanValue(self.Contains(other))
                }
            },
            object :
                PrimitiveMethod(
                    "overlaps",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), Type.BooleanType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).rect
                    val other = assertIs(args[0]).rect
                    return BooleanValue(self.DoesOverlap(other))
                }
            },
            object :
                PrimitiveMethod(
                    "is_empty",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.BooleanType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).rect
                    return BooleanValue(self.IsEmpty())
                }
            },
            object :
                PrimitiveMethod(
                    "is_finite",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.BooleanType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).rect
                    return BooleanValue(self.IsFinite())
                }
            },
            object :
                PrimitiveMethod(
                    "union",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).rect
                    val other = assertIs(args[0]).rect
                    return BoundingRect(self.Union(other))
                }
            },
            object :
                PrimitiveMethod(
                    "plus",
                    MethodSignature.simple(asType, listOf(Param("vec", Type.Vec2Type)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).rect
                    val other = Vec2Type.assertIs(args[0]).toDoubleVec2()
                    return BoundingRect(self.add(other))
                }
            },
            object :
                PrimitiveMethod(
                    "times",
                    MethodSignature.simple(asType, listOf(Param("vec", Type.Vec2Type)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).rect
                    val other = Vec2Type.assertIs(args[0]).toDoubleVec2()
                    return BoundingRect(self.multiply(other))
                }
            },
        )
    }
    override val providesVariables: Map<String, Value> = emptyMap()

    override fun assertIs(v: Value): BoundingRect {
        return if (v is BoundingRect) {
            v
        } else {
            throwTypeError(v)
        }
    }
}
