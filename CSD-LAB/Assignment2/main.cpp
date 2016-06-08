#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <iostream>
#include <list>
#include <cctype>
#include <cmath>
#include "Logic.cpp"

/*
	For simplicity we will assume that cache blocks are of multiples of 2
	and that every unit of memory is of multiple of 2
*/

int main(int argc, char* argv[]){
	FILE* infile;

	parseCommand(argc, argv);

	if(cacheLineSize)
		nBlocks= cacheSize/ cacheLineSize;
	if(associativity)
		nSets= nBlocks/associativity;

	nSetBits= getBits(nSets)-1;
	nOffsetBits= getBits(cacheLineSize)-1;
	nTagBits= 32-(nSetBits+ nOffsetBits);

	cout <<"CacheSize: "<<cacheSize <<",  " <<"BlockSize: "<<cacheLineSize 
		 <<", " <<"associativity: " <<associativity <<endl;
	cout <<"Number of Sets: " <<nSets <<",  " <<"number of Blocks: " <<nBlocks <<endl;
	cout <<"required set bits: " <<nSetBits <<", required offset bits: " <<nOffsetBits <<endl;
	infile= fopen("NormalMatrixMult.out","r");
	//cout <<"\nNormal Matrix Multiplication Results:" <<endl;
	calculate(infile);
	/*infile= fopen("BlockMatrixMult.out","r");
	cout<<"\nBlock Matrix Multiplication results:" <<endl;
	calculate(infile);*/
	return 0; 
}