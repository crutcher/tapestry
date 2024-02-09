grammar DimShapePattern;

prog : patternList EOF;

patternList : LBRACK pattern (COMMA pattern)* RBRACK ;

pattern
     : name=ID EQUALS parts=patternList # GroupPattern
     | STAR name=ID # StarPattern
     | PLUS name=ID # PlusPattern
     | name=ID # SingleDim
     ;

LBRACK : '[' ;
RBRACK : ']' ;
COMMA : ',' ;
EQUALS : '=' ;
PLUS : '+' ;
STAR : '*' ;

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

WS
    : [ \r\n\t]+ -> skip
    ;
