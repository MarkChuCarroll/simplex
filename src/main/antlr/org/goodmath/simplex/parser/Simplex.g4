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

model:  'model' ID 'is'
    d=def+
    r=render+
    'end'
;

def:
  varDef #optVarDef
| funDef #optFunDef
| tupleDef #optTupleDef
;

tupleDef:
    'tup' ID '{' params '}'
;

varDef:
  'var' ID ( ':' type )? '=' expr
;

funDef:
   'fun' ID '(' params ')' 'do'
    defs
    exprs
   'end'
 ;

defs:
    def*
;

param:
    ID ( ':'  type  )?
;

params:
   param (',' param )*
;

type:
   ID #optSimpleType
|  '[' type ']' #optVectorType
;

expOp: '^';
multOp: '*' #opOptStar
| '/' #opOptSlash
| '%' #opOptPercent;

addOp: '+' #opOptPlus
  | '-' #opOptMinus
  ;

compareOp: '==' #opOptEqEq
| '!=' #opOptBangEq
| '<'  #opOptLt
| '<=' #opOptLe
| '>' #opOptGt
| '>=' #opOptGe
;

logicOp: 'and' #opOptAnd
| 'or' #opOptOr
;

prefixOp: 'not' #opOptNot | '-' #opOptUnaryMinus;

exprs:
   ( expr ';' )*
;

expr:
  '(' expr ')' #exprOptParens
| suffixExpr #exprOptSuffix
;

suffixExpr:
   binaryExpr suffix*
;

suffix:
        '(' exprList ')' #suffixOptCall
   |    '[' expr ']' #suffixOptSubscript
   |    '.' ID  #suffixOptField
   |    '->' ID '(' exprList ')' #suffixOptMeth
;


binaryExpr:
    l=binaryExpr expOp r=multExpr #binOptTwo
|  multExpr #binOptOne
;

multExpr:
   l=multExpr  op=multOp r=addExpr #multOptTwo
| addExpr #multOptOne
;

addExpr:
   l=addExpr op=addOp r=compareExpr #addOptTwo
| compareExpr #addOptOne
;

compareExpr:
   l=compareExpr  op=compareOp r=logicExpr #compOpTwo
| logicExpr #compOpOne
;

logicExpr:
   l=logicExpr op=logicOp r=baseExpr #logOpTwo
| baseExpr #logOptOne
;

baseExpr:
  ID #baseOptId
|  condExpr #baseOptCond
|  loopExpr #baseOptLoop
|  tupleExpr #baseOptTuple
|  updateExpr #baseOptUpdate
|  withExpr #baseOptWith
|  literalExpr #baseOptLiteral
|  vectorExpr #baseOptVector
|  doExpr #baseOptDo
| letExpr #baseOptLet
;

exprList:
   e=expr (',' f=expr )*
;

doExpr: 'do' ( expr ';' )* 'end'
;

letExpr: 'let' bindings 'in' ( expr ';' )* 'end'
;

bindings:
   binding (',' binding)*
;

binding:
   ID ( ':' type )? '=' expr
;

vectorExpr:
   '['  exprList?   ']'
;

withExpr:
   'with' tup=expr 'do' body=exprs 'end'
;

updateExpr:
   'update' tup=expr 'set'
       updates
;

tupleExpr:
   '#{' ID ':' exprList '}'
;

updates:
   update (',' update)*
;

update:
   ID  '=' expr
;

loopExpr:
   'for' ID 'in' coll=expr 'do' body=exprs
;


condExpr:
   'if' condClause
   ( 'elif' condClause )*
   'else' e=expr
;

condClause:
   c=expr 'then' v=expr
;

literalExpr:
    LIT_INT #optInt
  | LIT_FLOAT #optFloat
  | LIT_STRING #optString
  | 'true' #optTrue
  | 'false' #optFalse
;


render:
   'render' ID 'do'
   exprs
   'end'
;

fragment
IDCHAR : // Characters that are usable as part of both UNAME and LNAME after
        // the first character.
   [\p{Ll}\p{Lu}\p{Pd}\p{Sm}\p{Sc}\p{So}/*_#%^&?!]
;

ID: IDCHAR+;

LIT_STRING :  '"' (ESC | ~["\\])* '"' ;

fragment ESC :   '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

LIT_INT : ('-')?[0-9]+ ;

LIT_FLOAT
    :   '-'? INT '.' INT EXP?   // 1.35, 1.35E-9, 0.3, -4.5
    |   '-'? INT EXP            // 1e10 -3e4
    |   '-'? INT                // -3, 45
    ;

fragment INT :   '0' | [1-9] [0-9]* ; // no leading zeros
fragment EXP :   [Ee] [+\-]? INT ;

// Comments and whitespace
COMMENT : '/*' .*? '*/' -> channel(HIDDEN) ;
LINE_COMMENT : '//' ~'\n'* '\n' -> channel(HIDDEN) ;
WS : [ \t\n\r]+ -> channel(HIDDEN) ;

