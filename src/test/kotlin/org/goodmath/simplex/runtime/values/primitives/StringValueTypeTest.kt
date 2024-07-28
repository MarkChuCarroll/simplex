package org.goodmath.simplex.runtime.values.primitives
import org.goodmath.simplex.runtime.SimplexError
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.PI
import kotlin.test.assertEquals

class StringValueTypeTest {

    @Test
    fun testOperations() {
        val s1 = StringValue("string one")
        val s2 = StringValue("STRING TWO")
        val s3 = StringValue(PI.toString())

        val a = StringValueType.add(s1, s2)
        a as StringValue
        assertEquals("string oneSTRING TWO",
            a.s)
        assertThrows<SimplexError> { StringValueType.subtract(s3, s2) }

        assertTrue(StringValueType.compare(s1, s2) > 0)
        assertTrue(StringValueType.compare(s2, s1) < 0)
        assertTrue(StringValueType.compare(s2, s2) == 0)
        assertEquals(IntegerValue(10), StringValueType.getPrimitiveMethod("length").execute(s1, emptyList()))
    }
}
