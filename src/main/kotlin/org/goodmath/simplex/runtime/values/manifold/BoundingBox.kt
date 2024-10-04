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

import manifold3d.pub.Box
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.BooleanValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.runtime.values.primitives.Vec2ValueType
import org.goodmath.simplex.runtime.values.primitives.Vec3
import org.goodmath.simplex.runtime.values.primitives.Vec3ValueType
import org.goodmath.simplex.twist.Twist

/** Simplex wrapper for the manifold BoundingBox type. */
class BoundingBox(val box: Box) : Value {
    override val valueType: ValueType = BoundingBoxValueType

    val size: Vec3 by lazy {
        Vec3.fromDoubleVec3(box.Size())
    }

    val center: Vec3 by lazy {
        Vec3.fromDoubleVec3(box.Center())
    }

    val low: Vec3 by lazy {
        Vec3(center.x - (size.x/2.0), center.y - (size.y/2.0), center.z - (size.z/2.0))
    }

    val high: Vec3 by lazy {
        Vec3(center.x + (size.x/2.0), center.y + (size.y/2.0), center.z + (size.z/2.0))
    }

    fun scale(v: Vec3): BoundingBox {
        return BoundingBox(box.multiply(v.toDoubleVec3()))
    }

    fun scale(x: Double, y: Double, z: Double): BoundingBox {
        return scale(Vec3(x, y, z))
    }

    fun contains(pt: Vec3): BooleanValue {
        return BooleanValue(box.Contains(pt.toDoubleVec3()))
    }

    fun contains(other: BoundingBox): BooleanValue {
        return BooleanValue(box.Contains(other.box))
    }

    fun union(b: BoundingBox): BoundingBox {
        return BoundingBox(box.Union(b.box))
    }

    fun move(v: Vec3): BoundingBox {
        return BoundingBox(box.add(v.toDoubleVec3()))
    }

    fun move(x: Double, y: Double, z: Double): BoundingBox {
        return move(Vec3(x, y, z))
    }

    override fun toString(): String {
        val halfSize: Vec3 = size / 2.0
        val low = center - halfSize
        val high = center + halfSize
        return "[$low, $high]"
    }


    fun expand(point: Vec3): BoundingBox {
        val result = Box(low.toDoubleVec3(), high.toDoubleVec3())
        result.Union(point.toDoubleVec3())
        return BoundingBox(result)
    }

    override fun twist(): Twist {
        return Twist.obj(
            "Box",
            Twist.attr("size", box.Size().toString()),
            Twist.attr("center", box.Center().toString()),
        )
    }
}

object BoundingBoxValueType : ValueType() {
    override val name: String = "BoundingBox"

    override val asType: Type by lazy {
        Type.simple(name)
    }

    override val supportsText = true

    override fun toText(v: Value): String {
        return v.toString()
    }

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object :
                PrimitiveFunctionValue(
                    "Box",
                    FunctionSignature.simple(
                        listOf(Param("low", Vec3ValueType.asType), Param("high", Vec2ValueType.asType)),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    val low = Vec3ValueType.assertIs(args[0]).toDoubleVec3()
                    val high = Vec3ValueType.assertIs(args[1]).toDoubleVec3()
                    return BoundingBox(Box(low, high))
                }
            }
        )
    }

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object :
                PrimitiveMethod(
                    "size",
                    MethodSignature.simple(asType, emptyList<Param>(), Vec3ValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(args[0])
                    return self.size
                }
            },
            object :
                PrimitiveMethod(
                    "center",
                    MethodSignature.simple(asType, emptyList<Param>(), Vec3ValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(args[0])
                    return self.center
                }
            },
            object :
                PrimitiveMethod(
                    "low",
                    MethodSignature.simple(asType, emptyList<Param>(), Vec3ValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(args[0])
                    return self.low
                }
            },
            object :
                PrimitiveMethod(
                    "high",
                    MethodSignature.simple(asType, emptyList<Param>(), Vec3ValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(args[0])
                    return self.high
                }
            },
            object :
                PrimitiveMethod(
                    "contains_point",
                    MethodSignature.simple(asType, listOf(Param("p", Vec3ValueType.asType)), BooleanValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(args[0])
                    val pt = Vec3ValueType.assertIs(args[1])
                    return self.contains(pt)
                }
            },
            object :
                PrimitiveMethod(
                    "contains_box",
                    MethodSignature.simple(asType, listOf(Param("b", asType)), BooleanValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(args[0])
                    val b = assertIs(args[1])
                    return self.contains(b)
                }
            },
            object :
                PrimitiveMethod(
                    "union",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(args[0])
                    val other = assertIs(args[1])
                    return self.union(other)
                }
            },
            object: PrimitiveMethod("expand_to",
                MethodSignature.simple(asType, listOf(Param("point", Vec3ValueType.asType)),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIs(target)
                    val pt = Vec3ValueType.assertIs(args[0])
                    return self.expand(pt)
                }
            },
            object :
                PrimitiveMethod("move",
                    MethodSignature.multi(asType,
                        listOf(
                            listOf(Param("pt", Vec3ValueType.asType)),
                            listOf(Param("x", FloatValueType.asType),
                                Param("y", FloatValueType.asType),
                                        Param("z", FloatValueType.asType))),
                                asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    if (args.size == 1) {
                        val pt = Vec3ValueType.assertIs(args[0])
                        return self.move(pt)
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        val z = assertIsFloat(args[2])
                        return self.move(x, y, z)
                    }
                }
            },
            object :
                PrimitiveMethod(
                    "scale",
                    MethodSignature.multi(asType,
                        listOf(
                            listOf(Param("pt", Vec3ValueType.asType)),
                            listOf(Param("x", FloatValueType.asType),
                                Param("y", FloatValueType.asType),
                                Param("z", FloatValueType.asType))), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return if (args.size == 1) {
                        val pt = Vec3ValueType.assertIs(args[0])
                        self.scale(pt)
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        val z = assertIsFloat(args[2])
                        return self.scale(x, y, z)
                    }
                }
            },
            object :
                PrimitiveMethod(
                    "doesOverlap",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), BooleanValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(args[0]).box
                    val other = assertIs(args[1]).box
                    return BooleanValue(self.DoesOverlap(other))
                }
            },
            object :
                PrimitiveMethod("is_finite", MethodSignature.simple(asType, emptyList<Param>(), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(args[0]).box
                    return BooleanValue(self.IsFinite())
                }
            },
        )
    }
    override val providesVariables: Map<String, Value> = emptyMap()

    override fun assertIs(v: Value): BoundingBox {
        return if (v is BoundingBox) {
            v
        } else {
            throwTypeError(v)
        }
    }
}
