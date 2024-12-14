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
abstract class Point {
     abstract val x: Double
     abstract val y: Double
    abstract val z: Double

    abstract fun set(x: Double, y: Double, z: Double)

    fun calculateHashCode(x: Double, y: Double, z: Double): Int {
        var result = 17

        val a = x.toRawBits()
        result += 31 * result + (a xor (a ushr 32)).toInt()

        val b = y.toRawBits()
        result += 31 * result + (b xor (b ushr 32)).toInt()

        val c = z.toRawBits()
        result += 31 * result +  (c xor (c ushr 32)).toInt()

        return result
    }
}


object PointGenerator {
    fun uniformDistribution(n: Int, scale: Double): List<TriangulationPoint> {
        val points =  ArrayList<TriangulationPoint>()
        for(i in 0 until n) {
            points.add(TPoint(scale*(0.5 - Math.random()), scale*(0.5 - Math.random())))
        }
        return points
    }

    fun uniformGrid(n: Int, scale: Double): List<TriangulationPoint> {
        var x = 0.0
        var size = scale/n.toDouble()
        val halfScale = 0.5 * scale

        val points = ArrayList<TriangulationPoint>()
        for (i in 0 until n) {
            x =  halfScale - i*size
            for(j in 0 until n+1) {
                points.add(TPoint(x, halfScale - j*size))
            }
        }
        return points
    }
}
