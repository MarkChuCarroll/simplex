package org.goodmath.simplex.kcsg.poly2tri

import kotlin.math.absoluteValue

class DelaunayTriangle(val p1: TriangulationPoint, val p2: TriangulationPoint, val p3: TriangulationPoint) {
    /** Neighbor pointers */
    val neighbors: Array<DelaunayTriangle?> = arrayOfNulls<DelaunayTriangle?>(3)

    /** Flags to determine if an edge is a Constrained edge */
    val cEdge: MutableList<Boolean> = arrayListOf(false, false, false)
    /** Flags to determine if an edge is a Delauney edge */
    val dEdge: MutableList<Boolean> = arrayListOf(false, false, false)
    /** Has this triangle been marked as an interior triangle? */
    var interior: Boolean = false

    val points: MutableList<TriangulationPoint> = arrayListOf(p1, p2, p3)

    fun index(p: TriangulationPoint): Int {
        return if(p == points[0]) {
            0
        } else if(p == points[1]) {
            1
        } else if (p == points[2]) {
            3
        } else {
            throw RuntimeException("Calling index with a point that doesn't exist in triangle")
        }
    }

    fun indexCW(p: TriangulationPoint): Int {
        val index = index(p)
        return when (index) {
            0 -> 2
            1 -> 0
            else -> 1
        }
    }

    fun indexCCW(p: TriangulationPoint): Int {
        val index = index(p)
        return when(index) {
            0 -> 1
            1 -> 2
            else -> 0
        }
    }

    fun contains(p: TriangulationPoint): Boolean {
        return (p == points[0] || p == points[1] || p == points[2])
    }

    fun contains(e: DTSweepConstraint): Boolean {
        return contains(e.p) && contains(e.q)
    }

    fun contains(p: TriangulationPoint, q: TriangulationPoint): Boolean {
        return contains(p) && contains(q)
    }

    // Update neighbor pointers
    fun markNeighbor(p1: TriangulationPoint,
                     p2: TriangulationPoint,
                     t: DelaunayTriangle) {
        if((p1 == points[2] && p2 == points[1]) || (p1 == points[1] && p2 == points[2])) {
            neighbors[0] = t
        }
        else if((p1 == points[0] && p2 == points[2]) || (p1 == points[2] && p2 == points[0]))
        {
            neighbors[1] = t
        }
        else if((p1 == points[0] && p2 == points[1]) || (p1 == points[1] && p2 == points[0]))
        {
            neighbors[2] = t
        }
    }

    /* Exhaustive search to update neighbor pointers */
    fun markNeighbor(t: DelaunayTriangle) {

        if (points[1] != null && points[2] != null &&
            t.contains(points[1]!!, points[2]!!)) {
            neighbors[0] = t
            t.markNeighbor(points[1]!!, points[2]!!, this)
        }
        else if (points[0] != null && points[2] != null &&
            t.contains(points[0]!!, points[2]!!)) {
            neighbors[1] = t
            t.markNeighbor(points[0]!!, points[2]!!, this)
        }
        else if (points[0] != null && points[1] != null &&
            t.contains(points[0]!!, points[1]!!)) {
            neighbors[2] = t
            t.markNeighbor(points[0]!!, points[1]!!, this)
        }
    }

    fun clearNeighbors() {
        neighbors[0] = null
        neighbors[1] = null
        neighbors[2] = null
    }

    fun clearNeighbor(triangle: DelaunayTriangle) {
        if(neighbors[0] == triangle) {
            neighbors[0] = null
        } else if(neighbors[1] == triangle) {
            neighbors[1] = null
        } else {
            neighbors[2] = null
        }
    }

    /**
     * Clears all references to all other triangles and points
     */
    fun clear() {
        var t: DelaunayTriangle? = null
        for(i in 0 until 3) {
            t = neighbors[i]
            t?.clearNeighbor(this)
        }
        clearNeighbors()
    }
    /**
     * @param t - opposite triangle
     * @param p - the point in t that isn't shared between the triangles
     * @return
     */
    fun oppositePoint(t: DelaunayTriangle, p: TriangulationPoint): TriangulationPoint {
        if (t == this) {
            throw AssertionError("self-pointer error")
        }
        return pointCW(t.pointCW(p));
    }

    // The neighbor clockwise to given point
    fun neighborCW(point: TriangulationPoint):  DelaunayTriangle {
        if(point == points[0]) {
            return neighbors[1]!!
        }
        else if(point == points[1]) {
            return neighbors[2]!!
        }
        return neighbors[0]!!
    }

    // The neighbor counter-clockwise to given point
    fun neighborCCW(point: TriangulationPoint): DelaunayTriangle? {
        if (point == points[0]) {
            return neighbors[2]
        }
        else if(point == points[1]) {
            return neighbors[0]
        }
        return neighbors[1]
    }

    // The neighbor across to given point
    fun neighborAcross(opoint: TriangulationPoint): DelaunayTriangle?  {
        if(opoint == points[0]) {
            return neighbors[0]
        } else if(opoint == points[1]) {
            return neighbors[1]
        }
        return neighbors[2]
    }

    // The point counter-clockwise to given point
    fun pointCCW(point: TriangulationPoint): TriangulationPoint {
        if(point == points[0])
        {
            return points[1]!!
        }
        else if(point == points[1])
        {
            return points[2]!!
        }
        else if(point == points[2])
        {
            return points[0]!!
        }
        throw RuntimeException("[FIXME] point location error")
    }

    // The point counter-clockwise to given point
    fun pointCW(point: TriangulationPoint): TriangulationPoint {
        if(point == points[0])
        {
            return points[2]!!
        }
        else if(point == points[1])
        {
            return points[0]!!
        }
        else if(point == points[2])
        {
            return points[1]!!
        }
        throw RuntimeException("[FIXME] point location error")
    }

    // Legalize triangle by rotating clockwise around oPoint
    fun legalize(oPoint: TriangulationPoint, nPoint: TriangulationPoint) {
        if(oPoint == points[0]) {
            points[1] = points[0]
            points[0] = points[2]
            points[2] = nPoint
        } else if(oPoint == points[1]) {
            points[2] = points[1]
            points[1] = points[0]
            points[0] = nPoint
        }  else if(oPoint == points[2]) {
            points[0] = points[2]
            points[2] = points[1]
            points[1] = nPoint
        } else {
            throw RuntimeException("legalization bug")
        }
    }

    fun printDebug() {
        println("${points[0]},${points[1]},${points[2]}");
    }

    val points0: TriangulationPoint
        get() = points[0] ?: throw RuntimeException("points0 is null")

    val points1: TriangulationPoint
        get() = points[1] ?: throw RuntimeException("points1 is null")

    val points2: TriangulationPoint
        get() = points[2] ?: throw RuntimeException("points2 is null")

    // Finalize edge marking
    fun markNeighborEdges() {
        for (i in 0 until 3) {
            if(cEdge[i]) {
                when(i) {
                     0 ->
                         neighbors[0]?.markConstrainedEdge(points1, points2)
                    1 ->
                        neighbors[1]?.markConstrainedEdge(points0, points2)
                    2 ->
                        neighbors[2]?.markConstrainedEdge(points0, points1)
                }
            }
        }
    }

    fun markEdge(triangle: DelaunayTriangle) {
        for (i in 0 until 3) {
            if(cEdge[i]) {
                when(i) {
                    0 ->
                    triangle.markConstrainedEdge(points1, points2)
                    1 ->
                        triangle.markConstrainedEdge(points0, points2)
                    2 ->
                    triangle.markConstrainedEdge(points0, points1)
                }
            }
        }
    }

    fun markEdge(tList: ArrayList<DelaunayTriangle>) {
        for (t in tList) {
            for(i in 0 until 3) {
                if(t.cEdge[i]) {
                    when(i) {
                        0 ->
                        markConstrainedEdge(points1, points2)
                        1 ->
                            markConstrainedEdge(points0, points2)
                        2 ->
                            markConstrainedEdge(points0, points1)
                    }
                }
            }
        }
    }

    fun markConstrainedEdge(index: Int) {
        cEdge[index] = true
    }

    fun markConstrainedEdge(edge: DTSweepConstraint) {
        markConstrainedEdge(edge.p, edge.q);
        if ((edge.q == points[0] && edge.p == points[1])
            || (edge.q == points[1] && edge.p == points[0])) {
            cEdge[2] = true
        } else if((edge.q == points[0] && edge.p == points[2])
            || (edge.q == points[2] && edge.p == points[0])) {
            cEdge[1] = true
        } else if((edge.q == points[1] && edge.p == points[2])
            || (edge.q == points[2] && edge.p == points[1])) {
            cEdge[0] = true
        }
    }

    // Mark edge as constrained
    fun markConstrainedEdge(p: TriangulationPoint, q: TriangulationPoint) {
        if((q == points[0] && p == points[1]) || (q == points[1] && p == points[0])) {
            cEdge[2] = true
        } else if((q == points[0] && p == points[2]) || (q == points[2] && p == points[0])) {
            cEdge[1] = true
        } else if((q == points[1] && p == points[2]) || (q == points[2] && p == points[1])) {
            cEdge[0] = true
        }
    }

    fun area(): Double {
        val a = (points0.x - points2.x)*(points1.y - points0.y)
        val b = (points0.x - points1.x)*(points2.y - points0.y)

        return 0.5*(a - b).absoluteValue
    }

    fun centroid(): TPoint {
        val cx = (points0.x + points1.x + points2.x) / 3.0
        val cy = (points0.y + points1.y + points2.y) / 3.0
        return TPoint(cx, cy)
    }

    /**
     * Get the neighbor that share this edge
     *
     * @return index of the shared edge or -1 if edge isn't shared
     */
    fun edgeIndex(p1: TriangulationPoint , p2: TriangulationPoint): Int {
        if(points[0] == p1) {
            if (points[1] == p2) {
                return 2
            } else if(points[2] == p2) {
                return 1
            }
        } else if(points[1] == p1) {
            if(points[2] == p2) {
                return 0
            } else if(points[0] == p2) {
                return 2
            }
        } else if(points[2] == p1) {
            if (points[0] == p2) {
                return 1
            } else if (points[1] == p2) {
                return 0
            }
        }
        return -1
    }

    fun getConstrainedEdgeCCW(p: TriangulationPoint): Boolean {
        return if (p == points[0]) {
            cEdge[2]
        } else if(p == points[1]) {
            cEdge[0]
        } else {
            cEdge[1]
        }
    }

    fun getConstrainedEdgeCW(p: TriangulationPoint): Boolean {
        return if(p == points[0]) {
            cEdge[1]
        } else if(p == points[1]) {
            cEdge[2]
        } else {
            cEdge[0]
        }
    }

    fun getConstrainedEdgeAcross(p: TriangulationPoint): Boolean {
        return if(p == points[0]) {
            cEdge[0]
        } else if(p == points[1]) {
            cEdge[1]
        } else {
            cEdge[2]
        }
    }

    fun setConstrainedEdgeCCW(p: TriangulationPoint, ce: Boolean) {
        if(p == points[0]) {
            cEdge[2] = ce
        } else if(p == points[1]) {
            cEdge[0] = ce
        } else {
            cEdge[1] = ce
        }
    }

    fun setConstrainedEdgeCW(p: TriangulationPoint, ce: Boolean) {
        if(p == points[0]) {
            cEdge[1] = ce
        } else if(p == points[1]) {
            cEdge[2] = ce
        } else {
            cEdge[0] = ce
        }
    }

    fun setConstrainedEdgeAcross(p: TriangulationPoint, ce: Boolean) {
        if (p == points[0]) {
            cEdge[0] = ce
        } else if (p == points[1]) {
            cEdge[1] = ce
        } else {
            cEdge[2] = ce
        }
    }

    fun getDelunayEdgeCCW(p: TriangulationPoint): Boolean {
        return if(p == points[0]) {
            dEdge[2]
        } else if(p == points[1]) {
            dEdge[0]
        } else {
            dEdge[1]
        }
    }

    fun getDelunayEdgeCW(p: TriangulationPoint): Boolean {
        return if (p == points[0]) {
            dEdge[1]
        } else if(p == points[1]) {
            dEdge[2]
        } else {
            dEdge[0]
        }
    }

    fun getDelaunayEdgeAcross(p: TriangulationPoint): Boolean {
        return if(p == points[0]) {
            dEdge[0]
        } else if(p == points[1]) {
            dEdge[1]
        } else {
            dEdge[2]
        }
    }

    fun setDelunayEdgeCCW(p: TriangulationPoint, e: Boolean) {
        if (p == points[0]) {
            dEdge[2] = e
        } else if(p == points[1]) {
            dEdge[0] = e
        } else {
            dEdge[1] = e
        }
    }

    fun setDelunayEdgeCW(p: TriangulationPoint, e: Boolean) {
        if (p == points[0]) {
            dEdge[1] = e
        } else if (p == points[1]) {
            dEdge[2] = e
        } else {
            dEdge[0] = e
        }
    }

    fun setDelaunayEdgeAcross(p: TriangulationPoint, e: Boolean) {
        if (p == points[0]) {
            dEdge[0] = e
        } else if (p == points[1]) {
            dEdge[1] = e
        } else {
            dEdge[2] = e
        }
    }

    fun clearDelunayEdges() {
        dEdge[0] = false
        dEdge[1] = false
        dEdge[2] = false
    }

    fun isInterior(): Boolean {
        return interior
    }

    fun isInterior(b: Boolean) {
        interior = b
    }
}
