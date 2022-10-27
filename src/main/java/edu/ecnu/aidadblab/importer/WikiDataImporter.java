package edu.ecnu.aidadblab.importer;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.config.GlobalConfig;
import edu.ecnu.aidadblab.constant.LabelConst;
import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.Vertex;
import edu.ecnu.aidadblab.tool.GraphGenerator;

import java.util.List;

public class WikiDataImporter {

    public static String INPUT_DIR = GlobalConfig.getDatasetDir() + "/wikidata/";

    private final JsonTxtImporter jsonTxtImporter = new JsonTxtImporter(INPUT_DIR);

    public void loadDataGraph(Graph dataGraph, List<JSONObject> supplyData) {
        JSONObject data = this.jsonTxtImporter.readLine("musae_chameleon_features.json").get(0);
        for (String entity : data.keySet()) {
            Vertex entityVertex = new Vertex(entity, LabelConst.ENTITY_LABEL);
            dataGraph.addVertex(entityVertex);
            JSONArray attributes = data.getJSONArray(entity);
            for (String attribute : attributes.toJavaList(String.class)) {
                Vertex attributeVertex = new Vertex(attribute);
                dataGraph.addVertex(attributeVertex);
                dataGraph.addEdge(entityVertex, attributeVertex);
            }
        }
        GraphGenerator.supplySpatialAttributes(dataGraph, supplyData);
    }

}
