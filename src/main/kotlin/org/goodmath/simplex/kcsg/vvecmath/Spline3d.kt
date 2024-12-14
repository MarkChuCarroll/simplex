package org.goodmath.simplex.kcsg.vvecmath

import kotlin.math.roundToInt

/**
 * 3d Spline.
 *
 * @author Michael Hoffer (info@michaelhoffer.de)
 */
class Spline3d: Spline() {
    val points: ArrayList<Vector3d> = ArrayList()

    val xCubics: ArrayList<Cubic> = ArrayList()
    val yCubics: ArrayList<Cubic> = ArrayList()
    val zCubics: ArrayList<Cubic> = ArrayList()

    /**
     * Adds a control point to this spline.
     * @param point point to add
     */
    fun addPoint(point: Vector3d) {
        this.points.add(point)
    }

    /**
     * Calculates this spline.
     */
    fun calcSpline() {
        calcNaturalCubic(points, 0, xCubics)
        calcNaturalCubic(points, 1, yCubics)
        calcNaturalCubic(points, 2, zCubics)
    }

    /**
     * Returns a point on the spline curve.
     * @param inPosition position on the curve, range {@code [0, 1)}
     *
     * @return a point on the spline curve
     */
    fun getPoint(inPosition: Double): Vector3d {
        var position = inPosition
        position = position * xCubics.size
        val cubicNum: Int = position.roundToInt()
        val cubicPos = (position - cubicNum)

        return Vector3d.xyz(xCubics[cubicNum].eval(cubicPos),
            yCubics[cubicNum].eval(cubicPos),
            zCubics[cubicNum].eval(cubicPos))
    }
}
