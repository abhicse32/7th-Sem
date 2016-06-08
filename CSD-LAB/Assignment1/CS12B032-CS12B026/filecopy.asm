section .text 
	global _start

_start:
	mov eax, inputprompt
	mov ebx, inputpromptl
	mov edx, infilename
	call enter_filename

	mov eax, outputprompt
	mov ebx, outputpromptl
	mov edx, outfilename
	call enter_filename

	; open inputfile, if it exists
	mov ebx, infilename
	mov eax, 5
	mov ecx, 2
	mov edx, 0777
	int 80h

	mov [fd_in], eax

	mov eax, 10
	mov ebx, outfilename
	int 80h
	
	; create outputfile
	mov eax, 8
	mov ebx, outfilename
	mov ecx, 0777
	int 80h

	mov [fd_out],eax

readfile:
	mov eax, 3
	mov ebx, [fd_in]
	mov ecx, buff
	mov edx, buflen
	int 80h

	mov edx, eax
	mov ecx, buff
	mov eax, 4
	mov ebx, [fd_out]
	int 80h

	cmp eax, buflen
	jl close_files
	jmp readfile

close_files:
	mov eax, 6
	mov ebx, [fd_in]
	mov eax, 6
	mov ebx, [fd_out]
	call another_file

another_file:
	mov eax, 4
	mov ebx, 1
	mov ecx, nextprompt
	mov edx, nextpromptl
	int 80h

	mov eax, 3
	mov ebx, 2
	mov ecx, yorno
	mov edx, 2
	int 80h

	cmp byte [yorno], 'n'
	je exit
	call _start

exit:
	mov eax, 1
	int 80h

enter_filename:
	push edx
	mov ecx, eax
	mov edx, ebx
	mov eax, 4
	mov ebx, 1
	int 80h

	pop edx
	mov ecx, edx
	push edx
	mov edx, filelen
	mov eax, 3
	mov ebx, 2
	int 80h

	pop edx
	dec eax				;remove newline
	mov ecx,edx
	add ecx,eax
	mov byte [ecx],0
	ret 

section .data
	inputprompt: db 'enter input filename:',0
	inputpromptl: equ $ - inputprompt
	outputprompt: db 'enter output filename:',0
	outputpromptl: equ $- outputprompt
	nextprompt: db 'want to perform another operation(y or n):',0
	nextpromptl:  equ $ - nextprompt

section .bss
	filelen: equ 30
	buflen: equ 100
	infilename: resb filelen
	outfilename: resb filelen
	buff: resb buflen
	fd_in: resb 4
	fd_out: resb 4
	yorno: resb 2