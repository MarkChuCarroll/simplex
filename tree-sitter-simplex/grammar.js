module.exports = grammar({
    name: 'simplex',
  extras: ($) => [$.comment, $._whitespace],

    rules: {
        source_file: $ =>
            seq(
                field('defs', repeat1($.definition)),
                field('products', repeat1($.product))),
        definition: $ =>
            choice($.varDef, $.funDef, $.tupDef, $.methDef),
        varDef: $ =>
            seq(
                'val',
                field('name', $.id),
                ':',
                field('type', $.type),
                '=',
                field('value', $.expr)
            ),
        funDef: $ =>
            seq(
            'fun',
              field('name', $.id),
              '(', field('parameters', optional($.params)) ,')',
              ':', field('type', $.type), 'do',
              field('localDefs', repeat($.definition)),
              field('body', repeat($.expr)),
              'end'),
        tupDef: $ => seq(
            'tup', field('name', $.id), 'fields', '(', field('fields', $.params), ')'
            ),
        methDef: $ =>
            seq('meth', $.type, '->', $.id, '(', optional($.params), ')', ':', $.type, 'do',
            repeat1($.expr), 'end'),
        params: $ => seq(
            $.param, repeat(seq(',', $.param))
        ),
        param: $ =>
            seq($.id, ':', $.type),
        types: $ =>
            seq($.type, repeat(seq(',', $.type))),
      simpleType: $ => $.id,
      arrayType: $ => prec.left(1, seq('[', $.type, ']')),
      funType: $ => prec.left(2, seq('(', optional($.types), ')', ':', $.type)),
      methType: $ => prec.left(3, seq($.type, '->', '(', optional($.types), ')', ':', $.type)),
      type: $ =>
            choice(
              $.simpleType,
              $.arrayType,
              $.funType,
              $.methType),
      methodCall: $ => prec.left(9, seq($.expr, '->', $.id, '(', optional($.exprs), ')')),
      subscript: $ => prec(9, seq($.expr,'[' , $.expr, ']')),
      funCall: $ => prec(9, seq($.expr, '(', optional($.exprs), ')')),
      power: $ => prec.left(8,seq($.expr, $.expOp, $.expr)),
      multiply: $ => prec.left(7, seq($.expr, $.multOp, $.expr)),
      add: $ => prec.left(6, seq($.expr, $.addOp, $.expr)),
      compare: $ => prec.left(5,seq($.expr, $.compOp, $.expr)),
      logic: $ => prec.left(4, seq($.expr, $.logicOp, $.expr)),
      unary: $ => prec.right(9, seq($.unaryOp, $.expr)),
        paren: $ => prec.left(10, seq('(', $.expr, ')')),
        expr:$ =>
            choice(
              $.paren,
                prec(9, $._primary),
                prec(9, $._complex),
              $.methodCall,
              $.subscript,
                $.funCall,
              $.unary,
                $.power,
              $.multiply,
              $.add,
              $.compare,
              $.logic),
      cond: $ => seq('if', $.condClause,
        repeat(seq('elif',$.condClause)),
        'else', $.expr,
        'end'),
      lambda: $ => seq('lambda', ':', $.type,
      '(', $.params, ')', 'do', repeat1($.expr), 'end'),
      block: $ => seq('do', repeat1($.expr), 'end'),
      with: $ => seq('with', $.expr, 'do', repeat1($.expr), 'end'),
      letExpr: $ => seq('let', $.bindings, 'in', repeat1($.expr),
        'end'),
      loop: $ => seq('for', $.id, 'in', $.expr, 'do', repeat1($.expr), 'end' ),
        _complex: $ =>
            choice(
              $.letExpr,
                $.cond,
                $.loop,
                $.block,
                $.lambda,
              $.with,
            ),
      assignment: $ =>  seq($.id, optional(seq(':=', $.expr))),
      tuple: $ => seq('#', $.id, '(', $.exprs, ')'),
      array: $ => seq('[', $.exprs, ']'),
        _primary: $ =>
            choice(
              $.assignment,
              $.array,
              $.tuple,
              $.litint,
              $.litfloat,
              $.litstr,
              $.litbool),
      litbool: $ =>
        choice('true','false'),
        bindings: $ =>
            seq($.binding, repeat(seq(',', $.binding))),
        binding: $ =>
            seq($.id, ':', $.type, '=', $.expr),
        expOp: $ => '^',
        multOp: $ => choice('*', '/', '%'),
        addOp: $ => choice('+', '-'),
        compOp: $ =>
            choice('<', '>', '<=', '>=', '==', '!='),
        logicOp: $ => choice('and', 'or'),
        unaryOp: $ => choice('not', '-'),
        condClause: $ => seq($.expr, 'then', $.expr),
        product: $ => seq('produce', '(', $.id, ')', 'do',
            repeat1($.expr), 'end'),
        exprs: $ =>
            seq($.expr, repeat(seq(',', $.expr))),
        id: $ => /[A-Za-z_][A-Za-z_0-9]*/,
        litint: $ => /[0-9]+/,
        litfloat: $ => /[0-9]+\.[0-9]*([eE]-?[0-9]+)?/,
        litstr: $ => /".*"/,
      _whitespace: $ => /\s+/,
      comment: $ => seq('//', /.*/, '\n')
    },
});
