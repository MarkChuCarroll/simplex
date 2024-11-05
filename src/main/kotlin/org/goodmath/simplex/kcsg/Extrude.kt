package org.goodmath.simplex.kcsg

import org.goodmath.simplex.kcsg.poly2tri.PolygonUtil
import org.goodmath.simplex.vvecmath.Transform
import org.goodmath.simplex.vvecmath.Vector3d


/*
* Extrudes concave and convex polygons.
*
* @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
* Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
*/
object Extrude {
    /**
     * Extrudes the specified path (convex or concave polygon without holes or
     * intersections, specified in CCW) into the specified direction.
     *
     * @param dir direction
     * @param points path (convex or concave polygon without holes or
     * intersections)
     *
     * @return a CSG object that consists of the extruded polygon
     */
    fun points(dir: Vector3d, points: List<Vector3d>): CSG {
        return extrude(dir, Polygon.fromPoints(toCCW(points)))
    }

    /**
     * Combines two polygons into one CSG object. Polygons p1 and p2 are treated as top and
     * bottom of a tube segment with p1 and p2 as the profile. <b>Note:</b> both polygons must have the
     * same number of vertices. This method does not guarantee intersection-free CSGs. It is in the
     * responsibility of the caller to ensure that the orientation of p1 and p2 allow for
     * intersection-free combination of both.
     *
     * @param p1 first polygon
     * @param p2 second polygon
     * @param bottom defines whether to close the bottom of the tube
     * @param top defines whether to close the top of the tube
     * @return List of polygons
     */
    fun combine(p1: Polygon, p2: Polygon, bottom: Boolean = true, top: Boolean = true): List<Polygon> {
        val newPolygons = ArrayList<Polygon>()

        if (p1.vertices.size != p2.vertices.size) {
            throw RuntimeException("Polygons must have the same number of vertices")
        }

        val numVertices = p1.vertices.size
        if (bottom) {
            newPolygons.add(p1.flipped())
        }

        for (i in 0 until numVertices) {
            val nexti = (i + 1) % numVertices
            val bottomV1 = p1.vertices[i].pos
            val topV1 = p2.vertices[i].pos
            val bottomV2 = p1.vertices[nexti].pos
            val topV2 = p2.vertices[nexti].pos

            var pPoints = listOf(bottomV2, topV2, topV1)
            newPolygons.add(Polygon.fromPoints(pPoints, p1.storage))
            pPoints = listOf(bottomV2,  topV1, bottomV1)
            newPolygons.add(Polygon.fromPoints(pPoints, p1.storage))
        }

        if (top) {
            newPolygons.add(p2)
        }

        return newPolygons
    }

    private fun extrude(dir: Vector3d, polygon1: Polygon): CSG {
        val newPolygons = ArrayList<Polygon>()

        if (dir.z < 0) {
            throw IllegalArgumentException("z < 0 currently not supported for extrude: $dir")
        }

        newPolygons.addAll(PolygonUtil.concaveToConvex(polygon1))
        var polygon2 = polygon1.translated(dir)

        val numvertices = polygon1.vertices.size
        for (i in 0 until numvertices) {
            val nexti = (i + 1) % numvertices
            val bottomV1 = polygon1.vertices[i].pos
            val topV1 = polygon2.vertices[i].pos
            val bottomV2 = polygon1.vertices[nexti].pos
            val topV2 = polygon2.vertices[nexti].pos
            var pPoints = listOf(bottomV2, topV2, topV1, bottomV1)
            newPolygons.add(Polygon.fromPoints(pPoints, polygon1.storage))
        }

        polygon2 = polygon2.flipped()
        val topPolygons = PolygonUtil.concaveToConvex(polygon2)
        newPolygons.addAll(topPolygons)
        return CSG.fromPolygons(newPolygons)
    }

    private fun extrude(dir: Vector3d, polygon1: Polygon, top: Boolean, bottom: Boolean):  List<Polygon> {
        val newPolygons = ArrayList<Polygon>()
        if (bottom) {
            newPolygons.addAll(PolygonUtil.concaveToConvex(polygon1))
        }
        var polygon2 = polygon1.translated(dir)
        var rot = Transform.unity()
        val a = polygon2.plane.normal.normalized()
        val b = dir.normalized()
        val c = a.crossed(b)
        val l = c.magnitude() // sine of angle
        if (l > 1e-9) {
            val axis = c.times(1.0 / l)
            val angle = a.angle(b)

            var sx = 0.0
            var sy = 0.0
            var sz = 0.0

            val n = polygon2.vertices.size

            for (v in polygon2.vertices) {
                sx += v.pos.x
                sy += v.pos.y
                sz += v.pos.z
            }
            val center = Vector3d.xyz(sx / n, sy / n, sz / n)
            rot = rot.rot(center, axis, angle * Math.PI / 180.0)
            for (v in polygon2.vertices) {
                v.pos = rot.transform(v.pos)
            }
        }

        val numvertices = polygon1.vertices.size
        for (i in 0 until numvertices) {
            val nexti = (i + 1) % numvertices
            val bottomV1 = polygon1.vertices[i].pos
            val topV1 = polygon2.vertices[i].pos
            val bottomV2 = polygon1.vertices[nexti].pos
            val topV2 = polygon2.vertices[nexti].pos
            var pPoints = listOf(bottomV2, topV2, topV1, bottomV1)
            newPolygons.add(Polygon.fromPoints(pPoints, polygon1.storage))
        }
        polygon2 = polygon2.flipped()
        val topPolygons = PolygonUtil.concaveToConvex(polygon2)
        if (top) {
            newPolygons.addAll(topPolygons)
        }
        return newPolygons
    }

    fun toCCW(points: List<Vector3d>): List<Vector3d> {
        val result = ArrayList<Vector3d>(points)
        if (!isCCW(Polygon.fromPoints(result))) {
            result.reverse()
        }
        return result
    }

    fun toCW(points: List<Vector3d>): List<Vector3d> {
        val result = ArrayList<Vector3d>(points)
        if (isCCW(Polygon.fromPoints(result))) {
            result.reverse()
        }
        return result
    }

    /**
     * Indicates whether the specified polygon is defined counter-clockwise.
     * @param polygon polygon
     * @return {@code true} if the specified polygon is defined counter-clockwise;
     * {@code false} otherwise
     */
    fun isCCW(polygon: Polygon): Boolean {
        // thanks to Sepp Reiter for explaining me the algorithm!
        if (polygon.vertices.size < 3) {
            throw IllegalArgumentException("Only polygons with at least 3 vertices are supported!")
        }
        // search highest left vertex
        var highestLeftVertexIndex = 0
        var highestLeftVertex = polygon.vertices[0]
        for (i in  0 until polygon.vertices.size) {
            val v = polygon.vertices[i]
            if (v.pos.y > highestLeftVertex.pos.y) {
                highestLeftVertex = v
                highestLeftVertexIndex = i
            } else if (v.pos.y == highestLeftVertex.pos.y
                && v.pos.x < highestLeftVertex.pos.x) {
                highestLeftVertex = v
                highestLeftVertexIndex = i
            }
        }

        // determine next and previous vertex indices
        var nextVertexIndex = (highestLeftVertexIndex + 1) % polygon.vertices.size
        var prevVertexIndex = highestLeftVertexIndex - 1
        if (prevVertexIndex < 0) {
            prevVertexIndex = polygon.vertices.size - 1
        }
        val nextVertex = polygon.vertices.get(nextVertexIndex)
        val prevVertex = polygon.vertices.get(prevVertexIndex)

        // edge 1
        val a1 = normalizedX(highestLeftVertex.pos, nextVertex.pos)

        // edge 2
        val a2 = normalizedX(highestLeftVertex.pos, prevVertex.pos)

        // select vertex with lowest x value
        var selectedVIndex = if (a2 > a1) {
            nextVertexIndex
        } else {
            prevVertexIndex
        }

        if (selectedVIndex == 0
            && highestLeftVertexIndex == polygon.vertices.size - 1) {
            selectedVIndex = polygon.vertices.size
        }

        if (highestLeftVertexIndex == 0
            && selectedVIndex == polygon.vertices.size - 1) {
            highestLeftVertexIndex = polygon.vertices.size
        }

        // indicates whether edge points from highestLeftVertexIndex towards
        // the sel index (ccw)
        return selectedVIndex > highestLeftVertexIndex
    }

    private fun normalizedX(v1: Vector3d, v2: Vector3d): Double {
        val v2MinusV1 = v2.minus(v1)
        return v2MinusV1.divided(v2MinusV1.magnitude()).times(Vector3d.X_ONE).x
    }
}
