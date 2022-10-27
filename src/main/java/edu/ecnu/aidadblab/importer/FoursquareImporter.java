package edu.ecnu.aidadblab.importer;

import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.config.GlobalConfig;
import edu.ecnu.aidadblab.constant.LabelConst;
import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.Vertex;
import edu.ecnu.aidadblab.tool.GraphGenerator;

import java.util.List;

public class FoursquareImporter {

    public static String INPUT_DIR = GlobalConfig.getDatasetDir() + "/foursquare/";


    private final PlainTxtImporter plainTxtImporter = new PlainTxtImporter(INPUT_DIR);

    public void loadDataGraph(Graph dataGraph, List<JSONObject> supplyData) {
        List<String> data = this.plainTxtImporter.readLine("dataset_ubicomp2013_tags.txt");
        for (String dataItem : data) {
            String[] splitInfo = dataItem.split("\t");
            if (splitInfo.length != 2) {
                continue;
            }
            Vertex entityVertex = new Vertex(splitInfo[0], LabelConst.ENTITY_LABEL);
            dataGraph.addVertex(entityVertex);
            for (String attribute : splitInfo[1].split(",")) {
                Vertex attributeVertex = new Vertex(attribute);
                dataGraph.addVertex(attributeVertex);
                dataGraph.addEdge(entityVertex, attributeVertex);
            }
        }
        GraphGenerator.supplySpatialAttributes(dataGraph, supplyData);
    }

}
