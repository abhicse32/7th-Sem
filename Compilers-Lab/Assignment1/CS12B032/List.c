#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "List.h"

list* addRight(list * newNode, list *listHead){
	list *temp= listHead;

	if(listHead==NULL)
		listHead= newNode;
	else{
		while(temp->right!=NULL)
			temp= temp->right;
		temp->right = newNode;
	}
	return newNode;
}


list* createListNode(char* name){
	list * newNode= (list*) malloc(sizeof(list));
	strcpy(newNode->nodeName,name);
	newNode->right=NULL;
	newNode->down= NULL;
	return newNode;
}

list* addDown(list * newNode, list * listHead){
	list* temp= listHead;
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
		list * temp= listHead;
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
		list * temp= listHead;
		while(temp!=NULL){
			if(temp->nodeName!=NULL)
				if(!strcmp(temp->nodeName, name))
					return temp;
		temp= temp->down;
		}
	}
	return NULL;
}

stack* createNode(char* key_, char* value_){
	stack* temp= (stack*) malloc(sizeof(stack));
	if(key_!=NULL)
		strcpy(temp->key,key_);

	if(value_ !=NULL)
		strcpy(temp->value, value_);

	temp-> bottom= NULL;
	return temp;
}
stack* push(stack* node, stack* head){
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
		if(temp->key !=NULL)
			printf("%s ",temp->key);
		if(temp->value != NULL)
			printf("%s\n",temp->value);

		temp=temp->bottom;
	}
	printf("\n");
}
stack* searchWithKey(char *key_, stack* head){
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

flist* addNameRelation(flist* head, char* name1, 
							char* relation, char* name2){
	flist* newNode=(flist*)malloc(sizeof(flist));
	newNode->line[0]=strdup(name1);
	newNode->line[1]=strdup(relation);
	newNode->line[2]=strdup(name2);
	newNode->next= NULL;

	if(head!=NULL){
		flist* temp=head;
		while(temp->next != NULL)
			temp= temp->next;
		temp->next= newNode;
	}else head= newNode;
	return head;
}

void addNodes(list * relationNode, flist* relationsDatabase, 
				char* name1, char*relation, char* name2){

	list* name1Node= searchRight(name1, relationNode);
	list* temp;
	if(name1Node==NULL){
		temp= createListNode(name1);
		name1Node= addRight(temp, relationNode);
	}

	list* name2Node= searchRight(name2, relationNode);
	if(name2Node==NULL){
		temp= createListNode(name2);
		name2Node= addRight(temp,relationNode);
	}

	if(searchDown(name2, name1Node)==NULL){

		relationsDatabase= addNameRelation(relationsDatabase, name1, relation, name2);
		temp= createListNode(name2);
		addDown(temp, name1Node);
	}
	if(searchDown(name1, name2Node)==NULL){
		temp= createListNode(name1);
		addDown(temp, name2Node);
	}
}

void addRelationNodes(list* head, flist* relationsDatabase,char* name1,
							char* relation, char* name2){
	if(!strcmp(name1, name2))
		return;
	list* temp= head;
	list* relationNode= searchDown(relation, temp);
	if(relationNode != NULL)
		addNodes(relationNode,relationsDatabase, name1,relation, name2);
	else{
		relationNode= createListNode(relation);
		relationNode= addDown(relationNode, temp);
		addNodes(relationNode, relationsDatabase, name1, relation, name2);
	}
}

