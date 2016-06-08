#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include "Logic.h"

flist* relationsDatabase=NULL;

char *name1;
char *relation;
char *name2;
list* nodesList; 
stack* st;
int counter=0;
FILE* outFile=NULL;

void parseStatement(flist* tempList){
	name1= tempList->line[counter++];
	relation= tempList->line[counter++];
	name2= tempList->line[counter++];
	return;
}

flist* processUnusedLines(char* condition, flist* tempList){
	int ifcount=0;
	int foreachCount=0;
	bool ifFlag=false;
	bool forFlag= false;

	if(!strcmp(condition,"if")){
		ifcount=1;
		ifFlag=true;
	}
	else {
		foreachCount=1;
		forFlag= true;
	}

	char* firstWord=NULL;
	while(tempList!=NULL){

		firstWord= tempList->line[0];
	    if(!strcmp(firstWord,"if"))
	    	ifcount++;
	    else if(!strcmp(firstWord,"foreach"))
	    	foreachCount++;
	    else if(!strcmp(firstWord, "endif"))
	    	ifcount--;
	    else if(!strcmp(firstWord,"endforeach"))
	    	foreachCount--;
	    if((!ifcount && ifFlag) || (!foreachCount && forFlag))
	    	return tempList->next;

	    tempList= tempList->next;
	}
	return NULL;
}

void searchNamesOnStack(){
	stack* newName1= searchWithKey(name1, st);
	stack* newName2= searchWithKey(name2, st);
			
	if(newName1!=NULL)
		name1= newName1->value;
	if(newName2!=NULL)
		name2= newName2->value;
	if(name1[0]=='$' || name2[0]=='$'){
		fprintf(outFile, "ERROR");
		exit(1);
	}
	return;
}

flist* executeIfBody(flist* tempList){

	while(tempList!=NULL){
		counter=0;
		char* firstWord= tempList->line[0];

		if(!strcmp(firstWord,"foreach")){
			tempList= handleForeach(tempList);
			continue;

		}else if(!strcmp(firstWord,"if")){
			tempList= handleIf(tempList);
			continue;

		}else if(!strcmp(firstWord,"endif")){
			return  tempList->next;

		}else{
			parseStatement(tempList);
			searchNamesOnStack();
			addTransitiveRelations();
		}
		tempList= tempList->next;
	}

	return NULL;
}

flist* handleIf(flist* tempList){
	counter++;
	parseStatement(tempList);
	searchNamesOnStack();

	list* relationNode= searchDown(relation,nodesList);
	list* node1= searchRight(name1, relationNode);
	list* node2= searchDown(name2, node1);

	if(relationNode==NULL || node1==NULL || node2==NULL)
	    return processUnusedLines("if", tempList->next);
	//printf("%s %s %s\n",name1, relation, name2);
	else return executeIfBody(tempList->next);
	return NULL;
}

flist* executeForeachBody(flist* tempList, list* neighborsList, char* varNode){
	list* tempNbr= neighborsList;
	flist* tempLines;
	char* firstWord;
	stack* tempStackNode=NULL;
	printf("%d\n",size(st));
	while(tempNbr!=NULL){

		tempStackNode= createNode(varNode, tempNbr->nodeName);
		st= push(tempStackNode, st);
		tempLines= tempList;
		while(tempLines!=NULL){
			counter=0;

			firstWord= tempLines->line[0];
			if(!strcmp(firstWord,"foreach")){
				tempLines = handleForeach(tempLines);
				continue;

			}else if(!strcmp(firstWord,"if")){
				tempLines = handleIf(tempLines);
				continue;

			}else if(!strcmp(firstWord,"endforeach")){
				break;

			}else{
				parseStatement(tempLines);
				searchNamesOnStack();
				addTransitiveRelations();
			}
			tempLines= tempLines->next;
		}
		tempNbr= tempNbr->down;
		st= pop(st);
	}
	return tempLines->next;
}

flist* handleForeach(flist* tempList){
	counter++;
	parseStatement(tempList);

	stack* name1Node=searchWithKey(name1, st);
	stack* name2Node=searchWithKey(name2, st);

	if(name2Node==NULL && name2[0]=='$'){
		fprintf(outFile,"ERROR");
		exit(1);
	}
	if(name1Node !=NULL){
		printf("Syntax Error in foreach");
		exit(1);
	}

	if(name2Node!= NULL)
		name2 = name2Node->value;

	list* relationNode= searchDown(relation, nodesList);
	list* presentNode= searchRight(name2, relationNode);


	if(presentNode!=NULL){
		list* neighborsList= presentNode->down;
		if(neighborsList!=NULL)
			return executeForeachBody(tempList->next, neighborsList, name1);
		else
			return processUnusedLines("foreach",tempList->next);
	}
	else
		return processUnusedLines("foreach",tempList->next);
	return NULL;
}

void addNodesTransitive(list* node1, list* node2, list* relationNode){
	list* temp1= node1;
	list* temp2=NULL;
	list* temp=NULL;
	list* node1Right=NULL;
	//printf("%s:\n",relation);
	while(temp1 != NULL){
		node1Right= searchRight(temp1->nodeName, relationNode);
		temp2= node2;
		//printf("\t%s:", temp1->nodeName);
		while(temp2!=NULL){
			temp= searchDown(temp2->nodeName, node1Right);
			if(temp == NULL){
				name1= temp1->nodeName;
				name2= temp2->nodeName;
				addRelationNodes(nodesList,relationsDatabase, name1, relation, name2);
			}
			temp2= temp2->down; 
		}
		//printf(" %d\n",getDownSize(searchRight(temp1->nodeName, relationNode)));
		temp1= temp1->down;
	}	
}
void addTransitiveRelations(){

	addRelationNodes(nodesList,relationsDatabase, name1, relation, name2);
	if(!strcmp(relation,"classmateof") || 
					!strcmp(relation,"roommateof")){

		list* relationNode= searchDown(relation, nodesList);
		list* name1Node= searchRight(name1, relationNode);
		list* name2Node= searchRight(name2, relationNode);
		addNodesTransitive(name1Node, name2Node, relationNode);
	}
	return;
}

void processLines(flist* linesList_, FILE* outFile_){

	outFile = outFile_;
	nodesList=(list*)malloc(sizeof(list));
	relationsDatabase= (flist*)malloc(sizeof(flist));

	flist* tempList= linesList_;
	while(tempList!=NULL){
		counter=0;
		name1=NULL;
		relation= NULL;
		name2= NULL;

		name1= tempList->line[counter];
		if(!strcmp(name1, "if")){
            tempList = handleIf(tempList);
            continue;
		}
		else if(!strcmp(name1,"foreach")){
			tempList = handleForeach(tempList);
			continue;

		}else{
			parseStatement(tempList);
			addTransitiveRelations();
		}
	    tempList= tempList -> next;
    }
}

void writeToOutputFile(){

	char* graphName="relationsGraph";
	fprintf(outFile,"graph %s {\n\n", graphName);
    	flist* temp= relationsDatabase->next;
    	while(temp!=NULL){
    		fprintf(outFile,"\t%s -- %s [label=\"%s\"]\n", temp->line[0],
    						temp->line[2], temp->line[1]);
    		temp= temp-> next;
      }
    	fprintf(outFile,"\n}\n");
    	fclose(outFile);
}
