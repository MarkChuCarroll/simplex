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

import { type LineAndColumnInfo } from "ohm-js"
import { Expression } from "simplex/expr/expr"
import { AstNode } from "simplex/ast/ast"
import { Twist } from "simplex/util/twist"
import { Environment } from "simplex/runtime/env"
import { TypeRegistry, VectorStaticType, type StaticType } from "simplex/runtime/value/static_types"
import { NoneValue, VectorValue, type Value } from "simplex/runtime/value/value"
import { NoneType, VectorType } from "simplex/runtime/value/types"

export class CondClause extends AstNode {
  condition: Expression
  value: Expression
  constructor(condition: Expression, value: Expression, loc: LineAndColumnInfo) {
    super(loc)
    this.condition = condition
    this.value = value
  }

  override twist(): Twist {
    return Twist.obj("CondClause",
      Twist.value("condition", this.condition),
      Twist.value("value", this.value))
  }
}

export class CondExpression extends Expression {
  clauses: CondClause[]
  else_clause: Expression

  constructor(cond_clauses: CondClause[], else_clause: Expression, loc: LineAndColumnInfo) {
    super(loc)
    this.clauses = cond_clauses
    this.else_clause = else_clause
  }

  static_type(in_env: Environment): StaticType {
    return this.else_clause.static_type(in_env)
  }

  validate(in_env: Environment): void {
    const s: Set<StaticType> = new Set()
    for (const cl of this.clauses) {
      cl.condition.validate(in_env)
      cl.value.validate(in_env)
      s.add(cl.value.static_type(in_env))
    }
    this.else_clause.validate(in_env)
    s.add(this.else_clause.static_type(in_env))
    if (s.size > 1) {
      throw new Error("Type error: result type of clauses in a cond must match")
    }
  }

  evaluate(in_env: Environment): Value {
    for (const cc of this.clauses) {
      const c = cc.condition.evaluate(in_env)
      if (c.get_type().is_truthy(c)) {
        return cc.value.evaluate(in_env)
      }
    }
    return this.else_clause.evaluate(in_env)
  }

  override twist(): Twist {
    return Twist.obj("Cond",
      Twist.array("clauses", this.clauses),
      Twist.value("else", this.else_clause)
    )
  }
}

export class ForExpr extends Expression {
  index: string
  range: Expression
  body: Expression[]
  env: Environment | null = null

  constructor(index: string,
    range: Expression,
    body: Expression[],
    loc: LineAndColumnInfo) {
    super(loc)
    this.index = index
    this.range = range
    this.body = body
  }

  local_env(in_env: Environment) {
    if (!this.env) {
      this.env = new Environment(in_env)
      const el_type = this.range.static_type(in_env)
      this.env.declare_type_of(this.index, TypeRegistry.get().get_value_type(el_type))
    }
    return this.env
  }

  static_type(in_env: Environment): StaticType {
    const env = this.local_env(in_env)
    return this.body[this.body.length - 1]!.static_type(env)
  }

  validate(in_env: Environment): void {
    this.range.validate(in_env)
    const el_type = this.range.static_type(in_env)
    if (!(this.range.static_type(in_env) instanceof VectorType)) {
      throw new Error("Range of a for loop must be a vector")
    }
    const local_env = this.local_env(in_env)
    for (const b of this.body) {
      b.validate(local_env)
    }
  }

  evaluate(in_env: Environment): Value {
    const coll = this.range.evaluate(in_env) as VectorValue
    const local_env = this.local_env(in_env)
    const result_element_type = (this.static_type(in_env) as VectorStaticType).element_type
    const result: Value[] = []
    for (const el of coll.values) {
      local_env.set_value(this.index, el)
      var result_element = NoneValue.get()
      for (const b of this.body) {
        result_element = b.evaluate(local_env)
      }
      result.push(result_element)
    }
    return new VectorValue(TypeRegistry.get().get_value_type(result_element_type), result)
  }

  override twist(): Twist {
    return Twist.obj("ForExpr",
      Twist.attr("idx", this.index),
      Twist.value("range", this.range),
      Twist.array("body", this.body)
    )
  }
}

export class WhileExpr extends Expression {
  cond: Expression
  body: Expression[]
  env: Environment | null = null

  constructor(cond: Expression, body: Expression[], loc: LineAndColumnInfo) {
    super(loc)
    this.cond = cond
    this.body = body
  }

  local_env(in_env: Environment) {
    if (!this.env) {
      this.env = new Environment(in_env)
    }
    return this.env
  }

  static_type(in_env: Environment): StaticType {
    const env = this.local_env(in_env)
    var stype: StaticType = NoneType.get().static_type()
    for (const b of this.body) {
      stype = b.static_type(env)
    }
    return stype
  }

  validate(in_env: Environment): void {
    const env = this.local_env(in_env)
    this.cond.validate(env)
    for (const b of this.body) {
      b.validate(env)
    }
  }

  evaluate(in_env: Environment): Value {
    const env = this.local_env(in_env)
    var cond_val = this.cond.evaluate(env)
    var result: Value = NoneValue.get()
    while (cond_val.get_type().is_truthy(cond_val)) {
      for (const b of this.body) {
        result = b.evaluate(env)
      }
    }
    return result
  }

  override twist(): Twist {
    return Twist.obj("WhileExpr",
      Twist.value("cond", this.cond),
      Twist.array("body", this.body)
    )
  }
}
