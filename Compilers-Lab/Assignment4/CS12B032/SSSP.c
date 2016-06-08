#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include "list.h"

#define MAX_VAL 999999999

void SSSP(int srcNode, list* adjacencyList[], int nNodes){
	int* distanceArray=(int*)malloc(sizeof(int)*nNodes);
	pQueue* priorityQueue= (pQueue*)malloc(sizeof(pQueue)*nNodes);
	int* visited= (int*)malloc(sizeof(int)*nNodes);
	int i,size=0,k;
	int updatedDistance, currentNode;
	pQueue node;

	for(i=0;i< nNodes; i++){
		priorityQueue[i].nodeVal= MAX_VAL;
		distanceArray[i]= MAX_VAL;
	}

	priorityQueue[size].nodeVal=0;
	priorityQueue[size].nodeId= srcNode;
	distanceArray[srcNode]=0;
	visited[srcNode]= 1;
	size++;

	while(size >0){
		currentNode= extractMin(priorityQueue,size--);
		list* iterator= adjacencyList[currentNode]->next;
		while(iterator!=NULL){
			updatedDistance= iterator->dist+ distanceArray[currentNode];
			if(updatedDistance < distanceArray[iterator->id])
				distanceArray[iterator->id] = updatedDistance;
			
			node.nodeId= iterator->id;
			node.nodeVal= distanceArray[iterator->id];
			k= checkIfVisited(priorityQueue,iterator->id, size);
			if(k<0 && !visited[node.nodeId]){
				insert(priorityQueue,node, size++);
				visited[node.nodeId]= 1;
			}
			else
				decreaseKey(priorityQueue,k,node);
			iterator= iterator->next;
		}
		minHeapify(priorityQueue,0,size);
	}
	for(i=0;i< nNodes; i++)
		printf("%d %d\n",i, distanceArray[i]);
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

	SSSP(0, adjacencyList, nNodes);
}