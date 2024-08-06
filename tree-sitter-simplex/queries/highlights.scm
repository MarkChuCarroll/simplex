"tup" @definition
"val" @variable
"fun" @function
"meth" @definition
"let" @definer
"in" @keyword
"if"  @keyword
"elif" @keyword
"else" @keyword
"lambda" @definer
"with" @keyword
"produce" @definition
(litInt) @number
(litStr) @string
(comment) @comment

(funDef name: (id) @function)

(methDef name: (id) @function.method)
(methDef (params (param)@parameter))
(letExpr "let"@sc   "in"@sc  "}"@sc) @keyword.local


(id) @variable
(simpleType) @type
(arrayType) @type
(funType) @type
(methType) @type



