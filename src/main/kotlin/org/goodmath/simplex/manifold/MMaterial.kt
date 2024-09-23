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

class MMaterial(val material: ManifoldMaterial): MType(material) {
    constructor(): this(mc.manifold_material(allocate()))

    fun setRoughness(roughness: Float) {
        mc.manifold_material_set_roughness(material, roughness)
    }

    fun setMetalness(metalness: Float) {
        mc.manifold_material_set_metalness(material, metalness)
    }

    fun setColor(color: Vec4) {
        mc.manifold_material_set_color(material, color.toManifoldVec4())
    }

    fun setVertColor(vertColor: Vec4, nVert: Int) {
        mc.manifold_material_set_vert_color(material, vertColor.toManifoldVec4(), nVert)
    }

    override fun close() {
        mc.manifold_destruct_material(material)
        super.close()
    }

    companion object {
        fun allocate(): Pointer {
            return Memory(mc.manifold_material_size())
        }
    }

}
