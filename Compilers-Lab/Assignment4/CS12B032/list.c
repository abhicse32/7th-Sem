#include <stdio.h>
#include "list.h"
#include <stdlib.h>
#define MAX_VAL  999999999

list* getNewNode(int nodeVal, int wt){
	list* newNode= (list*) malloc(sizeof(list));
	newNode-> id = nodeVal;
	newNode-> dist = wt;
	newNode -> next = NULL;
	return newNode;
}

int size(list* head){
	int i=0;
	list* temp= head;
	for( ;temp != NULL; temp= temp->next, i++);
	return i;
}

list* append(list* head, int nodeId, int dist){
	list* newNode = getNewNode(nodeId, dist);
	list* tempNode= head;
	if(tempNode == NULL)
		head= newNode;
	else{
		while(tempNode->next !=NULL)
			tempNode= tempNode->next;
		tempNode->next = newNode;
	}
	return head;
}

list* prepend(list* head, int id, int dist){
	list* newNode= getNewNode(id, dist);
	newNode->next= head;
	return newNode;
}

list* removeFirst(list *head){
	if(head !=NULL){
		list* tempNode = head; 
		head= head->next;
		free(tempNode);
	}else{
		printf("ERROR: trying to remove from an empty list\n");
		exit(1);
	}
	return head;
}

list* removeLast(list* head){
	list* temp = head;
	if(temp != NULL){
		if(temp->next ==NULL)
			head=NULL;
		else{
			list* temp2= temp->next;
			while(temp2->next !=NULL){
				temp= temp2;
				temp2= temp2->next;
			}
			temp->next= temp2->next;
		    free(temp2);
		}
	}else{
		printf("ERROR: trying to remove from an empty list\n");
		exit(1);	
	}
	return head;
}

void printList(list* head){
	list* temp= head;
	while(temp != NULL){
		printf("%d ",temp->id);
		temp= temp->next;
	}
	printf("\n");
}

void swap(pQueue* queue, int i, int j){
	int tempId= queue[i].nodeId;
	int tempVal= queue[i].nodeVal;
	queue[i].nodeId= queue[j].nodeId;
	queue[i].nodeVal= queue[j].nodeVal;
	queue[j].nodeId= tempId;
	queue[j].nodeVal= tempVal;
}

void minHeapify(pQueue* minQueue,int i, int size){
	int left = (i<<1)+1;
	int right = left+1;
	int smallest = i;

	if(left < size && minQueue[left].nodeVal < minQueue[smallest].nodeVal)
		smallest= left;
	if (right < size && minQueue[right].nodeVal < minQueue[smallest].nodeVal)
		smallest= right;

	if(smallest!=i){
		swap(minQueue, smallest, i);
		minHeapify(minQueue, smallest, size);
	}
}

int extractMin(pQueue* minQueue, int size){
	if(size < 1){
		printf("ERROR");
		exit(1);
	}
	int min = minQueue[0].nodeId;
	swap(minQueue, 0, size-1);
	minQueue[size-1].nodeVal= MAX_VAL;
	minHeapify(minQueue,0,--size);
	return min;
}

void decreaseKey(pQueue* minQueue,int i, pQueue node){
	if(minQueue[i].nodeVal < node.nodeVal)
		return;
	minQueue[i]= node;
	int parent= (i-1)/2;
	while(i>0 && minQueue[parent].nodeVal > minQueue[i].nodeVal){
		swap(minQueue,i, parent);
		i=parent;
		parent= (parent-1)/2;
	}
}

void insert(pQueue* minQueue, pQueue node, int size){
	minQueue[size].nodeVal= MAX_VAL;
	decreaseKey(minQueue, size, node);
}

int checkIfVisited(pQueue* minQueue, int id, int size){
	int i;
	for(i=0; i< size; i++){
		if(minQueue[i].nodeId == id)
			return i;
	}
	return -1;
}
