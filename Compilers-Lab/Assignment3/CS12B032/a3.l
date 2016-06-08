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
<<EOF>>								{yyterminate();}  
"int"				        		{ yylval.str= strdup(yytext); return TYPE;}
";"									{ yylval.str= strdup(yytext); return SEMICOLON;}
"(" 								{ yylval.str= strdup(yytext); return LPARA;}
"printf"							{ yylval.str= strdup(yytext); return PRINT;}	
")"									{ yylval.str= strdup(yytext); return RPARA;}
","									{ yylval.str= strdup(yytext); return COMMA;}
"="									{ yylval.str= strdup(yytext); return ASSIGNMENT;}
"+"									{ yylval.str= strdup(yytext); return ADD;}
"-"									{ yylval.str= strdup(yytext); return SUB;}
"void"								{ yylval.str= strdup(yytext); return VOID;}
"{"									{ yylval.str= strdup(yytext); return LCBRACES;}
"}"									{ yylval.str= strdup(yytext); return RCBRACES;}
({digit})*							{ yylval.str= strdup(yytext); return INTEGER;}
({letter}|_)({letter}|{digit}|_)*	{ yylval.str= strdup(yytext); return IDENTIFIER;}
\"[^"]*\"   	                	{ yylval.str= strdup(yytext); return QUOTEDID;}
\n									;
[ \t\r]								;	
"//"[^\r\n].*						;
.									{yyerror("ERROR");}
%%