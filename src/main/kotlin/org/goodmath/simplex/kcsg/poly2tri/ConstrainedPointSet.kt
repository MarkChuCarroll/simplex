package org.goodmath.simplex.kcsg.poly2tri

/**
 * Extend by adding some Constraints on how it will be triangulated<br>
 * A constraint defines an edge between two points in the set, these edges can not
 * be crossed. They will be enforced triangle edges after a triangulation.
 * <p>
 *
 *
 * @author Thomas ???, thahlen@gmail.com
 */
class ConstrainedPointSet(points: List<TriangulationPoint>,
                          constraints: ArrayList<TriangulationPoint> = ArrayList(),
                          val index: Array<Int>? = null): PointSet(points) {
    val constrainedPointList = ArrayList<TriangulationPoint>()

    init {
        constrainedPointList.addAll(constraints)
    }


    override fun getTriangulationMode(): TriangulationMode {
        return TriangulationMode.CONSTRAINED
    }

    fun getEdgeIndex(): Array<Int>? {
        return index
    }

    override fun prepareTriangulation(tcx: TriangulationContext<*>) {
        super.prepareTriangulation(tcx)
        if (constrainedPointList.isNotEmpty()) {
            var p1: TriangulationPoint
            var p2: TriangulationPoint
            val iterator = constrainedPointList.iterator()
            while (iterator.hasNext()) {
                p1 = iterator.next()
                p2 = iterator.next()
                tcx.newConstraint(p1, p2)
            }
        } else {
            if (index != null) {
                for (i in 0 until index.size step 2) {
                    // XXX: must change!!
                    tcx.newConstraint(points[index[i]], points[index[i + 1]])
                }
            }
        }
    }

    /**
     * TODO: TO BE IMPLEMENTED!
     * Performs a validation on given input<br>
     * 1. Check's if there any constraint edges are crossing or collinear<br>
     * 2.
     * @return
     */
    fun isValid(): Boolean {
        return true
    }
}
