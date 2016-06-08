#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include "list.h"

void nodeColoring(list** adjacencyList, int srcNode, int nNodes){
	int* nodeColors= (int*)malloc(sizeof(int)*nNodes);
	int i, gColor=0, maxColor, currNeighbor;
	list* iterator;
	for(i=0; i<nNodes; i++){
		maxColor=0;
		iterator= adjacencyList[i]->next;
		while(iterator!=NULL){
			currNeighbor= iterator->id;
			if(nodeColors[currNeighbor]!=0 && nodeColors[currNeighbor]> maxColor)
				maxColor= nodeColors[currNeighbor];
			iterator= iterator->next;
		}
		nodeColors[i]=maxColor+1;
		if(nodeColors[i] > gColor)
			gColor= maxColor;

	}
	printf("%d\n",gColor);
}
// void nodeColoring(list** adjacencyList, int srcNode, int nNodes){
// 	int gColor=0;
// 	int *assignedColors=(int*)malloc(sizeof(int)*nNodes);
// 	int *neighborColors=(int*)malloc(sizeof(int)*1000);
// 	int* visitedNodes=(int*) malloc(sizeof(int)*nNodes);
// 	list* queue=getNewNode(srcNode,0);
// 	int nodesCounter=0, currentNode, color, currNeighbor;
// 	list* iterator1;
	
// 	memset(neighborColors, -1, sizeof(int)*1000);
// 	visitedNodes[srcNode]=1;
// 	while(nNodes > nodesCounter){
// 		currentNode= queue->id;
		
// 		iterator1= adjacencyList[currentNode]->next;
// 		while(iterator1!=NULL){
// 			currNeighbor= iterator1->id;
// 			if(!visitedNodes[currNeighbor]){
// 				visitedNodes[currNeighbor]=1;
// 				queue= append(queue, currNeighbor,0);
// 			}
// 			color= assignedColors[currNeighbor];
// 			neighborColors[color]=currentNode;
// 			iterator1= iterator1->next;
// 		}
// 		for(color=1; color<= gColor; color++)
// 			if(neighborColors[color]!=currentNode)
// 				break;
// 		if(gColor < color)
// 			gColor++;
// 		assignedColors[currentNode]=color;
// 		//printList(queue);
// 		queue= removeFirst(queue);
// 		nodesCounter++;

// 		if(queue==NULL && nodesCounter < nNodes){
// 			for(color=0; color< nNodes; color++){
// 				if(!visitedNodes[color]){
// 					queue= getNewNode(color,0);
// 					break;
// 				}
// 			}
// 		}
// 		//printf("%d \n",gColor);
// 	}
// 	// for(color=0; color < nNodes; color++)
// 	// 	printf("%d ",assignedColors[color]);
// 	// printf("\n");
// 	printf("%d\n",gColor);
// }

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
									destNode, 0);
		adjacencyList[destNode]= append(adjacencyList[destNode],
									 srcNode, 0);
	}
	// for(i=0; i< nNodes; i++){
	// 	printf("node %d: ",i);
	// 	printList(adjacencyList[i]->next);
	// }
	nodeColoring(adjacencyList,0, nNodes);
}