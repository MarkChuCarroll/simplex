import { LineAndColumnInfo } from "ohm-js"
import { Definition } from "./def"
import { Expression } from "./expr/expr"
import { StaticType } from "./value/static_types"
import { Twist, Twistable } from "./twist"


export abstract class AstNode implements Twistable {
    loc: LineAndColumnInfo
    constructor(loc: LineAndColumnInfo) {
        this.loc = loc
    }

    abstract twist(): Twist
}

export class Model extends AstNode {
    defs: Map<string, Definition>
    products: Map<string, Product>

    constructor(defs: Definition[], products: Product[], loc: LineAndColumnInfo) {
        super(loc)
        this.defs = new Map()
        for (const d of defs) {
            this.defs.set(d.name, d)
        }
        this.products = new Map()
        for (const p of products) {
            this.products.set(p.name, p)
        }
    }

    override twist(): Twist {
        return Twist.obj("model",
            Twist.array("defs", Array.from(this.defs.values())),
            Twist.array("products", Array.from(this.products.values()))

        )
    }

}

export class Product extends AstNode {
    name: string
    values: Expression[]

    constructor(name: string, values: Expression[], loc: LineAndColumnInfo) {
        super(loc)
        this.name = name
        this.values = values
    }

    override twist(): Twist {
        return Twist.obj("Product",
            Twist.attr("name", this.name),
            Twist.array("values", this.values)
        )
    }

}

export class Param extends AstNode {
    name: string
    stype: StaticType

    constructor(name: string, stype: StaticType, loc: LineAndColumnInfo) {
        super(loc)
        this.name = name
        this.stype = stype
    }

    override twist(): Twist {
        return Twist.obj("Param",
            Twist.attr("name", this.name),
            Twist.value("type", this.stype)
        )
    }
}




