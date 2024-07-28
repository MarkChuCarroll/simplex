module.exports = grammar({
    name: 'simplex',

    rules: {
        // TODO: add the actual grammar rules
        source_file: $ =>
            seq(
                repeat($.definition),
                repeat($.product)),
        definition: $ =>
            choice($.vardef, $.fundef, $.tupdef),
        vardef: $ =>
            seq(
                'var',
                $.id,
                optional(seq(':', $.type)),
                '=',
                $.expr
            ),
        fundef: $ =>
            seq(
            'fun',
            $.id, '(', optional($.params) ,')', optional(seq(':', $.type)), 'do',
            repeat($.definition),
            repeat($.expr),
            'end'),
        tupdef: $ => seq(
            'tup', $.id, '(', $.params, ')'
            ),

        params: $ => seq(
            $.param, repeat(seq(',', $.param))
        ),
        param: $ =>
            seq($.id, optional(seq(':', $.type))),
        type: $ =>
            choice($.id, seq('[', $.type, ']')),
        expr:$ =>
            choice(
                seq('(', $.expr, ')'),
                $.primary,
                $.complex,
                seq($.expr, '->', $.id, '(', $.exprs, ')'),
                seq($.expr,'[' , $.expr, ']'),
                seq($.expr, '(', optional($.exprs), ')'),
                seq($.unaryop, $.expr),
                seq($.expr, $.expop, $.expr),
                seq($.expr, $.multop, $.expr),
                seq($.expr, $.addop, $.expr),
                seq($.expr, $.compop, $.expr),
                seq($.expr, $.logicop, $.expr)),
        complex: $ =>
            choice(
                seq('let', $.bindings, 'in', repeat1($.expr),
                    'end'),
                seq('if', $.condClause,
                    repeat(seq('elif',$.condClause)),
                    'else', $.expr,
                    'end'),
                seq('for', $.id, 'in', $.expr, 'do', repeat1($.expr), 'end' ),
                seq('do', repeat1($.expr), 'end'),
                seq('update', $.expr, 'set', $.updates),
                seq('lambda', optional(seq(':', $.type)),
                    '(', $.params, ')', 'do', repeat1($.expr), 'end'),
                seq('with', $.expr, 'do', repeat1($.expr), 'end')
            ),
        primary: $ =>
            choice($.id,
                seq('[', $.exprs, ']'),
                seq('#', $.id, '(', $.exprs, ')'),
                $.litint,
                $.litfloat,
                $.litstr,
                'true',
                'false'),
        updates: $ =>
            seq($.update, repeat(seq(',', $.update))),
        update: $ =>
            seq($.id, '=', $.expr),
        bindings: $ =>
            seq($.binding, repeat(seq(',', $.binding))),
        binding: $ =>
            seq($.id, optional(seq(':', $.type)), '=', $.expr),
        expop: $ => '^',
        multop: $ => choice('*', '/', '%'),
        addop: $ => choice('+', '-'),
        compop: $ =>
            choice('<', '>', '<=', '>=', '==', '!='),
        logicop: $ => choice('and', 'or'),
        unaryop: $ => choice('not', '-'),
        condClause: $ => seq($.expr, 'then', $.expr),
        product: $ => seq('produce', '(', $.id, ')', 'do',
            repeat1($.expr), 'end'),
        exprs: $ =>
            seq($.expr, repeat(seq(',', $.expr))),
        id: $ => /[A-Za-z_][A-Za-z_0-9]*/,
        litint: $ => /[0-9]+/,
        litfloat: $ => /[0-9]+\.[0-9]*([eE]-?[0-9]+)?/,
        litstr: $ => /'.*'/,
    }
});
