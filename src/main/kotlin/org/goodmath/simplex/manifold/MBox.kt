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
import com.sun.org.apache.xpath.internal.operations.Bool

class MBox(val box: ManifoldBox): MType(box) {
    constructor(
        one: Vec3, two: Vec3

    ) : this(
        ManifoldC.INSTANCE.manifold_box(
            allocate(), one.x, one.y, one.z, two.x, two.y, two.z
        )
    )

    fun min(): ManifoldC.ManifoldVec3 {
        return ManifoldC.INSTANCE.manifold_box_min(box)
    }

    fun max(): ManifoldC.ManifoldVec3 {
        return ManifoldC.INSTANCE.manifold_box_max(box)
    }

    fun dimensions(): ManifoldC.ManifoldVec3 {
        return ManifoldC.INSTANCE.manifold_box_dimensions(box)
    }

    fun center(): ManifoldC.ManifoldVec3 {
        return ManifoldC.INSTANCE.manifold_box_center(box)
    }

    fun containsPoint(v: Vec3): Boolean {
        return ManifoldC.INSTANCE.manifold_box_contains_pt(box, v.x, v.y, v.z) != 0
    }

    fun containsBox(b: MBox): Boolean {
        return ManifoldC.INSTANCE.manifold_box_contains_box(box, b.box) != 0
    }

    fun includePoint(point: Vec3) {
        ManifoldC.INSTANCE.manifold_box_include_pt(box, point.x, point.y, point.z)
    }

    fun union(b: MBox): MBox {
        val mem = allocate()
        return MBox(ManifoldC.INSTANCE.manifold_box_union(mem, box, b.box))
    }

    fun transform(one: Vec3, two: Vec3, three: Vec3, four: Vec3): MBox {
        return MBox(
            ManifoldC.INSTANCE.manifold_box_transform(
                allocate(), box,
                one.x, one.y, one.z,
                two.x, two.y, two.z,
                three.x, three.y, three.z,
                four.x, four.y, four.z
            )
        )
    }

    fun translate(offset: Vec3): MBox {
        return MBox(
            ManifoldC.INSTANCE.manifold_box_translate(
                allocate(), box, offset.x, offset.y, offset.z
            )
        )
    }

    fun mul(pt: Vec3): MBox {
        return MBox(ManifoldC.INSTANCE.manifold_box_mul(allocate(), box, pt.x, pt.y, pt.z))
    }

    fun overlaps_pt(pt: Vec3): Boolean {
        return ManifoldC.INSTANCE.manifold_box_does_overlap_pt(box, pt.x, pt.y, pt.z) != 0
    }

    fun overlaps_box(b: MBox): Boolean {
        return ManifoldC.INSTANCE.manifold_box_does_overlap_box(box, b.box) != 0
    }

    fun is_finite(): Boolean {
        return ManifoldC.INSTANCE.manifold_box_is_finite(box) != 0
    }

    override fun close() {
        ManifoldC.INSTANCE.manifold_destruct_box(box)
        super.close()
    }

    companion object {
        fun allocate(): Pointer {
            return Memory(ManifoldC.INSTANCE.manifold_box_size())
        }
    }
}
