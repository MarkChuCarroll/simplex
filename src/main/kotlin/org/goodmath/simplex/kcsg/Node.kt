package org.goodmath.simplex.kcsg

import java.util.stream.Collectors
import java.util.stream.Stream


/**
 * Holds a node in a BSP tree. A BSP tree is built from a collection of polygons
 * by picking a polygon to split along. That polygon (and all other coplanar
 * polygons) are added directly to that node and the other polygons are added to
 * the front and/or back subtrees. This is not a leafy BSP tree since there is
 * no distinction between internal and leaf nodes.
 */
class Node(polys: List<Polygon> = emptyList<Polygon>()): Cloneable {
    val polygons = ArrayList<Polygon>()
    init {
        if (polys.isNotEmpty()) {
            this.build(polys)
        }
    }

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


    public override fun clone(): Node {
        val node = Node()
        node.plane = plane?.clone()
        node.front = this.front?.clone()
        node.back = this.back?.clone()
        val polygonStream =
            if (polygons.size > 200) {
                polygons.parallelStream()
            } else {
                polygons.stream()
            }

        node.polygons.clear()
        node.polygons.addAll(ArrayList(polygonStream.map { p ->
            p.clone()
        }.collect(Collectors.toList())))
        return node
    }

    fun polygonsToStream(): Stream<Polygon> {
        return if (polygons.size > 200) {
            polygons.parallelStream()
        } else {
            polygons.stream()
        }
    }

    /**
     * Converts solid space to empty space and vice versa.
     */
    fun invert() {
        val polygonStream = polygonsToStream()

        polygonStream.forEach { polygon ->
            polygon.flip()
        }

        if (plane == null && !polygons.isEmpty()) {
            this.plane = polygons[0].storedCsgPlane.clone()
        } else if (plane == null && polygons.isEmpty()) {
            System.err.println("Please fix me! I don't know what to do?")
            return
        }

        plane!!.flip()

        front?.invert()
        back?.invert()

        val temp = front
        front = back
        back = temp
    }

    /**
     * Recursively removes all polygons in the {@link polygons} list that are
     * contained within this BSP tree.
     *
     * <b>Note:</b> polygons are split if necessary.
     *
     * @param polygons the polygons to clip
     *
     * @return the clipped list of polygons
     */
    fun clipPolygons(polygons: List<Polygon>): ArrayList<Polygon> {

        if (this.plane == null) {
            return ArrayList(polygons)
        }

        var frontP = ArrayList<Polygon>()
        var backP = ArrayList<Polygon>()

        for (polygon in polygons) {
            plane!!.splitPolygon(polygon, frontP, backP, frontP, backP)
        }
        if (front != null) {
            frontP = front!!.clipPolygons(frontP)
        }
        backP = if (this.back != null) {
            back!!.clipPolygons(backP)
        } else {
            ArrayList(0)
        }

        frontP.addAll(backP)
        return frontP
    }

    // Remove all polygons in this BSP tree that are inside the other BSP tree
    // `bsp`.
    /**
     * Removes all polygons in this BSP tree that are inside the specified BSP
     * tree ({@code bsp}).
     *
     * <b>Note:</b> polygons are split if necessary.
     *
     * @param bsp bsp that shall be used for clipping
     */
    fun clipTo(bsp: Node) {
        polygons.addAll(bsp.clipPolygons(this.polygons))
        front?.clipTo(bsp)
        back?.clipTo(bsp)
    }

    /**
     * Returns a list of all polygons in this BSP tree.
     *
     * @return a list of all polygons in this BSP tree
     */
    fun allPolygons(): List<Polygon> {
        val localPolygons = ArrayList(this.polygons)
        front?.let { localPolygons.addAll(it.allPolygons()) }
        back?.let { localPolygons.addAll(it.allPolygons()) }
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
    fun build(polygons: List<Polygon>) {
        if (polygons.isEmpty()) {
            return
        }
        if (this.plane == null) {
            this.plane = polygons[0].storedCsgPlane.clone()
        }

        val builtPolygons = polygons.filter { p ->
            p.valid
        }.distinct()

        val frontP = ArrayList<Polygon>()
        val backP = ArrayList<Polygon>()

        // parallel version does not work here
        builtPolygons.forEach { polygon ->
            plane!!.splitPolygon(
                polygon, this.polygons, this.polygons, frontP, backP)
        }

        if (frontP.isNotEmpty()) {
            if (front == null) {
                front = Node()
            }
            front!!.build(frontP)
        }
        if (backP.isNotEmpty()) {
            if (back == null) {
                back = Node()
            }
            back!!.build(backP)
        }
    }
}
