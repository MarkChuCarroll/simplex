package org.goodmath.simplex.kcsg.poly2tri

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.absoluteValue


/* Poly2Tri
 * Copyright (c) 2009-2010, Poly2Tri Contributors
 * http://code.google.com/p/poly2tri/
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Poly2Tri nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


class DelaunayTriangle(p1: TriangulationPoint, p2: TriangulationPoint, p3: TriangulationPoint) {
    /** Neighbor pointers */
    val neighbors = Array<DelaunayTriangle?>(3) { null }
    /** Flags to determine if an edge is a Constrained edge */
    val cEdge = Array<Boolean>(3) { false }
    /** Flags to determine if an edge is a Delaunay edge */
    val dEdge = Array<Boolean>(3) { false }
    /** Has this triangle been marked as an interior triangle? */
    var interior: Boolean  = false

    val points: Array<TriangulationPoint?> = arrayOf(p1, p2, p3)


    fun index(p: TriangulationPoint): Int {
        return if (p == points[0]) {
            0
        } else if (p == points[1]) {
            1
        } else if (p == points[2]) {
            2
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

    fun indexCCW(p: TriangulationPoint): Int
    {
        val index = index(p)
        return when (index) {
            0-> 1
            1 -> 2
            else -> 0
        }
    }

    fun contains(p: TriangulationPoint): Boolean {
        return (p == points[0] || p == points[1] || p == points[2])
    }

    fun contains(e: DTSweepConstraint): Boolean {
        return (contains(e.p) && contains(e.q))
    }

    fun contains(p: TriangulationPoint, q: TriangulationPoint): Boolean {
        return contains(p) && contains(q)
    }

    // Update neighbor pointers
    private fun markNeighbor(p1: TriangulationPoint,
                             p2: TriangulationPoint,
                             t: DelaunayTriangle) {
        if ((p1 == points[2] && p2 == points[1]) || (p1 == points[1] && p2 == points[2])) {
            neighbors[0] = t
        } else if ((p1 == points[0] && p2 == points[2]) || (p1 == points[2] && p2 == points[0])) {
            neighbors[1] = t
        } else if(( p1 == points[0] && p2 == points[1]) || (p1 == points[1] && p2 == points[0])) {
            neighbors[2] = t
        } else {
            logger.error( "Neighbor error, please report!" )
        }
    }

    /* Exhaustive search to update neighbor pointers */
    fun markNeighbor(t: DelaunayTriangle) {
        if (t.contains(points[1]!!, points[2]!!)) {
            neighbors[0] = t
            t.markNeighbor(points[1]!!, points[2]!!, this)
        } else if (t.contains( points[0]!!, points[2]!!)) {
            neighbors[1] = t
            t.markNeighbor(points[0]!!, points[2]!!, this)
        } else if (t.contains( points[0]!!, points[1]!!)) {
            neighbors[2] = t
            t.markNeighbor(points[0]!!, points[1]!!, this)
        } else {
            logger.error("markNeighbor failed")
        }
    }

    fun clearNeighbors() {
        neighbors[0] = null
        neighbors[1] = null
        neighbors[2] = null
    }

    fun clearNeighbor(triangle: DelaunayTriangle) {
        if (neighbors[0] == triangle) {
            neighbors[0] = null
        } else if (neighbors[1] == triangle) {
            neighbors[1] = null
        } else {
            neighbors[2] = null
        }
    }

    /**
     * Clears all references to all other triangles and points
     */
    fun clear() {
        for (t in neighbors) {
            t?.clearNeighbor(this)
        }
        clearNeighbors()
        points[0] = null
        points[1] = null
        points[2] =null
    }

    /**
     * @param t - opposite triangle
     * @param p - the point in t that isn't shared between the triangles
     * @return
     */
    fun oppositePoint(t: DelaunayTriangle, p: TriangulationPoint): TriangulationPoint? {
        assert(t != this) { "opposite self-pointer error" }
        return pointCW(t.pointCW(p)!!)
    }

    // The neighbor clockwise to given point
    fun neighborCW(point: TriangulationPoint): DelaunayTriangle {
        return (if (point == points[0]) {
            neighbors[1]
        } else if (point == points[1]) {
            neighbors[2]
        } else {
            neighbors[0]
        })!!
    }

    // The neighbor counter-clockwise to given point
    fun neighborCCW(point: TriangulationPoint): DelaunayTriangle {
        return (if (point == points[0]) {
            neighbors[2]
        } else if (point == points[1]) {
            neighbors[0]
        } else {
            neighbors[1]
        })!!
    }

    // The neighbor across to given point
    fun neighborAcross(oPoint: TriangulationPoint): DelaunayTriangle? {
        return (if (oPoint == points[0]) {
            neighbors[0]
        } else if (oPoint == points[1]) {
            neighbors[1]
        } else {
            neighbors[2]
        })
    }

    // The point counter-clockwise to given point
    fun pointCCW(point: TriangulationPoint): TriangulationPoint {
        if (point == points[0]) {
            return points[1]!!
        } else if (point == points[1]) {
            return points[2]!!
        } else if (point == points[2]) {
            return points[0]!!
        }
        logger.error("CCW point location error")
        throw RuntimeException("[FIXME] CCW point location error")
    }

    // The point counter-clockwise to given point
    fun pointCW(point: TriangulationPoint): TriangulationPoint? {
        if (point == points[0]) {
            return points[2]
        } else if (point == points[1]) {
            return points[0]
        } else if (point == points[2]) {
            return points[1]
        }
        logger.error("CW point location error")
        throw RuntimeException("[FIXME] CW point location error")
    }

    // Legalize triangle by rotating clockwise around oPoint
    fun legalize(oPoint: TriangulationPoint, nPoint: TriangulationPoint) {
        if (oPoint == points[0]) {
            points[1] = points[0]
            points[0] = points[2]
            points[2] = nPoint
        } else if (oPoint == points[1]) {
            points[2] = points[1]
            points[1] = points[0]
            points[0] = nPoint
        } else if (oPoint == points[2]) {
            points[0] = points[2]
            points[2] = points[1]
            points[1] = nPoint
        } else {
            logger.error("legalization error")
            throw RuntimeException("legalization bug")
        }
    }

    fun printDebug() {
        println("${points[0]},${points[1]},${points[2]}")
    }

    // Finalize edge marking
    fun markNeighborEdges() {
        for (i in 0 until 3) {
            if (cEdge[i]) {
                when(i) {
                    0 ->
                        if (neighbors[0] != null) {
                            neighbors[0]!!.markConstrainedEdge(points[1]!!, points[2]!!)
                        }
                    1 ->
                        if (neighbors[1] != null) {
                            neighbors[1]!!.markConstrainedEdge(points[0]!!, points[2]!!)
                        }
                    2 ->
                        if (neighbors[2] != null) {
                            neighbors[2]!!.markConstrainedEdge(points[0]!!, points[1]!!)
                        }
                    else -> throw RuntimeException("Invalid neighbor edge number $i")
                }
            }
        }
    }

    fun markEdge(triangle: DelaunayTriangle) {
        for (i in 0 until 3) {
            if (cEdge[i]) {
                when(i) {
                    0 ->
                        triangle.markConstrainedEdge(points[1]!!, points[2]!!)
                    1 ->
                        triangle.markConstrainedEdge(points[0]!!, points[2]!!)
                    2 ->
                        triangle.markConstrainedEdge(points[0]!!, points[1]!!)
                    else -> throw RuntimeException("Invalid neighbor edge number $i")
                }
            }
        }
    }

    fun markEdge(tList: ArrayList<DelaunayTriangle>) {
        for (t in tList) {
            for (i in 0 until 3) {
                if (t.cEdge[i]) {
                    when (i) {
                        0 ->
                        markConstrainedEdge(t.points[1]!!, t.points[2]!!)
                        1 ->
                        markConstrainedEdge(t.points[0]!!, t.points[2]!!)
                        2 ->
                        markConstrainedEdge(t.points[0]!!, t.points[1]!!)
                        else -> throw RuntimeException("Invalid neighbor edge number $i")
                    }
                }
            }
        }
    }

    fun markConstrainedEdge(index: Int) {
        cEdge[index] = true
    }

    fun markConstrainedEdge(edge: DTSweepConstraint) {
        markConstrainedEdge(edge.p, edge.q)
        if ((edge.q == points[0] && edge.p == points[1])
            || (edge.q == points[1] && edge.p == points[0])) {
            cEdge[2] = true
        } else if ((edge.q == points[0] && edge.p == points[2])
            || (edge.q == points[2] && edge.p == points[0])) {
            cEdge[1] = true
        } else if ((edge.q == points[1] && edge.p == points[2])
            || (edge.q == points[2] && edge.p == points[1])) {
            cEdge[0] = true
        }
    }

    // Mark edge as constrained
    fun markConstrainedEdge(p: TriangulationPoint, q: TriangulationPoint) {
        if ((q == points[0] && p == points[1]) || (q == points[1] && p == points[0])) {
            cEdge[2] = true
        } else if ((q == points[0] && p == points[2]) || (q == points[2] && p == points[0])) {
            cEdge[1] = true
        } else if ((q == points[1] && p == points[2]) || (q == points[2] && p == points[1])) {
            cEdge[0] = true
        }
    }

    fun area(): Double {
        val a = (points[0]!!.x - points[2]!!.x) * (points[1]!!.y - points[0]!!.y)
        val b = (points[0]!!.x - points[1]!!.x) * (points[2]!!.y - points[0]!!.y)
        return 0.5*(a - b).absoluteValue
    }

    val centroid: TPoint
        get() {
            val cx = (points[0]!!.x + points[1]!!.x + points[2]!!.x)/3
            val cy = (points[0]!!.y + points[1]!!.y + points[2]!!.y )/3
            return TPoint(cx, cy)
        }

    /**
     * Get the neighbor that share this edge
     * @param p1 start of the shared edge
     * @param p2 end of the shared edge
     * @return index of the shared edge or -1 if edge isn't shared
     */
    fun edgeIndex(p1: TriangulationPoint, p2: TriangulationPoint): Int {
        if (points[0] == p1) {
            if (points[1] == p2) {
                return 2
            } else if (points[2] == p2) {
                return 1
            }
        } else if (points[1] == p1) {
            if (points[2] == p2) {
                return 0
            } else if (points[0] == p2) {
                return 2
            }
        } else if (points[2] == p1) {
            if (points[0] == p2) {
                return 1
            } else if (points[1] == p2) {
                return 0
            }
        }
        return -1
    }

    fun getConstrainedEdgeCCW(p: TriangulationPoint): Boolean {
        if (p == points[0]) {
            return cEdge[2]
        } else if (p == points[1]) {
            return cEdge[0]
        }
        return cEdge[1]
    }

    fun getConstrainedEdgeCW(p: TriangulationPoint): Boolean {
        if (p == points[0]) {
            return cEdge[1]
        } else if (p == points[1]) {
            return cEdge[2]
        }
        return cEdge[0]
    }

    fun getConstrainedEdgeAcross(p: TriangulationPoint): Boolean {
        if (p == points[0]) {
            return cEdge[0]
        } else if (p == points[1]) {
            return cEdge[1]
        }
        return cEdge[2]
    }

    fun setConstrainedEdgeCCW(p: TriangulationPoint, ce: Boolean) {
        if (p == points[0]) {
            cEdge[2] = ce
        } else if  (p == points[1]) {
            cEdge[0] = ce
        } else {
            cEdge[1] = ce
        }
    }

    fun setConstrainedEdgeCW(p: TriangulationPoint, ce: Boolean) {
        if (p == points[0]) {
            cEdge[1] = ce
        } else if (p == points[1]) {
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

    fun getDelaunayEdgeCCW(p: TriangulationPoint): Boolean {
        if (p == points[0]) {
            return dEdge[2]
        } else if (p == points[1]) {
            return dEdge[0]
        }
        return dEdge[1]
    }

    fun getDelaunayEdgeCW(p: TriangulationPoint): Boolean {
        if (p == points[0]) {
            return dEdge[1]
        } else if (p == points[1]) {
            return dEdge[2]
        }
        return dEdge[0]
    }

    fun getDelaunayEdgeAcross(p: TriangulationPoint): Boolean {
        if (p == points[0]) {
            return dEdge[0]
        } else if (p == points[1]) {
            return dEdge[1]
        }
        return dEdge[2]
    }

    fun setDelaunayEdgeCCW(p: TriangulationPoint, e: Boolean) {
        if (p == points[0]) {
            dEdge[2] = e
        } else if (p == points[1]) {
            dEdge[0] = e
        } else {
            dEdge[1] = e
        }
    }

    fun setDelaunayEdgeCW(p: TriangulationPoint, e: Boolean) {
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

    fun clearDelaunayEdges() {
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

    companion object {
        val logger: Logger = LoggerFactory.getLogger(DelaunayTriangle.javaClass)
    }
}
