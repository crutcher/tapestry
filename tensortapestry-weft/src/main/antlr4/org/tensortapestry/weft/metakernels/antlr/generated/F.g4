grammar F;

prog
   : expr EOF
   ;

expr
   : lhs = expr op = (TIMES | DIV | MOD) rhs = expr # BinOpExpr
   | lhs = expr op = (PLUS | MINUS) rhs = expr # BinOpExpr
   | lhs = expr op = POW rhs = expr # BinOpExpr
   | op = MINUS e = expr # NegateExpr
   | expr ELLIPSIS # EllipsisExpr
   | expr select # SelectExpr
   | LBRACKET expr (COMMA expr)* RBRACKET # ListExpr
   | LPAREN e = expr RPAREN # ParensExpr
   | atom # AtomExpr
   ;

select
   : LBRACKET expr RBRACKET # IndexSelect
   | LBRACKET expr COLON RBRACKET # SliceToSelect
   | LBRACKET COLON expr RBRACKET # SliceFromSelect
   | LBRACKET expr COLON expr RBRACKET # SliceSelect
   ;

atom
   : val=integer # NumberAtom
   | id=variable # VariableAtom
   ;

integer
    : INTEGER_LITERAL
    ;

variable
   : DOLLAR id=qual_id
   ;

qual_id
   : ID (DOT ID)*
   ;

ID
   : VALID_ID_START VALID_ID_CHAR*
   ;

fragment VALID_ID_START
   : 'a' .. 'z'
   | 'A' .. 'Z'
   | '_'
   ;

fragment VALID_ID_CHAR
   : VALID_ID_START
   | '0' .. '9'
   ;

ELLIPSIS
   : '...'
   ;

DOLLAR
   : '$'
   ;

GT
   : '>'
   ;

LT
   : '<'
   ;

EQ
   : '='
   ;

GE
   : '>='
   ;

LE
   : '<='
   ;

NE
   : '!='
   ;

COMMA
   : ','
   ;

COLON
   : ':'
   ;

LBRACKET
   : '['
   ;

RBRACKET
    : ']'
    ;

LPAREN
   : '('
   ;

RPAREN
   : ')'
   ;

PLUS
   : '+'
   ;

MINUS
   : '-'
   ;

TIMES
   : '*'
   ;

DIV
   : '/'
   ;

MOD
   : '%'
   ;

POW
   : '^'
   ;

DOT
   : '.'
   ;

INTEGER_LITERAL
   : [0-9]+
   ;

WS
   : [ \r\n\t]+ -> skip
   ;

