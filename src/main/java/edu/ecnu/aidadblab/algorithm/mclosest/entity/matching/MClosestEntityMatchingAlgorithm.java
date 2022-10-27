package edu.ecnu.aidadblab.algorithm.mclosest.entity.matching;

import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.MatchGroup;

import java.util.List;

public interface MClosestEntityMatchingAlgorithm {

    MatchGroup query(Graph dataGraph, List<Graph> queryGraphs);

}
