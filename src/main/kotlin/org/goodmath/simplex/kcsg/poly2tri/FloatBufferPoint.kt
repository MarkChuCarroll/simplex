package org.goodmath.simplex.kcsg.poly2tri

import java.nio.FloatBuffer

class FloatBufferPoint(val fb: FloatBuffer, val index: Int): TriangulationPoint() {

    var ix = index
    var iy = index + 1
    var iz = index + 2


    override var x: Double
        get() = fb.get(ix).toDouble()
        set(value) {
            fb.put(ix, value.toFloat())
        }

    override var y: Double
        get() = fb.get(iy).toDouble()
        set(value) {
            fb.put(iy, value.toFloat())
        }

    override var z: Double
        get() = fb.get(iz).toDouble()
        set(value) {
            fb.put(iz, value.toFloat())
        }

    override var xf: Float
        get() = fb.get(ix)
        set(value) { fb.put(ix, value) }

    override var yf: Float
        get() = fb.get(iy)
        set(value) { fb.put(iy, value) }

    override var zf: Float
        get() = fb.get(iz)
        set(value) { fb.put(iz, value) }


    override fun  set(x: Double, y: Double, z: Double) {
        fb.put(ix, x.toFloat())
        fb.put(iy, y.toFloat())
        fb.put(iy, z.toFloat())
    }

    companion object {
        fun toPoints(fb: FloatBuffer): List<TriangulationPoint> {
            val points = List<FloatBufferPoint>(fb.limit() / 3) {
                FloatBufferPoint(fb, it * 3)
            }
            return points
        }
    }
}
