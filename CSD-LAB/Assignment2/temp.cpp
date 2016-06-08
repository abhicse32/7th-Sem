#include <iostream>
#include <cstdio>
#include <ctime>
#include <cstdlib>
using namespace std;

void printMatrix(int ** matrix, int rows, int columns){
	for(int i=0;i< rows; i++){
		for(int j=0; j< columns; j++)
			cout << matrix[i][j] <<" ";
		cout <<endl;
	}
	cout <<endl;
}

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

int **blockMatrixMult(int **matrix1, int rows1, int columns1,
					  int **matrix2, int rows2, int columns2,
					  int block_rows, int block_columns ){
	int i,j,k,l,m,n;
	int start_row_first, end_row_first;
	int start_col_first, end_col_first;
	int start_col_second, end_col_second;

	int rows_first = rows1 % block_rows ? (rows1/block_rows) + 1 : 
									rows1/block_rows;

	int columns_first = columns1 % block_columns ? (columns1/block_columns)+1 :
											columns1/block_columns;

	int rows_second= columns1;
	int columns_second= columns2 % block_rows ? (columns2/block_rows)+1 :
												columns2/block_rows;

	/*cout << rows_first <<"  " << columns_first <<"  " 
							  << columns_second <<endl*/;

	int **resultMatrix= new int*[rows1];

	for(i=0; i< rows1; i++)
		resultMatrix[i]= new int[columns2];

	for(i=0; i< rows_first; i++){
		
		start_row_first= i*block_rows;
		end_row_first= start_row_first + block_rows;

		if(end_row_first > rows1)
			end_row_first = rows1;

		for(j=0; j < columns_second; j++){

			start_col_second= j*block_rows;
			end_col_second =  start_col_second+ block_rows;

			if(end_col_second > columns2)
				end_col_second= columns2;

			for(k=0;k < columns_first ;k++){

				start_col_first= k*block_columns;
				end_col_first= start_col_first+ block_columns;

				if(end_col_first > columns1)
					end_col_first= columns1;

				for(l=start_row_first; l< end_row_first; l++){
					for(m=start_col_second; m< end_col_second; m++){

						for(n=start_col_first; n< end_col_first; n++){
							resultMatrix[l][m] += matrix1[l][n]*matrix2[n][m];
						}
					}
				}
			}
		}
	}
		return resultMatrix;
}
int main(int argc, char* argv[]){
	int rows1, columns1;
	int rows2, columns2;
	int block_rows, block_columns;
	int i,j;

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

	cout << "enter the blocking factor(row x columns):";
	cin >> block_rows >> block_columns;
	if(block_rows > rows1 || block_columns > columns1){
		cout << "Error: inappropriate block size!!"<<endl;
		exit(1);
	}

	srand(time(NULL));

	int ** matrix1= createMatrix(rows1, columns1);
	int ** matrix2= createMatrix(rows2, columns2);
	
	//printMatrix(matrix1, rows1, columns1);
	//printMatrix(matrix2, rows2, columns2);

	int ** normalMultRes= normalMult(matrix1, rows1, columns1,
									 matrix2, rows2, columns2);
	//printMatrix(normalMultRes, rows1, columns2);
	int ** blockMultRes= blockMatrixMult(matrix1, rows1, columns1, 
										matrix2, rows2, columns2,
										block_rows, block_columns);

	//printMatrix(blockMultRes, rows1, columns2);
	return 0;
}
