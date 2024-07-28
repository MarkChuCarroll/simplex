/*
 * Copyright 2024 Mark C. Chu-Carroll
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
package org.goodmath.simplex.parser

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeProperty
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.tree.TerminalNode
import org.goodmath.simplex.ast.ArrayExpr
import org.goodmath.simplex.ast.ArrayType
import org.goodmath.simplex.ast.Binding
import org.goodmath.simplex.ast.BlockExpr
import org.goodmath.simplex.ast.CondExpr
import org.goodmath.simplex.ast.Condition
import org.goodmath.simplex.ast.Definition
import org.goodmath.simplex.ast.Expr
import org.goodmath.simplex.ast.FunCallExpr
import org.goodmath.simplex.ast.FunctionDefinition
import org.goodmath.simplex.ast.LambdaExpr
import org.goodmath.simplex.ast.LetExpr
import org.goodmath.simplex.ast.LiteralExpr
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.LoopExpr
import org.goodmath.simplex.ast.MethodCallExpr
import org.goodmath.simplex.ast.MethodDefinition
import org.goodmath.simplex.ast.Model
import org.goodmath.simplex.ast.Operator
import org.goodmath.simplex.ast.OperatorExpr
import org.goodmath.simplex.ast.Product
import org.goodmath.simplex.ast.SimpleType
import org.goodmath.simplex.ast.TupleDefinition
import org.goodmath.simplex.ast.TupleExpr
import org.goodmath.simplex.ast.Type
import org.goodmath.simplex.ast.TypedName
import org.goodmath.simplex.ast.Update
import org.goodmath.simplex.ast.UpdateExpr
import org.goodmath.simplex.ast.VarRefExpr
import org.goodmath.simplex.ast.VariableDefinition
import org.goodmath.simplex.ast.WithExpr
import org.goodmath.simplex.runtime.SimplexError
import org.goodmath.simplex.runtime.csg.TwoDPointValueType
import org.goodmath.simplex.runtime.values.ValueType
import org.goodmath.simplex.runtime.values.csg.CsgValueType
import org.goodmath.simplex.runtime.values.csg.PolygonValueType
import org.goodmath.simplex.runtime.values.csg.ThreeDPointValueType
import org.goodmath.simplex.runtime.values.primitives.ArrayValueType
import org.goodmath.simplex.runtime.values.primitives.FloatValueType
import org.goodmath.simplex.runtime.values.primitives.FunctionValueType
import org.goodmath.simplex.runtime.values.primitives.IntegerValueType
import org.goodmath.simplex.runtime.values.primitives.StringValueType

@Suppress("UNCHECKED_CAST")
class SimplexParseListener: SimplexListener {

    fun parse(input: CharStream, echo: (Int, Any?, Boolean) -> Unit): Model {
        val lexer = SimplexLexer(input)
        val tokenStream = CommonTokenStream(lexer)
        val walker = ParseTreeWalker()
        val parser = SimplexParser(tokenStream)
        parser.removeErrorListeners()
        val errorListener = SimplexErrorListener()
        parser.addErrorListener(errorListener)
        val tree = parser.model()
        if (errorListener.errorCount > 0) {
            for (e in errorListener.getLoggedErrors()) {
                echo(0, e, true)
            }
            throw SimplexError(SimplexError.Kind.Parser,
                "See error log above for details")
        }
        walker.walk(this, tree)
        return getValueFor(tree) as Model
    }

    private var values: ParseTreeProperty<Any> = ParseTreeProperty()

    private fun setValueFor(node: ParseTree, ast: Any) {
        values.put(node, ast)
    }

    private fun getValueFor(node: ParseTree): Any {
        return values.get(node)
    }

    private fun loc(ctx: ParserRuleContext): Location {
        return Location(ctx.start.line, ctx.start.charPositionInLine + 1)
    }

    override fun enterModel(ctx: SimplexParser.ModelContext) {
    }

    override fun exitModel(ctx: SimplexParser.ModelContext) {
        val defs = ctx.def().map { getValueFor(it) as Definition }
        val products = ctx.product().map { getValueFor(it) as Product }
        setValueFor(ctx, Model(defs, products, loc(ctx)))
    }

    override fun enterProduct(ctx: SimplexParser.ProductContext) {
    }

    override fun exitProduct(ctx: SimplexParser.ProductContext) {
        val name = ctx.ID()?.let { it.text }
        val exprs = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(ctx, Product(name, exprs, loc(ctx)))
    }
    override fun enterOptVarDef(ctx: SimplexParser.OptVarDefContext) {
    }

    override fun exitOptVarDef(ctx: SimplexParser.OptVarDefContext) {
        val varDef = getValueFor(ctx.varDef())
        setValueFor(ctx, varDef)
    }

    override fun enterOptFunDef(ctx: SimplexParser.OptFunDefContext) {
    }

    override fun exitOptFunDef(ctx: SimplexParser.OptFunDefContext) {
        setValueFor(ctx, getValueFor(ctx.funDef()))
    }

    override fun enterOptTupleDef(ctx: SimplexParser.OptTupleDefContext) {
    }

    override fun exitOptTupleDef(ctx: SimplexParser.OptTupleDefContext) {
        setValueFor(ctx, getValueFor(ctx.tupleDef()))
    }

    override fun enterOptMethDef(ctx: SimplexParser.OptMethDefContext) {
    }

    override fun exitOptMethDef(ctx: SimplexParser.OptMethDefContext) {
        setValueFor(ctx, getValueFor(ctx.methDef()))
    }

    override fun enterTupleDef(ctx: SimplexParser.TupleDefContext) {
    }

    override fun exitTupleDef(ctx: SimplexParser.TupleDefContext) {
        val name = ctx.ID().text
        val fields = getValueFor(ctx.params()) as List<TypedName>
        setValueFor(ctx, TupleDefinition(name, fields, loc(ctx)))
    }

    override fun enterParams(ctx: SimplexParser.ParamsContext) {
    }

    override fun exitParams(ctx: SimplexParser.ParamsContext) {
        val params = ctx.param().map { getValueFor(it) as TypedName }
        setValueFor(ctx, params)
    }

    override fun enterVarDef(ctx: SimplexParser.VarDefContext) {
    }

    override fun exitVarDef(ctx: SimplexParser.VarDefContext) {
        val name = ctx.ID().text
        val type = ctx.type()?.let { getValueFor(it) as Type}
        val initValue = getValueFor(ctx.expr()) as Expr
        setValueFor(ctx, VariableDefinition(name, type, initValue, loc(ctx)))
    }

    override fun enterFunDef(ctx: SimplexParser.FunDefContext) {
    }

    override fun exitFunDef(ctx: SimplexParser.FunDefContext) {
        val name = ctx.ID().text
        val type = ctx.type()?.let { getValueFor(it) as Type}
        val localDefs = ctx.def().map { getValueFor(it) as Definition }
        val params = ctx.params()?.let { getValueFor(it) as List<TypedName>}
        val body = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(ctx, FunctionDefinition(name, type, params ?: emptyList(), localDefs, body, loc(ctx)))
    }

    override fun enterMethDef(ctx: SimplexParser.MethDefContext) {
    }

    override fun exitMethDef(ctx: SimplexParser.MethDefContext) {
        val type = getValueFor(ctx.target) as Type
        val result = ctx.result?.let { getValueFor(it) as Type }
        val name = ctx.ID().text
        val params = ctx.params()?.let { getValueFor(it) as List<TypedName> } ?: emptyList()
        val body = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(ctx, MethodDefinition(type, name, params, result, body, loc(ctx)))
    }


    override fun enterParam(ctx: SimplexParser.ParamContext) {
    }

    override fun exitParam(ctx: SimplexParser.ParamContext) {
        val name = ctx.ID().text
        val type = ctx.type()?.let { getValueFor(it) as Type }
        setValueFor(ctx, TypedName(name, type, loc(ctx)))
    }


    override fun enterOptSimpleType(ctx: SimplexParser.OptSimpleTypeContext) {
    }

    override fun exitOptSimpleType(ctx: SimplexParser.OptSimpleTypeContext) {
        val name = ctx.ID().text
        setValueFor(ctx, SimpleType(name))
    }

    override fun enterOptVectorType(ctx: SimplexParser.OptVectorTypeContext) {
    }

    override fun exitOptVectorType(ctx: SimplexParser.OptVectorTypeContext) {
        val elementType = getValueFor(ctx.type()) as Type
        setValueFor(ctx, ArrayType(elementType))
    }

    override fun enterBindings(ctx: SimplexParser.BindingsContext) {
    }

    override fun exitBindings(ctx: SimplexParser.BindingsContext) {
        val bindings = ctx.binding().map { getValueFor(it) as Binding }
        setValueFor(ctx, bindings)
    }

    override fun enterUpdates(ctx: SimplexParser.UpdatesContext) {
    }

    override fun exitUpdates(ctx: SimplexParser.UpdatesContext) {
        val ups = ctx.update().map { getValueFor(it) as Update }
        setValueFor(ctx, ups)
    }

    override fun enterExprs(ctx: SimplexParser.ExprsContext) {
    }

    override fun exitExprs(ctx: SimplexParser.ExprsContext) {
        val exprs = ctx.expr().map { getValueFor(it) as Expr}
        setValueFor(ctx, exprs)
    }

    override fun enterExprParen(ctx: SimplexParser.ExprParenContext) {

    }

    override fun exitExprParen(ctx: SimplexParser.ExprParenContext) {
        val e = getValueFor(ctx.expr())
        setValueFor(ctx, e)
    }

    override fun enterExprMethod(ctx: SimplexParser.ExprMethodContext) {
    }

    override fun exitExprMethod(ctx: SimplexParser.ExprMethodContext) {
        val lhs = getValueFor(ctx.expr()) as Expr
        val meth = ctx.ID().text
        val args = ctx.exprs()?.let { getValueFor(it) as List<Expr> } ?: emptyList()
        setValueFor(ctx, MethodCallExpr(lhs, meth, args, loc(ctx)))
    }

    override fun enterExprCall(ctx: SimplexParser.ExprCallContext) {
    }

    override fun exitExprCall(ctx: SimplexParser.ExprCallContext) {
        val funExpr = getValueFor(ctx.expr()) as Expr
        val args = ctx.exprs()?.let { getValueFor(it) as List<Expr>} ?: emptyList()
        setValueFor(ctx, FunCallExpr(funExpr, args, loc(ctx)))
    }

    override fun enterExprComplex(ctx: SimplexParser.ExprComplexContext) {
    }

    override fun exitExprComplex(ctx: SimplexParser.ExprComplexContext) {
        val expr = getValueFor(ctx.complex()) as Expr
        setValueFor(ctx, expr)
    }

    override fun enterExprAdd(ctx: SimplexParser.ExprAddContext) {
    }

    override fun exitExprAdd(ctx: SimplexParser.ExprAddContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.addOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))
    }

    override fun enterExprPow(ctx: SimplexParser.ExprPowContext) {
    }

    override fun exitExprPow(ctx: SimplexParser.ExprPowContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.expOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))
    }

    override fun enterExprMult(ctx: SimplexParser.ExprMultContext) {
    }

    override fun exitExprMult(ctx: SimplexParser.ExprMultContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.multOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))
    }

    override fun enterExprUnary(ctx: SimplexParser.ExprUnaryContext) {
    }

    override fun exitExprUnary(ctx: SimplexParser.ExprUnaryContext) {
        val op = getValueFor(ctx.unaryOp()) as Operator
        setValueFor(ctx, op)
    }

    override fun enterExprLogic(ctx: SimplexParser.ExprLogicContext) {
    }

    override fun exitExprLogic(ctx: SimplexParser.ExprLogicContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.logicOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))
    }

    override fun enterExprCompare(ctx: SimplexParser.ExprCompareContext) {
    }

    override fun exitExprCompare(ctx: SimplexParser.ExprCompareContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.compareOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))
    }

    override fun enterExprSubscript(ctx: SimplexParser.ExprSubscriptContext) {
    }

    override fun exitExprSubscript(ctx: SimplexParser.ExprSubscriptContext) {
        val target = getValueFor(ctx.expr()[0]) as Expr
        val sub = getValueFor(ctx.expr()[1]) as Expr
        setValueFor(ctx, OperatorExpr(Operator.Subscript, listOf(target, sub), loc(ctx)))
    }

    override fun enterExprPrimary(ctx: SimplexParser.ExprPrimaryContext) {
    }

    override fun exitExprPrimary(ctx: SimplexParser.ExprPrimaryContext) {
        val e = getValueFor(ctx.primary())
        setValueFor(ctx, e)
    }

    override fun enterComplexLetExpr(ctx: SimplexParser.ComplexLetExprContext) {
    }

    override fun exitComplexLetExpr(ctx: SimplexParser.ComplexLetExprContext) {
        val bindings = getValueFor(ctx.bindings()) as List<Binding>
        val body = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(ctx, LetExpr(bindings, body, loc(ctx)))
    }

    override fun enterComplexCondExpr(ctx: SimplexParser.ComplexCondExprContext) {
    }

    override fun exitComplexCondExpr(ctx: SimplexParser.ComplexCondExprContext) {
        val conds = ctx.condClause().map { getValueFor(it) as Condition }
        val elseClause = getValueFor(ctx.expr()) as Expr
        setValueFor(ctx, CondExpr(conds, elseClause, loc(ctx)))
    }

    override fun enterComplexForExpr(ctx: SimplexParser.ComplexForExprContext) {
    }

    override fun exitComplexForExpr(ctx: SimplexParser.ComplexForExprContext) {
        val idx = ctx.ID().text
        val coll = getValueFor(ctx.expr().first()) as Expr
        val body = ctx.expr().drop(1).map { getValueFor(it) as Expr }
        setValueFor(ctx, LoopExpr(idx, coll,body, loc(ctx)))
    }

    override fun enterComplexDoExpr(ctx: SimplexParser.ComplexDoExprContext) {
    }

    override fun exitComplexDoExpr(ctx: SimplexParser.ComplexDoExprContext) {
        val body = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(ctx, BlockExpr(body, loc(ctx)))
    }

    override fun enterComplexUpdateExpr(ctx: SimplexParser.ComplexUpdateExprContext) {
    }

    override fun exitComplexUpdateExpr(ctx: SimplexParser.ComplexUpdateExprContext) {
        val target = getValueFor(ctx.expr()) as Expr
        val updates = getValueFor(ctx.updates()) as List<Update>
        setValueFor(ctx, UpdateExpr(target, updates, loc(ctx)))
    }

    override fun enterComplexLambdaExpr(ctx: SimplexParser.ComplexLambdaExprContext) {
    }

    override fun exitComplexLambdaExpr(ctx: SimplexParser.ComplexLambdaExprContext) {
        val resultType = ctx.type()?.let { getValueFor(it) as Type }
        val args = getValueFor(ctx.params()) as List<TypedName>
        val body = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(ctx, LambdaExpr(resultType, args, body, loc(ctx)))
    }

    override fun enterComplexWithExpr(ctx: SimplexParser.ComplexWithExprContext) {
    }

    override fun exitComplexWithExpr(ctx: SimplexParser.ComplexWithExprContext) {
        val focus = getValueFor(ctx.expr()[0]) as Expr
        val body = ctx.expr().drop(1).map { getValueFor(it) as Expr }
        setValueFor(ctx, WithExpr(focus, body, loc(ctx)))
    }

    override fun enterOptIdExpr(ctx: SimplexParser.OptIdExprContext) {
    }

    override fun exitOptIdExpr(ctx: SimplexParser.OptIdExprContext) {
        val id = ctx.ID().text
        setValueFor(ctx, VarRefExpr(id, loc(ctx)))
    }

    override fun enterOptVecExpr(ctx: SimplexParser.OptVecExprContext) {
    }

    override fun exitOptVecExpr(ctx: SimplexParser.OptVecExprContext) {
        val es = getValueFor(ctx.exprs()) as List<Expr>
        setValueFor(ctx, ArrayExpr(es, loc(ctx)))
    }

    override fun enterOptTupleExpr(ctx: SimplexParser.OptTupleExprContext) {
    }

    override fun exitOptTupleExpr(ctx: SimplexParser.OptTupleExprContext) {
        val id = ctx.ID().text
        val args = getValueFor(ctx.exprs()) as List<Expr>
        setValueFor(ctx, TupleExpr(id, args, loc(ctx)))
    }

    override fun enterOptLitInt(ctx: SimplexParser.OptLitIntContext) {
    }

    override fun exitOptLitInt(ctx: SimplexParser.OptLitIntContext) {
        setValueFor(ctx, LiteralExpr(ctx.LIT_INT().text.toInt(), loc(ctx)))
    }

    override fun enterOptLitFloat(ctx: SimplexParser.OptLitFloatContext) {
    }

    override fun exitOptLitFloat(ctx: SimplexParser.OptLitFloatContext) {
        setValueFor(ctx, LiteralExpr(ctx.LIT_FLOAT().text.toDouble(), loc(ctx)))
    }

    override fun enterOptLitStr(ctx: SimplexParser.OptLitStrContext) {
    }

    override fun exitOptLitStr(ctx: SimplexParser.OptLitStrContext) {
        val litStr = ctx.LIT_STRING().text.drop(1).dropLast(1)

        setValueFor(ctx, LiteralExpr(litStr, loc(ctx)))
    }

    override fun enterOptTrue(ctx: SimplexParser.OptTrueContext) {
    }

    override fun exitOptTrue(ctx: SimplexParser.OptTrueContext) {
        setValueFor(ctx, VarRefExpr("true", loc(ctx)))
    }

    override fun enterOptFalse(ctx: SimplexParser.OptFalseContext) {
    }

    override fun exitOptFalse(ctx: SimplexParser.OptFalseContext) {
        setValueFor(ctx, VarRefExpr("false", loc(ctx)))
    }

    override fun enterUpdate(ctx: SimplexParser.UpdateContext) {
    }

    override fun exitUpdate(ctx: SimplexParser.UpdateContext) {
        val field = ctx.ID().text
        val value = getValueFor(ctx.expr()) as Expr
        setValueFor(ctx, Update(field, value))
    }

    override fun enterBinding(ctx: SimplexParser.BindingContext) {
    }

    override fun exitBinding(ctx: SimplexParser.BindingContext) {
        val name = ctx.ID().text
        val type = ctx.type()?.let { getValueFor(it) as Type }
        val value = getValueFor(ctx.expr()) as Expr
        setValueFor(ctx, Binding(name, type, value, loc(ctx)))
    }

    override fun enterOpOptPow(ctx: SimplexParser.OpOptPowContext) {
    }

    override fun exitOpOptPow(ctx: SimplexParser.OpOptPowContext) {
        setValueFor(ctx, Operator.Pow)
    }

    override fun enterOpOptTimes(ctx: SimplexParser.OpOptTimesContext) {
    }

    override fun exitOpOptTimes(ctx: SimplexParser.OpOptTimesContext) {
        setValueFor(ctx, Operator.Times)
    }


    override fun enterOpOptSlash(ctx: SimplexParser.OpOptSlashContext) {
        
    }

    override fun exitOpOptSlash(ctx: SimplexParser.OpOptSlashContext) {
        setValueFor(ctx, Operator.Times)
    }

    override fun enterOpOptPercent(ctx: SimplexParser.OpOptPercentContext) {
        
    }

    override fun exitOpOptPercent(ctx: SimplexParser.OpOptPercentContext) {
        setValueFor(ctx, Operator.Mod)
    }

    override fun enterOpOptPlus(ctx: SimplexParser.OpOptPlusContext) {
        
    }

    override fun exitOpOptPlus(ctx: SimplexParser.OpOptPlusContext) {
        setValueFor(ctx, Operator.Plus)
    }

    override fun enterOpOptMinus(ctx: SimplexParser.OpOptMinusContext) {
        
    }

    override fun exitOpOptMinus(ctx: SimplexParser.OpOptMinusContext) {
        setValueFor(ctx, Operator.Minus)
    }


    override fun enterOpOptEqEq(ctx: SimplexParser.OpOptEqEqContext) {
        
    }

    override fun exitOpOptEqEq(ctx: SimplexParser.OpOptEqEqContext) {
        setValueFor(ctx, Operator.Eq)
    }

    override fun enterOpOptBangEq(ctx: SimplexParser.OpOptBangEqContext) {
        
    }

    override fun exitOpOptBangEq(ctx: SimplexParser.OpOptBangEqContext) {
        setValueFor(ctx, Operator.Neq)
    }

    override fun enterOpOptLt(ctx: SimplexParser.OpOptLtContext) {
        
    }

    override fun exitOpOptLt(ctx: SimplexParser.OpOptLtContext) {
        setValueFor(ctx, Operator.Lt)
    }

    override fun enterOpOptLe(ctx: SimplexParser.OpOptLeContext) {
        
    }

    override fun exitOpOptLe(ctx: SimplexParser.OpOptLeContext) {
        setValueFor(ctx, Operator.Le)
    }

    override fun enterOpOptGt(ctx: SimplexParser.OpOptGtContext) {
        
    }

    override fun exitOpOptGt(ctx: SimplexParser.OpOptGtContext) {
        setValueFor(ctx, Operator.Gt)
    }

    override fun enterOpOptGe(ctx: SimplexParser.OpOptGeContext) {
        
    }

    override fun exitOpOptGe(ctx: SimplexParser.OpOptGeContext) {
        setValueFor(ctx, Operator.Ge)
    }

    override fun enterOpOptAnd(ctx: SimplexParser.OpOptAndContext) {

    }

    override fun exitOpOptAnd(ctx: SimplexParser.OpOptAndContext) {
        setValueFor(ctx, Operator.And)
    }

    override fun enterOpOptOr(ctx: SimplexParser.OpOptOrContext) {

    }

    override fun exitOpOptOr(ctx: SimplexParser.OpOptOrContext) {
        setValueFor(ctx, Operator.Or)
    }

    override fun enterOpOptNot(ctx: SimplexParser.OpOptNotContext) {
        
    }

    override fun exitOpOptNot(ctx: SimplexParser.OpOptNotContext) {
        setValueFor(ctx, Operator.Not)
    }

    override fun enterOpUnaryNeg(ctx: SimplexParser.OpUnaryNegContext) {
    }

    override fun exitOpUnaryNeg(ctx: SimplexParser.OpUnaryNegContext) {
        setValueFor(ctx, Operator.Minus)
    }

    override fun enterCondClause(ctx: SimplexParser.CondClauseContext) {
    }

    override fun exitCondClause(ctx: SimplexParser.CondClauseContext) {
        val cond = getValueFor(ctx.c) as Expr
        val value = getValueFor(ctx.v) as Expr
        setValueFor(ctx, Condition(cond, value))
    }

    override fun visitTerminal(node: TerminalNode) {
    }

    override fun visitErrorNode(node: ErrorNode) {
    }

    override fun enterEveryRule(ctx: ParserRuleContext) {
    }

    override fun exitEveryRule(ctx: ParserRuleContext) {
    }

}
