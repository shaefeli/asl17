#!/usr/bin/env python
import matplotlib.pyplot as plt
import numpy as np
from numpy.random import normal


mGetSize=[1,3,6,9]

timesInQueue=[]
timesInQueueStd=[]
throughput=[]
throughputStd=[]
serviceTimes=[]
serviceTimesStd=[]
timesInGet=[]
timesInGetStd=[]
timesInMGet=[]
timesInMGetStd=[]
timesInMGetMem=[]
timesInMGetMemStd=[]
timesInSet=[]
timesInSetStd=[]
queueLength=[]
queueLengthStd=[]
numberMisses=[]
numberMissesStd=[]


serviceTimesHist6=[]
timesInQueueHist6=[]
timesInGetHist6=[]
timesInMGetHist6=[]
timesInMGetMemHist6=[]

serviceTimesHist9=[]
timesInQueueHist9=[]
timesInGetHist9=[]
timesInMGetHist9=[]
timesInMGetMemHist9=[]


def readLine(line):
	words = line.split(",");
	lineTitle = words[0]
	words=np.array(words)
	words=np.delete(words,0)
	wordsPerClass =[]
	wordsOneClass=[]
	for i in range(0,len(words)):
		if "[]" in words[i]:
			wordsOneClass.append("0")
			wordsOneClass = np.array(wordsOneClass)
			wordsOneClass = wordsOneClass.astype(float)
			wordsPerClass.append(wordsOneClass)
			wordsOneClass=[]
		
		elif "]" in words[i]:
			words[i]=words[i].replace("]","")
			words[i]=words[i].replace("\n","")
			wordsOneClass.append(words[i])
			wordsOneClass = np.array(wordsOneClass)
			wordsOneClass = wordsOneClass.astype(float)
			wordsPerClass.append(wordsOneClass)
			wordsOneClass=[]
		else:
			words[i]=words[i].replace("[","")
			wordsOneClass.append(words[i])	
	
	if len(wordsPerClass) != 0:
		if lineTitle=="Times in queue ":
			for i in range(0,len(mGetSize)):
				timesInQueue.append(np.mean(wordsPerClass[i]))
				timesInQueueStd.append(np.std(wordsPerClass[i]))
			timesInQueueHist6.append(wordsPerClass[2])
			timesInQueueHist9.append(wordsPerClass[3])
		elif lineTitle=="Throughput ":
			for i in range(0,len(mGetSize)):
				throughput.append(np.mean(wordsPerClass[i]))
				throughputStd.append(np.std(wordsPerClass[i]))
		elif lineTitle=="Service times ":
			for i in range(0,len(mGetSize)):
				serviceTimes.append(np.mean(wordsPerClass[i]))
				serviceTimesStd.append(np.std(wordsPerClass[i]))
			serviceTimesHist6.append(wordsPerClass[2])
			serviceTimesHist9.append(wordsPerClass[3])
		elif lineTitle=="Times in mget ":
			for i in range(0,len(mGetSize)):
				timesInMGet.append(np.mean(wordsPerClass[i]))
				timesInMGetStd.append(np.std(wordsPerClass[i]))
			timesInMGetHist6.append(wordsPerClass[2])
			timesInMGetHist9.append(wordsPerClass[3])
		elif lineTitle=="Times in get ":
			for i in range(0,len(mGetSize)):
				timesInGet.append(np.mean(wordsPerClass[i]))
				timesInGetStd.append(np.std(wordsPerClass[i]))
			timesInGetHist6.append(wordsPerClass[2])
			timesInGetHist9.append(wordsPerClass[3])
		elif lineTitle=="Times in set ":
			for i in range(0,len(mGetSize)):
				timesInSet.append(np.mean(wordsPerClass[i]))
				timesInSetStd.append(np.std(wordsPerClass[i]))
		elif lineTitle=="Queue lengths ":
			for i in range(0,len(mGetSize)):
				queueLength.append(np.mean(wordsPerClass[i]))
				queueLengthStd.append(np.std(wordsPerClass[i]))
		elif lineTitle=="Number of misses get ":
			for i in range(0,len(mGetSize)):
				numberMisses.append(np.mean(wordsPerClass[i]))
				numberMissesStd.append(np.std(wordsPerClass[i]))
		elif lineTitle=="Times in mget only memcached part ":
			for i in range(0,len(mGetSize)):
				timesInMGetMem.append(np.mean(wordsPerClass[i]))
				timesInMGetMemStd.append(np.std(wordsPerClass[i]))
			timesInMGetMemHist6.append(wordsPerClass[2])
			timesInMGetMemHist9.append(wordsPerClass[3])
	

statFile = open('mw01_shardedfalse_rep1.txt','r');
for line in statFile:
	readLine(line)


toPlot = [timesInMGetHist6[i]+timesInQueueHist6[i] for i in range(len(timesInQueueHist6))]
toPlot = [toPlot[i]/1e6 for i in range(len(toPlot))]
plt.hist(toPlot,rwidth=0.7)
title = "Non sharded - Middleware"
xlabel = "response time [ms]"
ylabel = "Number of requests"
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('HistMGetsize6')
plt.close()

toPlot = [timesInMGetHist9[i]+timesInQueueHist9[i] for i in range(len(timesInQueueHist9))]
toPlot = [toPlot[i]/1e6 for i in range(len(toPlot))]
plt.hist(toPlot,rwidth=0.7)
title = "Non sharded - Middleware"
xlabel = "response time [ms]"
ylabel = "Number of requests"
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('HistMGetsize9')
plt.close()


mGetSizeString=["1","3","6","9"]
toPlot = timesInQueue
toPlotStd = timesInQueueStd
toPlot = [toPlot[i]/1e6 for i in range(len(toPlot))]
toPlotStd = [toPlotStd[i]/1e6 for i in range(len(toPlotStd))]
factor=1e6
title = "Average time in queue"
xlabel = "mGet size"
ylabel = "Queuing time [ms]"
bar_width=0.35
plt.figure()
plt.bar(mGetSizeString,toPlot,bar_width,color='b',yerr=toPlotStd,capsize=3)
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('timesInQueue')

toPlot = throughput
toPlotStd = throughputStd
factor=1e6
title = "Throughput"
xlabel = "mGet size"
ylabel = "throughput [ops/s]"
plt.figure()
plt.bar(mGetSizeString,toPlot,bar_width,color='b',yerr=toPlotStd,capsize=3)
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('throughput')

toPlot = timesInSet
toPlotStd = timesInSetStd
toPlot = [toPlot[i]/1e6 for i in range(len(toPlot))]
toPlotStd = [toPlotStd[i]/1e6 for i in range(len(toPlotStd))]
factor=1e6
title = "Time spent in the set handling"
xlabel = "mGet size"
ylabel = "time in set [ms]"
plt.figure()
plt.bar(mGetSizeString,toPlot,bar_width,color='b',yerr=toPlotStd,capsize=3)
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('timesInSet')

toPlot = serviceTimes
toPlotStd = serviceTimesStd
toPlot = [toPlot[i]/1e6 for i in range(len(toPlot))]
toPlotStd = [toPlotStd[i]/1e6 for i in range(len(toPlotStd))]
factor=1e6
title = "Service times for a request"
xlabel = "mGet size"
ylabel = "service time [ms]"
plt.figure()
plt.bar(mGetSizeString,toPlot,bar_width,color='b',yerr=toPlotStd,capsize=3)
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('serviceTime')


toPlot = timesInGet
toPlotStd = timesInGetStd
toPlot = [toPlot[i]/1e6 for i in range(len(toPlot))]
toPlotStd = [toPlotStd[i]/1e6 for i in range(len(toPlotStd))]
factor=1e6
title = "Time spent in the get handling"
xlabel = "mGet size"
ylabel = "time in get [ms]"
plt.figure()
plt.bar(mGetSizeString,toPlot,bar_width,color='b',yerr=toPlotStd,capsize=3)
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('timesInGet')

toPlot = timesInMGet
toPlotStd = timesInMGetStd
toPlot = [toPlot[i]/1e6 for i in range(len(toPlot))]
toPlotStd = [toPlotStd[i]/1e6 for i in range(len(toPlotStd))]
factor=1e6
title = "Time spent in the multi get handling"
xlabel = "mGet size"
ylabel = "time in multi get [ms]"
plt.figure()
plt.bar(mGetSizeString,toPlot,bar_width,color='b',yerr=toPlotStd,capsize=3)
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('timesInMGet')

toPlot = timesInMGetMem
toPlotStd = timesInMGetMemStd
toPlot = [toPlot[i]/1e6 for i in range(len(toPlot))]
toPlotStd = [toPlotStd[i]/1e6 for i in range(len(toPlotStd))]
factor=1e6
title = "Time waiting for memcached for multi get"
xlabel = "mGet size"
ylabel = "waiting time [ms]"
plt.figure()
plt.bar(mGetSizeString,toPlot,bar_width,color='b',yerr=toPlotStd,capsize=3)
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('timesInMGetMem')

toPlot = queueLength
toPlotStd = queueLengthStd
factor=1e6
title = "Queue length on one middlware"
xlabel = "mGet size"
ylabel = "queue length"
plt.figure()
plt.bar(mGetSizeString,toPlot,bar_width,color='b',yerr=toPlotStd,capsize=3)
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('queueLength')

toPlot = numberMisses
toPlotStd = numberMissesStd
factor=1e6
title = "Number of misses on one middleware"
xlabel = "mGet size"
ylabel = "number of misses"
plt.figure()
plt.bar(mGetSizeString,toPlot,bar_width,color='b',yerr=toPlotStd,capsize=3)
plt.title(title)
plt.xlabel(xlabel)
plt.ylabel(ylabel)
plt.savefig('numberMisses')





						
