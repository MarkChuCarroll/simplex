package org.goodmath.simplex.kcsg.quickhull

import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.StreamTokenizer
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.system.exitProcess

/**
 * Computes the convex hull of a set of three dimensional points.
 *
 * <p>The algorithm is a three-dimensional implementation of Quickhull, as
 * described in Barber, Dobkin, and Huhdanpaa, <a
 * href=http://citeseer.ist.psu.edu/barber96quickhull.html> ``The Quickhull
 * Algorithm for Convex Hulls''</a> (ACM Transactions on Mathematical Software,
 * Vol. 22, No. 4, December 1996), and has a complexity of O(n log(n)) with
 * respect to the number of points. A well-known C implementation of Quickhull
 * that works for arbitrary dimensions is provided by <a
 * href=http://www.qhull.org>qhull</a>.
 *
 * <p>A hull is constructed by providing a set of points
 * to either a constructor or a
 * {@link #build(Point3d[]) build} method. After
 * the hull is built, its vertices and faces can be retrieved
 * using {@link #getVertices()
 * getVertices} and {@link #getFaces() getFaces}.
 * A typical usage might look like this:
 * <pre>
 *   // x y z coordinates of 6 points
 *   Point3d[] points = new Point3d[]
 *    { new Point3d (0.0,  0.0,  0.0),
 *      new Point3d (1.0,  0.5,  0.0),
 *      new Point3d (2.0,  0.0,  0.0),
 *      new Point3d (0.5,  0.5,  0.5),
 *      new Point3d (0.0,  0.0,  2.0),
 *      new Point3d (0.1,  0.2,  0.3),
 *      new Point3d (0.0,  2.0,  0.0),
 *    };
 *
 *   QuickHull3D hull = new QuickHull3D();
 *   hull.build (points);
 *
 *   System.out.println ("Vertices:");
 *   Point3d[] vertices = hull.getVertices();
 *   for (int i = 0; i < vertices.length; i++)
 *    { Point3d pnt = vertices[i];
 *      System.out.println (pnt.x + " " + pnt.y + " " + pnt.z);
 *    }
 *
 *   System.out.println ("Faces:");
 *   int[][] faceIndices = hull.getFaces();
 *   for (int i = 0; i < faceIndices.length; i++)
 *    { for (int k = 0; k < faceIndices[i].length; k++)
 *       { System.out.print (faceIndices[i][k] + " ");
 *       }
 *      System.out.println ("");
 *    }
 * </pre>
 * As a convenience, there are also {@link #build(double[]) build}
 * and {@link #getVertices(double[]) getVertex} methods which
 * pass point information using an array of doubles.
 *
 * <h3><a name=distTol>Robustness</h3> Because this algorithm uses floating
 * point arithmetic, it is potentially vulnerable to errors arising from
 * numerical imprecision.  We address this problem in the same way as <a
 * href=http://www.qhull.org>qhull</a>, by merging faces whose edges are not
 * clearly convex. A face is convex if its edges are convex, and an edge is
 * convex if the centroid of each adjacent plane is clearly <i>below</i> the
 * plane of the other face. The centroid is considered below a plane if its
 * distance to the plane is less than the negative of a {@link
 * #getDistanceTolerance() distance tolerance}.  This tolerance represents the
 * smallest distance that can be reliably computed within the available numeric
 * precision. It is normally computed automatically from the point data,
 * although an application may {@link #setExplicitDistanceTolerance set this
 * tolerance explicitly}.
 *
 * <p>Numerical problems are more likely to arise in situations where data
 * points lie on or within the faces or edges of the convex hull. We have
 * tested QuickHull3D for such situations by computing the convex hull of a
 * random point set, then adding additional randomly chosen points which lie
 * very close to the hull vertices and edges, and computing the convex
 * hull again. The hull is deemed correct if {@link #check check} returns
 * <code>true</code>.  These tests have been successful for a large number of
 * trials and so we are confident that QuickHull3D is reasonably robust.
 *
 * <h3>Merged Faces</h3> The merging of faces means that the faces returned by
 * QuickHull3D may be convex polygons instead of triangles. If triangles are
 * desired, the application may {@link #triangulate triangulate} the faces, but
 * it should be noted that this may result in triangles which are very small or
 * thin and hence difficult to perform reliable convexity tests on. In other
 * words, triangulating a merged face is likely to restore the numerical
 * problems which the merging process removed. Hence is it
 * possible that, after triangulation, {@link #check check} will fail (the same
 * behavior is observed with triangulated output from <a
 * href=http://www.qhull.org>qhull</a>).
 *
 * <h3>Degenerate Input</h3>It is assumed that the input points
 * are non-degenerate in that they are not coincident, co-linear, or
 * coplanar, and thus the convex hull has a non-zero volume.
 * If the input points are detected to be degenerate within
 * the {@link #getDistanceTolerance() distance tolerance}, an
 * IllegalArgumentException will be thrown.
 *
 * @author John E. Lloyd, Fall 2004 */
class QuickHull3D() {
    val points = ArrayList<Point3d>()

    constructor(coords: ArrayList<Point3d>) : this() {
        build(coords, coords.size)
    }



    var findIndex: Int = -1

    // estimated size of the point set
    var charLength: Double = 0.0

    var debug = false

    val pointBuffer = ArrayList<Vertex>()
    val vertexPointIndices = ArrayList<Int>()
    val discardedFaces: ArrayList<Face?> = arrayListOf(null, null, null)

    var maxVertexes: Array<Vertex> = Array<Vertex>(3) { Vertex() }
    var minVertexes: Array<Vertex> = Array<Vertex>(3) { Vertex() }

    val _faces = ArrayList<Face>(16)
    val horizon = ArrayList<HalfEdge>(16)

    val newFaces = FaceList()
    val unclaimed = VertexList()
    val claimed =  VertexList()

    var _numVertices: Int = 0
    var _numFaces: Int = 0
    var numPoints: Int = 0

    var explicitTolerance = AUTOMATIC_TOLERANCE
    var tolerance: Double = 0.0



    /**
     * Returns the distance tolerance that was used for the most recently
     * computed hull. The distance tolerance is used to determine when
     * faces are unambiguously convex with respect to each other, and when
     * points are unambiguously above or below a face plane, in the
     * presence of <a href=#distTol>numerical imprecision</a>. Normally,
     * this tolerance is computed automatically for each set of input
     * points, but it can be set explicitly by the application.
     *
     * @return distance tolerance
     * @see QuickHull3D#setExplicitDistanceTolerance
     */
    fun getDistanceTolerance(): Double {
        return tolerance
    }

    fun addPointToFace(vtx: Vertex, face: Face) {
        vtx.face = face
        if (face.outside == null) {
            claimed.add(vtx)
        } else {
            claimed.insertBefore(vtx, face.outside!!)
        }
        face.outside = vtx
    }

    fun removePointFromFace(vtx: Vertex, face: Face) {
        if (vtx == face.outside) {
            if (vtx._next != null && vtx.getNext()!!.face == face) {
                face.outside = vtx.getNext()
            } else {
                face.outside = null
            }
        }
        claimed.delete(vtx)
    }

    fun removeAllPointsFromFace(face: Face): Vertex? {
        if (face.outside != null) {
            var end = face.outside
            while (end!!._next != null && end.getNext()!!.face == face) {
                end = end.getNext()
            }
            claimed.delete(face.outside!!, end)
            end.setNext(null)
            return face.outside
        } else {
            return null
        }
    }


    fun findHalfEdge(tail: Vertex, head: Vertex): HalfEdge? {
        // brute force ... OK, since setHull is not used much
        for (it in _faces) {
            val he: HalfEdge? = it.findEdge(tail, head)
            if (he != null) {
                return he
            }
        }
        return null
    }

    fun setHull(coords: List<Double>, nump: Int,
                faceIndices: ArrayList<ArrayList<Int>>,
                numf: Int) {
        initBuffers(nump)
        setPointsFromDoubleList(coords, nump)
        computeMaxAndMin()
        for (i in 0 until numf) {
            val face = Face.create(pointBuffer, faceIndices[i])
            var he = face.he0
            do {
                val heOpp = findHalfEdge(he.head(), he.tail()!!)
                if (heOpp != null) {
                    he.setOpposite(heOpp)
                }
                he = he.getNext()
            } while (he != face.he0)
            _faces.add(face)
        }
    }


    fun printQHullErrors(proc: Process) {
        var wrote = false
        var es = proc.errorStream
        while (es.available() > 0) {
            System.out.write(es.read())
            wrote = true
        }
        if (wrote) {
            println("")
        }
    }

    fun setFromQHull(coords: List<Double>, nump: Int, triangulate: Boolean) {
        var cmd = arrayListOf("./qhull", "i")
        if (triangulate) {
            cmd.add(" -Qt")
        }
        try {
            val proc = Runtime.getRuntime().exec(cmd.toTypedArray())
            val ps = PrintWriter(OutputStreamWriter(proc.outputStream))
            val stok = StreamTokenizer(
                    InputStreamReader(proc.inputStream))

            ps.println("3 $nump")
            for (i in 0 until nump) {
                ps.println("${coords[i*3+0]} ${coords[i*3+1]} ${coords[i*3+2]}")
            }
            ps.flush()
            ps.close()
            val indexList = ArrayList<Int>(3)
            stok.eolIsSignificant(true)
            printQHullErrors(proc)
            do {
                stok.nextToken()
            } while (stok.sval == null || !stok.sval.startsWith("MERGEexact"))
            for (i in 0 until 4) {
                stok.nextToken()
            }
            if (stok.ttype != StreamTokenizer.TT_NUMBER) {
                println("Expecting number of faces")
                exitProcess(1)
            }
            val numf = stok.nval.toInt()
            stok.nextToken() // clear EOL
            val faceIndices = ArrayList<ArrayList<Int>>()
            for (i in 0 until numf) {
                indexList.clear()
                while (stok.nextToken() != StreamTokenizer.TT_EOL) {
                    if (stok.ttype != StreamTokenizer.TT_NUMBER) {
                        println("Expecting face index")
                        exitProcess(1)
                    }
                    indexList.add(0,  stok.nval.toInt())
                }
                faceIndices[i] = ArrayList<Int>(indexList.size)
                for (idx in indexList) {
                    faceIndices[i].add(idx)
                }
            }
            setHull(coords, nump, faceIndices, numf)
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
    }

    fun printPoints(pw: PrintWriter) {
        for (i in 0 until numPoints) {
            val pnt = pointBuffer[i].pnt
            pw.println("${pnt.x}, ${pnt.y}, ${pnt.z},")
        }
    }

    /**
     * Constructs the convex hull of a set of points whose
     * coordinates are given by an array of doubles.
     *
     * @param coords x, y, and z coordinates of each input
     * point. The length of this array will be three times
     * the number of input points.
     * @throws IllegalArgumentException the number of input points is less
     * than four, or the points appear to be coincident, colinear, or
     * coplanar.
     */
    fun buildFromDoubleList(coords: List<Double>) {
        buildFromDoubleList(coords, coords.size/3)
    }

    /**
     * Constructs the convex hull of a set of points whose
     * coordinates are given by an array of doubles.
     *
     * @param coords x, y, and z coordinates of each input
     * point. The length of this array must be at least three times
     * <code>nump</code>.
     * @param nump number of input points
     * @throws IllegalArgumentException the number of input points is less
     * than four or greater than 1/3 the length of <code>coords</code>,
     * or the points appear to be coincident, colinear, or
     * coplanar.
     */
    fun buildFromDoubleList(coords: List<Double>, nump: Int) {
        if (nump < 4) {
            throw IllegalArgumentException("Less than four input points specified")
        }
        if (coords.size/3 < nump) {
            throw IllegalArgumentException(
                "Coordinate array too small for specified number of points")
        }
        initBuffers(nump)
        setPointsFromDoubleList(coords, nump)
        buildHull()
    }

    /**
     * Constructs the convex hull of a set of points.
     *
     * @param points input points
     * @throws IllegalArgumentException the number of input points is less
     * than four, or the points appear to be coincident, colinear, or
     * coplanar.
     */
    fun build(points: List<Point3d>) {
        build(points, points.size)
    }

    /**
     * Constructs the convex hull of a set of points.
     *
     * @param points input points
     * @param nump number of input points
     * @throws IllegalArgumentException the number of input points is less
     * than four or greater than the length of <code>points</code>, or the
     * points appear to be coincident, colinear, or coplanar.
     */
    fun build(points: List<Point3d>, nump: Int) {
        if (nump < 4) {
            throw IllegalArgumentException("Less than four input points specified")
        }
        if (points.size < nump) {
            throw IllegalArgumentException(
                "Point array too small for specified number of points")
        }
        initBuffers(nump)
        setPoints(points, nump)
        buildHull()
    }

    /**
     * Triangulates any non-triangular hull faces. In some cases, due to
     * precision issues, the resulting triangles may be very thin or small,
     * and hence appear to be non-convex (this same limitation is present
     * in <a href=http://www.qhull.org>qhull</a>).
     */
    fun triangulate() {
        val minArea = 1000 * charLength * DOUBLE_PREC
        newFaces.clear()
        for (face in _faces) {
            if (face.mark == FaceMark.VISIBLE) {
                face.triangulate(newFaces, minArea)
                // splitFace (face);
            }
        }
        var face: Face? = newFaces.first()
        while (face != null) {
            _faces.add(face)
            face = face._next
        }
    }

// 	private void splitFace (Face face)
// 	 {
//  	   Face newFace = face.split();
//  	   if (newFace != null)
//  	    { newFaces.add (newFace);
//  	      splitFace (newFace);
//  	      splitFace (face);
//  	    }
// 	 }

    fun initBuffers(nump: Int) {
        if (pointBuffer.size < nump) {
            val newBuffer = ArrayList<Vertex>()
            vertexPointIndices.clear()
            for (i in 0 until pointBuffer.size) {
                vertexPointIndices.add(0)
            }
            for (i in 0 until pointBuffer.size) {
                newBuffer.add(pointBuffer[i])
            }
            repeat(nump - pointBuffer.size) {
                newBuffer.add(Vertex())
            }
            pointBuffer.clear()
            pointBuffer.addAll(newBuffer)
        }
        _faces.clear()
        claimed.clear()
        _numFaces = 0
        numPoints = nump
    }

    fun setPointsFromDoubleList(coords: List<Double>, nump: Int) {
        for (i in 0 until nump) {
            val vtx = pointBuffer[i]
            vtx.pnt.set(coords[i * 3 + 0], coords[i * 3 + 1], coords[i * 3 + 2])
            vtx.index = i
        }
    }

    fun setPoints(pnts: List<Point3d>, nump: Int) {
        for (i in 0 until nump) {
            val vtx = pointBuffer[i]
            vtx.pnt.set (pnts[i])
            vtx.index = i
        }
    }

    fun computeMaxAndMin() {
        val max = Vector3d()
        val min = Vector3d()

        for (i in 0 until 3) {
            maxVertexes[i] = pointBuffer[0]
            minVertexes[i] = pointBuffer[0]
        }
        max.set(pointBuffer[0].pnt)
        min.set(pointBuffer[0].pnt)

        for (i in 1 until numPoints) {
            val pnt = pointBuffer[i].pnt
            if (pnt.x > max.x) {
                max.x = pnt.x
                maxVertexes[0] = pointBuffer[i]
            } else if (pnt.x < min.x) {
                min.x = pnt.x
                minVertexes[0] = pointBuffer[i]
            }
            if (pnt.y > max.y) {
                max.y = pnt.y
                maxVertexes[1] = pointBuffer[i]
            } else if (pnt.y < min.y) {
                min.y = pnt.y
                minVertexes[1] = pointBuffer[i]
            }
            if (pnt.z > max.z) {
                max.z = pnt.z
                maxVertexes[2] = pointBuffer[i]
            } else if (pnt.z < min.z) {
                min.z = pnt.z
                minVertexes[2] = pointBuffer[i]
            }
        }

        // this epsilon formula comes from QuickHull, and I'm
        // not about to quibble.
        charLength = max(max.x-min.x, max.y-min.y)
        charLength = max(max.z-min.z, charLength)
        tolerance = if (explicitTolerance == AUTOMATIC_TOLERANCE) {
            3*DOUBLE_PREC*(max(max.x.absoluteValue, min.x.absoluteValue) +
                    max(max.y.absoluteValue, min.y.absoluteValue) +
                    max(max.z.absoluteValue, min.z.absoluteValue))
        } else {
            explicitTolerance
        }
    }

    /**
     * Creates the initial simplex from which the hull will be built.
     */
    fun createInitialSimplex() {
        var max = 0.0
        var imax = 0

        for (i in 0 until 3) {
            val diff = maxVertexes[i].pnt.get(i) - minVertexes[i].pnt.get(i)
            if (diff > max) {
                max = diff
                imax = i
            }
        }

        if (max <= tolerance) {
            throw IllegalArgumentException("Input points appear to be coincident")
        }
        val vtx = ArrayList<Vertex>()
        // set first two vertices to be those with the greatest
        // one dimensional separation

        vtx.add(maxVertexes[imax])
        vtx.add(minVertexes[imax])
        vtx.add(Vertex())
        vtx.add(Vertex())

        // set third vertex to be the vertex farthest from
        // the line between vtx0 and vtx1
        val u01 = Vector3d()
        val diff02 = Vector3d()
        val nrml = Vector3d()
        val xprod = Vector3d()
        var maxSqr = 0.0
        u01.sub(vtx[1].pnt, vtx[0].pnt)
        u01.normalize()
        for (i in 0 until numPoints) {
            diff02.sub(pointBuffer[i].pnt, vtx[0].pnt)
            xprod.cross(u01, diff02)
            val lenSqr = xprod.normSquared()
            if (lenSqr > maxSqr &&
                pointBuffer[i] != vtx[0] &&  // paranoid
                pointBuffer[i] != vtx[1]) {
                maxSqr = lenSqr
                vtx[2] = pointBuffer[i]
                nrml.set(xprod)
            }
        }
        if (sqrt(maxSqr) <= 100*tolerance) {
            throw IllegalArgumentException("Input points appear to be colinear")
        }
        nrml.normalize()


        var maxDist = 0.0
        var d0 = vtx[2].pnt.dot(nrml)
        for (i in 0 until numPoints) {
            val dist = (pointBuffer[i].pnt.dot(nrml) - d0).absoluteValue
            if (dist > maxDist &&
                pointBuffer[i] != vtx[0] &&  // paranoid
                pointBuffer[i] != vtx[1] &&
                pointBuffer[i] != vtx[2]) {
                maxDist = dist
                vtx[3] = pointBuffer[i]
            }
        }
        if (maxDist.absoluteValue <= 100*tolerance) {
            throw IllegalArgumentException("Input points appear to be coplanar")
        }

        if (debug) {
            println("initial vertices:")
            println("${vtx[0].index}: ${vtx[0].pnt}")
            println("${vtx[1].index}: ${vtx[1].pnt}")
            println("${vtx[2].index}: ${vtx[2].pnt}")
            println("${vtx[3].index}: ${vtx[3].pnt}")
        }

        val tris = ArrayList<Face>()

        if (vtx[3].pnt.dot(nrml) - d0 < 0) {
            tris.add(Face.createTriangle(vtx[0], vtx[1], vtx[2]))
            tris.add(Face.createTriangle(vtx[3], vtx[1], vtx[0]))
            tris.add(Face.createTriangle(vtx[3], vtx[2], vtx[1]))
            tris.add(Face.createTriangle(vtx[3], vtx[0], vtx[2]))

            for (i in 0 until 3) {
                val k = (i+1)%3
                tris[i+1].getEdge(1).setOpposite(tris[k+1].getEdge(0))
                tris[i+1].getEdge(2).setOpposite(tris[0].getEdge(k))
            }
        } else {
            tris.add(Face.createTriangle(vtx[0], vtx[2], vtx[1]))
            tris.add(Face.createTriangle(vtx[3], vtx[0], vtx[1]))
            tris.add(Face.createTriangle(vtx[3], vtx[1], vtx[2]))
            tris.add(Face.createTriangle(vtx[3], vtx[2], vtx[0]))
            for (i in 0 until 3) {
                val k = (i+1)%3
                tris[i+1].getEdge(0).setOpposite(tris[k+1].getEdge(1))
                tris[i+1].getEdge(2).setOpposite(tris[0].getEdge((3-i)%3))
            }
        }


        for (i in 0 until 4) {
            _faces.add (tris[i])
        }

        for (i in 0 until numPoints) {
            val v = pointBuffer[i]
            if (v == vtx[0] || v == vtx[1] || v == vtx[2] || v == vtx[3]) {
                continue
            }

            maxDist = tolerance
            var maxFace: Face? = null
            for (k in 0 until 4) {
                val dist = tris[k].distanceToPlane(v.pnt)
                if (dist > maxDist) {
                    maxFace = tris[k]
                    maxDist = dist
                }
            }
            if (maxFace != null) {
                addPointToFace(v, maxFace)
            }
        }
    }


    /**
     * Returns the vertex points in this hull.
     *
     * @return array of vertex points
     * @see QuickHull3D#getVertices(double[])
     * @see QuickHull3D#getFaces()
     */
    fun getVertices(): List<Point3d> {
        val vtxs = ArrayList<Point3d>()
        for (i in 0 until _numVertices) {
            vtxs.add(pointBuffer[vertexPointIndices[i]].pnt)
        }
        return vtxs
    }

    /**
     * Returns the coordinates of the vertex points of this hull.
     *
     * @param coords returns the x, y, z coordinates of each vertex.
     * This length of this array must be at least three times
     * the number of vertices.
     * @return the number of vertices
     * @see QuickHull3D#getVertices()
     * @see QuickHull3D#getFaces()
     */
    fun getVertices(coords: ArrayList<Double>): Int {
        for (i in 0 until _numVertices) {
            val pnt = pointBuffer[vertexPointIndices[i]].pnt
            coords.add(pnt.x)
            coords.add(pnt.y)
            coords.add(pnt.z)
        }
        return _numVertices
    }

    /**
     * Returns an array specifying the index of each hull vertex
     * with respect to the original input points.
     *
     * @return vertex indices with respect to the original points
     */
    fun getVertexPointIndices(): List<Int> {
        val indices = ArrayList<Int>()
        for (i in 0 until _numVertices) {
            indices.add(vertexPointIndices[i])
        }
        return indices
    }


    fun getNumFaces(): Int {
        return _faces.size
    }

    /**
     * Returns the faces associated with this hull.
     *
     * <p>Each face is represented by an integer array which gives the
     * indices of the vertices. These indices are numbered
     * relative to the
     * hull vertices, are zero-based,
     * and are arranged counter-clockwise. More control
     * over the index format can be obtained using
     * {@link #getFaces(int) getFaces(indexFlags)}.
     *
     * @return array of integer arrays, giving the vertex
     * indices for each face.
     * @see QuickHull3D#getVertices()
     * @see QuickHull3D#getFaces(int)
     */
    fun getFaces(): ArrayList<ArrayList<Int>?> {
        return getFaces(0)
    }

    /**
     * Returns the faces associated with this hull.
     *
     * <p>Each face is represented by an integer array which gives the
     * indices of the vertices. By default, these indices are numbered with
     * respect to the hull vertices (as opposed to the input points), are
     * zero-based, and are arranged counter-clockwise. However, this
     * can be changed by setting {@link #POINT_RELATIVE
     * POINT_RELATIVE}, {@link #INDEXED_FROM_ONE INDEXED_FROM_ONE}, or
     * {@link #CLOCKWISE CLOCKWISE} in the indexFlags parameter.
     *
     * @param indexFlags specifies index characteristics (0 results
     * in the default)
     * @return array of integer arrays, giving the vertex
     * indices for each face.
     * @see QuickHull3D#getVertices()
     */
    fun getFaces(indexFlags: Int): ArrayList<ArrayList<Int>?> {
        val allFaces = ArrayList<ArrayList<Int>?>()
        for (face in _faces) {
            val indices = ArrayList<Int>()
            allFaces.add(indices)
            getFaceIndices(indices, face, indexFlags)
        }
        return allFaces
    }

    /**
     * Prints the vertices and faces of this hull to the stream ps.
     *
     * <p>
     * This is done using the Alias Wavefront .obj file
     * format, with the vertices printed first (each preceding by
     * the letter <code>v</code>), followed by the vertex indices
     * for each face (each
     * preceded by the letter <code>f</code>).
     *
     * <p>The face indices are numbered with respect to the hull vertices
     * (as opposed to the input points), with a lowest index of 1, and are
     * arranged counter-clockwise. More control over the index format can
     * be obtained using
     * {@link #print(PrintStream,int) print(ps,indexFlags)}.
     *
     * @param pw stream used for printing
     * @see QuickHull3D#print(PrintStream,int)
     * @see QuickHull3D#getVertices()
     * @see QuickHull3D#getFaces()
     */
    fun print(pw: PrintWriter) {
        print(pw, 0)
    }

    /**
     * Prints the vertices and faces of this hull to the stream ps.
     *
     * <p> This is done using the Alias Wavefront .obj file format, with
     * the vertices printed first (each preceding by the letter
     * <code>v</code>), followed by the vertex indices for each face (each
     * preceded by the letter <code>f</code>).
     *
     * <p>By default, the face indices are numbered with respect to the
     * hull vertices (as opposed to the input points), with a lowest index
     * of 1, and are arranged counter-clockwise. However, this
     * can be changed by setting {@link #POINT_RELATIVE POINT_RELATIVE},
     * {@link #INDEXED_FROM_ONE INDEXED_FROM_ZERO}, or {@link #CLOCKWISE
     * CLOCKWISE} in the indexFlags parameter.
     *
     * @param pw stream used for printing
     * @param inIndexFlags specifies index characteristics
     * (0 results in the default).
     * @see QuickHull3D#getVertices()
     * @see QuickHull3D#getFaces()
     */
    fun print(pw: PrintWriter, inIndexFlags: Int) {
        var indexFlags = inIndexFlags
        if ((indexFlags and INDEXED_FROM_ZERO) == 0) {
            indexFlags = indexFlags or INDEXED_FROM_ONE
        }
        for (i in 0 until _numVertices) {
            val pnt = pointBuffer[vertexPointIndices[i]].pnt
            pw.println("v ${pnt.x} ${pnt.y} ${pnt.z}")
        }
        for (face in _faces) {
            val indices = ArrayList<Int>()
            getFaceIndices(indices, face, indexFlags)
            pw.print("f")
            for (k in 0 until indices.size) {
                pw.print(" ${indices[k]}")
            }
            pw.println ("")
        }
    }

    fun getFaceIndices (indices: ArrayList<Int>, face: Face, flags: Int) {
        val ccw = ((flags and CLOCKWISE) == 0)
        val indexedFromOne = ((flags and INDEXED_FROM_ONE) != 0)
        val pointRelative = ((flags and POINT_RELATIVE) != 0)

        var hedge = face.he0
        do {
            var idx = hedge.head().index
            if (pointRelative) {
                idx = vertexPointIndices[idx]
            }
            if (indexedFromOne) {
                idx++
            }
            indices.add(idx)
            hedge = if (ccw) {
                hedge.getNext()
            } else {
                hedge.getPrev()
            }
        } while (hedge != face.he0)
    }

    fun resolveUnclaimedPoints(newFaces: FaceList) {
        var vtxNext: Vertex? = unclaimed.first()
        var vtx: Vertex? = vtxNext
        while (vtx != null) {
            vtxNext = vtx._next
            var maxDist = tolerance
            var maxFace: Face? = null
            var newFace: Face? = newFaces.first()
            while (newFace != null) {
                if (newFace.mark == FaceMark.VISIBLE) {
                    val dist = newFace.distanceToPlane(vtx.pnt)
                    if (dist > maxDist) {
                        maxDist = dist
                        maxFace = newFace
                    }
                    if (maxDist > 1000 * tolerance) {
                        break
                    }
                }
                newFace = newFace._next
            }
            if (maxFace != null) {
                addPointToFace(vtx, maxFace)
                if (debug && vtx.index == findIndex) {
                    println("$findIndex CLAIMED BY ${maxFace.getVertexString()}")
                }
            } else {
                if (debug && vtx.index == findIndex) {
                    println("$findIndex DISCARDED")
                }
            }
            vtx = vtxNext

        }

    }

    fun deleteFacePoints(face: Face, absorbingFace: Face?) {
        val faceVtxs = removeAllPointsFromFace(face)
        if (faceVtxs != null) {
            if (absorbingFace == null) {
                unclaimed.addAll(faceVtxs)
            } else {
                var vtxNext: Vertex? = faceVtxs
                var vtx = vtxNext
                while (vtx != null) {
                    vtxNext = vtx.getNext()
                    val dist = absorbingFace.distanceToPlane (vtx.pnt)
                    if (dist > tolerance) {
                        addPointToFace(vtx, absorbingFace)
                    } else {
                        unclaimed.add(vtx)
                    }
                    vtx = vtxNext
                }
            }
        }
    }



    fun oppFaceDistance(he: HalfEdge): Double {
        return he.getFace().distanceToPlane(he.getOpposite().getFace().centroid)
    }

    fun doAdjacentMerge(face: Face, mergeType: Int): Boolean {
        var hedge = face.he0

        var convex = true
        do {
            var oppFace = hedge.oppositeFace()
            var merge = false
            var dist1 = 0.0

            if (mergeType == NON_CONVEX) { // then merge faces if they are definitively non-convex
                if (oppFaceDistance(hedge) > -tolerance ||
                    oppFaceDistance(hedge.getOpposite()) > -tolerance) {
                    merge = true
                }
            } else { // mergeType == NONCONVEX_WRT_LARGER_FACE
                // merge faces if they are parallel or non-convex
                // wrt to the larger face; otherwise, just mark
                // the face non-convex for the second pass.
                if (face.area > oppFace!!.area) {
                    dist1 = oppFaceDistance(hedge)
                    if (dist1 > -tolerance) {
                        merge = true
                    } else if (oppFaceDistance(hedge.getOpposite()) > -tolerance) {
                        convex = false
                    }
                } else {
                    if (oppFaceDistance(hedge.getOpposite()) > -tolerance) {
                        merge = true
                    } else if (oppFaceDistance (hedge) > -tolerance) {
                        convex = false
                    }
                }
            }

            if (merge) {
                if (debug) {
                    println("merging ${face.getVertexString()} and ${oppFace?.getVertexString()}")
                }
                var numd = face.mergeAdjacentFace(hedge, discardedFaces)
                for (i in 0 until numd) {
                    deleteFacePoints(discardedFaces[i]!!, face)
                }
                if (debug) {
                    println("  result: ${face.getVertexString()}")
                }
                return true
            }
            hedge = hedge.getNext()
        } while (hedge != face.he0)
        if (!convex) {
            face.mark = FaceMark.NON_CONVEX
        }
        return false
    }

    fun calculateHorizon(
         eyePnt: Point3d,
         inEdge0: HalfEdge?,
         face: Face,
         horizon: ArrayList<HalfEdge>) {
        var edge0 = inEdge0
        deleteFacePoints(face, null)
        face.mark = FaceMark.DELETED
        if (debug) {
            println("  visiting face ${face.getVertexString()}")
        }
        var edge: HalfEdge?
        if (edge0 == null) {
            edge0 = face.getEdge(0)
            edge = edge0
        } else {
            edge = edge0.getNext()
        }
        do {
            val oppFace = edge!!.oppositeFace()
            if (oppFace!!.mark == FaceMark.VISIBLE) {
                if (oppFace.distanceToPlane(eyePnt) > tolerance) {
                    calculateHorizon(eyePnt, edge.getOpposite(),
                        oppFace, horizon)
                } else {
                    horizon.add(edge)
                    if (debug) {
                        println("  adding horizon edge ${edge.getVertexString()}")
                    }
                }
            }
            edge = edge.getNext()
        } while (edge != edge0)
    }

    fun addAdjoiningFace(eyeVtx: Vertex, he: HalfEdge): HalfEdge {
        val face = Face.createTriangle (
                eyeVtx, he.tail()!!, he.head())
        _faces.add (face)
        face.getEdge(-1).setOpposite(he.getOpposite())
        return face.getEdge(0)
    }

    fun addNewFaces(newFaces: FaceList, eyeVtx: Vertex, horizon: ArrayList<HalfEdge>) {
        newFaces.clear()

        var hedgeSidePrev: HalfEdge? = null
        var hedgeSideBegin: HalfEdge? = null

        for (horizonHe in horizon) {
            val hedgeSide = addAdjoiningFace(eyeVtx, horizonHe)
            if (debug) {
                println ("new face: ${hedgeSide.getFace().getVertexString()}")
            }
            if (hedgeSidePrev != null) {
                hedgeSide.getNext().setOpposite(hedgeSidePrev)
            } else {
                hedgeSideBegin = hedgeSide
            }
            newFaces.add(hedgeSide.getFace())
            hedgeSidePrev = hedgeSide
        }
        hedgeSideBegin!!.getNext().setOpposite(hedgeSidePrev!!)
    }

    fun nextPointToAdd(): Vertex? {
        if (!claimed.isEmpty()) {
            val eyeFace = claimed.first()!!.face
            var eyeVtx: Vertex? = null
            var maxDist = 0.0
            var vtx = eyeFace!!.outside
            while (vtx != null && vtx.face == eyeFace) {
                val dist = eyeFace.distanceToPlane(vtx.pnt)
                if (dist > maxDist) {
                    maxDist = dist
                    eyeVtx = vtx
                }
                vtx = vtx._next
            }
            return eyeVtx
        } else {
            return null
        }
    }

    fun addPointToHull(eyeVtx: Vertex) {
        horizon.clear()
        unclaimed.clear()

        if (debug) {
            println("Adding point: ${eyeVtx.index}")
            println(" which is ${eyeVtx.face?.distanceToPlane(eyeVtx.pnt)} above face ${eyeVtx.face?.getVertexString()}")
        }
        removePointFromFace(eyeVtx, eyeVtx.face!!)
        calculateHorizon(eyeVtx.pnt, null, eyeVtx.face!!, horizon)
        newFaces.clear()
        addNewFaces (newFaces, eyeVtx, horizon)

        // first merge pass ... merge faces which are non-convex
        // as determined by the larger face

        var face: Face? = newFaces.first()
        while (face != null) {

            if (face.mark == FaceMark.VISIBLE) {
                while (doAdjacentMerge(face, NON_CONVEX_WRT_LARGER_FACE)) {
                    // do nothing
                }
            }
            face=face._next

        }
        // second merge pass ... merge faces which are non-convex
        // wrt either face
        face = newFaces.first()
        while (face != null) {
            if (face.mark == FaceMark.NON_CONVEX) {
                face.mark = FaceMark.VISIBLE
                while (doAdjacentMerge(face, NON_CONVEX)) {
                    // nothing
                }
            }
            face = face._next
        }
        resolveUnclaimedPoints(newFaces)
    }

    fun buildHull() {
        var cnt = 0
        var eyeVtx: Vertex? = null
        computeMaxAndMin()
        createInitialSimplex()
        eyeVtx = nextPointToAdd()
        while (eyeVtx != null) {
            addPointToHull(eyeVtx)
            cnt++
            if (debug) {
                println("iteration ${cnt} done")
            }
            eyeVtx = nextPointToAdd()
        }
        reindexFacesAndVertices()
        if (debug) {
            println ("hull done")
        }
    }

    fun markFaceVertices(face: Face, mark: Int) {
        var he0 = face.getFirstEdge()
        var he = he0
        do {
            he.head().index = mark
            he = he.getNext()
        } while (he != he0)
    }

    fun reindexFacesAndVertices() {
        for (i in 0 until numPoints) {
            pointBuffer[i].index = -1
        }
        // remove inactive faces and mark active vertices
        _numFaces = 0
        val toRemove = ArrayList<Face>()
        for (face in _faces) {
            if (face.mark != FaceMark.VISIBLE) {
                toRemove.add(face)
            } else {
                markFaceVertices(face, 0)
                _numFaces++
            }
        }
        _faces.removeAll(toRemove)
        // reindex vertices
        _numVertices = 0
        for (i in 0 until numPoints) {
            val vtx = pointBuffer[i]
            if (vtx.index == 0) {
                vertexPointIndices.add(i)
                vtx.index = _numVertices++
            }
        }
    }

    fun checkFaceConvexity (face: Face, tol: Double, pw: PrintWriter?): Boolean {
        var dist = 0.0
        var he = face.he0
        do {
            face.checkConsistency()
            // make sure edge is convex
            dist = oppFaceDistance(he)
            if (dist > tol) {
                pw?.println("Edge ${he.getVertexString()} non-convex by ${dist}")
                return false
            }
            dist = oppFaceDistance(he.getOpposite())
            if (dist > tol) {
                pw?.println("Opposite edge ${he.getOpposite().getVertexString()} non-convex by $dist")
                return false
            }
            if (he.getNext().oppositeFace() == he.oppositeFace()) {
                pw?.println("Redundant vertex ${he.head().index} in face ${face.getVertexString()}")
                return false
            }
            he = he.getNext()
        } while (he != face.he0)
        return true
    }

    fun checkFaces(tol: Double, pw: PrintWriter?): Boolean {
        // check edge convexity
        var convex = true
        for (face in _faces) {
            if (face.mark == FaceMark.VISIBLE) {
                if (!checkFaceConvexity (face, tol, pw)) {
                    convex = false
                }
            }
        }
        return convex
    }

    /**
     * Checks the correctness of the hull using the distance tolerance
     * returned by {@link QuickHull3D#getDistanceTolerance
     * getDistanceTolerance}; see
     * {@link QuickHull3D#check(PrintStream,double)
     * check(PrintStream,double)} for details.
     *
     * @param pw print stream for diagnostic messages; may be
     * set to <code>null</code> if no messages are desired.
     * @return true if the hull is valid
     * @see QuickHull3D#check(PrintStream,double)
     */
    fun check(pw: PrintWriter?): Boolean {
        return check(pw, getDistanceTolerance())
    }

    /**
     * Checks the correctness of the hull. This is done by making sure that
     * no faces are non-convex and that no points are outside any face.
     * These tests are performed using the distance tolerance <i>tol</i>.
     * Faces are considered non-convex if any edge is non-convex, and an
     * edge is non-convex if the centroid of either adjoining face is more
     * than <i>tol</i> above the plane of the other face. Similarly,
     * a point is considered outside a face if its distance to that face's
     * plane is more than 10 times <i>tol</i>.
     *
     * <p>If the hull has been {@link #triangulate triangulated},
     * then this routine may fail if some of the resulting
     * triangles are very small or thin.
     *
     * @param pw print stream for diagnostic messages; may be
     * set to <code>null</code> if no messages are desired.
     * @param tol distance tolerance
     * @return true if the hull is valid
     * @see QuickHull3D#check(PrintStream)
     */
    fun check (pw: PrintWriter?, tol: Double): Boolean {
        // check to make sure all edges are fully connected
        // and that the edges are convex
        var dist = 0.0
        val pointTol = 10.0*tol

        if (!checkFaces(tolerance, pw)) {
            return false
        }

        // check point inclusion
        for (i in 0 until numPoints) {
            val pnt = pointBuffer[i].pnt
            for (face in _faces) {
                if (face.mark == FaceMark.VISIBLE) {
                    dist = face.distanceToPlane(pnt)
                    if (dist > pointTol) {
                        pw?.println("Point $i $dist above face ${face.getVertexString()}")
                        pw?.flush()
                        return false
                    }
                }
            }
        }
        return true
    }

    companion object {
        fun doubleListToPoints(dl: List<Double>): ArrayList<Point3d> {
            if (dl.size % 3 != 0) {
                throw IllegalArgumentException("List of point data has an invalid number of values - must be a multiple of 3")
            }
            val points = ArrayList<Point3d>()
            for (i in 0 until dl.size step 3) {
                val pt = Point3d(dl[i], dl[i + 1], dl[i + 2])
                points.add(pt)
            }
            return points
        }

        const val NON_CONVEX_WRT_LARGER_FACE: Int = 1
        const val NON_CONVEX: Int = 2
        /**
         * Specifies that (on output) vertex indices for a face should be
         * listed in clockwise order.
         */
        const val CLOCKWISE = 0x1

        /**
         * Specifies that (on output) the vertex indices for a face should be
         * numbered starting from 1.
         */
        const val INDEXED_FROM_ONE = 0x2

        /**
         * Specifies that (on output) the vertex indices for a face should be
         * numbered starting from 0.
         */
        const val INDEXED_FROM_ZERO = 0x4

        /**
         * Specifies that (on output) the vertex indices for a face should be
         * numbered with respect to the original input points.
         */
        const val POINT_RELATIVE = 0x8

        /**
         * Specifies that the distance tolerance should be
         * computed automatically from the input point data.
         */
        const val AUTOMATIC_TOLERANCE = -1.0

        /**
         * Precision of a double.
         */
        const val DOUBLE_PREC: Double = 2.2204460492503131e-16


        /**
         * Creates a convex hull object and initializes it to the convex hull
         * of a set of points whose coordinates are given by an
         * array of doubles.
         *
         * @param coords x, y, and z coordinates of each input
         * point. The length of this array will be three times
         * the the number of input points.
         * @throws IllegalArgumentException the number of input points is less
         * than four, or the points appear to be coincident, colinear, or
         * coplanar.
         */
        fun fromDoubleList(coords: ArrayList<Double>) = QuickHull3D(doubleListToPoints(coords))

    }
}
