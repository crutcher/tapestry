grammar SizeExpr;


eqnProgram
    : eqn EOF;

eqn
    : lhs=expr op=(GT | GE | EQ | NE | LT | LE) rhs=expr
    ;

exprProgram
    : e=expr EOF;

expr
    : lhs=expr op=(TIMES | DIV | MOD) rhs=expr # BinOpExpr
    | lhs=expr op=(PLUS | MINUS) rhs=expr # BinOpExpr
    | lhs=expr op=POW rhs=expr # BinOpExpr
    | op=MINUS e=expr # NegateExpr
    | LPAREN e=expr RPAREN # ParensExpr
    | a=atom # AtomExpr
    ;

atom
    : val=integer # NumberExpr
    | id=dotted_id # IdentifierExpr
    ;

dotted_id
    : ID (DOT ID)*
    ;

integer
    : INTEGER_LITERAL
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

GT : '>';
LT : '<';
EQ : '=';
GE : '>=';
LE : '<=';
NE : '!=';

COMMA : ',';
LPAREN : '(';
RPAREN : ')';

PLUS : '+';
MINUS : '-';

TIMES : '*';
DIV : '/';
MOD : '%';

POW : '^';

DOT : '.';

INTEGER_LITERAL
    : [0-9]+
    ;

WS
    : [ \r\n\t]+ -> skip
    ;
