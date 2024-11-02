package org.goodmath.simplex.kcsg

import eu.mihosoft.jcsg.ext.quickhull3d.Point3d
import eu.mihosoft.jcsg.ext.quickhull3d.QuickHull3D
import org.goodmath.simplex.vvecmath.Vector3d


/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
object HullUtil {
     fun hull(points: List<Vector3d>, storage: PropertyStorage): CSG {
        val hullPoints = points.map { vec -> Point3d(vec.x, vec.y, vec.z) }.toTypedArray()
        val hull = QuickHull3D()
        hull.build(hullPoints)
        hull.triangulate()
         val faces = hull.getFaces()
        val polygons = ArrayList<Polygon>()
        val vertices = ArrayList<Vector3d>()
        for (verts in faces) {
            for (i in verts) {
                vertices.add(points.get(hull.getVertexPointIndices()[i]))
            }
            polygons.add(Polygon.fromPoints(vertices, storage))
            vertices.clear()
        }
        return CSG.fromPolygons(polygons)
    }

    fun hull(csg: CSG, storage: PropertyStorage): CSG {
        val points = ArrayList<Vector3d>(csg.polygons.size * 3)
        csg.polygons.forEach { p -> p.vertices.forEach { v -> points.add(v.pos) } }
        return hull(points, storage)
    }
}
