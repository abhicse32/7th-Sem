#!/usr/bin/env python
import os
os.system("nasm -f elf ackermann.asm -o ackermann.o")
os.system("gcc -o ackermann ackermann.o")
os.system("nasm -f elf filecopy.asm -o filecopy.o")
os.system("ld -o filecopy filecopy.o")
