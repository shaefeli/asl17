#!/usr/bin/env python
import matplotlib.pyplot as plt
import numpy as np

statFile = open('statistics.txt','r');
for line in statFile:
	if line.startswith("Configuration"):
		pass
	else:
		words = line.split(",");
		lineTitle = words[0];
		words = np.array(words);
		words = np.delete(words,0);
		print(words);
		if words.size != 0:
			words = np.delete(words,words.size-1);
			numbers = words.astype(float);
			print(numbers)
			plt.plot(numbers);
			plt.title(lineTitle);
			plt.show();

						
