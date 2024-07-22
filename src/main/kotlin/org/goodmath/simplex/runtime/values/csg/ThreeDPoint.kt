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

import eu.mihosoft.vvecmath.Vector3d
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.SimplexTypeError
import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist


object ThreeDPointValueType: ValueType<ThreeDPoint>() {
    override val name: String = "ThreeDPoint"

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    fun assertIsPoint(v: Value): Vector3d {
        if (v is ThreeDPoint) {
            return v.xyz
        } else {
            throw SimplexTypeError("Point", v.valueType.name)
        }
    }

    override fun add(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIsPoint(v1)
        val p2 = assertIsPoint(v2)
        return ThreeDPoint(p1.plus(p2))
    }

    override fun subtract(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIsPoint(v1)
        val p2 = assertIsPoint(v2)
        return ThreeDPoint(p1.minus(p2))
    }

    override fun mult(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIsPoint(v1)
        val d = assertIsFloat(v2)
        return ThreeDPoint(p1.times(d))
    }

    override fun div(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIsPoint(v1)
        val d = assertIsFloat(v2)
        return ThreeDPoint(p1.divided(d))
    }

    override fun mod(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation("Point", "modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexTypeError("Point", "exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value
    ): Boolean {
        val p1 = assertIsPoint(v1)
        val p2 = assertIsPoint(v2)
        return p1 == p2
    }

    override fun neg(v1: Value): Value {
        val p1 = assertIsPoint(v1)
        return ThreeDPoint(p1.negated())
    }

    override fun compare(v1: Value, v2: Value): Int {
        throw SimplexTypeError("Point", "ordering")
    }

    override val providesFunctions: List<PrimitiveFunctionValue> = listOf(
        PrimitiveFunctionValue(
            "point", listOf(
                FloatValueType,
                FloatValueType, FloatValueType
            ), ThreeDPointValueType
        ) { args: List<Value> ->
            val x = assertIsFloat(args[0])
            val y = assertIsFloat(args[1])
            val z = assertIsFloat(args[2])
            ThreeDPoint(Vector3d.xyz(x, y, z))
        }
    )

    override val providesOperations: List<PrimitiveMethod> = emptyList()
}

class ThreeDPoint(val xyz: Vector3d): Value {
    override val valueType: ValueType<ThreeDPoint> = ThreeDPointValueType


    override fun twist(): Twist {
        return Twist.obj(
            "PointValue",
            Twist.attr("x", xyz.x.toString()),
            Twist.attr("y", xyz.y.toString()),
            Twist.attr("z", xyz.z.toString()),
        )
    }
}
