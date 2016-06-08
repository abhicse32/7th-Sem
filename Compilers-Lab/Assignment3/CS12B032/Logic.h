#include <stdbool.h>
#include <stdlib.h>
#include <stdio.h>
typedef struct List list;
typedef struct VarList vTable;

struct VarList{
	char* nodeName;
	int paramCounter;
	vTable* right;
	vTable* down;
};

struct List{
	char* formatStr;
	char* label;
	list* next; 
};
bool varDec;
int counter;
char* formattedStr;
int printParamCounter;
bool firstPrimaryExp;
int tempCounter;
char* ebp="%ebp";
char* eax="%eax";
char* edx="%edx";
char* esp="%esp";
char * errorString= "ERROR";
char* fileName= "temp.c";
extern FILE* yyin;
extern FILE* yyout;
bool flag;
list *stringList;
vTable* funcTable;
FILE* yyout_;
char* currFunction;
vTable *currFuncNode;

list* searchStr(list*, char*);
list* addString(list*,char*);
vTable* addFunction(vTable*, char* funcName);
vTable* addVar(vTable*, char*);
vTable* getFuncNode(vTable*, char*);
vTable* getVarNode(vTable*, char*);
int getVarCount(vTable* funcNode);
void yyerror(const char*);
void addFormattedString(char*);
void addVarToVTable(char* varName);
void printFormattedString();
void handleFunctionCall(char*);
void handleFunctionDef();
void checkIfDeclared(char*);
void translatePrimaryExpression(char* varName);
void callParser();
void funcTranslation();
void printTranslation();
void integerTranslation();