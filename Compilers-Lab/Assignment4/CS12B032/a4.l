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
<<EOF>>													 {yyterminate();}  
Integer|Node|NodeList|Character|Boolean|Stack|pQueue|Void|FileReader|Queue 	{ yylval.str= strdup(yytext);return TYPE;}
"return"												{ yylval.str= strdup(yytext);return RETURN;}											
"if"													{ yylval.str= strdup(yytext);return IF;}
"else"													{ yylval.str= strdup(yytext);return ELSE;}	
"locImport"												{ yylval.str= strdup(yytext);return LOCIMPORT;}
"stdImport"												{ yylval.str= strdup(yytext);return STDIMPORT;}
"define"												{ yylval.str= strdup(yytext);return DEFINE;}
"Macro"													{ yylval.str= strdup(yytext);return MACRO;}
"<"														{ yylval.str= strdup(yytext);return LT;}
">"														{ yylval.str= strdup(yytext);return GT;}				
"?"														{ yylval.str= strdup(yytext);return QUESTION;}
"(" 													{ yylval.str= strdup(yytext);return LPARA;}
"print"													{ yylval.str= strdup(yytext);return PRINT;}	
")"														{ yylval.str= strdup(yytext);return RPARA;}
","														{ yylval.str= strdup(yytext);return COMMA;}
"<="													{ yylval.str= strdup(yytext);return ASSIGNMENT;}
"+"														{ yylval.str= strdup(yytext);return ADD;}
"-"														{ yylval.str= strdup(yytext);return SUB;}
"<<~"													{ yylval.str= strdup(yytext);return MACROASSIGNMENT;}
"["													    { yylval.str= strdup(yytext);return LSQBRACKET;}
"]" 													{ yylval.str= strdup(yytext);return RSQBRACKET;}
"{" 													{ yylval.str= strdup(yytext);return LCBRACKET;}
"}" 													{ yylval.str= strdup(yytext);return RCBRACKET;}
"^"														{ yylval.str= strdup(yytext);return CARET;}
"%" 													{ yylval.str= strdup(yytext);return MOD;}
"for"												    { yylval.str= strdup(yytext);return FOR;}
"while" 												{ yylval.str= strdup(yytext);return WHILE;}
"FALSE" 												{ yylval.str= strdup(yytext);return FALSE;}
"TRUE" 													{ yylval.str= strdup(yytext);return TRUE;}
"&&" 													{ yylval.str= strdup(yytext);return AND;}
"~>>" 													{ yylval.str= strdup(yytext);return POINTER;}
"==" 													{ yylval.str= strdup(yytext);return EQUAL;}
"!=" 													{ yylval.str= strdup(yytext);return NOTEQUAL;}	
"!" 													{ yylval.str= strdup(yytext);return NOT;}
"null"  												{ yylval.str= strdup(yytext);return NULLVAL;}
"$"  													{ yylval.str= strdup(yytext);return DOLLAR;}
"allocate" 												{ yylval.str= strdup(yytext);return ALLOCATE;}
"."                                                     { yylval.str= strdup(yytext);return DOT;}
"break" 												{ yylval.str= strdup(yytext);return BREAK;}
"continue"             									{ yylval.str= strdup(yytext);return CONTINUE;}
"in" 													{ yylval.str= strdup(yytext);return IN;}
"/"														{ yylval.str= strdup(yytext);return DIV;}
"*"														{ yylval.str= strdup(yytext);return MUL;}
({letter}|_)({letter}|{digit}|_)*[.][hc]                { yylval.str= strdup(yytext);return LIBRARY;}
({letter}|_)({letter}|{digit}|_)*[+][+]       			{ yylval.str= strdup(yytext);return INCREMENT;}
({letter}|_)({letter}|{digit}|_)*[-][-]					{ yylval.str= strdup(yytext);return DECREMENT;}
-?[1-9]*{digit}*										{ yylval.str= strdup(yytext);return INTEGER;}
({letter}|_)({letter}|{digit}|_)*						{ yylval.str= strdup(yytext);return IDENTIFIER;}
\"[^"]*\"   	                						{ yylval.str= strdup(yytext);return QUOTEDID;}
\n														;
[ \t\r]													;	
"//"[^\r\n].*											;
.														{yyerror("ERROR");}
%%