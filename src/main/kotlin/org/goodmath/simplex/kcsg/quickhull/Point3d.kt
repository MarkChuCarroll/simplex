package org.goodmath.simplex.kcsg.quickhull

/**
 * Copyright John E. Lloyd, 2004. All rights reserved. Permission to use,
 * copy, modify and redistribute is granted, provided that this copyright
 * notice is retained and the author is given credit whenever appropriate.
 *
 * This  software is distributed "as is", without any warranty, including
 * any implied warranty of merchantability or fitness for a particular
 * use. The author assumes no responsibility for, and shall not be liable
 * for, any special, indirect, or consequential damages, or any damages
 * whatsoever, arising out of or in connection with the use of this
 * software.
 */

/**
 * A three-element spatial point.
 *
 * The only difference between a point and a vector is in the
 * the way it is transformed by an affine transformation. Since
 * the transform method is not included in this reduced
 * implementation for QuickHull3D, the difference is
 * purely academic.
 *
 * @author John E. Lloyd, Fall 2004
 */
class Point3d(x: Double, y: Double, z: Double): Vector3d(x, y, z) {
    /**
     * Creates a Point3d and initializes it to zero.
     */
    constructor(): this(0.0, 0.0, 0.0)


    /**
     * Creates a Point3d by copying a vector
     *
     * @param v vector to be copied
     */
    constructor(v: Vector3d): this(v.x, v.y, v.z)

}
