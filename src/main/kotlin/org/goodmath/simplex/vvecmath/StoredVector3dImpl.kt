package org.goodmath.simplex.vvecmath

/*
 * Copyright 2017-2019 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * If you use this software for scientific research then please cite the following publication(s):
 *
 * M. Hoffer, C. Poliwoda, & G. Wittum. (2013). Visual reflection library:
 * a framework for declarative GUI programming on the Java platform.
 * Computing and Visualization in Science, 2013, 16(4),
 * 181–192. http://doi.org/10.1007/s00791-014-0230-y
 *
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Michael Hoffer <info@michaelhoffer.de>.
 */

/**
 *
 * @author Michael Hoffer <info@michaelhoffer.de>
 */
class StoredVector3dImpl(override var storage: Array<Double>,
    override var offset: Int, override var stride: Int = 1): ModifiableStoredVector3d {

    public override fun clone(): Vector3d {
        return StoredVector3dImpl(storage, offset, stride)
    }


    override fun set(xyz: List<Double>): Vector3d {
        for (i in 0 until xyz.size) {
            set(i, xyz[i])
        }
        return this
    }

    override fun set(i: Int, value: Double): Vector3d {
        storage[offset+stride*i]=value
        return this
    }

    override var x: Double
        get() = storage[offset+stride*0]
        set(nx: Double) {
            storage[offset + stride * 0] = nx
        }

    override var y: Double
        get() = storage[offset+stride*1]
        set(ny: Double) {
            storage[offset + stride * 1] = ny
        }

    override var z: Double
        get() = storage[offset+stride*2]
        set(nz: Double) {
            storage[offset + stride * 2] = nz
        }

}
