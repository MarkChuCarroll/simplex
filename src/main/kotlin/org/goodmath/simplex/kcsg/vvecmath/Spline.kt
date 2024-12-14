package org.goodmath.simplex.kcsg.vvecmath


/**
 * Spline base class for 1, 2 and 3-dimensional spline curves.
 *
 * @author Michael Hoffer (info@michaelhoffer.de)
 */
open class Spline {

    /**
     * This is a highly improved version of the following code: based on
     * http://www.java-gaming.org/index.php?topic=9830.20
     *
     * Does not rely on reflection for switching vector components based on
     * dimension
     *
     * @param valueCollection
     * @param coordIndex
     * @param cubicCollection
     */
    fun calcNaturalCubic(valueCollection: List<Vector3d>,
                         coordIndex: Int,
                         cubicCollection: MutableCollection<Cubic>) {

        val num = valueCollection.size - 1

        val gamma = Array<Double>(num + 1) { 0.0 }
        val delta = Array<Double>(num + 1) { 0.0 }
        val d = Array<Double>(num + 1) { 0.0 }

        /*
           We solve the equation
          [2 1       ] [D[0]]   [3(x[1] - x[0])  ]
          |1 4 1     | |D[1]|   |3(x[2] - x[0])  |
          |  1 4 1   | | .  | = |      .         |
          |    ..... | | .  |   |      .         |
          |     1 4 1| | .  |   |3(x[n] - x[n-2])|
          [       1 2] [D[n]]   [3(x[n] - x[n-1])]

          by using row operations to convert the matrix to upper triangular
          and then back substitution.  The D[i] are the derivatives at the knots.
         */
        gamma[0] = 1.0 / 2.0
        for (i in 1 until num) {
            gamma[i] = 1.0f / (4.0f - gamma[i - 1])
        }
        gamma[num] = 1.0f / (2.0f - gamma[num - 1])

        var p0 = getByIndex(valueCollection[0], coordIndex)
        var p1 = getByIndex(valueCollection[1], coordIndex)

        delta[0] = 3.0f * (p1 - p0) * gamma[0]
        for (i in 1 until num) {
            p0 = getByIndex(valueCollection[i - 1], coordIndex)
            p1 = getByIndex(valueCollection[i + 1], coordIndex)
            delta[i] = (3.0f * (p1 - p0) - delta[i - 1]) * gamma[i]
        }
        p0 = getByIndex(valueCollection[num - 1], coordIndex)
        p1 = getByIndex(valueCollection[num], coordIndex)

        delta[num] = (3.0f * (p1 - p0) - delta[num - 1]) * gamma[num]

        d[num] = delta[num]
        for (i in num - 1 downTo 0) {
            d[i] = delta[i] - gamma[i] * d[i + 1]
        }

        /*
         * now compute the coefficients of the cubics
         */
        cubicCollection.clear()

        for (i in 0 until num) {
            p0 = getByIndex(valueCollection[i], coordIndex)
            p1 = getByIndex(valueCollection[i + 1], coordIndex)

            cubicCollection.add(Cubic(p0, d[i],
                3 * (p1 - p0) - 2 * d[i] - d[i + 1],
                2 * (p0 - p1) + d[i] + d[i + 1]))
        }
    }

    fun getByIndex(v: Vector3d, index: Int): Double {
        return when (index) {
            0 -> v.x
            1 -> v.y
            2 -> v.z
            else -> throw InvalidParameterException("Illegal index specified: $index")
        }
    }

    class Cubic(
        val a: Double,
        val b: Double,
        val c: Double,
        val d: Double) {

        fun eval(u: Double): Double {
            return (((d * u) + c) * u + b) * u + a
        }
    }
}
