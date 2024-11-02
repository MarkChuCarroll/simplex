package org.goodmath.simplex.kcsg

import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.twist.Twist
import org.goodmath.simplex.vvecmath.Vector3d
import kotlin.math.absoluteValue

/**
 * Bounding box for CSGs.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 * Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
class Bounds(low: Vector3d, high:  Vector3d): Value {
    val min = low.clone()
    val max = high.clone()


    override val valueType: ValueType
        get() = TODO("Not yet implemented")

    override fun twist(): Twist {
        TODO("Not yet implemented")
    }


    val center: Vector3d = Vector3d.xyz(
       (max.x + min.x) / 2,
       (max.y + min.y) / 2,
       (max.z + min.z) / 2)

    val bounds =  Vector3d.xyz(
        (max.x - min.x).absoluteValue,
        (max.y - min.y).absoluteValue,
        (max.z - min.z).absoluteValue)

    val csg: CSG by lazy {
        cuboid.toCSG()
    }

    val cuboid = Cuboid(center, bounds)

    fun clone(): Bounds {
        return Bounds(min, max)
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
        return p.vertices.all { contains(it) }
    }

    /**
     * Indicates whether the specified polygon intersects with this bounding box
     * (check includes box boundary).
     *
     * @param p polygon to check
     * @return {@code true} if the polygon intersects this bounding box;
     * {@code false} otherwise
     */
    fun intersects(p: Polygon): Boolean {
        return p.vertices.filter { contains(it) }.size > 0
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
