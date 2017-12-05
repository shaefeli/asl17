#!/usr/bin/env python
import matplotlib.pyplot as plt
import numpy as np


MWNumbers=[8,16,32,64]
virtualClients=[1,2,4,8,16,32]

throughput1=np.empty((len(MWNumbers),len(virtualClients)))
throughputStd1=np.empty((len(MWNumbers),len(virtualClients)))
throughput2=np.empty((len(MWNumbers),len(virtualClients)))
throughputStd2=np.empty((len(MWNumbers),len(virtualClients)))

def readLine(line,mwNumb,mw):
	words = line.split(",");
	lineTitle = words[0];
	words = np.array(words);
	words = np.delete(words,0);
	if words.size != 0:
		if mw==1:
			if lineTitle=="Throughput ":
				numbers = words.astype(float);
				throughput1[mwNumb] = numbers
			elif lineTitle=="std Throughput ":
				numbers = words.astype(float);
				throughputStd1[mwNumb] = numbers
		elif mw==2:
			if lineTitle=="Throughput ":
				numbers = words.astype(float);
				throughput2[mwNumb] = numbers
			elif lineTitle=="std Throughput ":
				numbers = words.astype(float);
				throughputStd2[mwNumb] = numbers


for i in range(0,len(MWNumbers)):
	statFile = open('mw01_thread'+str(MWNumbers[i])+"_rep2.txt",'r');
	for line in statFile:
		readLine(line,i,1)

for i in range(0,len(MWNumbers)):
	statFile = open('mw02_thread'+str(MWNumbers[i])+"_rep2.txt",'r');
	for line in statFile:
		readLine(line,i,2)

toPlot = throughput1+throughput2
toPlotStd = throughputStd1+throughputStd2
print(toPlot)
factor=1
title = "Total throughput of the two middlewares"
xlabel = "number of virtual clients"
ylabel = "throughput [ops/s]"
colors=['r','g','b','y']
fig,ax = plt.subplots()
for i in range(0,len(MWNumbers)):
	ax.errorbar(virtualClients,toPlot[i]/factor,toPlotStd[i]/factor,color=colors[i], label='MWT'+str(MWNumbers[i]),capsize=3)
	ax.legend()
plt.ticklabel_format(style='sci')
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('totalThroughput')


