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

import org.goodmath.simplex.manifold.ManifoldC.ManifoldVec2
import org.goodmath.simplex.manifold.ManifoldC.ManifoldVec3

data class Vec2(val x: Float, val y: Float) {
    fun toManifoldVec2(): ManifoldVec2 = ManifoldVec2(x, y)
}

fun ManifoldVec2.toVec2(): Vec2 {
    return Vec2(x, y)
}

data class Vec3(val x: Float, val y: Float, val z: Float) {
    fun toManifoldVec3(): ManifoldC.ManifoldVec3 = ManifoldC.ManifoldVec3(x, y, z)
}

fun ManifoldVec3.toVec3(): Vec3 {
    return Vec3(x, y, z)
}

data class Vec4(val x: Float, val y: Float, val z: Float, val w: Float) {
    fun toManifoldVec4(): ManifoldC.ManifoldVec4 = ManifoldC.ManifoldVec4(x, y, z, w)
}


