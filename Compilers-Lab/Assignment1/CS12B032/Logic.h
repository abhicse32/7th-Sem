#include <stdio.h>
#include "List.c"

flist* handleIf(flist*);
flist* handleForeach(flist*);
flist* executeIfBody(flist*);
flist* executeForeachBody(flist*, list*, char*);
flist* processUnusedLines(char*, flist*);
void parseStatement(flist* tempList);
void searchNamesOnStack();
void addNodesTransitive(list* node1, list* node2, list* relationNode);
void addTransitiveRelations();
void processLines(flist* linesList_, FILE* outFile);
void writeToOutputFile();
