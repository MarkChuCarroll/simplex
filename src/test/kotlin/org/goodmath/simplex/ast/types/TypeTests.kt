package org.goodmath.simplex.ast.types

import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse

class TypeTests {
    @Test
    fun testTypeCreation() {
        val s1 = Type.simple("foo")
        val s2 = Type.simple("bar")
        val a1 = Type.array(Type.simple("zip"))
        val a2 = Type.array(Type.simple("foo"))
        val a3 = Type.array(s1)
        val a4 = Type.array(s2)

        assertTrue(s1.matchedBy(s1))
        assertTrue(s2.matchedBy(s2))
        assertTrue(a1.matchedBy(a1))
        assertTrue(a2.matchedBy(a2))
        assertTrue(a3.matchedBy(a3))
        assertTrue(a4.matchedBy(a4))

        assertFalse(s1.matchedBy(s2))
        assertFalse(s2.matchedBy(s1))
        assertFalse(s1.matchedBy(a1))
        assertFalse(a1.matchedBy(a2))
        assertFalse(a2.matchedBy(a1))
        assertTrue(a2.matchedBy(a3))
        assertFalse(a2.matchedBy(a4))
        assertTrue(a2.matchedBy(Type.array(Type.simple("foo"))))
        assertTrue(Type.array(Type.simple("foo")).matchedBy(a2))
        assertTrue(s1.matchedBy(a2.elementType))
    }

    @Test
    fun testMethodTypes() {
        val same1 =
            Type.simpleMethod(
                Type.simple("Test"),
                listOf(Type.IntType, Type.array(Type.FloatType)),
                Type.StringType,
            )

        val same2 =
            Type.simpleMethod(
                Type.simple("Test"),
                listOf(Type.simple("Int"), Type.array(Type.FloatType)),
                Type.simple("String"),
            )

        val diff1 =
            Type.simpleMethod(
                Type.StringType,
                listOf(Type.simple("Int"), Type.array(Type.FloatType)),
                Type.simple("String"),
            )
        val diff2 =
            Type.simpleMethod(
                Type.simple("Test"),
                listOf(Type.simple("Int"), Type.array(Type.IntType)),
                Type.simple("String"),
            )
        val diff3 =
            Type.simpleMethod(
                Type.simple("Test"),
                listOf(Type.simple("Int"), Type.array(Type.FloatType)),
                Type.FloatType,
            )
        assertTrue(same1.matchedBy(same2))
        assertFalse(same1.matchedBy(diff1))
        assertFalse(same1.matchedBy(diff2))
        assertFalse(same1.matchedBy(diff3))
    }
}
