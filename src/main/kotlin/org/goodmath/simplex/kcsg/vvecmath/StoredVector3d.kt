package org.goodmath.simplex.kcsg.vvecmath

/**
 * A 3d vector that is stored in an external double array. As of today Java does
 * not support value objects. This class is designed to be used to store vast
 * amounts of vectors in large double arrays without the overhead that typically
 * comes with large object arrays. Another use case is shared memory with native
 * code.
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
interface StoredVector3d: Vector3d {

    /**
     * Sets the array that is used to store this vector.
     *
     * @param storage storage array to set
     */
    var storage: Array<Double>

    var offset: Int

    var stride: Int

        companion object {

        /**
         * Returns the struct size of this vector (3).
         *
         * @return the struct size of this vector (3)
         */
        fun getStructSize(): Int {
            return 3
        }

        /**
         * Creates a new stored vector from the specified double array.
         *
         * @param storage double array used to store the vector
         * @param offset the storage offset used by the vector
         * @param stride the stride used to store the vector elements (x,y,z)
         * @return a new stored vector from the specified double array
         */
        fun from(storage: Array<Double>, offset: Int, stride: Int): StoredVector3d {
            val result = StoredVector3dImpl(storage, offset, stride)
            return result
        }

        /**
         * Creates a new stored vector from the specified double array.
         *
         * @param storage double array used to store the vector
         * @param offset the storage offset used by the vector
         * @return a new stored vector from the specified double array
         */
        fun from(storage: Array<Double>, offset: Int): StoredVector3d {
            return from(storage, offset, getStructSize())
        }

    }
}



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
        fun from(storage: Array<Double>, offset: Int, stride: Int): ModifiableStoredVector3d {
            return StoredVector3dImpl(storage, offset, stride)
        }


        /**
         * Creates a new modifiable stored vector from the specified double array.
         *
         * @param storage double array used to store the vector
         * @param offset the storage offset used by the vector
         * @return a new stored vector from the specified double array
         */
        fun from(storage: Array<Double>, offset: Int): ModifiableStoredVector3d {
            return from(storage, offset, StoredVector3d.getStructSize())
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


/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
class StoredVector3dImpl(override var storage: Array<Double>, override var offset: Int, override var stride: Int = 3):  ModifiableStoredVector3d {

    override fun set(vararg xyz: Double): Vector3d {
        for (i in 0 until xyz.size) {
            set(i, xyz[i])
        }
        return this
    }

    override fun set(i: Int, value: Double): StoredVector3d {
        this.storage[offset*stride+i]=value
        return this
    }

    override var x: Double
        get() = this.storage[offset*stride+0]
        set(v) {
            this.storage[offset*stride] = v
        }



    override var y: Double
        get() = this.storage[offset*stride+1]
        set(v) {
            this.storage[offset*stride+1] = v
        }


    override var z: Double
        get() = this.storage[offset*stride+2]
        set(v) {
            this.storage[offset*stride+2] = v
        }


    public override fun clone(): Vector3d {
        return StoredVector3dImpl(storage, offset, stride)
    }


}

