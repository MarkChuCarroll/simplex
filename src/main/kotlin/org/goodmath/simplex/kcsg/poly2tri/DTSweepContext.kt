package org.goodmath.simplex.kcsg.poly2tri

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * DTSweepContext.java
 *
 * Copyright 2014-2014 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Michael Hoffer <info@michaelhoffer.de>.
 */

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



class DTSweepDebugContext(ctx: TriangulationContext<*>): TriangulationDebugContext(ctx) {

    /*
     * Fields used for visual representation of current triangulation
     */
    var storedPrimaryTriangle: DelaunayTriangle? = null
    var storedSecondaryTriangle:  DelaunayTriangle? = null
    var activePoint: TriangulationPoint? = null
    var storedActiveNode:  AdvancingFrontNode? = null
    var storedActiveConstraint: DTSweepConstraint? = null

    fun isDebugContext(): Boolean {
        return true
    }


    var primaryTriangle: DelaunayTriangle
        get() = storedPrimaryTriangle!!
        set(value) {
            storedPrimaryTriangle = value
            tcx.update("setPrimaryTriangle")
        }

    var secondaryTriangle: DelaunayTriangle
        get() = storedSecondaryTriangle!!
        set(value) {
            secondaryTriangle = value
            tcx.update("setSecondaryTriangle")
        }

    var activeConstraint: DTSweepConstraint
        get() = storedActiveConstraint!!
        set(value) {
            storedActiveConstraint = value
            tcx.update("setWorkingSegment")
        }

    var activeNode: AdvancingFrontNode
        get() = storedActiveNode!!
        set(value) {
            activeNode = value
            tcx.update("setWorkingNode")
        }


    override fun clear() {
        storedPrimaryTriangle = null
        storedSecondaryTriangle = null
        activePoint = null
        storedActiveNode = null
        storedActiveConstraint = null
    }
}


/**
 * @author Thomas ??? (thahlen@gmail.com)
 */
class DTSweepContext: TriangulationContext<DTSweepDebugContext>() {


    // Initial triangle factor, seed triangle will extend 30% of
    // PointSet width to both left and right.
    val alpha = 0.3

    /** Advancing front **/
    var aFront:  AdvancingFront? = null

    /** head point used with advancing front */
    var head:  TriangulationPoint? = null

    /** tail point used with advancing front */
    var tail:  TriangulationPoint? = null

    val basin = Basin()

    val edgeEvent = EdgeEvent()

    init {
        clear()
    }


    fun removeFromList(triangle: DelaunayTriangle) {
        triList.remove(triangle)
        // TODO: remove all neighbor pointers to this triangle
    }

    fun meshClean(triangle: DelaunayTriangle?) {
        var t1: DelaunayTriangle?
        var t2: DelaunayTriangle?
        if (triangle != null) {
            val deque = ArrayDeque<DelaunayTriangle>()
            deque.addFirst(triangle)
            triangle.isInterior(true)

            while (!deque.isEmpty()) {
                t1 = deque.removeFirst()
                triUnit!!.addTriangle(t1)
                for(i in 0 until 3) {
                    if (!t1.cEdge[i]) {
                        t2 = t1.neighbors[i]
                        if (t2 != null && !t2.isInterior()) {
                            t2.isInterior(true)
                            deque.addLast(t2)
                        }
                    }
                }
            }
        }
    }

    override fun clear() {
        super.clear()
        triList.clear()
    }

    fun getAdvancingFront(): AdvancingFront? {
        return aFront
    }

    fun addNode(node: AdvancingFrontNode) {
        aFront!!.addNode(node)
    }

    fun removeNode(node: AdvancingFrontNode) {
        aFront!!.removeNode(node)
    }

    fun locateNode(point: TriangulationPoint): AdvancingFrontNode? {
        return aFront!!.locateNode( point )
    }

    fun createAdvancingFront() {
        var head: AdvancingFrontNode
        var tail: AdvancingFrontNode
        var middle: AdvancingFrontNode
        // Initial triangle
        var iTriangle = DelaunayTriangle(points[0],
            this.tail!!,
            this.head!!)
        addToList(iTriangle)

        head = AdvancingFrontNode(iTriangle.points[1]!!)
        head.triangle = iTriangle
        middle = AdvancingFrontNode(iTriangle.points[0]!!)
        middle.triangle = iTriangle
        tail = AdvancingFrontNode(iTriangle.points[2]!!)

        aFront = AdvancingFront(head, tail)
        aFront!!.addNode(middle)

        // TODO: I think it would be more intuitive if head is middles next and not previous
        //       so swap head and tail
        aFront!!.head.next = middle
        middle.next = aFront!!.tail
        middle.prev = aFront!!.head
        aFront!!.tail.prev = middle
    }

    class Basin {
        var leftNode: AdvancingFrontNode? = null
        var bottomNode: AdvancingFrontNode? = null
        var rightNode: AdvancingFrontNode? = null
        var width: Double = 0.0
        var leftHighest: Boolean = false
    }

    class EdgeEvent {
        var constrainedEdge: DTSweepConstraint? = null
        var right: Boolean = false
    }

    /**
     * Try to map a node to all sides of this triangle that don't have
     * a neighbor.
     *
     * @param t
     */
    fun mapTriangleToNodes(t:  DelaunayTriangle) {
        var n: AdvancingFrontNode? = null
        for (i in 0 until 3) {
            if (t.neighbors[i] == null) {
                n = aFront!!.locatePoint(t.pointCW(t.points[i]!!)!!)
                if (n != null) {
                    n.triangle = t
                }
            }
        }
    }

    override fun prepareTriangulation(t: Triangulatable) {
        super.prepareTriangulation(t)
        val xMax = points.maxOf { it.x }
        val xMin = points.minOf { it.x }
        val yMax = points.maxOf { it.y }
        val yMin = points.minOf { it.x }

        val deltaX = alpha * (xMax - xMin)
        val deltaY = alpha * ( yMax - yMin )
        val p1 = TPoint(xMax + deltaX, yMin - deltaY)
        val p2 = TPoint(xMin - deltaX, yMin - deltaY)

        head = p1
        tail = p2

        points.sort()
    }


    fun finalizeTriangulation() {
        triUnit!!.addTriangles(triList)
        triList.clear()
    }

    override fun newConstraint(a: TriangulationPoint, b: TriangulationPoint): TriangulationConstraint {
        return DTSweepConstraint(a, b)
    }

    override val algorithm: TriangulationAlgorithm = TriangulationAlgorithm.DTSweep

    companion object {
        val logger: Logger = LoggerFactory.getLogger(DTSweepContext.javaClass)
    }
}
