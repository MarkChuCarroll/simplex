package org.goodmath.simplex.runtime.values.primitives

import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class IntValueTypeTest {
    @Test
    fun testOperations() {
        val v1 = IntegerValue(3)
        val v2 = IntegerValue(4)
        val v3 = IntegerValue(5)

        val sum = IntegerValueType.add(v1, v2)
        sum as IntegerValue
        val prod = IntegerValueType.mult(v2, v3)
        prod as IntegerValue
        val divi = IntegerValueType.div(v3, v1)
        divi as IntegerValue
        val modi = IntegerValueType.mod(prod, sum)
        modi as IntegerValue

        assertEquals(7, sum.i)
        assertEquals(20, prod.i)
        assertEquals(1, divi.i)
        assertEquals(6, modi.i)

        assertTrue(IntegerValueType.equals(divi, IntegerValue(1)))
        assertFalse(IntegerValueType.equals(prod, IntegerValue(6)))

        assertTrue(IntegerValueType.equals(IntegerValueType.neg(modi),
            IntegerValueType.subtract(IntegerValue(0), modi)))

        assertEquals(1, IntegerValueType.compare(IntegerValue(-1), IntegerValue(-2)))
        assertEquals(-1, IntegerValueType.compare(IntegerValue(-1), IntegerValue(2)))
        assertEquals(0, IntegerValueType.compare(IntegerValue(13), IntegerValue(13)))
        val p = IntegerValueType.pow(IntegerValue(3), IntegerValue(4))
        p as FloatValue
        assertEquals(81.0, p.d)
    }
}
