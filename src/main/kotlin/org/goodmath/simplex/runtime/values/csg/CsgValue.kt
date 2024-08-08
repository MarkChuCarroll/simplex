
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

import eu.mihosoft.jcsg.CSG
import eu.mihosoft.vvecmath.Transform
import eu.mihosoft.vvecmath.Vector3d
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.ArrayValue
import org.goodmath.simplex.runtime.values.primitives.BooleanValue
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.twist.Twist


object CsgValueType: ValueType() {
    override val name: String = "CSG"

    override val asType: Type = Type.CsgType

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    fun assertIsCsg(v: Value): CSG {
        if (v is CsgValue) {
            return v.csgValue
        } else {
            throw SimplexTypeError("CSG", v.valueType.name)
        }
    }

    override fun assertIs(v: Value): CsgValue {
        if (v is CsgValue) {
            return v
        } else {
            throwTypeError(v)
        }
    }


    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            CsgSphereFunction,
            CsgBlockFunction,
            CsgCylinderFunction
        )
    }


    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            CsgScaleMethod, CsgScaleThree, CsgMoveMethod, CsgRotateMethod,
            object: PrimitiveMethod("bounds",
                MethodSignature(
                    Type.CsgType,
                    emptyList(),
                    Type.array(Type.FloatType))) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val csg = assertIsCsg(target)
                    val bounds = csg.bounds!!
                    val result = listOf(ThreeDPoint(bounds.min),
                        ThreeDPoint(bounds.max))
                    return ArrayValue(FloatValueType, result)
                }
            },
            object: PrimitiveMethod("centroid",
                MethodSignature(asType,
                emptyList(),
                Type.ThreeDPointType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    target as CsgValue
                    return ThreeDPoint(target.centroid)
                }
            },
            object: PrimitiveMethod("hull",
                MethodSignature(asType, emptyList(), asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val csg = assertIsCsg(target)
                    return CsgValue(csg.hull())
                }
            },
            object: PrimitiveMethod("move_to",
                MethodSignature(asType,
                    listOf(Param("x", Type.FloatType), Param("y", Type.FloatType), Param("z", Type.FloatType)),
                    Type.CsgType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val csg = assertIs(target)
                    val x = assertIsFloat(args[0])
                    val y = assertIsFloat(args[1])
                    val z = assertIsFloat(args[2])
                    val center = csg.centroid
                    // We want to move the centroid to the
                    // location where (centroid + dist) = (x, y, z)
                    // So dist  x, y, z - centroid;
                    // d_x = x - c_x, d_y = y - c_y, d_z = c - d_z
                    val dx = x - center.x
                    val dy = y - center.y
                    val dz = z - center.z
                    return CsgValue(csg.csgValue.transformed(
                        Transform().translate(dx, dy, dz)
                    ))
                }
            },
            object: PrimitiveMethod("plus",
                MethodSignature(asType, listOf(Param("r", asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val c1 = assertIsCsg(target)
                    val c2 = assertIsCsg(args[0])
                    return CsgValue(c1.union(c2))
                }
            },
            object: PrimitiveMethod("minus",
                MethodSignature(asType, listOf(Param("r", asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val c1 = assertIsCsg(target)
                    val c2 = assertIsCsg(args[0])
                    return CsgValue(c1.difference(c2))
                }
            },
            object: PrimitiveMethod("div",
                MethodSignature(asType, listOf(Param("r", asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val c1 = assertIsCsg(target)
                    val c2 = assertIsCsg(args[0])
                    return CsgValue(c1.intersect(c2))
                }
            },
            object: PrimitiveMethod("eq",
                MethodSignature(asType, listOf(Param("r", asType)), Type.BooleanType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val c1 = assertIsCsg(target)
                    val c2 = assertIsCsg(args[0])
                    return BooleanValue(c1 == c2)
                }
            })

    }


}

class CsgValue(val csgValue: CSG): Value {
    override val valueType: ValueType = CsgValueType

    val centroid: Vector3d by lazy {
        val faceCenters = csgValue.polygons.map { it.centroid() }
        var xSum = 0.0
        var ySum = 0.0
        var zSum = 0.0
        for (p in faceCenters) {
            xSum += p.x
            ySum += p.y
            zSum += p.z
        }
        Vector3d.xyz(xSum/faceCenters.size.toDouble(), ySum/faceCenters.size.toDouble(),
            zSum/faceCenters.size.toDouble())
    }

    override fun twist(): Twist =
        Twist.obj(
            "CSGValue",
            Twist.attr("csg", csgValue.toObjString())
        )

}





