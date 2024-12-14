package org.goodmath.simplex.kcsg.poly2tri

import org.goodmath.simplex.kcsg.Extrude
import org.goodmath.simplex.kcsg.Vertex
import org.goodmath.simplex.kcsg.vvecmath.Vector3d
import org.goodmath.simplex.kcsg.Polygon as CsgPolygon
import org.goodmath.simplex.kcsg.Edge as CsgEdge
typealias TriPolygon = org.goodmath.simplex.kcsg.poly2tri.Polygon

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
object PolygonUtil {

    /**
     * Converts a CSG polygon to a poly-to-tri polygon (including holes)
     * @param polygon the polygon to convert
     * @return a CSG polygon to a poly-to-tri polygon (including holes)
     */
    fun fromCSGPolygon(polygon: CsgPolygon): TriPolygon {
        // convert polygon
        val points = ArrayList<PolygonPoint>()
        for (v in polygon.vertices) {
            val vp = PolygonPoint(v.pos.x, v.pos.y, v.pos.z)
            points.add(vp)
        }

        val result = TriPolygon(points)

        // convert holes
        val holesOfP = polygon.storage.getValue<List<CsgPolygon>>(CsgEdge.KEY_POLYGON_HOLES)
        holesOfP?.forEach { hP ->
            result.addHole(fromCSGPolygon(hP))
        }

        return result
    }

    fun  concaveToConvex(concave: CsgPolygon): List<CsgPolygon> {
        val result = ArrayList<CsgPolygon>()
        val normal = concave.vertices[0].normal.clone()
        val cw = !Extrude.isCCW(concave)
        val p: TriPolygon = fromCSGPolygon(concave)
        Poly2Tri.triangulate(p)

        val triangles = p.triangles
        var triPoints = ArrayList<Vertex>()
        for (t in triangles) {
            var counter = 0
            for (tp in t.points) {
                triPoints.add(Vertex(
                    Vector3d.xyz(tp!!.x, tp.y, tp.z),
                    normal))

                if (counter == 2) {
                    if (!cw) {
                        triPoints.reverse()
                    }
                    val poly = CsgPolygon(triPoints, concave.storage)
                    result.add(poly)
                    counter = 0
                    triPoints = ArrayList<Vertex>()
                } else {
                    counter++
                }
            }
        }
        return result
    }
}
