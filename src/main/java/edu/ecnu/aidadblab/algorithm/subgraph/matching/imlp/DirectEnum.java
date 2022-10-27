package edu.ecnu.aidadblab.algorithm.subgraph.matching.imlp;

import edu.ecnu.aidadblab.algorithm.subgraph.matching.SubgraphMatchingAlgorithm;
import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.Match;
import edu.ecnu.aidadblab.data.model.Vertex;

import java.util.*;

public class DirectEnum implements SubgraphMatchingAlgorithm {

    private Graph dataGraph;

    private Graph queryGraph;

    private int QUERY_SIZE;

    private Set<Match> results;

    private Set<Vertex> visitedGVertex;

    @Override
    public Set<Match> match(Graph dataGraph, Graph queryGraph) {
        this.dataGraph = dataGraph;
        this.queryGraph = queryGraph;
        this.QUERY_SIZE = queryGraph.adjList.size();
        Vertex[] matchingOrder = queryGraph.adjList.keySet().toArray(new Vertex[0]);
        results = new HashSet<>();
        visitedGVertex = new HashSet<>();

        int level = 0;
        Vertex u = matchingOrder[0];
        Match curMatch = new Match();
        for (Vertex v : dataGraph.adjList.keySet()) {
            if (baseFilter(u, v)) {
                curMatch.add(u, v);
                visitedGVertex.add(v);
                enumerate(matchingOrder, curMatch, level + 1);
                visitedGVertex.remove(v);
                curMatch.remove(u);
            }
        }

        return results;
    }

    private void enumerate(Vertex[] matchingOrder, Match curMatch, int level) {
        if (level == QUERY_SIZE) {
            results.add(curMatch.clone());
            return;
        }

        Vertex u = matchingOrder[level];
        for (Vertex v : dataGraph.adjList.keySet()) {
            if (!visitedGVertex.contains(v) && validate(v, curMatch, matchingOrder, level)) {
                curMatch.add(u, v);
                visitedGVertex.add(v);
                enumerate(matchingOrder, curMatch, level + 1);
                visitedGVertex.remove(v);
                curMatch.remove(u);
            }
        }
    }

    private boolean validate(Vertex v, Match curMatch, Vertex[] matchingOrder, int level) {
        Vertex u = matchingOrder[level];
        if (!baseFilter(u, v)) return false;

        for (int i = 0; i < level; ++i) {
            Vertex w = matchingOrder[i];
            if (queryGraph.hasEdge(u, w) && !dataGraph.hasEdge(v, curMatch.get(w))) {
                return false;
            }
        }

        return true;
    }

    private boolean baseFilter(Vertex u, Vertex v) {
        return LDF(u, v) && NLF(u, v);
    }

    private boolean LDF(Vertex u, Vertex v) {
        return u.label.equals(v.label) && queryGraph.getDegree(u) <= dataGraph.getDegree(v);
    }

    private boolean NLF(Vertex u, Vertex v) {
        Map<String, Integer> uFrequencyMap = generateLabelFrequencyMap(u, queryGraph);
        Map<String, Integer> vFrequencyMap = generateLabelFrequencyMap(v, dataGraph);

        for (String l : uFrequencyMap.keySet()) {
            int uL = uFrequencyMap.get(l);
            int vL = Optional.ofNullable(vFrequencyMap.get(l)).orElse(0);
            if (uL > vL) return false;
        }

        return true;
    }

    private Map<String, Integer> generateLabelFrequencyMap(Vertex vertex, Graph graph) {
        Map<String, Integer> labelFrequencyMap = new HashMap<>();

        for (Vertex neighbor : graph.getNeighbors(vertex)) {
            if (!labelFrequencyMap.containsKey(neighbor.label)) {
                labelFrequencyMap.put(neighbor.label, 0);
            }
            labelFrequencyMap.put(neighbor.label, labelFrequencyMap.get(neighbor.label) + 1);
        }

        return labelFrequencyMap;
    }

}
