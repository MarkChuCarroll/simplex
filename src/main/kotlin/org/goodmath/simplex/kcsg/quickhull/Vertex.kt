package org.goodmath.simplex.kcsg.quickhull


/**
 * Represents vertices of the hull, as well as the points from
 * which it is formed.
 *
 * @author John E. Lloyd, Fall 2004
 */
class Vertex(x: Double, y: Double, z: Double, var index: Int = -1): Vector3d(x, y, z) {
    /**
     * Spatial point associated with this vertex.
     */
    var pnt: Point3d = Point3d(x, y, z)


    /**
     * List forward link.
     */
    var storedNext: Vertex? = null

    /**
     * List backward link.
     */
    var storedPrev: Vertex? = null

    /**
     * Current face that this vertex is outside
     */
    var face: Face? = null

    /**
     * Constructs a vertex and sets its coordinates to 0.
     */
    constructor(): this(0.0, 0.0, 0.0, -1)

    fun getNext(): Vertex? {
        return storedNext
    }

    fun setNext(next: Vertex?) {
        storedNext = next
    }

    fun getPrev(): Vertex? {
        return storedPrev
    }

    fun setPrev(prev: Vertex?) {
        storedPrev = prev
    }

}
