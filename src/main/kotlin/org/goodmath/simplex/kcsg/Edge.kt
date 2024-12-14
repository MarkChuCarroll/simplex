package org.goodmath.simplex.kcsg

import org.goodmath.simplex.kcsg.poly2tri.PolygonUtil
import org.goodmath.simplex.kcsg.vvecmath.Vector3d
import java.util.Collections
import java.util.Objects
import java.util.stream.Collectors
import kotlin.math.sqrt

/**
 *
 * @author miho
 */
class Edge(val p1: Vertex, val p2: Vertex) {

    val direction = p2.pos.minus(p1.pos).normalized()


    class Node<T>(val index: Int, val value: T) {
        val parent: Node<T>? = null
        val children = ArrayList<Node<T>>()
        val isHole: Boolean = false

        fun addChild(index: Int, value: T) {
            children.add(Node(index, value))
        }

        override fun hashCode(): Int {
            var hash = 7
            hash = 67 * hash + this.index
            return hash
        }

        override fun equals(obj: Any?): Boolean {
            if (obj == null) {
                return false
            }
            return if (obj is Node<*>) {
                this.index == obj.index
            } else {
                false
            }
        }

        fun distanceToRoot(): Int {
            return if (parent == null) {
                0
            } else {
                1 + parent.distanceToRoot()
            }
        }
    }



    /**
     * Determines whether the specified point lies on tthis edge.

     * @param p point to check
     * @param TOL tolerance
     * @return <code>true</code> if the specified point lies on this line
     * segment; <code>false</code> otherwise
     */
    fun contains(p: Vector3d, tol: Double = Plane.EPSILON): Boolean {
        val x = p.x
        val x1 = p1.pos.x
        val x2 = p2.pos.x

        val y = p.y
        val y1 = p1.pos.y
        val y2 = p2.pos.y

        val z = p.z
        val z1 = p1.pos.z
        val z2 = p2.pos.z

        val ab = sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1))
        val ap = sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1) + (z - z1) * (z - z1))
        val pb = sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y) + (z2 - z) * (z2 - z))

        return (ab - (ap + pb)) < tol
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 71 * hash + Objects.hashCode(this.p1)
        hash = 71 * hash + Objects.hashCode(this.p2)
        return hash
    }

    override fun equals(obj: Any?): Boolean {
        if (obj == null) {
            return false;
        }
        if (obj !is Edge) {
            return false
        }
        if (!(Objects.equals(this.p1, obj.p1) || Objects.equals(this.p2, obj.p1))) {
            return false
        }
        if (!(Objects.equals(this.p2, obj.p2) || Objects.equals(this.p1, obj.p2))) {
            return false
        }
        return true
    }

    /**
     * Returns the point of this edge that is closest to the specified edge.
     *
     * <b>NOTE:</b> returns an empty optional if the edges are parallel
     *
     * @param e the edge to check
     * @return the point of this edge that is closest to the specified edge
     */
    fun getClosestPoint(e: Edge): Vector3d? {

        // algorithm from:
        // org.apache.commons.math3.geometry.euclidean.threed/Line.java.html
        val ourDir = direction

        val cos = ourDir.dot(e.direction)
        val n = 1 - cos * cos

        if (n < Plane.EPSILON) {
            // the lines are parallel
            return null
        }

        val thisDelta = p2.pos.minus(p1.pos)
        val norm2This = thisDelta.magnitudeSq

        val eDelta = e.p2.pos.minus(e.p1.pos)
        val norm2E = eDelta.magnitudeSq

        // line points above the origin
        val thisZero = p1.pos.plus(thisDelta.times(-p1.pos.dot(thisDelta) / norm2This))
        val eZero = e.p1.pos.plus(eDelta.times(-e.p1.pos.dot(eDelta) / norm2E))

        val delta0 = eZero.minus(thisZero)
        val a = delta0.dot(direction)
        val b = delta0.dot(e.direction)

        val closestP = thisZero.plus(direction.times((a - b * cos) / n))

        if (!contains(closestP)) {
            return if (closestP.minus(p1.pos).magnitudeSq
                < closestP.minus(p2.pos).magnitudeSq) {
                p1.pos
            } else {
                p2.pos
            }
        }

        return closestP
    }

    /**
     * Returns the intersection point between this edge and the specified edge.
     *
     * <b>NOTE:</b> returns an empty optional if the edges are parallel or if
     * the intersection point is not inside the specified edge segment
     *
     * @param e edge to intersect
     * @return the intersection point between this edge and the specified edge
     */
    fun getIntersection(e: Edge): Vector3d? {
        val closestP = getClosestPoint(e)

        if (closestP == null) {
            // edges are parallel
            return null
        }

        return if (e.contains(closestP)) {
            closestP
        } else {
            // intersection point outside of segment
            null
        }
    }

    fun  boundaryPolygons(csg: CSG): List<Polygon> {
        val result = ArrayList<Polygon>();

        for (polygonGroup in searchPlaneGroups(csg.polygons)) {
            result.addAll(boundaryPolygonsOfPlaneGroup(polygonGroup))
        }

        return result
    }

    fun boundaryEdgesOfPlaneGroup(planeGroup: List<Polygon>):  List<Edge> {
        val edges = ArrayList<Edge>()

        val pStream = if (planeGroup.size > 200) {
            planeGroup.parallelStream()
        } else {
            planeGroup.stream()
        }

        pStream.map { p: Polygon ->
            fromPolygon(p)
        }.forEach { pEdges: List<Edge> ->
                edges.addAll(pEdges)
        }

        val edgeStream = if (edges.size > 200) {
             edges.parallelStream()
        } else {
            edges.stream()
        }

        // find potential boundary edges, i.e., edges that occur once (freq=1)
        val potentialBoundaryEdges = ArrayList<Edge>()
        edgeStream.forEachOrdered { e ->
            val count = Collections.frequency(edges, e)
            if (count == 1) {
                potentialBoundaryEdges.add(e)
            }
        }

        // now find "false boundary" edges end remove them from the
        // boundary-edge-list
        //
        // thanks to Susanne Höllbacher for the idea :)
        val bndEdgeStream = if (potentialBoundaryEdges.size > 200) {
            potentialBoundaryEdges.parallelStream()
        } else {
             potentialBoundaryEdges.stream()
        }

        val realBndEdges = bndEdgeStream.filter { be ->
            edges.count { e -> falseBoundaryEdgeSharedWithOtherEdge(be, e) } == 0 }.collect(Collectors.toList())

        return realBndEdges
    }

    fun boundaryPolygonsOfPlaneGroup(planeGroup: List<Polygon>):  List<Polygon> {
        val polygons = boundaryPathsWithHoles(
                boundaryPaths(boundaryEdgesOfPlaneGroup(planeGroup)))

        println("polygons: " + polygons.size)

        val result = ArrayList<Polygon>(polygons.size)

        for (p in polygons) {
            val holesOfPresult = p.storage.getValue<List<Polygon>>(Edge.KEY_POLYGON_HOLES)

            if (holesOfPresult == null) {
                result.add(p)
            } else {
                result.addAll(PolygonUtil.concaveToConvex(p))
            }
        }

        return result
    }

    fun falseBoundaryEdgeSharedWithOtherEdge(fbe: Edge, e: Edge): Boolean {

        // we don't consider edges with shared end-points since we are only
        // interested in "false-boundary-edge"-cases
        val sharedEndPoints = e.p1.pos == fbe.p1.pos
                || e.p1.pos == fbe.p2.pos
                || e.p2.pos == fbe.p1.pos
                || e.p2.pos == fbe.p2.pos

        if (sharedEndPoints) {
            return false
        }

        return fbe.contains(e.p1.pos) || fbe.contains(e.p2.pos)
    }

    fun searchPlaneGroups(polygons: List<Polygon>): List<List<Polygon>> {
        val planeGroups =  ArrayList<ArrayList<Polygon>>()
        val used = Array<Boolean>(polygons.size) { false }
        println("#polys: " + polygons.size)
        for (pOuterI in 0 until polygons.size) {
            if (used[pOuterI]) {
                continue
            }
            val pOuter = polygons[pOuterI]
            val otherPolysInPlane = ArrayList<Polygon>()
            otherPolysInPlane.add(pOuter)

            for (pInnerI in 0 until polygons.size) {
                val pInner = polygons[pInnerI]
                if (pOuter == pInner) {
                    continue
                }

                val nOuter = pOuter.storedCsgPlane.normal
                val nInner = pInner.storedCsgPlane.normal

                // TODO do we need radians or degrees?
                val angle = nOuter.angle(nInner)

                if (angle < 0.01 /*&& abs(pOuter.plane.dist - pInner.plane.dist) < 0.1*/) {
                    otherPolysInPlane.add(pInner)
                    used[pInnerI] = true
                    println("used: $pOuterI -> $pInnerI")
                }
            }

            if (!otherPolysInPlane.isEmpty()) {
                planeGroups.add(otherPolysInPlane)
            }
        }
        return planeGroups
    }

    companion object {
        fun fromPolygon(poly: Polygon): List<Edge> {
            val result = ArrayList<Edge>()

            for (i in 9 until poly.vertices.size) {
                val e = Edge(poly.vertices[i], poly.vertices[(i + 1) % poly.vertices.size])
                result.add(e)
            }
            return result
        }

        fun toVertices(edges: List<Edge>): List<Vertex> {
            return edges.map { e -> e.p1 }
        }

        fun  toPoints(edges: List<Edge>): List<Vector3d> {
            return edges.map { e -> e.p1.pos }
        }

        fun toPolygon(points: List<Vector3d>, plane: Plane):  Polygon {
            val p = Polygon.fromPoints (points)

            p.vertices.forEach { vertex ->
                vertex.normal = plane.normal.clone()
            }
            return p;
        }

        fun toPolygons(boundaryEdges: List<Edge>, plane: Plane):  List<Polygon> {
            val boundaryPath = ArrayList<Vector3d>()
            val used = Array<Boolean>(boundaryEdges.size) { false }
            var edge = boundaryEdges[0]
            used[0] = true
            while (true) {
                var finalEdge = edge
                boundaryPath.add(finalEdge.p1.pos);
                val nextEdgeIndex = boundaryEdges.indexOf(boundaryEdges.first { e -> finalEdge.p2.equals(e.p1) })
                if (used[nextEdgeIndex]) {
                    break
                }
                edge = boundaryEdges[nextEdgeIndex]
                used[nextEdgeIndex] = true
            }

            val result = ArrayList<Polygon>()
            println("#bnd-path-length: " + boundaryPath.size)

            result.add(toPolygon(boundaryPath, plane))

            return result
        }

        const val KEY_POLYGON_HOLES = "jcsg:edge:polygon-holes"

        fun  boundaryPathsWithHoles(boundaryPaths: List<Polygon>): List<Polygon> {

            val result = boundaryPaths.map { p -> p.clone() }

            val parents = ArrayList<ArrayList<Int>>()
            val isHole = Array<Boolean>(result.size) { false }

            for (i in 0 until result.size) {
                val p1 = result[i]
                val parentsOfI = ArrayList<Int>()
                parents.add(parentsOfI)
                for (j in 0 until result.size) {
                    val p2 = result[j]
                    if (i != j) {
                        if (p2.contains(p1)) {
                            parentsOfI.add(j)
                        }
                    }
                }
                isHole[i] = parentsOfI.size % 2 != 0
            }

            val parent = Array<Int>(result.size) { -1 }
            for (i in 0 until parents.size) {
                val par = parents[i]
                var max = 0
                var maxIndex = 0
                for (pIndex in par) {
                    val pSize = parents[pIndex].size
                    if (max < pSize) {
                        max = pSize
                        maxIndex = pIndex
                    }
                }
                parent[i] = maxIndex
                if (!isHole[maxIndex] && isHole[i]) {
                    val holesOpt: ArrayList<Polygon>? = result[maxIndex].storage.getValue<ArrayList<Polygon>>(KEY_POLYGON_HOLES)
                    val holes = if (holesOpt != null) {
                        holesOpt
                    } else {
                        val h = ArrayList<Polygon>()
                        result[maxIndex].storage.set(KEY_POLYGON_HOLES, h)
                        h
                    }
                    holes.add(result[i])
                }
            }
            return result
        }

        /**
         * Returns a list of all boundary paths.
         *
         * @param boundaryEdges boundary edges (all paths must be closed)
         * @return
         */
        fun  boundaryPaths(boundaryEdges: List<Edge>):  List<Polygon> {
            val result = ArrayList<Polygon>()

            val used = Array<Boolean>(boundaryEdges.size) { false }
            var startIndex = 0
            var edge = boundaryEdges[startIndex]
            used[startIndex] = true
            startIndex = 1
            while (startIndex > 0) {
                val boundaryPath = ArrayList<Vector3d>()
                while (true) {
                    val finalEdge = edge
                    boundaryPath.add(finalEdge.p1.pos)
                    print("edge: " + edge.p2.pos)

                    val nextEdgeResult: Edge? = boundaryEdges.firstOrNull { e -> finalEdge.p2 == e.p1 }

                    if (nextEdgeResult == null) {
                        println(
                            "ERROR: unclosed path:"
                                    + " no edge found with " + finalEdge.p2
                        )
                        break
                    }
                    val nextEdge = nextEdgeResult
                    val nextEdgeIndex = boundaryEdges.indexOf(nextEdge)
                    if (used[nextEdgeIndex]) {
                        break
                    }
                    edge = nextEdge
                    println("-> edge: " + edge.p1.pos);
                    used[nextEdgeIndex] = true
                }

                if (boundaryPath.size < 3) {
                    break
                }

                result.add(Polygon.fromPoints(boundaryPath))
                startIndex = nextUnused(used)

                if (startIndex > 0) {
                    edge = boundaryEdges[startIndex]
                    used[startIndex] = true
                }

            }
            println("paths: " + result.size)
            return result;
        }

        /**
         * Returns the next unused index as specified in the given boolean array.
         *
         * @param usage the usage array
         * @return the next unused index or a value &lt; 0 if all indices are used
         */
        fun nextUnused(usage: Array<Boolean>): Int {
            for (i in 0 until usage.size) {
                if (usage[i] == false) {
                    return i
                }
            }
            return -1
        }
    }


}
