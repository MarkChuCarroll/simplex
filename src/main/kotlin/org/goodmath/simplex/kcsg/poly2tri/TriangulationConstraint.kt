package org.goodmath.simplex.kcsg.poly2tri

/**
 * Forces a triangle edge between two points p and q
 * when triangulating. For example used to enforce
 * Polygon Edges during a polygon triangulation.
 *
 * @author Thomas ???, thahlen@gmail.com
 */
open class TriangulationConstraint(var p: TriangulationPoint, var q: TriangulationPoint)
