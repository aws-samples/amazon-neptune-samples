'''
Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
SPDX-License-Identifier: MIT-0
'''

import matplotlib.pyplot as plt; plt.rcdefaults()
import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
import networkx as nx

def defaultLabelFormatter(label, colors):
    colors.append('#11cc77')
    
class Visualisation:
    def plotPaths(self, paths, formatter=defaultLabelFormatter):
        # Create a new empty DiGraph
        G=nx.DiGraph()

        # Add the paths we found to DiGraph we just created
        for p in paths:
            for i in range(len(p)-1):
                G.add_edge(p[i],p[i+1])

        # Give the vertices different colors
        colors = []

        for label in G:
            formatter(label, colors)

        # Now draw the graph    
        plt.figure(figsize=(5,5))
        nx.draw(G, node_color=colors, node_size=1200, with_labels=True)
        plt.show()
        
visualisation = Visualisation()