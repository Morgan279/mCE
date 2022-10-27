package edu.ecnu.aidadblab.algorithm.mclosest.entity.matching.impl;

import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.algorithm.mclosest.entity.matching.MClosestEntityMatchingAlgorithm;
import edu.ecnu.aidadblab.algorithm.subgraph.matching.imlp.VC;
import edu.ecnu.aidadblab.constant.AngleType;
import edu.ecnu.aidadblab.data.model.*;
import edu.ecnu.aidadblab.processor.ExactMatchProcessor;
import edu.ecnu.aidadblab.tool.CircleScanHelper;
import edu.ecnu.aidadblab.util.SpatialUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ExactScanAlgorithm implements MClosestEntityMatchingAlgorithm {

    private double S;

    private Map<Vertex, Double> maxInvalidRange;

    private List<Vertex> vertexList;

    private List<Vertex> matchVertex;

    private Map<Vertex, JSONObject> locationMap;

    private Map<Vertex, List<Integer>> structTagMap;

    private int QUERY_NUM;

    @Override
    public MatchGroup query(Graph dataGraph, List<Graph> queryGraphs) {
        ExactMatchProcessor exactMatchProcessor = new ExactMatchProcessor(dataGraph, queryGraphs, new VC());
        List<Set<Vertex>> entityVertexList = exactMatchProcessor.candidateDataEntityVertexes;

        locationMap = exactMatchProcessor.getLocationMap(entityVertexList);
        QUERY_NUM = queryGraphs.size();
        vertexList = new ArrayList<>();
        structTagMap = new HashMap<>();

        MatchGroup feasibleGroup = exactMatchProcessor.findFeasibleSolution();
        if (feasibleGroup.diameter == 0) {
            return feasibleGroup;
        }

        S = feasibleGroup.diameter;
        for (int i = 0; i < entityVertexList.size(); ++i) {
            for (Vertex v : entityVertexList.get(i)) {
                vertexList.add(v);
                if (!structTagMap.containsKey(v)) {
                    structTagMap.put(v, new ArrayList<>());
                }
                structTagMap.get(v).add(i);
            }
        }


        this.initMaxInvalidRange(vertexList);
        double searchUB = S;
        double searchLB = S / Math.sqrt(3);
        final double alpha = 1e-6;
        while (searchUB - searchLB > alpha) {
            double diameter = (searchLB + searchUB) / 2;
            boolean foundResult = false;
            for (int i = 0; i < vertexList.size(); ++i) {
                if (diameter < maxInvalidRange.get(vertexList.get(i))) continue;
                List<Vertex> localMatchVertex = circleScan(i, diameter);
                if (localMatchVertex != null) {
                    matchVertex = localMatchVertex;
                    S = getGroupDiameter(matchVertex);
                    foundResult = true;
                    searchUB = diameter;
                } else if (diameter > maxInvalidRange.get(vertexList.get(i))) {
                    maxInvalidRange.put(vertexList.get(i), diameter);
                }
            }
            if (!foundResult) {
                searchLB = diameter;
            }
        }

        exactSearch();

        return new MatchGroup(matchVertex, S);
    }

    private void exactSearch() {
        double upperBound = 2 / Math.sqrt(3) * S;
        for (int i = 0; i < vertexList.size(); ++i) {
            if (maxInvalidRange.get(vertexList.get(i)) < upperBound) {
                exactCircleScan(i, upperBound);
                if (S == 0) {
                    break;
                }
                upperBound = 2 / Math.sqrt(3) * S;
            }
        }
    }


    private void exactCircleScan(int idx, double upperbound) {
        final Vertex v = vertexList.get(idx);
        List<Vertex> selected = new ArrayList<>();
        int[] fuzzyGroup = new int[QUERY_NUM];
        int[] localFuzzyGroup = new int[QUERY_NUM];
        for (int tag : structTagMap.get(v)) {
            ++localFuzzyGroup[tag];
            ++fuzzyGroup[tag];
            selected.add(v);
        }
        if (checkFuzzyGroup(fuzzyGroup)) {
            matchVertex = new ArrayList<>(selected);
            S = 0;
            return;
        }
        List<Vertex> uList = getVertexWithSweepingArea(idx, upperbound);
        List<CircleScanItem> circleScanItemList = new ArrayList<>();
        Set<Vertex> coverVertex = new HashSet<>();
        for (Vertex u : uList) {
            Angle[] angles = CircleScanHelper.getInOutAngle(locationMap.get(v), locationMap.get(u), upperbound);
            Angle inAngle = angles[0];
            Angle outAngle = angles[1];
            circleScanItemList.add(new CircleScanItem(outAngle, u));
            if (outAngle.angleDegree < inAngle.angleDegree) {
                coverVertex.add(u);
                for (int tag : structTagMap.get(u)) {
                    ++fuzzyGroup[tag];
                }
            } else {
                circleScanItemList.add(new CircleScanItem(inAngle, u));
            }
        }
        circleScanItemList.sort(Comparator.comparingDouble(a -> a.angle.angleDegree));
        for (CircleScanItem circleScanItem : circleScanItemList) {
            if (AngleType.IN.equals(circleScanItem.angle.angleType)) {
                coverVertex.add(circleScanItem.vertex);
                for (int tag : structTagMap.get(circleScanItem.vertex)) {
                    ++fuzzyGroup[tag];
                }
            } else {
                if (checkFuzzyGroup(fuzzyGroup)) {
                    exhaustiveSearch(selected, coverVertex, localFuzzyGroup);
                    if (S == 0) {
                        break;
                    }
                }
                coverVertex.remove(circleScanItem.vertex);
                for (int tag : structTagMap.get(circleScanItem.vertex)) {
                    --fuzzyGroup[tag];
                }
            }
        }
    }


    private void exhaustiveSearch(List<Vertex> selected, Set<Vertex> candidates, int[] fuzzyGroup) {
        if (selected.size() == QUERY_NUM) {
            matchVertex = new ArrayList<>(selected);
            S = getGroupDiameter(matchVertex);
            return;
        }

        Set<Vertex> nextCandidates = new HashSet<>();
        for (Vertex u : candidates) {
            if (checkSingleDis(selected, u, S)) {
                nextCandidates.add(u);
            }
        }
        for (Vertex u : nextCandidates) {
            for (int tag : structTagMap.get(u)) {
                if (fuzzyGroup[tag] == 0 && checkSingleDis(selected, u, S)) {
                    ++fuzzyGroup[tag];
                    selected.add(u);
                    exhaustiveSearch(selected, nextCandidates, fuzzyGroup);
                    --fuzzyGroup[tag];
                    selected.remove(selected.size() - 1);
                }
            }
        }
    }

    private List<Vertex> circleScan(int idx, double upperbound) {
        final Vertex v = vertexList.get(idx);
        int[] fuzzyGroup = new int[QUERY_NUM];
        List<Vertex> coverVertex = new ArrayList<>();
        for (int tag : structTagMap.get(v)) {
            ++fuzzyGroup[tag];
            coverVertex.add(v);
        }
        if (checkFuzzyGroup(fuzzyGroup)) {
            matchVertex = new ArrayList<>(coverVertex);
            S = 0;
            return coverVertex;
        }
        List<Vertex> uList = getVertexWithSweepingArea(idx, upperbound);
        List<CircleScanItem> circleScanItemList = new ArrayList<>();
        for (Vertex u : uList) {
            Angle[] angles = CircleScanHelper.getInOutAngle(locationMap.get(v), locationMap.get(u), upperbound);
            Angle inAngle = angles[0];
            Angle outAngle = angles[1];
            circleScanItemList.add(new CircleScanItem(outAngle, u));
            if (outAngle.angleDegree < inAngle.angleDegree) {
                coverVertex.add(u);
                for (int tag : structTagMap.get(u)) {
                    ++fuzzyGroup[tag];
                }
            } else {
                circleScanItemList.add(new CircleScanItem(inAngle, u));
            }
        }
        circleScanItemList.sort(Comparator.comparingDouble(a -> a.angle.angleDegree));
        for (CircleScanItem circleScanItem : circleScanItemList) {
            if (AngleType.IN.equals(circleScanItem.angle.angleType)) {
                coverVertex.add(circleScanItem.vertex);
                for (int tag : structTagMap.get(circleScanItem.vertex)) {
                    ++fuzzyGroup[tag];
                }
            } else {
                if (checkFuzzyGroup(fuzzyGroup)) {
                    return coverVertex;
                }
                for (int tag : structTagMap.get(circleScanItem.vertex)) {
                    --fuzzyGroup[tag];
                }
                coverVertex.remove(circleScanItem.vertex);
            }

        }
        return null;
    }

    private List<Vertex> getVertexWithSweepingArea(int idx, double diameter) {
        List<Vertex> res = new ArrayList<>();
        Vertex v = vertexList.get(idx);
        BitSet bitSet = new BitSet(QUERY_NUM);
        for (int tag : structTagMap.get(v)) {
            bitSet.set(tag);
        }

        for (int i = 0; i < vertexList.size(); ++i) {
            if (i == idx) continue;
            Vertex u = vertexList.get(i);
            if (calculateDistance(v, u) <= diameter) {
                for (int tag : structTagMap.get(u)) {
                    bitSet.set(tag);
                }
                res.add(u);
            }
        }

        return bitSet.cardinality() == QUERY_NUM ? res : Collections.emptyList();
    }

    private boolean checkFuzzyGroup(int[] fuzzyGroup) {
        for (int num : fuzzyGroup) {
            if (num == 0) return false;
        }
        return true;
    }

    private void initMaxInvalidRange(List<Vertex> entityVertexList) {
        maxInvalidRange = new HashMap<>();
        for (Vertex entityVertex : entityVertexList) {
            maxInvalidRange.put(entityVertex, 0d);
        }
    }

    private double getGroupDiameter(List<Vertex> localMatchVertex) {
        boolean allTheSame = true;
        double cur = Double.MIN_VALUE;
        Vertex lastVertex = null;
        for (int i = 0; i < localMatchVertex.size(); ++i) {
            Vertex v1 = localMatchVertex.get(i);
            for (int j = i + 1; j < localMatchVertex.size(); ++j) {
                Vertex v2 = localMatchVertex.get(j);
                double d = calculateDistance(v1, v2);
                cur = Math.max(cur, d);
            }
            if (i > 0 && allTheSame && !v1.equals(lastVertex)) {
                allTheSame = false;
            }
            lastVertex = v1;
        }
        return allTheSame ? 0 : cur;
    }

    private boolean checkSingleDis(List<Vertex> selected, Vertex v, double curD) {
        for (Vertex u : selected) {
            if (calculateDistance(v, u) > curD) return false;
        }
        return true;
    }

    private double calculateDistance(Vertex v1, Vertex v2) {
        return SpatialUtil.calculateDistance(locationMap.get(v1), locationMap.get(v2));
    }
}
