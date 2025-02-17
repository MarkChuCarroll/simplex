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
import org.goodmath.simplex.runtime.values.primitives.VectorValue
import org.goodmath.simplex.runtime.values.primitives.VectorValueType
import org.goodmath.simplex.runtime.values.primitives.BooleanValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.IntegerValue
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.runtime.values.primitives.Vec3
import org.goodmath.simplex.runtime.values.primitives.Vec3ValueType
import org.goodmath.simplex.twist.Twist

/** The Simplex wrapper for Manifold's Manifold type. */
class Solid(val manifold: Manifold) : Value {
    override val valueType: ValueType = SolidValueType
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
    fun splitByPlane(norm: Vec3, offset: Double): VectorValue {
        val mPair = manifold.splitByPlane(norm.toDoubleVec3(), offset.toFloat())
        val mList = listOf(Solid(mPair.first()), Solid(mPair.second()))
        return VectorValue(SolidValueType, mList)
    }
    
    fun split(other: Solid): VectorValue {
        val mPair = manifold.split(other.manifold)
        val mList = listOf(Solid(mPair.first()), Solid(mPair.second()))
        return VectorValue(SolidValueType, mList)
    }

    fun slice(height: Double): Slice = Slice(manifold.slice(height.toFloat()))

    fun slices(low: Double, high: Double, count: Int): VectorValue {
        val slices = manifold.slices(low.toFloat(), high.toFloat(), count)
        val result = ArrayList<Value>()
        for (slice in slices) {
            result.add(Slice(slice))
        }
        return VectorValue(SliceValueType, result)
    }

    fun normals(idx: Int, minSharpAngle: Double): Solid =
        Solid(manifold.calculateNormals(idx, minSharpAngle.toFloat()))

    fun smoothByNormals(idx: Int): Solid = Solid(manifold.smoothByNormals(idx))

    fun smoothOut(minSharpAngle: Double, minSmoothness: Double): Solid =
        Solid(manifold.smoothOut(minSharpAngle.toFloat(), minSmoothness.toFloat()))


    fun project(): Slice = Slice(manifold.project())

    fun refineToLength(length: Double): Solid = Solid(manifold.refineToLength(length.toFloat()))

    fun hull(): Solid {
        return Solid(manifold.convexHull())
    }

    companion object {
        fun union(bodies: List<Solid>): Solid =
            Solid(Manifold.BatchBoolean(SolidValueType.listToVec(bodies), OpType.Add))

        fun cuboid(width: Double, height: Double, depth: Double,
                   center: Boolean): Solid =
            cuboid(Vec3(width, height, depth), center)

        fun cuboid(v: Vec3, center: Boolean): Solid =
            Solid(Manifold.Cube(v.toDoubleVec3(), center))

        fun cylinder(height: Double, lowRadius: Double): Solid =
            Solid(Manifold.Cylinder(height.toFloat(), lowRadius.toFloat(), lowRadius.toFloat(), 0)).move(0.0, 0.0, -height)

        fun cylinder(height: Double, lowRadius: Double, highRadius: Double): Solid =
            Solid(Manifold.Cylinder(height.toFloat(), lowRadius.toFloat(), highRadius.toFloat(), 0)).move(0.0, 0.0, -height)

        fun cylinder(height: Double, lowRadius: Double, highRadius: Double, facets: Int): Solid =
            Solid(Manifold.Cylinder(height.toFloat(), lowRadius.toFloat(), highRadius.toFloat(), facets)).move(0.0, 0.0, -height)

        fun spheroid(x: Double, y: Double, z: Double, segments: Int): Solid =
            Solid(Manifold.Sphere(x.toFloat(), segments).scale(1.0, y, z))
    }
}

object SolidValueType : ValueType() {
    override val name: String = "Solid"
    override val asType: Type by lazy {
        Type.simple(name)
    }

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
                    "ovoid",
                    FunctionSignature.multi(
                        listOf(
                            listOf(Param("radius", FloatValueType.asType)),
                            listOf(Param("radius", FloatValueType.asType), Param("segments", IntegerValueType.asType)),
                            listOf(Param("x", FloatValueType.asType),
                                Param("y", FloatValueType.asType),
                                Param("z", FloatValueType.asType),
                                Param("segments", IntegerValueType.asType))
                        ),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    if (args.size == 1) {
                        val radius = assertIsFloat(args[0])
                        return Solid.spheroid(radius, radius, radius, 0)
                    } else if (args.size == 2) {
                        val radius = assertIsFloat(args[0])
                        val segments = assertIsInt(args[1])
                        return Solid.spheroid(radius, radius, radius, segments)
                    } else {
                        val x = assertIsFloat(args[0])
                        val y = assertIsFloat(args[1])
                        val z = assertIsFloat(args[2])
                        val segments = if (args.size > 3) {
                            assertIsInt(args[3])
                        } else {
                            0
                        }
                        return Solid.spheroid(x, y, z, segments)
                    }
                }
            },
            object :
                PrimitiveFunctionValue(
                    "cuboid",
                    FunctionSignature.multi(
                        listOf(
                            listOf(Param("v", Vec3ValueType.asType)),
                            listOf(Param("v", Vec3ValueType.asType), Param("center", BooleanValueType.asType)),
                            listOf(Param("x", FloatValueType.asType),
                                Param("y", FloatValueType.asType),
                                Param("z", FloatValueType.asType)),
                            listOf(Param("x", FloatValueType.asType),
                                Param("y", FloatValueType.asType),
                                Param("z", FloatValueType.asType),
                                Param("centered", BooleanValueType.asType))),
                        asType,
                    ),
                ) {
                override fun execute(args: List<Value>): Value {
                    if (args.size == 1) {
                        return Solid.cuboid(Vec3ValueType.assertIs(args[0]), true)
                    } else if (args.size == 2) {
                        return Solid.cuboid(Vec3ValueType.assertIs(args[0]), assertIsBoolean(args[1]))
                    } else if (args.size == 3) {
                        return Solid.cuboid(
                            assertIsFloat(args[0]), assertIsFloat(args[1]),
                            assertIsFloat(args[2]), true
                        )
                    } else {
                            return Solid.cuboid(
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
                                Param("height", FloatValueType.asType),
                                Param("radiusLow", FloatValueType.asType)),
                            listOf(
                                Param("height", FloatValueType.asType),
                                Param("radiusLow", FloatValueType.asType),
                                Param("radiusHigh", FloatValueType.asType)),
                            listOf(
                                Param("height", FloatValueType.asType),
                                Param("radiusLow", FloatValueType.asType),
                                Param("radiusHigh", FloatValueType.asType),
                                Param("facets", IntegerValueType.asType))),
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
                    FunctionSignature.simple(listOf(Param("size", FloatValueType.asType)), asType),
                ) {
                override fun execute(args: List<Value>): Value {
                    val tet = Manifold.Tetrahedron()
                    val scale = assertIsFloat(args[0])
                    return Solid(tet.scale(Vec3(scale, scale, scale).toDoubleVec3()))
                }
            },
            object: PrimitiveFunctionValue(
                "union",
                FunctionSignature.simple(listOf(Param("shapes",
                                                      VectorValueType.of(this).asType)),
                                                asType)
                ) {
                override fun execute(args: List<Value>): Value {
                    val solids = ArrayList(VectorValueType.of(this@SolidValueType).assertIs(args[0]).elements.map { (it as Solid).manifold })
                    val vec = ManifoldVector(solids)
                    return Solid(Manifold.BatchBoolean(vec, ManifoldOpType.Add.opCode))
                }
            }
        )
    }

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object: PrimitiveMethod(
                "bounds",
                MethodSignature.simple(asType, emptyList<Param>(),
                                       BoundingBoxValueType.asType)
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
                                Param("x", FloatValueType.asType),
                                Param("y", FloatValueType.asType),
                                Param("z", FloatValueType.asType),
                                ),
                            listOf(Param("v", Vec3ValueType.asType)),
                            ),
                        asType
                    ),
                    ) {
                    override fun execute(target: Value, args: List<Value>, env: Env): Value {
                        val self: Solid = assertIs(target)
                        if (args.size == 1) {
                            val v = Vec3ValueType.assertIs(args[0])
                            return self.move(v)
                        } else {
                            val x = assertIsFloat(args[0])
                            val y = assertIsFloat(args[1])
                            val z = assertIsFloat(args[2])
                            return self.move(x, y, z)
                        }
                    }
                      },
            object: PrimitiveMethod(
                "left",
                MethodSignature.simple(asType, listOf(Param("distance", FloatValueType.asType)),
                                       asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val dist = assertIsFloat(args[0])
                    return self.move(-dist, 0.0, 0.0)
                }
            },
            object: PrimitiveMethod(
                "right",
                MethodSignature.simple(asType, listOf(Param("distance", FloatValueType.asType)),
                                       asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val dist = assertIsFloat(args[0])
                    return self.move(dist, 0.0, 0.0)
                }
            },
            object: PrimitiveMethod(
                "forward",
                MethodSignature.simple(asType, listOf(Param("distance", FloatValueType.asType)),
                                       asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val dist = assertIsFloat(args[0])
                    return self.move(0.0, dist, 0.0)
                }
            },
            object: PrimitiveMethod(
                "backward",
                MethodSignature.simple(asType, listOf(Param("distance", FloatValueType.asType)),
                                       asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val dist = assertIsFloat(args[0])
                    return self.move(0.0, -dist, 0.0)
                }
            },
            object: PrimitiveMethod(
                "up",
                MethodSignature.simple(asType, listOf(Param("distance", FloatValueType.asType)),
                                       asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val dist = assertIsFloat(args[0])
                    return self.move(0.0, 0.0, dist)
                }
            },
            object: PrimitiveMethod(
                "down",
                MethodSignature.simple(asType, listOf(Param("distance", FloatValueType.asType)),
                                       asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val dist = assertIsFloat(args[0])
                    return self.move(0.0, 0.0, -dist)
                }
            },
            object :
                PrimitiveMethod(
                    "scale",
                    MethodSignature.multi(
                        asType,
                        listOf(
                            listOf(
                                Param("x", FloatValueType.asType),
                                Param("y", FloatValueType.asType),
                                Param("z", FloatValueType.asType),
                                ),
                            listOf(Param("v", Vec3ValueType.asType)),
                            ),
                        asType,
                        ),
                    ) {
                    override fun execute(target: Value, args: List<Value>, env: Env): Value {
                        val self = assertIs(target)
                        if (args.size == 1) {
                            val v = Vec3ValueType.assertIs(args[0])
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
                                Param("x", FloatValueType.asType),
                                Param("y", FloatValueType.asType),
                                Param("z", FloatValueType.asType),
                                ),
                            listOf(Param("v", Vec3ValueType.asType)),
                            ),
                        asType,
                        ),
                    ) {
                    override fun execute(target: Value, args: List<Value>, env: Env): Value {
                        val self = assertIs(target)
                        if (args.size == 1) {
                            val v = Vec3ValueType.assertIs(args[0])
                            return self.rotate(v)
                        } else {
                            val x = assertIsFloat(args[0])
                            val y = assertIsFloat(args[1])
                            val z = assertIsFloat(args[2])
                            return self.rotate(x, y, z)
                        }
                    }
                      },
            object: PrimitiveMethod(
                "rotx",
                MethodSignature.simple(asType, listOf(Param("angle", FloatValueType.asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val angle = assertIsFloat(args[0])
                    return self.rotate(angle, 0.0, 0.0)
                }
            },
            object: PrimitiveMethod(
                "roty",
                MethodSignature.simple(asType, listOf(Param("angle", FloatValueType.asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val angle = assertIsFloat(args[0])
                    return self.rotate(0.0, angle, 0.0)
                }
            },
            object: PrimitiveMethod(
                "rotz",
                MethodSignature.simple(asType, listOf(Param("angle", FloatValueType.asType)), asType)) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val angle = assertIsFloat(args[0])
                    return self.rotate(0.0, 0.0, angle)
                }
            },
            object :
                PrimitiveMethod(
                    "mirror",
                    MethodSignature.simple(asType, listOf(Param("norm", FloatValueType.asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val norm = Vec3ValueType.assertIs(args[0])
                    return self.mirror(norm)
                }
            },
            object :
                PrimitiveMethod(
                    "refine",
                    MethodSignature.simple(asType, listOf(Param("factor", IntegerValueType.asType)), asType),
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
                    MethodSignature.simple(asType, emptyList<Param>(), IntegerValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.genus()
                }
            },
            object :
                PrimitiveMethod(
                    "surface_area",
                    MethodSignature.simple(asType, emptyList<Param>(), FloatValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.surfaceArea()
                }
            },
            object :
                PrimitiveMethod(
                    "volume",
                    MethodSignature.simple(asType, emptyList<Param>(), FloatValueType.asType),
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
                            Param("normal", Vec3ValueType.asType),
                            Param("origin_offset", FloatValueType.asType),
                        ),
                        VectorValueType(this).asType
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val norm = Vec3ValueType.assertIs(args[0])
                    val offset = assertIsFloat(args[1])
                    return self.splitByPlane(norm, offset)
                }
            },
            object :
                PrimitiveMethod(
                    "split",
                    MethodSignature.simple(asType, listOf(Param("other", asType)), FloatValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val other = assertIs(args[0])
                    return self.split(other)
                }
            },
            object: PrimitiveMethod("hull",
                MethodSignature.simple(asType,
                    emptyList<Param>(), asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIs(target)
                    return self.hull()
                }
            },
            object :
                PrimitiveMethod(
                    "slice",
                    MethodSignature.simple(
                        asType,
                        listOf(Param("height", FloatValueType.asType)),
                        SliceValueType.asType
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
                            Param("bottom_z", FloatValueType.asType),
                            Param("top_z", FloatValueType.asType),
                            Param("count", IntegerValueType.asType),
                        ),
                        SliceValueType.asType
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
                        listOf(Param("idx", IntegerValueType.asType), Param("minSharpAngle", FloatValueType.asType)),
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
                    MethodSignature.simple(asType, listOf(Param("normal_idx", IntegerValueType.asType)), asType),
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
                            Param("min_sharp_angle", FloatValueType.asType),
                            Param("minSmoothness", FloatValueType.asType),
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
                    MethodSignature.simple(asType, emptyList<Param>(), SliceValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return self.project()
                }
            },
            object :
                PrimitiveMethod(
                    "refine_to_length",
                    MethodSignature.simple(asType, listOf(Param("length", FloatValueType.asType)), asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val length = assertIsFloat(args[0])
                    return self.refineToLength(length)
                }
            },
            object: PrimitiveMethod("set_material",
                MethodSignature.simple(asType,
                    listOf(Param("material", SMaterialValueType.asType)),
                    asType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>,
                    env: Env
                ): Value {
                    val self = assertIs(target)
                    val material = SMaterialValueType.assertIs(args[0])
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
