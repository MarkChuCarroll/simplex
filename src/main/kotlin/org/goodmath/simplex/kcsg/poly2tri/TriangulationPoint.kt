package org.goodmath.simplex.kcsg.poly2tri

abstract class TriangulationPoint: Point() {
    // List of edges this point constitutes an upper ending point (CDT)
    val edges: ArrayList<DTSweepConstraint> = ArrayList()

    override fun toString(): String {
        return "[" + x + "," + y + "]";
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
        for(c in edges) {
            if( c.p == p ) {
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

}

open class TPoint(override var x: Double,
                  override var y:  Double,
                  override var z: Double = 0.0): TriangulationPoint() {

    override val xf: Float = x.toFloat()
    override val yf: Float = y.toFloat()
    override val zf: Float = z.toFloat()

    override fun set(nx: Double, ny: Double, nz: Double) {
        x = nx
        y = ny
        z = nz
    }
}
