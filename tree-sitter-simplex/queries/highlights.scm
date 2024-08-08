"tup" @definition
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

(varDef name:(id) @variable)

(funDef name: (id) @function)

(methDef name: (id) @function.method)
(methDef (params (param)@parameter))


(id) @variable
(simpleType) @type
(arrayType) @type
(funType) @type
(methType) @type



