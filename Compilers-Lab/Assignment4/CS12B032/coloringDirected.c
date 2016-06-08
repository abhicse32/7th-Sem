#include"list.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
void directedColoring(int  srcNode,list**  adList,int  nNodes){
int * nodeColors = (int*)malloc(sizeof(int)*nNodes);
int  gColor = 0,maxColor,currNeighbor,i;
list*  iterator;
for(i =0; i <nNodes; i = i +1){
maxColor = 0;
iterator = adList[i] ->next;
while((iterator!=NULL)){
currNeighbor = iterator ->id;
if((nodeColors[currNeighbor]!=0) && (nodeColors[currNeighbor]>maxColor)){
maxColor = nodeColors[currNeighbor];
}
iterator = iterator ->next;
}
nodeColors[i] = maxColor+1;
if((nodeColors[i]>gColor)){
gColor = nodeColors[i];
}
}
printf("%d\n",gColor);
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
directedColoring(0,nodeList,nNodes);
}
