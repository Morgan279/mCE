package edu.ecnu.aidadblab.processor;

import cn.hutool.core.lang.Console;
import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.algorithm.subgraph.matching.SubgraphMatchingAlgorithm;
import edu.ecnu.aidadblab.config.GlobalConfig;
import edu.ecnu.aidadblab.constant.LabelConst;
import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.Match;
import edu.ecnu.aidadblab.data.model.MatchGroup;
import edu.ecnu.aidadblab.data.model.Vertex;
import edu.ecnu.aidadblab.tool.GlobalData;
import edu.ecnu.aidadblab.util.SpatialUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExactMatchProcessor {

    private int QUERY_NUM;

    private Graph dataGraph;

    private List<Graph> queryGraphs;

    private final SubgraphMatchingAlgorithm subgraphMatchingAlgorithm;

    public List<Set<Vertex>> candidateDataEntityVertexes;

    private Map<Vertex, JSONObject> locationMap;


    public ExactMatchProcessor(Graph dataGraph, List<Graph> queryGraphs, SubgraphMatchingAlgorithm subgraphMatchingAlgorithm) {
        this.dataGraph = dataGraph;
        this.queryGraphs = queryGraphs;
        this.subgraphMatchingAlgorithm = subgraphMatchingAlgorithm;
        this.init();
    }

    private void init() {
        this.locationMap = dataGraph.locationMap;
        this.QUERY_NUM = queryGraphs.size();
        this.candidateDataEntityVertexes = getExactEntityVertexList();
    }

    private List<Set<Vertex>> getExactEntityVertexList() {
        List<Set<Match>> matchResults = this.getExactMatchResult();
        List<Set<Vertex>> res = new ArrayList<>(matchResults.size());
        for (Set<Match> matches : matchResults) {
            res.add(matches.stream().map(this::findEntityVertex).collect(Collectors.toSet()));
        }
        return res;
    }

    private List<Set<Match>> getExactMatchResult() {
        long startTime = System.nanoTime();
        List<Set<Match>> matchResults = new ArrayList<>(queryGraphs.size());
        for (Graph queryGraph : queryGraphs) {
            matchResults.add(subgraphMatchingAlgorithm.match(dataGraph, queryGraph));
        }
        GlobalData.ExactNonSpatialTime = (System.nanoTime() - startTime) / 1e6;
        return matchResults;
    }

    public Map<Vertex, JSONObject> getLocationMap(List<Set<Vertex>> entityVertexList) {
        return dataGraph.locationMap;
    }

    public MatchGroup findFeasibleSolution() {
        long startTime = System.currentTimeMillis();
        double curMinRadius = -1;
        List<Vertex> solutionVertexes = new ArrayList<>(QUERY_NUM);
        Vertex vStar = candidateDataEntityVertexes.get(0).iterator().next();
        solutionVertexes.add(vStar);
        for (int i = 1; i < QUERY_NUM; ++i) {
            Vertex u = findNearestEntityVertex(vStar, i);
            solutionVertexes.add(u);
            for (int j = 0; j < i; ++j) {
                Vertex v = solutionVertexes.get(j);
                double curDistance = calculateDistance(u, v);
                curMinRadius = Math.max(curMinRadius, curDistance);
            }
        }

        if (GlobalConfig.DEBUG) {
            Console.log("first feasible solution cost {} ms d: {}", System.currentTimeMillis() - startTime, curMinRadius);
        }

        return new MatchGroup(solutionVertexes, curMinRadius);
    }


    private Vertex findNearestEntityVertex(Vertex v, int index) {
        double minDistance = Double.MAX_VALUE;
        Vertex res = null;
        for (Vertex u : candidateDataEntityVertexes.get(index)) {
            double curDistance = calculateDistance(u, v);
            if (curDistance < minDistance) {
                minDistance = curDistance;
                res = u;
            }
        }
        if (res == null) {
            throw new IllegalArgumentException("there is no validate vertex");
        }
        return res;
    }

    private Vertex findEntityVertex(Match match) {
        for (Vertex u : match.getQueryVertexSet()) {
            if (LabelConst.ENTITY_LABEL.equals(u.label)) {
                return match.get(u);
            }
        }
        throw new IllegalArgumentException("no entity vertex found");
    }

    public double getGroupDiameter(List<Vertex> localMatchVertex) {
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

    public double calculateDistance(Vertex u, Vertex v) {
        return SpatialUtil.calculateDistance(locationMap.get(u), locationMap.get(v));
    }

}
