package org.goodmath.simplex.kcsg.poly2tri

import kotlin.reflect.KProperty

class LateInitDelegate<T>(val name: String) {
    var v: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return v ?: throw RuntimeException("LateInit $name not initialized")
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        v = value
    }

    fun clear() {
        v = null

    }
}

class DTSweepDebugContext(override val tcx: DTSweepContext): TriangulationDebugContext() {
    /*
     * Fields used for visual representation of current triangulation
     */
    var primaryTriangle: DelaunayTriangle by LateInitDelegate("primary")

    var secondaryTriangle: DelaunayTriangle by LateInitDelegate("secondary")

    var activePoint: TriangulationPoint by LateInitDelegate("active")

    fun isDebugContext(): Boolean {
        return true
    }

    var activeConstraint: DTSweepConstraint by LateInitDelegate("activeConstraint")

    var activeNode: AdvancingFrontNode by LateInitDelegate("activeNode")

    override fun clear() {
        throw RuntimeException("Can't clear")
    }


//  public void setWorkingCircumCircle( TPoint point, TPoint point2, TPoint point3 )
//  {
//          double dx,dy;
//
//          CircleXY.circumCenter( point, point2, point3, m_circumCircle.a );
//          dx = m_circumCircle.a.getX()-point.getX();
//          dy = m_circumCircle.a.getY()-point.getY();
//          m_circumCircle.b = Double.valueOf( Math.sqrt( dx*dx + dy*dy ) );
//
//  }
}


object DTSweepPointComparator: Comparator<TriangulationPoint> {
    override fun compare(p1: TriangulationPoint, p2: TriangulationPoint): Int {
        return when {
            p1.y < p2.y -> -1
            p1.y > p2.y -> 1
            p1.x < p2.x -> -1
            p1.x > p2.x -> 1
            else -> 0
        }
    }
}



/**
 * @author Thomas ??? (thahlen@gmail.com)
 */
class DTSweepContext: TriangulationContext<DTSweepDebugContext>() {

    override var triUnit: Triangulatable<*> by LateInitDelegate("triUnit")

    override var debug: DTSweepDebugContext = DTSweepDebugContext(this)

    fun setDebugActiveNode(node: AdvancingFrontNode) {
        if (debugEnabled) {
            debug.activeNode = node
        }
    }

    // Inital triangle factor, seed triangle will extend 30% of
    // PointSet width to both left and right.
    val ALPHA = 0.3f

    /** Advancing front **/
    var aFront: AdvancingFront by LateInitDelegate("aFront")


    /** head point used with advancing front */
    var head: TriangulationPoint by LateInitDelegate("head")

    /** tail point used with advancing front */
    var tail: TriangulationPoint by LateInitDelegate("tail")


    var edgeEvent: EdgeEvent by LateInitDelegate("edgeEvent")

    init {
        clear()
    }


    fun removeFromList(triangle: DelaunayTriangle?) {
        if (triangle == null) return
        triList.remove(triangle)
        // TODO: remove all neighbor pointers to this triangle
//        for( int i=0; i<3; i++ )
//        {
//            if( triangle.neighbors[i] != null )
//            {
//                triangle.neighbors[i].clearNeighbor( triangle );
//            }
//        }
//        triangle.clearNeighbors();
    }

    fun meshClean(triangle: DelaunayTriangle?) {
        var t1: DelaunayTriangle? = null
        var t2: DelaunayTriangle? = null
        if (triangle != null) {
            val deque = ArrayDeque<DelaunayTriangle>()
            deque.addFirst(triangle)
            triangle.isInterior(true)

            while (deque.isNotEmpty()) {
                t1 = deque.removeFirst();
                triUnit.addTriangle(t1)
                for (i in 0 until 3) {
                    if (!t1.cEdge[i]) {
                        t2 = t1.neighbors[i]
                        if (t2 != null && !t2.isInterior()) {
                            t2.isInterior(true)
                            deque.addLast(t2)
                        }
                    }
                }
            }
        }
    }

    override fun clear() {
        super.clear()
        triList.clear()
    }

    fun getAdvancingFront(): AdvancingFront? {
        return aFront
    }

    fun addNode(node: AdvancingFrontNode) {
//        System.out.println( "add:" + node.key + ":" + System.identityHashCode(node.key));
//        m_nodeTree.put( node.getKey(), node );
        aFront.addNode(node)
    }

    fun removeNode(node: AdvancingFrontNode) {
//        System.out.println( "remove:" + node.key + ":" + System.identityHashCode(node.key));
//        m_nodeTree.delete( node.getKey() );
        aFront.removeNode(node)
    }

    fun locateNode(point: TriangulationPoint): AdvancingFrontNode? {
        return aFront.locateNode(point)
    }

    fun createAdvancingFront() {

        // Initial triangle
        val iTriangle = DelaunayTriangle(points[0], tail, head)
        addToList(iTriangle)

        var head = AdvancingFrontNode(iTriangle.points1)
        head.triangle = iTriangle
        var middle = AdvancingFrontNode(iTriangle.points0)
        middle.triangle = iTriangle
        var tail = AdvancingFrontNode(iTriangle.points2)

        aFront = AdvancingFront(head, tail)
        aFront.addNode(middle)

        // TODO: I think it would be more intuitive if head is middles next and not previous
        //       so swap head and tail
        aFront.head.next = middle
        middle.next = aFront.tail
        middle.prev = aFront.head
        aFront.tail.prev = middle
    }

    val basin = Basin()

    class Basin {
        var leftNode: AdvancingFrontNode by LateInitDelegate("leftNode")
        var bottomNode: AdvancingFrontNode by LateInitDelegate("bottomNode")
        var rightNode: AdvancingFrontNode by LateInitDelegate("rightNode")
        var width: Double by LateInitDelegate("width")
        var leftHighest: Boolean by LateInitDelegate("leftHighest")
    }



    class EdgeEvent(
        val constrainedEdge: DTSweepConstraint,
        val right: Boolean
    )

    /**
     * Try to map a node to all sides of this triangle that don't have
     * a neighbor.
     *
     * @param t
     */
    fun mapTriangleToNodes(t: DelaunayTriangle) {
        for (i in 0 until 3) {
            if (t.neighbors[i] == null) {
                val n = aFront.locatePoint(t.pointCW(t.points[i]!!))
                if (n != null) {
                    n.triangle = t;
                }
            }
        }
    }

    override fun prepareTriangulation(t: Triangulatable<*>) {
        super.prepareTriangulation(t)


        var xmax = points[0].x
        var xmin = points[0].x
        var ymax = points[0].y
        var ymin = points[0].y

        // Calculate bounds. Should be combined with the sorting
        for (p in points) {
            if (p.x > xmax) {
                xmax = p.x
            }
            if (p.x < xmin) {
                xmin = p.x
            }
            if (p.y > ymax) {
                ymax = p.y
            }
            if (p.y < ymin) {
                ymin = p.y
            }
        }

        val deltaX = ALPHA * (xmax - xmin)
        val deltaY = ALPHA * (ymax - ymin)
        val p1 = TPoint(xmax + deltaX, ymin - deltaY)
        val p2 = TPoint(xmin - deltaX, ymin - deltaY)

        head = p1
        tail = p2

        points.sortWith(DTSweepPointComparator)
    }

    fun finalizeTriangulation() {
        triUnit.addTriangles(triList)
        triList.clear()
    }

    override fun newConstraint(a: TriangulationPoint, b: TriangulationPoint): TriangulationConstraint {
        return DTSweepConstraint(a, b)
    }
}
