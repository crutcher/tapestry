grammar SelectorSpec;

expression
    : p1Expression;


p1Expression
    : p2Expression ((PLUS | MINUS) p2Expression)*
    ;

p2Expression
    : p3Expression ((TIMES | DIV) p3Expression)*
    ;

p3Expression
    : signedAtom (POW signedAtom)*
    ;

signedAtom
    : op=PLUS val=signedAtom
    | op=MINUS val=signedAtom
    | atom
    ;

atom
    : variable
    | number
    | LPAREN expression RPAREN
    ;

variable : VARIABLE;

number
    : INTEGER_LITERAL
    ;

VARIABLE
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


LPAREN : '(';
RPAREN : ')';

PLUS : '+';
MINUS : '-';
TIMES : '*';
DIV : '/';
POW : '^';

INTEGER_LITERAL
    : [0-9]+
    ;

WS
    : [ \r\n\t]+ -> skip
    ;