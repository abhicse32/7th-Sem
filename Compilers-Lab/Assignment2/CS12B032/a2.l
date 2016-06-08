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
letter [a-zA-Z]

%%
<<EOF>>										{yyterminate();} 
Table|Column|Row				            { yylval.str= strdup(yytext); /*printf("%s",yytext);*/return TYPE;}
";"											{ yylval.str= strdup(yytext); /*printf("%s",yytext);*/return SEMICOLON;}
"."											{ yylval.str= strdup(yytext); /*printf("%s",yytext);*/return DOT;}
"(" 										{ yylval.str= strdup(yytext); /*printf("%s",yytext);*/return LPARA;}
")"											{ yylval.str= strdup(yytext); /*printf("%s",yytext);*/return RPARA;}
","											{ yylval.str= strdup(yytext); /*printf("%s",yytext);*/return COMMA;}
({letter}|_)({letter}|{digit}|_)*			{ yylval.str= strdup(yytext); /*printf("%s",yytext);*/return IDENTIFIER;}
\"[^"\n\\]*[\"]   	                        { yylval.str= strdup(yytext); return QUOTEDID;}     
\n											;
[ \t\r]										;	
.											{yyerror("ERROR");}
%%