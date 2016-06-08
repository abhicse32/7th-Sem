#!/usr/bin/env python
import os
os.system("nasm -f elf ackermann.asm -o ackermann.o")
os.system("gcc -o ackermann ackermann.o")
os.system("nasm -f elf question2.asm -o question2.o")
os.system("ld -o question2 question2.o")
os.system("nasm -f elf flcpy.asm -o flcpy.o");
os.system("ld -o flcpy flcpy.o")
