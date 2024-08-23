grammar DimShapePattern;

prog
   : patternList EOF
   ;

patternList
   : LBRACK pattern (COMMA pattern)* RBRACK
   ;

pattern
   : name = identifier EQUALS parts = patternList # GroupPattern
   | STAR name = identifier # StarPattern
   | PLUS name = identifier # PlusPattern
   | name = identifier # SingleDim
   ;

identifier
   : ID # GlobalID
   | DOLAR DOT ID # InstanceID
   ;

DOLAR
   : '$'
   ;

DOT
   : '.'
   ;

LBRACK
   : '['
   ;

RBRACK
   : ']'
   ;

COMMA
   : ','
   ;

EQUALS
   : '='
   ;

PLUS
   : '+'
   ;

STAR
   : '*'
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

WS
   : [ \r\n\t]+ -> skip
   ;

