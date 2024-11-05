package org.goodmath.simplex.kcsg.poly2tri

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


class Polygon(ps: MutableList<PolygonPoint>): Triangulatable<PolygonPoint> {
    override val points: MutableList<PolygonPoint> = ArrayList()

    val steinerPoints = ArrayList<TriangulationPoint>()
    val holes = ArrayList<Polygon>()

    val mTriangles = ArrayList<DelaunayTriangle>()

    var last: PolygonPoint by LateInitDelegate("last")

    init {
        if (ps.size == 3) {
            val p1 = ps[0]
            val p2 = ps[1]
            val p3 = ps[2]
            p1.next = p2
            p2.next = p3
            p3.next = p1
            p1.previous = p3
            p2.previous = p1
            p3.previous = p2
            points.add(p1)
            points.add(p2)
            points.add(p3)
        } else {
            // Lets do one sanity check that first and last point hasn't got same position
            // Its something that often happen when importing polygon data from other formats
            if (ps[0] == ps[ps.size - 1]) {
                ps.removeAt(ps.size - 1)
            }
            points.addAll(ps)
        }
    }

    fun pointCount(): Int {
        var count = points.size
        if(steinerPoints.isNotEmpty()) {
            count += steinerPoints.size
        }
        return count
    }

    fun addSteinerPoint(point: TriangulationPoint) {
        steinerPoints.add(point)
    }

    fun addSteinerPoints(ps: List<TriangulationPoint>) {
        steinerPoints.addAll(ps)
    }

    fun clearSteinerPoints() {
        steinerPoints.clear()
    }

    /**
     * Assumes: that given polygon is fully inside the current polygon
     * @param poly - a subtraction polygon
     */
    fun addHole(poly: Polygon) {
        holes.add(poly)
        // XXX: tests could be made here to be sure it is fully inside
//        addSubtraction( poly.getPoints() );
    }

    /**
     * Will insert a point in the polygon after given point
     *
     */
    fun insertPointAfter(a: PolygonPoint, newPoint: PolygonPoint) {
        // Validate that
        var index = points.indexOf(a)
        if(index != -1) {
            newPoint.next = a.next
            newPoint.previous = a
            a.next.previous = newPoint
            a.next = newPoint
            points.add(index+1, newPoint)
        } else {
            throw RuntimeException( "Tried to insert a point into a Polygon after a point not belonging to the Polygon" )
        }
    }

    fun addPoints(list: List<PolygonPoint>) {
        for(p in list) {
            p.previous = last
            p.next = last.next
            last.next = p
            last = p
            points.add(p)
        }
        val first = points[0]
        last.next = first
        first.previous = last
    }

    /**
     * Will add a point after the last point added
     *
     * @param p
     */
    fun addPoint(p: PolygonPoint) {
        p.previous = last
        p.next = last.next
        last.next = p
        points.add(p)
    }

    fun removePoint(p: PolygonPoint) {
        val next = p.next
        val prev = p.previous
        prev.next = next
        next.previous = prev
        points.remove(p)
    }

    val point: PolygonPoint
        get() = last

    override val triangles: MutableList<DelaunayTriangle>
        get() = mTriangles

    override fun addTriangle(t: DelaunayTriangle) {
        mTriangles.add(t)
    }

    override fun addTriangles(list: List<DelaunayTriangle>) {
        mTriangles.addAll(list)
    }

    override fun clearTriangulation() {
        mTriangles.clear()
    }

    override val triangulationMode: TriangulationMode = TriangulationMode.POLYGON

    /**
     * Creates constraints and populates the context with points
     */
    override fun prepareTriangulation(tcx: TriangulationContext<*>) {
        mTriangles.clear()

        // Outer constraints
        for(i in 0 until points.size - 1) {
            tcx.newConstraint(points[i], points[i+1])
        }
        tcx.newConstraint(points[0], points[points.size-1])
        tcx.addPoints(points)

        // Hole constraints
        if(holes.isNotEmpty()) {
            for(p in holes) {
                for (i in 0 until p.points.size - 1) {
                    tcx.newConstraint(p.points[i], p.points[i+1])
                }
                tcx.newConstraint(p.points[0], p.points[p.points.size-1])
                tcx.addPoints(p.points)
            }
        }

        if( steinerPoints.isNotEmpty()) {
            tcx.addPoints(steinerPoints)
        }
    }
}
