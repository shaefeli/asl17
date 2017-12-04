#!/usr/bin/env python
import matplotlib.pyplot as plt
import numpy as np


MWNumbers=[32,64]
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
	statFile = open('mw01_thread'+str(MWNumbers[i])+"_ratio0_rep2.txt",'r');
	for line in statFile:
		readLine(line,i)

toPlot = timesInGet
toPlotStd = timesInGetStd
factor=1e6
title = "Average answer time of memcached"
xlabel = "number of virtual clients"
ylabel = "Answer time [ms]"
colors=['r','g','b','y']
fig,ax = plt.subplots()
for i in range(0,len(MWNumbers)):
	ax.errorbar(virtualClients,toPlot[i]/factor,toPlotStd[i]/factor,color=colors[i], label='MWT'+str(MWNumbers[i]),capsize=3)
	ax.legend()
plt.ticklabel_format(style='sci')
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('timesInGet')

toPlot = throughput
toPlotStd = throughputStd
factor=1
title = "Throughput for one MW"
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
plt.savefig('throughput')

toPlot = serviceTimes
toPlotStd = serviceTimesStd
factor=1e6
title = "Average service time in one MW"
xlabel = "number of virtual clients"
ylabel = "service time [ms]"
colors=['r','g','b','y']
fig,ax = plt.subplots()
for i in range(0,len(MWNumbers)):
	ax.errorbar(virtualClients,toPlot[i]/factor,toPlotStd[i]/factor,color=colors[i], label='MWT'+str(MWNumbers[i]),capsize=3)
	ax.legend()
plt.ticklabel_format(style='sci')
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('serviceTime')

toPlot = timesInQueue
toPlotStd = timesInQueueStd
factor=1e6
title = "Average time of a request in queue"
xlabel = "number of virtual clients"
ylabel = "Time in queue [ms]"
colors=['r','g','b','y']
fig,ax = plt.subplots()
for i in range(0,len(MWNumbers)):
	ax.errorbar(virtualClients,toPlot[i]/factor,toPlotStd[i]/factor,color=colors[i], label='MWT'+str(MWNumbers[i]),capsize=3)
	ax.legend()
plt.ticklabel_format(style='sci')
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('timeInQueue')

toPlot = queueLength
toPlotStd = queueLengthStd
factor=1
title = "Average queue length of one VM"
xlabel = "number of virtual clients"
ylabel = "Average queue length"
colors=['r','g','b','y']
fig,ax = plt.subplots()
for i in range(0,len(MWNumbers)):
	ax.errorbar(virtualClients,toPlot[i]/factor,toPlotStd[i]/factor,color=colors[i], label='MWT'+str(MWNumbers[i]),capsize=3)
	ax.legend()
plt.ticklabel_format(style='sci')
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('queueLength')

						
