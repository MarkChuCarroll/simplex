import { LineAndColumnInfo, NonterminalNode } from 'ohm-js'
import { AstNode, Model, Param, Product } from '../ast'
import { DataDefinition, Definition, FunctionDefinition, OperationDefinition, VarDefinition } from '../def'
import { AssignmentExpr, BlockExpr, BooleanLiteralExpr, DataExpr, Expression, FieldExpr, FloatLiteralExpr, IntLiteralExpr, LambdaExpr, LetExpr, LValue, StringLiteralExpression, SubscriptExpr, VarRefExpr, VectorExpr } from '../expr/expr'
import grammar, { SimplexSemantics } from './grammar.ohm-bundle'
import { CondClause, CondExpression, ForExpr, WhileExpr } from '../expr/control'
import { ArithExpr } from '../expr/arith'
import { FuncallExpr, OpCallExpr } from '../expr/call_expr'
import { FunctionStaticType, NamedStaticType, OperationStaticType, StaticType, VectorStaticType } from '../value/static_types'
import { DynamicType } from '../value/types'
import { Twist } from '../twist'

import { readFileSync } from 'fs'


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


export class SimplexParser {
  private semantics: SimplexSemantics

  constructor() {
    this.semantics = grammar.createSemantics()
    this.semantics.addOperation<AstNode>('parse()', {
      Model(defs, products, _end) {
        const ps = products.children.map((prod: NonterminalNode) => prod.parse() as Product)
        return new Model(defs.children.map((d) => d.parse() as Definition), ps, this.source.getLineAndColumn())
      },
      Product(_a, id, _b, exprs, _) {
        const id_str = id.sourceString
        const expr_nodes = exprs.children.map((e) => e.parse() as Expression)
        return new Product(id_str, expr_nodes, this.source.getLineAndColumn())
      },
      OpDef(_op, target_type, _arrow, name, _lp, params, _rp, _colon, result_type, _do, body, _end) {
        const body_exprs = body.children.map((e) => e.parse() as Expression)
        return new OperationDefinition(name.sourceString, target_type.parse() as StaticType,
          (params.parse() as ListNode<Param>).nodes, result_type.parse() as StaticType, body_exprs,
          this.source.getLineAndColumn())
      },
      DataDef(_data, name, _lb, fields, _rb) {
        return new DataDefinition(name.sourceString, (fields.parse() as ListNode<Param>).nodes, this.source.getLineAndColumn())
      },
      VarDef(_var, name, _colon, optType, _eq, value) {
        var vartype: StaticType | null = null
        if (optType.numChildren === 1) {
          vartype = optType.child(0).parse() as StaticType
        }
        return new VarDefinition(name.sourceString, vartype, value.parse() as Expression, this.source.getLineAndColumn())

      },
      FunDef(_1, name, _2, params, _3, _4, return_type, _5, body, _6) {
        const param_nodes = (params.parse() as ListNode<Param>).nodes
        const body_nodes = body.children.map((item) => item.parse() as Expression)
        return new FunctionDefinition(name.sourceString, param_nodes, return_type.parse() as StaticType, body_nodes,
          this.source.getLineAndColumn())
      },
      Param(name, _colon, stype) {
        return new Param(name.sourceString, stype.parse() as StaticType, this.source.getLineAndColumn())
      },
      Params(params) {
        return new ListNode<Param>(params.asIteration().chilren().map((param: NonterminalNode) => param.parse() as Param),
          this.source.getLineAndColumn())
      },

      Expr_whileExpr(_w, cond, _d, body, _e) {
        const body_nodes = body.children.map((ex) => ex.parse() as Expression)
        return new WhileExpr(cond.parse() as Expression, body_nodes, this.source.getLineAndColumn())
      },
      Expr_blockExpr(_0, body, _2) {
        const body_nodes = body.children.map((ex) => ex.parse() as Expression)
        return new BlockExpr(body_nodes, this.source.getLineAndColumn())
      },
      Expr_forExpr(_for, name, _in, range, _do, body, _end) {
        const body_nodes = body.children.map((ex) => ex.parse() as Expression)
        const range_node = range.parse() as Expression
        return new ForExpr(name.sourceString, range_node, body_nodes, this.source.getLineAndColumn())
      },
      Expr_ifExpr(_if, cond_one, _elif, cond_rest, _else, else_clause) {
        const cond_clause_nodes: CondClause[] = []
        cond_clause_nodes.push(cond_one.parse() as CondClause)
        for (const cc of cond_rest.children) {
          cond_clause_nodes.push(cc.parse() as CondClause)
        }
        const else_node = else_clause.parse() as Expression
        return new CondExpression(cond_clause_nodes, else_node, this.source.getLineAndColumn())
      },
      Expr_lambdaExpr(_lam, _lp, params, _rp, _colon, rt, _do, body, _end) {
        const param_nodes = (params.parse() as ListNode<Param>).nodes
        const body_nodes = body.children.map((expr) => expr.parse() as Expression)
        return new LambdaExpr(param_nodes, rt.parse() as StaticType, body_nodes, this.source.getLineAndColumn())
      },
      Expr_letExpr(_let, name, _col, typeOpt, _eq, value) {
        var type_node: StaticType | null = null
        if (typeOpt.numChildren > 0) {
          type_node = typeOpt.child(0).parse() as StaticType
        }
        return new LetExpr(name.sourceString, type_node, value.parse() as Expression, this.source.getLineAndColumn())
      },

      Type_vector(_l, elt, _r) {
        return new VectorStaticType(elt.parse() as StaticType, this.source.getLineAndColumn())
      },
      Type_funType(_lp, args, _rp, _colon, rt) {
        const arg_types = args.asIteration().children.map((arg) => arg.parse() as StaticType)
        return new FunctionStaticType(arg_types, rt.parse() as StaticType, this.source.getLineAndColumn())
      },
      Type_opType(target_type, _arrow, _lp, args, _rp, _colon, rt) {
        const arg_types = args.asIteration().children.map((arg) => arg.parse() as StaticType)
        return new OperationStaticType(target_type.parse() as StaticType,
          arg_types, rt.parse() as StaticType, this.source.getLineAndColumn()
        )
      },
      Type_simple(name) {
        return new NamedStaticType(name.sourceString, this.source.getLineAndColumn())
      },
      Binary_binaryLogic(left, op, right) {
        return new ArithExpr(op.sourceString, [left.parse() as Expression, right.parse() as Expression],
          this.source.getLineAndColumn())
      },
      Compare_binaryCompare(l, op, r) {
        return new ArithExpr(op.sourceString, [l.parse() as Expression, r.parse() as Expression],
          this.source.getLineAndColumn())
      },
      Add_binaryAdd(l, op, r) {
        return new ArithExpr(op.sourceString, [l.parse() as Expression, r.parse() as Expression],
          this.source.getLineAndColumn())

      },
      Mult_binaryMult(l, op, r) {
        return new ArithExpr(op.sourceString, [l.parse() as Expression, r.parse() as Expression],
          this.source.getLineAndColumn())
      },
      Exponent_binaryExp(l, op, r) {
        return new ArithExpr(op.sourceString, [l.parse() as Expression, r.parse() as Expression],
          this.source.getLineAndColumn())
      },
      Unary_unaryOp(op, a) {
        return new ArithExpr(op.sourceString, [a.parse() as Expression],
          this.source.getLineAndColumn())
      },
      Arguments(args) {
        const arg_nodes = args.asIteration().children.map((item) => item.parse() as Expression)
        return new ListNode<Expression>(arg_nodes, this.source.getLineAndColumn())
      },
      Postfix_funCall(fun, _lp, args, _rp) {
        return new FuncallExpr(fun.parse() as Expression,
          (args.parse() as ListNode<Expression>).nodes, this.source.getLineAndColumn())
      },
      Postfix_opCall(tgt, _arrow, name, lp, args, rp) {
        return new OpCallExpr(tgt.parse() as Expression,
          name.sourceString,
          (args.parse() as ListNode<Expression>).nodes, this.source.getLineAndColumn())
      },
      Primary_vectorlit(_lb, values, rb) {
        const value_nodes = (values.parse() as ListNode<Expression>).nodes
        const stype = values[0].static_type()
        return new VectorExpr(stype, (values.parse() as ListNode<Expression>).nodes, this.source.getLineAndColumn())
      },
      Primary_assignment(lv, _ceq, value) {
        return new AssignmentExpr(lv.parse() as LValue, value.parse() as Expression, this.source.getLineAndColumn())
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
        return new StringLiteralExpression(arg0.sourceString, this.source.getLineAndColumn())
      },
      Primary_datalit(_lp, id, _colon, args, _rp) {
        const arg_nodes = args.asIteration().children.map((item) => item.parse() as Expression)
        return new DataExpr(id.sourceString, arg_nodes, this.source.getLineAndColumn())
      },
      LVal_fieldExpr(data_expr, _dot, fieldname) {
        return new FieldExpr(data_expr.parse() as Expression,
          fieldname.sourceString, this.source.getLineAndColumn())
      },
      LVal_idExpr(id) {
        return new VarRefExpr(id.sourceString, this.source.getLineAndColumn())
      },
      LVal_subscriptExpr(tgt, lb, sub, rb) {
        return new SubscriptExpr(tgt.parse() as Expression,
          sub.parse() as Expression, this.source.getLineAndColumn())
      },
      floatLiteral(arg0, arg1, arg2, arg3) {
        return new FloatLiteralExpr(Number.parseFloat(this.sourceString), this.source.getLineAndColumn())
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



