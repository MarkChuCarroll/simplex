package org.goodmath.simplex.kcsg

import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Holds a node in a BSP tree. A BSP tree is built from a collection of polygons
 * by picking a polygon to split along. That polygon (and all other coplanar
 * polygons) are added directly to that node and the other polygons are added to
 * the front and/or back subtrees. This is not a leafy BSP tree since there is
 * no distinction between internal and leaf nodes.
 *
 * Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
class Node(var polygons: MutableList<Polygon> = ArrayList()): Cloneable {

    /**
     * Plane used for BSP.
     */
    var plane: Plane? = null
    /**
     * Polygons in front of the plane.
     */
    var front: Node? = null

    /**
     * Polygons in back of the plane.
     */
    var back: Node? = null

    init {
        if (polygons.isNotEmpty()) {
            build(polygons)
        }
    }


    override fun clone(): Node {
        val node = Node()
        node.plane = this.plane?.clone()
        node.front = this.front?.clone()
        node.back = this.back?.clone()

        val polygonStream: Stream<Polygon> =
            if (polygons.size > 200) {
                 polygons.parallelStream()
            } else {
                 polygons.stream()
            }

        node.polygons = polygonStream.map { p -> p.clone() }.collect(Collectors.toList())
        return node
    }

    /**
     * Converts solid space to empty space and vice verca.
     */
    fun invert() {
        val polygonStream: Stream<Polygon> = if (polygons.size > 200) {
             polygons.parallelStream()
        } else {
             polygons.stream()
        }

        polygonStream.forEach { polygon -> polygon.flip() }

        if (plane == null && polygons.isNotEmpty()) {
            plane = polygons[0].csgPlane.clone()
        } else if (plane == null && polygons.isEmpty()) {
            System.err.println("Please fix me! I don't know what to do?");
            // throw new RuntimeException("Please fix me! I don't know what to do?");
            return
        }

        plane?.flip()

        this.front?.invert()
        this.back?.invert()

        val temp = this.front
        this.front = this.back
        this.back = temp
    }

    /**
     * Recursively removes all polygons in the {@link polygons} list that are
     * contained within this BSP tree.
     *
     * <b>Note:</b> polygons are splitted if necessary.
     *
     * @param polygons the polygons to clip
     *
     * @return the clipped list of polygons
     */
    private fun clipPolygons(polygons: List<Polygon>): ArrayList<Polygon> {
        val pl = plane
        if (pl == null) {
            return ArrayList(polygons)
        }

        var frontP: ArrayList<Polygon> = ArrayList()
        var backP: ArrayList<Polygon> = ArrayList()
        for (polygon in polygons) {
            pl.splitPolygon(polygon, frontP, backP, frontP, backP)
        }
        frontP = front?.clipPolygons(frontP) ?: ArrayList()
        backP = back?.clipPolygons(backP) ?: ArrayList()

        frontP.addAll(backP);
        return frontP
    }

    // Remove all polygons in this BSP tree that are inside the other BSP tree
    // `bsp`.
    /**
     * Removes all polygons in this BSP tree that are inside the specified BSP
     * tree ({@code bsp}).
     *
     * <b>Note:</b> polygons are splitted if necessary.
     *
     * @param bsp bsp that shall be used for clipping
     */
    fun clipTo(bsp: Node) {
        polygons = bsp.clipPolygons(polygons)
        front?.clipTo(bsp)
        back?.clipTo(bsp)
    }

    /**
     * Returns a list of all polygons in this BSP tree.
     *
     * @return a list of all polygons in this BSP tree
     */
    fun allPolygons(): List<Polygon> {
        val localPolygons = ArrayList<Polygon>(this.polygons)
        localPolygons.addAll(front?.allPolygons() ?: emptyList())
        localPolygons.addAll(back?.allPolygons() ?: emptyList())
        return localPolygons
    }

    /**
     * Build a BSP tree out of {@code polygons}. When called on an existing
     * tree, the new polygons are filtered down to the bottom of the tree and
     * become new nodes there. Each set of polygons is partitioned using the
     * first polygon (no heuristic is used to pick a good split).
     *
     * @param polygons polygons used to build the BSP
     */
    fun build(ps: List<Polygon>) {
        if (ps.isEmpty()) return
        if (plane == null) {
            plane = ps[0].csgPlane.clone()
        }
        polygons = ps.filter { p-> p.isValid }.distinct().toMutableList()

        val frontP = ArrayList<Polygon>()
        val backP = ArrayList<Polygon>()

        // parallel version does not work here
        ps.forEach { polygon ->
            plane!!.splitPolygon(
                polygon, polygons, polygons, frontP, backP
            )
        }

        if (frontP.isNotEmpty()) {
            if (front == null) {
                front = Node()
            }
            front!!.build(frontP)
        }
        if (backP.isNotEmpty()) {
            if (this.back == null) {
                this.back = Node()
            }
            this.back!!.build(backP)
        }
    }
}
