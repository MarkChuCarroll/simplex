package org.goodmath.simplex.runtime.values.csg

import eu.mihosoft.jcsg.Cube
import eu.mihosoft.jcsg.Sphere
import eu.mihosoft.vvecmath.Transform
import eu.mihosoft.vvecmath.Vector3d
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue

object CsgBlockFunction: PrimitiveFunctionValue("block",
    FunctionSignature(
        listOf(FloatValueType, FloatValueType, FloatValueType),
        CsgValueType)) {
    override fun execute(args: List<Value>): Value {
        val width = CsgValueType.assertIsFloat(args[0])
        val height = CsgValueType.assertIsFloat(args[1])
        val depth = CsgValueType.assertIsFloat(args[2])

        val lowerLeft = Vector3d.xyz(-width/2.0, -height/2.0, -depth/2.0)
        val upperRight = Vector3d.xyz(width/2.0, height/2.0, depth/2.0)
        return CsgValue(Cube(lowerLeft, upperRight).toCSG())
    }
}

object CsgSphereFunction: PrimitiveFunctionValue(
    "sphere",
    FunctionSignature(
        listOf(FloatValueType),
        CsgValueType)) {
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
    listOf(FloatValueType,
        FloatValueType,
        FloatValueType
    ),
    CsgValueType)) {
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

object CsgScaleMethod: PrimitiveMethod<CsgValue>(
    "scale",
    MethodSignature(CsgValueType, listOf(FloatValueType), CsgValueType)) {
    override fun execute(
        target: Value,
        args: List<Value>
    ): Value {
        val csg = CsgValueType.assertIsCsg(target)
        if (args.size == 1) {
            val factor = CsgValueType.assertIsFloat(args[0])
            return CsgValue(csg.transformed(Transform().scale(factor)))
        } else { // args.size == 3
            val xFactor = CsgValueType.assertIsFloat(args[0])
            val yFactor = CsgValueType.assertIsFloat(args[1])
            val zFactor = CsgValueType.assertIsFloat(args[2])
            return CsgValue(csg.transformed(Transform().scale(xFactor, yFactor, zFactor)))
        }
    }
}

object CsgMoveMethod: PrimitiveMethod<CsgValue>("move",
    MethodSignature<CsgValue>(
        CsgValueType,
        listOf(FloatValueType, FloatValueType, FloatValueType),
        CsgValueType)) {
    override fun execute(
        target: Value,
        args: List<Value>
    ): Value {
        val x = CsgValueType.assertIsFloat(args[0])
        val y = CsgValueType.assertIsFloat(args[1])
        val z = CsgValueType.assertIsFloat(args[2])
        val csg = CsgValueType.assertIsCsg(target)
        return CsgValue(csg.transformed(Transform().translate(x, y, z)))
    }
}

object CsgRotateMethod: PrimitiveMethod<CsgValue>("rot",
    MethodSignature<CsgValue>(CsgValueType, listOf(FloatValueType,
    FloatValueType, FloatValueType), CsgValueType)) {
    override fun execute(
        target: Value,
        args: List<Value>
    ): Value {
        val x = CsgValueType.assertIsFloat(args[0])
        val y = CsgValueType.assertIsFloat(args[1])
        val z = CsgValueType.assertIsFloat(args[2])
        val csg = CsgValueType.assertIsCsg(target)
        return CsgValue(csg.transformed(Transform().rot(x, y, z)))
    }
}
