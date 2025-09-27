import { LineAndColumnInfo } from "ohm-js"
import { AstNode } from "../ast"
import { Twist } from "../twist"


export abstract class StaticType extends AstNode {
    constructor(loc: LineAndColumnInfo) {
        super(loc)
    }

    abstract toString(): string
}

export class NamedStaticType extends StaticType {
    type_name: string

    constructor(name: string, loc: LineAndColumnInfo) {
        super(loc)
        this.type_name = name
    }

    override toString(): string {
        return this.type_name
    }

    override twist(): Twist {
        return Twist.obj("NamedSType", Twist.attr("name", this.type_name))
    }
}

export class VectorStaticType extends StaticType {
    element_type: StaticType

    constructor(el: StaticType, loc: LineAndColumnInfo) {
        super(loc)
        this.element_type = el
    }

    override toString(): string {
        return `[${this.element_type.toString()}]`
    }

    override twist(): Twist {
        return Twist.obj("VecSType", Twist.value("element_type", this.element_type))
    }
}

export class OperationStaticType extends StaticType {
    target_type: StaticType
    param_types: StaticType[]
    result_type: StaticType

    constructor(target: StaticType,
        params: StaticType[],
        result: StaticType,
        loc: LineAndColumnInfo) {
        super(loc)
        this.param_types = params
        this.result_type = result
        this.target_type = target
    }

    override toString(): string {
        return `${this.target_type.toString()}->(${this.param_types.map((it) => it.toString()).join(",")}):${this.result_type.toString()}`
    }

    override twist(): Twist {
        return Twist.obj("OpSType",
            Twist.value("target", this.target_type),
            Twist.array("params", this.param_types),
            Twist.value("result", this.result_type)
        )
    }
}

export class FunctionStaticType extends StaticType {
    param_types: StaticType[]
    result_type: StaticType

    constructor(
        params: StaticType[],
        result: StaticType,
        loc: LineAndColumnInfo) {
        super(loc)
        this.param_types = params
        this.result_type = result
    }

    override toString(): string {
        return `(${this.param_types.map((it) => it.toString()).join(",")}):${this.result_type.toString()} `
    }

    override twist(): Twist {
        return Twist.obj("FunSType",
            Twist.array("params", this.param_types),
            Twist.value("result", this.result_type)
        )
    }
}

