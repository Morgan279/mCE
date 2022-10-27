package edu.ecnu.aidadblab.tool;

import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.Match;
import edu.ecnu.aidadblab.data.model.Vertex;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
public class Filter {

    private Graph dataGraph;

    private Graph queryGraph;

    private Vertex[] matchingOrder;

    public boolean checkCondition(Match curMatch, int level, Vertex v) {
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

    public boolean baseFilter(Vertex u, Vertex v) {
        return LDF(u, v) && NLF(u, v);
    }

    public boolean LDF(Vertex u, Vertex v) {
        return u.label.equals(v.label) && queryGraph.getDegree(u) <= dataGraph.getDegree(v);
    }

    public boolean NLF(Vertex u, Vertex v) {
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
