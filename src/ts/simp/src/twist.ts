
export interface Twistable {
    twist(): Twist
}

export abstract class Twist implements Twistable {

    static obj(name: string, ...children: Twistable[]): Twist {
        return new ObjNode(name,
            ...children.map(it => it.twist())
        )
    }

    static array(name: string, children: Twistable[]): Twist {
        return new ArrayNode(name,
            children.map(it => it.twist())
        )
    }

    static value(name: string, value: Twistable | null): Twist {
        return new ValueNode(name, value?.twist())
    }

    static attr(name: string, value: string): Twist {
        return new AttrNode(name, value)
    }

    abstract render(indent: number): string

    i(n: number): string {
        return "   ".repeat(n)
    }

    twist(): Twist {
        return this
    }
}

class ObjNode extends Twist {
    name: string
    children: Twist[]

    constructor(name: string, ...children: Twist[]) {
        super()
        this.name = name
        this.children = children
    }

    override render(indent: number): string {
        var result = ""
        result += this.i(indent)
        result += `(obj ${this.name}\n`
        result += this.children.map(it =>
            it.render(indent + 1)
        ).filter(it => it.length != 0).join("\n")
        result += ")"
        return result
    }
}

class ArrayNode extends Twist {
    name: string
    children: Twist[]

    constructor(name: string, children: Twist[]) {
        super()
        this.name = name
        this.children = children
    }

    override render(indent: number): string {
        var result = ""
        result += this.i(indent)
        result += `[arr ${this.name}\n`
        result += this.children.map(it =>
            it.render(indent + 1)
        ).filter(it => it.length != 0).join("\n")
        result += "]"
        return result
    }

}

class ValueNode extends Twist {
    name: String
    value: Twist | undefined

    constructor(name: string, value: Twist | undefined) {
        super()
        this.name = name
        this.value = value
    }

    render(indent: number): string {
        if (this.value) {
            var result = ""
            result += this.i(indent)
            result += `{val ${this.name}\n`
            result += this.value.render(indent + 1)
            result += "}"
            return result
        } else {
            return ""
        }
    }
}

class AttrNode extends Twist {
    name: String
    value: String

    constructor(name: string, value: string) {
        super()
        this.name = name
        this.value = value
    }

    render(indent: number): string {
        var result = ""
        result += this.i(indent)
        result += `${this.name} = "${this.value}"`
        return result
    }
}



export function test() {
    const t = Twist.obj("foo",
        Twist.obj("bar",
            Twist.array("lots", [
                Twist.attr("one", "1"),
                Twist.attr("two", "2"),
                Twist.attr("three", "3")
            ]),
            Twist.value("whoops",
                Twist.attr("nope", "yup")
            )))

    const s = t.render(0)

    console.log(s)

}