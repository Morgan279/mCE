package edu.ecnu.aidadblab.algorithm.subgraph.matching.imlp;

import cn.hutool.core.lang.Console;
import edu.ecnu.aidadblab.algorithm.subgraph.matching.SubgraphMatchingAlgorithm;
import edu.ecnu.aidadblab.config.GlobalConfig;
import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.Match;
import edu.ecnu.aidadblab.data.model.PivotPair;
import edu.ecnu.aidadblab.data.model.Vertex;
import edu.ecnu.aidadblab.index.BiGraphIndex;
import edu.ecnu.aidadblab.processor.CoreProcessor;

import java.util.*;
import java.util.stream.Collectors;

public class VC implements SubgraphMatchingAlgorithm {

    private Map<Vertex, Set<Vertex>> candidateMap;

    private Set<Match> results;

    private Set<Vertex> visitedGVertex;

    private Graph dataGraph;

    private Graph queryGraph;

    private BiGraphIndex BI;

    private Map<Vertex, Vertex> pivotMap;

    Map<Vertex, Set<Vertex>> indexingBackwardNeighbors;

    Map<Vertex, Set<Vertex>> matchingBackwardNeighbors;

    private Map<Vertex, Integer> coreValueMap;

    private Map<Vertex, Integer> coreDegreeMap;

    private int N_Q;

    @Override
    public Set<Match> match(Graph dataGraph, Graph queryGraph) {
        this.dataGraph = dataGraph;
        this.queryGraph = queryGraph;
        this.N_Q = queryGraph.adjList.keySet().size();
        candidateMap = new HashMap<>();
        results = new HashSet<>();
        visitedGVertex = new HashSet<>();

        CoreProcessor coreProcessor = new CoreProcessor();
        this.coreValueMap = coreProcessor.getCoreValueMap(queryGraph);
        this.generateCoreDegreeMap();
        this.extractCandidates();
        if (GlobalConfig.DEBUG) {
            for (Vertex u : candidateMap.keySet()) {
                Console.log("{} => {}", u.label, candidateMap.get(u));
            }
        }

        int level = 0;
        Vertex[] matchingOrder = this.generateMatchingOrder();
        this.constructIndex();

        Vertex u = matchingOrder[0];
        Match curMatch = new Match();
        for (Vertex v : candidateMap.get(u)) {
            curMatch.add(u, v);
            visitedGVertex.add(v);
            enumerate(matchingOrder, curMatch, level + 1);
            visitedGVertex.remove(v);
            curMatch.remove(u);
        }

        return results;
    }

    private void enumerate(Vertex[] matchingOrder, Match curMatch, int level) {
        if (level == N_Q) {
            results.add(curMatch.clone());
            return;
        }

        Vertex u = matchingOrder[level];
        Vertex u2 = pivotMap.get(u);
        PivotPair pivotPair = new PivotPair(u2, u);
        List<Vertex> unmatchedNeighbors = Optional.ofNullable(BI.getNeighbors(pivotPair, curMatch.get(u2))).orElse(Collections.emptyList());

        for (Vertex v : unmatchedNeighbors) {
            if (!visitedGVertex.contains(v) && validate(curMatch, v, matchingOrder, level, u2)) {
                curMatch.add(u, v);
                visitedGVertex.add(v);
                enumerate(matchingOrder, curMatch, level + 1);
                visitedGVertex.remove(v);
                curMatch.remove(u);
            }
        }

    }

    private void extractCandidates() {
        Vertex[] indexingOrder = this.generateIndexingOrder();
        if (GlobalConfig.DEBUG) {
            Console.log("query graph: {}", queryGraph);
            for (Vertex x : indexingOrder) {
                Console.log("indexing: {}", x);
            }
        }

        // forward stage
        Vertex u = indexingOrder[0];
        for (Vertex v : queryGraph.adjList.keySet()) {
            candidateMap.put(v, new HashSet<>());
        }

        for (Vertex v : dataGraph.adjList.keySet()) {
            if (LDF(u, v) && NLF(u, v)) {
                candidateMap.get(u).add(v);
            }
        }

        if (GlobalConfig.DEBUG) {
            Console.log("after LDF and NLF prune");
            for (Vertex v : candidateMap.keySet()) {
                Console.log("u: {} u.C: {}", v.label, candidateMap.get(v));
            }
        }

        for (int i = 1; i < indexingOrder.length; ++i) {
            u = indexingOrder[i];
            boolean isFirst = true;
            Set<Vertex> intersectionResults = new HashSet<>();

            for (Vertex uBNeighbor : indexingBackwardNeighbors.get(u)) {
                Set<Vertex> candidatesNeighbors = new HashSet<>();
                for (Vertex v : candidateMap.get(uBNeighbor)) {
                    candidatesNeighbors.addAll(dataGraph.getNeighbors(v));
                }
                if (isFirst) {
                    intersectionResults.addAll(candidatesNeighbors);
                    isFirst = false;
                } else {
                    intersectionResults.removeIf(v -> !candidatesNeighbors.contains(v));
                }
            }

            for (Vertex v : intersectionResults) {
                if (LDF(u, v) && NLF(u, v)) {
                    candidateMap.get(u).add(v);
                }
            }
        }

        if (GlobalConfig.DEBUG) {
            Console.log("after forward stage");
            for (Vertex v : candidateMap.keySet()) {
                Console.log("u: {} u.C: {}", v.label, candidateMap.get(v));
            }
        }
        //backward stage
        for (int i = indexingOrder.length - 1; i >= 0; --i) {
            u = indexingOrder[i];
            Map<String, List<Vertex>> NLS = this.generateNLS(u);
            for (Iterator<Vertex> it = candidateMap.get(u).iterator(); it.hasNext(); ) {
                Vertex v = it.next();
                boolean validate = true;
                for (String l : NLS.keySet()) {
                    Set<Vertex> X = new HashSet<>();
                    int j = 0;
                    for (Vertex u2 : NLS.get(l)) {
                        ++j;
                        Set<Vertex> Y = new HashSet<>();
                        for (Vertex vNeighbor : dataGraph.getNeighbors(v)) {
                            if (candidateMap.get(u2).contains(vNeighbor)) {
                                Y.add(vNeighbor);
                            }
                        }
                        X.addAll(Y);
                        if (Y.isEmpty() || X.size() < j) {
                            it.remove();
                            validate = false;
                            break;
                        }
                    }
                    if (!validate) break;
                }
            }

            if (GlobalConfig.DEBUG) {
                Console.log("{}: intermidiate=========================================", u);
                for (Vertex v : candidateMap.keySet()) {
                    Console.log("u: {} u.C: {}", v.label, candidateMap.get(v));
                }
            }

            for (Vertex uNeighbor : queryGraph.getNeighbors(u)) {
                for (Iterator<Vertex> it = candidateMap.get(uNeighbor).iterator(); it.hasNext(); ) {
                    boolean isEmpty = true;
                    Vertex v = it.next();
                    for (Vertex vNeighbor : dataGraph.getNeighbors(v)) {
                        if (candidateMap.get(u).contains(vNeighbor)) {
                            isEmpty = false;
                            break;
                        }
                    }
                    if (isEmpty) {
                        it.remove();
                    }
                }
            }
        }

        if (GlobalConfig.DEBUG) {
            Console.log("after backward stage");
            for (Vertex v : candidateMap.keySet()) {
                Console.log("u: {} u.C: {}", v.label, candidateMap.get(v));
            }
        }

    }

    private Map<String, List<Vertex>> generateNLS(Vertex u) {
        Map<String, List<Vertex>> NLS = new HashMap<>();
        for (Vertex neighbor : queryGraph.getNeighbors(u)) {
            if (!NLS.containsKey(neighbor.label)) {
                NLS.put(neighbor.label, new ArrayList<>());
            }
            NLS.get(neighbor.label).add(neighbor);
        }

        return NLS;
    }

    private void constructIndex() {
        BI = new BiGraphIndex();
        for (Vertex u : pivotMap.keySet()) {
            Vertex u2 = pivotMap.get(u);
            PivotPair pivotPair = new PivotPair(u2, u);

            for (Vertex v : candidateMap.get(u2)) {
                BI.addVertex(pivotPair, v);
                for (Vertex v2 : dataGraph.getNeighbors(v)) {
                    if (candidateMap.get(u).contains(v2)) {
                        BI.addVertex(pivotPair, v2);
                        BI.addNeighbor(pivotPair, v, v2);
                    }
                }
            }
        }

        if (GlobalConfig.DEBUG) {
            System.out.println(BI);
        }

    }

    private Vertex[] generateIndexingOrder() {
        Vertex[] indexingOrder = new Vertex[N_Q];
        indexingBackwardNeighbors = new HashMap<>();

        for (Vertex u : queryGraph.adjList.keySet()) {
            indexingBackwardNeighbors.put(u, new HashSet<>());
        }

        Vertex uStart = null;
        int maxCoreValue = -1;
        for (Vertex u : coreValueMap.keySet()) {
            if (coreValueMap.get(u) > maxCoreValue) {
                maxCoreValue = coreValueMap.get(u);
                uStart = u;
            } else if (coreValueMap.get(u) == maxCoreValue && coreDegreeMap.get(u) > coreDegreeMap.get(uStart)) {
                uStart = u;
            } else if (coreValueMap.get(u) == maxCoreValue && coreDegreeMap.get(u).equals(coreDegreeMap.get(uStart)) && queryGraph.getDegree(u) > queryGraph.getDegree(uStart)) {
                uStart = u;
            }
        }
        int cnt = 0;
        indexingOrder[cnt++] = uStart;

        for (Vertex u : queryGraph.getNeighbors(uStart)) {
            Set<Vertex> indexingOrderSet = Arrays.stream(indexingOrder).collect(Collectors.toSet());
            if (indexingOrderSet.contains(u)) continue;
            indexingBackwardNeighbors.get(u).add(uStart);
        }

        while (cnt < N_Q) {
            int maxNeighborSize = -1;
            for (Vertex u : queryGraph.adjList.keySet()) {
                Set<Vertex> indexingOrderSet = Arrays.stream(indexingOrder).collect(Collectors.toSet());
                if (indexingOrderSet.contains(u)) continue;

                if (indexingBackwardNeighbors.get(u).size() > maxNeighborSize) {
                    maxNeighborSize = indexingBackwardNeighbors.get(u).size();
                    uStart = u;
                } else if (indexingBackwardNeighbors.get(u).size() == maxNeighborSize && coreValueMap.get(u) > coreValueMap.get(uStart)) {
                    uStart = u;
                } else if (indexingBackwardNeighbors.get(u).size() == maxNeighborSize && coreValueMap.get(u).equals(coreValueMap.get(uStart)) && coreDegreeMap.get(u) > coreDegreeMap.get(uStart)) {
                    uStart = u;
                } else if (indexingBackwardNeighbors.get(u).size() == maxNeighborSize && coreValueMap.get(u).equals(coreValueMap.get(uStart)) && coreDegreeMap.get(u).equals(coreDegreeMap.get(uStart)) && queryGraph.getDegree(u) > queryGraph.getDegree(uStart)) {
                    uStart = u;
                }
            }

            indexingOrder[cnt++] = uStart;
            for (Vertex u : queryGraph.getNeighbors(uStart)) {
                Set<Vertex> indexingOrderSet = Arrays.stream(indexingOrder).collect(Collectors.toSet());
                if (indexingOrderSet.contains(u)) continue;
                indexingBackwardNeighbors.get(u).add(uStart);
            }
        }

        return indexingOrder;
    }

    private Vertex[] generateMatchingOrder() {
        Vertex[] matchingOrder = new Vertex[N_Q];
        Map<Vertex, Map<Vertex, Float>> weightMap = this.generateWeightMap();
        Map<Vertex, Float> minWeightMap = new HashMap<>();
        matchingBackwardNeighbors = new HashMap<>();
        pivotMap = new HashMap<>();
        Set<Vertex> UN = new HashSet<>();
        Set<Vertex> VC = new HashSet<>();
        Set<Vertex> VNC = new HashSet<>();

        for (Vertex u : queryGraph.adjList.keySet()) {
            matchingBackwardNeighbors.put(u, new HashSet<>());
            minWeightMap.put(u, (float) dataGraph.adjList.keySet().size());
            if (coreValueMap.get(u) >= 2) VC.add(u);
            else VNC.add(u);
        }

        Set<Vertex> prioritySet = VC.isEmpty() ? VNC : VC;
        Vertex uStart = null;
        float minValue = Float.MAX_VALUE;
        for (Vertex u : prioritySet) {
            float curValue = (float) candidateMap.get(u).size() / coreValueMap.get(u);
            if (curValue < minValue) {
                minValue = curValue;
                uStart = u;
            }
        }
        int cnt = 0;
        matchingOrder[cnt++] = uStart;

        addPivot(uStart, UN, matchingOrder, weightMap, minWeightMap);

        while (cnt < VC.size()) {
            Set<Vertex> temp = new HashSet<>(UN);
            temp.removeIf(u -> !VC.contains(u));
            minValue = Float.MAX_VALUE;
            for (Vertex u : temp) {
                int BN = matchingBackwardNeighbors.get(u).size();
                float curMinWeight = minWeightMap.get(u);
                float curWeight = curMinWeight / BN / BN;
                if (curWeight < minValue) {
                    minValue = curWeight;
                    uStart = u;
                } else if (curWeight == minValue && curMinWeight / coreValueMap.get(u) / coreValueMap.get(u) < minValue) {
                    uStart = u;
                } else if (curWeight == minValue && curMinWeight / coreValueMap.get(u) / coreValueMap.get(u) == minValue && curMinWeight / coreDegreeMap.get(u) / coreDegreeMap.get(u) < minValue) {
                    uStart = u;
                }
            }
            matchingOrder[cnt++] = uStart;
            UN.remove(uStart);
            addPivot(uStart, UN, matchingOrder, weightMap, minWeightMap);
        }


        while (cnt < N_Q) {
            Set<Vertex> temp = new HashSet<>(UN);
            temp.removeIf(u -> !VNC.contains(u));
            minValue = Float.MAX_VALUE;
            for (Vertex u : temp) {
                int d = queryGraph.getDegree(u);
                float curWeight = minWeightMap.get(u) / d / d;
                if (curWeight < minValue) {
                    minValue = curWeight;
                    uStart = u;
                }
            }
            matchingOrder[cnt++] = uStart;
            UN.remove(uStart);
            addPivot(uStart, UN, matchingOrder, weightMap, minWeightMap);
        }

        if (GlobalConfig.DEBUG) {
            for (int i = 0; i < matchingOrder.length; ++i) {
                System.out.print(matchingOrder[i].label);
            }
            System.out.println();
            for (Vertex v : pivotMap.keySet()) {
                Console.log("({},{})", v, pivotMap.get(v));
            }
        }


        return matchingOrder;
    }

    private void addPivot(Vertex uStart, Set<Vertex> UN, Vertex[] matchingOrder, Map<Vertex, Map<Vertex, Float>> weightMap, Map<Vertex, Float> minWeightMap) {
        Set<Vertex> Pi = Arrays.stream(matchingOrder).collect(Collectors.toSet());

        for (Vertex u : queryGraph.getNeighbors(uStart)) {
            if (Pi.contains(u)) continue;

            matchingBackwardNeighbors.get(u).add(uStart);
            if (weightMap.get(uStart).get(u) <= minWeightMap.get(u)) {
                minWeightMap.put(u, weightMap.get(uStart).get(u));
                pivotMap.put(u, uStart);
            }
            UN.add(u);
        }
    }

    private Map<Vertex, Map<Vertex, Float>> generateWeightMap() {
        Map<Vertex, Map<Vertex, Float>> weightMap = new HashMap<>(N_Q);

        for (Vertex u : queryGraph.adjList.keySet()) {
            if (!weightMap.containsKey(u)) {
                weightMap.put(u, new HashMap<>());
            }

            for (Vertex uNeighbor : queryGraph.getNeighbors(u)) {
                if (weightMap.get(u).containsKey(uNeighbor)) continue;

                int m = this.calculateM(u, uNeighbor);
                weightMap.get(u).put(uNeighbor, (float) m / candidateMap.get(u).size());

                if (weightMap.containsKey(uNeighbor) && weightMap.get(uNeighbor).containsKey(u)) continue;

                if (!weightMap.containsKey(uNeighbor)) {
                    weightMap.put(uNeighbor, new HashMap<>());
                }

                weightMap.get(uNeighbor).put(u, (float) m / candidateMap.get(uNeighbor).size());
            }
        }

        return weightMap;
    }

    private int calculateM(Vertex u, Vertex u2) {
        int cnt = 0;

        for (Vertex v : candidateMap.get(u)) {
            for (Vertex v2 : candidateMap.get(u2)) {
                if (dataGraph.hasEdge(v, v2)) ++cnt;
            }
        }

        return cnt;
    }

    private boolean validate(Match curMatch, Vertex v, Vertex[] matchingOrder, int i, Vertex p) {
        for (Vertex u : matchingBackwardNeighbors.get(matchingOrder[i])) {
            if (u.equals(p)) continue;
            if (!dataGraph.hasEdge(v, curMatch.get(u))) return false;
        }
        return true;
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

            if (uL > vL) {
                return false;
            }
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

    private void generateCoreDegreeMap() {
        coreDegreeMap = new HashMap<>(N_Q);
        for (Vertex u : queryGraph.adjList.keySet()) {
            if (coreValueMap.get(u) == 1) {
                coreDegreeMap.put(u, 0);
                continue;
            }
            int coreDegree = 0;
            for (Vertex v : queryGraph.getNeighbors(u)) {
                if (coreValueMap.get(v) == 2) ++coreDegree;
            }
            coreDegreeMap.put(u, coreDegree);
        }
    }
}
