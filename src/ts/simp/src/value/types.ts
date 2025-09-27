import { DataDefinition, OperationDefinition } from '../def'
import { Environment } from '../env'
import { LineAndColumnInfo } from 'ohm-js'
import { FunctionStaticType, NamedStaticType, OperationStaticType, StaticType, VectorStaticType } from './static_types'
import { BooleanValue, BuiltinOperationValue, FloatValue, FunctionValue, IntValue, OperationValue, SimplexOperationValue, StringValue, Value, VectorValue } from './value'


export function fakeLocation(): LineAndColumnInfo {
    return {
        offset: -1,
        lineNum: -1,
        colNum: -1,
        line: "builtin",
        prevLine: "none",
        nextLine: "none",
        toString(...ranges) {
            return "none"

        }
    }
}


export abstract class DynamicType {

    ops: Map<String, OperationValue> = new Map()


    register_simplex_op(def: OperationDefinition) {
        const ov = new SimplexOperationValue(def, Environment.root)
        this.ops.set(ov.name, ov)
    }

    register_builtin_op(op: BuiltinOperationValue) {
        this.ops.set(op.name, op)
    }

    get_op(name: string): OperationValue {
        const op = this.ops.get(name)
        if (!op) {
            throw new Error(`Operation ${name} not found in type ${this.toString()}`)
        }
        return op
    }


    abstract toString(): string
    abstract static_type(): StaticType
    abstract is_truthy(v: Value): boolean
}

export abstract class BuiltinDynamicType extends DynamicType {
    name: string
    operations: OperationValue[]
    functions: FunctionValue[]

    constructor(name: string,
        operations: OperationValue[],
        functions: FunctionValue[]) {
        super()
        this.name = name
        this.operations = operations
        this.functions = functions
    }

    override toString(): string {
        return this.name
    }
}

export class NoneType extends BuiltinDynamicType {
    name: string = "None"

    private constructor() {
        super("None",
            [],
            []
        )
    }

    override is_truthy(v: Value): boolean {
        return false
    }

    override static_type(): StaticType {
        return new NamedStaticType("None", fakeLocation())
    }

    private static it: NoneType = new NoneType()
    static get(): NoneType {
        return this.it
    }

}

export class IntType extends BuiltinDynamicType {

    private constructor() {
        super("Int", [], [])
    }

    override is_truthy(v: Value): boolean {
        if (v instanceof IntValue) {
            return v.int_value != 0n
        } else {
            return false
        }
    }


    private static it: IntType = new IntType()
    static get(): IntType {
        return this.it
    }

    override static_type(): StaticType {
        throw new NamedStaticType("Int", fakeLocation())
    }

}


export class FloatType extends BuiltinDynamicType {

    private constructor() {
        super("Float", [], [])
    }

    override is_truthy(v: Value): boolean {
        if (v instanceof FloatValue) {
            return v.float_value != 0.0 && !Number.isNaN(v.float_value)
        } else {
            return false
        }
    }

    private static it: FloatType = new FloatType()
    static get(): FloatType {
        return this.it
    }

    override static_type(): StaticType {
        return new NamedStaticType("Float", fakeLocation())
    }
}


export class StringType extends BuiltinDynamicType {
    private constructor() {
        super("String", [], [])
    }

    override is_truthy(v: Value): boolean {
        if (v instanceof StringValue) {
            return v.str_value.length != 0
        } else {
            return false
        }
    }

    private static it: StringType = new StringType()
    static get(): StringType {
        return this.it
    }

    override static_type(): StaticType {
        return new NamedStaticType("String", fakeLocation())
    }
}


export class DataType extends DynamicType {

    def: DataDefinition

    constructor(def: DataDefinition) {
        super()
        this.def = def
    }

    override toString(): string {
        return this.def.name
    }

    index_of_field(fn: string): number {
        for (var i = 0; i < this.def.fields.length; i++) {
            if (this.def.fields[i].name === fn) {
                return i
            }
        }
        throw new Error(`Data type ${this.def.name} does not contain a field named ${fn}`)
    }

    override is_truthy(v: Value): boolean {
        return true
    }

    override static_type(): StaticType {
        return new NamedStaticType(this.def.name, fakeLocation())
    }

}



export class BooleanType extends BuiltinDynamicType {
    private constructor() {
        super("Boolean", [], [])
    }

    override toString(): string {
        return "Boolean"
    }

    override is_truthy(v: Value): boolean {
        if (v instanceof BooleanValue) {
            return v.bool_val
        } else {
            return false
        }
    }

    override static_type(): StaticType {
        return new NamedStaticType("Boolean", fakeLocation())
    }

    private static it: BooleanType = new BooleanType()
    static get(): BooleanType {
        return this.it
    }

}




export class OperationType extends DynamicType {
    target: DynamicType
    params: DynamicType[]
    return_type: DynamicType

    constructor(target: DynamicType, params: DynamicType[], return_type: DynamicType) {
        super()
        this.target = target
        this.params = params
        this.return_type = return_type
    }

    get_param_types(): DynamicType[] {
        return this.params
    }

    get_return_type(): DynamicType {
        return this.return_type
    }

    override toString(): string {
        return `${this.target.toString}(${this.get_param_types().map((item) => item.toString()).join(",")}):${this.return_type.toString()}`
    }

    override static_type(): StaticType {
        return new OperationStaticType(this.target.static_type(), this.params.map((it) => it.static_type()), this.return_type.static_type(),
            fakeLocation())
    }

    override is_truthy(v: Value): boolean {
        return true
    }
}




export class FunctionType extends DynamicType {
    params: DynamicType[]
    return_type: DynamicType

    constructor(params: DynamicType[], return_type: DynamicType) {
        super()
        this.params = params
        this.return_type = return_type
    }

    get_param_types(): DynamicType[] {
        return this.params
    }

    get_return_type(): DynamicType {
        return this.return_type
    }

    override toString(): string {
        const ps = this.params.map((item) => item.toString()).join(",")
        return `(${ps}):${this.return_type.toString()}`

    }

    override static_type(): StaticType {
        return new FunctionStaticType(this.params.map((it) => it.static_type()), this.return_type.static_type(),
            fakeLocation())
    }


    override is_truthy(v: Value): boolean {
        return true
    }
}


export class VectorType extends DynamicType {
    element_type: DynamicType

    constructor(element: DynamicType) {
        super()
        this.element_type = element
    }

    override toString(): string {
        return `[${this.element_type.toString()}]`
    }

    override is_truthy(v: Value): boolean {
        if (v instanceof VectorValue) {
            return v.values.length > 0
        } else {
            return false
        }
    }

    override static_type(): StaticType {
        return new VectorStaticType(this.element_type.static_type(), fakeLocation())
    }
}


export class TypeRegistry {
    private types: Map<String, DynamicType> = new Map()
    intType = IntType.get()
    floatType = FloatType.get()
    noneType = NoneType.get()
    stringType = StringType.get()
    boolType = BooleanType.get()


    private constructor() {
        this.addBuiltin(IntType.get())
        this.addBuiltin(StringType.get())
        this.addBuiltin(FloatType.get())
        this.addBuiltin(NoneType.get())
        this.addBuiltin(BooleanType.get())

        var s: LineAndColumnInfo
    }

    addBuiltin(bt: BuiltinDynamicType) {
        this.types.set(bt.name, bt)
    }

    dataType(def: DataDefinition): DataType {
        var typ = this.types.get(def.name)
        if (!typ) {
            typ = new DataType(def)
            this.types.set(def.name, typ)
        }
        return typ as DataType
    }


    operationType(target: DynamicType, params: DynamicType[], result: DynamicType): OperationType {
        const op_type_str = this.op_type_str(target, params, result)
        var typ = this.types.get(op_type_str)
        if (!typ) {
            typ = new OperationType(target, params, result)
            this.types.set(op_type_str, typ)
        } else if (!(typ instanceof OperationType)) {
            throw new Error(`Operation type name ${op_type_str} isn't an operation type`)
        }
        return typ as OperationType
    }

    functionType(params: DynamicType[], result: DynamicType): FunctionType {
        const fun_type_str = this.fun_type_str(params, result)
        var typ = this.types.get(fun_type_str)
        if (!typ) {
            typ = new FunctionType(params, result)
            this.types.set(fun_type_str, typ)
        } else if (!(typ instanceof OperationType)) {
            throw new Error(`Operation type name ${fun_type_str} isn't an operation type`)
        }
        return typ as OperationType
    }

    vectorType(element: DynamicType): VectorType {
        const vec_type_str = this.vec_type_str(element)
        var typ = this.types.get(vec_type_str)
        if (!typ) {
            typ = new VectorType(element)
            this.types.set(vec_type_str, typ)
        } else if (!(typ instanceof VectorType)) {
            throw new Error(`Vector type name ${vec_type_str} isn't a vector type`)
        }
        return typ as VectorType
    }

    vec_type_str(element: DynamicType): string {
        return `[${element.toString()}]`
    }

    op_type_str(target: DynamicType, params: DynamicType[], result: DynamicType): string {
        const param_str = params.map((item) => item.toString()).join(", ")
        return `${target.toString()}->(${param_str}):${result.toString()}`
    }

    fun_type_str(params: DynamicType[], result: DynamicType): string {
        const param_str = params.map((item) => item.toString()).join(", ")
        return `(${param_str}):${result.toString()}`
    }

    get_value_type(st: StaticType): DynamicType {
        return this.get_by_str(st.toString())
    }

    get_by_str(typeStr: string): DynamicType {
        const result = this.types.get(typeStr)
        if (result) {
            return result
        } else {
            throw new Error(`Type ${typeStr} not found`)
        }
    }

    private static it: TypeRegistry = new TypeRegistry()
    static get(): TypeRegistry { return this.it }
}


