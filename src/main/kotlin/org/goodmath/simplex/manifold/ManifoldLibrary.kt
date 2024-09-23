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

/**
 * A Kotlin wrapper for the Manifold CAD library.
 *
 * This is build on the standard Manifold bindings. Those are made available in Java
 * using JNA in ManifoldC.kt. But since they're pretty awkward, the rest of this
 * package is built to provide a decent set of Kotlin bindings.
 *
 * This class wraps a couple of configuration constants used by the library
 * as a whole.
 */
class ManifoldLibrary {
    fun setMinCircularAngle(degrees: Float) {
        mc.manifold_set_min_circular_angle(degrees)
    }

    fun setMinCircularEdgeLength(length: Float) {
        mc.manifold_set_min_circular_edge_length(length)
    }

    fun setCircularSegments(number: Int) {
        mc.manifold_set_circular_segments(number)
    }


}
