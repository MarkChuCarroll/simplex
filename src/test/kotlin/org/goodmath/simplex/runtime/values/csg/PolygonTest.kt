package org.goodmath.simplex.runtime.values.csg

import javafx.geometry.Point2D
import org.goodmath.simplex.runtime.csg.TwoDPoint
import org.goodmath.simplex.runtime.csg.TwoDPointValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PolygonTest {
    @Test
    fun testPolygonCreators() {
        val rect = PolygonValueType.getFunction("rectangle")
        val r = rect.execute(listOf(TwoDPoint(20.0, 20.0),
            FloatValue(10.0), FloatValue(16.0)))
        r as Polygon
        assertEquals("""obj(PolygonValue)
  array[points]:
    obj(Point2D)
      attr(x)='15.0'
      attr(y)='12.0'
    obj(Point2D)
      attr(x)='25.0'
      attr(y)='12.0'
    obj(Point2D)
      attr(x)='25.0'
      attr(y)='28.0'
    obj(Point2D)
      attr(x)='15.0'
      attr(y)='28.0'
""", r.twist().toString())
    }

}
