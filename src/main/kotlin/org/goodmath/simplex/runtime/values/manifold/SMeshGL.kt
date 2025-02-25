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

import manifold3d.manifold.MeshGL
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.VectorValue
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.twist.Twist

class SMeshGL(val mesh: MeshGL) : Value {
    override val valueType: ValueType = SMeshGLType

    override fun twist(): Twist {
        return Twist.obj(
            "SMeshGL",
            Twist.attr("numProp", mesh.numProp().toString()),
            Twist.attr("numTri", mesh.NumVert().toString()),
        )
    }
}

object SMeshGLType : ValueType() {
    override val name = "Mesh"

    override val asType: Type by lazy {
        Type.simple(name)
    }

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy { listOf() }

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object :
                PrimitiveMethod(
                    "num_vert",
                    MethodSignature.simple(asType, emptyList<Param>(), IntegerValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).mesh
                    return IntegerValue(self.NumVert())
                }
            },
            object :
                PrimitiveMethod(
                    "num_tri",
                    MethodSignature.simple(asType, emptyList<Param>(), IntegerValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).mesh
                    return IntegerValue(self.NumTri())
                }
            },
            object :
                PrimitiveMethod(
                    "num_prop",
                    MethodSignature.simple(asType, emptyList<Param>(), IntegerValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).mesh
                    return IntegerValue(self.numProp())
                }
            },
            object :
                PrimitiveMethod(
                    "vert_properties",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.vector(FloatValueType.asType)),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).mesh
                    val props = self.vertProperties()

                    val result = ArrayList<FloatValue>()
                    for (p in 0..<props.size()) {
                        result.add(FloatValue(props.get(p.toLong()).toDouble()))
                    }
                    return VectorValue(FloatValueType, result)
                }
            },
            // TODO
//            object: PrimitiveMethod("smooth",
//                MethodSignature(asType,
            object :
                PrimitiveMethod(
                    "tri_verts",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.vector(IntegerValueType.asType)),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).mesh
                    val verts = self.triVerts()
                    val result = ArrayList<IntegerValue>()
                    for (p in 0..<verts.size()) {
                        result.add(IntegerValue(verts.get(p.toLong()).toInt()))
                    }
                    return VectorValue(IntegerValueType, result)
                }
            },
        )
    }

    override val providesVariables: Map<String, Value> = emptyMap()

    override fun assertIs(v: Value): SMeshGL {
        return v as? SMeshGL ?: throwTypeError(v)
    }
}
