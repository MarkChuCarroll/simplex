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
package org.goodmath.simplex.manifold

import com.sun.jna.Memory
import com.sun.jna.Pointer

fun Boolean.toInt() = if (this) 1 else 0

fun List<MManifold>.toMVec(): ManifoldManifoldVec {
    val mvec = mc.manifold_manifold_vec(Memory(mc.manifold_manifold_vec_size()), size)
    this.forEachIndexed {idx, m ->
        mc.manifold_manifold_vec_set(mvec, idx, m.manifold)
    }
    return mvec
}

fun ManifoldManifoldVec.toList(): List<MManifold> {
    val result = ArrayList<MManifold>()
    for (i in 0..mc.manifold_manifold_vec_length(this)) {
        val mm = MManifold(mc.manifold_manifold_vec_get(
            MManifold.allocate(), this, i))
        result.add(mm)
    }
    mc.manifold_destruct_manifold_vec(this)
    (this as Memory).close()
    return result
}

class MManifold(val manifold: ManifoldManifold): MType(manifold) {
    fun copy(): MManifold {
        return MManifold(mc.manifold_copy(allocate(), manifold))
    }

    fun decompose(): List<MManifold> {
        val mem = Memory(mc.manifold_manifold_vec_size())
        val vec = mc.manifold_decompose(mem, this.manifold)
        val result = ArrayList<MManifold>()
        for (i in 0..mc.manifold_manifold_vec_length(vec)) {
            val m = mc.manifold_manifold_vec_get(allocate(),
                vec, i)
            result.add(MManifold(m))
        }
        return result
    }

    fun asOriginal(): MManifold {
        return MManifold(mc.manifold_as_original(allocate(), this.manifold))
    }

    fun isEmpty(): Boolean {
        return mc.manifold_is_empty(manifold) != 0
    }

    fun status(): ManifoldError {
        return mc.manifold_status(manifold)
    }

    fun numVert(): Int {
        return mc.manifold_num_vert(manifold)
    }

    fun numEdge(): Int {
        return mc.manifold_num_edge(manifold)
    }

    fun numTri(): Int {
        return mc.manifold_num_tri(manifold)
    }

    fun boundingBox(): MBox {
        return MBox(mc.manifold_bounding_box(
            Memory(mc.manifold_box_size()),
            manifold))
    }

    fun boolean(b: MManifold, op: ManifoldOpType): MManifold {
        return MManifold(mc.manifold_boolean(allocate(),
            manifold, b.manifold, op))
    }

    fun union(b: MManifold): MManifold {
        return MManifold(mc.manifold_union(allocate(), manifold, b.manifold))
    }

    fun difference(b: MManifold): MManifold {
        return MManifold(mc.manifold_difference(allocate(), manifold, b.manifold))
    }

    fun intersection(b: MManifold): MManifold {
        return MManifold(mc.manifold_intersection(allocate(), manifold, b.manifold))
    }

    fun split(b: MManifold): Pair<MManifold, MManifold> {
        val result =  mc.manifold_split(allocate(), allocate(),
            manifold, b.manifold
        )
        return Pair(MManifold(result.first), MManifold(result.second))
    }

    fun splitByPlane(normal: Vec3, offset: Float): Pair<MManifold, MManifold> {
        val result =  mc.manifold_split_by_plane(allocate(), allocate(),
            manifold, normal.x, normal.y, normal.z, offset)
        return Pair(MManifold(result.first), MManifold(result.second))
    }

    fun trimByPlane(normal: Vec3, offset: Float): MManifold {
        return MManifold(mc.manifold_trim_by_plane(allocate(),
            manifold, normal.x, normal.y, normal.z, offset))
    }

    fun slice(height: Float): MPolygons {
        return  MPolygons(mc.manifold_slice(MPolygons.allocate(),
            manifold,
            height
            ))
    }

    fun project(): MPolygons {
        return MPolygons(mc.manifold_project(allocate(), manifold))
    }

    fun hull(): MManifold {
        return MManifold(mc.manifold_hull(allocate(), manifold))
    }

    fun translate(v: Vec3): MManifold {
        return MManifold(mc.manifold_translate(allocate(), manifold, v.x, v.y, v.z))
    }

    fun rotate(v: Vec3): MManifold {
        return MManifold(mc.manifold_rotate(allocate(), manifold, v.x, v.y, v.z))
    }

    fun scale(v: Vec3): MManifold {
        return MManifold(mc.manifold_scale(allocate(), manifold, v.x, v.y, v.z))
    }

    fun transform(one: Vec3, two: Vec3, three: Vec3, four: Vec3): MManifold {
        return MManifold(mc.manifold_transform(allocate(),
            manifold, one.x, one.y, one.z,
            two.x, two.y, two.z,
            three.x, three.y, three.z,
            four.x, four.y,  four.z))
    }

    fun mirror(normal: Vec3): MManifold {
        return MManifold(mc.manifold_mirror(allocate(), manifold, normal.x, normal.y, normal.z))
    }

    fun warp(warp: ManifoldC.WarpCallback, ctx: Pointer): MManifold {
        return MManifold(mc.manifold_warp(allocate(), manifold, warp, ctx))
    }

    fun smoothByNormals(
        normalIdx: Int
    ): MManifold {
        return MManifold(mc.manifold_smooth_by_normals(allocate(), manifold, normalIdx))
    }

    fun smoothOut(
        minSharpAngle: Float, minSmoothness: Float
    ): MManifold {
        return MManifold(mc.manifold_smooth_out(allocate(), manifold, minSharpAngle, minSmoothness))
    }

    fun refine(refine: Int): MManifold {
        return MManifold(mc.manifold_refine(allocate(), manifold, refine))
    }

    fun refineToLength(length: Float): MManifold {
        return MManifold(mc.manifold_refine_to_length(allocate(), manifold, length))
    }

    fun genus(): Int {
        return mc.manifold_genus(manifold)
    }

    fun precision(): Float {
        return mc.manifold_precision(manifold)
    }

    fun properties(): ManifoldC.ManifoldProperties {
        return mc.manifold_get_properties(manifold)
    }

     fun originalId(): Int {
         return mc.manifold_original_id(manifold)
     }

    fun setProperties(numProp: Int, pset: ManifoldC.PropertySetCallback, ctx: Pointer): MManifold {
        return MManifold(mc.manifold_set_properties(allocate(), manifold, numProp, pset, ctx))
    }

    fun calculateCurvature(gaussianIdx: Int, meanIdx: Int): MManifold {
        return MManifold(mc.manifold_calculate_curvature(allocate(), manifold, gaussianIdx, meanIdx))
    }

    fun minGap(m:  MManifold, searchLength: Float): Float {
        return mc.manifold_min_gap(manifold, m.manifold, searchLength)
    }

    fun calculateNormals(normalIdx: Int, minSharpAngle: Int): MManifold {
        return MManifold(mc.manifold_calculate_normals(allocate(), manifold, normalIdx, minSharpAngle))
    }

    fun getMeshGl(): MMeshGL {
        return MMeshGL(mc.manifold_get_meshgl(MMeshGL.allocate(), manifold))
    }

    override fun close() {
        mc.manifold_destruct_manifold(manifold)
        super.close()
    }

    companion object {
        fun allocate(): Memory = Memory(mc.manifold_manifold_size())

        fun batchBoolean(ms: List<MManifold>, op: ManifoldOpType): MManifold {
            return MManifold(mc.manifold_batch_boolean(
                allocate(), ms.toMVec(), op)
            )
        }

        fun empty(): MManifold {
            val man = mc.manifold_empty(allocate())
            return MManifold(man)
        }

        fun tetrahedron(): MManifold {
            val man =  mc.manifold_tetrahedron(allocate())
            return MManifold(man)
        }

        fun cube(x:  Float, y: Float, z: Float, center: Boolean): MManifold {
            val man =  mc.manifold_cube(allocate(), x, y, z,
                center.toInt())
            return MManifold(man)
        }

        fun cylinder(height: Float, radiusLow: Float, radiusHigh: Float,
                     circularSegments: Int, center: Boolean): MManifold {
            val man = mc.manifold_cylinder(allocate(), height, radiusLow, radiusHigh, circularSegments, center.toInt())
            return MManifold(man)
        }

        fun sphere(radius: Float, circularSegments: Int): MManifold {
            val man = mc.manifold_sphere(allocate(), radius, circularSegments)
            return MManifold(man)
        }

        fun ofMeshGL(mesh: MMeshGL): MManifold {
            val man = mc.manifold_of_meshgl(allocate(), mesh.ptr)
            return MManifold(man)
        }


        fun smooth(mesh: MMeshGL, halfEdges: Array<Int>, smoothness: Array<Float>): MManifold {
            val man = mc.manifold_smooth(allocate(), mesh.ptr,
                halfEdges, smoothness, halfEdges.size)
            return MManifold(man)
        }

        fun extrude(polygons: MPolygons, height: Float,
                    slices: Int, twistDegrees: Float,
                    scaleX: Float, scaleY: Float): MManifold {
            val man = mc.manifold_extrude(allocate(),
                polygons.ptr, height, slices, twistDegrees, scaleX, scaleY)
            return MManifold(man)
        }

        fun manifold_revolve(polys: MPolygons,
                             circularSegments: Int): MManifold {
            val man = mc.manifold_revolve(allocate(),
                polys.msPolygons, circularSegments)
            return MManifold(man)
        }

        fun compose(manifolds: List<MManifold>): MManifold {
            val vec = mc.manifold_manifold_vec(
                Memory(mc.manifold_manifold_vec_size()),
                manifolds.size
            )
            manifolds.forEachIndexed { index, manifold ->
                mc.manifold_manifold_vec_set(vec, index, manifold.manifold)
            }
            val man = mc.manifold_compose(allocate(), vec)
            return MManifold(man)
        }

        fun batchHull(ms: List<MManifold>): MManifold {
            return MManifold(mc.manifold_batch_hull(allocate(), ms.toMVec()))
        }

        fun hullPoints(points: List<Vec3>): MManifold {
            return MManifold(mc.manifold_hull_pts(allocate(), points.map { it.toManifoldVec3() }.toTypedArray(), points.size))
        }

        fun getCircularSegments(radius: Float): Int {
            return mc.manifold_get_circular_segments(radius)
        }

        fun reserveIds(n: Int): Int {
            return mc.manifold_reserve_ids(n)
        }
    }
}
