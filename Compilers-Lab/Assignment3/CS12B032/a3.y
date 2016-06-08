%{
#include <stdio.h>
#include "Logic.c"
#define YYDEBUG 1
#define COPYTOFILE(str) if(flag) fprintf(yyout_, "%s\n",str)
%}

%union{
	char* str;
}

%start Goal

%token IDENTIFIER VOID ASSIGNMENT LPARA RPARA LCBRACES RCBRACES SEMICOLON
%token ADD SUB TYPE COMMA PRINT QUOTEDID INTEGER

%%			/* Grammar rules to be specified in this section*/
			Goal: 
   				 |Goal FuncDefinition
				 |{}
				 ;

  FuncDefinition:
  				 |Void Identifier LPara RPara LCBraces{
  						currFunction=$2.str;
  						handleFunctionDef();
  					} 
  					VarDeclaration Statements {
  						funcTranslation();
  					}RCBraces
  				 |{}
  				 ;

  VarDeclaration:
  				 |Type Identifier{
  				 	varDec=true;
  				 	if(flag)
  				 		addVarToVTable($2.str);
  				 } VarList Semicolon{varDec=false;} VarDeclaration
  				 |{}
  				 ;

  		VarList:
  				|Comma Identifier{
  					if(flag && varDec)
  				 		addVarToVTable($2.str);
  				} VarList
  				|{}
  				;

     Statements:
     			|PrintStatement Statements
     			|AssignmentStatement Statements  
     			|FunctionCall Statements 
     			|{}
     			;

 PrintStatement: Print LPara QuotedId{
 					formattedStr=$3.str;
 					if(flag) {
 						tempCounter=0;
 						addFormattedString(formattedStr);
 					}else
 						counter=1;
 				} PrintParamsList RPara{
 					printTranslation();
 				} Semicolon
 				;

PrintParamsList: 
			   |Comma{
			   		firstPrimaryExp=true;} 
			   	PrimaryExpression{
			   		firstPrimaryExp=false;} 
			   	Expression{
			   		tempCounter++;
			   		if(!flag){
			   			int stackIndex= counter<<2;
			   			fprintf(yyout, "    movl   %s,   %d(%s)\n",eax,stackIndex,esp);
			   			counter++;
			   		}
			   	}
			   	PrintParamsList
			   |{}
			   ;

AssignmentStatement:Identifier{
						if(flag)
							checkIfDeclared($1.str);
					} Assignment{
						firstPrimaryExp=true;} 
					 PrimaryExpression {
					 	firstPrimaryExp=false;}
					 Expression Semicolon{
					 	if(!flag){
					  		int index= getVarIndex(currFuncNode,$1.str);
					  		index= index <<2;
					  		fprintf(yyout, "    movl   %s,  -%d(%s)\n",eax,index,ebp);
					  }
					}
					;

	   FunctionCall:Identifier{
	   					handleFunctionCall($1.str);
	   				} LPara RPara Semicolon
	   				;

		Expression:
					| Add PrimaryExpression{
						if(!flag)
							fprintf(yyout, "    addl   %s,   %s\n",edx,eax);
					} Expression
					| Sub PrimaryExpression{
						if(!flag)
							fprintf(yyout, "    subl   %s,   %s\n",edx,eax);
					} Expression
					|{}
					;

  PrimaryExpression:Identifier{
  						if(flag)
  							checkIfDeclared($1.str);
  						else 
  							translatePrimaryExpression($1.str);
  					}
  					|Integer{if(!flag)integerTranslation($1.str);}
  					;

Identifier:	IDENTIFIER {COPYTOFILE($1.str);}
Void:VOID {COPYTOFILE($1.str);}
Assignment:ASSIGNMENT {COPYTOFILE($1.str);}
LPara:LPARA {COPYTOFILE($1.str);}
RPara:RPARA {COPYTOFILE($1.str);}
LCBraces:LCBRACES {COPYTOFILE($1.str);}
RCBraces:RCBRACES {COPYTOFILE($1.str);}
Semicolon:SEMICOLON {COPYTOFILE($1.str);}
Add:ADD {COPYTOFILE($1.str);}
Sub:SUB {COPYTOFILE($1.str);}
Type:TYPE {COPYTOFILE($1.str);}
Comma:COMMA {COPYTOFILE($1.str);}
Print:PRINT {COPYTOFILE($1.str);}
QuotedId:QUOTEDID {COPYTOFILE($1.str);}
Integer:INTEGER {COPYTOFILE($1.str);}
%%

int yywrap(){
        return 1;
} 

main(int argc, char* argv[]){
	flag=true;
	stringList= NULL;
	funcTable= (vTable*)malloc(sizeof(vTable));
	currFuncNode=NULL;
	varDec=false;
	printParamCounter=0;

	if(argc > 1){
	   yyin =fopen(argv[1],"r");
	}
	else yyin = stdin;

	if(argc > 2){
		yyout= fopen(argv[2],"w");
	}else
		yyout= stdout;
	callParser();
 }
