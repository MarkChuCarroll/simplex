package org.goodmath.simplex.kcsg

import org.goodmath.simplex.vvecmath.Transform
import org.goodmath.simplex.vvecmath.Vector3d
import java.util.Objects

/**
 * Represents a vertex of a polygon. This class provides {@link #normal} so
 * primitives like {@link Cube} can return a smooth vertex normal, but
 * {@link #normal} is not used anywhere else.
 *
 * Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
data class Vertex(
    var pos: Vector3d,
    var normal: Vector3d,
    var weight: Double = 1.0) {

    /**
     * Inverts all orientation-specific data. (e.g. vertex normal).
     */
    fun flip() {
        normal = normal.negated()
    }

    /**
     * Create a new vertex between this vertex and the specified vertex by
     * linearly interpolating all properties using a parameter t.
     *
     * @param other vertex
     * @param t interpolation parameter
     * @return a new vertex between this and the specified vertex
     */
    fun interpolate(other: Vertex, t: Double): Vertex {
        return Vertex(pos.lerp(other.pos, t),
            normal.lerp(other.normal, t))
    }

    /**
     * Returns this vertex in STL string format.
     *
     * @return this vertex in STL string format
     */
    fun toStlString(): String {
        return "vertex ${pos.toStlString()}"
    }

    /**
     * Returns this vertex in STL string format.
     *
     * @param sb string builder
     * @return the specified string builder
     */
    fun toStlString(sb: StringBuilder): StringBuilder {
        sb.append("vertex ")
        return this.pos.toStlString(sb)
    }

    /**
     * Returns this vertex in OBJ string format.
     *
     * @param sb string builder
     * @return the specified string builder
     */
    fun toObjString(sb: StringBuilder): StringBuilder {
        sb.append("v ")
        return this.pos.toObjString(sb).append("\n")
    }

    /**
     * Returns this vertex in OBJ string format.
     *
     * @return this vertex in OBJ string format
     */
    fun toObjString(): String {
        return toObjString(StringBuilder()).toString();
    }

    /**
     * Applies the specified transform to this vertex.
     *
     * @param transform the transform to apply
     * @return this vertex
     */
    public fun transform(transform: Transform): Vertex {
        pos = pos.transformed(transform, weight)
        return this
    }

    /**
     * Applies the specified transform to a copy of this vertex.
     *
     * @param transform the transform to apply
     * @return a copy of this transform
     */
    public fun transformed(transform: Transform): Vertex {
        return copy().transform(transform)
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 53 * hash + Objects.hashCode(this.pos)
        return hash
    }

    override fun equals(obj: Any?): Boolean {
        if (obj == null) {
            return false
        }
        if (obj.javaClass != this::class.java) {
            return false
        }
        val other = obj as Vertex
        if (!Objects.equals(this.pos, other.pos)) {
            return false
        }
        return true
    }

    override fun toString(): String {
        return pos.toString()
    }
}
