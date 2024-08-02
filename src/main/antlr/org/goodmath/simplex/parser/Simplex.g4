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
grammar Simplex;

@header {
package org.goodmath.simplex.parser;
}

model:
    d=def+

    product+
;

def:
  varDef #optVarDef
| funDef #optFunDef
| tupleDef #optTupleDef
| methDef #optMethDef
;

tupleDef:
   'tup' ID '(' params ')'
;

params:
   param (',' param)*
;

varDef:
  'val' ID  ':' type  '=' expr
;

funDef:
   'fun' ID '(' params? ')' ':' type 'do'
    def*
    expr*
  'end'
 ;

 methDef:
    'meth' target=type '->' ID '(' params? ')' ':' result=type 'do'
       expr+
    'end'
;

param:
    ID  ':' type
;

types:
   type (',' type)*
;

type:
   ID #optSimpleType
|  '[' type ']' #optVectorType
| '(' types? ')' ':' type # optFunType
| target=type '->' '(' types? ')' ':' result=type #optMethodType
;

bindings:
   binding ( ',' binding )*
;


exprs:
  expr (',' expr)*
;

expr:
  '(' expr ')' #exprParen
| primary  #exprPrimary
| complex  #exprComplex
| expr '->' ID '(' exprs? ')' #exprMethod
| expr '(' exprs? ')' #exprCall
| expr '[' expr ']' #exprSubscript
| expr '.' ID #exprField

| unaryOp  expr #exprUnary

| l=expr expOp r=expr #exprPow

| l=expr multOp r=expr #exprMult

| l=expr addOp r=expr #exprAdd

| l=expr compareOp r=expr #exprCompare

| l=expr logicOp r=expr #exprLogic
;

complex:
  'let' bindings 'in'
      expr+ 'end' #complexLetExpr
| 'if' condClause ( 'elif' condClause )* 'else' expr 'end' #complexCondExpr
| 'for' ID 'in' expr 'do' expr+ 'end' #complexForExpr
| 'do' expr+ 'end'  #complexDoExpr
| 'lambda' ':' type '(' params ')' 'do' expr+ 'end' #complexLambdaExpr
| 'with' focus=expr 'do' body=expr+ 'end' #complexWithExpr
;

primary:
  ID (':=' expr)? #optIdExpr
| '['  exprs   ']' #optVecExpr
| '#' ID '(' exprs ')' #optTupleExpr
| LIT_INT #optLitInt
| LIT_FLOAT #optLitFloat
| LIT_STRING #optLitStr
| 'true' #optTrue
| 'false' #optFalse
;

binding:
   ID ':' type  '=' expr
;

expOp:
    '^' #opOptPow
;

multOp:
 '*' # opOptTimes
| '/' #opOptSlash
| '%' #opOptPercent
;

addOp:
  '+' #opOptPlus
| '-' #opOptMinus
;

compareOp:
  '==' #opOptEqEq
| '!=' #opOptBangEq
| '<'  #opOptLt
| '<=' #opOptLe
| '>' #opOptGt
| '>=' #opOptGe
;

logicOp:
  'and' #opOptAnd
| 'or' #opOptOr
;

unaryOp:
 'not' #opOptNot
| '-' #opUnaryNeg
;


condClause:
    c=expr 'then' v=expr
;

product:
   'produce' '(' ID ')' 'do' expr+
   'end'
;


fragment IDCHAR :  [A-Za-z_];

ID: IDCHAR ( [A-Za-z_0-9] )*;

LIT_STRING :  '"' (ESC | ~["\\])* '"' ;

fragment ESC :   '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

LIT_INT : [0-9]+ ;

LIT_FLOAT
    :    INT '.' INT EXP?   // 1.35, 1.35E-9, 0.3, -4.5
    |    INT EXP            // 1e10 -3e4
    |    INT                // -3, 45
    ;

fragment INT :   '0' | [1-9] [0-9]* ; // no leading zeros
fragment EXP :   [Ee] [+\-]? INT ;

// Comments and whitespace
COMMENT : '/*' .*? '*/' -> channel(HIDDEN) ;
LINE_COMMENT : '//' ~'\n'* '\n' -> channel(HIDDEN) ;
WS : [ \t\n\r]+ -> channel(HIDDEN) ;

