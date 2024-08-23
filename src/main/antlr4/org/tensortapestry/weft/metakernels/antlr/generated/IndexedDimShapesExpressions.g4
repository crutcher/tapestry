grammar IndexedDimShapesExpressions;

prog
   : patternList EOF
   ;

patternList
   : LBRACK patternSequence RBRACK
   ;

patternSequence
   : pattern (COMMA pattern)*
   ;

pattern
   : name = qualName EQUALS LPAREN patternSequence RPAREN # GroupPattern
   | name = qualName ELLIPSIS # EllipsisPattern
   | name = qualName # SingleDimPattern
   ;

qualName
   : name = identifier LBRACK index = identifier RBRACK # IndexName
   | name = identifier # GlobalName
   ;

identifier
   : DOLLAR ID (DOT ID)*
   ;

ELLIPSIS
   : '...'
   ;

DOLLAR
   : '$'
   ;

DOT
   : '.'
   ;

LPAREN
   : '('
   ;

RPAREN
   : ')'
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

