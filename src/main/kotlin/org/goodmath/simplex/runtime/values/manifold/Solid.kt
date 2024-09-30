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

import manifold3d.Manifold
import manifold3d.ManifoldVector
import manifold3d.manifold.ExportOptions
import manifold3d.manifold.MeshIO
import manifold3d.pub.OpType
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.ArrayValue
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.runtime.values.primitives.Vec3
import org.goodmath.simplex.runtime.values.primitives.Vec3Type
import org.goodmath.simplex.twist.Twist

/** The Simplex wrapper for Manifold's Manifold type. */
class Solid(val manifold: Manifold) : Value {
    override val valueType: ValueType = SolidType
    var material: SMaterial = SMaterial.smoothGray

    override fun twist(): Twist =
        Twist.obj(
            "Manifold",
            Twist.attr("numTri", manifold.numTri().toString()),
            Twist.attr("numVert", manifold.numVert().toString()),
        )

    fun export(path: String, material: SMaterial? = null) {
        val exportOptions = ExportOptions()
        if (material != null) {
            exportOptions.material(material.material)
        }
        MeshIO.ExportMesh(path, manifold.mesh, exportOptions)
    }

    fun boundingBox(): BoundingBox = BoundingBox(manifold.boundingBox())

    fun move(v: Vec3): Solid = move(v.x, v.y, v.z)

    fun move(x: Double, y: Double, z: Double): Solid = Solid(manifold.translate(x, y, z))

    fun rotate(v: Vec3): Solid = rotate(v.x, v.y, v.z)

    fun rotate(x: Double, y: Double, z: Double): Solid =
        Solid(manifold.rotate(x.toFloat(), y.toFloat(), z.toFloat()))

    fun scale(v: Vec3): Solid = scale(v.x, v.y, v.z)

    fun scale(x: Double, y: Double, z: Double): Solid = Solid(manifold.scale(x, y, z))

    fun mirror(norm: Vec3): Solid = Solid(manifold.mirror(norm.toDoubleVec3()))

    fun refine(factor: Int): Solid = Solid(manifold.refine(factor))

    operator fun plus(other: Solid): Solid = Solid(manifold.add(other.manifold))

    operator fun minus(other: Solid): Solid = Solid(manifold.subtract(other.manifold))

    fun intersect(other: Solid): Solid = Solid(manifold.intersect(other.manifold))

    fun genus(): IntegerValue = IntegerValue(manifold.genus())

    fun surfaceArea(): FloatValue = FloatValue(manifold.properties.surfaceArea().toDouble())

    fun volume(): FloatValue = FloatValue(manifold.properties.surfaceArea().toDouble())

    fun splitByPlane(norm: Vec3, offset: Double): ArrayValue {
        val mPair = manifold.splitByPlane(norm.toDoubleVec3(), offset.toFloat())
        val mList = listOf(Solid(mPair.first()), Solid(mPair.second()))
        return ArrayValue(SolidType, mList)
    }

    fun split(other: Solid): ArrayValue {
        val mPair = manifold.split(other.manifold)
        val mList = listOf(Solid(mPair.first()), Solid(mPair.second()))
        return ArrayValue(SolidType, mList)
    }

    fun slice(height: Double): Slice = Slice(manifold.slice(height.toFloat()))

    fun slices(low: Double, high: Double, count: Int): ArrayValue {
        val slices = manifold.slices(low.toFloat(), high.toFloat(), count)
        val result = ArrayList<Value>()
        for (slice in slices) {
            result.add(Slice(slice))
        }
        return ArrayValue(SliceType, result)
    }

    fun normals(idx: Int, minSharpAngle: Double): Solid =
        Solid(manifold.calculateNormals(idx, minSharpAngle.toFloat()))

    fun smoothByNormals(idx: Int): Solid = Solid(manifold.smoothByNormals(idx))

    fun smoothOut(minSharpAngle: Double, minSmoothness: Double): Solid =
        Solid(manifold.smoothOut(minSharpAngle.toFloat(), minSmoothness.toFloat()))


    fun project(): Slice = Slice(manifold.project())

    fun refineToLength(length: Double): Solid = Solid(manifold.refineToLength(length.toFloat()))

    companion object {
        fun union(bodies: List<Solid>): Solid =
            Solid(Manifold.BatchBoolean(SolidType.listToVec(bodies), OpType.Add))

        fun brick(width: Double, height: Double, depth: Double,
                  center: Boolean): Solid =
            brick(Vec3(width, height, depth), center)

        fun brick(v: Vec3, center: Boolean): Solid =
            Solid(Manifold.Cube(v.toDoubleVec3(), center))

        fun cylinder(height: Double, lowRadius: Double): Solid =
            Solid(Manifold.Cylinder(height.toFloat(), lowRadius.toFloat(), lowRadius.toFloat(), 0))

        fun cylinder(height: Double, lowRadius: Double, highRadius: Double): Solid =
            Solid(Manifold.Cylinder(height.toFloat(), lowRadius.toFloat(), highRadius.toFloat(), 0))

        fun cylinder(height: Double, lowRadius: Double, highRadius: Double, facets: Int): Solid =
            Solid(Manifold.Cylinder(height.toFloat(), lowRadius.toFloat(), highRadius.toFloat(), facets))

        fun blob(x: Double, y: Double, z: Double, segments: Int): Solid =
            Solid(Manifold.Sphere(x.toFloat(), segments).scale(1.0, y, z))
    }
}

object SolidType : ValueType() {
    override val name: String = "Solid"
    override val asType: Type = Type.SolidType

    fun listToVec(solids: List<Solid>): ManifoldVector {
        return ManifoldVector(solids.map { it.manifold }.toTypedArray())
    }

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object :
                PrimitiveFunctionValue(
                    "blob",
                    FunctionSignature.multi(
                        listOf(
                            listOf(Param("radius", Type.FloatType)),
                            listOf(Param("radius", Type.FloatType), Param("segments", Type.IntType)),
                            listOf(Param("x", Type.FloatType),
                                Param("y", Type.FloatType),
                                Param("z", Type.FloatType),
                                Param("segments", Type.IntType))
                        ),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    if (args.size == 1) {
                        val radius = assertIsFloat(args[0])
                        return Solid.blob(radius, radius, radius, 0)
                    } else if (args.size == 2) {
                        val radius = assertIsFloat(args[0])
                        val segments = assertIsInt(args[1])
                        return Solid.blob(radius, radius, radius, segments)
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        val z = assertIsFloat(args[2])
                        val segments = if (args.size > 3) {
                            assertIsInt(args[3])
                        } else {
                            0
                        }
                        return Solid.blob(x, y, z, segments)
                    }
                }
            },
            object :
                PrimitiveFunctionValue(
                    "brick",
                    FunctionSignature.multi(
                        listOf(
                            listOf(Param("v", Type.Vec3Type)),
                            listOf(Param("v", Type.Vec3Type), Param("center", Type.BooleanType)),
                            listOf(Param("x", Type.FloatType),
                                Param("y", Type.FloatType),
                                Param("z", Type.FloatType)),
                            listOf(Param("x", Type.FloatType),
                                Param("y", Type.FloatType),
                                Param("z", Type.FloatType),
                                Param("centered", Type.BooleanType))),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    if (args.size == 1) {
                        return Solid.brick(Vec3Type.assertIs(args[0]), true)
                    } else if (args.size == 2) {
                        return Solid.brick(Vec3Type.assertIs(args[0]), assertIsBoolean(args[1]))
                    } else if (args.size == 3) {
                        return Solid.brick(
                            assertIsFloat(args[0]), assertIsFloat(args[1]),
                            assertIsFloat(args[2]), true
                        )
                    } else {
                            return Solid.brick(
                                assertIsFloat(args[0]), assertIsFloat(args[1]),
                                assertIsFloat(args[2]), assertIsBoolean(args[3]))
                        }
                }
            },
            object :
                PrimitiveFunctionValue(
                    "cylinder",
                    FunctionSignature.multi(
                        listOf(
                            listOf(
                                Param("height", Type.FloatType),
                                Param("radiusLow", Type.FloatType)),
                            listOf(
                                Param("height", Type.FloatType),
                                Param("radiusLow", Type.FloatType),
                                Param("radiusHigh", Type.FloatType)),
                            listOf(
                                Param("height", Type.FloatType),
                                Param("radiusLow", Type.FloatType),
                                Param("radiusHigh", Type.FloatType),
                                Param("facets", Type.IntType))),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    val height = assertIsFloat(args[0])
                    val radiusLow = assertIsFloat(args[1])
                    val radiusHigh = if (args.size > 2) {
                        assertIsFloat(args[2])
                    } else {
                        radiusLow
                    }
                    val facets = if (args.size > 3) {
                        assertIsInt(args[3])
                    } else {
                        0
                    }
                    return Solid.cylinder(height, radiusLow, radiusHigh, facets)
                }
            },
            object :
                PrimitiveFunctionValue(
                    "tetrahedron",
                    FunctionSignature.simple(emptyList<Param>(), Type.SolidType),
                ) {
                override fun execute(args: List<Value>): Value {
                    return Solid(Manifold.Tetrahedron())
                }
            },
        )
    }

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object: PrimitiveMethod(
                "bounds",
                MethodSignature.simple(asType, emptyList<Param>(),
                    Type.BoundingBoxType)
            ) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIs(target)
                    return self.boundingBox()
                }
            },
            object :
                PrimitiveMethod(
                    "move",
                    MethodSignature.multi(
                        asType,
                        listOf(
                            listOf(
                                Param("x", Type.FloatType),
                                Param("y", Type.FloatType),
                                Param("z", Type.FloatType),
                            ),
                            listOf(Param("v", Type.Vec3Type)),
                        ),
                        Type.SolidType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self: Solid = assertIs(target)
                    if (args.size == 1) {
                        val v = Vec3Type.assertIs(args[0])
                        return self.move(v)
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        val z = assertIsFloat(args[2])
                        return self.move(x, y, z)
                    }
                }
            },
            object :
                PrimitiveMethod(
                    "scale",
                    MethodSignature.multi(
                        asType,
                        listOf(
                            listOf(
                                Param("x", Type.FloatType),
                                Param("y", Type.FloatType),
                                Param("z", Type.FloatType),
                            ),
                            listOf(Param("v", Type.Vec3Type)),
                        ),
                        asType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    if (args.size == 1) {
                        val v = Vec3Type.assertIs(args[0])
                        return self.scale(v)
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        val z = assertIsFloat(args[2])
                        return self.scale(x, y, z)
                    }
                }
            },
            object :
                PrimitiveMethod(
                    "rotate",
                    MethodSignature.multi(
                        asType,
                        listOf(
                            listOf(
                                Param("x", Type.FloatType),
                                Param("y", Type.FloatType),
                                Param("z", Type.FloatType),
                            ),
                            listOf(Param("v", Type.Vec3Type)),
                        ),
                        asType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    if (args.size == 1) {
                        val v = Vec3Type.assertIs(args[0])
                        return self.rotate(v)
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        val z = assertIsFloat(args[2])
                        return self.rotate(x, y, z)
                    }
                }
            },
            object :
                PrimitiveMethod(
                    "mirror",
                    MethodSignature.simple(asType, listOf(Param("norm", Type.FloatType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val norm = Vec3Type.assertIs(args[0])
                    return self.mirror(norm)
                }
            },
            object :
                PrimitiveMethod(
                    "refine",
                    MethodSignature.simple(asType, listOf(Param("factor", Type.IntType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val n = assertIsInt(args[0])
                    return self.refine(n)
                }
            },
            object :
                PrimitiveMethod(
                    "plus",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return self + other
                }
            },
            object :
                PrimitiveMethod(
                    "minus",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return self - other
                }
            },
            object :
                PrimitiveMethod(
                    "intersect",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return self.intersect(other)
                }
            },
            object :
                PrimitiveMethod(
                    "genus",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.IntType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.genus()
                }
            },
            object :
                PrimitiveMethod(
                    "surface_area",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.FloatType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.surfaceArea()
                }
            },
            object :
                PrimitiveMethod(
                    "volume",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.FloatType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.volume()
                }
            },
            object :
                PrimitiveMethod(
                    "split_by_plane",
                    MethodSignature.simple(
                        asType,
                        listOf(
                            Param("normal", Type.Vec3Type),
                            Param("origin_offset", Type.FloatType),
                        ),
                        Type.array(Type.SolidType),
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val norm = Vec3Type.assertIs(args[0])
                    val offset = assertIsFloat(args[1])
                    return self.splitByPlane(norm, offset)
                }
            },
            object :
                PrimitiveMethod(
                    "split",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), Type.FloatType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return self.split(other)
                }
            },
            object :
                PrimitiveMethod(
                    "slice",
                    MethodSignature.simple(
                        asType,
                        listOf(Param("height", Type.FloatType)),
                        Type.SliceType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val height = assertIsFloat(args[0])
                    return self.slice(height)
                }
            },
            object :
                PrimitiveMethod(
                    "slices",
                    MethodSignature.simple(
                        asType,
                        listOf(
                            Param("bottom_z", Type.FloatType),
                            Param("top_z", Type.FloatType),
                            Param("count", Type.IntType),
                        ),
                        Type.array(Type.SliceType),
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val lowZ = assertIsFloat(args[0])
                    val highZ = assertIsFloat(args[1])
                    val count = assertIsInt(args[2])
                    return self.slices(lowZ, highZ, count)
                }
            },
            object :
                PrimitiveMethod(
                    "normals",
                    MethodSignature.simple(
                        asType,
                        listOf(Param("idx", Type.IntType), Param("minSharpAngle", Type.FloatType)),
                        asType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val idx = assertIsInt(args[0])
                    val minSharpAngle = assertIsFloat(args[1])
                    return self.normals(idx, minSharpAngle)
                }
            },
            object :
                PrimitiveMethod(
                    "smooth_by_normals",
                    MethodSignature.simple(asType, listOf(Param("normal_idx", Type.IntType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val idx = assertIsInt(args[0])
                    return self.smoothByNormals(idx)
                }
            },
            object :
                PrimitiveMethod(
                    "smooth_out",
                    MethodSignature.simple(
                        asType,
                        listOf(
                            Param("min_sharp_angle", Type.FloatType),
                            Param("minSmoothness", Type.FloatType),
                        ),
                        asType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val minSharpAngle = assertIsFloat(args[0])
                    val minSmoothness = assertIsFloat(args[1])
                    return self.smoothOut(minSharpAngle, minSmoothness)
                }
            },
            object :
                PrimitiveMethod(
                    "project",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.SliceType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.project()
                }
            },
            object :
                PrimitiveMethod(
                    "refine_to_length",
                    MethodSignature.simple(asType, listOf(Param("length", Type.FloatType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val length = assertIsFloat(args[0])
                    return self.refineToLength(length)
                }
            },
            object: PrimitiveMethod("set_material",
                MethodSignature.simple(asType,
                    listOf(Param("material", Type.MaterialType)),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIs(target)
                    val material = SMaterialType.assertIs(args[0])
                    self.material = material
                    return self
                }
            }
        )
    }
    override val providesVariables: Map<String, Value> by lazy { emptyMap() }

    override fun assertIs(v: Value): Solid {
        return if (v is Solid) {
            v
        } else {
            throwTypeError(v)
        }
    }
}
