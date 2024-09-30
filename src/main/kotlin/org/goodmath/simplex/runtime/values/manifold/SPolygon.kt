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
import org.goodmath.simplex.runtime.values.primitives.ArrayValue
import org.goodmath.simplex.runtime.values.primitives.ArrayValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.runtime.values.primitives.Vec2
import org.goodmath.simplex.runtime.values.primitives.Vec2Type
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

    override val asType: Type = Type.simple("Polygon")

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object :
                PrimitiveFunctionValue(
                    "polygon",
                    FunctionSignature.simple(listOf(Param("points", Type.array(Type.Vec2Type))), asType),
                ) {
                override fun execute(args: List<Value>): Value {
                    val points = ArrayValueType.of(Vec2Type).assertIs(args[0]).elements
                    val floatArray =
                        DoubleArray(
                            points.size,
                            { idx ->
                                val halfIdx = idx / 2
                                val mod = idx % 2
                                assertIsFloat(points[halfIdx + mod])
                            },
                        )
                    return SPolygon(SimplePolygon.FromArray(floatArray))
                }
            },
            object :
                PrimitiveFunctionValue(
                    "polygons_hull",
                    FunctionSignature.simple(
                        listOf(Param("polygons", Type.array(Type.PolygonType))),
                        Type.SliceType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    val polyList = ArrayValueType.of(SPolygonType).assertIs(args[0]).elements
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
                    MethodSignature.simple(asType, emptyList<Param>(), Type.array(Type.Vec2Type)),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).poly
                    val result = ArrayList<Value>()
                    for (p in self) {
                        result.add(Vec2(p.x(), p.y()))
                    }
                    return ArrayValue(Vec2Type, result)
                }
            },
            object :
                PrimitiveMethod(
                    "convex_hull",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.SliceType),
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