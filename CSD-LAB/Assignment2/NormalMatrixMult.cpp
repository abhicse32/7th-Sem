#include <iostream>
#include <ctime>
#include <cstdlib>
using namespace std;

int **createMatrix(int rows, int columns){
	int ** matrix= new int*[rows];
	
	for(int i=0; i < rows; i++){

		matrix[i]= new int[columns];
		for(int j=0;j< columns; j++)
			matrix[i][j]= rand()%10;
	}
	return matrix;
}

int ** normalMult(int **matrix1, int rows1, int columns1,
				  int **matrix2, int rows2, int columns2){
	
	int i, j, k;
	int **resultMatrix= new int*[rows1];
	for(i=0; i < rows1 ;i++)
		resultMatrix[i]= new int[columns2];

	for(i=0;i< rows1; i++){
		for(j=0;j< columns2; j++){

			resultMatrix[i][j]=0;
			for(k=0; k< columns1; k++){
				resultMatrix[i][j] += matrix1[i][k]*matrix2[k][j];
			}
		}
	}
	return resultMatrix;
}

int main(int argc, char* argv[]){
	int rows1, columns1;
	int rows2, columns2;
	int block_rows, block_columns;

	cout << "enter the size(rows x columns) of the first matrix:";
	cin >> rows1 >> columns1;

	if(rows1 <=0 || columns1 <=0){
		cout << "Error: memory allocation failed for matrix1: (enter +ve values for row or column size)\n";
		exit(1);
	}

	cout << "enter the size(rows x columns) of the second matrix:";
	cin >> rows2 >> columns2;

	if(rows2 <=0 || columns2 <=0 ){
		cout << "Error: memory allocation failed for matrix2: (enter +ve values for row or column size)\n";
		exit(1);
	}

	else if(columns1 != rows2 ){
		cout <<"Error: matrix multiplication not possible: (columns1!= rows2)\n";
		exit(1);
	}

	srand(time(NULL));

	int ** matrix1= createMatrix(rows1, columns1);
	int ** matrix2= createMatrix(rows2, columns2);
	int ** normalMultRes= normalMult(matrix1, rows1, columns1,
									 matrix2, rows2, columns2);
	return 0;
}
