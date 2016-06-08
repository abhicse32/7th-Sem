all: Assignment1

Assignment1: Assignment1.lex Assignment1.yacc List.c
	yacc -d Assignment1.yacc
	lex Assignment1.lex
	cc -o Assignment1 lex.yy.c y.tab.c Logic.h

test: Assignment1
	bash eval.sh ./Assignment1 testcases

clean:
	@rm lex.yy.c y.tab.h y.tab.c Assignment1
