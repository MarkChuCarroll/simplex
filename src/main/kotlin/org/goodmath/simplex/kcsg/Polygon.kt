package org.goodmath.simplex.kcsg

import org.goodmath.simplex.kcsg.poly2tri.PolygonUtil
import org.goodmath.simplex.kcsg.vvecmath.Transform
import org.goodmath.simplex.kcsg.vvecmath.Vector3d
import kotlin.math.absoluteValue
import org.goodmath.simplex.kcsg.vvecmath.Plane as VVPlane


/**
 * Represents a convex polygon.
 *
 * Each convex polygon has a {@code shared} property, which is shared between
 * all polygons that are clones of each other or where split from the same
 * polygon. This can be used to define per-polygon properties (such as surface
 * color).
 */
class Polygon(val vertices: ArrayList<Vertex>, var storage: PropertyStorage = PropertyStorage()): Cloneable {

    /**
     * Shared property (can be used for shared color etc.).
     */

    /**
     * Plane defined by this polygon.
     *
     * <b>Note:</b> uses first three vertices to define the plane.
     */
    var storedCsgPlane: Plane = Plane.createFromPoints(vertices[0].pos,
        vertices[1].pos,
        vertices[2].pos)

    var plane: VVPlane = VVPlane.fromPointAndNormal(centroid, storedCsgPlane.normal)

    var valid = true

    init {
        validateAndInit(vertices)
    }


    fun validateAndInit(vertices: List<Vertex>) {
        for (v in vertices) {
            v.normal = storedCsgPlane.normal
        }
        if (Vector3d.ZERO == storedCsgPlane.normal) {
            valid = false
            System.err.println(
                "Normal is zero! Probably, duplicate points have been specified!\n\n" + toStlString())
        }

        if (vertices.size < 3) {
            throw RuntimeException(
                    "Invalid polygon: at least 3 vertices expected, got: "
                            + vertices.size);
        }
    }

    public override fun clone(): Polygon {
        val newVertices = ArrayList<Vertex>()
        this.vertices.forEach { vertex ->
            newVertices.add(vertex.clone())
        }
        return Polygon(newVertices, storage)
    }

    /**
     * Flips this polygon.
     *
     * @return this polygon
     */
    fun flip(): Polygon {
        vertices.forEach { vertex ->
            vertex.flip()
        }
        vertices.reverse()

        storedCsgPlane.flip()
        return this
    }

    /**
     * Returns a flipped copy of this polygon.
     *
     * <b>Note:</b> this polygon is not modified.
     *
     * @return a flipped copy of this polygon
     */
    fun flipped(): Polygon {
        return clone().flip()
    }

    /**
     * Returns this polygon in STL string format.
     *
     * @return this polygon in STL string format
     */
    fun toStlString(): String {
        return toStlString(StringBuilder()).toString()
    }

    /**
     * Returns this polygon in STL string format.
     *
     * @param sb string builder
     *
     * @return the specified string builder
     */
    fun toStlString(sb: StringBuilder): StringBuilder {

        if (this.vertices.size >= 3) {
            // TODO: improve the triangulation?
            //
            // STL requires triangular polygons.
            // If our polygon has more vertices, create
            // multiple triangles:
            val firstVertexStl = this.vertices[0].toStlString();
            for (i in 0 until vertices.size - 2) {
                sb.
                append("  facet normal ").append(this.storedCsgPlane.normal.toStlString()).append("\n").
                append("    outer loop\n").
                append("      ").append(firstVertexStl).append("\n").
                append("      ")
                this.vertices[i + 1].toStlString(sb).append("\n").
                append("      ")
                this.vertices[i + 2].toStlString(sb).append("\n").
                append("    endloop\n").
                append("  endfacet\n")
            }
        }

        return sb
    }

    /**
     * Returns a triangulated version of this polygon.
     *
     * @return triangles
     */
    fun toTriangles(): List<Polygon> {
        val result = ArrayList<Polygon>()

        if (this.vertices.size >= 3) {

            // TODO: improve the triangulation?
            //
            // If our polygon has more vertices, create
            // multiple triangles:
            val firstVertexStl = this.vertices[0]
            for (i in 0 until vertices.size - 2) {
                // create triangle
                val polygon = Polygon.fromPoints(
                    listOf(firstVertexStl.pos,
                        this.vertices[i + 1].pos,
                        this.vertices[i + 2].pos
                ))
                result.add(polygon)
            }
        }

        return result
    }

    /**
     * Translates this polygon.
     *
     * @param v the vector that defines the translation
     * @return this polygon
     */
    fun translate(v: Vector3d): Polygon {
        vertices.forEach { vertex ->
            vertex.pos = vertex.pos.plus(v)
        }

        val a = this.vertices[0].pos
        val b = this.vertices[1].pos
        val c = this.vertices[2].pos

        // TODO plane update correct?
        this.storedCsgPlane.normal = b.minus(a).crossed(c.minus(a))

        this.plane = VVPlane.fromPointAndNormal(centroid, storedCsgPlane.normal)

        return this
    }

    /**
     * Returns a translated copy of this polygon.
     *
     * <b>Note:</b> this polygon is not modified
     *
     * @param v the vector that defines the translation
     *
     * @return a translated copy of this polygon
     */
    fun translated(v: Vector3d): Polygon {
        return clone().translate(v)
    }

    /**
     * Applies the specified transformation to this polygon.
     *
     * <b>Note:</b> if the applied transformation performs a mirror operation
     * the vertex order of this polygon is reversed.
     *
     * @param transform the transformation to apply
     *
     * @return this polygon
     */
    fun transform(transform: Transform): Polygon {

        this.vertices.forEach {
            v ->
            v.transform(transform)
        }


        val a = this.vertices[0].pos
        val b = this.vertices[1].pos
        val c = this.vertices[2].pos

        storedCsgPlane.normal = b.minus(a).crossed(c.minus(a)).normalized()
        storedCsgPlane.dist = storedCsgPlane.normal.dot(a)

        this.plane = VVPlane.fromPointAndNormal(centroid, storedCsgPlane.normal)

        vertices.forEach { vertex ->
            vertex.normal = plane.normal
        }

        if (transform.isMirror()) {
            // the transformation includes mirroring. flip polygon
            flip()

        }
        return this
    }

    /**
     * Returns a transformed copy of this polygon.
     *
     * <b>Note:</b> if the applied transformation performs a mirror operation
     * the vertex order of this polygon is reversed.
     *
     * <b>Note:</b> this polygon is not modified
     *
     * @param transform the transformation to apply
     * @return a transformed copy of this polygon
     */
    fun transformed(transform: Transform): Polygon {
        return clone().transform(transform)
    }



    /**
     * Returns the bounds of this polygon.
     *
     * @return bouds of this polygon
     */
    fun getBounds(): Bounds {
        val minX = vertices.minOf { it.pos.x }
        val maxX = vertices.maxOf { it.pos.x }
        val minY = vertices.minOf { it.pos.y }
        val maxY = vertices.maxOf { it.pos.y }
        val minZ = vertices.minOf { it.pos.z }
        val maxZ = vertices.maxOf { it.pos.z }
        return Bounds(
            Vector3d.xyz(minX, minY, minZ),
            Vector3d.xyz(maxX, maxY, maxZ))
    }

    val centroid: Vector3d
        get() {
            var sum = Vector3d.zero()

            for (v in vertices) {
                sum = sum.plus(v.pos)
            }
            return sum.times(1.0 / vertices.size)
        }

    /**
     * Indicates whether the specified point is contained within this polygon.
     *
     * @param p point
     * @return {@code true} if the point is inside the polygon or on one of the
     * edges; {@code false} otherwise
     */
    fun contains(p: Vector3d): Boolean {
        // P not on the plane
        if (plane.distance(p) > Plane.EPSILON) {
            return false
        }

        // if P is on one of the vertices, return true
        for (i in 0 until vertices.size) {
            if (p.minus(vertices[i].pos).magnitude < Plane.EPSILON) {
                return true
            }
        }

        // if P is on the plane, we proceed with projection to XY plane
        //
        // P1--P------P2
        //     ^
        //     |
        // P is on the segment if( dist(P1,P) + dist(P2,P) - dist(P1,P2) < TOL)
        for (i in 0 until vertices.size - 1) {
            val p1 = vertices[i].pos
            val p2 = vertices[i + 1].pos

            val onASegment = p1.minus(p).magnitude + p2.minus(p).magnitude - p1.minus(p2).magnitude < Plane.EPSILON

            if (onASegment) {
                return true
            }
        }

        // find projection plane
        // we start with XY plane
        var coordIndex1 = 0
        var coordIndex2 = 1

        val orthogonalToXY = VVPlane.XY_PLANE.normal.dot(plane.normal).absoluteValue < Plane.EPSILON

        var foundProjectionPlane = false
        if (!orthogonalToXY && !foundProjectionPlane) {
            coordIndex1 = 0
            coordIndex2 = 1
            foundProjectionPlane = true
        }

        val orthogonalToXZ = VVPlane.XZ_PLANE.normal.dot(plane.normal).absoluteValue < Plane.EPSILON

        if (!orthogonalToXZ && !foundProjectionPlane) {
            coordIndex1 = 0
            coordIndex2 = 2
            foundProjectionPlane = true
        }

        val orthogonalToYZ = VVPlane.YZ_PLANE.normal.dot(plane.normal).absoluteValue < Plane.EPSILON

        if (!orthogonalToYZ && !foundProjectionPlane) {
            coordIndex1 = 1
            coordIndex2 = 2
            foundProjectionPlane = true
        }

        // see from http://www.java-gaming.org/index.php?topic=26013.0
        // see http://alienryderflex.com/polygon/
        // see http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
        var j = vertices.size - 1
        var oddNodes = false
        var x = p.get(coordIndex1)
        var y = p.get(coordIndex2)
        for (i in 0 until vertices.size) {
            val xi = vertices[i].pos.get(coordIndex1)
            val yi = vertices[i].pos.get(coordIndex2)
            val xj = vertices[j].pos.get(coordIndex1)
            val yj = vertices[j].pos.get(coordIndex2)
            if ((yi < y && yj >= y
                        || yj < y && yi >= y)
                && (xi <= x || xj <= x)) {
                oddNodes = oddNodes.xor(xi + (y - yi) / (yj - yi) * (xj - xi) < x)
            }
            j = i
        }
        return oddNodes;

    }

    fun contains(p: Polygon): Boolean {
        for (v in p.vertices) {
            if (!contains(v.pos)) {
                return false
            }
        }
        return true
    }

    companion object {
        /**
         * Decomposes the specified concave polygon into convex polygons.
         *
         * @param points the points that define the polygon
         * @return the decomposed concave polygon (list of convex polygons)
         */
        fun fromConcavePoints(points: List<Vector3d>): List<Polygon> {
            val p = fromPoints(points)
            return PolygonUtil.concaveToConvex(p)
        }

        /**
         * Creates a polygon from the specified point list.
         *
         * @param points the points that define the polygon
         * @param shared shared property storage
         * @return a polygon defined by the specified point list
         */
        fun fromPoints(points: List<Vector3d>,
                       shared: PropertyStorage):  Polygon {
            return fromPoints(points, shared, null)
        }

        /**
         * Creates a polygon from the specified point list.
         *
         * @param points the points that define the polygon
         * @return a polygon defined by the specified point list
         */
        fun fromPoints(points: List<Vector3d>): Polygon {
            return fromPoints(points, PropertyStorage(), null)
        }


        /**
         * Creates a polygon from the specified point list.
         *
         * @param points the points that define the polygon
         * @param shared
         * @param plane may be null
         * @return a polygon defined by the specified point list
         */
        fun fromPoints(points: List<Vector3d>, shared: PropertyStorage, plane: Plane?):
                 Polygon {

            var normal = if (plane != null) {
                plane.normal.clone()
            } else {
                null
            }

            if (normal == null) {
                normal = Plane.createFromPoints(
                    points[0],
                    points[1],
                    points[2]
                ).normal
            }

            val vertices = ArrayList<Vertex>()

            for (p in points) {
                val vec = p.clone()
                val vertex = Vertex(vec, normal)
                vertices.add(vertex)
            }

            return Polygon(vertices, shared)
        }
    }
}

