#!/usr/bin/env python
import matplotlib.pyplot as plt
import numpy as np


MWNumbers=[8,16,32,64]
virtualClients=[1,2,4,8,16,32]

timesInQueue=np.empty((len(MWNumbers),len(virtualClients)))
timesInQueueStd=np.empty((len(MWNumbers),len(virtualClients)))
throughput=np.empty((len(MWNumbers),len(virtualClients)))
throughputStd=np.empty((len(MWNumbers),len(virtualClients)))
serviceTimes=np.empty((len(MWNumbers),len(virtualClients)))
serviceTimesStd=np.empty((len(MWNumbers),len(virtualClients)))
timesInGet=np.empty((len(MWNumbers),len(virtualClients)))
timesInGetStd=np.empty((len(MWNumbers),len(virtualClients)))
timesInSet=np.empty((len(MWNumbers),len(virtualClients)))
timesInSetStd=np.empty((len(MWNumbers),len(virtualClients)))
queueLength=np.empty((len(MWNumbers),len(virtualClients)))
queueLengthStd=np.empty((len(MWNumbers),len(virtualClients)))
numberMisses=np.empty((len(MWNumbers),len(virtualClients)))
numberMissesStd=np.empty((len(MWNumbers),len(virtualClients)))

def readLine(line,mwNumb):
	words = line.split(",");
	lineTitle = words[0];
	words = np.array(words);
	words = np.delete(words,0);
	#print(lineTitle)
	if words.size != 0:
		if lineTitle=="Times in queue ":
			numbers = words.astype(float);
			timesInQueue[mwNumb] = numbers
		elif lineTitle=="std Times in queue ":
			numbers = words.astype(float);
			timesInQueueStd[mwNumb] = numbers
		elif lineTitle=="Throughput ":
			numbers = words.astype(float);
			throughput[mwNumb] = numbers
		elif lineTitle=="std Throughput ":
			numbers = words.astype(float);
			throughputStd[mwNumb] = numbers
		elif lineTitle=="Service times ":
			numbers = words.astype(float);
			serviceTimes[mwNumb] = numbers
		elif lineTitle=="std Service times ":
			numbers = words.astype(float);
			serviceTimesStd[mwNumb] = numbers
		elif lineTitle=="Times in get ":
			numbers = words.astype(float);
			timesInGet[mwNumb] = numbers
		elif lineTitle=="std Times in get ":
			numbers = words.astype(float);
			timesInGetStd[mwNumb] = numbers
		elif lineTitle=="Times in set ":
			numbers = words.astype(float);
			timesInSet[mwNumb] = numbers
		elif lineTitle=="std Times in set ":
			numbers = words.astype(float);
			timesInSetStd[mwNumb] = numbers
		elif lineTitle=="Queue lengths ":
			numbers = words.astype(float);
			queueLength[mwNumb] = numbers
		elif lineTitle=="std Queue lengths ":
			numbers = words.astype(float);
			queueLengthStd[mwNumb] = numbers
		elif lineTitle=="Number of misses get ":
			numbers = words.astype(float);
			numberMisses[mwNumb] = numbers
		elif lineTitle=="std Number of misses get ":
			numbers = words.astype(float);
			numberMissesStd[mwNumb] = numbers

		

for i in range(0,len(MWNumbers)):
	statFile = open('mw01_thread'+str(MWNumbers[i])+".txt",'r');
	for line in statFile:
		readLine(line,i)

toPlot = numberMisses
toPlotStd = numberMissesStd
title = "Number of misses for one VM"
xlabel = "number of virtual clients"
ylabel = "nr of misses"
colors=['r','g','b','y']
fig,ax = plt.subplots()
for i in range(0,len(MWNumbers)):
	ax.errorbar(virtualClients,toPlot[i],toPlotStd[i],color=colors[i], label='MWT'+str(MWNumbers[i]),capsize=3)
	ax.legend()
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.show()

						
