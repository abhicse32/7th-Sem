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
	newNode->nodeName= strdup(name);
	newNode->text=NULL;
	newNode->right = NULL;
	newNode->down = NULL;
	newNode->attrList = NULL;
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

list* searchObject(list* typesList, char* objectName){
	if(typesList!=NULL){
		list* tempList= typesList->down;
		list* temp;
		while(tempList!=NULL){
			temp= searchRight(objectName, tempList);
			if(temp!=NULL)
				return tempList;
			tempList= tempList->down;
		}
	}
	return NULL;
}

bool isAttrPresent(list* headNode, char* key, char* value){
	attr* tempAttrList= headNode->attrList;
	while(tempAttrList!=NULL){
		if(!strcmp(tempAttrList->attrName, key)){
			tempAttrList->attrValue= strdup(value);
			return true;
		}
		tempAttrList= tempAttrList->nextAttr;
	}
	return false;
}

attr* createAttributeNode(char* key, char* value){
	attr* newNode= (attr*)malloc(sizeof(attr));
	newNode->attrName = strdup(key);
	newNode->attrValue= strdup(value);
	newNode->nextAttr=NULL;
	return newNode;
}

void addAttribute(list* headNode,attr* attribute){
	attr* tempNode= headNode->attrList;
	if(tempNode==NULL){
		headNode->attrList= attribute;
	}else{
		while(tempNode->nextAttr!=NULL)
			tempNode= tempNode->nextAttr;
		tempNode->nextAttr= attribute;
	}
}

void copyAttributes(list* headNode, attr* attributes){
	attr* tempAttrList= attributes;
	while(tempAttrList!=NULL){
		attr* attrNode= createAttributeNode(tempAttrList->attrName, 
						tempAttrList-> attrValue);

		addAttribute(headNode, attrNode);
		tempAttrList= tempAttrList->nextAttr;
	}
}

list* addRowToTable(list* row, list* table){
	list* tableDown=table->down;
	if(tableDown==NULL){
		tableDown= (list*) malloc(sizeof(list));
		table->down= tableDown;
	}
	list* searchNode=searchRight(row->nodeName, tableDown);
	if(searchNode==NULL){

		list* newNode=createListNode(row->nodeName);
		copyAttributes(newNode,row->attrList);
		addRight(newNode, tableDown);
		
		list*rowColumns= row->down;
		if(rowColumns==NULL || rowColumns->right==NULL){
			yyerror("ERROR\n");
		}
		rowColumns= rowColumns->right;
		while(rowColumns !=NULL){
			addColumnToRow(rowColumns, newNode);
			rowColumns= rowColumns->right;
		}
	}else{
		yyerror("ERROR\n");
	}
}

list* addColumnToRow(list* column, list* row){
	list* rowDown= row->down;
	if(rowDown==NULL){
		rowDown= (list*)malloc(sizeof(list));
		row->down= rowDown;
	}

	list* searchNode= searchRight(column->nodeName, rowDown);
	if(searchNode==NULL){
		list* newNode= createListNode(column->nodeName);
		copyAttributes(newNode, column->attrList);
		if(column->text!=NULL && column->down!=NULL){
			yyerror("ERROR\n");
		}
		else if(column->text!=NULL)
			newNode->text= strdup(column->text);
		
		addRight(newNode, rowDown);
		if(column->down!=NULL)
			addTableToColumn(column->down, newNode);
	}else
		yyerror("ERROR\n");
}

list* addTableToColumn(list* table, list* column){
	if(table!=NULL){
		list* newNode= createListNode(table->nodeName);
		copyAttributes(newNode, table->attrList);
		column->down= newNode;
		if(table->down ==NULL || table->down->right==NULL)
			yyerror("ERROR\n");

		list* tableRows= table->down->right;
		while(tableRows!=NULL){
			addRowToTable(tableRows, newNode);
			tableRows= tableRows->right;
		}
	}
}