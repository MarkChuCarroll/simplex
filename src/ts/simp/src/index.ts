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



import * as cmd from "cmd-ts";
import { Directory, ExistingPath } from 'cmd-ts/batteries/fs'
import { SimplexParser } from "simplex/parser/actions";


const CommaList: cmd.Type<string, string[]> = {
  async from(str: string): Promise<string[]> {
    return str.split(",")
  },

}

const app = cmd.command({
  name: 'my-first-app',
  args: {
    source: cmd.positional({
      type: ExistingPath,
      displayName: 'source-file'
    }),
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
export { SimplexParser };
