package org.goodmath.simplex.kcsg.quickhull

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.Random
import kotlin.math.sin
import kotlin.math.cos


/**
 * Testing class for QuickHull3D. Running the command
 * <pre>
 *   java quickhull3d.QuickHull3DTest
 * </pre>
 * will cause QuickHull3D to be tested on a number of randomly
 * choosen input sets, with degenerate points added near
 * the edges and vertics of the convex hull.
 *
 * <p>The command
 * <pre>
 *   java quickhull3d.QuickHull3DTest -timing
 * </pre>
 * will cause timing information to be produced instead.
 *
 * @author John E. Lloyd, Fall 2004
 */
class QuickHull3DTest {

    val rand = Random() // random number generator

    init {
        rand.setSeed(0x1234)
    }


    /**
     * Returns true if two face index sets are equal,
     * modulo a cyclical permuation.
     *
     * @param indices1 index set for first face
     * @param indices2 index set for second face
     * @return true if the index sets are equivalent
     */
    fun faceIndicesEqual(indices1: List<Int>, indices2: List<Int>): Boolean {
        if (indices1.size != indices2.size) {
            return false
        }
        val len = indices1.size
        var j = 0
        while (j < len) {
            if (indices1[0] == indices2[j]) {
                break
            }
            j++
        }
        if (j == len) {
            return false
        }
        for (i in 1 until len) {
            if (indices1[i] != indices2[(j + i) % len]) {
                return false
            }
        }
        return true
    }

    /**
     * Returns the coordinates for <code>num</code> points whose x, y, and
     * z values are randomly chosen within a given range.
     *
     * @param num number of points to produce
     * @param range coordinate values will lie between -range and range
     * @return array of coordinate values
     */
    fun randomPoints(num: Int, range: Double): ArrayList<Double> {
        val coords = ArrayList<Double>(num * 3)
        for (i in 0 until num * 3) {
            coords.add(0.0)
        }

        for (i in 0 until num) {
            for (k in 0 until 3) {
                coords[i * 3 + k] = 2 * range * (rand.nextDouble() - 0.5)
            }
        }
        return coords
    }

    fun randomlyPerturb(pnt: Point3d, tol: Double) {
        pnt.x += tol * (rand.nextDouble() - 0.5)
        pnt.y += tol * (rand.nextDouble() - 0.5)
        pnt.z += tol * (rand.nextDouble() - 0.5)
    }

    /**
     * Returns the coordinates for <code>num</code> randomly
     * chosen points which are degenerate which respect
     * to the specified dimensionality.
     *
     * @param num number of points to produce
     * @param dimen dimensionality of degeneracy: 0 = coincident,
     * 1 = colinear, 2 = coplaner.
     * @return array of coordinate values
     */
    fun randomDegeneratePoints(num: Int, dimen: Int): ArrayList<Double> {
        val coords = ArrayList<Double>(num * 3)
        for (i in 0 until num * 3) {
            coords.add(0.0)
        }
        val pnt = Point3d()

        val base = Point3d()
        base.setRandom(-1.0, 1.0, rand)

        val tol = DOUBLE_PREC

        if (dimen == 0) {
            for (i in 0 until num) {
                pnt.set(base)
                randomlyPerturb(pnt, tol)
                coords[i * 3 + 0] = pnt.x
                coords[i * 3 + 1] = pnt.y
                coords[i * 3 + 2] = pnt.z
            }
        } else if (dimen == 1) {
            val u = Vector3d()
            u.setRandom(-1.0, 1.0, rand)
            u.normalize()
            for (i in 0 until num) {
                val a = 2 * (rand.nextDouble() - 0.5)
                pnt.scale(a, u)
                pnt.add(base)
                randomlyPerturb(pnt, tol)
                coords[i * 3 + 0] = pnt.x
                coords[i * 3 + 1] = pnt.y
                coords[i * 3 + 2] = pnt.z
            }
        } else {// dimen == 2
            val nrm = Vector3d()
            nrm.setRandom(-1.0, 1.0, rand)
            nrm.normalize()
            for (i in 0 until num) {
                // compute a random point and project it to the plane
                val perp = Vector3d()
                pnt.setRandom(-1.0, 1.0, rand)
                perp.scale(pnt.dot(nrm), nrm)
                pnt.sub(perp)
                pnt.add(base)
                randomlyPerturb(pnt, tol)
                coords[i * 3 + 0] = pnt.x
                coords[i * 3 + 1] = pnt.y
                coords[i * 3 + 2] = pnt.z
            }
        }
        return coords
    }

    /**
     * Returns the coordinates for <code>num</code> points whose x, y, and
     * z values are randomly chosen to lie within a sphere.
     *
     * @param num number of points to produce
     * @param radius radius of the sphere
     * @return array of coordinate values
     */
    fun randomSphericalPoints(num: Int, radius: Double): ArrayList<Double> {
        val coords = ArrayList<Double>(num * 3)
        val pnt = Point3d()

        var i = 0
        while (i < num) {
            pnt.setRandom(-radius, radius, rand)
            if (pnt.norm() <= radius) {
                coords.add(pnt.x)
                coords.add(pnt.y)
                coords.add(pnt.z)
                i++
            }
        }
        return coords
    }

    /**
     * Returns the coordinates for <code>num</code> points whose x, y, and
     * z values are each randomly chosen to lie within a specified
     * range, and then clipped to a maximum absolute
     * value. This means a large number of points
     * may lie on the surface of cube, which is useful
     * for creating degenerate convex hull situations.
     *
     * @param num number of points to produce
     * @param range coordinate values will lie between -range and
     * range, before clipping
     * @param max maximum absolute value to which the coordinates
     * are clipped
     * @return array of coordinate values
     */
    fun randomCubedPoints(num: Int, range: Double, max: Double): ArrayList<Double> {
        val coords = ArrayList<Double>(num * 3)

        for (i in 0 until num) {
            for (k in 0 until 3) {
                var x = 2 * range * (rand.nextDouble() - 0.5)
                if (x > max) {
                    x = max
                } else if (x < -max) {
                    x = -max
                }
                coords.add(x)
            }
        }
        return coords
    }

    fun shuffleCoords(coords: ArrayList<Double>): ArrayList<Double> {
        val num = coords.size / 3

        for (i in 0 until num) {
            val i1 = rand.nextInt(num)
            val i2 = rand.nextInt(num)
            for (k in 0 until 0) {
                val tmp = coords[i1 * 3 + k]
                coords[i1 * 3 + k] = coords[i2 * 3 + k]
                coords[i2 * 3 + k] = tmp
            }
        }
        return coords
    }

    /**
     * Returns randomly shuffled coordinates for points on a
     * three-dimensional grid, with a presecribed width between each point.
     *
     * @param gridSize number of points in each direction,
     * so that the total number of points produced is the cube of
     * gridSize.
     * @param width distance between each point along a particular
     * direction
     * @return array of coordinate values
     */
    fun randomGridPoints(gridSize: Int, width: Double): ArrayList<Double> {
        // gridSize gives the number of points across a given dimension
        // any given coordinate indexed by i has value
        // (i/(gridSize-1) - 0.5)*width

        val num = gridSize * gridSize * gridSize
        val coords = ArrayList<Double>(num * 3)
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                for (k in 0 until gridSize) {
                    coords.add((i.toDouble() / (gridSize - 1).toDouble() - 0.5) * width)
                    coords.add((j.toDouble() / (gridSize - 1).toDouble() - 0.5) * width)
                    coords.add((k.toDouble() / (gridSize - 1).toDouble() - 0.5) * width)
                }
            }
        }
        shuffleCoords(coords)
        return coords
    }

    fun explicitFaceCheck(hull: QuickHull3D, checkFaces: ArrayList<ArrayList<Int>>) {
        val faceIndices = hull.getFaces()
        if (faceIndices.size != checkFaces.size) {
            throw Exception(
                "Error: " + faceIndices.size + " faces vs. " + checkFaces.size
            )
        }
        // translate face indices back into original indices
        val pnts = hull.getVertices()
        val vtxIndices = hull.getVertexPointIndices()

        for (j in 0 until faceIndices.size) {
            val idxs = faceIndices[j]!!
            for (k in 0 until idxs.size) {
                idxs[k] = vtxIndices[idxs[k]]
            }
        }
        for (i in 0 until checkFaces.size) {
            val cf = checkFaces[i]
            var j = 0
            while (j < faceIndices.size) {
                if (faceIndices[j] != null) {
                    if (faceIndicesEqual(cf, faceIndices[j]!!)) {
                        faceIndices[j] = null
                        break
                    }
                }
                j++
            }
            if (j == faceIndices.size) {
                var s = ""
                for (k in 0 until cf.size) {
                    s += "${cf[k]} "
                }
                throw Exception("Error: face $s not found")
            }
        }
    }

    var cnt = 0

    fun singleTest(coords: ArrayList<Double>, checkFaces: ArrayList<ArrayList<Int>>?) {
        val hull = QuickHull3D.fromDoubleList(coords)
        hull.debug = debugEnable

        if (triangulate) {
            hull.triangulate()
        }

        assertTrue(hull.check(PrintWriter(OutputStreamWriter(System.err))))
        if (checkFaces != null) {
            explicitFaceCheck(hull, checkFaces)
        }
        if (degeneracyTest != NO_DEGENERACY) {
            degenerateTest(hull, coords)
        }
    }

    fun addDegeneracy(type: Int, coords: List<Double>, hull: QuickHull3D): ArrayList<Double> {
        var numv = coords.size / 3
        val faces = hull.getFaces()
        val coordsx = ArrayList<Double>(coords.size + faces.size * 3)
        for (i in 0 until coords.size) {
            coordsx.add(coords[i])
        }
        for (i in 0 until faces.size * 3) {
            coordsx.add(0.0)
        }

        val lam = Array<Double>(3) { 0.0 }
        val eps = hull.getDistanceTolerance()

        for (i in 0 until faces.size) {
            // random point on an edge
            lam[0] = rand.nextDouble()
            lam[1] = 1 - lam[0]
            lam[2] = 0.0

            if (type == VERTEX_DEGENERACY && (i % 2 == 0)) {
                lam[0] = 1.0
                lam[1] = 0.0
                lam[2] = 0.0
            }

            for (j in 0 until 3) {
                val vtxi = faces[i]!![j]
                for (k in 0 until 3) {
                    coordsx[numv * 3 + k] += lam[j] * coords[vtxi * 3 + k] +
                            epsScale * eps * (rand.nextDouble() - 0.5)
                }
            }
            numv++
        }
        shuffleCoords(coordsx)
        return coordsx
    }

    fun degenerateTest(hull: QuickHull3D, coords: ArrayList<Double>) {
        val coordsx = addDegeneracy(degeneracyTest, coords, hull)
        var xhull: QuickHull3D? = null
        try {
            xhull = QuickHull3D.fromDoubleList(coordsx)
            if (triangulate) {
                xhull.triangulate()
            }
            assertTrue(xhull.check(PrintWriter(OutputStreamWriter(System.out))))
        } catch (e: Exception) {
            for (i in 0 until coordsx.size / 3) {
                println(
                    "${coordsx[i * 3 + 0]}, ${coordsx[i * 3 + 1]}, ${coordsx[i * 3 + 2]},"
                )
            }
        }
        assertTrue(xhull != null && xhull.check(PrintWriter(OutputStreamWriter(System.out))))
    }

    fun rotateCoords(
        res: ArrayList<Double>, xyz: ArrayList<Double>,
        roll: Double, pitch: Double, yaw: Double
    ) {
        val sroll: Double = sin(roll)
        val croll = cos(roll)
        val spitch = sin(pitch)
        val cpitch = cos(pitch)
        val syaw = sin(yaw)
        val cyaw = cos(yaw)

        val m00 = croll * cpitch
        val m10 = sroll * cpitch
        val m20 = -spitch

        val m01 = croll * spitch * syaw - sroll * cyaw
        val m11 = sroll * spitch * syaw + croll * cyaw
        val m21 = cpitch * syaw

        val m02 = croll * spitch * cyaw + sroll * syaw
        val m12 = sroll * spitch * cyaw - croll * syaw
        val m22 = cpitch * cyaw

        var x = 0.0
        var y = 0.0
        var z = 0.0

        for (i in 0 until xyz.size - 2 step 3) {
            res[i + 0] = m00 * xyz[i + 0] + m01 * xyz[i + 1] + m02 * xyz[i + 2]
            res[i + 1] = m10 * xyz[i + 0] + m11 * xyz[i + 1] + m12 * xyz[i + 2]
            res[i + 2] = m20 * xyz[i + 0] + m21 * xyz[i + 1] + m22 * xyz[i + 2]
        }
    }

    fun printCoords(coords: List<Double>) {
        val nump = coords.size / 3
        for (i in 0 until nump) {
            println("${coords[i * 3 + 0]}, ${coords[i * 3 + 1]}, ${coords[i * 3 + 2]}, ")
        }
    }

    fun testException(coords: ArrayList<Double>, msg: String) {
        var ex: Exception? = null
        try {
            val hull = QuickHull3D.fromDoubleList(coords)
        } catch (e: Exception) {
            ex = e
        }
        assertEquals(ex?.message, msg)
    }

    fun test(coords: ArrayList<Double>, checkFaces: ArrayList<ArrayList<Int>>?) {
        val rpyList = arrayOf(
            arrayOf(0.0, 0.0, 0.0),
            arrayOf(10.0, 20.0, 30.0),
            arrayOf(-45.0, 60.0, 91.0),
            arrayOf(125.0, 67.0, 81.0)
        )
        var xcoords = ArrayList<Double>(coords.size)
        for (i in 0 until coords.size) {
            xcoords.add(0.0)
        }

        singleTest(coords, checkFaces)
        if (testRotation) {
            for (i in 0 until rpyList.size) {
                val rpy = rpyList[i]
                rotateCoords(
                    xcoords,
                    coords,
                    Math.toRadians(rpy[0]),
                    Math.toRadians(rpy[1]),
                    Math.toRadians(rpy[2])
                )
                singleTest(xcoords, checkFaces)
            }
        }
    }

    /**
     * Runs a set of explicit and random tests on QuickHull3D,
     * and prints <code>Passed</code> to System.out if all is well.
     */
    @Test
    fun explicitAndRandomTests() {
        var coords: ArrayList<Double>? = null
        println("Testing degenerate input ...")
        for (dimen in 0 until 3) {
            for (i in 0 until 10) {
                coords = randomDegeneratePoints(10, dimen)
                if (dimen == 0) {
                    testException(coords, "Input points appear to be coincident")
                } else if (dimen == 1) {
                    testException(coords, "Input points appear to be co-linear")
                } else if (dimen == 2) {
                    testException(coords, "Input points appear to be coplanar")
                }
            }
        }
        println("Explicit tests ...")
        // test cases furnished by Mariano Zelke, Berlin
        coords = arrayListOf(
            21.0, 0.0, 0.0,
            0.0, 21.0, 0.0,
            0.0, 0.0, 0.0,
            18.0, 2.0, 6.0,
            1.0, 18.0, 5.0,
            2.0, 1.0, 3.0,
            14.0, 3.0, 10.0,
            4.0, 14.0, 14.0,
            3.0, 4.0, 10.0,
            10.0, 6.0, 12.0,
            5.0, 10.0, 15.0
        )
        test(coords, null)

        coords = arrayListOf(
            0.0, 0.0, 0.0,
            21.0, 0.0, 0.0,
            0.0, 21.0, 0.0,
            2.0, 1.0, 2.0,
            17.0, 2.0, 3.0,
            1.0, 19.0, 6.0,
            4.0, 3.0, 5.0,
            13.0, 4.0, 5.0,
            3.0, 15.0, 8.0,
            6.0, 5.0, 6.0,
            9.0, 6.0, 11.0,
        )
        test(coords, null)

        println("Testing 20 to 200 random points ...")
        for (n in 20 until 200 step 10) {
            for (i in 0 until 10) {
                coords = randomPoints(n, 1.0)
                test(coords, null)
            }
        }

        println("Testing 20 to 200 random points in a sphere ...")
        for (n in 20 until 200 step 10) {
            for (i in 0 until 10) {
                coords = randomSphericalPoints(n, 1.0)
                test(coords, null)
            }
        }

        println("Testing 20 to 200 random points clipped to a cube ...")
        for (n in 20 until 200 step 10) {
            for (i in 0 until 10) {
                coords = randomCubedPoints(n, 1.0, 0.5)
                test(coords, null)
            }
        }

        println("Testing 8 to 1000 randomly shuffled points on a grid ...")
        for (n in 2 until 10) {
            for (i in 0 until 10) {
                coords = randomGridPoints(n, 4.0)
                test(coords, null)
            }
        }
        println("\nPassed\n")
    }

    /**
     * Runs timing tests on QuickHull3D, and prints
     * the results to System.out.
     */
    @Test
    fun timingTests() {
        var t0 = 0L
        var t1 = 0L
        var n = 10


        println("warming up ... ")
        for (i in 0 until 2) {
            val coords = randomSphericalPoints(10000, 1.0)
            val hull = QuickHull3D.fromDoubleList(coords)
        }
        val cnt = 10
        for (i in 0 until 4) {
            n *= 10
            val coords = randomSphericalPoints(n, 1.0)
            t0 = System.currentTimeMillis()
            for (k in 0 until cnt) {
                val hull = QuickHull3D.fromDoubleList(coords)
            }
            t1 = System.currentTimeMillis()
            println("$n points: ${(t1 - t0) / cnt.toDouble()} msec")
        }
    }

    companion object {
        const val DOUBLE_PREC = 2.2204460492503131e-16
        var triangulate = false
        var doTesting = true
        var doTiming = false
        var debugEnable = true
        const val NO_DEGENERACY = 0
        const val EDGE_DEGENERACY = 1
        const val VERTEX_DEGENERACY = 2
        var testRotation = true
        var degeneracyTest = VERTEX_DEGENERACY
        var epsScale = 2.0

    }
}
