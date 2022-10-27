package edu.ecnu.aidadblab.algorithm.mclosest.entity.matching.impl;

import edu.ecnu.aidadblab.algorithm.mclosest.entity.matching.MClosestEntityMatchingAlgorithm;
import edu.ecnu.aidadblab.config.GlobalConfig;
import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.MatchGroup;

import java.util.List;

public class F2EScanAlgorithm implements MClosestEntityMatchingAlgorithm {

    private final FCircleScanAlgorithm fCircleScanAlgorithm = new FCircleScanAlgorithm();

    @Override
    public MatchGroup query(Graph dataGraph, List<Graph> queryGraphs) {
        GlobalConfig.ENABLE_INDEX = false;
        return fCircleScanAlgorithm.query(dataGraph, queryGraphs);
    }
}
