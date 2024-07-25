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

model:  '(def-model' ID
    d=def+
    r=render+
  ')'
;

def:
  varDef #optVarDef
| funDef #optFunDef
| tupleDef #optTupleDef
;

tupleDef:
    '(def-tup' ID '(' param* ')' ')'
;

varDef:
  '(def-var' ID ( '<' type '>' )? expr ')'
;

funDef:
   '(def-fun' ID '(' param* ')'
    def*
    expr*')'
 ;

param:
    ID ( '<' type '>' )?
;

type:
   ID #optSimpleType
|  '[' type ']' #optVectorType
;

expr:
  '(let' '(' local* ')'
      expr+ ')' #optLetExpr
| '(cond' condClause+ ( '(else' expr ')' ) ')' #optCondExpr
| '(for' ID expr expr+ ')' #optForExpr
| '(do' expr+ ')'  #optDoExpr
| '(update' target=expr update+ ')' #optUpdateExpr
| '(with' focus=expr body=expr+ ')' #optWithExpr
| '(->' ID expr+ ')' #optMethodExpr
| '(' op expr+ ')' #optOpExpr
| '(' expr* ')'  #optFuncallExpr
| ID #optIdExpr
| '['  expr+   ']' #optVecExpr
| '#' ID '(' expr* ')' #optTupleExpr
| LIT_INT #optLitInt
| LIT_FLOAT #optLitFloat
| LIT_STRING #optLitStr
| 'true' #optTrue
| 'false' #optFalse
;

update: '(' ID expr ')';

local:
    '(' ID ( '<' type '>'  ) expr ')'
;

op:  '^' #opOptPow
 | '*' # opOptTimes
| '/' #opOptSlash
| '%' #opOptPercent
| '+' #opOptPlus
| '-' #opOptMinus
| '==' #opOptEqEq
| '!=' #opOptBangEq
| '<'  #opOptLt
| '<=' #opOptLe
| '>' #opOptGt
| '>=' #opOptGe
| 'and' #opOptAnd
| 'or' #opOptOr
| 'not' #opOptNot
;

condClause:
   '{' c=expr v=expr  '}'
;

render:
   '(render' ID
   expr* ')'
;

fragment IDCHAR :  [A-Za-z_];

ID: IDCHAR ( [A-Za-z_0-9] )*;

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

