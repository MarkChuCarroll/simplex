module.exports = grammar({
  name: 'simplex',
  extras: ($) => [$.comment, $._whitespace],
  rules: {
    source_file: $ => seq(
      field('defs', repeat1($.definition)),
      field('products', repeat1($.product))),
    definition: $ => choice(
      $.varDef,
      $.funDef,
      $.dataDef,
      $.methDef),
    varDef: $ => seq(
      'let',
      field('name', $.id),
      optional(seq(
        ':',
          field('type', $._type)
        )
      ),
      '=',
      field('value', $._expr)
    ),
    funDef: $ => seq(
      'fun',
      field('name', $.id),
      '(',
      field('parameters', optional($.params))
      ,')',
      ':',
      field('type', $._type),
      '{',
      field('localDefs', repeat($.funDef)),
      field('body', repeat($._expr)),
      '}'
    ),
    dataDef: $ => seq(
      'data',
      field('name', $.id),
      '{',
      field('fields', $.params),
      '}'
    ),
    methDef: $ => seq(
      'meth',
      $._type,
      '->',
      field('name', $.id),
      '(',
      optional($.params),
      ')',
      ':',
      $._type,
      '{',
      repeat1($._expr),
      '}'
    ),
    params: $ => seq(
      $.param,
      repeat(seq(
        ',',
        $.param
      ))
    ),
    param: $ => seq(
      $.id,
      ':',
      $._type
    ),
    types: $ => seq(
      $._type,
      repeat(seq(
        ',',
        $._type
      ))
    ),
    simpleType: $ => field('name', $.id),
    arrayType: $ => prec.left(1, seq(
      '[',
      $._type,
      ']'
    )),
    funType: $ => prec.left(2, seq(
      '(',
      optional($.types),
      ')',
      ':',
      $._type
    )),
    methType: $ => prec.left(3, seq(
      $._type,
      '->',
      '(',
      optional($.types),
      ')',
      ':',
      $._type
    )),
    _type: $ => choice(
      $.simpleType,
      $.arrayType,
      $.funType,
      $.methType
    ),
    methodCall: $ => prec.left(9, seq(
      $._expr,
      '->',
      $.id,
      '(',
      optional($.exprs),
      ')'
    )),
    subscript: $ => prec(9, seq(
      $._expr,
      '[',
      $._expr,
      ']'
    )),
    funCall: $ => prec(9, seq(
      $._expr,
      '(',
      optional($.exprs),
      ')'
    )),
    power: $ => prec.left(8, seq(
      $._expr,
      $.expOp,
      $._expr
    )),
    multiply: $ => prec.left(7, seq(
      $._expr,
      $.multOp,
      $._expr
    )),
    add: $ => prec.left(6, seq(
      $._expr,
      $.addOp,
      $._expr
    )),
    compare: $ => prec.left(5, seq(
      $._expr,
      $.compOp,
      $._expr
    )),
    logic: $ => prec.left(4, seq(
      $._expr,
      $.logicOp,
      $._expr
    )),
    unary: $ => prec.right(9, seq(
      $.unaryOp,
      $._expr
    )),
    paren: $ => prec.left(10, seq(
      '(',
      $._expr,
      ')'
    )),
    field: $ => prec.left(8, seq(
      $._expr,
      '.',
      $.id
    )),
    update: $ => prec.left(9, seq(
      $._expr,
      '.',
      $.id,
      ':=',
      $._expr
    )),
    _expr:$ => choice(
      $.paren,
      prec(9, $._primary),
      prec(9, $._complex),
      $.methodCall,
      $.subscript,
      $.field,
      $.update,
      $.funCall,
      $.unary,
      $.power,
      $.multiply,
      $.add,
      $.compare,
      $.logic),
    cond: $ => seq(
      'if',
      $.condClause,
      repeat(seq(
        'elif',
        $.condClause
      )),
      'else',
      $._expr,
    ),
    lambda: $ => seq(
      'lambda',
      '(',
      $.params,
      ')',
       ':',
      $._type,
      '{',
      repeat1($._expr),
      '}'),
    block: $ => seq(
      '{',
      repeat1($._expr),
      '}'
    ),
    letExpr: $ => seq(
      'let',
      $.id,
      optional(seq(
        ':',
          $._type
      )),
      '=',
      $._expr
    ),
    loop: $ => seq(
      'for',
      field("index", $.id),
      'in',
      field("range", $._expr),
      '{',
      field("body", repeat1($._expr)),
      '}'
    ),
    _complex: $ => choice(
      $.letExpr,
      $.cond,
      $.loop,
      $.block,
      $.lambda,
      $.while
    ),
    while: $ => seq(
        'while', field('cond',  $._expr),  '{',
        field('body', repeat1($._expr)), '}'
    ),
    assignment: $ => seq(
      $.id,
      ':=',
      $._expr
    ),
    ref: $ => $.id,
    data: $ => seq(
      '#',
      $.id,
      '(',
      $.exprs,
      ')'
    ),
    array: $ => seq(
      '[',
      $.exprs,
      ']'
    ),
    _primary: $ => choice(
      $.assignment,
      $.ref,
      $.array,
      $.data,
      $.litInt,
      $.litFloat,
      $.litStr,
      $.litBool
    ),
    litBool: $ => choice(
      'true',
      'false'
    ),
    expOp: $ => '^',
    multOp: $ => choice(
      '*',
      '/',
      '%'
    ),
    addOp: $ => choice(
      '+',
      '-'
    ),
    compOp: $ => choice(
      '<',
      '>',
      '<=',
      '>=',
      '==',
      '!='
    ),
    logicOp: $ => choice(
      'and',
      'or'
    ),
    unaryOp: $ => choice(
      'not',
      '-'
    ),
    condClause: $ => seq(
      '(',
      $._expr,
      ')',
      '{',
      repeat1($._expr),
      '}'
    ),
    product: $ => seq(
      'produce',
      '(',
      $.litStr,
      ')',
      '{',
      repeat1($._expr),
      '}'
    ),
    exprs: $ => seq(
      $._expr,
      repeat(seq(
        ',',
        $._expr
      ))
    ),
    id: $ => /[A-Za-z_][A-Za-z_0-9]*/,
    litInt: $ => /[0-9]+/,
    litFloat: $ => /[0-9]+\.[0-9]*([eE]-?[0-9]+)?/,
    litStr: $ => /".*"/,
    _whitespace: $ => /\s+/,
    comment: $ => seq(
      '//',
      /.*/,
      '\n'
    )
  }
});
