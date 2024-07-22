/*
 * Copyright 2024 Mark C. Chu-Carroll
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.goodmath.simplex.runtime.values.csg

import java.util.ArrayList
import kotlin.collections.binarySearch
import kotlin.collections.filter
import kotlin.collections.last
import kotlin.collections.map
import kotlin.collections.maxOf
import kotlin.collections.plus
import kotlin.collections.reversed
import kotlin.collections.sorted
import kotlin.collections.toList
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.ranges.until

/*
* This code is derived from the profile package in chalumier,
* which in turn is largely a Kotlin Port of the profile code
* written by Paul Harrison for Demakein.
*/

fun List<Double>.bisect(target: Double): Int {
    val ind = this.binarySearch(target)
    return if (ind >= 0) {
        ind
    } else {
        (-ind) - 1
    }
}

/**
 * The solve function returns a three-tuple. Kotlin represents that
 * with a data class.
 */
data class Solution(val t1: Double, val t2: Double, val mirror: Boolean)

/**
 * Conceptually, you can think of an extrusion as a stack of
 * cross-sections, which are skinned. A profile describes
 * how a stack based on a uniform cross-section is built. It
 * defines a sequence of segments, where each segment has a
 * vertical distance from the previous segment,
 * and the desired "diameter" of the cross-section at that
 * elevation.
  */
data class ProfileSlice(val z: Double, val lowDiam: Double, val highDiam: Double)


data class Profile(val slices: List<ProfileSlice>) {

    /**
     * Compute the diameter of the profile at a location.
     */
    operator fun invoke(location: Double, useHigh: Boolean = false): Double {
        if (location < slices[0].z) {
            return slices[0].lowDiam
        } else if (location > slices.last().z) {
            return slices.last().highDiam
        }
        val i = slices.map { it.z }.bisect(location)
        if (slices[i].z == location) {
            return if (useHigh) {
                slices[i].highDiam
            } else {
                slices[i].lowDiam
            }
        }
        val t = (location - slices[i - 1].z) / (slices[i].z - slices[i - 1].z)
        return (1.0 - t) * slices[i - 1].highDiam + t * slices[i].lowDiam
    }

    fun maximum(): Double = max(
        slices.maxOf { it.lowDiam },
        slices.maxOf { it.highDiam})

    /**
     * Combine two profiles.
     *
     * ph: Fairly dumb way to combine profiles. Won't work perfectly for min, max.
     */
    private fun morph(other: Profile, op: (Double, Double) -> Double): Profile {
        val combinedZs = (slices.map { it.z } + other.slices.map { it.z }).sorted()
        val combinedWithLows = combinedZs.map { p -> Pair(p, op(this(p, false), other(p, false))) }
        val combinedWithHighs = combinedWithLows.map { (z, low) ->
            ProfileSlice(z, low, op(this(z, false), other(z, false)))
        }
        return Profile(combinedWithHighs)
    }

    private fun morph(other: Double, op: (Double, Double) -> Double): Profile {
        val otherProfile = Profile(
            listOf(ProfileSlice(0.0, other, other))
        )
        return morph(otherProfile, op)
    }


    fun maxWith(other: Profile): Profile = morph(other) { a, b -> max(a, b) }

    operator fun plus(other: Profile): Profile = morph(other) { a, b -> a + b }

    operator fun plus(other: Double): Profile = morph(other) { a, b -> a + b }


    operator fun minus(other: Profile): Profile = morph(other) { a, b -> a - b }

    operator fun minus(other: Double): Profile = morph(other) { a, b -> a - b }

    /**
     * Clip or extend a profile
     */
    fun clipped(start: Double, end: Double): Profile {
        val newSlices = ArrayList(listOf(
            ProfileSlice(
                start, this(start, true),
                this(start, true)
            )
        ) +
                slices.filter { slice ->
                    slice.z > start && slice.z < end
                }.toList()
        )
        newSlices.add(ProfileSlice(end, this(end, false), this(end, false)))
        return Profile(newSlices)
    }

    fun reversed(): Profile {
        return Profile(slices.reversed())
    }

    fun moved(offset: Double): Profile {
        return Profile(slices.map { (z, low, high) ->
            ProfileSlice(z + offset, low, high)
        })
    }


    fun appendedWith(other: Profile): Profile {

        val positionedOther = other.moved(slices.last().z)
        return Profile(
            slices + positionedOther.slices
        )
    }

    fun asStepped(maxStep: Double): Profile {
        // To create a smooth body, we'll replace the hard changes in
        // diameters with a sequence of smaller stepped ones.
        // To do that, we'll first work out the list of positions where
        // diameter changes will happen, and then we'll go through and
        // assign diameter values to them.
        val newZs = ArrayList<Double>()
        for (i in 0 until slices.size - 1) {
            // For a transition from segment i to segment i+1, we want
            // to create a smooth series of steps from the diameter at the
            // top of segment i to the diameter of the bottom of segment i+1.
            val lowerSegmentTopPosition = slices[i].z
            val lowerSegmentTopDiameter = slices[i].highDiam
            val higherSegmentBottomPosition = slices[i + 1].z
            val higherSegmentBottomDiameter = slices[i + 1].lowDiam
            val numberOfSteps = ((higherSegmentBottomDiameter - lowerSegmentTopDiameter).absoluteValue / maxStep).toInt() + 1
            if (numberOfSteps == 0) {
                continue
            }
            newZs.addAll((1 until numberOfSteps).map { stepNumber ->
                (higherSegmentBottomPosition - lowerSegmentTopPosition) * stepNumber.toDouble() / numberOfSteps + lowerSegmentTopPosition})
        }
        // Tack on the position of the very top of the instrument.
        newZs.add(slices.last().z)

        // Compute the new diameters.
        val newDiams = (0 until newZs.size- 1 ).map { i -> this(0.5*(newZs[i] + newZs[i+1])) }

        val newSlices = (0 until newZs.size - 1).map { i ->
            ProfileSlice(newZs[i], newDiams[max(i - 1, 0)], newDiams[min(i + 1, newZs.size - 1)])
        }
        return Profile(newSlices)
    }

    companion object {

        /**
         * Solve. As usual, ph didn't write down what he was solving.
         * It's some segment of the cornu/clothoid curve as a transition
         * between profile segments, but beyond that? No clue.
         */
        private fun solve(a1: Double, a2: Double): Solution {
            fun score(t1: Double, t2: Double, mirror: Boolean): Double {
                if (abs(t1 - t2) < 1e-6 || max(abs(t1), abs(t2)) > PI * 10.0) {
                    return 1e30
                }
                val (y1, x1) = Cornu.cornuYx(t1, mirror)
                val (y2, x2) = Cornu.cornuYx(t2, mirror)
                val chordA = atan2(y2 - y1, x2 - x1)
                var thisA1 = abs(t1)  // ph: t1*t1
                var thisA2 = abs(t2) // ph: #t2*t2
                if (mirror) {
                    thisA1 = -thisA1
                    thisA2 = -thisA2
                }
                if (t1 > t2) {
                    thisA1 += PI
                    thisA2 += PI
                }
                val ea1 = (thisA1 - chordA - a1 + PI) % (2 * PI) - PI
                val ea2 = (thisA2 - chordA - a2 + PI) % (2 * PI) - PI
                return ea1 * ea1 + ea2 * ea2
            }

            var s: Double? = null
            val n = 2
            var t1 = 0.0
            var t2 = 0.0
            var mirror = false
            for (newMirror in listOf(false, true)) {
                for (i in -n..n) {
                    for (j in -n..n) {
                        val newT1 = i * PI / n
                        val newT2 = j * PI / n
                        val newS = score(newT1, newT2, newMirror)
                        if (s == null || newS < s) {
                            t1 = newT1
                            t2 = newT2
                            mirror = newMirror
                            s = newS
                        }
                    }
                }
            }
            var step: Double = PI * n * 0.5
            while (step >= 1e-4) {
                for ((newT1, newT2) in listOf(
                    Pair(t1 + step, t2 + step),
                    Pair(t1 - step, t2 - step),
                    Pair(t1 - step, t2 + step),
                    Pair(t1 + step, t2 - step)
                )) {
                    val newS = score(newT1, newT2, mirror)
                    val t = s!!
                    if (newS < t) {
                        s = newS
                        t1 = newT1
                        t2 = newT2
                        break
                    } else {
                        step *= 0.5
                    }
                }
            }
            return Solution(t1, t2, mirror)
        }

        fun makeProfile(spec: List<List<Double>>): Profile {
            val slices = spec.map { item ->
                if (item.size == 2) {
                    ProfileSlice(item[0], item[1], item[1])
                } else {
                    ProfileSlice(item[0], item[1], item[2])
                }
            }
            return Profile(slices)
        }
    }
}


