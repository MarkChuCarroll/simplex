import { Definition } from "./def"
import { DynamicType } from "./value/types"
import { NoneValue, Value } from "./value/value"


export class Environment {
    parent: Environment | null
    defs: Map<string, Definition>
    type_declarations: Map<string, DynamicType>
    values: Map<string, Value>

    constructor(parent: Environment | null) {
        this.parent = parent
        this.defs = new Map()
        this.type_declarations = new Map()
        this.values = new Map()
    }

    add_definition(d: Definition): void {
        this.defs.set(d.name, d)
    }

    get_definition(name: string): Definition {
        const result = this.defs.get(name)
        if (result) {
            return result
        } else if (this.parent != null) {
            return this.parent.get_definition(name)
        } else {
            throw new Error(`Definition not found for name ${name}`)
        }
    }

    declare_type_of(name: string, stype: DynamicType): void {
        this.type_declarations.set(name, stype)
    }

    get_declared_type_of(name: string): DynamicType {
        if (this.type_declarations.has(name)) {
            return this.type_declarations.get(name)!
        } else if (this.parent != null) {
            return this.parent.get_declared_type_of(name)
        } else {
            throw new Error(`Type declaration not found for ${name}`)
        }
    }

    set_value(name: string, value: Value): void {
        if (this.type_declarations.has(name)) {
            this.values.set(name, value)
        } else if (this.parent != null) {
            this.parent.set_value(name, value)
        } else {
            throw new Error(`Variable ${name} not found`)
        }
    }

    get_value(name: string): Value {
        if (this.type_declarations.has(name)) {
            return this.values.get(name) ?? NoneValue.get()
        } else if (this.parent != null) {
            return this.parent.get_value(name)
        } else {
            throw new Error(`Variable ${name} not found`)
        }
    }

    activate() {
        for (const [name, def] of this.defs) {
            def.install_in(this)
        }
    }

    static root: Environment = new Environment(null)
}

