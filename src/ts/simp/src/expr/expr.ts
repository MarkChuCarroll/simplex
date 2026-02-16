/*
 * Copyright 2025 Mark C. Chu-Carroll
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

import { type LineAndColumnInfo } from "ohm-js";
import { AstNode, Param } from "simplex/ast/ast";
import type { Environment } from "simplex/runtime/env";
import { FunctionStaticType, NamedStaticType, TypeRegistry, VectorStaticType, type StaticType } from "simplex/runtime/value/static_types";
import { DataValue, FloatValue, IntValue, NoneValue, StringValue, VectorValue, type Value } from "simplex/runtime/value/value";
import { Twist } from "simplex/util/twist";
import { BooleanType, DataType, StringType } from "simplex/runtime/value/types";

export abstract class Expression extends AstNode {
  constructor(loc: LineAndColumnInfo) {
    super(loc)
  }

  abstract static_type(in_env: Environment): StaticType
  abstract validate(in_env: Environment): void
  abstract evaluate(in_env: Environment): Value
}

export class BlockExpr extends Expression {
  body: Expression[]

  constructor(body: Expression[], loc: LineAndColumnInfo) {
    super(loc)
    this.body = body
  }

  override static_type(in_env: Environment): StaticType {
    return this.body[this.body.length - 1]!.static_type(in_env)
  }

  override validate(in_env: Environment): void {
    for (const b of this.body) {
      b.validate(in_env)
    }
  }

  override evaluate(in_env: Environment): Value {
    var result: Value = NoneValue.get()
    for (const b of this.body) {
      result = b.evaluate(in_env)
    }
    return result
  }

  override twist(): Twist {
    return Twist.obj("BlockExpr", ...this.body)
  }
}

export class LetExpr extends Expression {
  name: string
  stype: StaticType | null
  value: Expression

  constructor(name: string, stype: StaticType | null, value: Expression, loc: LineAndColumnInfo) {
    super(loc)
    this.name = name
    this.stype = stype
    this.value = value
  }

  override static_type(in_env: Environment): StaticType {
    if (this.stype) {
      return this.stype
    } else {
      return this.value.static_type(in_env)
    }

  }

  override validate(in_env: Environment): void {
    const value_type = TypeRegistry.get().get_value_type(this.static_type(in_env))
    in_env.declare_type_of(this.name, value_type)
    this.value.validate(in_env)
    if (this.stype !== null && this.stype !== this.value.static_type(in_env)) {
      throw new Error(`Type error in let statement: declared type was ${this.stype}, but actual type was ${this.value.static_type(in_env)}`)
    }
  }

  override evaluate(in_env: Environment): Value {
    const v = this.value.evaluate(in_env)
    in_env.set_value(this.name, v)
    return v
  }

  override twist(): Twist {
    return Twist.obj("LetExpr",
      Twist.attr("name", this.name),
      Twist.value("type", this.stype),
      Twist.value("value", this.value))
  }
}

export class IntLiteralExpr extends Expression {
  int_value: bigint

  constructor(iv: bigint, loc: LineAndColumnInfo) {
    super(loc)
    this.int_value = iv
  }

  override static_type(_in_env: Environment): StaticType {
    return TypeRegistry.get().intType.static_type()
  }

  override validate(_in_env: Environment): void {
  }

  override evaluate(_in_env: Environment): Value {
    return new IntValue(this.int_value)
  }

  override twist(): Twist {
    return Twist.obj("IntLit",
      Twist.attr("value", this.int_value.toString())
    )
  }
}

export class FloatLiteralExpr extends Expression {
  float_value: number

  constructor(
    f_val: number,
    loc: LineAndColumnInfo) {
    super(loc)
    this.float_value = f_val
  }

  override static_type(_in_env: Environment): StaticType {
    return TypeRegistry.get().floatType.static_type()
  }

  override validate(_in_env: Environment): void {
  }

  override evaluate(_in_env: Environment): Value {
    return new FloatValue(this.float_value)
  }

  override twist(): Twist {
    return Twist.obj("FloatLit",
      Twist.attr("value", this.float_value.toString())
    )
  }
}

export class StringLiteralExpression extends Expression {
  str_value: string

  constructor(str_value: string,
    loc: LineAndColumnInfo) {
    super(loc)
    this.str_value = str_value
  }

  override static_type(_in_env: Environment): StaticType {
    return StringType.get().static_type()
  }

  override validate(_in_env: Environment): void {
  }

  override evaluate(_in_env: Environment): Value {
    return new StringValue(this.str_value)
  }

  override twist(): Twist {
    return Twist.obj("StrLit",
      Twist.attr("value", this.str_value.toString())
    )
  }

}

export class BooleanLiteralExpr extends Expression {
  bool_val: boolean

  constructor(
    b_val: boolean,
    loc: LineAndColumnInfo) {
    super(loc)
    this.bool_val = b_val
  }

  override static_type(_in_env: Environment): StaticType {
    return BooleanType.get().static_type()
  }

  override validate(_in_env: Environment): void {
    throw new Error("Method not implemented.");
  }

  override evaluate(_in_env: Environment): Value {
    throw new Error("Method not implemented.");
  }

  override twist(): Twist {
    return Twist.obj("BoolLit",
      Twist.attr("value", this.bool_val.toString())
    )
  }
}


export class VectorExpr extends Expression {
  value_type: StaticType
  values: Expression[]

  constructor(value_type: StaticType,
    values: Expression[], loc: LineAndColumnInfo) {
    super(loc)
    this.value_type = value_type
    this.values = values
  }

  override static_type(_in_env: Environment): StaticType {
    return new VectorStaticType(this.value_type)
  }

  override validate(in_env: Environment): void {
    for (const el of this.values) {
      el.validate(in_env)
      if (this.value_type !== el.static_type(in_env)) {
        throw new Error(`TypeError: vector of type ${this.static_type(in_env)} cannot contain an element of type ${el.static_type(in_env)}`)
      }
    }
  }

  override evaluate(in_env: Environment): Value {
    return new VectorValue(TypeRegistry.get().get_value_type(this.value_type),
      this.values.map((it) => it.evaluate(in_env)))
  }

  override twist(): Twist {
    return Twist.obj("VectorLit",
      Twist.value("type", this.value_type),
      Twist.array("value", this.values)
    )
  }
}

export class LambdaExpr extends Expression {
  params: Param[]
  return_type: StaticType
  body: Expression[]

  constructor(
    params: Param[],
    return_type: StaticType,
    body: Expression[],
    loc: LineAndColumnInfo) {
    super(loc)
    this.params = params
    this.return_type = return_type
    this.body = body
  }

  override static_type(_in_env: Environment): StaticType {
    return new FunctionStaticType(
      this.params.map((it) => it.stype),
      this.return_type)
  }

  override validate(_in_env: Environment): void {
    throw new Error("Method not implemented.");
  }

  override evaluate(_in_env: Environment): Value {
    throw new Error("Method not implemented.");
  }

  override twist(): Twist {
    return Twist.obj("LambdaExpr",
      Twist.array("params", this.params),
      Twist.value("return_type", this.return_type),
      Twist.array("body", this.body)
    )
  }
}

export class VarRefExpr extends Expression {
  name: string

  constructor(name: string, loc: LineAndColumnInfo) {
    super(loc)
    this.name = name
  }

  override static_type(in_env: Environment): StaticType {
    return in_env.get_declared_type_of(this.name).static_type()
  }

  override validate(in_env: Environment): void {
    in_env.get_declared_type_of(this.name)
  }

  override evaluate(in_env: Environment): Value {
    return in_env.get_value(this.name)
  }

  override twist(): Twist {
    return Twist.obj("VarRefExpr",
      Twist.attr("name", this.name)
    )
  }

}

export class SubscriptExpr extends Expression {
  vector_expr: Expression
  index: Expression

  constructor(vec: Expression, index: Expression, loc: LineAndColumnInfo) {
    super(loc)
    this.vector_expr = vec
    this.index = index
  }

  override static_type(in_env: Environment): StaticType {
    return (this.vector_expr.static_type(in_env) as VectorStaticType).element_type
  }

  override validate(in_env: Environment): void {
    if (!(this.vector_expr.static_type(in_env) instanceof VectorStaticType)) {
      throw new Error(`Type Error: the target of a subscript expression must be a vector, but found ${this.vector_expr.static_type(in_env)}`)
    }
    if (this.index.static_type(in_env) !== IntType.get().static_type()) {
      throw new Error(`Type Error: the index of a subscript expression must be an Int, but found ${this.vector_expr.static_type(in_env)}`)
    }
  }

  override evaluate(in_env: Environment): Value {
    const vec_val = this.vector_expr.evaluate(in_env) as VectorValue
    const idx = this.index.evaluate(in_env) as IntValue
    return vec_val.values[Number(idx.int_value)]!
  }

  override twist(): Twist {
    return Twist.obj("SubscriptExpr",
      Twist.value("vec", this.vector_expr),
      Twist.value("idx", this.index)
    )
  }
}

export class FieldExpr extends Expression {
  data_expr: Expression
  field_name: string

  constructor(data_expr: Expression,
    field_name: string, loc: LineAndColumnInfo) {
    super(loc)
    this.field_name = field_name
    this.data_expr = data_expr
  }


  override static_type(in_env: Environment): StaticType {
    const data_type = TypeRegistry.get().get_value_type(this.data_expr.static_type(in_env)) as DataType
    const fieldIdx = data_type.index_of_field(this.field_name)
    return data_type.def.fields[fieldIdx]!.stype
  }

  override validate(in_env: Environment): void {
    const data_type = TypeRegistry.get().get_value_type(this.data_expr.static_type(in_env))
    if (data_type instanceof DataType) {
      // Will throw an error if the field isn't defined in the type
      data_type.index_of_field(this.field_name)
    } else {
      throw new Error(`Type error: ${data_type} is not a data record type`)
    }
  }

  override evaluate(in_env: Environment): Value {
    const data_type = TypeRegistry.get().get_value_type(this.data_expr.static_type(in_env)) as DataType
    const fieldIdx = data_type.index_of_field(this.field_name)
    const data_value = this.data_expr.evaluate(in_env) as DataValue
    return data_value.fields[fieldIdx]!
  }

  override twist(): Twist {
    return Twist.obj("FieldExpr",
      Twist.value("value", this.data_expr),
      Twist.attr("field", this.field_name)
    )
  }
}

export class DataExpr extends Expression {

  name: string
  fields: Expression[]

  constructor(name: string, fields: Expression[], loc: LineAndColumnInfo) {
    super(loc)
    this.name = name
    this.fields = fields
  }

  override static_type(in_env: Environment): StaticType {
    return new NamedStaticType(this.name)
  }

  override validate(in_env: Environment): void {
    const dt = TypeRegistry.get().get_value_type(this.static_type(in_env))
    if (!(dt instanceof DataType)) {
      throw new Error(`${this.name} is not a data record`)
    }
    if (this.fields.length !== dt.def.fields.length) {
      throw new Error(`${this.name} expected ${dt.def.fields.length} values, but received ${this.fields.length}`)
    }
    for (let i = 0; i < this.fields.length; i++) {
      const value_type = this.fields[i]!.static_type(in_env)
      const field_type = dt.def.fields[i]!.stype
      if (value_type !== field_type) {
        throw new Error(`Type error: field ${dt.def.fields[i]!.name} is type ${field_type}, but received ${value_type}`)
      }
    }
  }

  override evaluate(in_env: Environment): Value {
    const dt = TypeRegistry.get().get_value_type(this.static_type(in_env)) as DataType
    return new DataValue(dt, this.fields.map((it) => it.evaluate(in_env)))
  }

  override twist(): Twist {
    return Twist.obj("DataExpr",
      Twist.attr("kind", this.name),
      Twist.array("fields", this.fields)
    )
  }
}

