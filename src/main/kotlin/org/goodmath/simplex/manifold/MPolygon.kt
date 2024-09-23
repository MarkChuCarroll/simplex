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
import org.goodmath.simplex.manifold.ManifoldC.ManifoldVec2
import org.jetbrains.kotlin.wasm.ir.WasmExport

val mc: ManifoldC = ManifoldC.INSTANCE

open class MType(val ptr: Pointer) {
    open fun close() {
        (ptr as Memory).close()
    }
}


class MPolygon(val msPolygon: ManifoldSimplePolygon): MType(msPolygon) {
    constructor(points: List<ManifoldVec2>): this(mc.manifold_simple_polygon(
        allocate(),
        points.toTypedArray(),
        points.size)) {
    }

    fun length(): Int {
        return mc.manifold_simple_polygon_length(this.msPolygon)
    }

     fun getPoint(idx: Int): ManifoldVec2 {
         return mc.manifold_simple_polygon_get_point(this.msPolygon, idx)
     }

    fun crossSection(fr: ManifoldFillRule): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_of_simple_polygon(MCrossSection.allocate(), msPolygon, fr))
    }

    fun hull(): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_hull_simple_polygon(MCrossSection.allocate(), msPolygon))
    }

    override fun close() {
        mc.manifold_destruct_simple_polygon(ptr)
        super.close()
    }

    companion object {
        fun allocate(): Pointer {
            return Memory(mc.manifold_simple_polygon_size())
        }
    }

}

class MPolygons(val msPolygons: ManifoldPolygons): MType(msPolygons) {
    constructor(ps: List<MPolygon>): this(mc.manifold_polygons(
        allocate(),
        ps.map { it.msPolygon }.toTypedArray(), ps.size)) {
    }

    fun asList(): List<MPolygon> {
        val result = ArrayList<MPolygon>()
        for (i in 0..<mc.manifold_polygons_length(msPolygons)) {
            result.add(MPolygon(mc.manifold_polygons_get_simple(MPolygon.allocate(), msPolygons, i)))
        }
        return result
    }

    fun lengthOfPolygon(idx: Int): Int {
        return mc.manifold_polygons_simple_length(msPolygons, idx)
    }

    fun crossSection(fr: ManifoldFillRule): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_of_polygons(MCrossSection.allocate(),
            msPolygons, fr
            ))
    }

    fun hull(): MCrossSection {
        return MCrossSection(mc.manifold_cross_section_hull_polygons(MCrossSection.allocate(), msPolygons))
    }

    fun length(): Int {
        return mc.manifold_polygons_length(this.msPolygons)
    }

    fun getSimple(idx: Int): MPolygon {
        val mem = Memory(mc.manifold_polygons_size())
        val poly= mc.manifold_polygons_get_simple(mem, ptr, idx)
        return MPolygon(poly)
    }

    fun getPoint(polyIdx: Int, pointIdx: Int): ManifoldVec2 {
        return mc.manifold_polygons_get_point(ptr, polyIdx, pointIdx)
    }

    override fun close() {
        mc.manifold_destruct_polygons(ptr)
        super.close()
    }

    companion object {
        fun allocate(): Pointer {
            return Memory(mc.manifold_polygons_size())
        }
    }
}
