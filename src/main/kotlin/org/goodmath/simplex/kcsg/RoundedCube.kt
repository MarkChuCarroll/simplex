package org.goodmath.simplex.kcsg

import org.goodmath.simplex.vvecmath.Transform
import org.goodmath.simplex.vvecmath.Vector3d


/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 * Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
class RoundedCube(
    val center: Vector3d,
    val dimensions: Vector3d): PrimitiveShape {
    constructor(): this(Vector3d.xyz(0.0, 0.0, 0.0), Vector3d.xyz(1.0, 1.0, 1.0))
    constructor(size: Double): this(Vector3d.xyz(0.0, 0.0, 0.0), Vector3d.xyz(size, size, size))
    constructor(length: Double, width: Double, depth: Double): this(Vector3d.xyz(0.0, 0.0, 0.0), Vector3d.xyz(length, width, depth))

    override val properties = PropertyStorage()
    var cornerRadius = 0.1
    var resolution = 8

    override fun toPolygons(): List<Polygon> {
        val spherePrototype = Sphere(cornerRadius, resolution * 2, resolution).toCSG()
        val x = dimensions.x / 2.0 - cornerRadius
        val y = dimensions.y / 2.0 - cornerRadius
        val z = dimensions.z / 2.0 - cornerRadius
        val sphere1 = spherePrototype.transformed(Transform.unity().translate(-x, -y, -z))
        val sphere2 = spherePrototype.transformed(Transform.unity().translate(x, -y, -z))
        val sphere3 = spherePrototype.transformed(Transform.unity().translate(x, y, -z))
        val sphere4 = spherePrototype.transformed(Transform.unity().translate(-x, y, -z))
        val sphere5 = spherePrototype.transformed(Transform.unity().translate(-x, -y, z))
        val sphere6 = spherePrototype.transformed(Transform.unity().translate(x, -y, z))
        val sphere7 = spherePrototype.transformed(Transform.unity().translate(x, y, z))
        val sphere8 = spherePrototype.transformed(Transform.unity().translate(-x, y, z))
        val result = sphere1.union(listOf(
                sphere2, sphere3, sphere4,
            sphere5, sphere6, sphere7, sphere8)).hull().polygons

        val locTransform = Transform.unity().translate(center)

        for (p in result) {
            p.transform(locTransform)
        }
        return result
    }
}
