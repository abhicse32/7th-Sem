#!/usr/bin/env python
import re,os
test='testcases/'
passed=0
os.system("make clean");
os.system("make")
for ii in range(1,11):
	os.system('./Assignment1 <'+test+str(ii)+'.test >'+test+str(ii)+'.output')
	a=open(test+str(ii)+'.answer','r').readlines()
	b=open(test+str(ii)+'.output','r').readlines()
	a2=[]
	b2=[]
	if a[0][:5]=='ERROR':
		if len(b[0])>=5 and b[0][:5]=='ERROR':
			print ii,'Passed (output: ERROR)'
			passed+=1
		else:
			print ii,'Failed'
		continue
	for i in a:
		temp = re.findall('[a-zA-Z]+',i)
		if len(temp)==4:
			a2.append('+'.join([temp[3]]+sorted(temp[:2])))
	for i in b:
		temp = re.findall('[a-zA-Z]+',i)
		if len(temp)==4:
			b2.append('+'.join([temp[3]]+sorted(temp[:2])))
	a2.sort()
	b2.sort()
	if a2==b2:
		print ii,'Passed'
		passed+=1
	else:
		print ii,'Failed'
print str(passed)+'/10'
