package org.goodmath.simplex.runtime.values.primitives

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.expect

class FloatValueTypeTest {
    @Test
    fun testFloatOperations() {
        val v1 = FloatValue(3.14159)
        val v2 = FloatValue(2.718281828)
        val v3 = FloatValue(sqrt(2.0))
        val a = FloatValueType.add(v1, v2)
        assertEquals(FloatValueType, a.valueType)
        a as FloatValue
        assertEquals(5.859871827999999, a.d, 0.0001)
        val b = FloatValueType.subtract(a, v3)
        b as FloatValue
        assertEquals(4.445658265626904, b.d, 0.0001)
        val c = FloatValueType.pow(b, FloatValue(0.5))
        c as FloatValue
        assertEquals(2.1084729700963454, c.d, 0.0001)
        val d = FloatValueType.mult(FloatValue(2.1), c)
        d as FloatValue
        assertEquals(4.427793237202326, d.d)
        val e = FloatValueType.div(d, FloatValue(sqrt(3.0)))
        e as FloatValue
        assertEquals(2.5563876174147673, e.d)

        assertFalse(FloatValueType.equals(e, d))
        assert(FloatValueType.equals(e, e))

        assertEquals(-1, FloatValueType.compare(c, d))
        assertEquals(1, FloatValueType.compare(d, c))
        assertEquals(0, FloatValueType.compare(c, c))

        assertTrue(FloatValueType.isTruthy(c))
        assertFalse(FloatValueType.isTruthy(FloatValueType.subtract(c, c)))
    }
}
