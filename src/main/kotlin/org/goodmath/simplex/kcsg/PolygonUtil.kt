package org.goodmath.simplex.kcsg

import eu.mihosoft.jcsg.ext.org.poly2tri.PolygonPoint
import eu.mihosoft.jcsg.ext.org.poly2tri.Poly2Tri
import org.goodmath.simplex.vvecmath.Vector3d
import eu.mihosoft.jcsg.ext.org.poly2tri.Polygon as PTPolygon

object PolygonUtil {

    /**
     * Converts a CSG polygon to a poly2tri polygon (including holes)
     * @param polygon the polygon to convert
     * @return a CSG polygon to a poly2tri polygon (including holes)
     */
    fun fromCSGPolygon(polygon: Polygon): PTPolygon {
        // convert polygon
        val points = ArrayList<PolygonPoint>()
        for (v in polygon.vertices) {
            val vp = PolygonPoint(v.pos.x, v.pos.y, v.pos.z)
            points.add(vp)
        }
        val result = PTPolygon(points)
        // convert holes
        val holesOfP = polygon.storage.getValue<List<Polygon>>(Edge.KEY_POLYGON_HOLES)
        holesOfP?.forEach { hP ->
            result.addHole(fromCSGPolygon(hP))
        }

        return result
    }

    fun concaveToConvex(concave: Polygon): List<Polygon> {
        val result = ArrayList<Polygon>()
        val normal = concave.vertices[0].normal.clone()
        val cw = !Extrude.isCCW(concave)
        val p = fromCSGPolygon(concave)
        Poly2Tri.triangulate(p)
        val triangles = p.triangles
        var triPoints = ArrayList<Vertex>()
        for (t in triangles) {
            var counter = 0
            for (tp in t.points) {
                triPoints.add(
                    Vertex(
                        Vector3d.xyz(tp.getX(), tp.getY(), tp.getZ()),
                        normal
                    )
                )

                if (counter == 2) {
                    if (!cw) {
                        triPoints.reverse()
                    }
                    val poly = Polygon(triPoints, concave.storage)
                    result.add(poly)
                    counter = 0
                    triPoints = ArrayList()

                } else {
                    counter++
                }
            }
        }
        return result
    }
}
