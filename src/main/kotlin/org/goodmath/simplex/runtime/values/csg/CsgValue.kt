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
import eu.mihosoft.vvecmath.Vector3d
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist


object CsgValueType: ValueType<CsgValue>() {
    override val name: String = "CSG"

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

    override fun add(
        v1: Value,
        v2: Value
    ): Value {
        val c1 = assertIsCsg(v1)
        val c2 = assertIsCsg(v2)
        return CsgValue(c1.union(c2))
    }

    override fun subtract(
        v1: Value,
        v2: Value
    ): Value {
        val c1 = assertIsCsg(v1)
        val c2 = assertIsCsg(v2)
        return CsgValue(c1.difference(c2))
    }

    override fun mult(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("CSG", "multiplication")
    }

    override fun div(
        v1: Value,
        v2: Value
    ): Value {
        val c1 = assertIsCsg(v1)
        val c2 = assertIsCsg(v2)
        return CsgValue(c1.intersect(c2))
    }

    override fun mod(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("CSG", "modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("CSG", "exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value
    ): Boolean {
        val c1 = assertIsCsg(v1)
        val c2 = assertIsCsg(v2)
        return c1 == c2
    }

    override fun neg(v1: Value): Value {
        throw SimplexTypeError("CSG", "negation")
    }

    override fun compare(v1: Value, v2: Value): Int {
        throw SimplexTypeError("CSG", "ordering")
    }


    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            CsgSphereFunction,
            CsgBlockFunction,
            CsgCylinderFunction
        )
    }


    override val providesOperations: List<PrimitiveMethod<CsgValue>> by lazy {
        listOf(
            CsgScaleMethod, CsgMoveMethod, CsgRotateMethod,
            object: PrimitiveMethod<CsgValue>("centroid",
                MethodSignature(CsgValueType,
                emptyList(),
                ThreeDPointValueType)) {

                override fun execute(
                    target: Value,
                    args: List<Value>
                ): Value {
                    target as CsgValue
                    return ThreeDPoint(target.centroid)
                }
            }
        )
    }
}

class CsgValue(val csgValue: CSG): Value {
    override val valueType: ValueType<CsgValue> = CsgValueType

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




