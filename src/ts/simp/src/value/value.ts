import { FunctionDefinition, OperationDefinition } from "../def"
import { Environment } from "../env"
import { Expression, LambdaExpr } from "../expr/expr"
import { BooleanType, DataType, FloatType, FunctionType, IntType, NoneType, OperationType, StringType, DynamicType, TypeRegistry, VectorType } from "./types"

export abstract class Value {
    abstract get_type(): DynamicType
}

export class NoneValue extends Value {


    private constructor() {
        super()
    }

    override get_type(): DynamicType {
        return NoneType.get()
        //
    }

    private static it: NoneValue = new NoneValue()
    static get(): NoneValue {
        return this.it
    }
}

export class IntValue extends Value {
    int_value: bigint

    constructor(iv: bigint) {
        super()
        this.int_value = iv
    }

    override get_type(): DynamicType {
        return IntType.get()
    }
}

export class FloatValue extends Value {
    float_value: number

    constructor(fv: number) {
        super()
        this.float_value = fv
    }

    override get_type(): DynamicType {
        return FloatType.get()
    }
}



export class StringValue extends Value {
    str_value: string

    constructor(sv: string) {
        super()
        this.str_value = sv
    }

    override get_type(): DynamicType {
        return StringType.get()
    }
}

export class DataValue extends Value {
    type_name: string
    value_type: DataType
    fields: Value[]

    constructor(data_type: DataType, fieldValues: Value[]) {
        super()
        this.type_name = data_type.def.name
        this.value_type = data_type
        this.fields = fieldValues
        const type_fields = data_type.def.fields
        if (this.fields.length != type_fields.length) {
            throw new Error(`Data type ${this.type_name} expected ${type_fields.length} fields, but received ${this.fields.length}`)
        }
        for (var i = 0; i < this.fields.length; i++) {
            const field_name = type_fields[i].name
            const field_def_type = type_fields[i].stype
            const field_val_type = this.fields[i].get_type()
            if (field_val_type !== TypeRegistry.get().get_value_type(field_def_type)) {
                throw new Error(`Data type ${this.type_name} expected a value of type ${field_def_type} for field ${field_name}, but received ${field_val_type}`)
            }
        }
    }

    override get_type(): DynamicType {
        return this.value_type
    }
}

export class BooleanValue extends Value {
    bool_val: boolean

    constructor(v: boolean) {
        super()
        this.bool_val = v
    }

    override get_type(): DynamicType {
        return BooleanType.get()
    }
}

export abstract class OperationValue extends Value {
    name: string
    op_type: OperationType
    param_names: string[]

    constructor(name: string, op_type: OperationType, param_names: string[]) {
        super()
        this.name = name
        this.op_type = op_type
        this.param_names = param_names
    }

    override get_type(): DynamicType {
        return this.op_type
    }

    abstract apply(target_value: Value, args: Value[], in_env: Environment): Value
}

export class BuiltinOperationValue extends OperationValue {
    impl: (t: Value, args: Value[], e: Environment) => Value

    constructor(name: string, op_type: OperationType, param_names: string[], impl: (t: Value, args: Value[], e: Environment) => Value) {
        super(name, op_type, param_names)
        this.impl = impl
    }

    override apply(target: Value, args: Value[], in_env: Environment): Value {
        return this.impl(target, args, in_env)
    }
}

export class SimplexOperationValue extends OperationValue {
    definition: OperationDefinition
    static_scope: Environment
    body: Expression[]

    constructor(def: OperationDefinition, in_env: Environment) {
        const tt = TypeRegistry.get().get_value_type(def.target_type)
        const op_type = TypeRegistry.get().operationType(tt, def.params.map((item) => TypeRegistry.get().get_value_type(item.stype)),
            TypeRegistry.get().get_value_type(def.return_type))
        super(def.name, op_type, def.params.map((item) => item.name))
        this.definition = def
        this.static_scope = in_env
        this.body = def.body
    }

    override apply(target: Value, args: Value[], in_env: Environment): Value {
        const local_env = new Environment(in_env)
        local_env.declare_type_of("self", target.get_type())
        local_env.set_value("self", target)
        if (args.length !== this.definition.params.length) {
            throw new Error(`Parameter count mismatch: recieved ${args.length}, but expected ${this.definition.params.length}`)
        }
        for (var i = 0; i < args.length; i++) {
            let a = args[i]
            let p = this.definition.params[i]
            if (a.get_type() !== TypeRegistry.get().get_value_type(p.stype)) {
                throw new Error(`Parameter type mismatch: param ${p.name} expected a ${p.stype}, but received a ${a.get_type()}`)
            }
            local_env.declare_type_of(p.name, TypeRegistry.get().get_value_type(p.stype))
            local_env.set_value(p.name, a)
        }
        var result: Value = NoneValue.get()
        for (const expr of this.body) {
            result = expr.evaluate(local_env)
        }
        return result
    }
}

export abstract class FunctionValue extends Value {
    name: string | undefined
    fun_type: FunctionType
    param_names: string[]

    constructor(fun_type: FunctionType, param_names: string[], name?: string) {
        super()
        this.name = name
        this.fun_type = fun_type
        this.param_names = param_names
    }

    override get_type(): DynamicType {
        return this.fun_type
    }

    abstract apply(args: Value[], in_env: Environment): Value
}

export class BuiltinFunctionValue extends FunctionValue {
    impl: (args: Value[], e: Environment) => Value

    constructor(name: string, op_type: FunctionType, param_names: string[], impl: (args: Value[], e: Environment) => Value) {
        super(op_type, param_names, name)
        this.impl = impl
    }

    override apply(args: Value[], in_env: Environment): Value {
        return this.impl(args, in_env)
    }

}

export class SimplexFunctionValue extends FunctionValue {
    def: FunctionDefinition | LambdaExpr
    static_scope: Environment
    body: Expression[]

    constructor(def: FunctionDefinition | LambdaExpr, in_env: Environment) {
        const fun_type = TypeRegistry.get().functionType(def.params.map((item) => TypeRegistry.get().get_value_type(item.stype)),
            TypeRegistry.get().get_value_type(def.return_type))
        super(fun_type, def.params.map((item) => item.name))
        this.def = def
        this.static_scope = in_env
        this.body = def.body
    }

    override apply(args: Value[], in_env: Environment): Value {
        const local_env = new Environment(in_env)
        if (args.length !== this.def.params.length) {
            throw new Error(`Parameter count mismatch: recieved ${args.length}, but expected ${this.def.params.length}`)
        }
        for (var i = 0; i < args.length; i++) {
            let a = args[i]
            let p = this.def.params[i]
            if (a.get_type() !== TypeRegistry.get().get_value_type(p.stype)) {
                throw new Error(`Parameter type mismatch: param ${p.name} expected a ${p.stype}, but received a ${a.get_type()}`)
            }
            local_env.declare_type_of(p.name, TypeRegistry.get().get_value_type(p.stype))
            local_env.set_value(p.name, a)
        }
        var result: Value = NoneValue.get()
        for (const expr of this.body) {
            result = expr.evaluate(local_env)
        }
        return result
    }
}


export class VectorValue extends Value {
    stype: VectorType
    values: Value[]

    constructor(element_type: DynamicType, values: Value[]) {
        super()
        this.stype = TypeRegistry.get().vectorType(element_type)
        this.values = values
    }

    override get_type(): DynamicType {
        return this.stype
    }
}
