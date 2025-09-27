import * as cmd from "cmd-ts";
import { Directory, ExistingPath } from 'cmd-ts/batteries/fs';
import { SimplexParser } from "./grammar/actions";


const CommaList: cmd.Type<string, string[]> = {
    async from(str: string): Promise<string[]> {
        return str.split(",")
    },

}

const app = cmd.command({
    name: 'my-first-app',
    args: {
        source: cmd.positional({ type: ExistingPath, displayName: 'source-file' }),
        products: cmd.option({
            type: CommaList,
            long: "produce",
            description: "the products to generate"
        }),
        out: cmd.option({
            type: Directory,
            long: "out-dir",
            description: "the directory where outputs should be written"
        })
    },
    handler: ({ source, products, out }) => {
        const parser = new SimplexParser()
        const program = parser.parse_file(source)
        console.log(`Parsed program\n${program.twist().render(0)}\s`)
    },
})


cmd.run(app, process.argv.slice(2));
