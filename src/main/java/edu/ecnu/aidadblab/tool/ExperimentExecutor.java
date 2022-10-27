package edu.ecnu.aidadblab.tool;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.algorithm.mclosest.entity.matching.MClosestEntityMatchingAlgorithm;
import edu.ecnu.aidadblab.algorithm.mclosest.entity.matching.impl.*;
import edu.ecnu.aidadblab.algorithm.subgraph.matching.SubgraphMatchingAlgorithm;
import edu.ecnu.aidadblab.algorithm.subgraph.matching.imlp.VC;
import edu.ecnu.aidadblab.config.GlobalConfig;
import edu.ecnu.aidadblab.constant.Dataset;
import edu.ecnu.aidadblab.constant.FuzzyLevel;
import edu.ecnu.aidadblab.constant.IndexType;
import edu.ecnu.aidadblab.constant.LabelConst;
import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.MatchGroup;
import edu.ecnu.aidadblab.data.model.Vertex;
import edu.ecnu.aidadblab.importer.YelpImporter;
import edu.ecnu.aidadblab.index.arcforest.ArcForest;
import edu.ecnu.aidadblab.index.arctree.ArcTree;
import edu.ecnu.aidadblab.index.arctree.ArcTreeLeafNode;
import edu.ecnu.aidadblab.processor.ExactMatchProcessor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openjdk.jol.info.GraphLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
public class ExperimentExecutor {

    private Graph dataGraph;

    private List<Graph> queryGraphs = new ArrayList<>();

    private static final int EXPERIMENT_TIMES = 20;

    private final double[] dataGraphSizes = {1e3, 1e4, 1e5, 1e6};

    double currentDataGraphSize = dataGraphSizes[0];

    private final MClosestEntityMatchingAlgorithm timeRatioStatisticsAlgorithm = new ExactTimeRatioTestAlgorithm();

    private final MClosestEntityMatchingAlgorithm FETimeRatioStatisticsAlgorithm = new FETimeRatioTestAlgorithm();

    private final MClosestEntityMatchingAlgorithm indexTestAlgorithm = new IF2EScanAlgorithm();

    private final SubgraphMatchingAlgorithm vc = new VC();

    private List<MClosestEntityMatchingAlgorithm> testAlgorithms;


    public void indexTest() {
        GlobalConfig.ENABLE_INDEX = true;
        GraphGenerator.queryGraphCount = 4;
        GraphGenerator.queryGraphSize = 8;
        this.switchYelpDataSetWithSpecificReadLine();
        indexTestAlgorithm.query(dataGraph, GraphGenerator.generateRandomQueryGraph(dataGraph));
        for (double graphSize : dataGraphSizes) {
            currentDataGraphSize = graphSize;
            indexEfficientTest();
        }

        log.info("index efficient test over");

        for (double graphSize : dataGraphSizes) {
            currentDataGraphSize = graphSize;
            indexSpaceTest();
        }

    }


    public void efficientTestVariedM() {
        //for building index
        GlobalConfig.ENABLE_INDEX = true;
        GlobalConfig.MAX_READ_LINE = (int) 1e6;
        ExperimentDataset.init();

        testAlgorithms = new ArrayList<>();
        testAlgorithms.add(new ExactEnumAlgorithm());
        testAlgorithms.add(new ExactScanAlgorithm());
        testAlgorithms.add(new F2EScanAlgorithm());
        testAlgorithms.add(new IF2EScanAlgorithm());
        int[] variedSize = {8, 16};

        for (int queryGraphSize : variedSize) {
            GraphGenerator.queryGraphSize = queryGraphSize;

            this.switchYelpDataSet();
            this.conductEfficientTestVariedM();

            log.info("switching FS dataset");
            this.switchFoursquareDataSet();
            this.conductEfficientTestVariedM();

            log.info("switching WD dataset");
            this.switchWikiDataSet();
            this.conductEfficientTestVariedM();

            log.info("switching GW dataset");
            this.switchGowallaDataSet();
            this.conductEfficientTestVariedM();

            log.info("switching BK dataset");
            this.switchBrightkiteDataSet();
            this.conductEfficientTestVariedM();

            log.info("query graph size {} is going to switch", queryGraphSize);
        }

    }

    public void efficientTestVariedQueryGraphCount() {
        //for building index
        GlobalConfig.ENABLE_INDEX = true;
        GlobalConfig.MAX_READ_LINE = (int) 1e6;
        ExperimentDataset.init();

        testAlgorithms = new ArrayList<>();
        testAlgorithms.add(new ExactEnumAlgorithm());
        testAlgorithms.add(new ExactScanAlgorithm());
        testAlgorithms.add(new F2EScanAlgorithm());
        testAlgorithms.add(new IF2EScanAlgorithm());

        int[] variedQueryGraphCount = {3, 6};

        for (int queryGraphCount : variedQueryGraphCount) {
            GraphGenerator.queryGraphCount = queryGraphCount;

            this.switchYelpDataSet();
            this.conductEfficientTest();

            log.info("switching FS dataset");
            this.switchFoursquareDataSet();
            this.conductEfficientTest();

            log.info("switching WD dataset");
            this.switchWikiDataSet();
            this.conductEfficientTest();

            log.info("switching GW dataset");
            this.switchGowallaDataSet();
            this.conductEfficientTest();

            log.info("switching BK dataset");
            this.switchBrightkiteDataSet();
            this.conductEfficientTest();

            log.info("query graph count {} is going to switch", queryGraphCount);
        }

    }

    public void efficientTestWithDataSizeVaried() {
        //for building index
        GlobalConfig.ENABLE_INDEX = true;


        testAlgorithms = new ArrayList<>(4);
        testAlgorithms.add(new ExactEnumAlgorithm());
        testAlgorithms.add(new ExactScanAlgorithm());
        testAlgorithms.add(new F2EScanAlgorithm());
        testAlgorithms.add(new IF2EScanAlgorithm());

        final int[] queryGraphSizes = {8, 16};
        final int[] queryGraphCounts = {3, 6};

        for (int i = 0; i < 2; ++i) {
            GraphGenerator.queryGraphSize = queryGraphSizes[i];
            GraphGenerator.queryGraphCount = queryGraphCounts[i];

            this.conductEfficientWithDataSizeVariedTest(Dataset.YELP);

            log.info("switching FS dataset");
            this.conductEfficientWithDataSizeVariedTest(Dataset.FOURSQUARE);

            log.info("switching WD dataset");
            this.conductEfficientWithDataSizeVariedTest(Dataset.WIKIDATA);

            log.info("switching GW dataset");
            this.conductEfficientWithDataSizeVariedTest(Dataset.GOWALLA);

            log.info("switching BK dataset");
            this.conductEfficientWithDataSizeVariedTest(Dataset.BRIGHTKITE);
        }

    }

    public void timeRatioStatisticsWithQueryNodesVaried() {
        GlobalConfig.TIME_RATIO_TEST = true;
        GlobalConfig.MAX_READ_LINE = (int) 1e6;
        GraphGenerator.queryGraphCount = 6;
        ExperimentDataset.init();

        final int[] queryGraphSizes = {4, 6, 8, 10, 12, 14, 16};
        for (int queryGraphSize : queryGraphSizes) {
            GraphGenerator.queryGraphSize = queryGraphSize;

            this.switchYelpDataSet();
            this.conductTimeRatioStatisticsWithQueryNodesVaried();

            log.info("switching FS dataset");
            this.switchFoursquareDataSet();
            this.conductTimeRatioStatisticsWithQueryNodesVaried();

            log.info("switching WD dataset");
            this.switchWikiDataSet();
            this.conductTimeRatioStatisticsWithQueryNodesVaried();

            log.info("switching GW dataset");
            this.switchGowallaDataSet();
            this.conductTimeRatioStatisticsWithQueryNodesVaried();

            log.info("switching BK dataset");
            this.switchBrightkiteDataSet();
            this.conductTimeRatioStatisticsWithQueryNodesVaried();
        }

    }

    public void timeRatioStatisticsWithDataGraphSizeVaried() {
        GlobalConfig.TIME_RATIO_TEST = true;
        GraphGenerator.queryGraphCount = 6;
        GraphGenerator.queryGraphSize = 12;

        for (double dataGraphSize : dataGraphSizes) {
            GlobalConfig.MAX_READ_LINE = (int) dataGraphSize;
            ExperimentDataset.init();

            this.switchYelpDataSet();
            this.conductTimeRatioStatisticsWithDataGraphSizeVaried();

            log.info("switching FS dataset");
            this.switchFoursquareDataSet();
            this.conductTimeRatioStatisticsWithDataGraphSizeVaried();

            log.info("switching WD dataset");
            this.switchWikiDataSet();
            this.conductTimeRatioStatisticsWithDataGraphSizeVaried();

            log.info("switching GW dataset");
            this.switchGowallaDataSet();
            this.conductTimeRatioStatisticsWithDataGraphSizeVaried();

            log.info("switching BK dataset");
            this.switchBrightkiteDataSet();
            this.conductTimeRatioStatisticsWithDataGraphSizeVaried();
        }

    }

    private void conductTimeRatioStatisticsWithQueryNodesVaried() {
        double ExactTotalTimeRatio = 0;
        double FETotalTimeRatio = 0;
        final int queryGraphCountPerQuery = GraphGenerator.queryGraphCount;
        for (int i = 0; i < EXPERIMENT_TIMES; ++i) {
            this.queryGraphs = GraphGenerator.generateRandomQueryGraphWithSkip(dataGraph, i * queryGraphCountPerQuery);
            timeRatioStatisticsAlgorithm.query(dataGraph, queryGraphs);
            double radio = GlobalData.ExactTimeRatio;
            ExactTotalTimeRatio += radio;
            Console.log("{} round exact ratio: {}", i + 1, radio);
            FETimeRatioStatisticsAlgorithm.query(dataGraph, queryGraphs);
            radio = GlobalData.FEtimeRatio;
            FETotalTimeRatio += radio;
            Console.log("{} round fuzzy-exact ratio: {}", i + 1, radio);
        }
        log.info("{} {}", GraphGenerator.queryGraphSize, ExactTotalTimeRatio / (double) EXPERIMENT_TIMES);
        log.info("{} {}", GraphGenerator.queryGraphSize, FETotalTimeRatio / (double) EXPERIMENT_TIMES);
    }

    private void conductTimeRatioStatisticsWithDataGraphSizeVaried() {
        double ExactTotalTimeRatio = 0;
        double FETotalTimeRatio = 0;
        final int queryGraphCountPerQuery = GraphGenerator.queryGraphCount;
        for (int i = 0; i < EXPERIMENT_TIMES; ++i) {
            this.queryGraphs = GraphGenerator.generateRandomQueryGraphWithSkip(dataGraph, i * queryGraphCountPerQuery);
            timeRatioStatisticsAlgorithm.query(dataGraph, queryGraphs);
            double radio = GlobalData.ExactTimeRatio;
            ExactTotalTimeRatio += GlobalData.ExactTimeRatio;
            Console.log("{} round ratio: {}", i + 1, radio);
            FETimeRatioStatisticsAlgorithm.query(dataGraph, queryGraphs);
            radio = GlobalData.FEtimeRatio;
            FETotalTimeRatio += radio;
            Console.log("{} round fuzzy-exact ratio: {}", i + 1, radio);
        }
        log.info("$10^{}$ {}", (String.valueOf(GlobalConfig.MAX_READ_LINE)).length() - 1, ExactTotalTimeRatio / (double) EXPERIMENT_TIMES);
        log.info("$10^{}$ {}", (String.valueOf(GlobalConfig.MAX_READ_LINE)).length() - 1, FETotalTimeRatio / (double) EXPERIMENT_TIMES);
    }


    private void conductEfficientTestVariedM() {
        boolean isWarmUp = true;
        int[] variedCount = {2, 3, 4, 5, 6, 7};
        for (int queryGraphCount : variedCount) {
            GraphGenerator.queryGraphCount = queryGraphCount;
            this.queryGraphs = GraphGenerator.generateRandomQueryGraph(dataGraph);
            for (MClosestEntityMatchingAlgorithm algorithm : testAlgorithms) {
                if (isWarmUp) {
                    List<Graph> currentRoundQueryGraphs = GraphGenerator.generateRandomQueryGraph(dataGraph);
                    long startTime = System.currentTimeMillis();
                    algorithm.query(dataGraph, currentRoundQueryGraphs);
                    long cost = System.currentTimeMillis() - startTime;
                    Console.log("{} warm up, cost: {}", algorithm.getClass().getSimpleName(), cost);
                } else {
                    long totalCost = 0;
                    for (int i = 0; i < EXPERIMENT_TIMES; ++i) {
                        List<Graph> currentRoundQueryGraphs = GraphGenerator.generateRandomQueryGraphWithSkip(dataGraph, i * queryGraphCount);
                        long startTime = System.currentTimeMillis();
                        algorithm.query(dataGraph, currentRoundQueryGraphs);
                        long cost = System.currentTimeMillis() - startTime;
                        Console.log("{} {} round cost: {}", algorithm.getClass().getSimpleName(), i + 1, cost);
                        totalCost += cost;
                    }
                    log.info("{}:{}", algorithm.getClass().getSimpleName(), Math.round(totalCost / (double) EXPERIMENT_TIMES));
                }
            }
            isWarmUp = false;
        }
    }

    private void conductEfficientTest() {
        boolean isWarmUp = true;
        final int queryGraphCountPerQuery = GraphGenerator.queryGraphCount;
        int[] variedQueryGraphSizes = {6, 8, 10, 12, 14, 16};
        for (int queryGraphSize : variedQueryGraphSizes) {
            GraphGenerator.queryGraphSize = queryGraphSize;
            for (MClosestEntityMatchingAlgorithm algorithm : testAlgorithms) {
                if (isWarmUp) {
                    List<Graph> currentRoundQueryGraphs = GraphGenerator.generateRandomQueryGraph(dataGraph);
                    long startTime = System.currentTimeMillis();
                    algorithm.query(dataGraph, currentRoundQueryGraphs);
                    long cost = System.currentTimeMillis() - startTime;
                    Console.log("{} warm up, cost: {}", algorithm.getClass().getSimpleName(), cost);
                } else {
                    long totalCost = 0;
                    for (int i = 0; i < EXPERIMENT_TIMES; ++i) {
                        List<Graph> currentRoundQueryGraphs = GraphGenerator.generateRandomQueryGraphWithSkip(dataGraph, i * queryGraphCountPerQuery);
                        long startTime = System.currentTimeMillis();
                        algorithm.query(dataGraph, currentRoundQueryGraphs);
                        long cost = System.currentTimeMillis() - startTime;
                        Console.log("{} {} round cost: {}", algorithm.getClass().getSimpleName(), i + 1, cost);
                        totalCost += cost;
                    }
                    log.info("{}:{}", algorithm.getClass().getSimpleName(), Math.round(totalCost / (double) EXPERIMENT_TIMES));
                }
            }
            isWarmUp = false;
        }
    }

    private void conductEfficientWithDataSizeVariedTest(String datasetName) {
        boolean isWarmUp = true;
        final int queryGraphCountPerQuery = GraphGenerator.queryGraphCount;
        for (double dataGraphSize : dataGraphSizes) {
            GlobalConfig.MAX_READ_LINE = (int) dataGraphSize;
            ExperimentDataset.init();
            this.dataGraph = ExperimentDataset.switchDataset(datasetName);
            for (MClosestEntityMatchingAlgorithm algorithm : testAlgorithms) {
                if (isWarmUp) {
                    List<Graph> currentRoundQueryGraphs = GraphGenerator.generateRandomQueryGraph(dataGraph);
                    long startTime = System.currentTimeMillis();
                    algorithm.query(dataGraph, currentRoundQueryGraphs);
                    long cost = System.currentTimeMillis() - startTime;
                    Console.log("{} warm up, cost: {}", algorithm.getClass().getSimpleName(), cost);
                } else {
                    long totalCost = 0;
                    for (int i = 0; i < EXPERIMENT_TIMES; ++i) {
                        List<Graph> currentRoundQueryGraphs = GraphGenerator.generateRandomQueryGraphWithSkip(dataGraph, i * queryGraphCountPerQuery);
                        long startTime = System.currentTimeMillis();
                        algorithm.query(dataGraph, currentRoundQueryGraphs);
                        long cost = System.currentTimeMillis() - startTime;
                        Console.log("{} {} round cost: {}", algorithm.getClass().getSimpleName(), i + 1, cost);
                        totalCost += cost;
                    }
                    log.info("{}:{}", algorithm.getClass().getSimpleName(), Math.round(totalCost / (double) EXPERIMENT_TIMES));
                }
            }
            isWarmUp = false;
        }
    }


    public void effective() {
        GlobalConfig.MAX_READ_LINE = (int) 1e6;
        ExperimentDataset.init();
        GraphGenerator.queryGraphCount = 3;
        GraphGenerator.queryGraphSize = 3;

        this.switchYelpDataSet();
        queryGraphs.clear();
        queryGraphs.add(generateQueryGraph3());
        queryGraphs.add(generateQueryGraph1());
        queryGraphs.add(generateQueryGraph2());
        this.conductCaseStudy();

        log.info("switching FS dataset");
        this.switchFoursquareDataSet();
        queryGraphs = GraphGenerator.generateRandomQueryGraph(dataGraph);
        this.conductCaseStudy();

        log.info("switching WD dataset");
        this.switchWikiDataSet();
        queryGraphs = GraphGenerator.generateRandomQueryGraph(dataGraph);
        this.conductCaseStudy();

        log.info("switching GW dataset");
        this.switchGowallaDataSet();
        queryGraphs.clear();
        queryGraphs.add(generateQueryGraph3());
        queryGraphs.add(generateQueryGraph1());
        queryGraphs.add(generateQueryGraph2());
        this.conductCaseStudy();

        log.info("switching BK dataset");
        this.switchBrightkiteDataSet();
        queryGraphs.clear();
        queryGraphs.add(generateQueryGraph3());
        queryGraphs.add(generateQueryGraph1());
        queryGraphs.add(generateQueryGraph2());
        this.conductCaseStudy();
    }

    private void conductCaseStudy() {
        final int m = GraphGenerator.queryGraphCount;
        ExactMatchProcessor exactMatchProcessor = new ExactMatchProcessor(dataGraph, queryGraphs, vc);

        List<List<Vertex>> candidates = exactMatchProcessor.candidateDataEntityVertexes.stream().
                map(ArrayList::new).collect(Collectors.toList());
        List<Vertex> selectedVertexes = new ArrayList<>(m);
        double randomSelectTotalDiameter = 0;
        for (int i = 0; i < 100; ++i) {
            selectedVertexes.clear();
            for (int j = 0; j < m; ++j) {
                List<Vertex> curQueryVertexes = candidates.get(j);
                selectedVertexes.add(curQueryVertexes.get(ThreadLocalRandom.current().nextInt(curQueryVertexes.size())));
            }
            double diameter = exactMatchProcessor.getGroupDiameter(selectedVertexes);
            randomSelectTotalDiameter += diameter;
            List<JSONObject> locations = selectedVertexes.stream()
                    .map(item -> dataGraph.locationMap.get(item))
                    .collect(Collectors.toList());
            log.info("Random Select, Round {}: #diameter:{}#locations:{}", i + 1, diameter, locations);
        }
        log.info("Random Select Average Diameter: {}", randomSelectTotalDiameter / 100);
        MatchGroup matchGroup = timeRatioStatisticsAlgorithm.query(dataGraph, queryGraphs);
        List<JSONObject> matchLocations = matchGroup.matchVertex.stream()
                .map(item -> dataGraph.locationMap.get(item))
                .collect(Collectors.toList());
        log.info("Query Answer: #diameter:{}#locations:{}", matchGroup.diameter, matchLocations);
    }

    private Graph generateQueryGraph1() {
        Graph queryGraph = new Graph();
        Vertex bank = new Vertex(LabelConst.ENTITY_LABEL);
        Vertex location = new Vertex(new JSONObject().toJSONString(), LabelConst.LOCATION_LABEL);
        Vertex category = new Vertex("Banks & Credit Unions");
        Vertex city = new Vertex("Gilbert");
        Vertex state = new Vertex("AZ");
        Vertex isOpen = new Vertex("1");

        queryGraph.addVertex(bank);
        queryGraph.addVertex(location);
        queryGraph.addVertex(category);
        queryGraph.addVertex(isOpen);

        queryGraph.addEdge(bank, location);
        queryGraph.addEdge(bank, category);
        queryGraph.addEdge(bank, isOpen);

        return queryGraph;
    }

    private Graph generateQueryGraph2() {
        Graph queryGraph = new Graph();
        Vertex hotel = new Vertex(LabelConst.ENTITY_LABEL);
        Vertex location = new Vertex(new JSONObject().toJSONString(), LabelConst.LOCATION_LABEL);
        Vertex category = new Vertex("Hotels & Travel");
        Vertex stars = new Vertex("5.0");
        Vertex isOpen = new Vertex("1");

        queryGraph.addVertex(hotel);
        queryGraph.addVertex(location);
        queryGraph.addVertex(category);
        queryGraph.addVertex(category);
        queryGraph.addVertex(stars);
        queryGraph.addVertex(isOpen);

        queryGraph.addEdge(hotel, location);
        queryGraph.addEdge(hotel, category);
        queryGraph.addEdge(hotel, stars);
        queryGraph.addEdge(hotel, isOpen);

        return queryGraph;
    }

    private Graph generateQueryGraph3() {
        Graph queryGraph = new Graph();
        Vertex restaurant = new Vertex(LabelConst.ENTITY_LABEL);
        Vertex location = new Vertex(new JSONObject().toJSONString(), LabelConst.LOCATION_LABEL);
        Vertex category = new Vertex("Restaurants");
        Vertex category2 = new Vertex("Shopping");
        Vertex stars = new Vertex("5.0");
        Vertex isOpen = new Vertex("1");

        queryGraph.addVertex(restaurant);
        queryGraph.addVertex(location);
        queryGraph.addVertex(category);
        queryGraph.addVertex(stars);
        queryGraph.addVertex(isOpen);
        queryGraph.addVertex(category2);

        queryGraph.addEdge(restaurant, location);
        queryGraph.addEdge(restaurant, category);
        queryGraph.addEdge(restaurant, stars);
        queryGraph.addEdge(restaurant, isOpen);
        queryGraph.addEdge(restaurant, category2);

        return queryGraph;
    }

    public void fuzzyLevelTest() {
        GlobalConfig.ENABLE_INDEX = false;
        GlobalConfig.MAX_READ_LINE = (int) 1e6;
        ExperimentDataset.init();
        GraphGenerator.queryGraphCount = 3;
        GraphGenerator.queryGraphSize = 20;

        this.switchYelpDataSet();
        this.conductFuzzyLevelTest();

        log.info("switching FS dataset");
        this.switchFoursquareDataSet();
        this.conductFuzzyLevelTest();

        log.info("switching WD dataset");
        this.switchWikiDataSet();
        this.conductFuzzyLevelTest();

        log.info("switching GW dataset");
        this.switchGowallaDataSet();
        this.conductFuzzyLevelTest();

        log.info("switching BK dataset");
        this.switchBrightkiteDataSet();
        this.conductFuzzyLevelTest();
    }

    public void conductFuzzyLevelTest() {
        MClosestEntityMatchingAlgorithm algorithm = new F2EScanAlgorithm();
        String[] fuzzyLevels = {FuzzyLevel.ONE_HOP, FuzzyLevel.EGO_NETWORK, FuzzyLevel.TWO_HOP};
        //warmup
        algorithm.query(dataGraph, GraphGenerator.generateRandomQueryGraph(dataGraph));
        for (String fuzzyLevel : fuzzyLevels) {
            GlobalConfig.FUZZY_LEVEL = fuzzyLevel;
            long totalCost = 0;
            for (int i = 1; i <= EXPERIMENT_TIMES; ++i) {
                List<Graph> currentRoundQueryGraphs = GraphGenerator.generateRandomQueryGraphWithSkip(dataGraph, i * GraphGenerator.queryGraphCount);
                long startTime = System.currentTimeMillis();
                algorithm.query(dataGraph, currentRoundQueryGraphs);
                long cost = System.currentTimeMillis() - startTime;
                Console.log("{} {} round cost: {}", fuzzyLevel, i, cost);
                totalCost += cost;
            }
            log.info("{}:{}", fuzzyLevel, Math.round(totalCost / (double) EXPERIMENT_TIMES));
        }
    }

    public void arcScaleableTest() {
        int[] intermediateNums = {(int) 1e5, 2 * (int) 1e5, 3 * (int) 1e5, 4 * (int) 1e5, 5 * (int) 1e5};
        int[] queryNums = {(int) 1e3, 2 * (int) 1e3, 3 * (int) 1e3, 4 * (int) 1e3, 5 * (int) 1e3};

        for (int intermediateNum : intermediateNums) {
            conductArcScaleableTest(intermediateNum, queryNums[0]);
        }

        log.info("switching to number of queries statistics");

        for (int queryNum : queryNums) {
            conductArcScaleableTest(intermediateNums[0], queryNum);
        }
    }

    private void conductArcScaleableTest(int intermediateNum, int queryNum) {
        final int N = intermediateNum;
        long arcTotalCost = 0;
        long sortedListTotalCost = 0;
        for (int k = 0; k < EXPERIMENT_TIMES; ++k) {
            List<IntermediateSearchResult> sortedList = new ArrayList<>(N);
            final int C = (int) Math.sqrt(N);
            ArcForest arcForest = new ArcForest(C);
            for (int i = 0; i < C; ++i) {
                ArcTree arcTree = new ArcTree(null);
                for (int j = 0; j < C; ++j) {
                    MatchGroup matchGroup = new MatchGroup(null, getMonotonicDiameter(0));
                    ArcTreeLeafNode arcTreeLeafNode = new ArcTreeLeafNode(matchGroup);
                    IntermediateSearchResult intermediateSearchResult = new IntermediateSearchResult(matchGroup);
                    arcTree.addLeafNode(arcTreeLeafNode);
                    sortedList.add(intermediateSearchResult);
                }
                arcTree.constructArcTree();
                arcForest.add(arcTree);
            }
            Collections.sort(sortedList);

            for (int i = 0; i < queryNum; ++i) {
                long startTime = System.nanoTime();
                if (!arcForest.isRemainCandidate()) {
                    arcForest.pop();
                }
                ArcTree arcTree = arcForest.peek();
                ArcTreeLeafNode arcTreeLeafNode = arcTree.getBestGroupArcTreeLeafNode();
                MatchGroup matchGroup1 = arcTreeLeafNode.getMatchGroup();
                MatchGroup newMatchGroup = new MatchGroup(null, getMonotonicDiameter(matchGroup1.diameter));
                arcTreeLeafNode.updateMatchGroup(newMatchGroup);
                arcForest.update(arcTree);
                arcTotalCost += (System.nanoTime() - startTime) / 1e6;

                startTime = System.nanoTime();
                IntermediateSearchResult intermediateSearchResult = sortedList.get(0);
                MatchGroup matchGroup2 = intermediateSearchResult.getMatchGroup();
                Assert.isTrue(matchGroup1.diameter == matchGroup2.diameter);
                intermediateSearchResult.setMatchGroup(newMatchGroup);
                updateSortedList(sortedList);
                sortedListTotalCost += (System.nanoTime() - startTime) / 1e6;
            }
        }
        arcTotalCost /= EXPERIMENT_TIMES;
        sortedListTotalCost /= EXPERIMENT_TIMES;
        Console.log("arcTotalCost: {}, sortedListTotalCost: {}", arcTotalCost, sortedListTotalCost);
        log.info("{}-{}:{}-{}", intermediateNum / 1e5, queryNum / 1e3, arcTotalCost, sortedListTotalCost);
    }

    private void updateSortedList(List<IntermediateSearchResult> sortedList) {
        IntermediateSearchResult updatedResult = sortedList.get(0);
        for (int i = 1; i < sortedList.size(); ++i) {
            IntermediateSearchResult cur = sortedList.get(i);
            if (cur.compareTo(updatedResult) < 0) {
                sortedList.set(i - 1, cur);
            } else {
                sortedList.set(i - 1, updatedResult);
                return;
            }
        }
        sortedList.set(sortedList.size() - 1, updatedResult);
    }

    private double getMonotonicDiameter(double cur) {
        return RandomUtil.randomDouble(cur + 1 / RandomUtil.randomDouble(1e-6, 1e6), Double.MAX_VALUE);
    }

    @AllArgsConstructor
    private static class IntermediateSearchResult implements Comparable<IntermediateSearchResult> {

        @Getter
        @Setter
        private MatchGroup matchGroup;

        @Override
        public int compareTo(IntermediateSearchResult o) {
            return Double.compare(matchGroup.diameter, o.matchGroup.diameter);
        }
    }

    private void indexEfficientTest() {
        this.switchYelpDataSet();
        this.queryGraphs = GraphGenerator.generateRandomQueryGraph(dataGraph);
        GlobalConfig.INDEX_TYPE = IndexType.BLOOM;
        long bloomCost = this.conductIndexEfficientTest();

        GlobalConfig.INDEX_TYPE = IndexType.TALE;
        this.conductIndexEfficientTest();
        long taleCost = this.conductIndexEfficientTest();
        log.info("{}-{}", bloomCost, taleCost);
    }

    private long conductIndexEfficientTest() {
        long startTime = System.currentTimeMillis();
        indexTestAlgorithm.query(dataGraph, queryGraphs);
        return System.currentTimeMillis() - startTime;
    }

    private void indexSpaceTest() {
        GlobalConfig.INDEX_TYPE = IndexType.BLOOM;
        this.switchYelpDataSet();
        long bloomIndexSize = GraphLayout.parseInstance(dataGraph.bloomIndex).totalSize();

        GlobalConfig.INDEX_TYPE = IndexType.TALE;
        this.switchYelpDataSet();
        long bloomIndexSize2 = GraphLayout.parseInstance(dataGraph.bloomIndex).totalSize();
        long bPlusTreeSize = GraphLayout.parseInstance(dataGraph.bPlusTree).totalSize();
        long tableIndexSize = bloomIndexSize2 + bPlusTreeSize;

        log.info("{}-{}", bloomIndexSize / 1e6, tableIndexSize / 1e6);
    }


    private void switchYelpDataSetWithSpecificReadLine() {
        //avoid OutOfMemoryError
        GlobalConfig.MAX_READ_LINE = (int) currentDataGraphSize;
        this.dataGraph = new Graph();
        System.gc();
        YelpImporter yelpImporter = new YelpImporter();
        yelpImporter.loadDataGraph(dataGraph);
        if (GlobalConfig.ENABLE_INDEX) {
            dataGraph.constructIndex();
        }
    }

    private void switchYelpDataSet() {
        this.dataGraph = ExperimentDataset.switchDataset(Dataset.YELP);
    }

    private void switchWikiDataSet() {
        this.dataGraph = ExperimentDataset.switchDataset(Dataset.WIKIDATA);
    }

    private void switchFoursquareDataSet() {
        this.dataGraph = ExperimentDataset.switchDataset(Dataset.FOURSQUARE);
    }

    private void switchGowallaDataSet() {
        this.dataGraph = ExperimentDataset.switchDataset(Dataset.GOWALLA);
    }

    private void switchBrightkiteDataSet() {
        this.dataGraph = ExperimentDataset.switchDataset(Dataset.BRIGHTKITE);
    }

    public void datasetStatistics() {
        GlobalConfig.MAX_READ_LINE = (int) 1e6;
        ExperimentDataset.init();
        ExperimentDataset.datasetStatistics();
    }
}
