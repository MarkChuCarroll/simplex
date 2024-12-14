package org.goodmath.simplex.kcsg.poly2tri

import java.util.List

abstract class Matrix3Transform(
    var m00: Double, var m01: Double, var m02: Double,
    var m10: Double, var m11: Double, var m12: Double,
    var m20: Double, var m21: Double, var m22: Double,
): CoordinateTransform {

    override fun transform(p: Point, store: Point) {
        store.set(m00 * p.x + m01 * p.y + m02 * p.z,
            m10 * p.x + m11 * p.y + m12 * p.z,
            m20 * p.x + m21 * p.y + m22 * p.z)
    }

    override fun transform(p: Point) {
        p.set(m00 * p.x + m01 * p.y + m02 * p.z,
            m10 * p.x + m11 * p.y + m12 * p.z,
            m20 * p.x + m21 * p.y + m22 * p.z)
    }

    override fun <P : Point> transform(list: List<P>) {
        for (p in list) {
            transform(p)
        }
    }
}
