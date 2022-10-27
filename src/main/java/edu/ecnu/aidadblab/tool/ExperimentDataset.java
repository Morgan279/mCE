package edu.ecnu.aidadblab.tool;

import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.config.GlobalConfig;
import edu.ecnu.aidadblab.constant.Dataset;
import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.Vertex;
import edu.ecnu.aidadblab.importer.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ExperimentDataset {

    private static final Map<String, Graph> datasets = new HashMap<>(5);

    public static void init() {
        YelpImporter yelpImporter = new YelpImporter();
        List<JSONObject> yelpData = yelpImporter.loadYelp();

        Graph graph = new Graph();
        yelpImporter.loadDataGraph(graph);
        datasets.put(Dataset.YELP, graph);

        graph = new Graph();
        FoursquareImporter foursquareImporter = new FoursquareImporter();
        foursquareImporter.loadDataGraph(graph, yelpData);
        datasets.put(Dataset.FOURSQUARE, graph);

        graph = new Graph();
        WikiDataImporter wikiDataImporter = new WikiDataImporter();
        wikiDataImporter.loadDataGraph(graph, yelpData);
        datasets.put(Dataset.WIKIDATA, graph);

        graph = new Graph();
        GowallaImporter gowallaImporter = new GowallaImporter();
        gowallaImporter.loadDataGraph(graph, yelpData);
        datasets.put(Dataset.GOWALLA, graph);

        graph = new Graph();
        BrightkiteImporter brightkiteImporter = new BrightkiteImporter();
        brightkiteImporter.loadDataGraph(graph, yelpData);
        datasets.put(Dataset.BRIGHTKITE, graph);

        if (GlobalConfig.ENABLE_INDEX) {
            for (String datasetName : datasets.keySet()) {
                datasets.get(datasetName).constructIndex();
            }
        }
    }

    public static Graph switchDataset(String datasetName) {
        return datasets.get(datasetName);
    }

    public static void datasetStatistics() {
        for (String datasetName : datasets.keySet()) {
            long totalEdges = 0;
            Graph dataGraph = datasets.get(datasetName);
            for (Vertex v : dataGraph.adjList.keySet()) {
                totalEdges += dataGraph.getDegree(v);
            }
            log.info("{}-|V|:{} |V_s|:{} |E|:{}", datasetName, NumberUtil.decimalFormat(",###", dataGraph.adjList.keySet().size()), NumberUtil.decimalFormat(",###", dataGraph.entityVertexes.size()), NumberUtil.decimalFormat(",###", totalEdges / 2));
        }
    }

}
