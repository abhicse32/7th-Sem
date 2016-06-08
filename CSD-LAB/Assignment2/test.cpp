#include <iostream>
#include <stdint.h>
#include <stdio.h>
using namespace std;
int main(){
	int a=10;
	int *p=&a;
	unsigned long x=(unsigned long)p;
	printf("%lu, %d\n",x, *p);
	return 0;
}