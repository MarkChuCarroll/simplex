package org.goodmath.simplex.kcsg

import javafx.scene.paint.Color
import javafx.scene.shape.TriangleMesh
import org.goodmath.simplex.vvecmath.Transform
import org.goodmath.simplex.vvecmath.Vector3d
import java.io.IOException
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.io.path.bufferedWriter
import kotlin.math.absoluteValue

/**
 * Constructive Solid Geometry (CSG).
 *
 * This implementation is a Java port of
 * <a
 * href="https://github.com/evanw/csg.js/">https://github.com/evanw/csg.js/</a>
 * with some additional features like polygon extrude, transformations etc. Thanks to the author for creating the CSG.js
 * library.<br><br>
 *
 * <b>Implementation Details</b>
 *
 * All CSG operations are implemented in terms of two functions, {@link Node#clipTo(Node)} and {@link Node#invert()},
 * which remove parts of a BSP tree inside another BSP tree and swap solid and empty space, respectively. To find the
 * union of {@code a} and {@code b}, we want to remove everything in {@code a} inside {@code b} and everything in
 * {@code b} inside {@code a}, then combine polygons from {@code a} and {@code b} into one solid:
 *
 * <blockquote><pre>
 *     a.clipTo(b);
 *     b.clipTo(a);
 *     a.build(b.allPolygons());
 * </pre></blockquote>
 *
 * The only tricky part is handling overlapping coplanar polygons in both trees. The code above keeps both copies, but
 * we need to keep them in one tree and remove them in the other tree. To remove them from {@code b} we can clip the
 * inverse of {@code b} against {@code a}. The code for union now looks like this:
 *
 * <blockquote><pre>
 *     a.clipTo(b);
 *     b.clipTo(a);
 *     b.invert();
 *     b.clipTo(a);
 *     b.invert();
 *     a.build(b.allPolygons());
 * </pre></blockquote>
 *
 * Subtraction and intersection naturally follow from set operations. If union is {@code A | B}, differenceion is
 * {@code A - B = ~(~A | B)} and intersection is {@code A & B =
 * ~(~A | ~B)} where {@code ~} is the complement operator.
 *
 * Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
class CSG(var storage: PropertyStorage = PropertyStorage()): Cloneable {
    enum class OptType {
        CSG_BOUND,
        POLYGON_BOUND,
        NONE
    }

    var polygons: MutableList<Polygon> = ArrayList()
    var optType: OptType = defaultOptType

    public override fun clone(): CSG {
        val csg = CSG()
        csg.optType = optType
        val polys = polygons
        val polygonStream: Stream<Polygon> = if (polys.size > 200) {
             polys.parallelStream()
        } else {
             polys.stream()
        }
        csg.polygons = polygonStream.map { p -> p.clone() }.collect(Collectors.toList())
        return csg
    }

    /**
     * Defines the CSg optimization type.
     *
     * @param type optimization type
     * @return this CSG
     */
    fun optimization(type: OptType): CSG {
        this.optType = type
        return this
    }

    /**
     * Returns the bounds of this csg.
     *
     * @return bounds of this csg
     */
    val bounds: Bounds
        get() {
        if (polygons.isEmpty()) {
            return Bounds(Vector3d.ZERO, Vector3d.ZERO)
        }
        var minX = polygons.flatMap { it.vertices.map { p -> p.pos.x } }.min()
        var minY = polygons.flatMap { it.vertices.map { p -> p.pos.y } }.min()
        var minZ = polygons.flatMap { it.vertices.map { p -> p.pos.z } }.min()

        var maxX = polygons.flatMap { it.vertices.map { p -> p.pos.x } }.max()
        var maxY = polygons.flatMap { it.vertices.map { p -> p.pos.y } }.max()
        var maxZ = polygons.flatMap { it.vertices.map { p -> p.pos.z } }.max()
        return Bounds(
            Vector3d.xyz(minX, minY, minZ),
            Vector3d.xyz(maxX, maxY, maxZ))
    }

    /**
     * Return a new CSG solid representing the union of this csg and the specified csg.
     *
     * <b>Note:</b> Neither this csg nor the specified csg are weighted.
     *
     * <blockquote><pre>
     *    A.union(B)
     *
     *    +-------+            +-------+
     *    |       |            |       |
     *    |   A   |            |       |
     *    |    +--+----+   =   |       +----+
     *    +----+--+    |       +----+       |
     *         |   B   |            |       |
     *         |       |            |       |
     *         +-------+            +-------+
     * </pre></blockquote>
     *
     *
     * @param csg other csg
     *
     * @return union of this csg and the specified csg
     */
    fun union(csg: CSG): CSG {
        return when(optType) {
            OptType.CSG_BOUND -> unionCSGBoundsOpt(csg)
            OptType.POLYGON_BOUND ->
                unionPolygonBoundsOpt(csg)
            else ->
                unionNoOpt(csg)
        }
    }

    /**
     * Return a new CSG solid representing the union of this csg and the specified csgs.
     *
     * <b>Note:</b> Neither this csg nor the specified csg are weighted.
     *
     * <blockquote><pre>
     *    A.union(B)
     *
     *    +-------+            +-------+
     *    |       |            |       |
     *    |   A   |            |       |
     *    |    +--+----+   =   |       +----+
     *    +----+--+    |       +----+       |
     *         |   B   |            |       |
     *         |       |            |       |
     *         +-------+            +-------+
     * </pre></blockquote>
     *
     *
     * @param csgs other csgs
     *
     * @return union of this csg and the specified csgs
     */
    fun union(csgs: List<CSG>): CSG {

        var result = this

        for (csg in csgs) {
            result = result.union(csg)
        }

        return result
    }


    /**
     * Returns the convex hull of this csg.
     *
     * @return the convex hull of this csg
     */
    fun hull(): CSG {
        return HullUtil.hull(this, storage)
    }

    /**
     * Returns the convex hull of this csg and the union of the specified csgs.
     *
     * @param csgs csgs
     * @return the convex hull of this csg and the specified csgs
     */
    fun hull(csgs: List<CSG>): CSG {
        var csgsUnion = CSG()
        csgsUnion.storage = storage
        csgsUnion.optType = optType
        csgsUnion.polygons = this.clone().polygons
        csgs.forEach { csg ->
            csgsUnion.polygons.addAll(csg.clone().polygons)
        }
        csgsUnion.polygons.forEach { p -> p.storage = storage }
        return csgsUnion.hull()
    }

    private fun unionCSGBoundsOpt(csg: CSG): CSG {
        System.err.println("WARNING: using " + CSG.OptType.NONE
                + " since other optimization types missing for union operation.")
        return unionIntersectOpt(csg)
    }

    private fun unionPolygonBoundsOpt(csg: CSG): CSG {
        val inner = ArrayList<Polygon>()
        val outer = ArrayList<Polygon>()

        val bounds = csg.bounds

        this.polygons.forEach { p ->
            if (bounds.intersects(p.bounds)) {
                inner.add(p)
            } else {
                outer.add(p)
            }
        }

        val allPolygons = ArrayList<Polygon>()

        if (inner.isNotEmpty()) {
            val innerCSG = CSG.fromPolygons(inner)
            allPolygons.addAll(outer)
            allPolygons.addAll(innerCSG.unionNoOpt(csg).polygons)
        } else {
            allPolygons.addAll(this.polygons)
            allPolygons.addAll(csg.polygons)
        }

        return CSG.fromPolygons(allPolygons).optimization(optType)
    }

    /**
     * Optimizes for intersection. If csgs do not intersect create a new csg that consists of the polygon lists of this
     * csg and the specified csg. In this case no further space partitioning is performed.
     *
     * @param csg csg
     * @return the union of this csg and the specified csg
     */
    private fun unionIntersectOpt(csg: CSG): CSG {
        var intersects = false
        var bounds = csg.bounds

        for (p in polygons) {
            if (bounds.intersects(p.bounds)) {
                intersects = true
                break
            }
        }

        val allPolygons = ArrayList<Polygon>()

        if (intersects) {
            return unionNoOpt(csg)
        } else {
            allPolygons.addAll(this.polygons)
            allPolygons.addAll(csg.polygons)
        }
        return fromPolygons(allPolygons).optimization(optType)
    }

    private fun unionNoOpt(csg: CSG): CSG {
        val a = Node(this.clone().polygons)
        val b = Node(csg.clone().polygons)
        a.clipTo(b)
        b.clipTo(a)
        b.invert()
        b.clipTo(a)
        b.invert()
        a.build(b.allPolygons())
        return CSG.fromPolygons(a.allPolygons()).optimization(optType)
    }

    /**
     * Return a new CSG solid representing the difference of this csg and the specified csgs.
     *
     * <b>Note:</b> Neither this csg nor the specified csgs are weighted.
     *
     * <blockquote><pre>
     * A.difference(B)
     *
     * +-------+            +-------+
     * |       |            |       |
     * |   A   |            |       |
     * |    +--+----+   =   |    +--+
     * +----+--+    |       +----+
     *      |   B   |
     *      |       |
     *      +-------+
     * </pre></blockquote>
     *
     * @param csgs other csgs
     * @return difference of this csg and the specified csgs
     */
    fun difference(csgs: List<CSG>): CSG {
        if (csgs.isEmpty()) {
            return this.clone()
        }
        val csgsUnion = csgs.reduce { csg1, csg2 -> csg1.union(csg2) }
        return difference(listOf(csgsUnion))
    }


    /**
     * Return a new CSG solid representing the difference of this csg and the specified csg.
     *
     * <b>Note:</b> Neither this csg nor the specified csg are weighted.
     *
     * <blockquote><pre>
     * A.difference(B)
     *
     * +-------+            +-------+
     * |       |            |       |
     * |   A   |            |       |
     * |    +--+----+   =   |    +--+
     * +----+--+    |       +----+
     *      |   B   |
     *      |       |
     *      +-------+
     * </pre></blockquote>
     *
     * @param csg other csg
     * @return difference of this csg and the specified csg
     */
    fun difference(csg: CSG): CSG {
        return when (optType) {
            OptType.CSG_BOUND ->
                differenceCSGBoundsOpt(csg)

            OptType.POLYGON_BOUND ->
                differencePolygonBoundsOpt(csg)
            else ->
                differenceNoOpt(csg)
        }
    }

    private fun differenceCSGBoundsOpt(csg: CSG): CSG {
        val b = csg
        val a1 = differenceNoOpt(csg.bounds.csg)
        val a2 = intersect(csg.bounds.csg)
        return a2.differenceNoOpt(b).unionIntersectOpt(a1).optimization(optType)
    }

    private fun differencePolygonBoundsOpt(csg: CSG): CSG {
        val inner = ArrayList<Polygon>()
        val outer = ArrayList<Polygon>()

        val bounds = csg.bounds

        this.polygons.forEach { p ->
            if (bounds.intersects(p.bounds)) {
                inner.add(p)
            } else {
                outer.add(p)
            }
        }
        val innerCSG = CSG.fromPolygons(inner)
        val allPolygons = ArrayList<Polygon>()
        allPolygons.addAll(outer)
        allPolygons.addAll(innerCSG.differenceNoOpt(csg).polygons)
        return fromPolygons(allPolygons).optimization(optType)
    }

    private fun differenceNoOpt(csg: CSG): CSG {
        val a = Node(this.clone().polygons)
        val b = Node(csg.clone().polygons)
        a.invert()
        a.clipTo(b)
        b.clipTo(a)
        b.invert()
        b.clipTo(a)
        b.invert()
        a.build(b.allPolygons())
        a.invert()

        return fromPolygons(a.allPolygons()).optimization(optType)

    }

    /**
     * Return a new CSG solid representing the intersection of this csg and the specified csg.
     *
     * <b>Note:</b> Neither this csg nor the specified csg are weighted.
     *
     * <blockquote><pre>
     *     A.intersect(B)
     *
     *     +-------+
     *     |       |
     *     |   A   |
     *     |    +--+----+   =   +--+
     *     +----+--+    |       +--+
     *          |   B   |
     *          |       |
     *          +-------+
     * }
     * </pre></blockquote>
     *
     * @param csg other csg
     * @return intersection of this csg and the specified csg
     */
    public fun intersect(csg: CSG): CSG {
        val a = Node(this.clone().polygons)
        val b = Node(csg.clone().polygons)
        a.invert()
        b.clipTo(a)
        b.invert()
        a.clipTo(b)
        b.clipTo(a)
        a.build(b.allPolygons())
        a.invert()
        return fromPolygons(a.allPolygons()).optimization(optType)
    }

    /**
     * Return a new CSG solid representing the intersection of this csg and the specified csgs.
     *
     * <b>Note:</b> Neither this csg nor the specified csgs are weighted.
     *
     * <blockquote><pre>
     *     A.intersect(B)
     *
     *     +-------+
     *     |       |
     *     |   A   |
     *     |    +--+----+   =   +--+
     *     +----+--+    |       +--+
     *          |   B   |
     *          |       |
     *          +-------+
     * }
     * </pre></blockquote>
     *
     * @param csgs other csgs
     * @return intersection of this csg and the specified csgs
     */
    fun intersect(csgs: List<CSG>): CSG {
        if (csgs.isEmpty()) {
            return this.clone()
        }
        var csgsUnion = csgs.reduce { csg1, csg2 -> csg1.union(csg2) }
        return intersect(csgsUnion)
    }


    /**
     * Returns this csg in STL string format.
     *
     * @return this csg in STL string format
     */
    fun toStlString(): String {
        val sb = StringBuilder()
        toStlString(sb)
        return sb.toString()
    }


    /**
     * Returns this csg in STL string format.
     *
     * @param sb string builder
     *
     * @return the specified string builder
     */
    public fun toStlString(sb: StringBuilder): StringBuilder {
        sb.append("solid v3d.csg\n")
        this.polygons.forEach {
            p -> p.toStlString(sb)
        }
        sb.append("endsolid v3d.csg\n")
        return sb
    }

    fun color(c: Color): CSG {
        val result = this.clone()
        storage.set("material:color",
            "${c.red} ${c.green} ${c.blue}")
        return result
    }

    fun toObj(): ObjFile {
        // we triangulate the polygon to ensure
        // compatibility with 3d printer software
        return toObj(3)
    }

    fun toObj(maxNumberOfVerts: Int): ObjFile {
        if (maxNumberOfVerts != 3) {
            throw UnsupportedOperationException(
                    "maxNumberOfVerts > 3 not supported yet")
        }

        val objSb = StringBuilder()

        objSb.append("mtllib " + ObjFile.MTL_NAME)

        objSb.append("# Group").append("\n")
        objSb.append("g v3d.csg\n")

        class PolygonStruct(
            val storage: PropertyStorage,
            val indices: List<Int>,
            val materialName: String)


        val vertices = ArrayList<Vertex>()
        val indices = ArrayList<PolygonStruct>()

        objSb.append("\n# Vertices\n")

        val materialNames = HashMap<PropertyStorage, Int>()

        var materialIndex = 0

        for (p in polygons) {
            val polyIndices = ArrayList<Int>()

            p.vertices.forEach{ v ->
                if (!vertices.contains(v)) {
                    vertices.add(v)
                    v.toObjString(objSb)
                    polyIndices.add(vertices.size)
                } else {
                    polyIndices.add(vertices.indexOf(v) + 1)
                }
            }
            if (!materialNames.containsKey(p.storage)) {
                materialIndex++
                materialNames[p.storage] = materialIndex
                p.storage.set("material:name", materialIndex)
            }
            indices.add(PolygonStruct(
                    p.storage, polyIndices,
                "material-" + materialNames[p.storage]))
        }

        objSb.append("\n# Faces").append("\n")
        for (ps in indices) {

            // add mtl info
            if (ps.storage.getValue<String>("material:color") != null) {
                objSb.append("usemtl ").append(ps.materialName).append("\n")
            }

            // we triangulate the polygon to ensure
            // compatibility with 3d printer software
            val pVerts = ps.indices
            val index1 = pVerts[0]
            for (i in 0 until pVerts.size - 2) {
                val index2 = pVerts[i + 1]
                val index3 = pVerts[i + 2]
                objSb.append("f ").append(index1).append(" ").append(index2).append(" ").append(index3).append("\n")
            }
            objSb.append("\n")
        }

        objSb.append("\n# End Group v3d.csg").append("\n")

        val mtlSb = StringBuilder()

        materialNames.keys.forEach { s ->
            {
                if (s.contains("material:color")) {
                    mtlSb.append("newmtl material-").append(s.getValue<String>("material:name")).append("\n")
                    mtlSb.append("Kd ").append(s.getValue<String>("material:color")).append("\n")
                }
            }
        }

        return ObjFile(objSb.toString(), mtlSb.toString())
    }

    /**
     * Returns this csg in OBJ string format.
     *
     * @param sb string builder
     * @return the specified string builder
     */
    fun toObjString(sb: StringBuilder): StringBuilder {
        sb.append("# Group").append("\n")
        sb.append("g v3d.csg\n")

        class PolygonStruct(
            val storage: PropertyStorage,
            val indices: List<Integer>,
            val materialName: String)

        val vertices = ArrayList<Vertex>()
        val indices = ArrayList<PolygonStruct>()

        sb.append("\n# Vertices\n")

        for (p in polygons) {
            val polyIndices = ArrayList<Int>()
            p.vertices.forEach { v ->
                if (!vertices.contains(v)) {
                    vertices.add(v)
                    v.toObjString(sb)
                    polyIndices.add(vertices.size)
                } else {
                    polyIndices.add(vertices.indexOf(v) + 1)
                }
            }
        }

        sb.append("\n# Faces").append("\n")

        for (ps in indices) {
            // we triangulate the polygon to ensure
            // compatibility with 3d printer software
            val pVerts = ps.indices
            val index1 = pVerts[0]
            for (i in 0 until pVerts.size - 2) {
                val index2 = pVerts[i + 1]
                val index3 = pVerts[i + 2]
                sb.append("f ").append(index1).append(" ").append(index2).append(" ").append(index3).append("\n")
            }
        }
        sb.append("\n# End Group v3d.csg").append("\n")
        return sb
    }

    /**
     * Returns this csg in OBJ string format.
     *
     * @return this csg in OBJ string format
     */
    fun toObjString(): String {
        val sb = StringBuilder()
        return toObjString(sb).toString()
    }

    fun weighted(f: WeightFunction): CSG {
        return Modifier(f).modified(this)
    }

    /**
     * Returns a transformed copy of this CSG.
     *
     * @param transform the transform to apply
     *
     * @return a transformed copy of this CSG
     */
    fun transformed(transform: Transform): CSG {
        if (polygons.isEmpty()) {
            return clone()
        }

        val newPolygons = polygons.map { p ->
            p.transformed(transform)
        }

        val result = CSG.fromPolygons(newPolygons).optimization(optType)
        result.storage = storage
        return result
    }

    // TODO finish experiment (20.7.2014)
    fun toJavaFXMesh(): MeshContainer {
        return toJavaFXMeshSimple()
    }

    /**
     * Returns the CSG as JavaFX triangle mesh.
     *
     * @return the CSG as JavaFX triangle mesh
     */
    fun toJavaFXMeshSimple(): MeshContainer {
        val mesh = TriangleMesh()
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY

        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY

        var counter = 0
        for (p in polygons) {
            if (p.vertices.size >= 3) {

                // TODO: improve the triangulation?
                //
                // JavaOne requires triangular polygons.
                // If our polygon has more vertices, create
                // multiple triangles:
                val firstVertex = p.vertices[0]
                for (i in 0 until p.vertices.size - 2) {
                    if (firstVertex.pos.x < minX) {
                        minX = firstVertex.pos.x
                    }
                    if (firstVertex.pos.y < minY) {
                        minY = firstVertex.pos.y
                    }
                    if (firstVertex.pos.z < minZ) {
                        minZ = firstVertex.pos.z
                    }

                    if (firstVertex.pos.x > maxX) {
                        maxX = firstVertex.pos.x
                    }
                    if (firstVertex.pos.y > maxY) {
                        maxY = firstVertex.pos.y
                    }
                    if (firstVertex.pos.z > maxZ) {
                        maxZ = firstVertex.pos.z
                    }

                    mesh.points.addAll(
                        firstVertex.pos.x.toFloat(),
                        firstVertex.pos.y.toFloat(),
                        firstVertex.pos.z.toFloat())

                    mesh.getTexCoords().addAll(0f) // texture (not covered)
                    mesh.getTexCoords().addAll(0f)

                    val secondVertex = p.vertices[i + 1]

                    if (secondVertex.pos.x < minX) {
                        minX = secondVertex.pos.x
                    }
                    if (secondVertex.pos.y < minY) {
                        minY = secondVertex.pos.y
                    }
                    if (secondVertex.pos.z < minZ) {
                        minZ = secondVertex.pos.z
                    }

                    if (secondVertex.pos.x > maxX) {
                        maxX = firstVertex.pos.x
                    }
                    if (secondVertex.pos.y > maxY) {
                        maxY = firstVertex.pos.y
                    }
                    if (secondVertex.pos.z > maxZ) {
                        maxZ = firstVertex.pos.z
                    }

                    mesh.getPoints().addAll(
                        secondVertex.pos.x.toFloat(),
                        secondVertex.pos.y.toFloat(),
                        secondVertex.pos.z.toFloat())

                    mesh.getTexCoords().addAll(0f) // texture (not covered)
                    mesh.getTexCoords().addAll(0f)

                    val thirdVertex = p.vertices[i + 2]

                    mesh.getPoints().addAll(
                        thirdVertex.pos.x.toFloat(),
                        thirdVertex.pos.y.toFloat(),
                        thirdVertex.pos.z.toFloat())

                    if (thirdVertex.pos.x < minX) {
                        minX = thirdVertex.pos.x
                    }
                    if (thirdVertex.pos.y < minY) {
                        minY = thirdVertex.pos.y
                    }
                    if (thirdVertex.pos.z < minZ) {
                        minZ = thirdVertex.pos.z
                    }

                    if (thirdVertex.pos.x > maxX) {
                        maxX = firstVertex.pos.x
                    }
                    if (thirdVertex.pos.y > maxY) {
                        maxY = firstVertex.pos.y
                    }
                    if (thirdVertex.pos.z > maxZ) {
                        maxZ = firstVertex.pos.z
                    }

                    mesh.getTexCoords().addAll(0f) // texture (not covered)
                    mesh.getTexCoords().addAll(0f)

                    mesh.getFaces().addAll(
                        counter, // first vertex
                        0, // texture (not covered)
                        counter + 1, // second vertex
                        0, // texture (not covered)
                        counter + 2, // third vertex
                        0 // texture (not covered)
                    )
                    counter += 3
                } // end for
            } // end if #verts >= 3

        } // end for polygon

        return MeshContainer(
                Vector3d.xyz(minX, minY, minZ),
            Vector3d.xyz(maxX, maxY, maxZ), listOf(mesh))
    }




    /**
     * Computes and returns the volume of this CSG based on a triangulated version
     * of the internal mesh.
     * @return volume of this csg
     */
    fun computeVolume(): Double {
        if(polygons.isEmpty()) return 0.0

        // triangulate polygons (parallel for larger meshes)
        val polyStream: Stream<Polygon> =
            if (polygons.size > 200) {
                polygons.parallelStream()
            } else {
                polygons.stream()
            }
        val triangles = polyStream.flatMap { poly ->
            poly.toTriangles().stream()
        }.collect(Collectors.toList())

        // compute sum over signed volumes of triangles
        // we use parallel streams for larger meshes
        // see http://chenlab.ece.cornell.edu/Publication/Cha/icip01_Cha.pdf
        val triangleStream =
            if(triangles.size > 200) {
                triangles.parallelStream()
            } else {
                triangles.stream()
            }

        val volume = triangleStream.mapToDouble { tri ->
            val p1 = tri.vertices.get(0).pos
            val p2 = tri.vertices.get(1).pos
            val p3 = tri.vertices.get(2).pos

            p1.dot(p2.crossed(p3)) / 6.0
        }.sum()

        return volume.absoluteValue
    }


    /**
     * Saves this csg using STL ASCII format.
     * @param path destination path
     */
    fun toStlFile(path: Path) {
        path.bufferedWriter(Charsets.UTF_8).use { out ->
            out.write("solid v3d.csg\n")
            polygons.forEach { p ->
                try {
                    out.write(p.toStlString())
                } catch (ex: IOException) {
                    System.err.println("IO Error writing STL file: $ex")
                    throw RuntimeException(ex)
                }
            }
            out.append("endsolid v3d.csg\n")
        }
    }

    companion object {
        var defaultOptType = OptType.NONE

        /**
         * Constructs a CSG from a list of {@link Polygon} instances.
         *
         * @param polygons polygons
         * @return a CSG instance
         */
        fun fromPolygons(polygons: List<Polygon>): CSG {
            val csg = CSG()
            csg.polygons = polygons.toMutableList()
            return csg
        }


        /**
         * Constructs a CSG from a list of {@link Polygon} instances.
         *
         * @param storage shared storage
         * @param polygons polygons
         * @return a CSG instance
         */
        fun fromPolygons(storage: PropertyStorage, polygons: List<Polygon>): CSG {
            val csg = CSG()
            csg.polygons = polygons.toMutableList()

            csg.storage = storage

            for (polygon in polygons) {
                polygon.storage = storage
            }
            return csg
        }


    }

    /**
     * Loads a CSG from stl.
     * @param path file path
     * @return CSG
     * @throws IOException if loading failed
     */
    fun file(path: Path): CSG  {
        val loader = STLLoader()

        val polygons = ArrayList<Polygon>()
        var vertices = ArrayList<Vector3d>()
        for(p  in loader.parse(path.toFile())) {
            vertices.add(p.clone())
            if (vertices.size == 3) {
                polygons.add(Polygon.fromPoints(vertices))
                vertices = ArrayList()
            }
        }
        return CSG.fromPolygons(PropertyStorage(), polygons)
    }
}

/**
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
class Modifier(val function: WeightFunction) {
    fun modify(csg: CSG) {
        for(p in csg.polygons) {
            for(v in p.vertices) {
                v.weight = function.eval(v.pos, csg)
            }
        }
    }

    fun modified(csg: CSG): CSG {
        val result = csg.clone()
        modify(result)
        return result
    }
}
