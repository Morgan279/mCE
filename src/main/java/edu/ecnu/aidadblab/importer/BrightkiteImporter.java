package edu.ecnu.aidadblab.importer;

import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.config.GlobalConfig;
import edu.ecnu.aidadblab.constant.LabelConst;
import edu.ecnu.aidadblab.constant.LocationComponent;
import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.Vertex;
import edu.ecnu.aidadblab.tool.GraphGenerator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BrightkiteImporter {

    public static String INPUT_DIR = GlobalConfig.getDatasetDir() + "/brightkite/";


    private final PlainTxtImporter plainTxtImporter = new PlainTxtImporter(INPUT_DIR);

    public void loadDataGraph(Graph dataGraph, List<JSONObject> supplyData) {
        List<String> data = this.plainTxtImporter.readLine("Brightkite_totalCheckins.txt");
        Set<Vertex> locationVertexes = new HashSet<>(data.size());
        for (String dataItem : data) {
            String[] splitInfo = dataItem.split("\t");
            if (splitInfo.length != 5) {
                continue;
            }
            JSONObject location = new JSONObject();
            location.put(LocationComponent.LATITUDE, splitInfo[2]);
            location.put(LocationComponent.LONGITUDE, splitInfo[3]);
            Vertex locationVertex = new Vertex(location.toJSONString(), LabelConst.LOCATION_LABEL);
            locationVertexes.add(locationVertex);
        }
        GraphGenerator.supplyNonSpatialAttributes(dataGraph, supplyData, locationVertexes);
    }

}
