package org.goodmath.simplex.vvecmath


/**
 * A modifiable 3d vector that is stored in an external double array.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
interface ModifiableStoredVector3d: StoredVector3d, ModifiableVector3d {


    companion object {
        /**
         * Creates a new stored vector from the specified double array.
         *
         * @param storage double array used to store the vector
         * @param offset the storage offset used by the vector
         * @param stride the stride used to store the vector elements (x,y,z)
         * @return a new stored vector from the specified double array
         */
         fun from(storage: Array<Double>, offset: Int, stride: Int = 1): ModifiableStoredVector3d {
            return StoredVector3dImpl(storage, offset, stride)
        }

        /**
         * Creates a new modifiable stored vector from the specified double array.
         *
         * @param storage double array used to store the vector
         * @param v the vector whose storage offset and stride shall be used
         * @return a new stored vector from the specified double array
         */
        fun from(storage: Array<Double>, v: StoredVector3d): ModifiableStoredVector3d {
            return from(storage, v.offset, v.stride)
        }
    }

}
