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
import type { Environment } from "simplex/runtime/env"
import { TypeRegistry, type StaticType } from "simplex/runtime/value/static_types"
import { BooleanValue, type Value } from "simplex/runtime/value/value"
import { Twist } from "simplex/util/twist"

interface Operator {
  syntax: string
  name: string
  arity: number
  category: "method" | "comparison" | "logic"
}

export const Operators: Operator[] = [
  { syntax: "+", name: "plus", arity: 2, category: "method" },
  { syntax: "-", name: "minus", arity: 2, category: "method" },
  { syntax: "*", name: "times", arity: 2, category: "method" },
  { syntax: "/", name: "div", arity: 2, category: "method" },
  { syntax: "%", name: "mod", arity: 2, category: "method" },
  { syntax: "^", name: "exp", arity: 2, category: "method" },
  { syntax: "&", name: "and", arity: 2, category: "logic" },
  { syntax: "|", name: "or", arity: 2, category: "logic" },
  { syntax: "!", name: "not", arity: 1, category: "logic" },
  { syntax: ">", name: "gt", arity: 2, category: "comparison" },
  { syntax: ">=", name: "ge", arity: 2, category: "comparison" },
  { syntax: "<", name: "lt", arity: 2, category: "comparison" },
  { syntax: "<=", name: "le", arity: 2, category: "comparison" },
  { syntax: "==", name: "eq", arity: 2, category: "method" },
  { syntax: "!=", name: "neq", arity: 2, category: "method" },
  { syntax: "-", name: "uminus", arity: 1, category: "method" }
]

export class ArithExpr extends Expression {
  op: Operator
  args: Expression[]

  constructor(op_sym: string, args: Expression[], loc: LineAndColumnInfo) {
    super(loc)
    this.op = ArithExpr.get_operator(op_sym, args.length)
    this.args = args
  }

  static_type(in_env: Environment): StaticType {
    const target = this.args[0]!
    switch (this.op.category) {
      case "method":
        const op_impl = TypeRegistry.get().get_value_type(target.static_type(in_env)).get_op(this.op.name)
        return op_impl.op_type.static_type()
      case "comparison":
        // Verify that the target type supports comparisons
        TypeRegistry.get().get_value_type(target.static_type(in_env)).get_op("compare")
        return TypeRegistry.get().boolType.static_type()
      case "logic":
        return TypeRegistry.get().boolType.static_type()
      default:
        throw new Error(`Invalid operation: ${this.op.syntax}`)
    }
  }

  validate(in_env: Environment): void {
    const target_type = this.args[0]!.static_type(in_env)
    switch (this.op.category) {
      case "method":
        const op_impl = TypeRegistry.get().get_value_type(target_type).get_op(this.op.name)
        // We can assume the parameter count is correct, because the arity was used
        // to find the op.
        const param_types = op_impl.op_type.get_param_types()
        for (let i = 0; i < param_types.length; i++) {
          if (param_types[i]!.static_type() !== this.args[i + 1]!.static_type(in_env)) {
            throw new Error(`Type error: ${this.op.syntax} expected an argument of type ${param_types[i]!.toString()}, but recieved a ${this.args[i + 1]!.static_type(in_env)}`)
          }
        }
        return
      case "logic":
        for (const a of this.args) {
          a.validate(in_env)
        }
        return
      case "comparison":
        for (const a of this.args) {
          a.validate(in_env)
        }
        if (this.args.length > 1) {
          const op = TypeRegistry.get().get_value_type(this.args[0]!.static_type(in_env)).get_op("compare")
          if (this.args[1]!.static_type(in_env) !== op.op_type.params[0]!.static_type()) {
            throw new Error(`Type error: type ${this.args[0]!.static_type(in_env)} cannot be compared to a ${this.args[1].static_type(in_env)}`)
          }
        }
        return
      default:
        throw new Error("Invalid arithmetic operation")
    }
  }


  evaluate(in_env: Environment): Value {
    const target_type = this.args[0]!.static_type(in_env)
    switch (this.op.category) {
      case "method": {
        const target = this.args[0]!.evaluate(in_env)
        const op_impl = TypeRegistry.get().get_value_type(target_type).get_op(this.op.name)
        const others: Value[] = []
        for (let i = 1; i < this.args.length; i++) {
          others.push(this.args[i]!.evaluate(in_env))
        }
        return op_impl.apply(target, others, in_env)
      }
      case "comparison": {
        const target = this.args[0]!.evaluate(in_env)
        const compare = TypeRegistry.get().get_value_type(target_type).get_op(this.op.name)
        const others: Value[] = []
        for (let i = 1; i < this.args.length; i++) {
          others.push(this.args[i]!.evaluate(in_env))
        }
        const cmp = compare.apply(target, others, in_env) as IntValue
        switch (this.op.name) {
          case "gt": return new BooleanValue(cmp.int_value > 0n)
          case "ge": return new BooleanValue(cmp.int_value >= 0n)
          case "lt": return new BooleanValue(cmp.int_value < 0n)
          case "le": return new BooleanValue(cmp.int_value <= 0n)
          default: throw new Error(`Invalid comparison operation ${this.op.syntax}`)
        }
      }
      case "logic":
        let a0 = this.args[0]!.evaluate(in_env)
        switch (this.op.name) {
          case "and":
            if (a0.get_type().is_truthy(a0)) {
              const a1 = this.args[1]!.evaluate(in_env)
              return new BooleanValue(a1.get_type().is_truthy(a1))
            } else {
              return new BooleanValue(false)
            }
          case "or":
            if (a0.get_type().is_truthy(a0)) {
              return new BooleanValue(true)
            } else {
              const a1 = this.args[1]!.evaluate(in_env)
              return new BooleanValue(a1.get_type().is_truthy(a1))
            }
          case "not":
            return new BooleanValue(!a0.get_type().is_truthy(a0))
          default:
            throw new Error(`Invalid logic operation ${this.op.syntax}`)
        }
      default: throw new Error(`Invalid operator category ${this.op.category}`)
    }
  }

  static operator_map: Map<string, Operator[]> = new Map()
  static get_operator(syntax: string, arity: number): Operator {
    if (this.operator_map.size === 0) {
      for (const o of Operators) {
        var oprec = this.operator_map.get(syntax)
        if (!oprec) {
          oprec = []
          this.operator_map.set(syntax, oprec)
        }
        oprec.push(o)
      }
    }
    const possibles = this.operator_map.get(syntax)
    if (possibles) {
      for (const p of possibles) {
        if (p.arity === arity) {
          return p
        }
      }
    }
    throw new Error(`Operator ${syntax} with arity ${arity} not found`)
  }

  override twist(): Twist {
    return Twist.obj("ArithExpr",
      Twist.attr("op", this.op.syntax),
      Twist.array("args", this.args)
    )
  }
}
