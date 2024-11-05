package org.goodmath.simplex.kcsg.poly2tri

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
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.List;




/**
 *
 * @author Thomas ???, thahlen@gmail.com
 *
 */

enum class TriangulationProcessEvent {
    Started, Waiting, Failed, Aborted, Done
}

interface TriangulationProcessListener {
    fun triangulationEvent(e: TriangulationProcessEvent, u: Triangulatable<*>)
}


class TriangulationProcess(): Runnable {
    val tcx = Poly2Tri.createContext()

    private var thread: Thread? = null
    private var isTerminated = false
    private var pointCount = 0
    private var timestamp: Long = 0
    private var triangulationTime: Long = 0
    private var awaitingTermination = false
    private var restart = false

    private val triangulations = ArrayList<Triangulatable<*>>()

    private var listeners = ArrayList<TriangulationProcessListener>()

    fun addListener(listener: TriangulationProcessListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: TriangulationProcessListener) {
        listeners.remove(listener)
    }

    fun clearListeners() {
        listeners.clear()
    }

    /**
     * Notify all listeners of this new event
     * @param event
     */
    fun sendEvent(event: TriangulationProcessEvent) {
        for(l in listeners) {
            l.triangulationEvent(event, tcx.getTriangulatable())
        }
    }

    val stepCount: Int
        get() = tcx.stepCount

    /**
     * This retriangulates same set as previous triangulation
     * useful if you want to do consecutive triangulations with
     * same data. Like when you when you want to do performance
     * tests.
     */

    /**
     * Triangulate a PointSet with eventual constraints
     *
     */
    fun triangulate(ps: PointSet) {
        triangulations.clear()
        triangulations.add(ps)
        start()
    }

    /**
     * Triangulate a PointSet with eventual constraints
     *
     * @param cps
     */
    fun triangulate(cps: ConstrainedPointSet) {
        triangulations.clear()
        triangulations.add(cps)
        start()
    }

    /**
     * Triangulate a PolygonSet
     *
     * @param ps
     */
    fun triangulate(ps: Set<Polygon>) {
        triangulations.clear()
        triangulations.addAll(ps)
        start()
    }

    /**
     * Triangulate a Polygon
     *
     */
    fun triangulate(polygon: Polygon) {
        triangulations.clear()
        triangulations.add(polygon)
        start()
    }

    /**
     * Triangulate a List of Triangulatables
     *
     */
    fun triangulate(list: List<Triangulatable<*>>) {
        triangulations.clear()
        triangulations.addAll(list)
        start()
    }

    fun start() {
        if( thread == null || thread?.getState() == State.TERMINATED) {
            isTerminated = false
            thread = Thread( this, "Triangulation.${tcx.triangulationMode}")
            thread?.start()
            sendEvent(TriangulationProcessEvent.Started)
        } else {
            // Triangulation already running. Terminate it so we can start a new
            shutdown()
            restart = true
        }
    }

    fun isWaiting(): Boolean {
        return (thread != null && thread?.getState() == State.WAITING)
    }

    override fun run() {
        pointCount = 0
        try {
            var time = System.nanoTime()
            for(t in triangulations) {
                tcx.clear()
                tcx.prepareTriangulation(t)
                pointCount += tcx.points.size
                Poly2Tri.triangulate(tcx)
            }
            triangulationTime = (System.nanoTime() - time ) / 1000000
            sendEvent( TriangulationProcessEvent.Done );
        } catch (e: RuntimeException) {
            if(awaitingTermination) {
                awaitingTermination = false
                sendEvent( TriangulationProcessEvent.Aborted )
            } else {
                e.printStackTrace()
                sendEvent(TriangulationProcessEvent.Failed)
            }
        } catch(e: Exception) {
            e.printStackTrace();
            sendEvent( TriangulationProcessEvent.Failed );
        } finally {
            timestamp = System.currentTimeMillis()
            isTerminated = true
            thread = null
        }

        // Autostart a new triangulation?
        if(restart) {
            restart = false
            start()
        }
    }

    fun resume() {
        if (thread != null) {
            // Only force a resume when process is waiting for a notification
            if(thread?.getState() == State.WAITING ) {
                synchronized(tcx) {
                    (tcx as Object).notify()
                }
            } else if (thread?.getState() == State.TIMED_WAITING ) {
                tcx.waitUntilNotified( false )
            }
        }
    }

    fun shutdown() {
        awaitingTermination = true
        tcx.terminateTriangulation()
        resume()
    }

    fun isDone(): Boolean {
        return isTerminated
    }

    fun requestRead() {
        tcx.waitUntilNotified(true)
    }

    fun isReadable(): Boolean {
        if (thread == null) {
            return true
        } else {
            synchronized(thread!!) {
                if (thread?.getState() == State.WAITING) {
                    return true
                } else if (thread?.getState() == State.TIMED_WAITING) {
                    // Make sure that it stays readable
                    tcx.waitUntilNotified(true)
                    return true
                }
                return false
            }
        }
    }
}
