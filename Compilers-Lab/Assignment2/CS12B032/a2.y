%{
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include "List.c"
#define YYDEBUG 1
void yyerror(const char *str);
void showTable(list*);
bool doesExist(char *varName);
void methodCall(char* objectName, char* methodName, char* paramName);
void showRows(list* rowsList);
int countColumns(list *columnsList, int* rowSpanEntries);
void showColumns(list* columnList, int nColumns, int* rowSpanEntries);


extern FILE* yyin;
extern FILE* yyout;
FILE* outFile;
char* currType;
char* var;
list* vTable;
list* typeNode;

%}

%union{
	char* str;
}

%start Goal

%token TYPE, SEMICOLON, DOT, LPARA, RPARA, COMMA, IDENTIFIER, QUOTEDID

%%			/* Grammar rules to be specified in this section*/

Goal: 
	  | statement Goal
	  | varDeclaration Goal
	  | {}
	  ;

varDeclaration: 
			   |varDeclaration type identifier{
						  currType= $2.str;
						  var= $3.str;
						  typeNode= searchDown(currType, vTable);
						  if(typeNode==NULL)
						    typeNode= addDown(createListNode(currType), vTable);
						  
						  if(doesExist(var))
						      yyerror("ERROR\n");
						 addRight(createListNode(var),typeNode);
					   }varList semicolon
			   |{}
			   ;

varList:
		| varList comma identifier{
				var= $3.str;
				if(doesExist(var))
				  yyerror("ERROR\n");
				addRight(createListNode(var), typeNode);
			    }
		|{}
		;

param: 
	| identifier
	|{}
	;

quotedParam: 
		   |quotedId 
		   |{}
		   ;

statement: 
		  | Goal identifier dot identifier lpara 
		  	param{
			    char* objectName= $2.str;
			    char* methodName= $4.str;
			    char* parameter= $6.str;
			    //printf("%s %s %s\n",objectName, methodName, parameter);
			    methodCall(objectName, methodName, parameter);
			} rpara semicolon

		  | Goal identifier dot identifier lpara quotedParam{
			    char* objectName= $2.str;
			    char* methodName= $4.str;
			    char* parameter= $6.str;
			    //printf("%s %s %s\n",objectName, methodName, parameter);
			    methodCall(objectName, methodName, parameter);
		} 
		  	rpara semicolon
	      
	      |{}
	      ;

semicolon: SEMICOLON   	//{printf("%s\n",$1.str);}
comma: COMMA           	//{printf("%s ",$1.str);}
lpara: LPARA           	//{printf("%s ",$1.str);}
rpara: RPARA           	//{printf("%s ",$1.str);}
dot: DOT               	//{printf("%s ",$1.str);}
type: TYPE             	//{printf("%s ",$1.str);}
identifier: IDENTIFIER 	//{printf("%s ",$1.str);}
quotedId: QUOTEDID      //{printf("%s ",$1.str);}

%%

int yywrap(){
        return 1;
} 
  
void yyerror(const char *str){
        fprintf(yyout,"%s\n",str);
        exit(0);
}

main(int argc, char* argv[]){

	vTable= (list*)malloc(sizeof(list));
	
	if(argc > 1){
	   yyin =fopen(argv[1],"r");
	}
	else yyin = stdin;

	if(argc > 2){
		yyout= fopen(argv[2],"w");
	}else
		yyout= stdout;
	
	outFile=fopen("out.answer","w");
	if(yyin!=NULL){
	    yyparse();
	}
	else
	    yyerror("ERROR\n");
	
	fclose(outFile);
	FILE*inFile= fopen("out.answer","r");
	char buff[200];
	while(fgets(buff,199,inFile)!=NULL){
	    printf("%s",buff);
	}
	fclose(inFile);
 }

 bool doesExist(char *varName){
   list* tempVTable= vTable->down;
   while(tempVTable!=NULL){
     list* found= searchRight(varName, tempVTable);
     if(found!=NULL)
       return true;
     tempVTable= tempVTable->down;
  }
  return false;
}

void methodCall(char* objectName, char* methodName, char* paramName){
    list* nodeType= searchObject(vTable, objectName);
    if(nodeType==NULL)
	yyerror("ERROR\n");
    
    attr* newAttribute;
    list* varNode= searchRight(objectName, nodeType);
    char* typeClass= nodeType->nodeName	;
    if(!strcmp(typeClass,"Table")){
	
	if(!strcmp(methodName,"addRow")){
	    list* rowTypeNode= searchDown("Row",vTable);
	    if(rowTypeNode!=NULL){
		list* rowNode= searchRight(paramName,rowTypeNode);
		if(rowNode!=NULL){
		    addRowToTable(rowNode, varNode);
		}else{
		    yyerror("ERROR\n");
		}
	    }else{
		yyerror("ERROR\n");
	    }
	}else if(!strcmp(methodName,"width") || !strcmp(methodName,"bgcolor") || 
		  !strcmp(methodName,"border")){
	    
	    if(!isAttrPresent(varNode,methodName, paramName)){
		newAttribute= createAttributeNode(methodName, paramName);
		addAttribute(varNode, newAttribute);
	    }
	
	}else if(!strcmp(methodName,"show")){
	    if(varNode->down==NULL || varNode->down->right==NULL)
		yyerror("ERROR\n");
	    showTable(varNode);
	}else{
	    yyerror("ERROR\n");
	}
	
    }else if(!strcmp(typeClass,"Row")){
	
	if(!strcmp(methodName,"addColumn")){
	    list* columnTypeNode= searchDown("Column", vTable);
	    if(columnTypeNode!=NULL){
		list* columnNode= searchRight(paramName, columnTypeNode);
		if(columnNode!=NULL){
		    addColumnToRow(columnNode, varNode);
		}else{
		    yyerror("ERROR\n");
		}
	    }else{
		yyerror("ERROR\n");
	    }
	}else{
	    yyerror("ERROR\n");
	}
	
    }else if(!strcmp(typeClass,"Column")){
	
	if(!strcmp(methodName,"align") || !strcmp(methodName,"bgcolor") ||
	   !strcmp(methodName,"width") || !strcmp(methodName,"colspan") ||
	   !strcmp(methodName,"rowspan")){
	    
	    if(!isAttrPresent(varNode, methodName, paramName)){
		newAttribute= createAttributeNode(methodName, paramName);
		addAttribute(varNode, newAttribute);
	    }
	    //printf("%s:%s\n",objectName, paramName);
	}else if(!strcmp(methodName,"addText")){
	    if(paramName!=NULL){
		int len= strlen(paramName);
		paramName[len-1]='\0';
		varNode->text= strdup(paramName+1);
	    }
	}else if(!strcmp(methodName,"addTable")){
	    list* tableTypeNode= searchDown("Table", vTable);
	    if(tableTypeNode!=NULL){
		list* tableNode= searchRight(paramName,tableTypeNode);
		if(tableNode!=NULL){
		    addTableToColumn(tableNode, varNode);
		}else
		    yyerror("ERROR\n");
	    }else
		yyerror("ERROR\n");
	}else{
	    yyerror("ERROR\n");
	}
    }
}

void getAvailableColumns(int nColumns, int* rowSpanEntries, int rowSpan, int colSpan){
    int i,j,k;
    for(i=0; i < nColumns && rowSpanEntries[i]>0;i++);
    k= colSpan+i;
    for(j=i; j < nColumns && j<=k && !rowSpanEntries[j];j++)
	rowSpanEntries[j]= rowSpan;
    if(j<=k)
	yyerror("ERROR\n");
}

void showColumns(list* columnList, int nColumns, int* rowSpanEntries){
    
list* tempColumns= columnList->right;
    int rowSpan;
    int colSpan,currColumns=0;
    
    while(tempColumns!=NULL){
	currColumns++;
	fprintf(outFile,"<td");
	rowSpan=1;
	colSpan=1;
	attr* columnAttr= tempColumns->attrList;
	while(columnAttr!=NULL){
	    if(!strcmp(columnAttr->attrName,"colspan")){
		char* dup=strdup(columnAttr->attrValue);
		dup[strlen(dup)-1]='\0';
		colSpan= atoi(dup+1);
	    }
	    else if(!strcmp(columnAttr->attrName,"rowspan")){
		char* dup=strdup(columnAttr->attrValue);
		dup[strlen(dup)-1]='\0';
		rowSpan= atoi(dup+1);
	    }
	    
	    fprintf(outFile," %s=%s",columnAttr->attrName, columnAttr->attrValue);
	    columnAttr=columnAttr->nextAttr;
	}
	fprintf(outFile,">");
	colSpan--;
	currColumns+=colSpan;
	if(currColumns>nColumns)
	    yyerror("ERROR\n");
	getAvailableColumns(nColumns, rowSpanEntries,rowSpan, colSpan);
	
	if(tempColumns->text!=NULL){
	    fprintf(outFile,"%s",tempColumns->text);
	}else if(tempColumns->down!=NULL){
	    fprintf(outFile,"\n");
	    showTable(tempColumns->down);
	}
	fprintf(outFile,"</td>\n");
	tempColumns= tempColumns->right;
    }
}

int countColumns(list *columnsList, int* rowSpanEntries){
    
    list* tempColumns= columnsList->right;
    int i=0;
    int nColumns=0;
    int j,k, rowSpan, colSpan;
    
    while(tempColumns!=NULL){
	nColumns++;
	fprintf(outFile,"<td");
	attr* columnAttr= tempColumns->attrList;
	rowSpan=1;
	colSpan=1;
	
	while(columnAttr!=NULL){
	    if(!strcmp(columnAttr->attrName,"rowspan")){
		char* dup=strdup(columnAttr->attrValue);
		dup[strlen(dup)-1]='\0';
		rowSpan= atoi(dup+1);
	    }
	    else if(!strcmp(columnAttr->attrName,"colspan")){
		char* dup=strdup(columnAttr->attrValue);
		dup[strlen(dup)-1]='\0';
		colSpan= atoi(dup+1);
	    }
	    
	    fprintf(outFile," %s=%s",columnAttr->attrName,columnAttr->attrValue);
	    columnAttr= columnAttr->nextAttr;
	}
	fprintf(outFile,">");
	
	if(tempColumns->text!=NULL && tempColumns->down!=NULL)
	    yyerror("ERROR\n");
	if(tempColumns->text!=NULL){
	    fprintf(outFile,"%s",tempColumns->text);
	}else if(tempColumns->down!=NULL){
	    fprintf(outFile,"\n");
	    showTable(tempColumns->down);
	}
	fprintf(outFile,"</td>\n");
	colSpan--; rowSpan--;
	nColumns+= colSpan;
	k= i+colSpan;
	for(j=i;j<=k;j++)
	    rowSpanEntries[j]=rowSpan;
	i=j;
	tempColumns= tempColumns->right;
    }
    return nColumns;
}

void showRows(list* rowsList){
    int nColumns=0;
    int *rowSpanEntries=(int*)malloc(sizeof(int)*30);
    bool flag=false;
    int i=0;
    
    list* tempRows= rowsList->right;
    while(tempRows!=NULL){
	fprintf(outFile,"<tr>\n");
	if(!flag){
	    nColumns=countColumns(tempRows->down,rowSpanEntries);
	    flag= true;
	}else{
	    showColumns(tempRows->down, nColumns, rowSpanEntries);
	    for(i=0;i<nColumns;i++){
		if(rowSpanEntries[i])
		    rowSpanEntries[i]--;
	    }
	
	}
	fprintf(outFile,"</tr>\n");
	tempRows= tempRows->right;
    }
}

void showTable(list* table){
    fprintf(outFile,"<table");
    attr* tableAttr= table->attrList;
    while(tableAttr!=NULL){
	fprintf(outFile," %s=%s",tableAttr->attrName, tableAttr->attrValue);
	tableAttr= tableAttr->nextAttr;
    }
    fprintf(outFile,">\n");
    showRows(table->down);
    fprintf(outFile,"</table>\n");
}
