package org.goodmath.simplex.kcsg.poly2tri

abstract class Matrix3Transform: CoordinateTransform {
    abstract var m00: Double
    abstract var m01: Double
    abstract var m02: Double
    abstract var m10: Double
    abstract var m11: Double
    abstract var m12: Double
    abstract var m20: Double
    abstract var m21: Double
    abstract var m22: Double

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

    override fun<T: Point> transform(list: List<T>) {
        for (p in list) {
            transform(p)
        }
    }
}

class NoTransform: CoordinateTransform {
    override fun transform(p: Point, store: Point) {
        store.set(p.x, p.y, p.z)
    }

    override fun transform(p: Point) {
    }

    override fun<T: Point> transform(list: List<T>) {
    }
}
