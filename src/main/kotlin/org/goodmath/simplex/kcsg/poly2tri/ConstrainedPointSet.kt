package org.goodmath.simplex.kcsg.poly2tri

/**
 * Exteet by adding some Constraints on how it will be triangulated<br>
 * A constraint defines an edge between two points in the set, these edges can not
 * be crossed. They will be enforced triangle edges after a triangulation.
 * <p>
 *
 *
 * @author Thomas ???, thahlen@gmail.com
 */
class ConstrainedPointSet(points: MutableList<TriangulationPoint>, idx: List<Int> = emptyList(),
    constraints: List<TriangulationPoint> = emptyList()): PointSet(points) {

    val constrainedPointList: MutableList<TriangulationPoint> = ArrayList()
    init {
        constrainedPointList.addAll(constraints)
    }

    val index: List<Int> = idx


    override val triangulationMode = TriangulationMode.CONSTRAINED;

    fun getEdgeIndex(): List<Int> {
        return index
    }

    override fun prepareTriangulation(tcx: TriangulationContext<*>) {
        super.prepareTriangulation(tcx)
        if(constrainedPointList.isNotEmpty()) {
            val iterator = constrainedPointList.iterator()
            while(iterator.hasNext()) {
                val p1 = iterator.next()
                val p2 = iterator.next()
                tcx.newConstraint(p1,p2)
            }
        } else {
            for (i in 0 until index.size step 2) {
                // XXX: must change!!
                tcx.newConstraint(points[index[i]], points[index[i+1]])
            }
        }
    }

    /**
     * TODO: TO BE IMPLEMENTED!
     * Peforms a validation on given input<br>
     * 1. Check's if there any constraint edges are crossing or collinear<br>
     * 2.
     * @return
     */
    fun isValid(): Boolean {
        return true
    }
}
