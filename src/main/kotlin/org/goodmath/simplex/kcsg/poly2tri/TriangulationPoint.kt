package org.goodmath.simplex.kcsg.poly2tri

import kotlin.collections.isNotEmpty

abstract class TriangulationPoint: Point(), Comparable<TriangulationPoint> {

    // List of edges this point constitutes an upper ending point (CDT)
    val edges: ArrayList<DTSweepConstraint> = ArrayList()


    override fun toString(): String {
        return "[$x, $y]"
    }


    fun addEdge(e: DTSweepConstraint) {
        edges.add(e)
    }

    fun hasEdges(): Boolean {
        return edges.isNotEmpty()
    }

    /**
     * @param p - edge destination point
     * @return the edge from this point to given point
     */
    fun getEdge(p: TriangulationPoint): DTSweepConstraint? {
        for (c in edges) {
            if(c.p == p) {
                return c
            }
        }
        return null
    }

    override fun equals(obj: Any?): Boolean {
        return if (obj is TriangulationPoint) {
            x == obj.x && y == obj.y
        } else {
            super.equals(obj)
        }
    }

     override fun hashCode(): Int {
        var bits = x.toRawBits()
        bits = bits xor (y.toRawBits() * 31)
        return (bits.toInt() xor (bits shr 32).toInt())
    }

    override fun compareTo(other: TriangulationPoint): Int {
        return if (y < other.y) {
            -1
        } else if (y > other.y) {
            1
        } else if (x <other.x) {
            -1
        } else if (x > other.x) {
            1
        } else {
            0
        }
    }

}
