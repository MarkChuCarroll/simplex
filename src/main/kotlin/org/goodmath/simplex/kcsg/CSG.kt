package org.goodmath.simplex.kcsg

import javafx.scene.paint.Color
import javafx.scene.shape.TriangleMesh
import org.goodmath.simplex.kcsg.quickhull.HullUtil
import org.goodmath.simplex.kcsg.vvecmath.Transform
import org.goodmath.simplex.kcsg.vvecmath.Vector3d
import java.util.stream.Collectors


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
 */
 class CSG: Cloneable {

    val polygons = ArrayList<Polygon>()
    var defaultOptType = OptType.NONE
    var optType: OptType = OptType.NONE
    var storage: PropertyStorage = PropertyStorage()


    public override fun clone(): CSG {
        val csg = CSG()
        csg.optType = optType
        val polygonStream = if (polygons.size > 200) {
            polygons.parallelStream()
        } else {
            polygons.stream()
        }
        csg.polygons.addAll(polygonStream.map { p -> p.clone() }.collect(Collectors.toList()))
        return csg
    }


    /**
     * Defines the CSg optimization type.
     *
     * @param type optimization type
     * @return this CSG
     */
    fun optimization(type: OptType): CSG {
        optType = type
        return this
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
            OptType.CSG_BOUND ->
                unionCSGBoundsOpt(csg)
            OptType.POLYGON_BOUND ->
                unionPolygonBoundsOpt(csg)
            else ->
                unionNoOpt(csg)
        }
    }

    /**
     * Returns a csg consisting of the polygons of this csg and the specified csg.
     *
     * The purpose of this method is to allow fast union operations for objects that do not intersect.
     *
     * <p>
     * <b>WARNING:</b> this method does not apply the csg algorithms. Therefore, please ensure that this csg and the
     * specified csg do not intersect.
     *
     * @param csg csg
     *
     * @return a csg consisting of the polygons of this csg and the specified csg
     */
    fun dumbUnion(csg: CSG): CSG {
        val result = clone()
        val other = csg.clone()
        result.polygons.addAll(other.polygons)
        return result
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
        var result = this.clone()

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
        val csgsUnion = CSG()
        csgsUnion.storage = storage
        csgsUnion.optType = optType
        csgsUnion.polygons.addAll(polygons)

        csgs.forEach { csg ->
            csgsUnion.polygons.addAll(csg.clone().polygons)
        }

        csgsUnion.polygons.forEach { p -> p.storage = storage }
        return csgsUnion.hull()
    }

    fun unionCSGBoundsOpt(csg: CSG): CSG {
        System.err.println(
            "WARNING: using " + OptType.NONE
                    + " since other optimization types missing for union operation."
        );
        return unionIntersectOpt(csg)
    }

    fun unionPolygonBoundsOpt(csg: CSG): CSG {
        val inner = ArrayList<Polygon>()
        val outer = ArrayList<Polygon>()

        val bounds = csg.bounds

        this.polygons.forEach { p ->
            if (bounds.intersects(p.getBounds())) {
                inner.add(p)
            } else {
                outer.add(p)
            }
        }

        val allPolygons = ArrayList<Polygon>()

        if (!inner.isEmpty()) {
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
    fun unionIntersectOpt(csg: CSG): CSG {
        var intersects = false

        val bounds = csg.bounds
        for (p in polygons) {
            if (bounds.intersects(p.getBounds())) {
                intersects = true
            }
        }

         val allPolygons = ArrayList<Polygon>()

        if (intersects) {
            return unionNoOpt(csg)
        } else {
            allPolygons.addAll(this.polygons)
            allPolygons.addAll(csg.polygons)
        }

        return CSG.fromPolygons(allPolygons).optimization(optType)
    }

    fun unionNoOpt(csg: CSG): CSG {
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

        var csgsUnion = csgs[0]

        for (i in 1 until csgs.size) {
            csgsUnion = csgsUnion.union(csgs[i])
        }
        return difference(csgsUnion)
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
        return when(optType) {
            OptType.CSG_BOUND ->
                differenceCSGBoundsOpt(csg)
            OptType.POLYGON_BOUND ->
                differencePolygonBoundsOpt(csg)
            else ->
                differenceNoOpt(csg)
        }
    }

    fun differenceCSGBoundsOpt(csg: CSG): CSG {
        val b = csg

        val a1 = differenceNoOpt(csg.bounds.csg)
        val a2 = intersect(csg.bounds.csg)

        return a2.differenceNoOpt(b).unionIntersectOpt(a1).optimization(optType)
    }

    fun differencePolygonBoundsOpt(csg: CSG): CSG {
        val inner = ArrayList<Polygon>()
        val outer =  ArrayList<Polygon>()

        val bounds = csg.bounds

        polygons.forEach { p ->
            if (bounds.intersects(p.getBounds())) {
                inner.add(p)
            } else {
                outer.add(p)
            }
        }

        val innerCSG = CSG.fromPolygons(inner)

        val allPolygons = ArrayList<Polygon>()
        allPolygons.addAll(outer)
        allPolygons.addAll(innerCSG.differenceNoOpt(csg).polygons)

        return CSG.fromPolygons(allPolygons).optimization(optType)
    }

    fun differenceNoOpt(csg: CSG):  CSG {
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

        val csgA = CSG.fromPolygons(a.allPolygons()).optimization(optType)
        return csgA
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
    fun intersect(csg: CSG): CSG {
        val a = Node(this.clone().polygons)
        val b = Node(csg.clone().polygons)
        a.invert()
        b.clipTo(a)
        b.invert()
        a.clipTo(b)
        b.clipTo(a)
        a.build(b.allPolygons())
        a.invert()
        return CSG.fromPolygons(a.allPolygons()).optimization(optType)
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
        var csgsUnion = csgs[0]

        for (i in 1 until csgs.size) {
            csgsUnion = csgsUnion.union(csgs[i])
        }

        return intersect(csgsUnion)
    }


    /**
     * Returns this csg in STL string format.
     *
     * @return this csg in STL string format
     */
    fun toStlString(): String {
        val sb = StringBuilder();
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
    fun toStlString(sb: StringBuilder): StringBuilder {
        sb.append("solid v3d.csg\n")
        this.polygons.stream().forEach {
            p ->p.toStlString(sb)
        }
        sb.append("endsolid v3d.csg\n")
        return sb
    }

    fun color(c: Color): CSG {
        val result = this.clone()

        storage.set(
            "material:color",
            "" + c.red
                    + " " + c.green
                    + " " + c.blue)

        return result
    }

    fun toObj(): ObjFile {
        // we triangulate the polygon to ensure
        // compatibility with 3d printer software
        return toObj(3)
    }

    fun toObj(maxNumberOfVerts: Int): ObjFile {
        if (maxNumberOfVerts != 3) {
            throw UnsupportedOperationException (
                    "maxNumberOfVerts > 3 not supported yet")
        }

        val objSb = StringBuilder()

        objSb.append("mtllib " + ObjFile.MTL_NAME);

        objSb.append("# Group").append("\n")
        objSb.append("g v3d.csg\n")

        class PolygonStruct(var storage: PropertyStorage,
            val indices: ArrayList<Int>,
            var materialName: String)

        val vertices = ArrayList<Vertex>()
        val indices =  ArrayList<PolygonStruct>()

        objSb.append("\n# Vertices\n")

        val materialNames = HashMap<PropertyStorage, Int>()

        var materialIndex = 0

        for (p in polygons) {
            val polyIndices = ArrayList<Int>()
            p.vertices.stream().forEach { v ->
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

        indices.add(
            PolygonStruct (
                p.storage, polyIndices,
                "material-" + materialNames.get(p.storage)))
        }
        objSb.append("\n# Faces").append("\n")

        for (ps in indices) {
            // add mtl info
            ps.storage.getValue<String>("material:color")?.let { v ->
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
        objSb.append("\n# End Group v3d.csg").append("\n");

        val mtlSb = StringBuilder()

        materialNames.keys.forEach { s ->
            if (s.contains("material:color")) {
                mtlSb.append("newmtl material-").append(s.getValue<String>("material:name")!!).append("\n")
                mtlSb.append("Kd ").append(s.getValue<String>("material:color")!!).append("\n")
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
        class PolygonStruct(val storage: PropertyStorage,
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
        val newPolygons = this.polygons.map { p ->
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
        var minX = polygons.minOf { p ->
            p.vertices.minOf { v -> v.pos.x }
        }
        var minY = polygons.minOf { p ->
            p.vertices.minOf { v -> v.pos.y }
        }
        var minZ = polygons.minOf { p ->
            p.vertices.minOf { v -> v.pos.z }
        }

        var maxX = polygons.maxOf { p ->
            p.vertices.maxOf { v -> v.pos.x }
        }
        var maxY = polygons.maxOf { p ->
            p.vertices.maxOf { v -> v.pos.y }
        }
        var maxZ = polygons.maxOf { p ->
            p.vertices.maxOf { v -> v.pos.z }
        }

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
                    mesh.points.addAll(
                        firstVertex.pos.x.toFloat(),
                        firstVertex.pos.y.toFloat(),
                        firstVertex.pos.z.toFloat())

                    mesh.texCoords.addAll(0.0f) // texture (not covered)
                    mesh.texCoords.addAll(0.0f)

                    val secondVertex = p.vertices[i+1]
                    mesh.points.addAll(
                        secondVertex.pos.x.toFloat(),
                        secondVertex.pos.y.toFloat(),
                        secondVertex.pos.z.toFloat())

                    mesh.texCoords.addAll(0.0f) // texture (not covered)
                    mesh.texCoords.addAll(0.0f)

                    val thirdVertex = p.vertices[i+1]

                    mesh.points.addAll(
                        thirdVertex.pos.x.toFloat(),
                        thirdVertex.pos.y.toFloat(),
                        thirdVertex.pos.z.toFloat())

                    mesh.texCoords.addAll(0.0f) // texture (not covered)
                    mesh.texCoords.addAll(0.0f)

                mesh.faces.addAll(
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
            Vector3d.xyz(maxX, maxY, maxZ),
            arrayListOf(mesh))
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
            val minX = polygons.minOf { p ->
                p.vertices.minOf { v -> v.pos.x }
            }
            val minY = polygons.minOf { p ->
                p.vertices.minOf { v -> v.pos.y }
            }
            val minZ = polygons.minOf { p ->
                p.vertices.minOf { v -> v.pos.z }
            }
            val maxX = polygons.maxOf { p ->
                p.vertices.maxOf { v -> v.pos.x }
            }
            val maxY = polygons.maxOf { p ->
                p.vertices.maxOf { v -> v.pos.y }
            }
            val maxZ = polygons.maxOf { p ->
                p.vertices.maxOf { v -> v.pos.z }
            }
            return Bounds(
                Vector3d.xyz(minX, minY, minZ),
                Vector3d.xyz(maxX, maxY, maxZ))
        }


    companion object {
        /**
         * Constructs a CSG from a list of {@link Polygon} instances.
         *
         * @param polygons polygons
         * @return a CSG instance
         */
        fun fromPolygons(polygons: List<Polygon>): CSG {
            val csg = CSG()
            csg.polygons.addAll(polygons)
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
            csg.polygons.addAll(polygons)
            csg.storage = storage

            for (polygon in polygons) {
                polygon.storage = storage
            }
            return csg
        }

        enum class OptType {
            CSG_BOUND,
            POLYGON_BOUND,
            NONE
        }
    }
}
