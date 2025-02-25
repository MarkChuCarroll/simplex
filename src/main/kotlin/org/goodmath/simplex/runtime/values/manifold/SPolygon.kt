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

import manifold3d.manifold.CrossSection
import manifold3d.pub.Polygons
import manifold3d.pub.SimplePolygon
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.VectorValue
import org.goodmath.simplex.runtime.values.primitives.VectorValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.runtime.values.primitives.Vec2
import org.goodmath.simplex.runtime.values.primitives.Vec2ValueType
import org.goodmath.simplex.twist.Twist

class SPolygon(val poly: SimplePolygon) : Value {
    override val valueType: ValueType = SPolygonType

    override fun twist(): Twist =
        Twist.obj(
            "Polygon",
            Twist.attr("position", poly.position().toString()),
            Twist.attr("size", poly.size().toString()),
        )
}

object SPolygonType : ValueType() {
    override val name: String = "Polygon"

    override val asType: Type by lazy {
        Type.simple("Polygon")
    }

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object :
                PrimitiveFunctionValue(
                    "polygon",
                    FunctionSignature.simple(listOf(Param("points", Type.vector(Vec2ValueType.asType))), asType),
                ) {
                override fun execute(args: List<Value>): Value {
                    @Suppress("UNCHECKED_CAST")
                    val points = VectorValueType.of(Vec2ValueType).assertIs(args[0]).elements as List<Vec2>
                    val floatArray = DoubleArray(points.size * 2)
                    for (idx in points.indices) {
                        floatArray[2*idx] = points[idx].x
                        floatArray[2*idx + 1] = points[idx].y
                    }
                    val result = SPolygon(SimplePolygon.FromArray(floatArray))
                    return result
                }
            },
            object :
                PrimitiveFunctionValue(
                    "polygons_hull",
                    FunctionSignature.simple(
                        listOf(Param("polygons", Type.vector(asType))),
                        SliceValueType.asType
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    val polyList = VectorValueType.of(SPolygonType).assertIs(args[0]).elements
                    val polys = Polygons()
                    polys.resize(polyList.size.toLong())
                    for (p in polyList) {
                        polys.pushBack(assertIs(p).poly)
                    }
                    return Slice(CrossSection.ConvexHull(polys))
                }
            },
        )
    }

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object :
                PrimitiveMethod(
                    "points",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.vector(Vec2ValueType.asType)),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).poly
                    val result = ArrayList<Value>()
                    for (p in self) {
                        result.add(Vec2(p.x(), p.y()))
                    }
                    return VectorValue(Vec2ValueType, result)
                }
            },
            object :
                PrimitiveMethod(
                    "convex_hull",
                    MethodSignature.simple(asType, emptyList<Param>(), SliceValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val poly = assertIs(target).poly
                    return Slice(CrossSection.ConvexHull(poly))
                }
            },
        )
    }

    override val providesVariables: Map<String, Value> = emptyMap()

    override fun assertIs(v: Value): SPolygon {
        if (v is SPolygon) {
            return v
        } else {
            throwTypeError(v)
        }
    }
}
