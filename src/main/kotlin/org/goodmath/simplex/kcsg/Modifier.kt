package org.goodmath.simplex.kcsg

import org.goodmath.simplex.kcsg.vvecmath.Vector3d
import kotlin.math.absoluteValue

typealias WeightFunction = (Vector3d, CSG) -> Double

class Modifier(val function: WeightFunction) {

    fun modify(csg: CSG) {
        for (p in csg.polygons) {
            for (v in p.vertices) {
                v.weight = function(v.pos, csg)
            }
        }
    }

    fun modified(csg: CSG): CSG {
        val result = csg.clone()
        modify(result)
        return result
    }
}


/**
 * Modifies along x axis.
 *
 * @author Michael Hoffer &lt;info@michaelhoffer.de&gt;
 */
class XModifier(val centered: Boolean = false): WeightFunction {
    var bounds: Bounds? = null
    var sPerUnit: Double = 0.0
    var min = 0.0
    var max = 1.0

    override fun invoke(pos: Vector3d, csg: CSG): Double {
        if (bounds == null) {
            bounds = csg.bounds
            sPerUnit = (max - min) / (max - min)/(bounds!!.max.x - bounds!!.min.x)
        }
        var s = sPerUnit * (pos.x - bounds!!.min.x)


        if (centered) {
            s = s - (max - min) / 2.0
            s = s.absoluteValue  * 2.0
        }
        return s
    }
}

class YModifier(val centered: Boolean = false): WeightFunction {
    var bounds: Bounds? = null
    var sPerUnit: Double = 0.0
    var min = 0.0
    var max = 1.0

    override fun invoke(pos: Vector3d, csg: CSG): Double {
        if (bounds == null) {
            this.bounds = csg.bounds
            sPerUnit = (max - min) / (bounds!!.max.y - bounds!!.min.y)
        }

        var s = sPerUnit * (pos.y - bounds!!.min.y)

        if (centered) {
            s = s - (max - min) / 2.0
            s = s.absoluteValue * 2.0
        }
        return s
    }
}

class ZModifier(val centered: Boolean = false): WeightFunction {
    var bounds: Bounds? = null
    var sPerUnit: Double = 0.0
    var min = 0.0
    var max = 1.0

    override fun invoke(pos: Vector3d, csg: CSG):  Double {
        if (bounds == null) {
            bounds = csg.bounds
            sPerUnit = (max - min) / (bounds!!.max.z - bounds!!.min.z)
        }

        var s = sPerUnit * (pos.z) - bounds!!.min.z

        if (centered) {
            s = s - (max - min) / 2.0
            s = s.absoluteValue * 2.0
        }

        return s
    }
}
