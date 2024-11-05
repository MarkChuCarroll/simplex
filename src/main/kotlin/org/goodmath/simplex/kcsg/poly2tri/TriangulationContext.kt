package org.goodmath.simplex.kcsg.poly2tri

import org.goodmath.simplex.ast.expr.Condition
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantLock

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


abstract class TriangulationDebugContext {
    abstract val tcx: TriangulationContext<*>
    abstract fun clear()
}

abstract class TriangulationContext<A: TriangulationDebugContext> {
    abstract val debug: A?
    var debugEnabled: Boolean = false

    var triangulationMode: TriangulationMode by LateInitDelegate("triangulationMode")


    val triList = ArrayList<DelaunayTriangle>()

    val points = ArrayList<TriangulationPoint>(200)
    abstract var triUnit: Triangulatable<*>

    var terminated: Boolean = false
    var waitUntilNotified: Boolean = false

    var stepTime: Int = -1
    var stepCount: Int = 0

    fun done() {
        stepCount++
    }

    open fun prepareTriangulation(t: Triangulatable<*>) {
        triUnit = t
        triangulationMode = t.triangulationMode
        t.prepareTriangulation(this)
    }

    abstract fun  newConstraint(a: TriangulationPoint, b: TriangulationPoint): TriangulationConstraint

    open fun addToList(triangle: DelaunayTriangle) {
        triList.add(triangle)
    }

    open fun getTriangles(): List<DelaunayTriangle> {
        return triList
    }

    open fun getTriangulatable(): Triangulatable<*> {
        return triUnit
    }


    open fun update(message: String) {
        synchronized(this) {
            if (debugEnabled) {
                try {
                    stepCount++
                    if (stepTime > 0) {
                        (this as Object).wait(stepTime.toLong())
                        /** Can we resume execution or are we expected to wait? */
                        if (waitUntilNotified) {
                            wait()
                        }
                    } else {
                        (this as Object).wait()
                    }
                    // We have been notified
                    waitUntilNotified = false
                } catch (e: InterruptedException) {
                    update("Triangulation was interrupted")
                }
                if (terminated) {
                    throw RuntimeException("Triangulation process terminated before completion")
                }
            }
        }
    }

    open fun clear() {
        points.clear()
        terminated = false
        debug?.clear()
        stepCount = 0
    }

    open fun waitUntilNotified(b: Boolean) {
        synchronized(this) {
            waitUntilNotified = b
        }
    }

    open fun terminateTriangulation() {
        terminated = true
    }

    open fun getDebugContext(): A? {
        return debug
    }

    open fun addPoints(points: List<TriangulationPoint>) {
        this.points.addAll(points)
    }
}
