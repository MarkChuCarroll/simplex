package org.goodmath.simplex.kcsg.quickhull

/**
 * Maintains a double-linked list of vertices for use by QuickHull3D
 */
class VertexList {
    var _head: Vertex? = null
    var _tail: Vertex? = null

    /**
     * Clears this list.
     */
    fun clear() {
        _head = null
        _tail = null
    }

    /**
     * Adds a vertex to the end of this list.
     */
    fun add(vtx: Vertex) {
        if (_head == null) {
            _head = vtx
        } else {
            _tail!!._next = vtx
        }
        vtx._prev = _tail
        vtx._next = null
        _tail = vtx
    }

    /**
     * Adds a chain of vertices to the end of this list.
     */
    fun addAll(inVtx: Vertex) {
        var vtx = inVtx
        if (_head == null) {
            _head = vtx
        } else {
            _tail!!._next = vtx
        }
        vtx._prev = _tail
        while (vtx._next != null) {
            vtx = vtx._next!!
        }
        _tail = vtx
    }

    /**
     * Deletes a vertex from this list.
     */
    fun delete(vtx: Vertex) {
        if (vtx._prev == null) {
            _head = vtx._next
        } else {
            vtx._prev!!._next = vtx._next
        }
        if (vtx._next == null) {
            _tail = vtx._prev
        } else {
            vtx._next!!._prev = vtx._prev
        }
    }

    /**
     * Deletes a chain of vertices from this list.
     */
    fun delete(vtx1: Vertex, vtx2: Vertex) {
        if (vtx1._prev == null) {
            _head = vtx2._next
        } else {
            vtx1._prev!!._next = vtx2._next
        }
        if (vtx2._next == null) {
            _tail = vtx1._prev
        } else {
            vtx2._next!!._prev = vtx1._prev
        }
    }

    /**
     * Inserts a vertex into this list before another
     * specificed vertex.
     */
    fun insertBefore(vtx: Vertex, next: Vertex) {
        vtx._prev = next._prev
        if (next._prev == null) {
            _head = vtx
        } else {
            next._prev!!._next = vtx
        }
        vtx._next = next
        next._prev = vtx
    }

    /**
     * Returns the first element in this list.
     */
    fun first(): Vertex? {
        return _head
    }

    /**
     * Returns true if this list is empty.
     */
    fun isEmpty(): Boolean {
        return _head == null
    }
}
