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

import eu.mihosoft.jcsg.Cube
import eu.mihosoft.jcsg.Sphere
import eu.mihosoft.vvecmath.Transform
import eu.mihosoft.vvecmath.Vector3d
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue

object CsgBlockFunction: PrimitiveFunctionValue("block",
    FunctionSignature(
        listOf(Param("x", Type.FloatType), Param("y", Type.FloatType),
            Param("z", Type.FloatType)),
        Type.CsgType)) {
    override fun execute(args: List<Value>): Value {
        val width = CsgValueType.assertIsFloat(args[0])
        val height = CsgValueType.assertIsFloat(args[1])
        val depth = CsgValueType.assertIsFloat(args[2])

        val lowerLeft = Vector3d.xyz(0.0, 0.0, 0.0)
        val upperRight = Vector3d.xyz(width, height, depth)
        return CsgValue(Cube(lowerLeft, upperRight)
            .toCSG())
    }
}

object CsgSphereFunction: PrimitiveFunctionValue(
    "sphere",
    FunctionSignature(
        listOf(Param("radius", Type.FloatType)),
        Type.CsgType)) {
    override fun execute(args: List<Value>): Value {
        val radius = CsgValueType.assertIsFloat(args[0])
        val (slices, stacks) = if (args.size == 3) {
            Pair(
                CsgValueType.assertIsInt(args[1]),
                CsgValueType.assertIsInt(args[2])
            )
        } else {
            Pair(16, 8)
        }
        return CsgValue(Sphere(Vector3d.xyz(0.0, 0.0, 0.0), radius, slices, stacks).toCSG())
    }
}

object CsgCylinderFunction: PrimitiveFunctionValue(
    "cylinder",
    FunctionSignature(
    listOf(Param("height", Type.FloatType),
        Param("lowerRadius", Type.FloatType),
        Param("upperRadius", Type.FloatType)
    ),
    Type.CsgType)) {
    override fun execute(args: List<Value>): Value {
        val height = CsgValueType.assertIsFloat(args[0])
        val lowerDiam = CsgValueType.assertIsFloat(args[1])
        val upperDiam = CsgValueType.assertIsFloat(args[2])
        return CsgValue(extrudeProfile(
            listOf(
                ExtrusionProfile(listOf(ProfileSlice(0.0, lowerDiam, lowerDiam),
                    ProfileSlice(height, upperDiam, upperDiam)))),
            ::circleCrossSection)
        )
    }
}

object CsgScaleMethod: PrimitiveMethod(
    "scale",
    MethodSignature(Type.CsgType, listOf(Param("factor", Type.FloatType)), Type.CsgType)) {
    override fun execute(
        target: Value,
        args: List<Value>,
        env: Env
    ): Value {
        val csg = CsgValueType.assertIsCsg(target)
        val factor = CsgValueType.assertIsFloat(args[0])
        return CsgValue(csg.transformed(Transform().scale(factor)))
    }
}
object CsgScaleThree: PrimitiveMethod(
    "scale3",
    MethodSignature(Type.CsgType, listOf(Param("x", Type.FloatType),
        Param("y", Type.FloatType), Param("z", Type.FloatType)), Type.CsgType)) {
    override fun execute(target: Value, args: List<Value>, env: Env): Value {
        val csg = CsgValueType.assertIsCsg(target)
        val xFactor = CsgValueType.assertIsFloat(args[0])
        val yFactor = CsgValueType.assertIsFloat(args[1])
        val zFactor = CsgValueType.assertIsFloat(args[2])
        return CsgValue(csg.transformed(Transform().scale(xFactor, yFactor, zFactor)))
    }
}

object CsgMoveMethod: PrimitiveMethod("move",
    MethodSignature(
        Type.CsgType,
        listOf(Param("xFactor", Type.FloatType),
            Param("yFactor", Type.FloatType),
            Param("zFactor", Type.FloatType)),
        Type.CsgType)) {
    override fun execute(
        target: Value,
        args: List<Value>,
        env: Env
    ): Value {
        val x = CsgValueType.assertIsFloat(args[0])
        val y = CsgValueType.assertIsFloat(args[1])
        val z = CsgValueType.assertIsFloat(args[2])
        val csg = CsgValueType.assertIsCsg(target)
        return CsgValue(csg.transformed(Transform().translate(x, y, z)))
    }
}

object CsgRotateMethod: PrimitiveMethod("rot",
    MethodSignature(
        Type.CsgType,
        listOf(Param("xAngle", Type.FloatType),
            Param("yAngle", Type.FloatType),
            Param("zAngle", Type.FloatType)), Type.CsgType)) {
    override fun execute(
        target: Value,
        args: List<Value>,
        env: Env
    ): Value {
        val x = CsgValueType.assertIsFloat(args[0])
        val y = CsgValueType.assertIsFloat(args[1])
        val z = CsgValueType.assertIsFloat(args[2])
        val csg = CsgValueType.assertIsCsg(target)
        return CsgValue(csg.transformed(Transform().rot(x, y, z)))
    }
}
