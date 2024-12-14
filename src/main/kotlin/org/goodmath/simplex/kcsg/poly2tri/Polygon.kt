package org.goodmath.simplex.kcsg.poly2tri

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


class PolygonPoint(x: Double, y: Double, z: Double = 0.0): TPoint(x, y, z) {
    var next: PolygonPoint? = null
    var previous: PolygonPoint? = null
}


class Polygon(points: ArrayList<PolygonPoint>): Triangulatable {
    override val points: ArrayList<TriangulationPoint> = ArrayList<TriangulationPoint>()
    val steinerPoints: ArrayList<TriangulationPoint> = ArrayList<TriangulationPoint>()
    val holes: ArrayList<Polygon> = ArrayList<Polygon>()

    override val triangles: ArrayList<DelaunayTriangle> = ArrayList()

    var last: PolygonPoint? = null
    constructor(p1: PolygonPoint, p2: PolygonPoint, p3: PolygonPoint) : this(arrayListOf(p1, p2, p3)) {
        p1.next = p2
        p2.next = p3
        p3.next = p1
        p1.previous = p3
        p2.previous = p1
        p3.previous = p2
    }

    /**
     * Requires at least 3 points
     * @param points - ordered list of points forming the polygon.
     *                 No duplicates are allowed
     */
    init {
        // Let's do one sanity check that first and last point hasn't got same position
        // It's something that often happen when importing polygon data from other formats
        if (points[0] == points[points.size - 1]) {
            logger.warn("Removed duplicate point")
            points.removeAt(points.size-1)
        }
        this.points.addAll(points)
    }


    fun pointCount(): Int {
        var count = points.size
        count += steinerPoints.size
        return count
    }

    fun addSteinerPoint(point: TriangulationPoint) {
        steinerPoints.add(point)
    }

    fun addSteinerPoints(points: List<TriangulationPoint>) {
        steinerPoints.addAll(points)
    }

    fun clearSteinerPoints() {
        steinerPoints.clear()
    }

    /**
     * Assumes: that given polygon is fully inside the current polygon
     * @param poly - a subtraction polygon
     */
    fun addHole(poly: Polygon) {
        holes.add( poly )
        // XXX: tests could be made here to be sure it is fully inside
    }

    /**
     * Will insert a point in the polygon after given point
     *
     * @param a
     * @param newPoint
     */
    fun insertPointAfter(a: PolygonPoint, newPoint: PolygonPoint) {
        // Validate that
        val index = points.indexOf(a)
        if (index != -1) {
            newPoint.next = a.next
            newPoint.previous = a
            a.next!!.previous = newPoint
            a.next = newPoint
            points.add(index+1, newPoint)
        } else {
            throw RuntimeException("Tried to insert a point into a Polygon after a point not belonging to the Polygon")
        }
    }

    fun addPoints(list: List<PolygonPoint>) {
        for (p in list) {
            p.previous = last
            if(last != null) {
                p.next = last!!.next
                last!!.next = p
            }
            last = p
            points.add(p)
        }
        val first: PolygonPoint = points[0] as PolygonPoint
        last!!.next = first
        first.previous = last
    }

    /**
     * Will add a point after the last point added
     *
     * @param p
     */
    fun addPoint(p: PolygonPoint) {
        p.previous = last
        p.next = last!!.next
        last!!.next = p
        points.add(p)
    }

    fun removePoint(p: PolygonPoint) {
        var next = p.next
        var prev = p.previous
        prev!!.next = next
        next!!.previous = prev
        points.remove(p)
    }

    fun getPoint(): PolygonPoint? {
        return last
    }

    override fun addTriangle(t: DelaunayTriangle) {
        triangles.add(t)
    }

    override fun addTriangles(list: List<DelaunayTriangle>) {
        triangles.addAll(list)
    }

    override fun clearTriangulation() {
        triangles.clear()
    }

    override fun getTriangulationMode(): TriangulationMode {
        return TriangulationMode.POLYGON
    }

    /**
     * Creates constraints and populates the context with points
     */
    override fun prepareTriangulation(tcx: TriangulationContext<*>) {
        triangles.clear()

        // Outer constraints
        for (i in 0 until points.size - 1) {
            tcx.newConstraint(points[i], points[i+1])
        }
        tcx.newConstraint(points[0], points[points.size - 1])
        tcx.addPoints(points)

        // Hole constraints
        if(holes.isNotEmpty()) {
            for(p in holes) {
                for(i in 0 until p.points.size - 1) {
                    tcx.newConstraint( p.points[i], p.points[i+1])
                }
                tcx.newConstraint(p.points[0], p.points[p.points.size-1])
                tcx.addPoints(p.points)
            }
        }

        if (steinerPoints.isNotEmpty()) {
            tcx.addPoints(steinerPoints)
        }
    }
    companion object {
        val logger: Logger = LoggerFactory.getLogger(Polygon::class.java)
    }

}

object PolygonGenerator {
    const val PI2 = 2.0 * PI

    fun randomCircleSweep(scale: Double, vertexCount: Int): Polygon {
        val points = ArrayList<PolygonPoint>(vertexCount)
        var radius = scale/4.0

        for(i in 0 until vertexCount) {
            do {
                radius += if (i%250 == 0) {
                    scale/2*(0.5 - Math.random())
                } else if( i%50 == 0 ) {
                    scale/5*(0.5 - Math.random())
                } else {
                    25*scale/vertexCount*(0.5 - Math.random())
                }
                radius = if (radius > scale/2) {
                    scale/2
                } else {
                    radius
                }
                radius = if (radius < scale/10) {
                    scale/10
                } else {
                    radius
                }
            } while (radius < scale/10 || radius > scale/2)
            val point = PolygonPoint(radius * cos((PI2 * i)/vertexCount),
                radius * sin((PI2*i)/vertexCount))
            points[i] = point
        }
        return Polygon(points)
    }

    fun randomCircleSweep2(scale: Double, vertexCount: Int): Polygon {
        val points = ArrayList<PolygonPoint>()
        var radius = scale/4

        for (i in 0 until vertexCount) {
            do {
                radius += scale/5*(0.5 - Math.random())
                radius = if (radius > scale/2) {
                    scale/2
                } else {
                    radius
                }
                radius = if (radius < scale/10) {
                    scale/10
                } else {
                    radius
                }
            } while (radius < scale/10 || radius > scale/2)
            val point = PolygonPoint(radius * cos((PI2 * i)/vertexCount),
                radius * sin((PI2 * i)/vertexCount))
            points[i] = point
        }
        return Polygon(points)
    }


}

