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
| dataDef #optDataDef
| methDef #optMethDef
;

dataDef:
   'data' ID '{' params '}'
;

params:
   param (',' param)*
;

varDef:
  'let' ID  (':' type)?  '=' expr
;

funDef:
   'fun' ID '(' params? ( ';' kwParams )? ')' ':' type '{'
    funDef*
    exprs
  '}'
;

kwParams:
  kwParam (',' kwParam)*
;

kwParam:
  ID ':' type '=' expr
;


 methDef:
    'meth' target=type '->' ID '(' params? ( ';' kwParams )? ')' ':' result=type '{'
       exprs
    '}'
;

param:
    ID  ':' type
;

types:
   type (',' type)*
;

paramSignature:
   ( types )? ( ';' kwParams)?
;

type:
  ID #optSimpleType
| '[' type ']' #optVectorType
| '(' paramSignature ')' ':' type # optFunType
| target=type '->' '(' paramSignature ')' ':' result=type #optMethodType
;

exprs:
   ( expr ( ';' )? )*
;

exprList:
  expr (',' expr)*
;

args:
  ( exprList )? (';' kwArgs )?
;

kwArgs:
  kwArg ( ',' kwArg )*
;

kwArg:
  ID '=' expr
;

expr:
  '(' expr ')' #exprParen
| primary  #exprPrimary
| complex  #exprComplex
| expr '->' ID '(' args ')' #exprMethod
| expr '(' args ')' #exprCall
| expr '[' expr ']' #exprSubscript
| expr '.' ID  #exprField
| target=expr '.' ID ':=' value=expr #exprUpdate
| unaryOp  expr #exprUnary
| l=expr expOp r=expr #exprPow
| l=expr multOp r=expr #exprMult
| l=expr addOp r=expr #exprAdd
| l=expr compareOp r=expr #exprCompare
| l=expr logicOp r=expr #exprLogic
;

complex:
  'let' ID (':' type)? '=' expr #complexLet
| 'if' condClause ( 'elif' condClause )* 'else' expr  #complexCondExpr
| 'for' ID 'in' expr '{' expr+ '}' #complexForExpr
| '{' expr+ '}'  #complexDoExpr
| 'lambda'  '(' params ')' ':' type '{' expr+ '}' #complexLambdaExpr
| 'while' expr '{' expr+ '}' #complexWhileExpr
;

primary:
  ID (':=' expr)? #optIdExpr
| '['  exprList   ']' #optVecExpr
| '#' ID '(' exprList ')' #optDataExpr
| LIT_INT #optLitInt
| LIT_FLOAT #optLitFloat
| LIT_STRING #optLitStr
| 'true' #optTrue
| 'false' #optFalse
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
    '(' c=expr ')' v=expr
;

product:
   'produce' '(' LIT_STRING ')' '{' expr+
   '}'
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
