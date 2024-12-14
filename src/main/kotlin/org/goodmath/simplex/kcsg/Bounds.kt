package org.goodmath.simplex.kcsg

import org.goodmath.simplex.kcsg.vvecmath.Vector3d
import kotlin.math.absoluteValue

/**
 * Bounding box for CSGs.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
open class Bounds(inMin: Vector3d, inMax: Vector3d): Cloneable {
    val min = inMin.clone()
    val max = inMax.clone()

    val center: Vector3d =  Vector3d.xyz(
        (max.x + min.x) / 2,
        (max.y + min.y) / 2,
        (max.z + min.z) / 2)
    val bounds: Vector3d = Vector3d.xyz(
        (max.x - min.x).absoluteValue,
        (max.y - min.y).absoluteValue,
        (max.z - min.z).absoluteValue)

    val csg: CSG by lazy {
        cube.toCSG()
    }

    val cube: Cube = Cube(center, bounds)



    override fun clone(): Bounds {
        return Bounds(min, max);
    }



    /**
     * Indicates whether the specified vertex is contained within this bounding
     * box (check includes box boundary).
     *
     * @param v vertex to check
     * @return {@code true} if the vertex is contained within this bounding box;
     * {@code false} otherwise
     */
    fun contains(v: Vertex): Boolean {
        return contains(v.pos)
    }

    /**
     * Indicates whether the specified point is contained within this bounding
     * box (check includes box boundary).
     *
     * @param v vertex to check
     * @return {@code true} if the point is contained within this bounding box;
     * {@code false} otherwise
     */
    fun contains(v: Vector3d): Boolean {
        val inX = min.x <= v.x && v.x <= max.x
        val inY = min.y <= v.y && v.y <= max.y
        val inZ = min.z <= v.z && v.z <= max.z

        return inX && inY && inZ
    }

    /**
     * Indicates whether the specified polygon is contained within this bounding
     * box (check includes box boundary).
     *
     * @param p polygon to check
     * @return {@code true} if the polygon is contained within this bounding
     * box; {@code false} otherwise
     */
    fun contains(p: Polygon): Boolean {
        return p.vertices.all { v -> contains(v) }
    }

    /**
     * Indicates whether the specified bounding box intersects with this
     * bounding box (check includes box boundary).
     *
     * @param b box to check
     * @return {@code true} if the bounding box intersects this bounding box;
     * {@code false} otherwise
     */
    fun intersects(b: Bounds): Boolean {
        if (b.min.x > max.x || b.max.x < min.x) {
            return false
        }
        if (b.min.y > max.y || b.max.y < min.y) {
            return false
        }
        if (b.min.z > max.z || b.max.z < min.z) {
            return false
        }
        return true

    }

    override fun toString(): String {
        return "[center: $center, bounds: $bounds]"
    }
}

