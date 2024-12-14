package org.goodmath.simplex.kcsg.quickhull

import org.goodmath.simplex.kcsg.CSG
import org.goodmath.simplex.kcsg.Polygon
import org.goodmath.simplex.kcsg.PropertyStorage
import org.goodmath.simplex.kcsg.vvecmath.Vector3d as KVector3d

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
object HullUtil {

    fun hull(points: List<KVector3d>, storage: PropertyStorage): CSG {

        val hullPoints = points.map { vec -> Point3d(vec.x, vec.y, vec.z) }

        val hull = QuickHull3D()
        hull.build(hullPoints)
        hull.triangulate()

        val faces = hull.getFaces()

        val polygons = ArrayList<Polygon>()
        val vertices = ArrayList<KVector3d>()

        for (verts in faces) {
            for (i in verts!!) {
                val p = points[hull.getVertexPointIndices()[i]]
                vertices.add(p)
            }
            polygons.add(Polygon.fromPoints(vertices, storage))
            vertices.clear()
        }

        return CSG.fromPolygons(polygons)
    }

    fun hull(csg: CSG, storage: PropertyStorage): CSG {
        val points = ArrayList<KVector3d>(csg.polygons.size * 3)

        csg.polygons.forEach { p ->
            p.vertices.forEach { v ->
                points.add(v.pos)
            }
        }
        return hull(points, storage)
    }
}
