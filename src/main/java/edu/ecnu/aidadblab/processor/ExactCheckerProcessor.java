package edu.ecnu.aidadblab.processor;

import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.Match;
import edu.ecnu.aidadblab.data.model.Vertex;

import java.util.*;

public class ExactCheckerProcessor {

    private Graph dataGraph;

    private Graph queryGraph;

    private int QUERY_SIZE;

    private boolean result;

    private Set<Vertex> visitedGVertex;

    private Map<Vertex, Vertex> pivotMap;

    public boolean check(Vertex v, Graph dataGraph, Graph queryGraph) {
        this.dataGraph = dataGraph;
        this.queryGraph = queryGraph;
        this.QUERY_SIZE = queryGraph.adjList.size();
        this.pivotMap = new HashMap<>();
        Vertex[] matchingOrder = generateMatchingOrder(queryGraph);
        result = false;
        visitedGVertex = new HashSet<>();

        int level = 0;
        Match curMatch = new Match();
        curMatch.add(matchingOrder[0], v);
        visitedGVertex.add(v);
        enumerate(matchingOrder, curMatch, level + 1);

        return result;
    }

    private void enumerate(Vertex[] matchingOrder, Match curMatch, int level) {
        if (level == QUERY_SIZE) {
            result = true;
            return;
        }

        Vertex u = matchingOrder[level];
        for (Vertex v : dataGraph.getNeighbors(curMatch.get(pivotMap.get(u)))) {
            if (result) break;
            if (!visitedGVertex.contains(v) && validate(v, curMatch, matchingOrder, level)) {
                curMatch.add(u, v);
                visitedGVertex.add(v);
                enumerate(matchingOrder, curMatch, level + 1);
                visitedGVertex.remove(v);
                curMatch.remove(u);
            }
        }
    }

    private Vertex[] generateMatchingOrder(Graph queryGraph) {
        Vertex[] matchingOrder = new Vertex[QUERY_SIZE];
        int cnt = 0;
        Vertex u = queryGraph.entityVertexes.iterator().next();
        Queue<Vertex> queue = new LinkedList<>();
        Set<Vertex> vis = new HashSet<>();
        queue.add(u);
        while (!queue.isEmpty()) {
            Vertex next = queue.poll();
            vis.add(next);
            matchingOrder[cnt++] = next;
            for (Vertex neighbor : queryGraph.getNeighbors(next)) {
                if (!vis.contains(neighbor)) {
                    queue.add(neighbor);
                    pivotMap.put(neighbor, next);
                }
            }
        }
        return matchingOrder;
    }

    private boolean validate(Vertex v, Match curMatch, Vertex[] matchingOrder, int level) {
        Vertex u = matchingOrder[level];
        if (!baseFilter(u, v)) return false;
        for (int i = 0; i < level; ++i) {
            Vertex w = matchingOrder[i];
            if (queryGraph.hasEdge(u, w) && !dataGraph.hasEdge(v, curMatch.get(w))) {
                //Console.log("u:{} w:{} v:{} m:{}", u, w, v, curMatch.get(w));
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
