#include <stdio.h>
typedef struct List list;
typedef struct PriorityNode pQueue;
struct List{
	int id;
	int dist;
	list* next;
};

struct PriorityNode{
	int nodeId;
	int nodeVal;
};

list* getNewNode(int nodeVal, int );
list* append(list* head, int nodeVal, int );
list* prepend(list*, int ,int);
int size(list*);
list* removeFirst(list*);
list* removeLast(list*);

void minHeapify(pQueue*,int, int);
int extractMin(pQueue*, int size);
void decreaseKey(pQueue* minQueue,int i, pQueue node);
void insert(pQueue* minQueue, pQueue node, int size);