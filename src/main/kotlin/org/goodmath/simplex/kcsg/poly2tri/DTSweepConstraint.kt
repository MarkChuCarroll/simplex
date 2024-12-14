package org.goodmath.simplex.kcsg.poly2tri

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *
 * @author Thomas ???, thahlen@gmail.com
 *
 */
class DTSweepConstraint(p1: TriangulationPoint, p2: TriangulationPoint): TriangulationConstraint(p1, p2) {
    init {
        if (p1.y > p2.y) {
            q = p1
            p = p2
        } else if (p1.y == p2.y) {
            if (p1.x > p2.x) {
                q = p1
                p = p2
            } else if( p1.x == p2.x) {
                logger.info("Failed to create constraint {}={}", p1, p2)
            }
        }
        q.addEdge(this)
    }


    companion object {
        val logger: Logger = LoggerFactory.getLogger(DTSweepConstraint.javaClass)
    }
}
