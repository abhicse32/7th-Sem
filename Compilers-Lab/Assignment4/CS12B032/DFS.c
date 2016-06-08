#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include "list.h"
#define MAX_VAL 999999999

void DFS(int srcNode, list* adjacencyList[], int nNodes){
	
	int *visited = (int*) malloc(sizeof(int)*nNodes);
	int *visitOrder = (int*) malloc(sizeof(int)*nNodes);
	list* stack= getNewNode(srcNode,0);

	int counter=0;
	int currentNode;
	bool flag;
	int i;

	visited[srcNode]=1;
	visitOrder[counter++] =srcNode; 

	for( ;counter < nNodes && stack!=NULL;){
		
		currentNode= stack->id;
		list* iterator= adjacencyList[currentNode]->next;
		flag= false;
		while(iterator!=NULL){
			if(!visited[iterator->id]){
				currentNode= iterator->id;
				visitOrder[counter++]= currentNode;
				visited[currentNode]=1;
				stack= prepend(stack, iterator->id, 0);
				flag= true;
				break;
			}
			iterator= iterator->next;
		}
		if(!flag)
			stack= removeFirst(stack);
	}

	for(i=0; i< counter ; i++)
		printf("%d\n",visitOrder[i]);
}

void main(int argc, char*argv[]){

	FILE* fp= fopen(argv[1],"r");
	if(fp==NULL){
		printf("ERROR");
		exit(1);
	}

	int nNodes, nEdges;
	if(fscanf(fp,"%d%d",&nNodes, &nEdges)==EOF){
		printf("ERROR");
		exit(1);
	}

	int srcNode, destNode, dist;
	int i,j,k;
	list** adjacencyList= (list**)malloc(sizeof(list*)*nNodes);

	for( i=0; i< nNodes; i++)
		adjacencyList[i] = getNewNode(i,0);

	while(fscanf(fp,"%d%d%d",&srcNode, &destNode, &dist)!=EOF){	
		adjacencyList[srcNode]= append(adjacencyList[srcNode],
									destNode, dist);
	}

	DFS(0, adjacencyList, nNodes);
}