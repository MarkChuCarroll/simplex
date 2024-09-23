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

class MRect(val rect: ManifoldRect): MType(rect) {
    constructor(a: Vec2, b: Vec2): this(
        mc.manifold_rect(allocate(), a.x, a.y, b.x, b.y)
    )

    fun min(): Vec2 {
        return mc.manifold_rect_min(rect).toVec2()
    }

    fun max(): Vec2 {
        return mc.manifold_rect_max(rect).toVec2()
    }

    fun dimensions(): Vec2 {
        return mc.manifold_rect_dimensions(rect).toVec2()
    }

    fun center(): Vec2 {
        return mc.manifold_rect_center(rect).toVec2()
    }

    fun scale(): Float {
        return mc.manifold_rect_scale(rect)
    }

    fun containsPoint(point: Vec2): Boolean {
        return mc.manifold_rect_contains_pt(rect, point.x, point.y) != 0
    }

    fun containsRect(other: MRect): Boolean {
        return mc.manifold_rect_contains_rect(rect, other.rect) != 0
    }

    fun includePoint(point: Vec2) {
        mc.manifold_rect_include_pt(rect, point.x, point.y)
    }

    fun union(other: MRect): MRect {
        return MRect(mc.manifold_rect_union(allocate(), rect, other.rect))
    }

    fun transform(a: Vec2, b: Vec2, c: Vec2): MRect {
        return MRect(mc.manifold_rect_transform(allocate(), rect, a.x, a.y, b.x, b.y, c.x, c.y))
    }

    fun translate(offset: Vec2): MRect {
        return MRect(mc.manifold_rect_translate(allocate(), rect, offset.x, offset.y))
    }

    fun mult(x: Float, y: Float): MRect {
        return MRect(mc.manifold_rect_mul(allocate(), rect, x, y))
    }

    fun overlaps(other: MRect): Boolean {
        return mc.manifold_rect_does_overlap_rect(rect, other.rect) != 0
    }

    fun is_empty(): Boolean {
        return mc.manifold_rect_is_empty(rect) != 0
    }

    fun is_finite(): Boolean {
        return mc.manifold_rect_is_finite(rect) != 0
    }

    override fun close() {
        mc.manifold_destruct_rect(rect)
        super.close()
    }

    companion object {
        fun allocate(): Pointer {
            return Memory(mc.manifold_rect_size())
        }
    }

}
