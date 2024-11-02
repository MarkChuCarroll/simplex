package org.goodmath.simplex.kcsg

/**
 * A primitive geometry.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 * Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
interface PrimitiveShape {
    /**
     * Returns the polygons that define this primitive.
     *
     * <b>Note:</b> this method computes the polygons each time this method is
     * called. The polygons can be cached inside a {@link CSG} object.
     *
     * @return al list of polygons that define this primitive
     */
    fun toPolygons(): List<Polygon>

    /**
     * Returns this primitive as {@link CSG}.
     *
     * @return this primitive as {@link CSG}
     */
    fun toCSG(): CSG {
        return CSG.fromPolygons(properties, toPolygons())
    }

    /**
     * Returns the property storage of this primitive.
     * @return the property storage of this primitive
     */
    val properties: PropertyStorage
}
