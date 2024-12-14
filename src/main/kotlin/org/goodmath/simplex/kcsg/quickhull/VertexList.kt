package org.goodmath.simplex.kcsg.quickhull

/**
 * Maintains a double-linked list of vertices for use by QuickHull3D
 */
class VertexList {
    var storedHead: Vertex? = null
    var storedTail: Vertex? = null

    /**
     * Clears this list.
     */
    fun clear() {
        storedHead = null
        storedTail = null
    }

    /**
     * Adds a vertex to the end of this list.
     */
    fun add(vtx: Vertex) {
        if (storedHead == null) {
            storedHead = vtx
        } else {
            storedTail!!.storedNext = vtx
        }
        vtx.storedPrev = storedTail
        vtx.storedNext = null
        storedTail = vtx
    }

    /**
     * Adds a chain of vertices to the end of this list.
     */
    fun addAll(inVtx: Vertex) {
        var vtx = inVtx
        if (storedHead == null) {
            storedHead = vtx
        } else {
            storedTail!!.storedNext = vtx
        }
        vtx.storedPrev = storedTail
        while (vtx.storedNext != null) {
            vtx = vtx.storedNext!!
        }
        storedTail = vtx
    }

    /**
     * Deletes a vertex from this list.
     */
    fun delete(vtx: Vertex) {
        if (vtx.storedPrev == null) {
            storedHead = vtx.storedNext
        } else {
            vtx.storedPrev!!.storedNext = vtx.storedNext
        }
        if (vtx.storedNext == null) {
            storedTail = vtx.storedPrev
        } else {
            vtx.storedNext!!.storedPrev = vtx.storedPrev
        }
    }

    /**
     * Deletes a chain of vertices from this list.
     */
    fun delete(vtx1: Vertex, vtx2: Vertex) {
        if (vtx1.storedPrev == null) {
            storedHead = vtx2.storedNext
        } else {
            vtx1.storedPrev!!.storedNext = vtx2.storedNext
        }
        if (vtx2.storedNext == null) {
            storedTail = vtx1.storedPrev
        } else {
            vtx2.storedNext!!.storedPrev = vtx1.storedPrev
        }
    }

    /**
     * Inserts a vertex into this list before another
     * specified vertex.
     */
    fun insertBefore(vtx: Vertex, next: Vertex) {
        vtx.storedPrev = next.storedPrev
        if (next.storedPrev == null) {
            storedHead = vtx
        } else {
            next.storedPrev!!.storedNext = vtx
        }
        vtx.storedNext = next
        next.storedPrev = vtx
    }

    /**
     * Returns the first element in this list.
     */
    fun first(): Vertex? {
        return storedHead
    }

    /**
     * Returns true if this list is empty.
     */
    fun isEmpty(): Boolean {
        return storedHead == null
    }
}
