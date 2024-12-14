package org.goodmath.simplex.kcsg.poly2tri

import java.util.List

class NoTransform: CoordinateTransform {
    override fun transform(p: Point, store: Point) {
        store.set(p.x, p.y, p.z)
    }

    override fun transform(p: Point) {
    }

    override fun <P : Point> transform(list: List<P>) {
    }
}
