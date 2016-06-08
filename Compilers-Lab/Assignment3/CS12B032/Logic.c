#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include "Logic.h"

void yyerror(const char *str){
        fprintf(yyout,"ERROR");
        exit(0);
}

int getLength(list* head){
	int i=0;
	list* temp= head;
	while(temp!=NULL){
		i++; temp= temp->next;
	}
	return i;
}

list* searchStr(list* head,char* str){
	list* tempList= head;
	if(tempList!=NULL){
		while(tempList!=NULL){
			if(!strcmp(tempList->formatStr,str))
				return tempList;
			tempList= tempList->next;
		}
	}
	return NULL;
}

int getVarCount(vTable* funcNode){
	vTable* tempNode= funcNode->right;
	int i=0;
	while(tempNode!=NULL){
		i++; tempNode= tempNode->right;
	}
	return i;
}
char *getLabel(int len){
	char *labelPrefix="LC";
	char str[4];
	sprintf(str,"%d",len);
	char* label= (char*)malloc(3+strlen(str));
	strcpy(label,labelPrefix);
	strcat(label,str);
	return label;
}

list* addString(list* head, char* str){
	list* tempNode= head;
	list* newNode=(list*)malloc(sizeof(list));
	newNode->formatStr= strdup(str);
	newNode->label= getLabel(getLength(head));
	newNode->next= NULL;
	if(tempNode!=NULL){
		while(tempNode->next!=NULL)
			tempNode= tempNode->next;
		tempNode->next= newNode;
	}else
		head= newNode;
	return head;
}

vTable* getVarNode(vTable* head, char* varName){
	vTable* tempNode= head->right;
	while(tempNode!=NULL){
		if(!strcmp(tempNode->nodeName,varName))
			return tempNode;
		tempNode= tempNode->right;
	}
	return NULL;
}

vTable* getFuncNode(vTable* head, char* funcName){
	vTable* tempNode= head->down;
	while(tempNode!=NULL){
		if(!strcmp(tempNode->nodeName,funcName))
			return tempNode;
		tempNode= tempNode->down;
	}
	return NULL;
}


vTable* addFunction(vTable* head, char* funcName){
	vTable* tempNode= head;
	vTable* newNode= (vTable*)malloc(sizeof(vTable));
	newNode->nodeName= strdup(funcName);
	newNode->right=NULL;
	newNode->down=NULL;
	if(head!=NULL){
		while(tempNode->down!=NULL)
			tempNode=tempNode->down;
		tempNode->down=newNode;
	}else head=newNode;
	return head;
}

vTable* addVar(vTable* funcNode, char*varName){
	vTable* varNode= (vTable*)malloc(sizeof(vTable));
	varNode->nodeName= strdup(varName);
	varNode->right= NULL;
	varNode->down= NULL;
	vTable* tempNode= funcNode;
		while(tempNode->right!=NULL)
			tempNode = tempNode->right;
		tempNode->right= varNode;
	return funcNode;
}

int getVarIndex(vTable* funcNode,char* varName){
	vTable* tempNode= funcNode->right;
	int i=1;
	while(tempNode!=NULL){
		++i;
		if(!strcmp(tempNode->nodeName, varName))
			break;
		tempNode= tempNode->right;
	}
	return i;
}

void checkForMain(){
	vTable* tempTable= funcTable->down;
	while(tempTable!=NULL){
		if(!strcmp("main", tempTable->nodeName))
			return;
		tempTable= tempTable->down;
	}
	yyerror(errorString);
}

void printFormattedString(){

	fprintf(yyout, "    .section\t.rodata\n");
	list* tempList= stringList;
	while(tempList!=NULL){
		fprintf(yyout, ".%s:\n    .string %s\n",tempList->label, 
			tempList->formatStr);
		tempList= tempList->next;
	}
	fprintf(yyout, "    .text\n");
}

 void addFormattedString(char* string){
 		if(searchStr(stringList,string)==NULL)
 			stringList= addString(stringList,string);
 }

void addVarToVTable(char* varName){
	if(getVarNode(currFuncNode,varName)==NULL){
		addVar(currFuncNode,varName);
	}else yyerror(errorString);
}

void handleFunctionDef(){
	if(flag){
		printParamCounter=0;
		if(getFuncNode(funcTable,currFunction)==NULL){
			funcTable= addFunction(funcTable,currFunction);
		}else yyerror(errorString);
		currFuncNode= getFuncNode(funcTable,currFunction);

	}else{
		currFuncNode= getFuncNode(funcTable,currFunction);
  	 	fprintf(yyout, "    .global    %s\n",currFunction);
  		fprintf(yyout, "    .type    %s, @function\n%s:\n",
  								currFunction,currFunction);
  		fprintf(yyout, "    .cfi_startproc\n");
  		fprintf(yyout, "    pushl   %s\n",ebp);
  		fprintf(yyout, "    movl  %s, %s\n",esp,ebp);
  		if(!strcmp("main",currFunction))
  			fprintf(yyout, "    andl   $-16, %s\n",esp);
  		int varCount= getVarCount(currFuncNode);
  		int allocatedStackSpace= ((((varCount+currFuncNode->
  										paramCounter)>>2)+1)<<4)+16;
  		fprintf(yyout, "    subl   $%d,  %s\n",allocatedStackSpace, esp);
  	}
}

void handleFunctionCall(char* calleeName){
	vTable* calleeNode= getFuncNode(funcTable, calleeName);
	if(flag && (calleeNode==NULL || 
			!strcmp(calleeNode->nodeName, currFunction)))
			yyerror(errorString);
	else if(!flag)
		fprintf(yyout, "    call    %s\n",calleeName);
}

void checkIfDeclared(char* varName){
	if(getVarNode(currFuncNode, varName)==NULL)
		yyerror(errorString);
}

void translatePrimaryExpression(char* varName){
	int index= getVarIndex(currFuncNode,varName);
	index= index<<2;
	char* reg=NULL;
	if(firstPrimaryExp)
		reg= eax;
	else
		reg= edx;
	fprintf(yyout, "    movl   -%d(%s),   %s\n",index,ebp,reg);
}

void callParser(){
	yyout_=fopen(fileName,"w");
	if(yyin!=NULL){
	    yyparse();
	    fclose(yyin);
	    fclose(yyout_);
	}
	else
	    yyerror(errorString);
	flag=false;
	checkForMain();
	printFormattedString();
	yyin= fopen(fileName,"r");
	if(yyin!=NULL)
		yyparse();
	fprintf(yyout, "    .section	.note.GNU-stack,\"\",@progbits\n");
}

void funcTranslation(){
	if(flag)
  		currFuncNode->paramCounter= printParamCounter;
  	else{
  		fprintf(yyout, "    leave\n");
  		fprintf(yyout, "    ret\n");
  		fprintf(yyout, "    .size   %s,  .-%s\n",currFunction, currFunction);
  		fprintf(yyout, "    .cfi_endproc\n");	
  	}
}

void printTranslation(){
	if(flag && printParamCounter< tempCounter)
 		printParamCounter= tempCounter;
 	else if(!flag){
	 	list* fStrNode=searchStr(stringList, formattedStr);
	 	fprintf(yyout, "    movl   $.%s,   (%s)\n",fStrNode->label,esp);
	 	fprintf(yyout, "    call   printf\n");
	}
}

void integerTranslation(char* str){
  	char* reg;
  	if(firstPrimaryExp)
  		reg=eax;
  	else reg = edx;
  		fprintf(yyout, "    movl   $%s,   %s\n",str,reg);
}
