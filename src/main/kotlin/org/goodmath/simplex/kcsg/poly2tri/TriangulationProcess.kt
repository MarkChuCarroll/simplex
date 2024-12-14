package org.goodmath.simplex.kcsg.poly2tri

import org.slf4j.Logger
import org.slf4j.LoggerFactory

enum class TriangulationProcessEvent {
    Started,
    Waiting,
    Failed,
    Aborted,
    Done
}

interface TriangulationProcessListener {
    fun triangulationEvent(e: TriangulationProcessEvent, unit: Triangulatable)
}


/**
 *
 * @author Thomas ???, thahlen@gmail.com
 *
 */
class TriangulationProcess: Runnable {
    val algorithm = TriangulationAlgorithm.DTSweep
    var tcx: TriangulationContext<*> = Poly2Tri.createContext(algorithm)
    var thread: Thread? = null
    var isTerminated = false
    var pointCount = 0
    var timestamp = 0L
    var triangulationTime = 0.0
    var awaitingTermination = false
    var restart = false

    val triangulations = ArrayList<Triangulatable>()

    var listeners = ArrayList<TriangulationProcessListener>()

    fun  addListener(listener: TriangulationProcessListener) {
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
        for (l in listeners) {
            l.triangulationEvent(event, tcx.getTriangulatable())
        }
    }



    /**
     * Triangulate a PointSet with eventual constraints
     *
     * @param ps
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
    fun triangulate(ps: PolygonSet) {
        triangulations.clear()
        triangulations.addAll(ps.polygons)
        start()
    }

    /**
     * Triangulate a Polygon
     *
     * @param polygon
     */
    fun triangulate(polygon: Polygon) {
        triangulations.clear()
        triangulations.add(polygon)
        start()
    }

    /**
     * Triangulate a List of Triangulates
     *
     * @param list
     */
    fun triangulate(list: List<Triangulatable>) {
        triangulations.clear()
        triangulations.addAll(list)
        start()
    }

    fun start() {
        if(thread == null || thread!!.state == Thread.State.TERMINATED) {
            isTerminated = false
            thread = Thread( this, algorithm.name + "." + tcx.triangulationMode + "-" + timestamp )
            thread!!.start()
            sendEvent(TriangulationProcessEvent.Started)
        } else {
            // Triangulation already running. Terminate it so we can start a new
            shutdown()
            restart = true
        }
    }

    fun isWaiting(): Boolean {
        return thread != null && thread!!.state == Thread.State.WAITING
    }

    override fun run() {
        pointCount = 0
        try {
            val time = System.nanoTime()
            for(t in triangulations) {
                tcx.clear()
                tcx.prepareTriangulation(t)
                pointCount += tcx.points.size
                Poly2Tri.triangulate(tcx)
            }
            triangulationTime = (System.nanoTime() - time) / 1e6
            logger.info( "Triangulation of {} points [{}ms]", pointCount, triangulationTime )
            sendEvent(TriangulationProcessEvent.Done)
        } catch(e: RuntimeException) {
            if (awaitingTermination) {
                awaitingTermination = false
                logger.info( "Thread[{}] : {}", thread!!.name, e.message)
                sendEvent(TriangulationProcessEvent.Aborted)
            } else {
                e.printStackTrace()
                sendEvent(TriangulationProcessEvent.Failed)
            }
        } catch(e: Exception) {
            e.printStackTrace()
            logger.info("Triangulation exception {}", e.message)
            sendEvent(TriangulationProcessEvent.Failed)
        } finally {
            timestamp = System.currentTimeMillis()
            isTerminated = true
            thread = null
        }

        // Autostart a new triangulation?
        if (restart) {
            restart = false
            start()
        }
    }

    fun resume() {
        if (thread != null) {
            // Only force a resume when process is waiting for a notification
            if(thread!!.state == Thread.State.WAITING) {
                synchronized(tcx) {
                    tcx.signal()
                }
            } else if (thread!!.state == Thread.State.TIMED_WAITING) {
                tcx.waitUntilNotified(false)
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
                if (thread!!.state == Thread.State.WAITING) {
                    return true
                } else if (thread!!.state == Thread.State.TIMED_WAITING) {
                    // Make sure that it stays readable
                    tcx.waitUntilNotified(true)
                    return true
                }
                return false
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TriangulationProcess.javaClass)
    }
}
