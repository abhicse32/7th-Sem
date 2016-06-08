#include"list.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#define MAX_VAL   999999999
void DFS(int  srcNode,list**  adList,int  nNodes){
int * visited = (int*)malloc(sizeof(int)*nNodes);
int * visitOrder = (int*)malloc(sizeof(int)*nNodes);
list*  stack = getNewNode(srcNode,0);
int  counter = 0,currentNode,i;
bool  flag;
visited[srcNode] = 1;
visitOrder[counter++] = srcNode;
while((nNodes>counter) && (stack!=NULL)){
currentNode = stack ->id;
list*  iterator = adList[currentNode] ->next;
flag = false;
while((iterator!=NULL)){
if((!visited[iterator ->id])){
currentNode = iterator ->id;
visitOrder[counter++] = currentNode;
visited[currentNode] = 1;
stack = prepend(stack,iterator ->id,0);
flag = true;
break;
}
iterator = iterator ->next;
}
if((!flag)){
stack = removeFirst(stack);
}
}
for(i =0; i <counter; i = i +1){
printf("%d\n",visitOrder[i]);
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
DFS(0,nodeList,nNodes);
}
