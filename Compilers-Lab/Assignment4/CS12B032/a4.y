%{
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include "list.h"
#define YYDEBUG 1

extern FILE * yyin;
extern FILE* yyout;
void yyerror(const char *str);
bool importFlag;
char* comparisonOp;
char* type;
char* getType(char*);
int caretCounter;
void translateAllocation(char* type);
void printStar(int number);
bool fscanfOn;
bool firstParam;
char* objectVars;
void printTC(char* type, int counter);
%}

%union{
	char* str;
}

%start Goal
  
%token IDENTIFIER STDIMPORT LOCIMPORT DEFINE MACRO LT GT LIBRARY
%token MACROASSIGNMENT LSQBRACKET RSQBRACKET LCBRACKET RCBRACKET QUESTION CARET INTEGER
%token MOD ASSIGNMENT COMMA FOR WHILE ELSE IF VOID RETURN
%token FALSE TRUE AND POINTER  EQUAL NOTEQUAL NOT NULLVAL DOLLAR ALLOCATE INCREMENT DECREMENT 
%token DOT BREAK CONTINUE LPARA RPARA 
%token ADD SUB DIV MUL TYPE PRINT QUOTEDID IN 

%%			/* Grammar rules to be specified in this section*/
			Goal: ImportMacroStatements FunctionsDefinition
			      ;
				 
	ImportMacroStatements: |ImportStatement ImportMacroStatements
			       |MacroDefinition ImportMacroStatements
			       ;
				
		ImportStatement:  | LOCIMPORT LT LIBRARY{importFlag=false; printf("#include\"%s\"\n",$3.str);} Libraries GT{importFlag=true;}
				  | STDIMPORT LT LIBRARY Libraries{printf("#include <%s>\n",$3.str);} GT
				  ;
		
		    Libraries: | COMMA LIBRARY{
				    if(importFlag)
					    printf("#include <%s>\n",$2.str);
				    else 
					printf("#include\"%s\"\n",$2.str);
				} Libraries
			       ;
			     
	       MacroDefinition: | MACRO IDENTIFIER MACROASSIGNMENT INTEGER{printf("#define %s   %s\n",$2.str, $4.str);}
				;
	       
	      FunctionsDefinition:
				 |Type IDENTIFIER LSQBRACKET RSQBRACKET MOD{printTC(type, caretCounter);
				  printf(" %s(){\n",$2.str);} Statements MOD{printf("}\n");} FunctionsDefinition
				    
				 | Type IDENTIFIER LSQBRACKET{printTC(type,caretCounter);printf("%s(",$2.str);}
				    Type IDENTIFIER{printTC(type, caretCounter);printf(" %s",$6.str);} 
				   Parameters RSQBRACKET{printf("){\n");} MOD Statements MOD{printf("}\n");} FunctionsDefinition
				 ;
	
		     Parameters: | COMMA{printf(",");} Type IDENTIFIER{printTC(type, caretCounter);printf(" %s",$4.str);} Parameters
				 ;
		
		    Statements: 
		    	        |VarDeclaration Statements
				|WhileStatement Statements
				|ForStatement Statements
				|PrintStatement Statements
				|AssignmentStatement Statements
				|IfStatement Statements
				|ElseStatement Statements
				|ReturnStatement Statements
				|FunctionCall Question Statements
				|BreakStatement Statements
				|ContinueStatement Statements
				;
		
		BreakStatement: |BREAK {printf("break");} Question	
				;
				
	     ContinueStatement:  |CONTINUE {printf("continue");} Question 
				 ;
				 
	       VarDeclaration:  |Type IDENTIFIER{printTC(type, caretCounter);printf(" %s",$2.str);} VarDecTimeAssignment 
				    VarList Question
				;
			
	  VarDecTimeAssignment:  | ASSIGNMENT{printf(" = ");} Expression
				 ;
		
		      VarList: |COMMA IDENTIFIER{printf(",%s",$2.str);} VarDecTimeAssignment VarList
			       ;
	    
		WhileStatement:  WHILE LCBRACKET{printf("while(");} ConditionalExpression LoopConditionals 
				 RCBRACKET{printf("){\n");} DOLLAR Statements {printf("}\n");}DOLLAR
				 ;
				
		LoopConditionals:  
				   |ComparisonOperators{printf(" %s ",comparisonOp);} ConditionalExpression LoopConditionals
				   |{}
				   ;
		
		  ForStatement:  FOR LCBRACKET IDENTIFIER{printf("for(%s =",$3.str);} ComparisonOperators LPARA
				 PrimaryExpr COMMA{printf("; ");
				     if(!strcmp(comparisonOp,"!"))
					 printf("%s%s",comparisonOp,$3.str);
				     else printf("%s %s",$3.str, comparisonOp);
				} PrimaryExpr
				    COMMA{printf("; ");
					if(!strcmp(comparisonOp,"<"))
					    printf("%s = %s +",$3.str, $3.str);
					else 
					    printf("%s = %s - ",$3.str,$3.str);
				    } PrimaryExpr RPARA
				    RCBRACKET DOLLAR{printf("){\n");} Statements {printf("}\n");}DOLLAR
				 ;
				
	   ComparisonOperators: |LT       {comparisonOp=$1.str;}
				|GT	 {comparisonOp=$1.str;}
				|EQUAL   {comparisonOp=$1.str;}
				|NOTEQUAL{comparisonOp=$1.str;}
				|NOT	 {comparisonOp=$1.str;}
				|AND	 {comparisonOp=$1.str;}
				;
				
	 ConditionalExpression:	|LPARA{printf("(");} Expression ComparisonOperators{printf("%s",comparisonOp);} 
				    Expression {printf(")");}RPARA
				|LPARA{printf("(");} ComparisonOperators{printf("%s",comparisonOp);} Expression {printf(")");}RPARA
				;
				
	          PrimaryExpr: INTEGER          {printf("%s",$1.str);}          
			       |IDENTIFIER      {if(fscanfOn && !firstParam)
						    printf("&%s",$1.str);
				   else 
					printf("%s",$1.str);
			    }
			       |ArrayLookUp	
			       |PointedToValue  
			       |INCREMENT	{printf("%s",$1.str);}
			       |DECREMENT	{printf("%s",$1.str);}
			       |DottedVar
			       ;
			       
		  IfStatement:  |IF LCBRACKET{printf("if(");}ConditionalExpression LoopConditionals RCBRACKET{printf("){\n");} 
				    DOLLAR Statements DOLLAR{printf("}\n");}
				;
				
		ElseStatement:  |ELSE{printf("else ");} IfStatement
				|ELSE DOLLAR{printf("else{\n");} Statements DOLLAR{printf("}\n");}
				;
		
		   FunctionCall: 
				|IDENTIFIER LSQBRACKET RSQBRACKET{printf("%s()",$1.str);}
			        |IDENTIFIER LSQBRACKET{printf("%s(",$1.str);if(!strcmp("fscanf",$1.str))
				   fscanfOn=true;firstParam=true;
			    } PassParameter{firstParam=false;} 
				    Params{printf(")"); fscanfOn=false;} RSQBRACKET
			       ;
			
		PrintStatement:  |PRINT LSQBRACKET QUOTEDID{printf("printf(%s",$3.str);} Params RSQBRACKET{printf(")");} Question
				 ;
	
			Params:  |COMMA{printf(",");} PassParameter Params
				 ;
		
		  PassParameter: Expression
				 |QUOTEDID{printf("%s",$1.str);}
				 ;
				 
		   DottedVar:  ArrayLookUp DOT IDENTIFIER{printf(".%s",$3.str);}
				| IDENTIFIER DOT IDENTIFIER{printf("%s.%s",$1.str,$3.str);}
				;
			
	   AssignmentStatement:  | IDENTIFIER ASSIGNMENT{printf("%s = ",$1.str);} Expression Question
				 | ArrayLookUp ASSIGNMENT{printf(" = ");} Expression Question
				 | DottedVar ASSIGNMENT{printf(" = ");} Expression Question
				 ;
				 
	           ArrayLookUp: IDENTIFIER LT {printf("%s[",$1.str);}PrimaryExpr {printf("]");}GT
				;
				
		PointedToValue: IDENTIFIER POINTER IDENTIFIER{printf("%s ->%s",$1.str,$3.str);} 
				| ArrayLookUp POINTER IDENTIFIER{printf(" ->%s",$3.str);}
				;
				 
	   AllocationExpresion: ALLOCATE Type{translateAllocation(type);} LT PrimaryExpr {printf(")");}GT
				; 
				
		    Expression: AllocationExpresion|PrimaryExpr ArithmaticExpression| FunctionCall
		                |NULLVAL{printf("NULL");}|TRUE{printf("true");}|FALSE{printf("false");}
				 ;
		
	  ArithmaticExpression: |ADD{printf("+");} PrimaryExpr ArithmaticExpression
				|SUB{printf("-");} PrimaryExpr ArithmaticExpression
				|MUL{printf("*");} PrimaryExpr ArithmaticExpression
				|DIV{printf("/");} PrimaryExpr ArithmaticExpression
				;

		ReturnStatement:  RETURN{printf("%s ",$1.str);} Expression Question
				  ;
			
		          Type: TYPE   {caretCounter=0; type=getType($1.str);} 
				|TYPE  {caretCounter=0; type=getType($1.str);} Caret
				;
				
			 Caret:  |CARET{caretCounter++;} Caret
				 ;
				 
Question: QUESTION{printf(";\n");}
		  
%%

int yywrap(){
        return 1;
}

void yyerror(const char *str){
        fprintf(yyout,"ERROR");
        exit(0);
}

main(int argc, char* argv[]){
	importFlag=true;
	comparisonOp= NULL;
	type=NULL;
	caretCounter=0;
	fscanfOn=false;
	firstParam=false;
	objectVars=NULL;
	if(argc > 1){
	   yyin =fopen(argv[1],"r");
	}
	else yyin = stdin;

	if(argc > 2){
		yyout= fopen(argv[2],"w");
	}else
		yyout= stdout;
	if(yyin!=NULL)
	    yyparse();
	else
	    yyerror("ERROR\n");
	
 }

 
 char* getType(char *type){
     char* retType=NULL;
     if(!strcmp(type,"Integer"))
	 retType= "int";
     else if(!strcmp(type,"Character"))
	 retType="char";
     else if(!strcmp(type,"Stack") || !strcmp(type,"Node") || !strcmp(type,"Queue"))
	 retType= "list*";
     else if(!strcmp(type,"pQueue")) 
	 retType="pQueue";
     else if(!strcmp(type,"NodeList"))
	 retType="list**";
     else if(!strcmp(type,"Boolean"))
	 retType="bool";
     else if(!strcmp(type,"Void"))
	 retType="void";
     else if(!strcmp(type,"FileReader"))
	 retType="FILE*";
     return retType;
}

void translateAllocation(char* type){
    if(!strcmp(type,"int"))
	printf("(int*)malloc(sizeof(int)*");
    else if(!strcmp(type,"list**"))
	printf("(list**)malloc(sizeof(list*)*");
    else if(!strcmp(type,"list*"))
	printf("(list*)malloc(sizeof(list)*");
    else if(!strcmp(type,"char"))
	printf("(char*)malloc(sizeof(char)*");
    else if(!strcmp(type,"pQueue"))
	printf("(pQueue*)malloc(sizeof(pQueue)*");
}

void printStar(int number){
    int i=0;
    while(i<number){
	printf("*");
	i++;
    }
}

void printTC(char* type, int counter){
    printf("%s ",type);
    printStar(counter);
}