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
import eu.mihosoft.jcsg.Cube
import eu.mihosoft.jcsg.Cylinder
import eu.mihosoft.jcsg.Sphere
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
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

    override val providesFunctions: List<PrimitiveFunctionValue> =
        listOf(
            PrimitiveFunctionValue(
                "block",
                listOf(ThreeDPointValueType, ThreeDPointValueType),
                CsgValueType
            ) { args: List<Value> ->
                val center = ThreeDPointValueType.assertIsPoint(args[0])
                val size = ThreeDPointValueType.assertIsPoint(args[1])
                CsgValue(Cube(center, size).toCSG())
            },
            PrimitiveFunctionValue(
                "sphere", listOf(ThreeDPointValueType, FloatValueType),
                CsgValueType
            ) { args: List<Value> ->
                val center = ThreeDPointValueType.assertIsPoint(args[0])
                val radius = assertIsFloat(args[1])
                CsgValue(Sphere(center, radius, 16, 8).toCSG())
            },
            PrimitiveFunctionValue(
                "cylinder", listOf(ThreeDPointValueType, ThreeDPointValueType, FloatValueType, FloatValueType),
                CsgValueType
            ) { args: List<Value> ->
                assertArity(args, 4)
                val start = ThreeDPointValueType.assertIsPoint(args[0])
                val end = ThreeDPointValueType.assertIsPoint(args[1])
                val r1 = assertIsFloat(args[2])
                val r2 = assertIsFloat(args[3])
                CsgValue(Cylinder(start, end, r1, r2, 10).toCSG())
            }
        )


    override val providesOperations: List<PrimitiveMethod> = emptyList()

}

class CsgValue(val csgValue: CSG): Value {
    override val valueType: ValueType<CsgValue> = CsgValueType

    override fun twist(): Twist =
        Twist.obj(
            "CSGValue",
            Twist.attr("csg", csgValue.toObjString())
        )

}





