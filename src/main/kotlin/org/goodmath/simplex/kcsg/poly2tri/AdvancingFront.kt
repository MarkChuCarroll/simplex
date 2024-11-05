package org.goodmath.simplex.kcsg.poly2tri


/*
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

/**
 * @author Thomas ??? (thahlen@gmail.com)
 */
 class AdvancingFront(var head: AdvancingFrontNode,
     var tail: AdvancingFrontNode) {
     var search: AdvancingFrontNode = head

    init {
        addNode(head)
        addNode(tail)
    }

    fun addNode(node: AdvancingFrontNode) {
//        _searchTree.put( node.key, node );
    }

    fun removeNode(node: AdvancingFrontNode) {
//        _searchTree.delete( node.key );
    }

    override fun toString(): String {
        val sb = StringBuilder()
        var node = head
        while (node != tail) {
            sb.append(node.point.x).append("->")
            node = node.next
        }
        sb.append(tail.point.x)
        return sb.toString()
    }

    fun findSearchNode(x: Double): AdvancingFrontNode {
        return search
    }

    /**
     * We use a balancing tree to locate a node smaller or equal to given key
     * value
     *
     */
    fun locateNode(point: TriangulationPoint): AdvancingFrontNode? {
        return locateNode(point.x)
    }

    fun locateNode(x: Double): AdvancingFrontNode? {
        var node = findSearchNode(x)
        if (x < node.value) {
            while (node.hasPrevious()) {
                node = node.prev
                if (x >= node.value) {
                    search = node
                    return node
                }
            }
        } else {
            while (node.hasNext()) {
                node = node.next
                if (x < node.value) {
                    search = node.prev
                    return node.prev
                }
            }
        }
        return null
    }

    /**
     * This implementation will use simple node traversal algorithm to find a
     * point on the front
     *
     * @param point
     * @return
     */
    fun locatePoint(point: TriangulationPoint): AdvancingFrontNode? {
        val px = point.x
        var node = findSearchNode(px)
        var nx = node.point.x

        if (px == nx) {
            if (point != node.point) {
                // We might have two nodes with same x value for a short time
                if (point == node.prev.point) {
                    node = node.prev
                } else if (point == node.next.point) {
                    node = node.next
                } else {
                    throw RuntimeException("Failed to find Node for given afront point");
                }
            }
        } else if (px < nx) {
            while (node.hasPrevious()) {
                node = node.prev
                if (point == node.point) {
                    break
                }
            }
        } else {
            while (node.hasNext()) {
                node = node.next
                if (point == node.point) {
                    break
                }
            }
        }
        search = node
        return node
    }
}
