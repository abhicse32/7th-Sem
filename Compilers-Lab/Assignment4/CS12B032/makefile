a.out: y.tab.c lex.yy.c list.h
	gcc -O3 lex.yy.c y.tab.c list.c

y.tab.c: a4.y
	yacc -d a4.y

lex.yy.c: a4.l y.tab.h
	lex a4.l

test: a.out
	bash eval.sh ./a.out testcases

clean:	
	@rm lex.yy.c y.tab.h y.tab.c a.out

tar:
	@mv a.out /tmp/
	@mv testcases /tmp/
	@tar czf `ls -d1 ../CS* | grep -v tar | grep -v 000`.tar.gz `ls -d1 ../CS | grep -v tar | grep -v 000`
	@mv /tmp/testcases .
	@mv /tmp/a.out .
	@ls -d1 ../CS*.tar.gz | grep -v 000

# all:coloring
# coloring:coloring.c list.h
# 	gcc -o coloring coloring.c list.c

# clean:
# 	rm coloring