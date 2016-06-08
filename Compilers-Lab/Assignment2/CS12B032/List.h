#include <stdio.h>
typedef struct nodesList list;
typedef struct Attributes attr;

struct Attributes{
	char *attrName;
	char *attrValue;
	attr *nextAttr;	
 }; 

struct nodesList{
	char *nodeName;
	char* text;
	attr* attrList;
	list *right;
	list *down;
};

list* addRight(list * newNode, list *listHead);
list* createListNode(char* name);
list* addDown(list * newNode, list * listHead);
list* searchRight(char * name, list * listHead);
list* searchDown(char * name, list* listHead);
list* addColumnToRow(list* row, list* column);
list* searchObject(list* typesList, char* objectName);
list* addTableToColumn(list* table, list* column);
list* addColumnToRow(list* column, list* row);
list* addRowToTable(list* row, list* table);
void addAttribute(list* headNode,attr* attribute);
attr* createAttributeNode(char* key, char* value);
void copyAttributes(list* headNode, attr* attributes);