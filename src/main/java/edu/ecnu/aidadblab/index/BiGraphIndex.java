package edu.ecnu.aidadblab.index;

import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.PivotPair;
import edu.ecnu.aidadblab.data.model.Vertex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BiGraphIndex {


    private Map<PivotPair, Graph> biGraph = new HashMap<>();

    public void addVertex(PivotPair pivotPair, Vertex v) {
        if (!biGraph.containsKey(pivotPair)) {
            biGraph.put(pivotPair, new Graph());
        }
        Graph graph = biGraph.get(pivotPair);
        if (!graph.hasVertex(v)) {
            graph.addVertex(v);
        }
    }

    public void addNeighbor(PivotPair pivotPair, Vertex v, Vertex vNeighbor) {
        biGraph.get(pivotPair).addEdge(v, vNeighbor);
    }

    public List<Vertex> getNeighbors(PivotPair pivotPair, Vertex v) {
        return biGraph.get(pivotPair).getNeighbors(v);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (PivotPair pivotPair : biGraph.keySet()) {
            stringBuilder.append(pivotPair.uPrime).append("->").append(pivotPair.u).append('\n');
            Graph graph = biGraph.get(pivotPair);
            for (Vertex u : graph.adjList.keySet()) {
                stringBuilder.append(u).append(": ").append(graph.getNeighbors(u)).append('\n');
            }
            stringBuilder.append("======================\n");
        }
        return stringBuilder.toString();
    }
}
