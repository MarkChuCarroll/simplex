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
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ErrorNode
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeProperty
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.tree.TerminalNode
import org.goodmath.simplex.ast.Location
import org.goodmath.simplex.ast.Model
import org.goodmath.simplex.ast.Product
import org.goodmath.simplex.ast.def.Definition
import org.goodmath.simplex.ast.def.FunctionDefinition
import org.goodmath.simplex.ast.def.MethodDefinition
import org.goodmath.simplex.ast.def.DataDefinition
import org.goodmath.simplex.ast.def.VariableDefinition
import org.goodmath.simplex.ast.expr.VectorExpr
import org.goodmath.simplex.ast.expr.AssignmentExpr
import org.goodmath.simplex.ast.expr.BlockExpr
import org.goodmath.simplex.ast.expr.CondExpr
import org.goodmath.simplex.ast.expr.Condition
import org.goodmath.simplex.ast.expr.Expr
import org.goodmath.simplex.ast.expr.FieldRefExpr
import org.goodmath.simplex.ast.expr.FunCallExpr
import org.goodmath.simplex.ast.expr.LambdaExpr
import org.goodmath.simplex.ast.expr.LetExpr
import org.goodmath.simplex.ast.expr.LiteralExpr
import org.goodmath.simplex.ast.expr.LoopExpr
import org.goodmath.simplex.ast.expr.MethodCallExpr
import org.goodmath.simplex.ast.expr.Operator
import org.goodmath.simplex.ast.expr.OperatorExpr
import org.goodmath.simplex.ast.expr.DataExpr
import org.goodmath.simplex.ast.expr.DataFieldUpdateExpr
import org.goodmath.simplex.ast.expr.ScopedRefExpr
import org.goodmath.simplex.ast.expr.VarRefExpr
import org.goodmath.simplex.ast.expr.WhileExpr
import org.goodmath.simplex.ast.types.Type
import org.goodmath.simplex.ast.types.TypedName
import org.goodmath.simplex.runtime.RootEnv
import org.goodmath.simplex.runtime.SimplexError
import java.io.FileReader
import java.nio.file.Path

@Suppress("UNCHECKED_CAST")
class SimplexParseListener : SimplexListener {
    var filename: String = "<unknown>"

    fun parse(filename: String, input: CharStream, echo: (Int, Any?, Boolean) -> Unit): Model {
        this.filename = filename
        val lexer = SimplexLexer(input)
        val tokenStream = CommonTokenStream(lexer)
        val walker = ParseTreeWalker()
        val parser = SimplexParser(tokenStream)
        parser.removeErrorListeners()
        val errorListener = SimplexErrorListener()
        parser.addErrorListener(errorListener)
        val tree = parser.model()
        val t = Type
        if (errorListener.errorCount > 0) {
            for (e in errorListener.getLoggedErrors()) {
                echo(0, e, true)
            }
            throw SimplexError(SimplexError.Kind.Parser, "See error log above for details")
        }
        walker.walk(this, tree)
        while (importSet.isNotEmpty()) {
            val lib = importSet.removeFirst()
            if (!RootEnv.importedScopes.containsKey(lib.name)) {
                parseLibraryFile(lib.name, lib.path,
                    echo)
            }
        }
        return getValueFor(tree) as Model

    }

    data class LibraryImport(
        val name: String,
        val path: Path
    )

    val importSet = ArrayList<LibraryImport>()

    fun parseLibraryFile(moduleName: String,
                         path: Path,
                         echo: (Int, Any?, Boolean) -> Unit) {
        System.err.println("Reading library '$path'")
        val input = CharStreams.fromPath(path)
        val libLexer = SimplexLexer(input)
        val tokenStream = CommonTokenStream(libLexer)
        val walker = ParseTreeWalker()
        val parser = SimplexParser(tokenStream)
        parser.removeErrorListeners()
        val errorListener = SimplexErrorListener()
        parser.addErrorListener(errorListener)
        val tree = parser.libraryModule()
        val t = Type
        if (errorListener.errorCount > 0) {
            for (e in errorListener.getLoggedErrors()) {
                echo(0, e, true)
            }
            throw SimplexError(SimplexError.Kind.Parser, "See error log above for details")
        }
        walker.walk(this, tree)
        val defs = getValueFor(tree) as List<Definition>
        RootEnv.addImportedLibrary(moduleName, defs)
    }

    private var values: ParseTreeProperty<Any> = ParseTreeProperty()

    private fun setValueFor(node: ParseTree, ast: Any) {
        values.put(node, ast)
    }

    private fun getValueFor(node: ParseTree): Any {
        return values.get(node)
    }

    private fun loc(ctx: ParserRuleContext): Location {
        return Location(filename, ctx.start.line, ctx.start.charPositionInLine + 1)
    }

    override fun enterModel(ctx: SimplexParser.ModelContext) {}

    override fun exitModel(ctx: SimplexParser.ModelContext) {
        val defs = ctx.def().map { getValueFor(it) as Definition }
        val products = ctx.product().map { getValueFor(it) as Product }
        setValueFor(ctx, Model(defs, products, loc(ctx)))
    }

    override fun enterLibraryModule(ctx: SimplexParser.LibraryModuleContext) {
    }

    override fun exitLibraryModule(ctx: SimplexParser.LibraryModuleContext) {
        val defs = ctx.def().map { getValueFor(it) as Definition }
        setValueFor(ctx, defs)
    }

    override fun enterImportLibrary(ctx: SimplexParser.ImportLibraryContext) {
    }

    override fun exitImportLibrary(ctx: SimplexParser.ImportLibraryContext) {
        val libPathStr = ctx.path.text
        val path = Path.of(libPathStr.substring(1, libPathStr.length - 1))
        val id = ctx.id.text
        importSet.add(LibraryImport(id, path))
    }

    override fun enterProduct(ctx: SimplexParser.ProductContext) {}

    override fun exitProduct(ctx: SimplexParser.ProductContext) {
        val name = ctx.LIT_STRING().text.drop(1).dropLast(1)
        val exprs = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(ctx, Product(name, exprs, loc(ctx)))
    }

    override fun enterOptVarDef(ctx: SimplexParser.OptVarDefContext) {}

    override fun exitOptVarDef(ctx: SimplexParser.OptVarDefContext) {
        val varDef = getValueFor(ctx.varDef())
        setValueFor(ctx, varDef)
    }

    override fun enterOptFunDef(ctx: SimplexParser.OptFunDefContext) {}

    override fun exitOptFunDef(ctx: SimplexParser.OptFunDefContext) {
        setValueFor(ctx, getValueFor(ctx.funDef()))
    }

    override fun enterOptDataDef(ctx: SimplexParser.OptDataDefContext) {}

    override fun exitOptDataDef(ctx: SimplexParser.OptDataDefContext) {
        setValueFor(ctx, getValueFor(ctx.dataDef()))
    }

    override fun enterOptMethDef(ctx: SimplexParser.OptMethDefContext) {}

    override fun exitOptMethDef(ctx: SimplexParser.OptMethDefContext) {
        setValueFor(ctx, getValueFor(ctx.methDef()))
    }

    override fun enterDataDef(ctx: SimplexParser.DataDefContext) {}

    override fun exitDataDef(ctx: SimplexParser.DataDefContext) {
        val name = ctx.ID().text
        val fields = getValueFor(ctx.params()) as List<TypedName>
        setValueFor(ctx, DataDefinition(name, fields, loc(ctx)))
    }

    override fun enterParams(ctx: SimplexParser.ParamsContext) {}

    override fun exitParams(ctx: SimplexParser.ParamsContext) {
        val params = ctx.param().map { getValueFor(it) as TypedName }
        setValueFor(ctx, params)
    }

    override fun enterVarDef(ctx: SimplexParser.VarDefContext) {}

    override fun exitVarDef(ctx: SimplexParser.VarDefContext) {
        val name = ctx.ID().text
        val type = ctx.type()?.let { getValueFor(it) as Type }
        val initValue = getValueFor(ctx.expr()) as Expr
        setValueFor(ctx, VariableDefinition(name, type, initValue, loc(ctx)))
    }

    override fun enterFunDef(ctx: SimplexParser.FunDefContext) {}

    override fun exitFunDef(ctx: SimplexParser.FunDefContext) {
        val name = ctx.ID().text
        val type = getValueFor(ctx.type()) as Type
        val localDefs = ctx.funDef().map { getValueFor(it) as FunctionDefinition }
        val params = ctx.params()?.let { getValueFor(it) as List<TypedName> }
        val body = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(
            ctx,
            FunctionDefinition(name, type, params ?: emptyList(), localDefs, body, loc(ctx)),
        )
    }

    override fun enterMethDef(ctx: SimplexParser.MethDefContext) {}

    override fun exitMethDef(ctx: SimplexParser.MethDefContext) {
        val type = getValueFor(ctx.target) as Type
        val result = getValueFor(ctx.result) as Type
        val name = ctx.ID().text
        val params = ctx.params()?.let { getValueFor(it) as List<TypedName> } ?: emptyList()
        val body = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(ctx, MethodDefinition(type, name, params, result, body, loc(ctx)))
    }

    override fun enterParam(ctx: SimplexParser.ParamContext) {}

    override fun exitParam(ctx: SimplexParser.ParamContext) {
        val name = ctx.ID().text
        val type = getValueFor(ctx.type()) as Type
        setValueFor(ctx, TypedName(name, type, loc(ctx)))
    }

    override fun enterTypes(ctx: SimplexParser.TypesContext) {}

    override fun exitTypes(ctx: SimplexParser.TypesContext) {
        val t = ctx.type().map { getValueFor(it) as Type }
        setValueFor(ctx, t)
    }

    override fun enterOptSimpleType(ctx: SimplexParser.OptSimpleTypeContext) {}

    override fun exitOptSimpleType(ctx: SimplexParser.OptSimpleTypeContext) {
        val name = ctx.ID().text
        setValueFor(ctx, Type.simple(name))
    }

    override fun enterOptMethodType(ctx: SimplexParser.OptMethodTypeContext) {}

    override fun exitOptMethodType(ctx: SimplexParser.OptMethodTypeContext) {
        val target = getValueFor(ctx.target) as Type
        val args = ctx.types()?.let { getValueFor(it) as List<Type> } ?: emptyList()
        val result = getValueFor(ctx.result) as Type
        setValueFor(ctx, Type.simpleMethod(target, args, result))
    }

    override fun enterOptVectorType(ctx: SimplexParser.OptVectorTypeContext) {}

    override fun exitOptVectorType(ctx: SimplexParser.OptVectorTypeContext) {
        val elementType = getValueFor(ctx.type()) as Type
        setValueFor(ctx, Type.vector(elementType))
    }

    override fun enterOptFunType(ctx: SimplexParser.OptFunTypeContext) {}

    override fun exitOptFunType(ctx: SimplexParser.OptFunTypeContext) {
        val args = ctx.types()?.let { getValueFor(it) as List<Type> } ?: emptyList()
        val result = getValueFor(ctx.type()) as Type
        setValueFor(ctx, Type.function(listOf(args), result))
    }

    override fun enterExprs(ctx: SimplexParser.ExprsContext) {}

    override fun exitExprs(ctx: SimplexParser.ExprsContext) {
        val exprs = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(ctx, exprs)
    }

    override fun enterExprUpdate(ctx: SimplexParser.ExprUpdateContext) {}

    override fun exitExprUpdate(ctx: SimplexParser.ExprUpdateContext) {
        val target = getValueFor(ctx.target) as Expr
        val field = ctx.ID().text
        val value = getValueFor(ctx.value) as Expr
        setValueFor(ctx, DataFieldUpdateExpr(target, field, value, loc(ctx)))
    }

    override fun enterExprParen(ctx: SimplexParser.ExprParenContext) {}

    override fun exitExprParen(ctx: SimplexParser.ExprParenContext) {
        val e = getValueFor(ctx.expr())
        setValueFor(ctx, e)
    }

    override fun enterExprMethod(ctx: SimplexParser.ExprMethodContext) {}

    override fun exitExprMethod(ctx: SimplexParser.ExprMethodContext) {
        val lhs = getValueFor(ctx.expr()) as Expr
        val meth = ctx.ID().text
        val args = ctx.exprs()?.let { getValueFor(it) as List<Expr> } ?: emptyList()
        setValueFor(ctx, MethodCallExpr(lhs, meth, args, loc(ctx)))
    }

    override fun enterExprCall(ctx: SimplexParser.ExprCallContext) {}

    override fun exitExprCall(ctx: SimplexParser.ExprCallContext) {
        val funExpr = getValueFor(ctx.expr()) as Expr
        val args = ctx.exprs()?.let { getValueFor(it) as List<Expr> } ?: emptyList()
        setValueFor(ctx, FunCallExpr(funExpr, args, loc(ctx)))
    }

    override fun enterExprComplex(ctx: SimplexParser.ExprComplexContext) {}

    override fun exitExprComplex(ctx: SimplexParser.ExprComplexContext) {
        val expr = getValueFor(ctx.complex()) as Expr
        setValueFor(ctx, expr)
    }

    override fun enterExprAdd(ctx: SimplexParser.ExprAddContext) {}

    override fun exitExprAdd(ctx: SimplexParser.ExprAddContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.addOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))
    }

    override fun enterExprPow(ctx: SimplexParser.ExprPowContext) {}

    override fun exitExprPow(ctx: SimplexParser.ExprPowContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.expOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))
    }

    override fun enterExprMult(ctx: SimplexParser.ExprMultContext) {}

    override fun exitExprMult(ctx: SimplexParser.ExprMultContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.multOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))
    }

    override fun enterExprUnary(ctx: SimplexParser.ExprUnaryContext) {}

    override fun exitExprUnary(ctx: SimplexParser.ExprUnaryContext) {
        val op = getValueFor(ctx.unaryOp()) as Operator
        val target = getValueFor(ctx.expr()) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(target), loc(ctx)))
    }

    override fun enterExprLogic(ctx: SimplexParser.ExprLogicContext) {}

    override fun exitExprLogic(ctx: SimplexParser.ExprLogicContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.logicOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))
    }

    override fun enterExprCompare(ctx: SimplexParser.ExprCompareContext) {}

    override fun exitExprCompare(ctx: SimplexParser.ExprCompareContext) {
        val l = getValueFor(ctx.l) as Expr
        val op = getValueFor(ctx.compareOp()) as Operator
        val r = getValueFor(ctx.r) as Expr
        setValueFor(ctx, OperatorExpr(op, listOf(l, r), loc(ctx)))
    }

    override fun enterExprSubscript(ctx: SimplexParser.ExprSubscriptContext) {}

    override fun exitExprSubscript(ctx: SimplexParser.ExprSubscriptContext) {
        val target = getValueFor(ctx.expr()[0]) as Expr
        val sub = getValueFor(ctx.expr()[1]) as Expr
        setValueFor(ctx, OperatorExpr(Operator.Subscript, listOf(target, sub), loc(ctx)))
    }

    override fun enterExprPrimary(ctx: SimplexParser.ExprPrimaryContext) {}

    override fun exitExprPrimary(ctx: SimplexParser.ExprPrimaryContext) {
        val e = getValueFor(ctx.primary())
        setValueFor(ctx, e)
    }

    override fun enterExprField(ctx: SimplexParser.ExprFieldContext) {}

    override fun exitExprField(ctx: SimplexParser.ExprFieldContext) {
        val e = getValueFor(ctx.expr()) as Expr
        val n = ctx.ID().text
        setValueFor(ctx, FieldRefExpr(e, n, loc(ctx)))
    }

    override fun enterComplexLet(ctx: SimplexParser.ComplexLetContext) {}

    override fun exitComplexLet(ctx: SimplexParser.ComplexLetContext) {
        val name = ctx.ID().text
        val type = ctx.type()?.let { getValueFor(it) as Type }
        val value = getValueFor(ctx.expr()) as Expr
        setValueFor(ctx, LetExpr(name, type, value, loc(ctx)))
    }

    override fun enterComplexCondExpr(ctx: SimplexParser.ComplexCondExprContext) {}

    override fun exitComplexCondExpr(ctx: SimplexParser.ComplexCondExprContext) {
        val conds = ctx.condClause().map { getValueFor(it) as Condition }
        val elseClause = getValueFor(ctx.expr()) as Expr
        setValueFor(ctx, CondExpr(conds, elseClause, loc(ctx)))
    }

    override fun enterComplexForExpr(ctx: SimplexParser.ComplexForExprContext) {}

    override fun exitComplexForExpr(ctx: SimplexParser.ComplexForExprContext) {
        val idx = ctx.ID().text
        val coll = getValueFor(ctx.expr().first()) as Expr
        val body = ctx.expr().drop(1).map { getValueFor(it) as Expr }
        setValueFor(ctx, LoopExpr(idx, coll, body, loc(ctx)))
    }

    override fun enterComplexDoExpr(ctx: SimplexParser.ComplexDoExprContext) {}

    override fun exitComplexDoExpr(ctx: SimplexParser.ComplexDoExprContext) {
        val body = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(ctx, BlockExpr(body, loc(ctx)))
    }

    override fun enterComplexLambdaExpr(ctx: SimplexParser.ComplexLambdaExprContext) {}

    override fun exitComplexLambdaExpr(ctx: SimplexParser.ComplexLambdaExprContext) {
        val resultType = getValueFor(ctx.type()) as Type
        val args = getValueFor(ctx.params()) as List<TypedName>
        val body = ctx.expr().map { getValueFor(it) as Expr }
        setValueFor(ctx, LambdaExpr(resultType, args, body, loc(ctx)))
    }


    override fun enterComplexWhileExpr(ctx: SimplexParser.ComplexWhileExprContext) {}

    override fun exitComplexWhileExpr(ctx: SimplexParser.ComplexWhileExprContext) {
        val cond = getValueFor(ctx.expr(0)) as Expr
        val body = ctx.expr().drop(1).map { getValueFor(it) as Expr }
        setValueFor(ctx, WhileExpr(cond, body, loc(ctx)))
    }

    override fun enterOptIdExpr(ctx: SimplexParser.OptIdExprContext) {}

    override fun exitOptIdExpr(ctx: SimplexParser.OptIdExprContext) {
        val id = ctx.ID().text
        val v = ctx.expr()?.let { getValueFor(it) as Expr }
        if (v == null) {
            setValueFor(ctx, VarRefExpr(id, loc(ctx)))
        } else {
            setValueFor(ctx, AssignmentExpr(id, v, loc(ctx)))
        }
    }

    override fun enterOptScopedId(ctx: SimplexParser.OptScopedIdContext) {
    }

    override fun exitOptScopedId(ctx: SimplexParser.OptScopedIdContext) {
        val scope = ctx.scope.text
        val name = ctx.name.text
        setValueFor(ctx, ScopedRefExpr(scope, name, loc(ctx)))
    }

    override fun enterOptVecExpr(ctx: SimplexParser.OptVecExprContext) {}

    override fun exitOptVecExpr(ctx: SimplexParser.OptVecExprContext) {
        val es = getValueFor(ctx.exprs()) as List<Expr>
        setValueFor(ctx, VectorExpr(es, loc(ctx)))
    }

    override fun enterOptDataExpr(ctx: SimplexParser.OptDataExprContext) {}

    override fun exitOptDataExpr(ctx: SimplexParser.OptDataExprContext) {
        val id = ctx.ID().text
        val args = getValueFor(ctx.exprs()) as List<Expr>
        setValueFor(ctx, DataExpr(id, args, loc(ctx)))
    }

    override fun enterOptLitInt(ctx: SimplexParser.OptLitIntContext) {}

    override fun exitOptLitInt(ctx: SimplexParser.OptLitIntContext) {
        setValueFor(ctx, LiteralExpr(ctx.LIT_INT().text.toInt(), loc(ctx)))
    }

    override fun enterOptLitFloat(ctx: SimplexParser.OptLitFloatContext) {}

    override fun exitOptLitFloat(ctx: SimplexParser.OptLitFloatContext) {
        setValueFor(ctx, LiteralExpr(ctx.LIT_FLOAT().text.toDouble(), loc(ctx)))
    }

    override fun enterOptLitStr(ctx: SimplexParser.OptLitStrContext) {}

    override fun exitOptLitStr(ctx: SimplexParser.OptLitStrContext) {
        val litStr = ctx.LIT_STRING().text.drop(1).dropLast(1)

        setValueFor(ctx, LiteralExpr(litStr, loc(ctx)))
    }

    override fun enterOptTrue(ctx: SimplexParser.OptTrueContext) {}

    override fun exitOptTrue(ctx: SimplexParser.OptTrueContext) {
        setValueFor(ctx, VarRefExpr("true", loc(ctx)))
    }

    override fun enterOptFalse(ctx: SimplexParser.OptFalseContext) {}

    override fun exitOptFalse(ctx: SimplexParser.OptFalseContext) {
        setValueFor(ctx, VarRefExpr("false", loc(ctx)))
    }


    override fun enterOpOptPow(ctx: SimplexParser.OpOptPowContext) {}

    override fun exitOpOptPow(ctx: SimplexParser.OpOptPowContext) {
        setValueFor(ctx, Operator.Pow)
    }

    override fun enterOpOptTimes(ctx: SimplexParser.OpOptTimesContext) {}

    override fun exitOpOptTimes(ctx: SimplexParser.OpOptTimesContext) {
        setValueFor(ctx, Operator.Times)
    }

    override fun enterOpOptSlash(ctx: SimplexParser.OpOptSlashContext) {}

    override fun exitOpOptSlash(ctx: SimplexParser.OpOptSlashContext) {
        setValueFor(ctx, Operator.Div)
    }

    override fun enterOpOptPercent(ctx: SimplexParser.OpOptPercentContext) {}

    override fun exitOpOptPercent(ctx: SimplexParser.OpOptPercentContext) {
        setValueFor(ctx, Operator.Mod)
    }

    override fun enterOpOptPlus(ctx: SimplexParser.OpOptPlusContext) {}

    override fun exitOpOptPlus(ctx: SimplexParser.OpOptPlusContext) {
        setValueFor(ctx, Operator.Plus)
    }

    override fun enterOpOptMinus(ctx: SimplexParser.OpOptMinusContext) {}

    override fun exitOpOptMinus(ctx: SimplexParser.OpOptMinusContext) {
        setValueFor(ctx, Operator.Minus)
    }

    override fun enterOpOptEqEq(ctx: SimplexParser.OpOptEqEqContext) {}

    override fun exitOpOptEqEq(ctx: SimplexParser.OpOptEqEqContext) {
        setValueFor(ctx, Operator.Eq)
    }

    override fun enterOpOptBangEq(ctx: SimplexParser.OpOptBangEqContext) {}

    override fun exitOpOptBangEq(ctx: SimplexParser.OpOptBangEqContext) {
        setValueFor(ctx, Operator.Neq)
    }

    override fun enterOpOptLt(ctx: SimplexParser.OpOptLtContext) {}

    override fun exitOpOptLt(ctx: SimplexParser.OpOptLtContext) {
        setValueFor(ctx, Operator.Lt)
    }

    override fun enterOpOptLe(ctx: SimplexParser.OpOptLeContext) {}

    override fun exitOpOptLe(ctx: SimplexParser.OpOptLeContext) {
        setValueFor(ctx, Operator.Le)
    }

    override fun enterOpOptGt(ctx: SimplexParser.OpOptGtContext) {}

    override fun exitOpOptGt(ctx: SimplexParser.OpOptGtContext) {
        setValueFor(ctx, Operator.Gt)
    }

    override fun enterOpOptGe(ctx: SimplexParser.OpOptGeContext) {}

    override fun exitOpOptGe(ctx: SimplexParser.OpOptGeContext) {
        setValueFor(ctx, Operator.Ge)
    }

    override fun enterOpOptAnd(ctx: SimplexParser.OpOptAndContext) {}

    override fun exitOpOptAnd(ctx: SimplexParser.OpOptAndContext) {
        setValueFor(ctx, Operator.And)
    }

    override fun enterOpOptOr(ctx: SimplexParser.OpOptOrContext) {}

    override fun exitOpOptOr(ctx: SimplexParser.OpOptOrContext) {
        setValueFor(ctx, Operator.Or)
    }

    override fun enterOpOptNot(ctx: SimplexParser.OpOptNotContext) {}

    override fun exitOpOptNot(ctx: SimplexParser.OpOptNotContext) {
        setValueFor(ctx, Operator.Not)
    }

    override fun enterOpUnaryNeg(ctx: SimplexParser.OpUnaryNegContext) {}

    override fun exitOpUnaryNeg(ctx: SimplexParser.OpUnaryNegContext) {
        setValueFor(ctx, Operator.Uminus)
    }

    override fun enterCondClause(ctx: SimplexParser.CondClauseContext) {}

    override fun exitCondClause(ctx: SimplexParser.CondClauseContext) {
        val cond = getValueFor(ctx.c) as Expr
        val value = getValueFor(ctx.v) as Expr
        setValueFor(ctx, Condition(cond, value))
    }

    override fun visitTerminal(node: TerminalNode) {}

    override fun visitErrorNode(node: ErrorNode) {}

    override fun enterEveryRule(ctx: ParserRuleContext) {}

    override fun exitEveryRule(ctx: ParserRuleContext) {}
}
