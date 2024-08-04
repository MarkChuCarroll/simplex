"tup" @definition
"val" @variable
"fun" @function
"do" @keyword
"end" @keyword
"meth" @definition
"let" @definer
"in" @keyword
"if"  @keyword
"elif" @keyword
"else" @keyword
"lambda" @definer
"with" @keyword
"produce" @definition
(litint) @number
(litstr) @string
(comment) @comment

(funDef name: (id) @function)

(methDef name: (id) @function.method)
(methDef (params (param)@parameter))
(letExpr "let"@sc   "in"@sc  "end"@sc) @keyword.local


(id) @variable
(simpleType) @type
(arrayType) @type
(funType) @type
(methType) @type



