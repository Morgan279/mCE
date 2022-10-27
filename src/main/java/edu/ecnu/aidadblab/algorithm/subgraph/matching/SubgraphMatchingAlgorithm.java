package edu.ecnu.aidadblab.algorithm.subgraph.matching;

import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.Match;

import java.util.Set;

public interface SubgraphMatchingAlgorithm {

    Set<Match> match(Graph dataGraph, Graph queryGraph);

}
