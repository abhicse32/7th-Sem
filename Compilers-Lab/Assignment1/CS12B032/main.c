#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#define BUFFLEN 150

typedef struct Stack stack;
typedef struct nodesList list;

struct Stack{
	char key[20];
	char value[20];
	stack* bottom;
};

struct nodesList{
	char nodeName[20];
	list *right;
	list * down;
};

typedef struct LinesList flist;
struct LinesList{
	char line[150];
	flist* next;
};

flist* handleIf(flist*);
flist* handleForeach(flist*);
flist* executeIfBody(flist*);
flist* executeForeachBody(flist*, list*);
flist* processUnusedLines(char*, flist*);

FILE *inputFile;
FILE *outputFile;
char *name1;
char *relation;
char *name2;
char *delimiter= " ";
list* nodesList; 
stack* st;
flist* linesList;
char *buff;
char buffer[BUFFLEN];

flist* add(flist* head, char*str){
	flist* temp= head;
	flist* newLine= (flist*)malloc(sizeof(flist));
	strcpy(newLine->line, str);
	newLine->next= NULL;
	if(head==NULL)
		head= newLine;
	else{
		while(temp->next!=NULL)
			temp= temp->next;
		temp-> next= newLine;
	}
	return head;
}

void printLines(flist* head){
	flist* temp= head;
	while(temp!=NULL){
		printf("%s\n",temp->line);
		temp= temp->next;
	}
}

stack* createNode(char* key_, char* value_){
	stack* temp= (stack*) malloc(sizeof(stack));
	strcpy(temp->key,key_);
	strcpy(temp->value, value_);
	temp-> bottom= NULL;
	return temp;
}
stack* push(stack* node, stack* head){
	if(head!=NULL)
		node->bottom= head;
	return node;
}

stack* pop(stack* st){
	if(st!=NULL){
		stack* temp= st;
		st= st->bottom;
		free(temp);
	}
	return st;
}

void printStack(stack* st){
	stack* temp= st;
	while(temp!=NULL){
		printf("%s %s\n",temp->key, temp->value);
		temp= temp->bottom;
	}
}

stack* searchWithKey(char *key_,stack* head){
	stack* temp= head;
	while(temp!=NULL){
		if(temp->key != NULL)
			if(!strcmp(key_, temp->key))
				return temp;
	    temp= temp->bottom;
	}
	return NULL;
}

int size(stack* st){
	int i=0;
	stack* temp=st;
	for( ;temp!=NULL; temp= temp->bottom, i++);
	return i;
}

list* addRight(char * name, list *listHead){
	list *temp= listHead;
	list* newNode= (list*)malloc(sizeof(list));
	strcpy(newNode->nodeName,name);
	newNode->right = NULL;
	newNode->down = NULL;

	if(listHead==NULL)
		listHead= newNode;
	else{
		while(temp->right!=NULL)
			temp= temp->right;
		temp->right = newNode;
	}
	return newNode;
}

list* addDown(char * name, list * listHead){
	list* temp= listHead;
	list * newNode= (list*) malloc(sizeof(list));
	strcpy(newNode->nodeName,name);
	newNode->right=NULL;
	newNode->down= NULL;

	if(temp==NULL)
		listHead= newNode;
	else{
		while(temp->down!=NULL)
			temp= temp->down;
		temp->down= newNode;
	}
	return newNode;
}

list* searchRight(char * name, list * listHead){
	if(listHead!=NULL){
		list * temp= listHead->right;
		while(temp!=NULL){
			if(temp->nodeName!=NULL)
				if(!strcmp(temp->nodeName, name))
					return temp;
			temp= temp->right;
		}
	}
	return NULL;
}

list* searchDown(char * name, list* listHead){
	if(listHead!=NULL){
		list * temp= listHead->down;
		while(temp!=NULL){
			if(temp->nodeName!=NULL)
				if(!strcmp(temp->nodeName, name))
					return temp;
		temp= temp->down;
		}
	}
	return NULL;
}

void printDown(list *head){
	list* temp= head->down;
	while(temp!=NULL){
		if(temp->nodeName!=NULL)
			printf("%s\n",temp->nodeName);
		temp = temp->down;
	}
}

void printRight(list* head){
	list* temp= head->right;
	while(temp!=NULL){
		if(temp->nodeName!=NULL)
			printf("%s\n",temp->nodeName);
		temp= temp->right;
	}
}

int getRightSize(list *head){
	int i=0;
	list* temp= head->right;
	while(temp!=NULL){
		i++;
		temp= temp->right;
	}
	return i;
}

int getDownSize(list* head){
	int i=0;
	list* temp= head->down;
	while(temp!=NULL){
		i++;
		temp= temp->down;
	}
	return i;	
}

void addNodes(list * relationNode){
	//printf("%s %s %s\n",name1, relation, name2);
	list* name1Node= searchRight(name1, relationNode);
	if(name1Node==NULL)
		name1Node= addRight(name1, relationNode);

	list* name2Node= searchRight(name2, relationNode);
	if(name2Node==NULL)
		name2Node= addRight(name2,relationNode);

	if(searchDown(name2, name1Node)==NULL){
		fprintf(outputFile, "\t%s -- %s [label= \"%s\"];\n",
					name1, name2, relation);
		addDown(name2, name1Node);
	}
	if(searchDown(name1, name2Node)==NULL)
		addDown(name1, name2Node);
}

void addRelationNodes(list* head){
	list* temp= head;
	list* relationNode= searchDown(relation, temp);
	if(relationNode != NULL)
		addNodes(relationNode);
	else{
		relationNode= addDown(relation, temp);
		addNodes(relationNode);
	}
}

void error(){
	printf("error: condition evaluation failed!!!\n");
	exit(1);
}

void parseStatement(char *delimiter, char* str){
	if((name1= strtok(str, delimiter))==NULL)
		error();
	if((relation= strtok(NULL, delimiter))==NULL)
		error();
	if((name2= strtok(NULL, delimiter))==NULL)
		error();
	return;
}

void printRelations(){
	list* temp= nodesList->down;
	while(temp!=NULL){
		printf("\t%s....\n",temp->nodeName);
		list* temp2= temp->right;
		while(temp2!=NULL){
			printf("   %s\n", temp2->nodeName);
			printDown(temp2);
			temp2= temp2->right;
		}
		temp= temp->down;
	}
}

char* findFirstWord(char buff[]){
	int i=0,j=0;
	char* firstWord= (char*)malloc(sizeof(char)*25);
	int len=strlen(buff);

	while(i < len && buff[i]==' ')
		i++;

	while(i < len && buff[i]!=' ')
		firstWord[j++]=buff[i++];
	firstWord[j]='\0';

	return firstWord;
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

	while(tempList!=NULL){

		buff= tempList->line;
		char* firstWord= findFirstWord(buff);

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

	if(foreachCount || ifcount){
		printf("error: no matching \"endforeach\" to \"foreach\" or \"endif\" to \"if\"!!!");
		exit(1);
	}
	return NULL;
}

int strcmp(const char* str1,const char* str2){
	int len1, len2;
	int i;
	if(str1!=NULL && str2!=NULL){
		len1= strlen(str1);
		len2= strlen(str2);

		for(i=0;i<len1 && i<len2;i++)
			if(str1[i]!=str2[i])
				return str1[i]- str2[i];
	}
	return 0;
}
flist* executeIfBody(flist* tempList){

	while(tempList!=NULL){
		buff= tempList->line;
		//printf("%s\n",buff);
		char* firstWord= findFirstWord(buff);
	    //printf("%s\n",firstWord);
		if(!strcmp(firstWord,"foreach")){
			strtok(buff,delimiter);
			tempList= handleForeach(tempList->next);
			continue;

		}else if(!strcmp(firstWord,"if")){
			strtok(buff, delimiter);
			tempList= handleIf(tempList->next);
			continue;

		}else if(!strcmp(firstWord,"endif")){
			return  tempList->next;

		}else if(!strcmp(firstWord,"endforeach")){
			printf("error: unmatched \"endforeach: expecting endif\"\n");
			exit(1);

		}else{
			parseStatement(delimiter, buff);
			//printf("%s %s %s\n",name1, name2, relation);
			stack* newName1= searchWithKey(name1, st);
			stack* newName2= searchWithKey(name2, st);
			//printf("%s %s %s\n",name1, name2, relation);
			if(newName1!=NULL)
				name1= newName1->value;
			if(newName2!=NULL)
				name2= newName2->value;

			//printf("%s %s %s\n",name1, name2, relation);
			addRelationNodes(nodesList);
		}
		tempList= tempList->next;
	}

	return NULL;
}

void processLines(){

	flist* tempList= linesList;
	while(tempList!=NULL){
		strcpy (buffer,tempList->line);
		name1=NULL;
		relation= NULL;
		name2= NULL;
		//printf("%s\n",buff);
		name1= strtok(buffer, delimiter);
		if(name1!=NULL){
			if(!strcmp(name1, "if")){
                tempList = handleIf(tempList->next);
                continue;
			}
			else if(!strcmp(name1,"foreach")){
				//printf("came here\n");
				tempList = handleForeach(tempList->next);
				continue;

			}else{
				relation= strtok(NULL, delimiter);
				if(relation!=NULL)
					name2= strtok(NULL, delimiter);
				if(name2==NULL){
					printf("error:insufficient data!!\n");
					exit(1);
				}
				addRelationNodes(nodesList);
			}
	    }
	    tempList= tempList -> next;
    }
}

flist* handleIf(flist* tempList){
	parseStatement(delimiter,NULL);
	//printf("%s %s %s\n",name1, relation, name2);
	stack* stackName1= searchWithKey(name1,st);
	stack* stackName2= searchWithKey(name2,st);

	if(stackName1!=NULL)
		name1= stackName1->value;

	if(stackName2!=NULL)
		name2= stackName2->value;

	if(name1!=NULL && name2!=NULL){

		list* relationNode= searchDown(relation,nodesList);
	    list* node1= searchRight(name1, relationNode);
	    list* node2= searchDown(name2, node1);

	    if(relationNode==NULL || node1==NULL || node2==NULL){
	    	return processUnusedLines("if", tempList);
	    }
	    //printf("%s %s %s\n",name1, relation, name2);
	    return executeIfBody(tempList);
	}
	return NULL;
}

flist* executeForeachBody(flist* tempList, list* neighborsList){
	list* tempNbr= neighborsList;
	flist* tempLines;
	char* firstWord;

	while(tempNbr!=NULL){
		printf("%s\n",tempNbr->nodeName);
		//printf("%s\n",tempList-> line);
		tempLines= tempList;
		while(tempLines!=NULL){
			strcpy(buffer, tempLines->line);
			//printf("%s\n",buff);
			firstWord= findFirstWord(buffer);
	    	//printf("%s\n",firstWord);

			if(!strcmp(firstWord,"foreach")){
				strtok(buffer,delimiter);
				tempLines = handleForeach(tempLines->next);
				continue;

			}else if(!strcmp(firstWord,"if")){
				strtok(buffer, delimiter);
				tempLines= handleIf(tempLines->next);
				continue;

			}else if(!strcmp(firstWord,"endforeach")){
				break;

			}else if(!strcmp(firstWord,"endif")){
				printf("error: unmatched \"endif: expecting endforeach\"\n");
				exit(1);

			}else{
				parseStatement(delimiter, buff);
				//printf("%s %s %s\n",name1, name2, relation);
				stack* newName1= searchWithKey(name1, st);
				stack* newName2= searchWithKey(name2, st);
				//printf("%s %s %s\n",name1, name2, relation);
				if(newName1!=NULL)
					name1= newName1->value;
				if(newName2!=NULL)
					name2= newName2->value;

				//printf("%s %s %s\n",name1, name2, relation);
				addRelationNodes(nodesList);
			}
			tempLines= tempLines->next;
		}
		tempNbr= tempNbr->down;
	}
	return tempLines->next;
}

flist* handleForeach(flist* tempList){
	parseStatement(delimiter,NULL);
	//printf("%s %s %s\n",name1, relation, name2);
	list* relationNode= searchDown(relation, nodesList);
	list* node1= searchRight(name1, relationNode);

	if(node1==NULL){
		stack* parentVar= searchWithKey(name2, st);
		if(parentVar!=NULL)
			name2= parentVar->value;

		//printf("%s %s %s\n",name1, relation, name2);
		list* node2= searchRight(name2, relationNode);
		list* neighborsList;

		if(node2==NULL){
			printf("foreach: rvalue: NULL \n");
			exit(1);

		}else if(node2!=NULL){
			neighborsList= node2->down;
			if(neighborsList!=NULL)
				return executeForeachBody(tempList, neighborsList);
			else
				return processUnusedLines("foreach",tempList);
		}
	}else{
		printf("loop condition failed to evaluate!!");
		exit(1);
	}
	return NULL;
}

void readFromFile(){
	if(inputFile==NULL){
		printf("error in opening file!!\n");
		exit(1);
	}
	char buff[BUFFLEN];
	int len;
	while(fgets(buff, BUFFLEN-1, inputFile)!=NULL){
		len= strlen(buff);
		if(buff[len-1]=='\n' || buff[len-1]==' ')
			buff[len-1]='\0';
		if(strcmp("\n",buff))
			linesList= add(linesList, buff);
	}
}
int main(int argc, char* argv[]){

	inputFile= fopen(argv[1],"r");
    outputFile= fopen(argv[2],"w");
    readFromFile();
    //printLines(tempList);
    nodesList= (list*) malloc(sizeof(list));
    st=NULL;
    fprintf(outputFile, "graph relations{\n");
    processLines();
    fprintf(outputFile, "}");
    //printRelations();*/
    return 0;
}
