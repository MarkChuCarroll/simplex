package org.goodmath.simplex.vvecmath

    /*
 * Copyright 2017-2019 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * If you use this software for scientific research then please cite the following publication(s):
 *
 * M. Hoffer, C. Poliwoda, & G. Wittum. (2013). Visual reflection library:
 * a framework for declarative GUI programming on the Java platform.
 * Computing and Visualization in Science, 2013, 16(4),
 * 181–192. http://doi.org/10.1007/s00791-014-0230-y
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Michael Hoffer <info@michaelhoffer.de>.
 */


    /**
     *
     * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
     */
class Vector3dImpl(var x: Double, var y: Double, var z: Double): Vector3d, Cloneable{

        /**
         * Creates a new vector with specified {@code x}, {@code y} and
         * {@code z = 0}.
         *
         * @param x x value
         * @param y y value
         */
        constructor(x: Double, y: Double): this(x, y, 0.0)

        public override fun clone(): Vector3d {
            return Vector3dImpl(x, y, z)
        }

        fun set(xyz: List<Double>): Vector3d {
            if(xyz.size > 3) {
                throw IllegalArgumentException(
                        "Wrong number of components.  expected <= 3, got: ${xyz.size}")
            }

            for (i in xyz.indices) {
                set(i, xyz[i])
            }

            return this;
        }

        fun set(i: Int,  value: Double): Vector3d {
            when (i) {
                0 -> x = value
                1 -> y = value
                2 -> z = value
                else -> throw new RuntimeException("Illegal index: " + i);
            }
            return this
        }

        override fun toString(): String {
            return VectorUtilInternal.toString(this)
        }

        override fun equals(obj: Any?): Boolean {
            return VectorUtilInternal.equals(this, obj)
        }

        override fun hashCode(): Int {
            return VectorUtilInternal.getHashCode(this)
        }

    }

