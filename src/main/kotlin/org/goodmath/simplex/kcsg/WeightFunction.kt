package org.goodmath.simplex.kcsg

import org.goodmath.simplex.vvecmath.Vector3d
import kotlin.math.absoluteValue

/*
* Ported to Kotlin and adapted for simplex by Mark Chu-Carroll.
 */
interface WeightFunction {
    fun eval(v: Vector3d, csg: CSG): Double
}

object UnityModifier: WeightFunction {
    override fun eval(v: Vector3d, csg: CSG): Double {
        return 1.0
    }
}

class XModifier(val centered: Boolean): WeightFunction {
    var sPerUnit: Double = 0.0
    var min: Double = 0.0
    var max: Double = 0.0
    var bounds: Bounds? = null
    override fun eval(pos: Vector3d, csg: CSG): Double {
        if (bounds == null) {
            bounds = csg.bounds
            sPerUnit = (max - min) / (bounds!!.max.x - bounds!!.min.x)
        }
        var s = sPerUnit * (pos.x - bounds!!.min.x)
        if (centered) {
            s = (s - (max - min)/2.0).absoluteValue * 2.0
        }
        return s

    }
}


class YModifier(val centered: Boolean): WeightFunction {
    var sPerUnit: Double = 0.0
    var min: Double = 0.0
    var max: Double = 0.0
    var bounds: Bounds? = null
    override fun eval(pos: Vector3d, csg: CSG): Double {
        if (bounds == null) {
            bounds = csg.bounds
            sPerUnit = (max - min) / (bounds!!.max.y - bounds!!.min.y)
        }
        var s = sPerUnit * (pos.y - bounds!!.min.y)
        if (centered) {
            s = (s - (max - min)/2.0).absoluteValue * 2.0
        }
        return s

    }
}


class ZModifier(val centered: Boolean): WeightFunction {
    var sPerUnit: Double = 0.0
    var min: Double = 0.0
    var max: Double = 0.0
    var bounds: Bounds? = null

    override fun eval(pos: Vector3d, csg: CSG): Double {
        if (bounds == null) {
            bounds = csg.bounds
            sPerUnit = (max - min) / (bounds!!.max.z - bounds!!.min.z)
        }
        var s = sPerUnit * (pos.z - bounds!!.min.z)
        if (centered) {
            s = (s - (max - min)/2.0).absoluteValue * 2.0
        }
        return s
    }
}
