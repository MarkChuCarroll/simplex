package org.goodmath.simplex.kcsg.poly2tri

interface CoordinateTransform {
    fun transform(p: Point, store: Point)
    fun transform(p: Point)
    fun<T: Point> transform(list: List<T>)
}
