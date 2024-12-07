package org.goodmath.simplex.kcsg.quickhull

import kotlin.math.absoluteValue
import kotlin.math.sqrt

// Based on quickhull - original copyright:
    /*
      * Copyright John E. Lloyd, 2003. All rights reserved. Permission
      * to use, copy, and modify, without fee, is granted for non-commercial
      * and research purposes, provided that this copyright notice appears
      * in all copies.
      *
      * This  software is distributed "as is", without any warranty, including
      * any implied warranty of merchantability or fitness for a particular
      * use. The authors assume no responsibility for, and shall not be liable
      * for, any special, indirect, or consequential damages, or any damages
      * whatsoever, arising out of or in connection with the use of this
      * software.
      */


enum class FaceMark {
    VISIBLE,
    NON_CONVEX,
    DELETED
}

/**
 * Basic triangular face used to form the hull.
 *
 * <p>The information stored for each face consists of a planar
 * normal, a planar offset, and a doubly-linked list of three <a
 * href=HalfEdge>HalfEdges</a> which surround the face in a
 * counter-clockwise direction.
 *
 * @author John E. Lloyd, Fall 2004 */
class Face(var normal: Vector3d = Vector3d(), var centroid: Point3d = Point3d(), var mark: FaceMark = FaceMark.VISIBLE) {
    lateinit var he0: HalfEdge

    var area: Double = 0.0

    var planeOffset: Double = 0.0
    var index: Int = 0
    var numVerts: Int = 0

    var _next: Face? = null

    fun getNext(): Face {
        return _next!!
    }

    fun setNext(next: Face?) {
        _next = next
    }


    var outside: Vertex? = null

    fun computeCentroid(centroid: Point3d) {
        centroid.setZero()
        var he = he0
        do {
            centroid.add(he.head().pnt)
            he = he.getNext()
        } while (he != he0)
        centroid.scale(1 / numVerts.toDouble())
    }

    fun computeNormal(normal: Vector3d, minArea: Double) {
        computeNormal(normal)

        if (area < minArea) {
            // make the normal more robust by removing
            // components parallel to the longest edge

            var hedgeMax: HalfEdge? = null
            var lenSqrMax = 0.0
            var hedge = he0
            do {
                var lenSqr = hedge.lengthSquared()
                if (lenSqr > lenSqrMax) {
                    hedgeMax = hedge
                    lenSqrMax = lenSqr
                }
                hedge = hedge._next!!
            } while (hedge != he0)

            var p2 = hedgeMax!!.head().pnt
            var p1 = hedgeMax.tail()!!.pnt
            val lenMax = sqrt(lenSqrMax)
            val ux = (p2.x - p1.x) / lenMax
            val uy = (p2.y - p1.y) / lenMax
            val uz = (p2.z - p1.z) / lenMax
            val dot = normal.x * ux + normal.y * uy + normal.z * uz
            normal.x -= dot * ux
            normal.y -= dot * uy
            normal.z -= dot * uz

            normal
        }
    }

    fun computeNormal(normal: Vector3d) {
        var he1 = he0._next!!
        var he2 = he1._next!!

        var p0 = he0.head().pnt
        var p2 = he1.head().pnt

        var d2x = p2.x - p0.x
        var d2y = p2.y - p0.y
        var d2z = p2.z - p0.z

        normal.setZero()

        numVerts = 2

        while (he2 != he0) {
            var d1x = d2x
            var d1y = d2y
            var d1z = d2z

            p2 = he2.head().pnt
            d2x = p2.x - p0.x
            d2y = p2.y - p0.y
            d2z = p2.z - p0.z

            normal.x += d1y * d2z - d1z * d2y
            normal.y += d1z * d2x - d1x * d2z
            normal.z += d1x * d2y - d1y * d2x

            he1 = he2
            he2 = he2.getNext()
            numVerts++
        }
        area = normal.norm()
        normal.scale(1 / area)
    }

    fun computeNormalAndCentroid() {
        computeNormal(normal)
        computeCentroid(centroid)
        planeOffset = normal.dot(centroid)
        var numv = 0
        var he = he0
        do {
            numv++
            he = he.getNext()
        } while (he != he0)
        if (numv != numVerts) {
            throw InternalErrorException(
                "face " + getVertexString() + " numVerts=" + numVerts + " should be " + numv
            )
        }
    }

    fun computeNormalAndCentroid(minArea: Double) {
        computeNormal(normal, minArea)
        computeCentroid(centroid)
        planeOffset = normal.dot(centroid)
    }


    /**
     * Gets the i-th half-edge associated with the face.
     *
     * @param i the half-edge index, in the range 0-2.
     * @return the half-edge
     */
    fun getEdge(number: Int): HalfEdge {
        var i = number
        var he = he0
        while (i > 0) {
            he = he.getNext()
            i--
        }
        while (i < 0) {
            he = he.getPrev()
            i++
        }
        return he
    }

    fun getFirstEdge(): HalfEdge {
        return he0
    }

    /**
     * Finds the half-edge within this face which has
     * tail <code>vt</code> and head <code>vh</code>.
     *
     * @param vt tail point
     * @param vh head point
     * @return the half-edge, or null if none is found.
     */
    fun findEdge(vt: Vertex, vh: Vertex): HalfEdge? {
        var he = he0
        do {
            if (he.head() == vh && he.tail() == vt) {
                return he
            }
            he = he.getNext()
        } while (he != he0)

        return null
    }

    /**
     * Computes the distance from a point p to the plane of
     * this face.
     *
     * @param p the point
     * @return distance from the point to the plane
     */
    fun distanceToPlane(p: Point3d): Double {
        return normal.x * p.x + normal.y * p.y + normal.z * p.z - planeOffset
    }


    fun getVertexString(): String {
        var s: String = ""
        var he = he0
        do {
            if (s.isEmpty()) {
                s = he.head().index.toString()
            } else {
                s += " ${he.head().index}"
            }
            he = he.getNext()
        } while (he != he0)
        return s
    }

    fun getVertexIndices(idxs: IntArray) {
        var he = he0
        var i = 0
        do {
            idxs[i++] = he.head().index
            he = he.getNext()
        } while (he != he0)
    }

    private fun connectHalfEdges(
        hedgePrev: HalfEdge, hedge: HalfEdge
    ): Face? {
        var discardedFace: Face? = null

        if (hedgePrev.oppositeFace() == hedge.oppositeFace()) {
            // then there is a redundant edge that we can get rid off
            val oppFace = hedge.oppositeFace()
            var hedgeOpp: HalfEdge

            if (hedgePrev == he0) {
                he0 = hedge
            }
            if (oppFace!!.numVerts == 3) {
                // then we can get rid of the opposite face altogether
                hedgeOpp = hedge.getOpposite().getPrev().getOpposite()
                oppFace.mark = FaceMark.DELETED
                discardedFace = oppFace
            } else {
                hedgeOpp = hedge.getOpposite().getNext()
                if (oppFace.he0 == hedgeOpp._prev) {
                    oppFace.he0 = hedgeOpp
                }
                hedgeOpp._prev = hedgeOpp.getPrev()._prev
                hedgeOpp.getPrev()._next = hedgeOpp
            }
            hedge._prev = hedgePrev._prev
            hedge.getPrev()._next = hedge

            hedge._opposite = hedgeOpp
            hedgeOpp._opposite = hedge

            // oppFace was modified, so need to recompute
            oppFace.computeNormalAndCentroid()
        } else {
            hedgePrev._next = hedge
            hedge._prev = hedgePrev
        }
        return discardedFace
    }

    fun checkConsistency() {
        // do a sanity check on the face
        var hedge = he0
        var maxd = 0.0
        var numv = 0

        if (numVerts < 3) {
            throw InternalErrorException(
                "degenerate face: " + getVertexString()
            )
        }

        do {
            val hedgeOpp = hedge._opposite
            if (hedgeOpp == null) {
                throw InternalErrorException(
                    "face " + getVertexString() + ": " +
                            "unreflected half edge " + hedge.getVertexString()
                )
            } else if (hedgeOpp.getOpposite() != hedge) {
                throw InternalErrorException(
                    "face " + getVertexString() + ": " +
                            "opposite half edge " + hedgeOpp.getVertexString() +
                            " has opposite " +
                            hedgeOpp.getOpposite().getVertexString()
                )
            }
            if (hedgeOpp.head() != hedge.tail() ||
                hedge.head() != hedgeOpp.tail()
            ) {
                throw InternalErrorException(
                    "face " + getVertexString() + ": " +
                            "half edge " + hedge.getVertexString() +
                            " reflected by " + hedgeOpp.getVertexString()
                )
            }
            var oppFace = hedgeOpp._face
            if (oppFace == null) {
                throw InternalErrorException(
                    "face " + getVertexString() + ": " +
                            "no face on half edge " + hedgeOpp.getVertexString()
                )
            } else if (oppFace.mark == FaceMark.DELETED) {
                throw InternalErrorException(
                    "face " + getVertexString() + ": " +
                            "opposite face " + oppFace.getVertexString() +
                            " not on hull"
                )
            }
            val d = distanceToPlane(hedge.head().pnt).absoluteValue
            if (d > maxd) {
                maxd = d
            }
            numv++
            hedge = hedge.getNext()
        } while (hedge != he0)

        if (numv != numVerts) {
            throw InternalErrorException(
                "face " + getVertexString() + " numVerts=" + numVerts + " should be " + numv
            )
        }

    }

    fun mergeAdjacentFace(hedgeAdj: HalfEdge, discarded: ArrayList<Face?>): Int {
        val oppFace = hedgeAdj.oppositeFace()
        var numDiscarded = 0

        discarded[numDiscarded++] = oppFace!!
        oppFace.mark = FaceMark.DELETED

        var hedgeOpp = hedgeAdj.getOpposite()

        var hedgeAdjPrev = hedgeAdj.getPrev()
        var hedgeAdjNext = hedgeAdj.getNext()
        var hedgeOppPrev = hedgeOpp.getPrev()
        var hedgeOppNext = hedgeOpp.getNext()

        while (hedgeAdjPrev.oppositeFace() == oppFace) {
            hedgeAdjPrev = hedgeAdjPrev.getPrev()
            hedgeOppNext = hedgeOppNext.getNext()
        }

        while (hedgeAdjNext.oppositeFace() == oppFace) {
            hedgeOppPrev = hedgeOppPrev.getPrev()
            hedgeAdjNext = hedgeAdjNext.getNext()
        }

        var hedge: HalfEdge = hedgeOppNext
        while (hedge != hedgeOppPrev._next) {
            hedge._face = this
            hedge = hedge.getNext()
        }

        if (hedgeAdj == he0) {
            he0 = hedgeAdjNext
        }

        // handle the half edges at the head
        var discardedFace = connectHalfEdges(hedgeOppPrev, hedgeAdjNext)
        if (discardedFace != null) {
            discarded[numDiscarded++] = discardedFace
        }

        // handle the half edges at the tail
        discardedFace = connectHalfEdges(hedgeAdjPrev, hedgeOppNext)
        if (discardedFace != null) {
            discarded[numDiscarded++] = discardedFace
        }

        computeNormalAndCentroid()
        checkConsistency()

        return numDiscarded
    }

    fun areaSquared(hedge0: HalfEdge, hedge1: HalfEdge): Double {
        // return the squared area of the triangle defined
        // by the half edge hedge0 and the point at the
        // head of hedge1.

        val p0 = hedge0.tail()!!.pnt
        val p1 = hedge0.head().pnt
        val p2 = hedge1.head().pnt

        val dx1 = p1.x - p0.x
        val dy1 = p1.y - p0.y
        val dz1 = p1.z - p0.z

        val dx2 = p2.x - p0.x
        val dy2 = p2.y - p0.y
        val dz2 = p2.z - p0.z

        val x = dy1 * dz2 - dz1 * dy2
        val y = dz1 * dx2 - dx1 * dz2
        val z = dx1 * dy2 - dy1 * dx2

        return x * x + y * y + z * z
    }

    fun triangulate(newFaces: FaceList, minArea: Double) {
        if (numVerts < 4) {
            return
        }

        var v0 = he0.head()
        var prevFace: Face? = null

        var hedge = he0.getNext()
        var oppPrev = hedge._opposite
        var face0: Face? = null

        hedge = hedge.getNext()
        while (hedge != he0.getPrev()) {
            val face = createTriangle(v0, hedge.getPrev().head(), hedge.head(), minArea)
            face.he0.getNext().setOpposite(oppPrev!!)
            face.he0.getPrev().setOpposite(hedge._opposite!!)
            oppPrev = face.he0
            newFaces.add(face)
            if (face0 == null) {
                face0 = face
            }
            hedge = hedge.getNext()
        }
        hedge = HalfEdge(he0.getPrev().getPrev().head(), this)
        hedge.setOpposite(oppPrev!!)

        hedge._prev = he0
        hedge.getPrev()._next = hedge

        hedge._next = he0.getPrev()
        hedge.getNext()._prev = hedge

        computeNormalAndCentroid(minArea)
        checkConsistency()

        var face: Face? = face0
        while (face != null) {
            face.checkConsistency()
            face = face._next
        }
    }

    companion object {
        fun createTriangle(v0: Vertex, v1: Vertex, v2: Vertex): Face {
            return createTriangle(v0, v1, v2, 0.0)
        }

        /**
         * Constructs a triangular Face from vertices v0, v1, and v2.
         *
         * @param v0 first vertex
         * @param v1 second vertex
         * @param v2 third vertex
         */
        fun createTriangle(
            v0: Vertex, v1: Vertex, v2: Vertex,
            minArea: Double
        ): Face {
            val face = Face()
            val he0 = HalfEdge(v0, face)
            val he1 = HalfEdge(v1, face)
            val he2 = HalfEdge(v2, face)

            he0._prev = he2
            he0._next = he1
            he1._prev = he0
            he1._next = he2
            he2._prev = he1
            he2._next = he0
            face.he0 = he0

            // compute the normal and offset
            face.computeNormalAndCentroid(minArea)
            return face
        }

        fun create(vertices: List<Vertex>, indices: List<Int>): Face {
            val face = Face()
            var hePrev: HalfEdge? = null
            for (i in 0 until indices.size) {
                val he = HalfEdge(vertices[indices[i]], face)
                if (hePrev != null) {
                    he.setPrev(hePrev)
                    hePrev.setNext(he)
                } else {
                    face.he0 = he
                }
                hePrev = he
            }
            face.he0.setPrev(hePrev!!)
            hePrev.setNext(face.he0)

            // compute the normal and offset
            face.computeNormalAndCentroid()
            return face
        }
    }
}
