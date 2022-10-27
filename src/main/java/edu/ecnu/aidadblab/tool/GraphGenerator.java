package edu.ecnu.aidadblab.tool;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.config.GlobalConfig;
import edu.ecnu.aidadblab.constant.LabelConst;
import edu.ecnu.aidadblab.constant.LocationComponent;
import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.Vertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class GraphGenerator {

    public static int queryGraphSize = 8;

    public static int queryGraphCount = 3;

    public static void supplyNonSpatialAttributes(Graph dataGraph, List<JSONObject> supplyData, Set<Vertex> locationVertexes) {
        int cnt = 0;
        for (Vertex locationVertex : locationVertexes) {
            if (cnt == supplyData.size()) {
                break;
            }
            JSONObject nonSpatialAttributes = supplyData.get(cnt++);
            Vertex entity = new Vertex(LabelConst.ENTITY_LABEL);
            Vertex name = new Vertex(nonSpatialAttributes.getString("name"));
            Vertex star = new Vertex(nonSpatialAttributes.getString("stars"));
            Vertex city = new Vertex(nonSpatialAttributes.getString("city"));
            Vertex isOpen = new Vertex(nonSpatialAttributes.getString("is_open"));
            Vertex state = new Vertex(nonSpatialAttributes.getString("state"));
            Vertex postalCode = new Vertex(nonSpatialAttributes.getString("postal_code"));
            Vertex address = new Vertex(nonSpatialAttributes.getString("address"));
            Vertex reviewCount = new Vertex(nonSpatialAttributes.getString("review_count"));

            dataGraph.addVertex(entity);
            dataGraph.addVertex(locationVertex);
            dataGraph.addVertex(name);
            dataGraph.addVertex(state);
            dataGraph.addVertex(postalCode);
            dataGraph.addVertex(address);
            dataGraph.addVertex(star);
            dataGraph.addVertex(city);
            dataGraph.addVertex(isOpen);
            dataGraph.addVertex(reviewCount);
            dataGraph.addEdge(entity, name);
            dataGraph.addEdge(entity, state);
            dataGraph.addEdge(entity, postalCode);
            dataGraph.addEdge(entity, address);
            dataGraph.addEdge(entity, star);
            dataGraph.addEdge(entity, city);
            dataGraph.addEdge(entity, isOpen);
            dataGraph.addEdge(entity, reviewCount);
            dataGraph.addEdge(entity, locationVertex);

            if (nonSpatialAttributes.getString("categories") != null) {
                for (String categoryString : nonSpatialAttributes.getString("categories").split(", ")) {
                    Vertex category = new Vertex(categoryString);
                    dataGraph.addVertex(category);
                    dataGraph.addEdge(entity, category);
                }
            }

            if (nonSpatialAttributes.getJSONObject("hours") != null) {
                JSONObject hours = nonSpatialAttributes.getJSONObject("hours");
                for (String hour : hours.keySet()) {
                    Vertex hourVertex = new Vertex(hour + "-" + hours.getString(hour));
                    dataGraph.addVertex(hourVertex);
                    dataGraph.addEdge(entity, hourVertex);
                }
            }
        }
    }

    public static void supplySpatialAttributes(Graph dataGraph, List<JSONObject> supplyData) {
        int cnt = 0;
        for (Vertex entity : dataGraph.entityVertexes) {
            if (cnt == supplyData.size()) {
                break;
            }
            JSONObject locationItem = supplyData.get(cnt++);
            double latitude = locationItem.getDoubleValue(LocationComponent.LATITUDE);
            double longitude = locationItem.getDoubleValue(LocationComponent.LONGITUDE);
            JSONObject location = new JSONObject();
            location.put(LocationComponent.LATITUDE, latitude);
            location.put(LocationComponent.LONGITUDE, longitude);
            Vertex locationVertex = new Vertex(location.toJSONString(), LabelConst.LOCATION_LABEL);
            dataGraph.addVertex(locationVertex);
            dataGraph.addEdge(entity, locationVertex);
        }
    }

    public static Graph generateRandomDataGraphWithLocationVertexes(Graph dataGraph, Set<Vertex> locationVertexes) {
        final int size = locationVertexes.size();
        final int LABEL_AMOUNT = (int) (Math.exp(5) * Math.sqrt(size));
        List<Vertex> vertexes = new ArrayList<>(LABEL_AMOUNT);
        for (Vertex locationVertex : locationVertexes) {
            Vertex entityVertex = new Vertex(LabelConst.ENTITY_LABEL);
            vertexes.add(entityVertex);
            dataGraph.addVertex(entityVertex);
            dataGraph.addVertex(locationVertex);
            dataGraph.addEdge(entityVertex, locationVertex);
            for (int j = 0, len = ThreadLocalRandom.current().nextInt(8, 12); j < len; ++j) {
                String value = String.valueOf(RandomUtil.randomInt(0, LABEL_AMOUNT));
                Vertex vertex = new Vertex(value, value);
                dataGraph.addVertex(vertex);
                dataGraph.addEdge(entityVertex, vertex);
                vertexes.add(vertex);
            }
        }
        for (int i = 0; i < vertexes.size(); ++i) {
            for (int j = i + 1; j < vertexes.size(); ++j) {
                if (ThreadLocalRandom.current().nextDouble() < 0.618 && !dataGraph.hasEdge(vertexes.get(i), vertexes.get(j))) {
                    Vertex vertex1 = vertexes.get(i);
                    Vertex vertex2 = vertexes.get(j);
                    if (LabelConst.ENTITY_LABEL.equals(vertex1.label) || LabelConst.ENTITY_LABEL.equals(vertex2.label)) {
                        continue;
                    }
                    dataGraph.addEdge(vertex1, vertex2);
                }
            }
        }
        return dataGraph;
    }

    public Graph generateRandomDataGraph(int size) {
        Graph dataGraph = new Graph();
        final int LABEL_AMOUNT = size * 20;
        List<Vertex> vertexes = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            Vertex entityVertex = new Vertex(LabelConst.ENTITY_LABEL);
            Vertex locationVertex = generateRandomLocationVertex();
            dataGraph.addVertex(entityVertex);
            dataGraph.addVertex(locationVertex);
            dataGraph.addEdge(entityVertex, locationVertex);
            for (int j = 0, len = size / 2; j < len; ++j) {
                Vertex vertex = new Vertex(String.valueOf(RandomUtil.randomInt(0, LABEL_AMOUNT)));
                vertexes.add(vertex);
                dataGraph.addVertex(vertex);
                dataGraph.addEdge(entityVertex, vertex);
            }
        }
        for (int i = 0; i < vertexes.size(); ++i) {
            for (int j = i + 1; j < vertexes.size(); ++j) {
                if (ThreadLocalRandom.current().nextDouble() < 0.6) {
                    dataGraph.addEdge(vertexes.get(i), vertexes.get(j));
                }
            }
        }
        if (GlobalConfig.ENABLE_INDEX) {
            dataGraph.constructIndex();
        }
        return dataGraph;
    }

    public static Vertex generateRandomLocationVertex() {
        JSONObject location = new JSONObject();
        location.put(LocationComponent.LATITUDE, ThreadLocalRandom.current().nextDouble(-90, 90));
        location.put(LocationComponent.LONGITUDE, ThreadLocalRandom.current().nextDouble(-180, 180));
        return new Vertex(location.toJSONString(), LabelConst.LOCATION_LABEL);
    }

    public static List<Graph> generateRandomQueryGraph(Graph dataGraph) {
        List<Graph> randomQueryGraphs = new ArrayList<>();
        for (Vertex v : dataGraph.entityVertexes) {
            Vertex locationVertex = findLocationVertex(v, dataGraph);
            if (locationVertex == null) continue;

            Graph queryGraph = new Graph();
            queryGraph.addVertex(v);
            queryGraph.addVertex(locationVertex);
            queryGraph.addEdge(v, locationVertex);
            dfs(v, dataGraph, queryGraph);
            if (queryGraph.adjList.keySet().size() != queryGraphSize) {
                continue;
            }
            randomQueryGraphs.add(queryGraph);
            if (randomQueryGraphs.size() == queryGraphCount) {
                break;
            }
        }

        return randomQueryGraphs;
    }


    public static List<Graph> generateRandomQueryGraphWithSkip(Graph dataGraph, int skip) {
        List<Graph> randomQueryGraphs = new ArrayList<>();
        for (Vertex v : dataGraph.entityVertexes) {
            Vertex locationVertex = findLocationVertex(v, dataGraph);
            if (locationVertex == null) continue;
            if (skip > 0) {
                --skip;
                continue;
            }
            Graph queryGraph = new Graph();
            queryGraph.addVertex(v);
            queryGraph.addVertex(locationVertex);
            queryGraph.addEdge(v, locationVertex);
            dfs(v, dataGraph, queryGraph);
            if (queryGraph.adjList.keySet().size() != queryGraphSize) {
                continue;
            }
            randomQueryGraphs.add(queryGraph);
            if (randomQueryGraphs.size() == queryGraphCount) {
                break;
            }
        }

        return randomQueryGraphs;
    }


    private static void dfs(Vertex v, Graph dataGraph, Graph queryGraph) {

        for (Vertex u : dataGraph.getNeighbors(v)) {
            if (queryGraph.adjList.keySet().size() >= queryGraphSize) return;
            if (!queryGraph.hasVertex(u) && !LabelConst.ENTITY_LABEL.equals(u.label)) {
                queryGraph.addVertex(u);
                if (!queryGraph.hasEdge(v, u)) {
                    queryGraph.addEdge(v, u);
                }
                dfs(u, dataGraph, queryGraph);
            }
        }
    }

    private static Vertex findLocationVertex(Vertex v, Graph dataGraph) {
        for (Vertex u : dataGraph.getNeighbors(v)) {
            if (LabelConst.LOCATION_LABEL.equals(u.label)) return u;
        }
        return null;
    }

    private static boolean hasLocationVertex(Graph queryGraph) {
        for (Vertex u : queryGraph.getNeighbors(queryGraph.entityVertexes.iterator().next())) {
            if (LabelConst.LOCATION_LABEL.equals(u.label)) return true;
        }
        return false;
    }

}
