package org.goodmath.simplex.kcsg.poly2tri

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Poly2Tri {
    val logger: Logger = LoggerFactory.getLogger(Poly2Tri.javaClass)

    val defaultAlgorithm:  TriangulationAlgorithm = TriangulationAlgorithm.DTSweep

    fun triangulate(ps: PolygonSet)  {
        val tcx = createContext(defaultAlgorithm)
        for (p in ps.polygons) {
            tcx.prepareTriangulation(p)
            triangulate(tcx)
            tcx.clear()
        }
    }

    fun triangulate(p: Polygon) {
        triangulate(defaultAlgorithm, p)
    }

    fun triangulate(cps: ConstrainedPointSet) {
        triangulate(defaultAlgorithm, cps)
    }

    fun triangulate(ps: PointSet) {
        triangulate(defaultAlgorithm, ps)
    }

    fun createContext(algorithm: TriangulationAlgorithm):  TriangulationContext<*> {
        return DTSweepContext()
    }

    fun triangulate(algorithm: TriangulationAlgorithm,
                    t: Triangulatable) {
        val tcx = createContext(algorithm)
        tcx.prepareTriangulation(t)
        triangulate(tcx)
    }

    fun triangulate(tcx: TriangulationContext<*>) {
        DTSweep.triangulate(tcx as DTSweepContext)
    }

    /**
     * Will do a warmup run to let the JVM optimize the triangulation code
     */
    fun warmup() {
        /*
         * After a method is run 10000 times, the Hotspot compiler will compile
         * it into native code. Periodically, the Hotspot compiler may recompile
         * the method. After an unspecified amount of time, then the compilation
         * system should become quiet.
         */
        val poly = PolygonGenerator.randomCircleSweep2(50.0, 50000)
        val process = TriangulationProcess()
        process.triangulate(poly)
    }
}
