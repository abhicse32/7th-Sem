%{
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include "Logic.c"

#define YYDEBUG 1
#define BUFFLEN 150

flist* addFlistNode(flist* parentList, flist* node);
void addCondition(char * token);
void addStatement(char* token1, char* token2, char * token3);
void yyerror(const char *str);

bool ifFlag=false;
bool foreachFlag=false;
bool endifFlag=false;
bool endforeachFlag=false;
%}

%union{
	char* str;
}

%start Goal

%token NAME RELATION IF ENDIF FOREACH ENDFOREACH VAR

%%			/* Grammar rules to be specified in this section*/

Goal: 
	  | statement Goal
	  | ifStatement 
	  | foreachStatement
	  | {}
	  ;

statement: NAME RELATION NAME  { addStatement($1.str, $2.str, $3.str);}
		  ;

firstVarStatement:  VAR RELATION NAME { addStatement($1.str, $2.str, $3.str); }
	              | VAR RELATION VAR  { addStatement($1.str, $2.str, $3.str); }

secondVarStatement: NAME RELATION VAR { addStatement($1.str, $2.str, $3.str); }
	  			  ;

varGoal:  varGoal statement
	  | varGoal firstVarStatement
	  | varGoal ifStatement
	  | varGoal secondVarStatement
	  | varGoal varIfStatement 
	  | varGoal foreachStatement
	  |{}
	  ;

ifStatement: ifst statement varGoal endifst varGoal ;

ifst: IF {ifFlag=true; addCondition($1.str);}; 

foreachst: FOREACH { foreachFlag = true; addCondition($1.str)};

endifst: ENDIF {endifFlag= true; addCondition($1.str)};

endforeachst: ENDFOREACH {endforeachFlag= true; addCondition($1.str)};

varIfStatement: ifst firstVarStatement varGoal endifst varGoal ;

foreachStatement: foreachst firstVarStatement varGoal endforeachst varGoal ;
%%

extern FILE* yyin;
extern FILE* yyout;
flist* linesList=NULL;
flist* globalList=NULL;
bool errorFlag=false;

int index_=0;
void error(){
	printf("error: condition evaluation failed!!!\n");
	exit(1);
}


flist* addFlistNode(flist* parentList, flist* node){
	if(parentList != NULL){
		flist* temp= parentList;
		while(temp->next!=NULL)
			temp = temp->next;
		temp->next= node;
	}
	else parentList= node;
	return parentList;
}

void addCondition(char * token){
	globalList= (flist*) malloc(sizeof(flist));
	globalList->line[index_]= (char*)malloc(strlen(token)+1);
	strcpy(globalList->line[index_], token);

	if(endifFlag || endforeachFlag){
		linesList= addFlistNode(linesList, globalList);
		globalList=NULL;
		index_= 0;
		endifFlag= endforeachFlag= false;
	}
	else index_= index_ + 1;
}

void addStatement(char* token1, char* token2, char * token3){
	if(globalList==NULL || !index_){
		globalList= (flist*)malloc(sizeof(flist));
		index_=0;
	}

	globalList->line[index_++]= strdup(token1);
	globalList->line[index_++]= strdup(token2);
	globalList->line[index_]= strdup(token3);

	linesList= addFlistNode(linesList, globalList);
	globalList= NULL;
	index_=0;
	ifFlag= foreachFlag= false;
}

int yywrap(){
        return 1;
} 
  
void yyerror(const char *str){
        fprintf(yyout,"ERROR");
        errorFlag=true;
}

main(int argc, char* argv[]){

	if(argc > 1){
	   yyin =fopen(argv[1],"r");
	}
	else yyin = stdin;

	if(argc > 2){
		yyout= fopen(argv[2],"w");
	}else
		yyout= stdout;

	if(yyin!=NULL){
        yyparse();
	}

    else{
    	printf("error occured in opening file");
    	exit(1);
    }

    fclose(yyin);
    if(errorFlag)
    	exit(1);
     processLines(linesList, yyout);
     writeToOutputFile();
    return ; 
}