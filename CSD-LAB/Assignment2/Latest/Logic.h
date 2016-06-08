#include <iostream>
#include <bitset>
using namespace std;

typedef struct PseudoLRU treeLRU;
typedef struct Block block;

/*
 structure to build internal nodes and leafs of the tree
*/
struct PseudoLRU{
	treeLRU * parent;
	bitset<1> direction;  // bit to indicate the left or right direction 
	bitset<1> which;	  /*bit to indicate if the current node is 
						  right or left child of its parent*/ 
	block* dataNode;
	treeLRU *left;
	treeLRU *right;
};

/*
  structure used to store data at leaf nodes
*/
struct Block{
	unsigned int tag;
};

