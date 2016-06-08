#!/bin/bash

prog=$1;
tests=$2;

debug=1;

	export file=`ls -d1 ../CS*B* | grep -v tar | grep -v 000`;
	export corrects=0;
	export wrongs=0;
	
	export starttime=`date '+%s'`
	for t in {1..10}; do
		export output=`$prog <$tests/$t.test`
		export relevantoutput=`echo $output | sed 's/^[ ]*//' | sed 's/[ ]*$//'`

		export golden=`cat $tests/$t.answer | sed 's/^[ ]*//' | sed 's/[ ]*$//'`;
		export relevantgolden=`echo $golden`;
		if [ "$relevantoutput" = "$relevantgolden" ]; then
			if [ "$debug" -eq 1 ]; then
				echo "$t Correct";
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
	echo $file $corrects $wrongs #$totaltime
