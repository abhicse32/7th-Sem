#!/bin/bash

prog=$1;
tests=$2;

debug=1;

#	export file=`ls -d1 ../CS*B* | grep -v tar | grep -v 000`;
	export corrects=0;
	export wrongs=0;
	
	export starttime=`date '+%s'`
	for t in {1..10}; do
		export output=`$prog <$tests/$t.test`
		if [ "`$prog < $tests/$t.test | grep ERROR`" = "" ]; then
			export relevantoutput=`$prog <$tests/$t.test | sed 's/^[ \t]*//' | sed 's/[ \t]*$//' | sed 's/[ \t]+/ /g' | sed 's/<[ \t]*/</g' | sed 's/[ \t]*>/ > /g' | sed 's/[ \t]*=[ \t]*/=/g' | sed 's/ [a-z]*=""//g' | awk '{if ($0 ~ /=/) { if ($2 < $3 && $3 < $4) print $0; else if ($2 < $3 && $2 < $4) print $2 " " $4 " " $3; else if ($2 < $3) print $4 " " $2 " " $3; else if ($2 < $4) print $3 " " $2 " " $4; else if ($3 < $4) print $3 " " $4 " " $2; else print $4 " " $3 " " $2;} else {print;}}'`
		else
			export relevantoutput="ERROR";
		fi

		if [ "`grep ERROR $tests/$t.answer`" = "" ]; then
			export          golden=`cat $tests/$t.answer | sed 's/^[ \t]*//' | sed 's/[ \t]*$//' | sed 's/[ \t]+/ /g' | sed 's/<[ \t]*/</g' | sed 's/[ \t]*>/ > /g' | sed 's/[ \t]*=[ \t]*/=/g' | sed 's/ [a-z]*=""//g' | awk '{if ($0 ~ /=/) { if ($2 < $3 && $3 < $4) print $0; else if ($2 < $3 && $2 < $4) print $2 " " $4 " " $3; else if ($2 < $3) print $4 " " $2 " " $3; else if ($2 < $4) print $3 " " $2 " " $4; else if ($3 < $4) print $3 " " $4 " " $2; else print $4 " " $3 " " $2;} else {print;}}'`
		else
			export golden=`cat $tests/$t.answer`;
		fi

		export relevantgolden=$golden;
		if [ "$relevantoutput" = "$relevantgolden" ]; then
			if [ "$debug" -eq 1 ]; then
				echo "$t Correct";
				#echo "golden=$relevantgolden";
				#echo "actual=$relevantoutput";
			fi
			corrects=`expr $corrects + 1`;
		else
			if [ "$debug" -eq 1 ]; then
				#echo "golden=$relevantgolden";
				#echo "actual=$relevantoutput";
				echo "$t Wrong";
			fi
			wrongs=`expr $wrongs + 1`;
		fi
	done
	export endtime=`date '+%s'`
	export totaltime=`expr $endtime - $starttime`
	echo $1 $corrects $wrongs #$totaltime
