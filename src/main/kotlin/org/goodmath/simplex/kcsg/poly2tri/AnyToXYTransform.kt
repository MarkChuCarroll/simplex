package org.goodmath.simplex.kcsg.poly2tri

/* Poly2Tri
 * Copyright (c) 2009-2010, Poly2Tri Contributors
 * http://code.google.com/p/poly2tri/
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Poly2Tri nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * A transform that aligns given source normal with the XY plane normal [0,0,1]
 *
 * @author thahlen@gmail.com
 */

class AnyToXYTransform(val nx: Double, val ny: Double, val nz: Double): Matrix3Transform() {
    override var m00: Double = 0.0
    override var m01: Double = 0.0
    override var m02: Double = 0.0
    override var m10: Double = 0.0
    override var m11: Double = 0.0
    override var m12: Double = 0.0
    override var m20: Double = 0.0
    override var m21: Double = 0.0
    override var m22: Double = 0.0
    /**
     * Assumes source normal is normalized
     */
     init {
        setSourceNormal(nx, ny, nz)
    }

    /**
     * Assumes source normal is normalized
     *
     * @param nx
     * @param ny
     * @param nz
     */
    fun setSourceNormal(nx: Double, ny: Double, nz: Double) {
        val vx = -ny
        val vy = nx
        val c = nz

        val h = (1-c)/(1-c*c)
        val hvx = h*vx
        val f = if (c < 0) {
            -c
        } else {
            c
        }

        if (f < 1.0 - 1.0E-4) {
            m00=c + hvx*vx
            m01=hvx*vy
            m02=-vy
            m10=hvx*vy
            m11=c + h*vy*vy
            m12=vx
            m20=vy
            m21=-vx
            m22=c
        } else {
            // if "from" and "to" vectors are nearly parallel
            m00=1.0
            m01=0.0
            m02=0.0
            m10=0.0
            m11=1.0
            m12=0.0
            m20=0.0
            m21=0.0
            m22 = if (c > 0.0) {
                1.0
            } else {
                -1.0
            }
        }
    }
}
