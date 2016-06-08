#include <vector>
#include <iostream>
using namespace std;
int main(){
	vector<int> vect = new vector<int>[5];
	for(int i=0;i<vect.size();i++)
		cout << vect[0] <<endl;
	return 0;
}