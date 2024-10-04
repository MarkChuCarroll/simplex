package org.goodmath.simplex.ast.types

import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.def.DataDefinition
import org.goodmath.simplex.runtime.Env
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach

class TypeTests {

    @BeforeEach
    fun initRootEnvironment() {
        Env.createRootEnv()
        val t = Type
    }

    @Test
    fun testTypeCreation() {
        val s1 = Type.simple("foo")
        val s2 = Type.simple("bar")
        val zip = DataDefinition("zip",
            listOf(), Location("none", 0, 0))
        val foo = DataDefinition("foo",
            listOf(), Location("none", 0, 0))
        Type.registerValueType(s1, foo.valueType)
        Type.registerValueType(s2, zip.valueType)
        val a1 = Type.vector(Type.simple("zip"))
        val a2 = Type.vector(Type.simple("foo"))
        val a3 = Type.vector(s1)
        val a4 = Type.vector(s2)

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
        assertTrue(a2.matchedBy(Type.vector(Type.simple("foo"))))
        assertTrue(Type.vector(Type.simple("foo")).matchedBy(a2))
        assertTrue(s1.matchedBy(a2.elementType))
    }

    @Test
    fun testMethodTypes() {
        val same1 =
            Type.simpleMethod(
                Type.simple("Test"),
                listOf(Type.IntType, Type.vector(Type.FloatType)),
                Type.StringType,
            )

        val same2 =
            Type.simpleMethod(
                Type.simple("Test"),
                listOf(Type.simple("Int"), Type.vector(Type.FloatType)),
                Type.simple("String"),
            )

        val diff1 =
            Type.simpleMethod(
                Type.StringType,
                listOf(Type.simple("Int"), Type.vector(Type.FloatType)),
                Type.simple("String"),
            )
        val diff2 =
            Type.simpleMethod(
                Type.simple("Test"),
                listOf(Type.simple("Int"), Type.vector(Type.IntType)),
                Type.simple("String"),
            )
        val diff3 =
            Type.simpleMethod(
                Type.simple("Test"),
                listOf(Type.simple("Int"), Type.vector(Type.FloatType)),
                Type.FloatType,
            )
        assertTrue(same1.matchedBy(same2))
        assertFalse(same1.matchedBy(diff1))
        assertFalse(same1.matchedBy(diff2))
        assertFalse(same1.matchedBy(diff3))
    }
}
