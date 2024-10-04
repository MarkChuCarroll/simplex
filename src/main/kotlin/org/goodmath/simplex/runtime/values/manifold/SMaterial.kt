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
package org.goodmath.simplex.runtime.values.manifold

import manifold3d.glm.DoubleVec4Vector
import manifold3d.manifold.Material
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.runtime.Env
import org.goodmath.simplex.runtime.values.FunctionSignature
import org.goodmath.simplex.runtime.values.MethodSignature
import org.goodmath.simplex.runtime.values.Param
import org.goodmath.simplex.runtime.values.Value
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.primitives.ArrayValue
import org.goodmath.simplex.runtime.values.primitives.ArrayValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValue
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.NoneValueType
import org.goodmath.simplex.runtime.values.primitives.NoneValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveFunctionValue
import org.goodmath.simplex.runtime.values.primitives.PrimitiveMethod
import org.goodmath.simplex.runtime.values.primitives.StringValue
import org.goodmath.simplex.runtime.values.primitives.StringValueType
import org.goodmath.simplex.twist.Twist

class SMaterial(val name: String, val material: Material) : Value {
    override val valueType = SMaterialValueType

    override fun twist(): Twist = Twist.obj("Material", Twist.attr("name", name))

    fun setColor(color: RGBA) {
        material.color(color.toDVec4())
    }

    fun setMetalness(factor: Double) {
        material.metalness(factor.toFloat())
    }

    fun setRoughness(factor: Double) {
        material.roughness(factor.toFloat())
    }

    companion object {
        val smoothGray by lazy {
            val m = SMaterial("plain_gray", Material())
            m.setColor(RGBA.gray)
            m
        }

        val smoothBlue by lazy {
            val m = SMaterial("smooth_blue", Material())
            m.setColor(RGBA.blue)
            m
        }
        val smoothGreen by lazy {
            val m = SMaterial("smooth_green", Material())
            m.setColor(RGBA.gray)
            m
        }
        val smoothAqua by lazy {
            val m = SMaterial("smooth_aqua", Material())
            m.setColor(RGBA.gray)
            m
        }

        val roughGray by lazy {
            val m = SMaterial("rough_gray", Material())
            m.setColor(RGBA.gray)
            m.setRoughness(1.0)
            m
        }

        val roughBlue by lazy {
            val m = SMaterial("rough_blue", Material())
            m.setColor(RGBA.blue)
            m.setRoughness(1.0)
            m
        }

        val roughGreen by lazy {
            val m = SMaterial("rough_green", Material())
            m.setColor(RGBA.gray)
            m.setRoughness(1.0)
            m
        }
        val roughAqua by lazy {
            val m = SMaterial("rough_aqua", Material())
            m.setColor(RGBA.gray)
            m.setRoughness(1.0)
            m
        }
        val metalGray by lazy {
            val m = SMaterial("metal_gray", Material())
            m.setColor(RGBA.gray)
            m.setMetalness(1.0)
            m
        }

        val metalBlue by lazy {
            val m = SMaterial("metal_blue", Material())
            m.setColor(RGBA.blue)
            m.setMetalness(1.0)
            m
        }

        val metalGreen by lazy {
            val m = SMaterial("metal_green", Material())
            m.setColor(RGBA.gray)
            m.setMetalness(1.0)
            m
        }
        val metalAqua by lazy {
            val m = SMaterial("metal_aqua", Material())
            m.setColor(RGBA.gray)
            m.setMetalness(1.0)
            m
        }
        val metalGold by lazy {
            val m = SMaterial("metal_gold", Material())
            m.setColor(RGBA(0.7, 0.7, 0.7, 1.0))
            m.setMetalness(1.0)
            m
        }
    }
}

object SMaterialValueType : ValueType() {
    override val name: String = "Material"

    override val asType: Type by lazy {
        Type.simple(name)
    }

    override fun isTruthy(v: Value): Boolean {
        return true
    }

    override val providesFunctions: List<PrimitiveFunctionValue> by lazy {
        listOf(
            object :
                PrimitiveFunctionValue(
                    "material",
                    FunctionSignature.simple(listOf(Param("name", StringValueType.asType)), asType),
                ) {
                override fun execute(args: List<Value>): Value {
                    val name = assertIsString(args[0])
                    return SMaterial(name, Material())
                }
            }
        )
    }

    override val providesPrimitiveMethods: List<PrimitiveMethod> by lazy {
        listOf(
            object :
                PrimitiveMethod(
                    "set_roughness",
                    MethodSignature.simple(
                        asType,
                        listOf(Param("roughness", FloatValueType.asType)),
                        NoneValueType.asType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val roughness = assertIsFloat(args[0])
                    self.material.roughness(roughness.toFloat())
                    return NoneValue
                }
            },
            object :
                PrimitiveMethod(
                    "set_metalness",
                    MethodSignature.simple(
                        asType,
                        listOf(Param("metalness", FloatValueType.asType)),
                        NoneValueType.asType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).material
                    val metalness = assertIsFloat(args[0])
                    self.metalness(metalness.toFloat())
                    return NoneValue
                }
            },
            object :
                PrimitiveMethod(
                    "set_vert_colors",
                    MethodSignature.simple(
                        asType,
                        listOf(Param("colors", Type.array(RGBAValueType.asType))),
                        NoneValueType.asType,
                    ),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val colorList =
                        ArrayValueType.of(RGBAValueType).assertIs(args[0]).elements.map {
                            RGBAValueType.assertIs(it).toDVec4()
                        }
                    val colorVec = DoubleVec4Vector()
                    colorVec.resize(colorList.size.toLong())
                    colorList.forEach { color -> colorVec.pushBack(color) }
                    self.material.vertColor(colorVec)
                    return NoneValue
                }
            },
            object :
                PrimitiveMethod(
                    "set_color",
                    MethodSignature.simple(asType, listOf(Param("color", RGBAValueType.asType)), NoneValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val color = RGBAValueType.assertIs(args[0])
                    self.material.color(color.toDVec4())
                    return NoneValue
                }
            },
            object :
                PrimitiveMethod(
                    "get_color",
                    MethodSignature.simple(asType, emptyList<Param>(), RGBAValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).material
                    val color = self.color()
                    return RGBA.fromDVec4(color)
                }
            },
            object :
                PrimitiveMethod(
                    "get_vert_colors",
                    MethodSignature.simple(asType, emptyList<Param>(), Type.array(RGBAValueType.asType)),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    val colorVec = self.material.vertColor().map { RGBA.fromDVec4(it) }
                    return ArrayValue(RGBAValueType, colorVec)
                }
            },
            object :
                PrimitiveMethod(
                    "metalness",
                    MethodSignature.simple(asType, emptyList<Param>(), FloatValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).material
                    return FloatValue(self.metalness().toDouble())
                }
            },
            object :
                PrimitiveMethod(
                    "roughness",
                    MethodSignature.simple(asType, emptyList<Param>(), FloatValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target).material
                    return FloatValue(self.roughness().toDouble())
                }
            },
            object :
                PrimitiveMethod(
                    "name",
                    MethodSignature.simple(asType, emptyList<Param>(), StringValueType.asType),
                ) {
                override fun execute(target: Value, args: List<Value>, env: Env): Value {
                    val self = assertIs(target)
                    return StringValue(self.name)
                }
            },
        )
    }
    override val providesVariables: Map<String, Value> by lazy {
        mapOf(
            "smooth_gray" to SMaterial.smoothGray,
            "smooth_blue" to SMaterial.smoothBlue,
            "smooth_green" to SMaterial.smoothGreen,
            "smooth_aqua" to SMaterial.smoothAqua,
            "rough_gray" to SMaterial.roughGray,
            "rough_blue" to SMaterial.roughBlue,
            "rough_green" to SMaterial.roughGreen,
            "rough_aqua" to SMaterial.roughAqua,
            "metal_gray" to SMaterial.metalGray,
            "metal_blue" to SMaterial.metalBlue,
            "metal_green" to SMaterial.metalGreen,
            "metal_aqua" to SMaterial.metalAqua,
            "metal_gold" to SMaterial.metalGold
        )
    }

    override fun assertIs(v: Value): SMaterial {
        return if (v is SMaterial) {
            v
        } else {
            throwTypeError(v)
        }
    }
}
