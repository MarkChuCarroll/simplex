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

import org.goodmath.simplex.runtime.SimplexUnsupportedOperation
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.PrimitiveMethod
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.ArrayValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.twist.Twist
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
import kotlin.math.absoluteValue
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
 * Conceptually, you can think of an extrusion as a stack of
 * cross-sections, which are skinned. A profile describes
 * how a stack based on a uniform cross-section is built. It
 * defines a sequence of segments, where each segment has a
 * vertical distance from the previous segment,
 * and the desired "diameter" of the cross-section at that
 * elevation.
 *
 * For simplex, each cross-section in the stack is defined
 * by a ProfileSlice.
 */
data class ProfileSlice(val z: Double, val lowDiam: Double, val highDiam: Double): Value {
    override val valueType: ValueType<ProfileSlice> = ProfileSliceType

    override fun twist(): Twist =
        Twist.obj("ProfileSlice",
            Twist.attr("z", z.toString()),
            Twist.attr("low", lowDiam.toString()),
            Twist.attr("high", highDiam.toString()))
}

object ProfileSliceType: ValueType<ProfileSlice>() {
    override val name: String = "ProfileSlice"

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override fun add(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "addition")
    }

    override fun subtract(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "subtraction")
    }

    override fun mult(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "multiplication")
    }

    override fun div(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "division")
    }

    override fun mod(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value
    ): Boolean {
        val s1 = assertIs(v1)
        val s2 = assertIs(v2)
        return s1 == s2
    }

    override fun neg(v1: Value): Value {
        throw SimplexUnsupportedOperation(name, "negation")
    }

    override fun compare(
        v1: Value,
        v2: Value
    ): Int {
        throw SimplexUnsupportedOperation(name, "ordering")
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object : PrimitiveFunctionValue(
                "slice",
                FunctionSignature(
                    listOf(
                        Param("pos", FloatValueType),
                        Param("lowDiam", FloatValueType),
                        Param("highDiam", FloatValueType)),
                    ProfileSliceType)
            ) {
                override fun execute(args: List<Value>): Value {
                    val z = assertIsFloat(args[0])
                    val lowDiam = assertIsFloat(args[1])
                    val highDiam = assertIsFloat(args[2])
                    return ProfileSlice(z, lowDiam, highDiam)
                }
            }
        )
    }
    override val providesOperations: List<PrimitiveMethod<ProfileSlice>> = emptyList()
}

data class ExtrusionProfile(val slices: List<ProfileSlice>): Value {

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
        slices.maxOf { it.highDiam })

    /**
     * Combine two profiles.
     *
     * ph: Fairly dumb way to combine profiles. Won't work perfectly for min, max.
     */
    private fun morph(other: ExtrusionProfile, op: (Double, Double) -> Double): ExtrusionProfile {
        val combinedZs = (slices.map { it.z } + other.slices.map { it.z }).sorted()
        val combinedWithLows = combinedZs.map { p -> Pair(p, op(this(p, false), other(p, false))) }
        val combinedWithHighs = combinedWithLows.map { (z, low) ->
            ProfileSlice(z, low, op(this(z, false), other(z, false)))
        }
        return ExtrusionProfile(combinedWithHighs)
    }

    private fun morph(other: Double, op: (Double, Double) -> Double): ExtrusionProfile {
        val otherProfile = ExtrusionProfile(
            listOf(ProfileSlice(0.0, other, other))
        )
        return morph(otherProfile, op)
    }


    fun maxWith(other: ExtrusionProfile): ExtrusionProfile = morph(other) { a, b -> max(a, b) }

    operator fun plus(other: ExtrusionProfile): ExtrusionProfile = morph(other) { a, b -> a + b }

    operator fun plus(other: Double): ExtrusionProfile = morph(other) { a, b -> a + b }


    operator fun minus(other: ExtrusionProfile): ExtrusionProfile = morph(other) { a, b -> a - b }

    operator fun minus(other: Double): ExtrusionProfile = morph(other) { a, b -> a - b }

    /**
     * Clip or extend a profile
     */
    fun clipped(start: Double, end: Double): ExtrusionProfile {
        val newSlices = ArrayList(
            listOf(
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
        return ExtrusionProfile(newSlices)
    }

    fun reversed(): ExtrusionProfile {
        return ExtrusionProfile(slices.reversed())
    }

    fun moved(offset: Double): ExtrusionProfile {
        return ExtrusionProfile(slices.map { (z, low, high) ->
            ProfileSlice(z + offset, low, high)
        })
    }

    fun appendedWith(other: ExtrusionProfile): ExtrusionProfile {
        val positionedOther = other.moved(slices.last().z)
        return ExtrusionProfile(
            slices + positionedOther.slices
        )
    }

    fun asStepped(maxStep: Double): ExtrusionProfile {
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
            val numberOfSteps =
                ((higherSegmentBottomDiameter - lowerSegmentTopDiameter).absoluteValue / maxStep).toInt() + 1
            if (numberOfSteps == 0) {
                continue
            }
            newZs.addAll((1 until numberOfSteps).map { stepNumber ->
                (higherSegmentBottomPosition - lowerSegmentTopPosition) * stepNumber.toDouble() / numberOfSteps + lowerSegmentTopPosition
            })
        }
        // Tack on the position of the very top of the instrument.
        newZs.add(slices.last().z)

        // Compute the new diameters.
        val newDiams = (0 until newZs.size - 1).map { i -> this(0.5 * (newZs[i] + newZs[i + 1])) }

        val newSlices = (0 until newZs.size - 1).map { i ->
            ProfileSlice(newZs[i], newDiams[max(i - 1, 0)], newDiams[min(i + 1, newZs.size - 1)])
        }
        return ExtrusionProfile(newSlices)
    }

    override val valueType: ValueType<ExtrusionProfile> = ExtrusionProfileType

    override fun twist(): Twist =
        Twist.obj("ExtrusionProfile",
            Twist.array("slices", slices)
        )
}

object ExtrusionProfileType: ValueType<ExtrusionProfile>() {
    override val name: String = "ExtrusionProfile"

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override fun add(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIs(v1)
        val p2 = assertIs(v2)
        return p1 + p2
    }

    override fun subtract(
        v1: Value,
        v2: Value
    ): Value {
        val p1 = assertIs(v1)
        val p2 = assertIs(v2)
        return p1 - p2
    }

    override fun mult(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "multiplication")
    }

    override fun div(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "division")
    }

    override fun mod(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "modulo")
    }

    override fun pow(
        v1: Value,
        v2: Value
    ): Value {
        throw SimplexUnsupportedOperation(name, "exponentiation")
    }

    override fun equals(
        v1: Value,
        v2: Value
    ): Boolean {
        return v1 == v2
    }

    override fun neg(v1: Value): Value {
        val p = assertIs(v1)
        return p.reversed()
    }

    override fun compare(
        v1: Value,
        v2: Value
    ): Int {
        throw SimplexUnsupportedOperation(name, "ordering")
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object: PrimitiveFunctionValue("profile",
                FunctionSignature(listOf(
                    Param("slices", ArrayValueType.of(ProfileSliceType))),
                    ExtrusionProfileType)) {
                override fun execute(args: List<Value>): Value {
                    val arr = ArrayValueType.of(ProfileSliceType).assertIsArray(args[0]).map {
                        ProfileSliceType.assertIs(it)
                    }
                    return ExtrusionProfile(arr)
                }
            }
        )
    }

    override val providesOperations: List<PrimitiveMethod<ExtrusionProfile>> by lazy {
        listOf(
            object: PrimitiveMethod<ExtrusionProfile>("clipped",
                MethodSignature<ExtrusionProfile>(
                    ExtrusionProfileType,
                    listOf(Param("low", FloatValueType), Param("high", FloatValueType)), ExtrusionProfileType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>
                ): Value {
                    val profile = assertIs(target)
                    val start = assertIsFloat(args[0])
                    val end = assertIsFloat(args[1])
                    return profile.clipped(start, end)
                }
            },
            object: PrimitiveMethod<ExtrusionProfile>("move",
                MethodSignature(ExtrusionProfileType,
                    listOf(Param("distance", FloatValueType)),
                    ExtrusionProfileType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>
                ): Value {
                    val profile = assertIs(target)
                    val distance = assertIsFloat(args[1])
                    return profile.moved(distance)
                }
            },
            object: PrimitiveMethod<ExtrusionProfile>("append",
                MethodSignature(ExtrusionProfileType,
                    listOf(Param("other", ExtrusionProfileType)),
                    ExtrusionProfileType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>
                ): Value {
                    val profile = assertIs(target)
                    val other = assertIs(args[0])
                    return profile.appendedWith(other)
                }
            },
            object: PrimitiveMethod<ExtrusionProfile>("stepped",
                MethodSignature<ExtrusionProfile>(
                    ExtrusionProfileType,
                    listOf(Param("stepSize", FloatValueType)),
                    ExtrusionProfileType)) {
                override fun execute(
                    target: Value,
                    args: List<Value>
                ): Value {
                    val profile = assertIs(target)
                    val maxStep = assertIsFloat(args[0])
                    return profile.asStepped(maxStep)
                }
            }
        )
    }
}
