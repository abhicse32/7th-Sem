%{
#include<stdio.h>
#include"y.tab.h"
#include<string.h>
extern int yydebug;
#ifndef YYSTYPE
#define YYSTYPE char*
#endif

%}

digit [0-9]
letter [A-Za-z]

%%
<<EOF>>										{yyterminate();} 
friendof|classmateof|roommateof				{ yylval.str= strdup(yytext); return RELATION;}
if 											{ yylval.str= strdup(yytext); return IF;}
endif 										{ yylval.str= strdup(yytext); return ENDIF;}
foreach 									{ yylval.str= strdup(yytext); return FOREACH;}
endforeach 									{ yylval.str= strdup(yytext); return ENDFOREACH;}
["$"][a-zA-Z0-9]*								{ yylval.str= strdup(yytext); return VAR;}
[a-zA-Z][a-zA-Z0-9]*						{ yylval.str= strdup(yytext); return NAME;}
\n											;
[ \t]										;	
%%