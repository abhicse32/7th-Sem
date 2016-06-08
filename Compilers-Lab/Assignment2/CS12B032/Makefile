a.out: y.tab.c lex.yy.c List.c
	gcc -O3 lex.yy.c y.tab.c List.h 

y.tab.c: a2.y
	yacc -d a2.y

lex.yy.c: a2.l y.tab.h
	lex a2.l

test: a.out
	bash eval.sh ./a.out testcases

clean:
	@rm lex.yy.c y.tab.h y.tab.c a.out

tar:
	@mv a.out /tmp/
	@mv testcases /tmp/
	@tar czf `ls -d1 ../CS* | grep -v tar | grep -v 000`.tar.gz `ls -d1 ../CS* | grep -v tar | grep -v 000`
	@mv /tmp/testcases .
	@mv /tmp/a.out .
	@ls -d1 ../CS*.tar.gz | grep -v 000
