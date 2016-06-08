section .text
	global main
	extern printf
	extern scanf

section .data
	;message: db 'you entered: %d', 10, 0					;message shows the number entered 
	requestm: db 0xa,'enter the first parameter(m):',0		;message to ask for the first input  
	requestn: db 'enter the second parameter(n):',0			;message to ask for the second input 
	m: times 4 db 0											;allocate 4 bytes to both the parameters
	n: times 4 db 0											;and initialize them to 0
	formatin: db "%d",0										;passed as parameter to scanf to accept input
	formatout: db "result : %d",0							;message to show result
	negError: db 'negative input detected',0				;error message to indicate negative input
	lenneger: equ $-negError
	nextinput: db 'y to continue n to exit:',0
	nextinputl: equ $- nextinput

main:
	mov ebx, m
	mov ecx, requestm									
	call take_input										;prompt user to enter first input
	mov ebx, n
	mov ecx, requestn									
	call take_input										;prompt user to enter second input

	mov ebx, dword[m]									;ebx = m, ecx = n
	mov ecx, dword[n]

	cmp ebx, 0
	jl  error 											; check for the negative inputs
	cmp ecx, 0
	jl error

	mov eax, 0 
	push  ebx
	push  ecx
	call cal_ackermann									
	pop ecx		
	pop ebx

	push eax										; printing result(eax stores the result)		
	push formatout
	call printf
	add esp, 8
	call main									; call main to process further inputs

take_next:
 	mov eax, 4
	mov ebx, 1
	mov ecx, nextinput
	mov edx, nextinputl
	int 80h

	mov eax, 3
	mov ebx, 2
	mov ecx, yorn
	mov edx, 2
	int 80h

	cmp byte [yorn], 'n'
	je exit
	call main

error: 
	push negError									; error function to report negative inputs
	call printf
	add esp, 4
	
	call main

cal_ackermann:
	;enter 0, 0
	push ebp			; enter 0, 0 also works
	mov ebp, esp

	mov ebx, [esp+12]
	mov ecx, [esp+8]

	cmp ebx, 0										; check if first parameter is 0
	je  case1							
	cmp ecx, 0										; check if second parameter is 0	
	je  case2

	dec ecx											; logic to execute ackermann(m-1, ackermann(m, n-1))
	push ebx  										; to call the function as second parameter
	push ecx                                        ; second parameter is decremented and then both 
	call cal_ackermann   							; are pushed on the stack followed by function call
	pop ecx             				
	pop ebx

	dec ebx  										; to call the outer function, first parameter
	push ebx 										; is decremented and return value of first function
	push eax                                        ; call is passed as second parameter to this call
	call cal_ackermann
	pop ecx 										; after call, second parameter is stored back in ecx
	pop ebx 											
	mov esp, ebp
	pop ebp
	;leave
	ret

case1: 												 
	mov eax,ecx 									; if first parameter is 0, then
	inc eax											; eax= ecx+ 1, i.e. eax contains 2nd param+ 1
	;leave
	mov esp, ebp
	pop ebp
	ret

case2:
	dec ebx
	mov edx, dword 1  								; if 2nd parameter is 0, function is called as  
	push ebx										; ackermann(ebx, edx) where ebx= first param -1
	push edx                                        ; edx= 1 which will be latter popped in ecx to
	call cal_ackermann                              ; be used in recursive call
	pop ecx
	pop ebx
	mov esp, ebp
	pop ebp
	;leave
	ret

take_input:   										; function used to take input for both parameters
	push ecx										; ecx contains the message description to ask user for input
	call printf 									; used to print the message pointed to by address contained in ecx
	add esp, 4   									;removes ecx content off the stack 
	push ebx 				                        ;address of m, where the input is going to be stored
  	push formatin 			              	
  	call scanf
    add esp, 8	    								; remove ebx and formatin off the stack
   	ret

 exit:						; terminate the program
  	mov eax, 1
  	mov ebx, 0 
  	int 80h

section .bss
	yorn: resb 2
