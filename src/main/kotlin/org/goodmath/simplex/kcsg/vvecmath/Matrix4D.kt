package org.goodmath.simplex.kcsg.vvecmath

class Matrix4d() {
    var m00: Double = 0.0
    var m01: Double = 0.0
    var m02: Double = 0.0
    var m03: Double = 0.0
    var m10: Double = 0.0
    var m11: Double = 0.0
    var m12: Double = 0.0
    var m13: Double = 0.0
    var m20: Double = 0.0
    var m21: Double = 0.0
    var m22: Double = 0.0
    var m23: Double = 0.0
    var m30: Double = 0.0
    var m31: Double = 0.0
    var m32: Double = 0.0
    var m33: Double = 0.0

    constructor(v: Array<Double>): this() {
        if (v.size != 16) {
            throw InvalidParameterException("Matrix4d requires a vector of 16 elements, but saw ${v.size}")
        }
        this.m00 = v[0]
        this.m01 = v[1]
        this.m02 = v[2]
        this.m03 = v[3]
        this.m10 = v[4]
        this.m11 = v[5]
        this.m12 = v[6]
        this.m13 = v[7]
        this.m20 = v[8]
        this.m21 = v[9]
        this.m22 = v[10]
        this.m23 = v[11]
        this.m30 = v[12]
        this.m31 = v[13]
        this.m32 = v[14]
        this.m33 = v[15]
    }

    constructor(v: List<Double>): this() {
        if (v.size != 16) {
            throw InvalidParameterException("Matrix4d requires a vector of 16 elements, but saw ${v.size}")
        }
        this.m00 = v[0]
        this.m01 = v[1]
        this.m02 = v[2]
        this.m03 = v[3]
        this.m10 = v[4]
        this.m11 = v[5]
        this.m12 = v[6]
        this.m13 = v[7]
        this.m20 = v[8]
        this.m21 = v[9]
        this.m22 = v[10]
        this.m23 = v[11]
        this.m30 = v[12]
        this.m31 = v[13]
        this.m32 = v[14]
        this.m33 = v[15]
    }

    fun copy(): Matrix4d {
        val result = Matrix4d()
        result.m00 = m00
        result.m01 = m01
        result.m02 = m02
        result.m03 = m03
        result.m10 = m10
        result.m11 = m11
        result.m12 = m12
        result.m13 = m13
        result.m20 = m20
        result.m21 = m21
        result.m22 = m22
        result.m23 = m23
        result.m30 = m30
        result.m31 = m31
        result.m32 = m32
        result.m33 = m33
        return result
    }


    /**
     * Multiplies this matrix with the specified matrix.
     *
     * @param m matrix to multiply
     */
    fun mul(m: Matrix4d) {
        val n00 = this.m00 * m.m00 + this.m01 * m.m10 + this.m02 * m.m20 + this.m03 * m.m30
        val n01 = this.m00 * m.m01 + this.m01 * m.m11 + this.m02 * m.m21 + this.m03 * m.m31
        val n02 = this.m00 * m.m02 + this.m01 * m.m12 + this.m02 * m.m22 + this.m03 * m.m32
        val n03 = this.m00 * m.m03 + this.m01 * m.m13 + this.m02 * m.m23 + this.m03 * m.m33
        val n10 = this.m10 * m.m00 + this.m11 * m.m10 + this.m12 * m.m20 + this.m13 * m.m30
        val n11 = this.m10 * m.m01 + this.m11 * m.m11 + this.m12 * m.m21 + this.m13 * m.m31
        val n12 = this.m10 * m.m02 + this.m11 * m.m12 + this.m12 * m.m22 + this.m13 * m.m32
        val n13 = this.m10 * m.m03 + this.m11 * m.m13 + this.m12 * m.m23 + this.m13 * m.m33
        val n20 = this.m20 * m.m00 + this.m21 * m.m10 + this.m22 * m.m20 + this.m23 * m.m30
        val n21 = this.m20 * m.m01 + this.m21 * m.m11 + this.m22 * m.m21 + this.m23 * m.m31
        val n22 = this.m20 * m.m02 + this.m21 * m.m12 + this.m22 * m.m22 + this.m23 * m.m32
        val n23 = this.m20 * m.m03 + this.m21 * m.m13 + this.m22 * m.m23 + this.m23 * m.m33
        val n30 = this.m30 * m.m00 + this.m31 * m.m10 + this.m32 * m.m20 + this.m33 * m.m30
        val n31 = this.m30 * m.m01 + this.m31 * m.m11 + this.m32 * m.m21 + this.m33 * m.m31
        val n32 = this.m30 * m.m02 + this.m31 * m.m12 + this.m32 * m.m22 + this.m33 * m.m32
        val n33 = this.m30 * m.m03 + this.m31 * m.m13 + this.m32 * m.m23 + this.m33 * m.m33

        this.m00 = n00
        this.m01 = n01
        this.m02 = n02
        this.m03 = n03

        this.m10 = n10
        this.m11 = n11
        this.m12 = n12
        this.m13 = n13

        this.m20 = n20
        this.m21 = n21
        this.m22 = n22
        this.m23 = n23

        this.m30 = n30
        this.m31 = n31
        this.m32 = n32
        this.m33 = n33
    }

    operator fun times(m: Matrix4d): Matrix4d {
        val result = copy()
        result.mul(m)
        return result
    }

    fun determinant(): Double {
        var det = 0.0

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
}
