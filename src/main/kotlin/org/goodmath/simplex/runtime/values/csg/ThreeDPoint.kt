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
package org.goodmath.simplex.runtime.values.csg

import eu.mihosoft.vvecmath.Transform
import eu.mihosoft.vvecmath.Vector3d
import org.goodmath.simplex.ast.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.PrimitiveMethod

import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.primitives.BooleanValue


object ThreeDPointValueType: ValueType() {
    override val name: String = "ThreeDPoint"
    override val asType: Type = Type.ThreeDPointType


    override val supportsText: Boolean = true

    override fun toText(v: Value): String {
        val point = assertIs(v).xyz
        return "(x=${point.x}, y=${point.y}, z=${point.z})"
    }

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override fun assertIs(v: Value): ThreeDPoint {
        return if (v is ThreeDPoint) {
            v
        } else {
            throwTypeError(v)
        }
    }



    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object: PrimitiveFunctionValue(
                "point",
                FunctionSignature(listOf(
                    Param("x", Type.FloatType),
                    Param("y", Type.FloatType),
                    Param("z", Type.FloatType)
                ), asType
            )) {
                override fun execute(args: List<Value>): Value {
                    val x = assertIsFloat(args[0])
                    val y = assertIsFloat(args[1])
                    val z = assertIsFloat(args[2])
                    return ThreeDPoint(Vector3d.xyz(x, y, z))
                }
            })
    }

    override val providesOperations: List<PrimitiveMethod> by lazy {
        listOf(
            object: PrimitiveMethod("scale",
                MethodSignature(asType,
                    listOf(Param("factor", Type.FloatType)), asType),
                MethodSignature(
                    asType,
                    listOf(Param("xFactor", Type.FloatType),
                        Param("yFactor", Type.FloatType),
                        Param("zFactor", Type.FloatType)),
                    asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val point = assertIs(target).xyz
                    if (args.size == 1) {
                        val factor = assertIsFloat(args[0])
                        return ThreeDPoint(point.transformed(Transform().scale(factor)))
                    } else {
                        val xFactor = assertIsFloat(args[0])
                        val yFactor = assertIsFloat(args[1])
                        val zFactor = assertIsFloat(args[2])
                        return ThreeDPoint(point.transformed(Transform().scale(xFactor, yFactor, zFactor)))
                    }
                }
            },
            object: PrimitiveMethod("rot",
                MethodSignature(
                    asType,
                    listOf(
                        Param("xAngle", Type.FloatType),
                        Param("yAngle", Type.FloatType),
                        Param("zAngle", Type.FloatType)),
                asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val point = assertIs(target).xyz
                    val x = assertIsFloat(args[0])
                    val y = assertIsFloat(args[1])
                    val z = assertIsFloat(args[2])
                    return ThreeDPoint(point.transformed(Transform().rot(x, y, z)))
                }
            },
            object: PrimitiveMethod("plus",
                MethodSignature(asType, listOf(Param("r", asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val p1 = assertIs(target).xyz
                    val p2 = assertIs(args[0]).xyz
                    return ThreeDPoint(p1.plus(p2))
                }
            },
            object: PrimitiveMethod("minus",
                MethodSignature(asType, listOf(Param("r", asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val p1 = assertIs(target).xyz
                    val p2 = assertIs(args[0]).xyz
                    return ThreeDPoint(p1.minus(p2))
                }
            },
            object: PrimitiveMethod("multBy",
                MethodSignature(asType, listOf(Param("r", Type.FloatType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val p1 = assertIs(target).xyz
                    val p2 = assertIsFloat(args[0])
                    return ThreeDPoint(p1.times(p2))
                }
            },
            object: PrimitiveMethod("divBy",
                MethodSignature(asType, listOf(Param("r", Type.FloatType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val p1 = assertIs(target).xyz
                    val p2 = assertIsFloat(args[0])
                    return ThreeDPoint(p1.divided(p2))
                }
            },
            object: PrimitiveMethod("eq",
                MethodSignature(asType, listOf(Param("r", asType)), Type.BooleanType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val p1 = assertIs(target).xyz
                    val p2 = assertIs(args[0]).xyz
                    return BooleanValue(p1 == p2)
                }
            },
            object: PrimitiveMethod("neg",
                MethodSignature(asType, emptyList(), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val p1 = assertIs(target).xyz
                    return ThreeDPoint(p1.negated())
                }
            })
    }
}

class ThreeDPoint(val xyz: Vector3d): Value {
    override val valueType: ValueType = ThreeDPointValueType


    override fun twist(): Twist {
        return Twist.obj(
            "PointValue",
            Twist.attr("x", xyz.x.toString()),
            Twist.attr("y", xyz.y.toString()),
            Twist.attr("z", xyz.z.toString()),
        )
    }
}
