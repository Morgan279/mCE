package edu.ecnu.aidadblab.algorithm.mclosest.entity.matching.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.algorithm.mclosest.entity.matching.MClosestEntityMatchingAlgorithm;
import edu.ecnu.aidadblab.algorithm.subgraph.matching.imlp.VC;
import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.MatchGroup;
import edu.ecnu.aidadblab.data.model.Vertex;
import edu.ecnu.aidadblab.processor.ExactMatchProcessor;
import edu.ecnu.aidadblab.util.SpatialUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExactEnumAlgorithm implements MClosestEntityMatchingAlgorithm {

    private Map<Vertex, JSONObject> locationMap;

    private double diameter;

    private List<Vertex> matchVertex;

    private int QUERY_NUM;

    @Override
    public MatchGroup query(Graph dataGraph, List<Graph> queryGraphs) {
        QUERY_NUM = queryGraphs.size();
        this.locationMap = dataGraph.locationMap;
        this.diameter = Double.MAX_VALUE;
        ExactMatchProcessor exactMatchProcessor = new ExactMatchProcessor(dataGraph, queryGraphs, new VC());
        List<Set<Vertex>> entityVertexList = exactMatchProcessor.candidateDataEntityVertexes;
        for (Vertex v : entityVertexList.get(0)) {
            vit(CollUtil.newArrayList(v), entityVertexList, 1);
        }
        return new MatchGroup(matchVertex, diameter);
    }

    private void vit(List<Vertex> selected, List<Set<Vertex>> entityVertexList, int level) {
        if (level == QUERY_NUM) {
            matchVertex = new ArrayList<>(selected);
            diameter = getGroupDiameter(matchVertex);
            return;
        }

        for (Vertex v : entityVertexList.get(level)) {
            if (!checkSingleDis(selected, v, diameter)) {
                continue;
            }
            selected.add(v);
            vit(selected, entityVertexList, level + 1);
            selected.remove(v);
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
