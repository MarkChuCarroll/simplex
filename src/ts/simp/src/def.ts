import { AstNode, Param } from "./ast"
import { Environment } from "./env"
import { DataType, DynamicType, TypeRegistry } from "./value/types"
import { Expression } from "./expr/expr"
import { LineAndColumnInfo } from "ohm-js"
import { FunctionStaticType, NamedStaticType, OperationStaticType, StaticType } from "./value/static_types"
import { Twist } from "./twist"
import { FunctionValue, SimplexFunctionValue, SimplexOperationValue } from "./value/value"


export abstract class Definition extends AstNode {
    name: string

    constructor(name: string, loc: LineAndColumnInfo) {
        super(loc)
        this.name = name
    }

    abstract typeCheck(in_env: Environment): void

    abstract install_in(env: Environment): void

    abstract static_type(): StaticType
}


export class OperationDefinition extends Definition {
    target_type: StaticType
    params: Param[]
    return_type: StaticType
    body: Expression[]

    constructor(name: string,
        target_type: StaticType,
        params: Param[],
        return_type: StaticType,
        body: Expression[],
        loc: LineAndColumnInfo) {
        super(name, loc)
        this.target_type = target_type
        this.params = params
        this.return_type = return_type
        this.body = body
    }

    override install_in(env: Environment): void {
        const op_val = new SimplexOperationValue(this, env)
        const target_value_type = TypeRegistry.get().get_value_type(this.target_type) as DataType
        target_value_type.register_simplex_op(this)
    }

    override typeCheck(in_env: Environment): boolean {
        throw new Error("Method not implemented.")
    }

    override twist(): Twist {
        return Twist.obj("OpDef",
            Twist.attr("name", this.name),
            Twist.value("target", this.target_type),
            Twist.array("params", this.params),
            Twist.value("return_type", this.return_type),
            Twist.array("body", this.body)
        )
    }

    override static_type(): StaticType {
        return new OperationStaticType(this.target_type, this.params.map((it) => it.stype), this.return_type, this.loc)
    }

}

export class FunctionDefinition extends Definition {
    params: Param[]
    return_type: StaticType
    body: Expression[]

    constructor(name: string,
        params: Param[],
        return_type: StaticType,
        body: Expression[],
        loc: LineAndColumnInfo) {
        super(name, loc)
        this.params = params
        this.return_type = return_type
        this.body = body
    }

    override install_in(env: Environment): void {
        const fval = new SimplexFunctionValue(this, env)
        env.declare_type_of(this.name, fval.get_type())
        env.set_value(this.name, fval)
    }

    override typeCheck(in_env: Environment): boolean {
        throw new Error("Method not implemented.")
    }

    override twist(): Twist {
        return Twist.obj("name",
            Twist.array("args", this.params),
            Twist.value("return_type", this.return_type),
            Twist.array("body", this.body))
    }

    override static_type(): StaticType {
        return new FunctionStaticType(this.params.map((it) => it.stype), this.return_type, this.loc)
    }

}

export class DataDefinition extends Definition {
    fields: Param[]

    constructor(name: string, fields: Param[], loc: LineAndColumnInfo) {
        super(name, loc)
        this.fields = fields
    }

    override static_type(): StaticType {
        return new NamedStaticType(this.name, this.loc)
    }

    override typeCheck(in_env: Environment): boolean {
        return true
    }

    override install_in(env: Environment): void {
        const data_type = TypeRegistry.get().dataType(this)
    }

    override twist(): Twist {
        return Twist.obj("DataDef",
            Twist.attr("name", this.name),
            Twist.array("fields", this.fields)
        )
    }


}

export class VarDefinition extends Definition {
    stype: StaticType | null
    value: Expression

    constructor(name: string, stype: StaticType | null, value: Expression, loc: LineAndColumnInfo) {
        super(name, loc)
        this.stype = stype
        this.value = value
    }

    typeCheck(in_env: Environment): boolean {
        if (this.stype != null && this.stype !== this.value.static_type(in_env)) {
            throw new Error(`Type error: variable expected a value of type ${this.stype}, but received a ${this.value.static_type(in_env)}`)
        }
        return true

    }
    install_in(env: Environment): void {
        const value = this.value.evaluate(env)
        env.declare_type_of(this.name, value.get_type())
        env.set_value(this.name, value)
    }

    override twist(): Twist {
        return Twist.obj("VarDef",
            Twist.attr("name", this.name),
            Twist.value("type", this.stype),
            Twist.value("value", this.value)

        )
    }

    static_type(): StaticType {
        if (this.stype != null) {
            return this.stype
        } else {
            return this.value.static_type(Environment.root)
        }
    }
}