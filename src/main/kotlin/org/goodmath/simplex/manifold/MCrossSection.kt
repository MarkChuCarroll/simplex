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


fun List<MCrossSection>.toCrossSectionVec(): ManifoldCrossSectionVec {
    val csv = mc.manifold_cross_section_vec(
        Memory(mc.manifold_cross_section_vec_size()),
        this.size
    )
    this.forEachIndexed { idx, cs ->
        mc.manifold_cross_section_vec_set(csv, idx, cs.cross)
    }
    return csv
}

fun ManifoldCrossSectionVec.toCrossSectionList(): List<MCrossSection> {
    val result = ArrayList<MCrossSection>()
    for (i in 0..<mc.manifold_cross_section_vec_length(this)) {
        result.add(MCrossSection(mc.manifold_cross_section_vec_get(MCrossSection.allocate(), this, i)))
    }
    mc.manifold_destruct_cross_section_vec(this)
    (this as Memory).close()
    return result
}


class MCrossSection(val cross: ManifoldCrossSection): MType(cross) {

    fun copy(): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_copy(allocate(), cross))
    }

    fun decompose(): List<MCrossSection> {
        val targetMem = Memory(mc.manifold_cross_section_vec_size())
        val csv = mc.manifold_cross_section_decompose(targetMem, cross)
        return csv.toCrossSectionList()
    }

    fun boolean(b: MCrossSection, op: ManifoldOpType): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_boolean(allocate(), cross, b.cross, op))
    }

    fun union(b: MCrossSection): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_union(allocate(), cross, b.cross))
    }

    fun difference(b: MCrossSection): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_difference(allocate(), cross, b.cross))
    }

    fun intersection(b: MCrossSection): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_intersection(allocate(), cross, b.cross))
    }

    fun hull(): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_hull(allocate(), cross))
    }

    fun translate(v: Vec2): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_translate(allocate(), cross, v.x, v.y))
    }

    fun rotate(degrees: Float): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_rotate(allocate(), cross, degrees))
    }

    fun scale(v: Vec2): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_scale(allocate(), cross, v.x, v.y))
    }

    fun mirror(axX: Float, axY: Float): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_mirror(allocate(), cross, axX, axY))
    }

    fun transform(a: Vec2, b: Vec2, c: Vec2): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_transform(
            allocate(), cross, a.x, a.y, b.x, b.y, c.x, c.y)
        )
    }

    fun warp(warp: ManifoldC.CrossSectionWarpCallback): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_warp(allocate(), cross, warp))
    }

    fun warpWithContext(warp: ManifoldC.CrossSectionWarpContextCallback, context: Pointer): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_warp_context(allocate(), cross, warp, context))
    }

    fun simplify(epsilon: Float): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_simplify(allocate(), cross, epsilon))
    }

    fun offset(delta: Double, jt: ManifoldJoinType, miterLimit: Double, circularSegments: Int): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_offset(allocate(), cross, delta, jt, miterLimit, circularSegments))
    }

    fun area(): Double {
        return mc.manifold_cross_section_area(cross)
    }

    fun numVert(): Int  {
        return mc.manifold_cross_section_num_vert(cross)
    }

    fun numContour(): Int {
        return mc.manifold_cross_section_num_contour(cross)
    }

    fun isEmpty(): Boolean {
        return mc.manifold_cross_section_is_empty(cross) != 0
    }

    fun bounds(): MRect {
        return MRect(mc.manifold_cross_section_bounds(MRect.allocate(), cross))
    }

    fun toPolygons(): List<MPolygon> {
        return MPolygons(mc.manifold_cross_section_to_polygons(MPolygons.allocate(), cross)).asList()
    }

    override fun close() {
        mc.manifold_destruct_cross_section(cross)
        super.close()
    }

    companion object {
        fun allocate(): Pointer = Memory(mc.manifold_cross_section_size())

        fun batchBoolean(csl: List<MCrossSection>, op: ManifoldOpType): MCrossSection {
            val csv = csl.toCrossSectionVec()
            return MCrossSection(mc.manifold_cross_section_batch_boolean(allocate(), csv, op))
        }

        fun batchHull(csl: List<MCrossSection>, op: ManifoldOpType): MCrossSection {
            val csv = csl.toCrossSectionVec()
            return MCrossSection(mc.manifold_cross_section_batch_hull(allocate(), csv))
        }

        fun empty(): MCrossSection {
            return MCrossSection(mc.manifold_cross_section_empty(allocate()))
        }

        fun square(x: Float, y: Float, center: Boolean): MCrossSection {
            return MCrossSection(mc.manifold_cross_section_square(allocate(), x, y, center.toInt()))
        }

        fun circle(radius: Float, segments: Int): MCrossSection {
            return MCrossSection(mc.manifold_cross_section_circle(allocate(), radius, segments))
        }

        fun compose(css: List<MCrossSection>): MCrossSection {
            val csPointers = css.map { it.cross }
            val csv = mc.manifold_cross_section_vec(
                Memory(mc.manifold_cross_section_vec_size()),
                css.size
            )
            csPointers.forEachIndexed { idx, csPointer ->
                mc.manifold_cross_section_vec_set(csv, idx, csPointer)
            }
            return MCrossSection(mc.manifold_cross_section_compose(allocate(), csv))
        }

    }
}
