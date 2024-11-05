package org.goodmath.simplex.vvecmath

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.assertEquals

/*
 * Copyright 2017-2019 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * If you use this software for scientific research then please cite the following publication(s):
 *
 * M. Hoffer, C. Poliwoda, & G. Wittum. (2013). Visual reflection library:
 * a framework for declarative GUI programming on the Java platform.
 * Computing and Visualization in Science, 2013, 16(4),
 * 181–192. http://doi.org/10.1007/s00791-014-0230-y
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Michael Hoffer <info@michaelhoffer.de>.
 */
class Vector3dTest {

    val EPSILON = 1e-12

    fun assertDoubleEquals(expected: Double, actual: Double, epsilon: Double) {
        assertEquals(expected, actual, epsilon, "expected value diverges from actual value! Delta: ${(expected - actual).absoluteValue}")
    }

    fun randomRotation(): Transform {
        return Transform.unity().rot(
            360.0 * random.nextDouble(),
            360.0 * random.nextDouble(),
            360.0 * random.nextDouble());
    }

    fun randomVector(): Vector3d {
        return Vector3d.xyz(random.nextDouble(), random.nextDouble(), random.nextDouble())
    }

    @Test
    fun getterTest() {
        val x = 100.0 * random.nextDouble()
        val y = 100.0 * random.nextDouble()
        val z = 100.0 * random.nextDouble()

        val v = Vector3d.xyz(x, y, z)

        val buff = v.get()

        var xDelta = (x - buff[0]).absoluteValue
        assertEquals( x, buff[0], EPSILON, "getter value diverges from set value! Delta: $xDelta")

        var yDelta = (y - buff[1]).absoluteValue
        assertEquals(y, buff[1], EPSILON, "getter value diverges from set value! Delta: $yDelta")

        var zDelta = (z - buff[2]).absoluteValue
        assertEquals(z, buff[2], EPSILON, "getter value diverges from set value! Delta: $zDelta")

        xDelta = (x - v[0]).absoluteValue
        assertEquals( x, v[0], EPSILON, "getter value diverges from set value! Delta: $xDelta")

        yDelta = (y - v[1]).absoluteValue
        assertEquals(y, v[1], EPSILON, "getter value diverges from set value! Delta: $yDelta")

        zDelta = (z - v[2]).absoluteValue
        assertEquals(z, v[2], EPSILON, "getter value diverges from set value! Delta: $zDelta")


        xDelta = (x - v.x).absoluteValue
        assertEquals(x, v.x, EPSILON, "getter value diverges from set value! Delta: $xDelta")

        yDelta = Math.abs(y - v.y).absoluteValue
        assertEquals(y, v.y, EPSILON, "getter value diverges from set value! Delta: $yDelta")


        zDelta = Math.abs(z - v.z).absoluteValue
        assertEquals(z, v.z, EPSILON, "getter value diverges from set value! Delta: $zDelta")



    }

    @Test
    fun plusMinusAddedSubtractedTest() {
        val randRot = randomRotation()
        val angleTrans = Transform.unity().rotZ(180.0 * random.nextDouble())

        val va = randRot.transform(Vector3d.X_ONE.times(100.0 * random.nextDouble()))
        val vc = randRot.transform(angleTrans.transform(Vector3d.X_ONE.times(100.0 * random.nextDouble())))
        val vb = Vector3d.xyz(vc.x- va.x, vc.y - va.y, vc.z - va.z)


        val ac = va.added(vb)
        assertDoubleEquals( ac.magnitude(), ac.magnitude(), EPSILON)


        val pc = va.plus(vb)
        assertDoubleEquals(pc.magnitude(), vc.magnitude(), EPSILON)

        val p2c = va.plus(vb.x, vb.y, vb.z)
        assertDoubleEquals(p2c.magnitude(), vc.magnitude(), EPSILON)

        val sb = vc.subtracted(va)
        assertDoubleEquals(sb.magnitude(), vb.magnitude(), EPSILON)

        val mb = vc.minus(va)
        assertDoubleEquals(mb.magnitude(), vb.magnitude(), EPSILON)

        val m2b = vc.minus(va.x, va.y, va.z)
        assertDoubleEquals(m2b.magnitude(), vb.magnitude(), EPSILON)
    }

    @Test
    fun timesMultipliedDividedTest() {

        val randRot = randomRotation();
        val angleTrans = Transform.unity().rotZ(180.0 * random.nextDouble())
        val alpha = 100.0 * random.nextDouble()

        val va = randRot.transform(Vector3d.X_ONE)
        val vb = randRot.transform(angleTrans.transform(Vector3d.X_ONE.times(100.0 * random.nextDouble())))

        // times
        val ta = va.times(alpha)
        assertDoubleEquals(alpha, ta.magnitude(), EPSILON)

        val t2a = va.times(vb)
        var dot = t2a.x + t2a.y + t2a.z
        assertDoubleEquals(va.dot(vb), dot, EPSILON)

        val t3a = va.times(vb.x, vb.y, vb.z)
        dot = t3a.x + t3a.y + t3a.z
        assertDoubleEquals(va.dot(vb), dot, EPSILON)

        // multiplied
        val t4a = va.multiplied(alpha)
        assertDoubleEquals(alpha, t4a.magnitude(), EPSILON)

        val t5a = va.multiplied(vb)
        dot = t5a.x + t5a.y + t5a.z
        assertDoubleEquals(va.dot(vb), dot, EPSILON)

        // divided
        val t6a = va.divided(alpha)
        assertDoubleEquals(1/alpha, t6a.magnitude(), EPSILON)
    }

    /*
    @Test
    public void dotTest() {
        Transform randRot = RandomRotation();

        Vector3d va = randRot.transform(Vector3d.X_ONE.times(100.0 * random.nextDouble()));
        Vector3d vb = randRot.transform(Vector3d.Y_ONE.times(100.0 * random.nextDouble()));
        Vector3d vc = randRot.transform(Vector3d.Z_ONE.times(100.0 * random.nextDouble()));

        double delta = va.dot(vb);
        Assert.assertEquals(String.format("scalar product of perpendicular vectors diverges from zero! Delta:%f", delta), 0, va.dot(vb), EPSILON);

        delta = vb.dot(vc);
        Assert.assertEquals(String.format("scalar product of perpendicular vectors diverges from zero! Delta:%f", delta), 0, vb.dot(vc), EPSILON);

        delta = vc.dot(va);
        Assert.assertEquals(String.format("scalar product of perpendicular vectors diverges from zero! Delta:%f", delta), 0, vc.dot(va), EPSILON);

        Vector3d ca = Vector3d.xyz(1, 2, 3);
        Vector3d cb = Vector3d.xyz(3, 4, 5);

        delta = Math.abs(ca.dot(cb) - 26.0);
        Assert.assertEquals(String.format("scalar product of vectors diverges from generated value! Delta:%f", delta), 26.0, ca.dot(cb), EPSILON);

    }

    @Test
    public void crossedTest() {

        Transform randRot = RandomRotation();

        double a = random.nextDouble();
        double b = random.nextDouble();
        double c = random.nextDouble();

        Vector3d va = randRot.transform(Vector3d.xyz(a, 0, 0));
        Vector3d vb = randRot.transform(Vector3d.xyz(0, b, 0));
        Vector3d vc = randRot.transform(Vector3d.xyz(0, 0, c));

        Vector3d w = va.crossed(vb);
        double delta = Math.abs(a * b - w.magnitude());

        Assert.assertEquals(String.format("result vector magnitude not equal to magnitude-product of orthogonal precursor vectors. Delta: %f", delta), a * b, w.magnitude(), EPSILON);
        Assert.assertEquals(String.format("result vector not perpendicular to precursor vectors. Delta: %f", w.dot(va)), w.dot(va), 0, EPSILON);
        Assert.assertEquals(String.format("result vector not perpendicular to precursor vectors. Delta: %f", w.dot(vb)), w.dot(vb), 0, EPSILON);

        w = vb.crossed(vc);
        delta = Math.abs(b * c - w.magnitude());

        Assert.assertEquals(String.format("result vector magnitude not equal to magnitude-product of orthogonal precursor vectors. Delta: %f", delta), b * c, w.magnitude(), EPSILON);
        Assert.assertEquals(String.format("result vector not perpendicular to precursor vectors. Delta: %f", w.dot(vb)), w.dot(vb), 0, EPSILON);
        Assert.assertEquals(String.format("result vector not perpendicular to precursor vectors. Delta: %f", w.dot(vc)), w.dot(vc), 0, EPSILON);

        w = vc.crossed(va);
        delta = Math.abs(c * a - w.magnitude());

        Assert.assertEquals(String.format("result vector magnitude not equal to magnitude-product of orthogonal precursor vectors. Delta: %f", delta), c * a, w.magnitude(), EPSILON);
        Assert.assertEquals(String.format("result vector not perpendicular to precursor vectors. Delta: %f", w.dot(vc)), w.dot(vc), 0, EPSILON);
        Assert.assertEquals(String.format("result vector not perpendicular to precursor vectors. Delta: %f", w.dot(va)), w.dot(va), 0, EPSILON);

    }

    @Test
    public void magnitudeTest() {

        Transform randRot = RandomRotation();

        double a = 100.0 * random.nextDouble();
        double aSqr = a * a;
        Vector3d va = randRot.transform(Vector3d.X_ONE.times(a));

        double delta = Math.abs(a - va.magnitude());
        Assert.assertEquals(String.format("magnitude of vector diverges from generated value! Delta:%f", delta), a, va.magnitude(), EPSILON);

        delta = Math.abs(aSqr - va.magnitudeSq());
        Assert.assertEquals(String.format("squared magnitude of vector diverges from generated value! Delta:%f", delta), aSqr, va.magnitudeSq(), EPSILON);

    }

    @Test
    public void normalizedTest() {
        Transform randRot = RandomRotation();

        double a = 100.0 * random.nextDouble();
        Vector3d va = randRot.transform(Vector3d.X_ONE.times(a));
        Vector3d na = va.normalized();

        double delta = Math.abs(1.0 - na.magnitude());
        Assert.assertEquals(String.format("magnitude of normalized vector diverges from 1.0! Delta:%f", delta), 1.0, na.magnitude(), EPSILON);

        delta = Math.abs(a - na.dot(va));
        Assert.assertEquals(String.format("dotproduct of normalized vector diverges from generated value! Delta:%f", delta), a, na.dot(va), EPSILON);

    }

    @Test
    public void negatedTest() {
        Vector3d v = RandomVector().times(100.0);
        Vector3d n = v.negated();

        double delta = Math.abs(v.magnitude() - n.magnitude());

        Assert.assertEquals(String.format("calculated magnitudes diverges for negated vector! Delta: %f", delta), v.magnitude(), n.magnitude(), EPSILON);

        Vector3d z = v.plus(n);

        Assert.assertEquals(String.format("sum of vector and its negation not equal to zero! Delta: %f", z.magnitude()), z.magnitude(), 0, EPSILON);

    }

    @Test
    public void orthogonalTest() {
        Transform randRot = RandomRotation();

        double a = 100.0 * random.nextDouble();
        Vector3d va = randRot.transform(Vector3d.X_ONE.times(a));
        Vector3d oa = va.orthogonal();

        double delta = Math.abs(oa.dot(va));
        Assert.assertEquals(String.format("dotproduct of orthogonal vector diverges from zero! Delta:%f", delta), 0, oa.dot(va), EPSILON);
    }

    @Test
    public void angleTest() {
        double angle = 180.0 * random.nextDouble();
        Transform angleTrans = Transform.unity().rotZ(angle);
        Transform randRot = RandomRotation();
        double a = 100.0*random.nextDouble();
        double b = 100.0*random.nextDouble();

        Vector3d va = Vector3d.X_ONE.times(a);
        Vector3d vb = angleTrans.transform(Vector3d.X_ONE.times(b));

        va = randRot.transform(va);
        vb = randRot.transform(vb);

        double calcAngle = va.angle(vb);

        double delta = Math.abs(calcAngle - angle);

        Assert.assertEquals(String.format("calculated angle diverges from generated angle! Delta: %f", delta), angle, calcAngle, EPSILON);

    }

    @Test
    public void distanceTest() {
        Transform randTrans = RandomRotation();
        double distance = 100.0*random.nextDouble();
        Vector3d d = randTrans.transform(Vector3d.X_ONE.times(distance));
        Vector3d a = RandomVector().times(100.0);
        Vector3d b = a.plus(d);
        double calcDistance = a.distance(b);

        double delta = Math.abs(calcDistance - distance);


        Assert.assertEquals(String.format("calculated distance diverges from generated distance! Delta: %f", delta), distance, calcDistance, EPSILON);
    }

    @Test
    public void lerpTest() {

        double t = random.nextDouble();
        Vector3d va = RandomVector();
        Vector3d vb = RandomVector();

        Vector3d x = va.lerp(vb, t);
        Vector3d tx = va.times(1.0 - t).plus(vb.times(t));

        Assert.assertEquals(String.format("lerp vector diverges from alternatively generated vector! Delta: %f", x.distance(tx)), 0, x.distance(tx), EPSILON);
    }

    @Test
    public void projectTest() {

        Transform randRot = RandomRotation();

        double a = 100.0 * random.nextDouble();
        double b = 100.0 * random.nextDouble();

        Vector3d va = Vector3d.X_ONE.times(a);
        Vector3d vb = Vector3d.Y_ONE.times(b);
        Vector3d vc = va.plus(vb);
        va = va.normalized();

        va = randRot.transform(va);
        vb = randRot.transform(vb);
        vc = randRot.transform(vc);

        Vector3d p = va.project(vc);

        double delta = Math.abs(a - p.magnitude());
        Assert.assertEquals(String.format("projected vector magnitude diverges from generated value! Delta: %f", delta), a, p.magnitude(), EPSILON);
    }




    @Test
    public void collinearityTest() {
        {
            Vector3d p1 = Vector3d.xyz(1,1,1);
            Vector3d p2 = p1.times(5);
            Vector3d p3 = p1.times(10);

            Assert.assertTrue("p1, p2, p3 must be collinear", p1.collinear(p2, p3));
        }
        {
            Vector3d p1 = Vector3d.xyz(1,1,1);
            Vector3d p2 = p1.times(5,5,4);
            Vector3d p3 = p1.times(10);

            Assert.assertTrue("p1, p2, p3 must not be collinear", !p1.collinear(p2, p3));
        }
        {
            Vector3d p1 = Vector3d.xyz(10,10,10);
            Vector3d p2 = Vector3d.xyz(-1,-1,-1);
            Vector3d p3 = p1.times(5);

            Assert.assertTrue("p1, p2, p3 must be collinear", p1.collinear(p2, p3));
        }
        {
            Vector3d p1 = Vector3d.xyz(10,20,10);
            Vector3d p2 = p1.clone();
            Vector3d p3 = p2.clone();

            Assert.assertTrue("p1, p2, p3 must be collinear", p1.collinear(p2, p3));
        }
    }
*/

    val random: Random

    init {
            val seed = System.currentTimeMillis()
            println(">> Performing tests with seed $seed")
            random = Random(seed)
    }
}
