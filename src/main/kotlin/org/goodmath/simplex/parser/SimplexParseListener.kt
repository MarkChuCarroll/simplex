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
import org.goodmath.simplex.ast.FieldRefExpr
import org.goodmath.simplex.ast.FunCallExpr
import org.goodmath.simplex.ast.FunctionDefinition
import org.goodmath.simplex.ast.LetExpr
import org.goodmath.simplex.ast.LiteralExpr
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.LoopExpr
import org.goodmath.simplex.ast.MethodCallExpr
import org.goodmath.simplex.ast.Model
import org.goodmath.simplex.ast.Operator
import org.goodmath.simplex.ast.OperatorExpr
import org.goodmath.simplex.ast.Render
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

@Suppress("UNCHECKED_CAST")
class SimplexParseListener: SimplexListener {

    fun parse(input: CharStream, echo: (String, Boolean) -> Unit): Model {
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
                echo(e, true)
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
        return Location(ctx.start.line, ctx.start.charPositionInLine)
    }

    override fun enterModel(ctx: SimplexParser.ModelContext) {
    }

    override fun exitModel(ctx: SimplexParser.ModelContext) {
        val name = ctx.ID().text
        val defs = ctx.def().map { getValueFor(it) as Definition }
        val renders = ctx.render().map { getValueFor(it) as Render }
        setValueFor(ctx, Model(name, defs, renders, loc(ctx)))
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

    override fun enterTupleDef(ctx: SimplexParser.TupleDefContext) {
    }

    override fun exitTupleDef(ctx: SimplexParser.TupleDefContext) {
        val name = ctx.ID().text
        val fields = getValueFor(ctx.params()) as List<TypedName>
        setValueFor(ctx, TupleDefinition(name, fields, loc(ctx)))
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
        val localDefs = getValueFor(ctx.defs()) as List<Definition>
        val params = getValueFor(ctx.params()) as List<TypedName>
        val body = getValueFor(ctx.exprs()) as List<Expr>
        setValueFor(ctx, FunctionDefinition(name, params, localDefs, body, loc(ctx)))
    }

    override fun enterDefs(ctx: SimplexParser.DefsContext) {
    }

    override fun exitDefs(ctx: SimplexParser.DefsContext) {
        setValueFor(ctx, ctx.def().map { getValueFor(it) as Definition })
    }

    override fun enterParam(ctx: SimplexParser.ParamContext) {
    }

    override fun exitParam(ctx: SimplexParser.ParamContext) {
        val name = ctx.ID().text
        val type = ctx.type()?.let { getValueFor(it) as Type }
        setValueFor(ctx, TypedName(name, type, loc(ctx)))
    }

    override fun enterParams(ctx: SimplexParser.ParamsContext) {
    }

    override fun exitParams(ctx: SimplexParser.ParamsContext) {
        val params = ctx.param().map { getValueFor(it) as TypedName }
        setValueFor(ctx, params)
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

    override fun enterExpOp(ctx: SimplexParser.ExpOpContext) {

    }

    override fun exitExpOp(ctx: SimplexParser.ExpOpContext) {
        setValueFor(ctx, Operator.Pow)
    }

    override fun enterOpOptStar(ctx: SimplexParser.OpOptStarContext) {
    }

    override fun exitOpOptStar(ctx: SimplexParser.OpOptStarContext) {
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

    override fun enterOpOptUnaryMinus(ctx: SimplexParser.OpOptUnaryMinusContext) {
        
    }

    override fun exitOpOptUnaryMinus(ctx: SimplexParser.OpOptUnaryMinusContext) {
        setValueFor(ctx, Operator.Minus)
    }

    override fun enterExprs(ctx: SimplexParser.ExprsContext) {
        
    }

    override fun exitExprs(ctx: SimplexParser.ExprsContext) {
        setValueFor(ctx, ctx.expr().map { getValueFor(it) as Expr})
    }

    override fun enterExprOptParens(ctx: SimplexParser.ExprOptParensContext) {
    }

    override fun exitExprOptParens(ctx: SimplexParser.ExprOptParensContext) {
        setValueFor(ctx, getValueFor(ctx.expr()))
    }

    override fun enterExprOptSuffix(ctx: SimplexParser.ExprOptSuffixContext) {
    }

    override fun exitExprOptSuffix(ctx: SimplexParser.ExprOptSuffixContext) {
        setValueFor(ctx, getValueFor(ctx.suffixExpr()))
    }

    override fun enterSuffixExpr(ctx: SimplexParser.SuffixExprContext) {
    }

    data class CallSuffix(val args: List<Expr>)
    data class SubscriptSuffix(val arg: Expr)
    data class FieldSuffix(val name: String)
    data class MethSuffix(val name: String, val args: List<Expr>)

    override fun exitSuffixExpr(ctx: SimplexParser.SuffixExprContext) {
        val root = getValueFor(ctx.binaryExpr()) as Expr
        val suffixes = ctx.suffix().map { getValueFor(it) }
        var result = root
        for (s in suffixes) {
            result = when(s) {
                is CallSuffix -> FunCallExpr(result, s.args, loc(ctx))
                is SubscriptSuffix -> OperatorExpr(Operator.Subscript, listOf(result, s.arg), loc(ctx))
                is FieldSuffix -> FieldRefExpr(result, s.name, loc(ctx))
                is MethSuffix -> MethodCallExpr(result, s.name, s.args, loc(ctx))
                else -> throw SimplexError(SimplexError.Kind.Internal, "Invalid suffix expr; should be impossible")
            }
        }
        setValueFor(ctx, result)
    }

    override fun enterSuffixOptCall(ctx: SimplexParser.SuffixOptCallContext) {
    }

    override fun exitSuffixOptCall(ctx: SimplexParser.SuffixOptCallContext) {
        var args = getValueFor(ctx.exprList()) as List<Expr>
        setValueFor(ctx, CallSuffix(args))
    }

    override fun enterSuffixOptSubscript(ctx: SimplexParser.SuffixOptSubscriptContext) {
    }

    override fun exitSuffixOptSubscript(ctx: SimplexParser.SuffixOptSubscriptContext) {
        val subscript = getValueFor(ctx.expr()) as Expr
        setValueFor(ctx, SubscriptSuffix(subscript))
    }

    override fun enterSuffixOptField(ctx: SimplexParser.SuffixOptFieldContext) {
    }

    override fun exitSuffixOptField(ctx: SimplexParser.SuffixOptFieldContext) {
        val name = ctx.ID().text
        setValueFor(ctx, FieldSuffix(name))
    }

    override fun enterSuffixOptMeth(ctx: SimplexParser.SuffixOptMethContext) {
    }

    override fun exitSuffixOptMeth(ctx: SimplexParser.SuffixOptMethContext) {
        val name = ctx.ID().text
        val args = getValueFor(ctx.exprList()) as List<Expr>
        setValueFor(ctx, MethSuffix(name, args))
    }

    override fun enterBinOptTwo(ctx: SimplexParser.BinOptTwoContext) {
    }

    override fun exitBinOptTwo(ctx: SimplexParser.BinOptTwoContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.expOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))
    }

    override fun enterMultOptTwo(ctx: SimplexParser.MultOptTwoContext) {
    }

    override fun exitMultOptTwo(ctx: SimplexParser.MultOptTwoContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.op) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))
    }

    override fun enterMultOptOne(ctx: SimplexParser.MultOptOneContext) {
    }

    override fun exitMultOptOne(ctx: SimplexParser.MultOptOneContext) {
        setValueFor(ctx, getValueFor(ctx.addExpr()))
    }

    override fun enterAddOptOne(ctx: SimplexParser.AddOptOneContext) {
    }

    override fun exitAddOptOne(ctx: SimplexParser.AddOptOneContext) {
        setValueFor(ctx, getValueFor(ctx.compareExpr()))
    }

    override fun enterAddOptTwo(ctx: SimplexParser.AddOptTwoContext) {
    }

    override fun exitAddOptTwo(ctx: SimplexParser.AddOptTwoContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.addOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))

    }

    override fun enterCompOpTwo(ctx: SimplexParser.CompOpTwoContext) {
    }

    override fun exitCompOpTwo(ctx: SimplexParser.CompOpTwoContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.compareOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))

    }

    override fun enterCompOpOne(ctx: SimplexParser.CompOpOneContext) {
    }

    override fun exitCompOpOne(ctx: SimplexParser.CompOpOneContext) {
        setValueFor(ctx, getValueFor(ctx.logicExpr()))
    }

    override fun enterLogOpTwo(ctx: SimplexParser.LogOpTwoContext) {

    }

    override fun exitLogOpTwo(ctx: SimplexParser.LogOpTwoContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.logicOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))

    }

    override fun enterLogOptOne(ctx: SimplexParser.LogOptOneContext) {

    }

    override fun exitLogOptOne(ctx: SimplexParser.LogOptOneContext) {
        setValueFor(ctx, getValueFor(ctx.baseExpr()))
    }

    override fun enterBinOptOne(ctx: SimplexParser.BinOptOneContext) {
    }

    override fun exitBinOptOne(ctx: SimplexParser.BinOptOneContext) {
        setValueFor(ctx, getValueFor(ctx.multExpr()))
    }



    override fun enterBaseOptId(ctx: SimplexParser.BaseOptIdContext) {
        
    }

    override fun exitBaseOptId(ctx: SimplexParser.BaseOptIdContext) {
        val id = ctx.ID().text
        setValueFor(ctx, VarRefExpr(id, loc(ctx)))
    }

    override fun enterBaseOptCond(ctx: SimplexParser.BaseOptCondContext) {
        
    }

    override fun exitBaseOptCond(ctx: SimplexParser.BaseOptCondContext) {
        setValueFor(ctx, getValueFor(ctx.condExpr()))
    }

    override fun enterBaseOptLoop(ctx: SimplexParser.BaseOptLoopContext) {
        
    }

    override fun exitBaseOptLoop(ctx: SimplexParser.BaseOptLoopContext) {
        setValueFor(ctx, getValueFor(ctx.loopExpr()))
    }

    override fun enterBaseOptTuple(ctx: SimplexParser.BaseOptTupleContext) {
        
    }

    override fun exitBaseOptTuple(ctx: SimplexParser.BaseOptTupleContext) {
        setValueFor(ctx, getValueFor(ctx.tupleExpr()))
    }

    override fun enterBaseOptUpdate(ctx: SimplexParser.BaseOptUpdateContext) {
        
    }

    override fun exitBaseOptUpdate(ctx: SimplexParser.BaseOptUpdateContext) {
        setValueFor(ctx, getValueFor(ctx.updateExpr()))
    }

    override fun enterBaseOptWith(ctx: SimplexParser.BaseOptWithContext) {
        
    }

    override fun exitBaseOptWith(ctx: SimplexParser.BaseOptWithContext) {
        
    }

    override fun enterBaseOptLiteral(ctx: SimplexParser.BaseOptLiteralContext) {
    }

    override fun exitBaseOptLiteral(ctx: SimplexParser.BaseOptLiteralContext) {
        setValueFor(ctx, getValueFor(ctx.literalExpr()))
    }

    override fun enterBaseOptVector(ctx: SimplexParser.BaseOptVectorContext) {
        
    }

    override fun exitBaseOptVector(ctx: SimplexParser.BaseOptVectorContext) {
        setValueFor(ctx, getValueFor(ctx.vectorExpr()))
    }

    override fun enterBaseOptDo(ctx: SimplexParser.BaseOptDoContext) {
        
    }

    override fun exitBaseOptDo(ctx: SimplexParser.BaseOptDoContext) {
        setValueFor(ctx, getValueFor(ctx.doExpr()))
    }

    override fun enterBaseOptLet(ctx: SimplexParser.BaseOptLetContext) {
        
    }

    override fun exitBaseOptLet(ctx: SimplexParser.BaseOptLetContext) {
        setValueFor(ctx, getValueFor(ctx.letExpr()))
    }

    override fun enterExprList(ctx: SimplexParser.ExprListContext) {
        
    }

    override fun exitExprList(ctx: SimplexParser.ExprListContext) {
        setValueFor(ctx, ctx.expr().map { getValueFor(it) as Expr })
    }

    override fun enterDoExpr(ctx: SimplexParser.DoExprContext) {
        
    }

    override fun exitDoExpr(ctx: SimplexParser.DoExprContext) {
        val body = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(ctx, BlockExpr(body, loc(ctx)))
    }

    override fun enterLetExpr(ctx: SimplexParser.LetExprContext) {

    }

    override fun exitLetExpr(ctx: SimplexParser.LetExprContext) {
        val bindings = getValueFor(ctx.bindings()) as List<Binding>
        val body = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(ctx, LetExpr(bindings, body, loc(ctx)))
    }

    override fun enterBindings(ctx: SimplexParser.BindingsContext) {
    }

    override fun exitBindings(ctx: SimplexParser.BindingsContext) {
        setValueFor(ctx, ctx.binding().map { getValueFor(it) as Binding})
    }

    override fun enterBinding(ctx: SimplexParser.BindingContext) {
        
    }

    override fun exitBinding(ctx: SimplexParser.BindingContext) {
        val name = ctx.ID().text
        val type = ctx.type()?.let { getValueFor(it) as Type }
        val value = getValueFor(ctx.expr()) as Expr
        setValueFor(ctx, Binding(name, type, value, loc(ctx)))
    }

    override fun enterVectorExpr(ctx: SimplexParser.VectorExprContext) {
        
    }

    override fun exitVectorExpr(ctx: SimplexParser.VectorExprContext) {
        val elements = getValueFor(ctx.exprList()) as List<Expr>
        setValueFor(ctx, ArrayExpr(elements, loc(ctx)))
    }


    override fun enterWithExpr(ctx: SimplexParser.WithExprContext) {
        
    }

    override fun exitWithExpr(ctx: SimplexParser.WithExprContext) {
        val focus = getValueFor(ctx.expr()) as Expr
        val body = getValueFor(ctx.exprs()) as List<Expr>
        setValueFor(ctx, WithExpr(focus, body, loc(ctx)))
    }

    override fun enterUpdateExpr(ctx: SimplexParser.UpdateExprContext) {
        
    }

    override fun exitUpdateExpr(ctx: SimplexParser.UpdateExprContext) {
        val target = getValueFor(ctx.expr()) as Expr
        val updates = getValueFor(ctx.updates()) as List<Update>
        setValueFor(ctx, UpdateExpr(target, updates, loc(ctx)))
    }

    override fun enterTupleExpr(ctx: SimplexParser.TupleExprContext) {
        
    }

    override fun exitTupleExpr(ctx: SimplexParser.TupleExprContext) {
        val type = ctx.ID().text
        val fields = getValueFor(ctx.exprList()) as List<Expr>
        setValueFor(ctx, TupleExpr(type, fields, loc(ctx)))
    }

    override fun enterUpdates(ctx: SimplexParser.UpdatesContext) {

    }

    override fun exitUpdates(ctx: SimplexParser.UpdatesContext) {
        setValueFor(ctx, ctx.update().map { getValueFor(it) as Update })
    }

    override fun enterUpdate(ctx: SimplexParser.UpdateContext) {
        
    }

    override fun exitUpdate(ctx: SimplexParser.UpdateContext) {
        val name = ctx.ID().text
        val value = getValueFor(ctx.expr()) as Expr
        setValueFor(ctx, Update(name, value))
    }

    override fun enterLoopExpr(ctx: SimplexParser.LoopExprContext) {
        
    }

    override fun exitLoopExpr(ctx: SimplexParser.LoopExprContext) {
        val idx = ctx.ID().text
        val coll = getValueFor(ctx.coll) as Expr
        val body = getValueFor(ctx.body) as List<Expr>
        setValueFor(ctx, LoopExpr(idx, coll, body, loc(ctx)))
    }

    override fun enterCondExpr(ctx: SimplexParser.CondExprContext) {
    }

    override fun exitCondExpr(ctx: SimplexParser.CondExprContext) {
        val clauses = ctx.condClause().map { getValueFor(it) as Condition }
        val elseClause = getValueFor(ctx.e) as Expr
        setValueFor(ctx, CondExpr(clauses, elseClause, loc(ctx)))
    }

    override fun enterCondClause(ctx: SimplexParser.CondClauseContext) {

    }

    override fun exitCondClause(ctx: SimplexParser.CondClauseContext) {
        val cond = getValueFor(ctx.c) as Expr
        val value = getValueFor(ctx.v) as Expr
        setValueFor(ctx, Condition(cond, value))
    }

    override fun enterOptInt(ctx: SimplexParser.OptIntContext) {
    }

    override fun exitOptInt(ctx: SimplexParser.OptIntContext) {
        val i = ctx.LIT_INT().text.toInt()
        setValueFor(ctx, LiteralExpr(i, loc(ctx)))
    }

    override fun enterOptFloat(ctx: SimplexParser.OptFloatContext) {
    }

    override fun exitOptFloat(ctx: SimplexParser.OptFloatContext) {
        val d = ctx.LIT_FLOAT().text.toDouble()
        setValueFor(ctx, LiteralExpr(d, loc(ctx)))
    }

    override fun enterOptString(ctx: SimplexParser.OptStringContext) {
    }

    override fun exitOptString(ctx: SimplexParser.OptStringContext) {
        val s = ctx.LIT_STRING().text
        setValueFor(ctx, LiteralExpr(s, loc(ctx)))
    }

    override fun enterOptTrue(ctx: SimplexParser.OptTrueContext) {
    }

    override fun exitOptTrue(ctx: SimplexParser.OptTrueContext) {
        setValueFor(ctx, LiteralExpr(true, loc(ctx)))
    }

    override fun enterOptFalse(ctx: SimplexParser.OptFalseContext) {
    }

    override fun exitOptFalse(ctx: SimplexParser.OptFalseContext) {
        setValueFor(ctx, LiteralExpr(false, loc(ctx)))
    }

    override fun enterRender(ctx: SimplexParser.RenderContext) {

    }

    override fun exitRender(ctx: SimplexParser.RenderContext) {
        val name = ctx.ID().text
        val values = getValueFor(ctx.exprs()) as List<Expr>
        setValueFor(ctx, Render(name, values, loc(ctx)))
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
