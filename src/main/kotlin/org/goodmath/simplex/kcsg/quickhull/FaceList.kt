package org.goodmath.simplex.kcsg.quickhull



/**
 * Maintains a single-linked list of faces for use by QuickHull3D
 */
class FaceList {
    private var _head: Face? = null
    private var _tail: Face? = null

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
    fun add(vtx: Face) {
        if (_head == null) {
            _head = vtx
        } else {
            _tail!!.storedNext = vtx
        }
        vtx.storedNext = null
        _tail = vtx
    }

    fun first(): Face {
        return _head!!
    }

    /**
     * Returns true if this list is empty.
     */
    fun isEmpty(): Boolean {
        return _head == null
    }
}
