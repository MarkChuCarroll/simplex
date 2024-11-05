package org.goodmath.simplex.kcsg.poly2tri

import kotlin.random.Random

object PointGenerator {
    fun uniformDistribution(n: Int, scale: Double):  List<TriangulationPoint> {
        val points = ArrayList<TriangulationPoint>()
        for(i in 0 until n) {
            points.add(TPoint(scale*(0.5 - Random.nextDouble()), scale*(0.5 - Random.nextDouble())))
        }
        return points
    }

    fun uniformGrid(n: Int, scale: Double): List<TriangulationPoint> {
        var x = 0.0
        val size = scale/n
        val halfScale = 0.5*scale

        val points = ArrayList<TriangulationPoint>()
        for(i in 0 until n+1) {
            x =  halfScale - i*size
            for(j in 0 until n+1) {
                points.add(TPoint(x, halfScale - j*size))
            }
        }
        return points
    }
}
