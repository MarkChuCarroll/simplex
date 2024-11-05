package org.goodmath.simplex.kcsg.poly2tri

/**
 * TriangulationUtil.java
 *
 * Copyright 2014-2014 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
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
 * @author Thomas ???, thahlen@gmail.com
 */


enum class Orientation {
    CW,CCW,Collinear
}

object TriangulationUtil {
    val EPSILON = 1e-12

    /**
     * <b>Requirement</b>:<br>
     * 1. a,b and c form a triangle.<br>
     * 2. a and d is know to be on opposite side of bc<br>
     * <pre>
     *                a
     *                +
     *               / \
     *              /   \
     *            b/     \c
     *            +-------+
     *           /    B    \
     *          /           \
     * </pre>
     * <b>Fact</b>: d has to be in area B to have a chance to be inside the circle formed by
     *  a,b and c<br>
     *  d is outside B if orient2d(a,b,d) or orient2d(c,a,d) is CW<br>
     *  This preknowledge gives us a way to optimize the incircle test
     * @param pa - triangle point, opposite d
     * @param pb - triangle point
     * @param pc - triangle point
     * @param pd - point opposite a
     * @return true if d is inside circle, false if on circle edge
     */
    fun smartIncircle(pa: TriangulationPoint,
                      pb: TriangulationPoint,
                      pc: TriangulationPoint,
                      pd: TriangulationPoint): Boolean {
        val pdx = pd.x
        val pdy = pd.y
        val adx = pa.x - pdx
        val ady = pa.y - pdy
        val bdx = pb.x - pdx
        val bdy = pb.y - pdy

        val adxbdy = adx * bdy
        val bdxady = bdx * ady;
        val oabd = adxbdy - bdxady
        if(oabd <= 0) {
            return false
        }

        val cdx = pc.x - pdx
        val cdy = pc.y - pdy

        val cdxady = cdx * ady
        val adxcdy = adx * cdy
        val ocad = cdxady - adxcdy

        if (ocad <= 0.0) {
            return false
        }

        val bdxcdy = bdx * cdy
        val cdxbdy = cdx * bdy

        val alift = adx * adx + ady * ady
        val blift = bdx * bdx + bdy * bdy
        val clift = cdx * cdx + cdy * cdy

        val det = alift * ( bdxcdy - cdxbdy ) + blift * ocad + clift * oabd

        return det > 0.0
    }

    /**
     * @param pa
     * @param pb
     * @param pc
     * @param pd
     * @return
     */
    fun inScanArea(pa: TriangulationPoint,
                   pb: TriangulationPoint,
                   pc: TriangulationPoint,
                   pd: TriangulationPoint): Boolean {
        val pdx = pd.x
        val pdy = pd.y
        val adx = pa.x - pdx
        val ady = pa.y - pdy
        val bdx = pb.x - pdx
        val bdy = pb.y - pdy

        val adxbdy = adx * bdy
        val bdxady = bdx * ady
        val oabd = adxbdy - bdxady
        if (oabd <= 0) {
            return false
        }

        val cdx = pc.x - pdx
        val cdy = pc.y - pdy

        val cdxady = cdx * ady
        val adxcdy = adx * cdy
        val ocad = cdxady - adxcdy
        if(ocad <= 0) {
            return false
        }
        return true
    }

    /**
     * Forumla to calculate signed area<br>
     * Positive if CCW<br>
     * Negative if CW<br>
     * 0 if collinear<br>
     * <pre>
     * A[P1,P2,P3]  =  (x1*y2 - y1*x2) + (x2*y3 - y2*x3) + (x3*y1 - y3*x1)
     *              =  (x1-x3)*(y2-y3) - (y1-y3)*(x2-x3)
     * </pre>
     */
    fun orient2d(pa: TriangulationPoint,
                 pb: TriangulationPoint,
                 pc: TriangulationPoint): Orientation {
        val detleft = (pa.x - pc.x) * (pb.y - pc.y)
        val detright = (pa.y - pc.y) * (pb.x - pc.x)
        val value = detleft - detright
        if (value > -EPSILON && value < EPSILON) {
            return Orientation.Collinear;
        } else if (value > 0) {
            return Orientation.CCW
        }
        return Orientation.CW
    }
}
