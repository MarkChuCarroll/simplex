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
import { TypeRegistry, type FunctionStaticType, type StaticType } from "simplex/runtime/value/static_types"
import { FunctionType } from "simplex/runtime/value/types"
import type { FunctionValue, Value } from "simplex/runtime/value/value"
import { Twist } from "simplex/util/twist"

export class FuncallExpr extends Expression {
  fun: Expression
  args: Expression[]

  constructor(fun: Expression, args: Expression[], loc: LineAndColumnInfo) {
    super(loc)
    this.fun = fun
    this.args = args
  }

  static_type(in_env: Environment): StaticType {
    const fun_type = this.fun.static_type(in_env) as FunctionStaticType
    return fun_type.result_type
  }

  validate(in_env: Environment): void {
    const fun_type = this.fun.static_type(in_env)
    if (!(fun_type instanceof FunctionType)) {
      throw new Error("Not a function")
    }
    if (this.args.length !== fun_type.params.length) {
      throw new Error(`Invalid function call: expected ${fun_type.params.length} arguments, but received ${this.args.length}`)
    }
    for (let i = 0; i < this.args.length; i++) {
      const param = fun_type.params[i]!
      const arg = this.args[i]!
      arg.validate(in_env)
      if (arg.static_type(in_env) !== param.static_type()) {
        throw new Error(`Type mismatch: argument number ${i} expected type ${param}, but received ${arg.static_type(in_env)}`)
      }
    }
  }


  evaluate(in_env: Environment): Value {
    const fun_val = this.fun.evaluate(in_env) as FunctionValue
    const args: Value[] = []
    for (const a of this.args) {
      args.push(a.evaluate(in_env))
    }
    return fun_val.apply(args, in_env)
  }

  override twist(): Twist {
    return Twist.obj("FuncallExpr",
      Twist.value("fun", this.fun),
      Twist.array("args", this.args)
    )
  }
}

export class MethodCallExpr extends Expression {
  target: Expression
  op: string
  args: Expression[]

  constructor(
    target: Expression,
    op: string,
    args: Expression[],
    loc: LineAndColumnInfo
  ) {
    super(loc)
    this.target = target
    this.op = op
    this.args = args
  }

  static_type(in_env: Environment): StaticType {
    const target_type = TypeRegistry.get().get_value_type(this.target.static_type(in_env))
    const op = target_type.get_op(this.op)
    return op.op_type.return_type.static_type()
  }

  validate(in_env: Environment): void {
    this.target.validate(in_env)
    const target_type = this.target.static_type(in_env)
    const op = TypeRegistry.get().get_value_type(target_type).get_op(this.op)
    const param_types = op.op_type.get_param_types()
    if (this.args.length != param_types.length) {
      throw new Error(`Operation ${this.op} on type ${target_type} expected ${param_types.length} arguments, but received ${this.args.length}`)
    }
    for (let i = 0; i < this.args.length; i++) {
      const param = param_types[i]!
      const arg = this.args[i]!
      arg.validate(in_env)
      if (arg.static_type(in_env) !== param.static_type()) {
        throw new Error(`Type mismatch: argument ${op.param_names[i]} expected type ${param}, but received ${arg.static_type(in_env)}`)
      }
    }
  }

  evaluate(in_env: Environment): Value {
    const target_val = this.target.evaluate(in_env)
    const op = target_val.get_type().get_op(this.op)
    const args: Value[] = []
    for (const a of this.args) {
      args.push(a.evaluate(in_env))
    }
    return op.apply(target_val, args, in_env)
  }

  override twist(): Twist {
    return Twist.obj("OpcallExpr",
      Twist.value("target", this.target),
      Twist.attr("name", this.op),
      Twist.array("args", this.args)
    )
  }

}
