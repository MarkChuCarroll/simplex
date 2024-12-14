package org.goodmath.simplex.kcsg.poly2tri

import org.stringtemplate.v4.compiler.STParser
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

/**
 * TriangulationContext.java
 *
 * Copyright 2014-2014 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
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

/* Poly2Tri
 * Copyright (c) 2009-2010, Poly2Tri Contributors
 * http://code.google.com/p/poly2tri/
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Poly2Tri nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

enum class TriangulationAlgorithm {
    DTSweep
}


abstract class TriangulationDebugContext(
    val tcx: TriangulationContext<*>) {

    abstract fun clear()
}


abstract class TriangulationContext<A: TriangulationDebugContext> {
    val debug: A? = null
    var debugEnabled = false

    val triList: ArrayList<DelaunayTriangle> = ArrayList()

    val points: ArrayList<TriangulationPoint> = ArrayList<TriangulationPoint>(200)
    var triangulationMode = TriangulationMode.UNCONSTRAINED

    var triUnit: Triangulatable? = null

    var terminated = false
    var waitUntilNotified = false

    var stepTime = -1L
    var stepCount = 0L

    fun done() {
        stepCount++
    }

    abstract val algorithm: TriangulationAlgorithm

    open fun prepareTriangulation(t: Triangulatable) {
        triUnit = t
        triangulationMode = t.getTriangulationMode()
        t.prepareTriangulation(this)
    }

    abstract fun newConstraint(a: TriangulationPoint, b: TriangulationPoint): TriangulationConstraint

    fun addToList(triangle: DelaunayTriangle) {
        triList.add(triangle)
    }

    fun getTriangles(): List<DelaunayTriangle> {
        return triList
    }

    fun getTriangulatable(): Triangulatable {
        return triUnit!!
    }

    fun getPoints(): List<TriangulationPoint> {
        return points
    }

    val lock = ReentrantLock()
    val cond: Condition = lock.newCondition()

    fun update(msg: String?) {
        try {
            lock.lock()
            if (debugEnabled) {
                try {
                    stepCount++
                    if (stepTime > 0) {
                        cond.await(stepTime, TimeUnit.MILLISECONDS)
                        /** Can we resume execution or are we expected to wait? */
                        if (waitUntilNotified) {
                            cond.await()
                        }
                    } else {
                        cond.await()
                    }
                    // We have been notified
                    waitUntilNotified = false
                } catch (e: InterruptedException) {
                    update("Triangulation was interrupted")
                }
            }
            if (terminated) {
                throw RuntimeException("Triangulation process terminated before completion")
            }
        } finally {
            lock.unlock()
        }
    }

    open fun clear() {
        points.clear()
        terminated = false
        debug?.clear()
        stepCount = 0
    }

    fun waitUntilNotified(b: Boolean) {
        lock.lock()
        try {
            waitUntilNotified = b
        } finally {
            lock.unlock()
        }
    }

    fun terminateTriangulation() {
        terminated=true
    }

    fun getDebugContext(): A? {
        return debug
    }

    fun addPoints(points: List<TriangulationPoint>) {
        this.points.addAll(points)
    }

    fun signal() {
        cond.signalAll()
    }
}

