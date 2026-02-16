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

import { readFileSync } from 'fs'
import { type LineAndColumnInfo, type Node, type NonterminalNode } from 'ohm-js'
import grammar, { type SimplexSemantics } from './simplex_grammar.ohm-bundle.d'
import { CondClause, CondExpression, ForExpr, WhileExpr } from 'simplex/expr/control'
import { ArithExpr } from 'simplex/expr/arith'
import { FuncallExpr, MethodCallExpr } from 'simplex/expr/call_expr'
import { AstNode, ImportDef, Model, Param, Product } from 'simplex/ast/ast'
import { Twist } from 'simplex/util/twist'
import { FunctionStaticType, MethodStaticType, NamedStaticType, VectorStaticType, type StaticType } from 'simplex/runtime/value/static_types'
import { BlockExpr, BooleanLiteralExpr, DataExpr, Expression, FieldExpr, FloatLiteralExpr, IntLiteralExpr, LambdaExpr, LetExpr, StringLiteralExpression, SubscriptExpr, VarRefExpr, VectorExpr } from 'simplex/expr/expr'
import { DataDefinition, Definition, FunctionDefinition, MethodDefinition, VarDefinition } from 'simplex/ast/def'


class ListNode<T extends AstNode> extends AstNode {
  nodes: T[]
  constructor(nodes: T[], loc: LineAndColumnInfo) {
    super(loc)
    this.nodes = nodes
  }

  override twist(): Twist {
    return Twist.array("List", this.nodes)
  }
}

class StaticTypeNode extends AstNode {
  static_type: StaticType

  constructor(loc: LineAndColumnInfo, t: StaticType) {
    super(loc)
    this.static_type = t
  }

  override twist(): Twist {
    throw new Error('Method not implemented.')
  }

}

const as_stype = (n: AstNode): StaticType => {
  if (n instanceof StaticTypeNode) {
    return n.static_type
  } else {
    throw new Error("Type error in parse: expected a type node")
  }
}

const as_expr = (n: AstNode): Expression => {
  if (n instanceof Expression) {
    return n
  } else {
    throw new Error("Type error in parse: expected an expression node")
  }
}


export class SimplexParser {
  private semantics: SimplexSemantics

  constructor() {
    this.semantics = grammar.createSemantics()
    this.semantics.addOperation<AstNode>('parse()', {

      Model(imports, body) {
        const defs: Definition[] = []
        const products: Product[] = []
        const imps = imports.children.map((i) => i.parse() as ImportDef)
        body.children.forEach((b: Node) => {
          const it = b.parse()
          if (it instanceof Definition) {
            defs.push(it)
          } else if (it instanceof Product) {
            products.push(it)
          } else {
            console.error("Invalid body element in module", it)
          }
        })
        return new Model(imps, defs, products, this.source.getLineAndColumn())
      },

      Product(
        _produce,
        id,
        _lc,
        exprs,
        _rc) {
        const id_str = id.sourceString
        const expr_nodes = exprs.children.map((e) => as_expr(e.parse()))
        return new Product(id_str, expr_nodes, this.source.getLineAndColumn())
      },

      MethDef(
        _meth,
        target_type,
        _arrow,
        name,
        _lp,
        params,
        _rp,
        _colon,
        result_type,
        _lb,
        body,
        _rb
      ) {
        const body_exprs = body.children.map((e) => as_expr(e.parse()))
        return new MethodDefinition(name.sourceString, as_stype(target_type.parse()),
          (params.parse() as ListNode<Param>).nodes, as_stype(result_type.parse()), body_exprs,
          this.source.getLineAndColumn())
      },

      DataDef(
        _data,
        name,
        _lb,
        fields,
        _rb
      ) {
        return new DataDefinition(
          name.sourceString,
          (fields.parse() as ListNode<Param>).nodes,
          this.source.getLineAndColumn())
      },

      VarDef(
        _let,
        name,
        _colon,
        optType,
        _eq,
        value
      ) {
        let vartype: StaticType | null = null
        if (optType.numChildren === 1) {
          vartype = as_stype(optType.child(0).parse())
        }
        return new VarDefinition(
          name.sourceString,
          vartype,
          as_expr(value.parse()),
          this.source.getLineAndColumn())
      },

      FunDef(
        _fun,
        name,
        _lp,
        params,
        _rp,
        _colon,
        return_type,
        _lb,
        body,
        _rb
      ) {
        const param_nodes = (params.parse() as ListNode<Param>).nodes
        const body_nodes = body.children.map((item) => as_expr(item.parse()))
        return new FunctionDefinition(
          name.sourceString,
          param_nodes,
          as_stype(return_type.parse()),
          body_nodes,
          this.source.getLineAndColumn())
      },

      Param(
        name,
        _colon,
        stype
      ) {
        return new Param(
          name.sourceString,
          as_stype(stype.parse()),
          this.source.getLineAndColumn())
      },

      Params(params) {
        return new ListNode<Param>(
          params.asIteration().chilren().map((param: NonterminalNode) =>
            param.parse() as Param),
          this.source.getLineAndColumn())
      },

      Expr_parenExpr(
        _lp,
        e,
        _rp
      ) {
        return e.parse()
      },

      Expr_funCallExpr(
        fun,
        _lp,
        args,
        _rp
      ) {
        return new FuncallExpr(
          as_expr(fun.parse()),
          (args.parse() as ListNode<Expression>).nodes,
          this.source.getLineAndColumn())
      },

      Expr_methCallExpr(
        tgt,
        _arrow,
        name,
        _lp,
        args,
        _rp
      ) {
        return new MethodCallExpr(
          as_expr(tgt.parse()),
          name.sourceString,
          (args.parse() as ListNode<Expression>).nodes,
          this.source.getLineAndColumn())
      },

      Expr_subscriptExpr(
        tgt,
        _lb,
        sub,
        _rb
      ) {
        return new SubscriptExpr(
          as_expr(tgt.parse()),
          as_expr(sub.parse()),
          this.source.getLineAndColumn())
      },

      Expr_unaryExpr(
        op,
        a
      ) {
        return new ArithExpr(
          op.sourceString,
          [as_expr(a.parse())],
          this.source.getLineAndColumn())
      },

      Expr_whileExpr(
        _w,
        cond,
        _d,
        body,
        _e
      ) {
        const body_nodes = body.children.map((ex) => as_expr(ex.parse()))
        return new WhileExpr(
          as_expr(cond.parse()),
          body_nodes,
          this.source.getLineAndColumn())
      },

      Expr_blockExpr(
        _lb,
        body,
        _rb) {
        const body_nodes = body.children.map((ex) => as_expr(ex.parse()))
        return new BlockExpr(body_nodes, this.source.getLineAndColumn())
      },

      Expr_forExpr(
        _for,
        name,
        _in,
        range,
        _do,
        body,
        _end
      ) {
        const body_nodes = body.children.map((ex) => as_expr(ex.parse()))
        const range_node = as_expr(range.parse())
        return new ForExpr(
          name.sourceString,
          range_node,
          body_nodes,
          this.source.getLineAndColumn())
      },

      Expr_ifExpr(
        _if,
        cond_one,
        _elif,
        cond_rest,
        _else,
        else_clause
      ) {
        const cond_clause_nodes: CondClause[] = []
        cond_clause_nodes.push(cond_one.parse() as CondClause)
        for (const cc of cond_rest.children) {
          cond_clause_nodes.push(cc.parse() as CondClause)
        }
        const else_node = as_expr(else_clause.parse())
        return new CondExpression(
          cond_clause_nodes,
          else_node,
          this.source.getLineAndColumn())
      },

      Expr_lambdaExpr(
        _lam,
        _lp,
        params,
        _rp,
        _colon,
        rt,
        _lb,
        body,
        _rb
      ) {
        const param_nodes = (params.parse() as ListNode<Param>).nodes
        const body_nodes = body.children.map((expr) => as_expr(expr.parse()))
        return new LambdaExpr(
          param_nodes,
          as_stype(rt.parse()),
          body_nodes,
          this.source.getLineAndColumn())
      },

      Expr_letExpr(
        _let,
        name,
        _col,
        typeOpt,
        _eq,
        value
      ) {
        let type_node: StaticType | null = null
        if (typeOpt.numChildren > 0) {
          type_node = as_stype(typeOpt.child(0).parse())
        }
        return new LetExpr(
          name.sourceString,
          type_node, as_expr(value.parse()),
          this.source.getLineAndColumn())
      },

      Binary_binaryLogic(
        left,
        op,
        right
      ) {
        return new ArithExpr(
          op.sourceString,
          [as_expr(left.parse()), as_expr(right.parse())],
          this.source.getLineAndColumn())
      },

      Compare_binaryCompare(
        l,
        op,
        r
      ) {
        return new ArithExpr(
          op.sourceString,
          [as_expr(l.parse()), as_expr(r.parse())],
          this.source.getLineAndColumn())
      },

      Add_binaryAdd(
        l,
        op,
        r) {
        return new ArithExpr(
          op.sourceString,
          [as_expr(l.parse()), as_expr(r.parse())],
          this.source.getLineAndColumn())
      },

      Mult_binaryMult(
        l,
        op,
        r
      ) {
        return new ArithExpr(
          op.sourceString,
          [as_expr(l.parse()), as_expr(r.parse())],
          this.source.getLineAndColumn())
      },

      Exponent_binaryExp(
        l,
        op,
        r
      ) {
        return new ArithExpr(
          op.sourceString,
          [as_expr(l.parse()), as_expr(r.parse())],
          this.source.getLineAndColumn())
      },

      Primary_vectorlit(
        _lb,
        values,
        _rb
      ) {
        const value_nodes = (values.parse() as ListNode<Expression>).nodes
        const stype = value_nodes[0]!.static_type()
        return new VectorExpr(stype, (values.parse() as ListNode<Expression>).nodes, this.source.getLineAndColumn())
      },

      Primary_binfalse(_) {
        return new BooleanLiteralExpr(false, this.source.getLineAndColumn())
      },

      Primary_bintrue(_) {
        return new BooleanLiteralExpr(true, this.source.getLineAndColumn())
      },

      Primary_intlit(arg0) {
        return new IntLiteralExpr(BigInt(arg0.sourceString),
          this.source.getLineAndColumn())
      },

      Primary_strlit(arg0) {
        return new StringLiteralExpression(
          arg0.sourceString,
          this.source.getLineAndColumn())
      },

      Primary_datalit(
        _lp,
        id,
        _colon,
        args,
        _rp
      ) {
        const arg_nodes = args.asIteration()
          .children.map((item) => as_expr(item.parse()))
        return new DataExpr(
          id.sourceString,
          arg_nodes,
          this.source.getLineAndColumn())
      },

      LVal_fieldExpr(
        data_expr,
        _dot,
        fieldname
      ) {
        return new FieldExpr(
          as_expr(data_expr.parse()),
          fieldname.sourceString,
          this.source.getLineAndColumn())
      },

      LVal_idExpr(id) {
        return new VarRefExpr(id.sourceString, this.source.getLineAndColumn())
      },

      floatLiteral(
        _arg0,
        _arg1,
        _arg2,
        _arg3
      ) {
        return new FloatLiteralExpr(
          Number.parseFloat(this.sourceString),
          this.source.getLineAndColumn())
      },


      Type_vector(
        _l,
        elt,
        _r
      ) {
        return new StaticTypeNode(
          this.source.getLineAndColumn(),
          new VectorStaticType(as_stype(elt.parse())))
      },

      Type_funType(
        _lp,
        args,
        _rp,
        _colon,
        rt
      ) {
        const arg_types = args.asIteration()
          .children.map((arg) => as_stype(arg.parse()))
        return new StaticTypeNode(
          this.source.getLineAndColumn(),
          new FunctionStaticType(
            arg_types,
            as_stype(rt.parse()))
      },

      Type_opType(
        target_type,
        _arrow,
        _lp,
        args,
        _rp,
        _colon,
        rt
      ) {
        const arg_types = args.asIteration().children.map((arg) =>
          as_stype(arg.parse()))
        return new StaticTypeNode(
          this.source.getLineAndColumn(),
          new MethodStaticType(as_stype(target_type.parse()),
            arg_types, as_stype(rt.parse())))
      },

      Type_simple(name) {
        return new StaticTypeNode(this.source.getLineAndColumn(),
          new NamedStaticType(name.sourceString))
      },

      Arguments(args) {
        const arg_nodes = args.asIteration()
          .children.map((item) => as_expr(item.parse()))
        return new ListNode<Expression>(arg_nodes, this.source.getLineAndColumn())
      },

    })
  }

  parse_file(path: string): Model {
    const text = readFileSync(path)
    let m = grammar.match(text.toString())
    if (m.failed()) {
      console.error("Syntax error: ", m)
      throw new Error(`Syntax error: ${m.message}`)
    }
    const s = this.semantics(m)
    const result = s.parse()
    return result
  }
}

