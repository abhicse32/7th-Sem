#include"list.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#define MAX_VAL   999999999
void SSSP(int  srcNode,list**  adjList,int  nNodes){
int * distanceArray = (int*)malloc(sizeof(int)*nNodes);
pQueue * priorityQueue = (pQueue*)malloc(sizeof(pQueue)*nNodes);
int * visited = (int*)malloc(sizeof(int)*nNodes);
list*  iterator;
int  i,size = 0,k;
pQueue  node;
int  updatedDistance,currentNode;
for(i =0; i <nNodes; i = i +1){
priorityQueue[i].nodeVal = MAX_VAL;
distanceArray[i] = MAX_VAL;
}
priorityQueue[size].nodeVal = 0;
priorityQueue[size].nodeId = srcNode;
distanceArray[srcNode] = 0;
visited[srcNode] = 1;
size = size+1;
while((size>0)){
currentNode = extractMin(priorityQueue,size--);
iterator = adjList[currentNode] ->next;
while((iterator!=NULL)){
updatedDistance = iterator ->dist+distanceArray[currentNode];
if((distanceArray[iterator ->id]>updatedDistance)){
distanceArray[iterator ->id] = updatedDistance;
}
node.nodeId = iterator ->id;
node.nodeVal = distanceArray[iterator ->id];
k = checkIfVisited(priorityQueue,iterator ->id,size);
if((0>k) && (!visited[node.nodeId])){
insert(priorityQueue,node,size++);
visited[node.nodeId] = 1;
}
else{
decreaseKey(priorityQueue,k,node);
}
iterator = iterator ->next;
}
minHeapify(priorityQueue,0,size);
}
for(i =0; i <nNodes; i = i +1){
printf("%d %d\n",i,distanceArray[i]);
}
}
void main(int  argc,char ** argv){
FILE*  fr = fopen(argv[1],"r");
if((fr==NULL)){
printf("ERROR");
exit(1);
}
int  nNodes,nEdges;
if((fscanf(fr,"%d%d",&nNodes,&nEdges)==-1)){
printf("ERROR");
exit(1);
}
int  srcNode,destNode,dist;
int  i,j,k;
list**  nodeList;
nodeList = (list**)malloc(sizeof(list*)*nNodes);
for(i =0; i <nNodes; i = i +1){
nodeList[i] = getNewNode(i,0);
}
while((fscanf(fr,"%d%d%d",&srcNode,&destNode,&dist)!=EOF)){
nodeList[srcNode] = append(nodeList[srcNode],destNode,dist);
}
SSSP(0,nodeList,nNodes);
}
