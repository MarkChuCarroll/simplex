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
 * @author Michael Hoffer (info@michaelhoffer.de)
 */
class Matrix4d(val slots: Array<Double>) {
    var m00: Double = slots[0]
    var m01: Double = slots[1]
    var m02: Double = slots[2]
    var m03: Double = slots[3]
    var m10: Double = slots[4]
    var m11: Double = slots[5]
    var m12: Double = slots[6]
    var m13: Double = slots[7]
    var m20: Double = slots[8]
    var m21: Double = slots[9]
    var m22: Double = slots[10]
    var m23: Double = slots[11]
    var m30: Double = slots[12]
    var m31: Double = slots[13]
    var m32: Double = slots[14]
    var m33: Double = slots[15]


    constructor(): this(Array<Double>(16) { 0.0 })

    fun set(values: List<Double>) {
        m00 = values[0]
        m01 = values[1]
        m02 = values[2]
        m03 = values[3]

        m10 = values[4]
        m11 = values[5]
        m12 = values[6]
        m13 = values[7]

        m20 = values[8]
        m21 = values[9]
        m22 = values[10]
        m23 = values[11]

        m30 = values[12]
        m31 = values[13]
        m32 = values[14]
        m33 = values[15]
    }

    fun get(vals: Array<Double>?): Array<Double> {
        val values = vals ?: Array(16) { 0.0 }
        values[0] = m00
        values[1] = m01
        values[2] = m02
        values[3] = m03

        values[4] = m10
        values[5] = m11
        values[6] = m12
        values[7] = m13

        values[8] = m20
        values[9] = m21
        values[10] = m22
        values[11] = m23

        values[12] = m30
        values[13] = m31
        values[14] = m32
        values[15] = m33

        return values
    }

    /**
     * Multiplies this matrix with the specified matrix.
     *
     * @param m matrix to multiply
     */
    fun mul(m: Matrix4d) {
        val results = listOf(
            m00 * m.m00 + m01 * m.m10 + m02 * m.m20 + m03 * m.m30,
            m00 * m.m01 + m01 * m.m11 + m02 * m.m21 + m03 * m.m31,
            m00 * m.m02 + m01 * m.m12 + m02 * m.m22 + m03 * m.m32,
            m00 * m.m03 + m01 * m.m13 + m02 * m.m23 + m03 * m.m33,
            m10 * m.m00 + m11 * m.m10 + m12 * m.m20 + m13 * m.m30,
            m10 * m.m01 + m11 * m.m11 + m12 * m.m21 + m13 * m.m31,
            m10 * m.m02 + m11 * m.m12 + m12 * m.m22 + m13 * m.m32,
            m10 * m.m03 + m11 * m.m13 + m12 * m.m23 + m13 * m.m33,
            m20 * m.m00 + m21 * m.m10 + m22 * m.m20 + m23 * m.m30,
            m20 * m.m01 + m21 * m.m11 + m22 * m.m21 + m23 * m.m31,
            m20 * m.m02 + m21 * m.m12 + m22 * m.m22 + m23 * m.m32,
            m20 * m.m03 + m21 * m.m13 + m22 * m.m23 + m23 * m.m33,
            m30 * m.m00 + m31 * m.m10 + m32 * m.m20 + m33 * m.m30,
            m30 * m.m01 + m31 * m.m11 + m32 * m.m21 + m33 * m.m31,
            m30 * m.m02 + m31 * m.m12 + m32 * m.m22 + m33 * m.m32,
            m30 * m.m03 + m31 * m.m13 + m32 * m.m23 + m33 * m.m33)

        set(results)
    }

    fun determinant(): Double {
        var det: Double = 0.0
        det = m00 * (m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32
                - m13 * m22 * m31 - m11 * m23 * m32 - m12 * m21 * m33)
        det -= m01 * (m10 * m22 * m33 + m12 * m23 * m30 + m13 * m20 * m32
                - m13 * m22 * m30 - m10 * m23 * m32 - m12 * m20 * m33)
        det += m02 * (m10 * m21 * m33 + m11 * m23 * m30 + m13 * m20 * m31
                - m13 * m21 * m30 - m10 * m23 * m31 - m11 * m20 * m33)
        det -= m03 * (m10 * m21 * m32 + m11 * m22 * m30 + m12 * m20 * m31
                - m12 * m21 * m30 - m10 * m22 * m31 - m11 * m20 * m32)
        return det
    }

    companion object {
        fun identity() = Matrix4d(arrayOf(
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 1.0))
    }
}
