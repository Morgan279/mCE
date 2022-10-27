package edu.ecnu.aidadblab.data.model;

import java.util.List;

public class FuzzyQueryGraph {

    public static Graph generateFuzzyQueryGraph(Graph queryGraph) {
        Graph fuzzyQueryGraph = new Graph();
        Vertex Eq = queryGraph.entityVertexes.iterator().next();
        fuzzyQueryGraph.addVertex(Eq);
        List<Vertex> neighbors = queryGraph.getNeighbors(Eq);
        for (int i = 0, len = neighbors.size(); i < len; ++i) {
            Vertex neighbor = neighbors.get(i);
            fuzzyQueryGraph.addVertex(neighbor);
            fuzzyQueryGraph.addEdge(Eq, neighbor);
        }
        return fuzzyQueryGraph;
    }

}
