package edu.ecnu.aidadblab.algorithm.mclosest.entity.matching.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.algorithm.mclosest.entity.matching.MClosestEntityMatchingAlgorithm;
import edu.ecnu.aidadblab.constant.AngleType;
import edu.ecnu.aidadblab.data.model.*;
import edu.ecnu.aidadblab.index.arcforest.ArcForest;
import edu.ecnu.aidadblab.index.arctree.ArcTree;
import edu.ecnu.aidadblab.index.arctree.ArcTreeLeafNode;
import edu.ecnu.aidadblab.processor.FuzzyMatchProcessor;
import edu.ecnu.aidadblab.tool.CircleScanHelper;
import edu.ecnu.aidadblab.util.SpatialUtil;

import java.util.*;

public class FCircleScanAlgorithm implements MClosestEntityMatchingAlgorithm {

    private double S;

    private double feasibleS;

    private List<Vertex> vertexList;

    private Map<Vertex, JSONObject> locationMap;

    private Map<Vertex, List<Integer>> structTagMap;

    private int QUERY_NUM;

    private FuzzyMatchProcessor fuzzyMatchProcessor;

    private ArcForest arcForest;

    @Override
    public MatchGroup query(Graph dataGraph, List<Graph> queryGraphs) {
        QUERY_NUM = queryGraphs.size();
        fuzzyMatchProcessor = new FuzzyMatchProcessor(dataGraph, queryGraphs);
        MatchGroup feasibleGroup = fuzzyMatchProcessor.findFeasibleSolution();
        if (feasibleGroup.diameter == 0) {
            return feasibleGroup;
        }

        FuzzyMatchIntermediate fuzzyMatchIntermediate = fuzzyMatchProcessor.getFuzzyMatchIntermediate();
        S = feasibleGroup.diameter;
        locationMap = fuzzyMatchIntermediate.locationMap;
        vertexList = fuzzyMatchIntermediate.vertexList;
        structTagMap = fuzzyMatchIntermediate.structTagMap;
        feasibleS = 2 / Math.sqrt(3) * feasibleGroup.diameter;
        this.arcForest = new ArcForest(vertexList.size());

        for (int i = 0; i < vertexList.size(); ++i) {
            MatchGroup matchGroup = exactCircleScan(vertexList.get(i), feasibleS, i);
            if (matchGroup != null) {
                return matchGroup;
            }
        }

        return findBestExactMatch();
    }

    private MatchGroup findBestExactMatch() {
        while (arcForest.isRemainCandidate()) {
            ArcTree arcTree = arcForest.peek();
            if (!arcTree.isRemainCandidates()) {
                arcForest.pop();
                continue;
            }
            ArcTreeLeafNode bestGroupArcTreeLeafNode = arcTree.getBestGroupArcTreeLeafNode();
            MatchGroup bestGroup = bestGroupArcTreeLeafNode.getMatchGroup();
            boolean validate = true;

            for (int i = 0; i < QUERY_NUM; ++i) {
                if (!fuzzyMatchProcessor.checkExact(bestGroup.entityMap.get(i), i)) {
                    updateWhenMatchFailed(bestGroup.entityMap.get(i), i, arcTree, bestGroupArcTreeLeafNode);
                    validate = false;
                    break;
                }
            }

            if (validate) {
                return bestGroup;
            }
        }

        throw new IllegalStateException("There is no validate answer");
    }


    private void updateWhenMatchFailed(Vertex notMatchVertex, int notMatchTag, ArcTree arcTree, ArcTreeLeafNode bestGroupArcTreeLeafNode) {
        if (bestGroupArcTreeLeafNode.getTab()[notMatchTag] == 1) {
            bestGroupArcTreeLeafNode.delete();
        } else {
            --bestGroupArcTreeLeafNode.getTab()[notMatchTag];
            structTagMap.get(notMatchVertex).remove(Integer.valueOf(notMatchTag));
            List<Vertex> selected = new ArrayList<>(QUERY_NUM);
            Map<Integer, Vertex> entityMap = new HashMap<>(QUERY_NUM);
            for (int tag : structTagMap.get(arcTree.centerVertex)) {
                selected.add(arcTree.centerVertex);
                entityMap.put(tag, arcTree.centerVertex);
            }
            MatchGroup newMatchGroup = doExhaustiveSearch(selected, bestGroupArcTreeLeafNode.getCoverVertex(), entityMap);
            if (CollUtil.isEmpty(newMatchGroup.matchVertex)) {
                bestGroupArcTreeLeafNode.delete();
            } else {
                bestGroupArcTreeLeafNode.updateMatchGroup(newMatchGroup);
            }
        }
        arcForest.update(arcTree);
    }

    private MatchGroup exactCircleScan(Vertex v, double upperbound, int index) {
        ArcTree arcTree = new ArcTree(v);
        List<Vertex> selected = new ArrayList<>();
        Map<Integer, Vertex> entityMap = new HashMap<>(QUERY_NUM);
        int[] fuzzyGroup = new int[QUERY_NUM];
        for (Iterator<Integer> it = structTagMap.get(v).iterator(); it.hasNext(); ) {
            int tag = it.next();
            if (fuzzyMatchProcessor.checkExact(v, tag)) {
                entityMap.put(tag, v);
                ++fuzzyGroup[tag];
                selected.add(v);
            } else {
                it.remove();
            }
        }
        if (entityMap.keySet().isEmpty()) {
            return null;
        }
        if (checkFuzzyGroup(fuzzyGroup)) {
            return new MatchGroup(selected, 0);
        }
        List<Vertex> uList = getVertexWithSweepingArea(v, upperbound, index);
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

                    MatchGroup matchGroup = doExhaustiveSearch(selected, coverVertex, entityMap);
                    if (CollUtil.isNotEmpty(matchGroup.matchVertex)) {
                        if (matchGroup.diameter == 0) {
                            return matchGroup;
                        }
                        arcTree.addLeafNode(new ArcTreeLeafNode(circleScanItem, matchGroup, new HashSet<>(coverVertex), Arrays.copyOf(fuzzyGroup, fuzzyGroup.length)));
                    }
                }
                coverVertex.remove(circleScanItem.vertex);
                for (int tag : structTagMap.get(circleScanItem.vertex)) {
                    --fuzzyGroup[tag];
                }
            }
        }

        arcTree.constructArcTree();
        if (arcTree.isRemainCandidates()) {
            arcForest.add(arcTree);
        }
        return null;
    }

    private MatchGroup doExhaustiveSearch(List<Vertex> selected, Set<Vertex> coverVertex, Map<Integer, Vertex> entityMap) {
        S = feasibleS;
        MatchGroup matchGroup = new MatchGroup();
        exhaustiveSearch(selected, new HashSet<>(coverVertex), matchGroup, entityMap);
        return matchGroup;
    }

    private void exhaustiveSearch(List<Vertex> selected, Set<Vertex> candidates, MatchGroup matchGroup, Map<Integer, Vertex> entityMap) {
        if (entityMap.size() == QUERY_NUM) {
            matchGroup.matchVertex = new ArrayList<>(selected);
            matchGroup.entityMap = new HashMap<>(entityMap);
            matchGroup.diameter = getGroupDiameter(selected);
            S = matchGroup.diameter;
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
                if (!entityMap.containsKey(tag) && checkSingleDis(selected, u, S)) {
                    selected.add(u);
                    entityMap.put(tag, u);
                    exhaustiveSearch(selected, nextCandidates, matchGroup, entityMap);
                    entityMap.remove(tag);
                    selected.remove(selected.size() - 1);
                }
            }
        }
    }

    private List<Vertex> getVertexWithSweepingArea(Vertex v, double diameter, int index) {
        List<Vertex> res = new ArrayList<>();
        BitSet bitSet = new BitSet(QUERY_NUM);
        for (int tag : structTagMap.get(v)) {
            bitSet.set(tag);
        }

        for (int i = 0; i < vertexList.size(); ++i) {
            if (i == index) continue;
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
