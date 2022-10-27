package edu.ecnu.aidadblab.data.model;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class FuzzyMatchIntermediate {

    public List<List<Vertex>> candidateDataEntityVertexes;

    public List<ReduceMap> reduceMaps;

    public Map<Vertex, JSONObject> locationMap;

    public Map<Vertex, List<Integer>> structTagMap;

    public List<Vertex> vertexList;

    public FuzzyMatchIntermediate(List<List<Vertex>> candidateDataEntityVertexes, List<ReduceMap> reduceMaps, Map<Vertex, JSONObject> locationMap) {
        this.candidateDataEntityVertexes = candidateDataEntityVertexes;
        this.reduceMaps = reduceMaps;
        this.locationMap = locationMap;
    }
}
