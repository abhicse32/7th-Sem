#include <stdio.h>
typedef struct nodesList list;
typedef struct Stack stack;
typedef struct LinesList flist;

struct Stack{
	char key[20];
	char value[20];
	stack* bottom;
};

struct LinesList{
	char *line[4];
	flist* next;
};

struct nodesList{
	char nodeName[20];
	list *right;
	list * down;
};

list* addRight(list * newNode, list *listHead);
list* createListNode(char* name);
list* addDown(list * newNode, list * listHead);
list* searchRight(char * name, list * listHead);
list* searchDown(char * name, list* listHead);
void addNodes(list * relationNode, flist* , char*,char*, char*);
void addRelationNodes(list* head,flist*, char*, char*, char*);


stack* createNode(char*, char*);
stack* push(stack*, stack*);
stack* pop(stack*);
int size();
stack* searchWithKey(char*, stack*);

