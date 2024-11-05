package org.goodmath.simplex.kcsg.poly2tri

open class PointSet(initialPoints: MutableList<TriangulationPoint>): Triangulatable<TriangulationPoint> {
    override val points: MutableList<TriangulationPoint> = ArrayList()
    init {
        points.addAll(initialPoints)
    }

    override val triangles: MutableList<DelaunayTriangle> = ArrayList()

    override val triangulationMode = TriangulationMode.UNCONSTRAINED

    override fun addTriangle(t: DelaunayTriangle) {
        triangles.add(t)
    }

    override fun addTriangles(list: List<DelaunayTriangle>) {
        triangles.addAll(list)
    }

    override fun clearTriangulation() {
        triangles.clear()
    }

    override fun prepareTriangulation(tcx: TriangulationContext<*>) {
        triangles.clear()
        tcx.addPoints(points)
    }
}
