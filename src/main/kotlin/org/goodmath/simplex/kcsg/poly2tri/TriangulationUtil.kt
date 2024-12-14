package org.goodmath.simplex.kcsg.poly2tri

import kotlin.math.absoluteValue

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
object TriangulationUtil {
    const val EPSILON = 1e-12

    /**
     * <b>Requirement</b>:<br>
     * 1. a,b and c form a triangle.<br>
     * 2. a and d is known to be on opposite side of bc<br>
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
     *  d is outside B if <code>orient2d(a,b,d)</code> or <code>orient2d(c,a,d)</code> is CW<br>
     *  This knowledge gives us a way to optimize the in-circle test
     * @param pa - triangle point, opposite d
     * @param pb - triangle point
     * @param pc - triangle point
     * @param pd - point opposite a
     * @return true if d is inside circle, false if on circle edge
     */
    fun smartInCircle(pa: TriangulationPoint,
                      pb: TriangulationPoint,
                      pc: TriangulationPoint,
                      pd: TriangulationPoint): Boolean {
        val pdx = pd.x
        val pdy = pd.y
        val adx = pa.x - pdx
        val ady = pa.y - pdy
        val bdx = pb.x - pdx
        val bdy = pb.y - pdy
        val oabd = (adx * bdy) - (bdx * ady)
        if( oabd <= 0.0 ) {
            return false
        }
        val cdx = pc.x - pdx
        val cdy = pc.y - pdy
        val ocad = (cdx * ady) - (adx * cdy)
        if( ocad <= 0 ) {
            return false
        }
        val det = (adx * adx + ady * ady) * ((bdx * cdy) - (cdx * bdy)) + (bdx * bdx + bdy * bdy) * ocad + (cdx * cdx + cdy * cdy) * oabd
        return det > 0.0
    }

    /**
     * @see smartInCircle
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
        if (((pa.x - pd.x) * (pb.y - pd.y)) - ((pb.x - pd.x) * (pa.y - pd.y)) <= 0) {
            return false
        }

        if ((pc.x - pd.x) * (pa.y - pd.y) - (pa.x - pd.x) * (pc.y - pd.y) <= 0) {
            return false
        }
        return true
    }

    /**
     * Formula to calculate signed area:
     *
     * Positive if CCW,
     * Negative if CW,
     * 0 if collinear.
     *
     * <pre>
     * A[P1,P2,P3]  =  (x1*y2 - y1*x2) + (x2*y3 - y2*x3) + (x3*y1 - y3*x1)
     *              =  (x1-x3)*(y2-y3) - (y1-y3)*(x2-x3)
     * </pre>
     */
    fun orient2d(pa: TriangulationPoint,
                 pb: TriangulationPoint,
                 pc: TriangulationPoint): Orientation {
        val detLeft = (pa.x - pc.x) * (pb.y - pc.y)
        val detRight = (pa.y - pc.y) * (pb.x - pc.x)
        val value = detLeft - detRight
        if (value.absoluteValue < EPSILON) {
            return Orientation.Collinear
        } else if (value > 0.0) {
            return Orientation.CCW
        }
        return Orientation.CW
    }

    enum class Orientation {
        CW,
        CCW,
        Collinear
    }
}
