package edu.ecnu.aidadblab.data.model;

import cn.hutool.core.util.HashUtil;
import com.alibaba.fastjson.JSONObject;
import edu.ecnu.aidadblab.config.GlobalConfig;
import edu.ecnu.aidadblab.constant.IndexType;
import edu.ecnu.aidadblab.constant.LabelConst;
import edu.ecnu.aidadblab.index.bplustree.BPlusTree;
import edu.ecnu.aidadblab.index.bplustree.BPlusTreeEntry;
import lombok.NoArgsConstructor;

import java.util.*;

@NoArgsConstructor
public class Graph implements IGraph {

    public Map<Vertex, List<Vertex>> adjList = new HashMap<>(GlobalConfig.MAX_READ_LINE << 4);

    public Set<Vertex> entityVertexes = new HashSet<>(GlobalConfig.MAX_READ_LINE);

    public Map<Vertex, JSONObject> locationMap = new HashMap<>(GlobalConfig.MAX_READ_LINE);

    public Map<Vertex, List<Long>> bloomIndex = new HashMap<>();

    public BPlusTree bPlusTree = new BPlusTree();

    public void constructIndex() {
        for (Vertex entityVertex : entityVertexes) {
            bloomIndex.put(entityVertex, getEntityVertexHashKeys(entityVertex));
        }
        if (GlobalConfig.INDEX_TYPE.equals(IndexType.TALE)) {
            for (Vertex entityVertex : entityVertexes) {
                bPlusTree.insert(entityVertex, new BPlusTreeEntry(getDegree(entityVertex), getNeighborConnection(entityVertex)));
            }
        }
    }

    public List<Long> getEntityVertexHashKeys(Vertex entityVertex) {
        List<Long> hashKeys = new ArrayList<>(3);
        long hashKey1 = 0;
        long hashKey2 = 0;
        long hashKey3 = 0;
        List<Vertex> neighbors = getNeighbors(entityVertex);
        for (int i = 0, len = neighbors.size(); i < len; ++i) {
            Vertex neighbor = neighbors.get(i);
            hashKey1 |= (1L << (Math.abs(HashUtil.rsHash(neighbor.label)) % 64));
            hashKey2 |= (1L << (Math.abs(HashUtil.jsHash(neighbor.label)) % 64));
            hashKey3 |= (1L << (Math.abs(HashUtil.apHash(neighbor.label)) % 64));
        }
        hashKeys.add(hashKey1);
        hashKeys.add(hashKey2);
        return hashKeys;
    }

    public int getNeighborConnection(Vertex entityVertex) {
        int cnt = 0;
        List<Vertex> neighbors = getNeighbors(entityVertex);
        for (int i = 0, end = neighbors.size() - 1; i < end; ++i) {
            for (int j = i + 1; j < neighbors.size(); ++j) {
                if (hasEdge(neighbors.get(i), neighbors.get(j))) {
                    ++cnt;
                }
            }
        }
        return cnt;
    }

    @Override
    public void addVertex(Vertex vertex) {
        if (!hasVertex(vertex)) {
            adjList.put(vertex, new ArrayList<>());
            if (LabelConst.ENTITY_LABEL.equals(vertex.label)) {
                entityVertexes.add(vertex);
            }
        }
//        if (oneHopIndex != null && LabelConst.ENTITY_LABEL.equals(vertex.label)) {
//            oneHopIndex.addEntity(vertex);
//        }
    }

    @Override
    public void addEdge(Vertex startVertex, Vertex endVertex) {
        adjList.get(startVertex).add(endVertex);
        adjList.get(endVertex).add(startVertex);
        if (LabelConst.LOCATION_LABEL.equals(endVertex.label) && LabelConst.ENTITY_LABEL.equals(startVertex.label)) {
            locationMap.put(startVertex, JSONObject.parseObject(endVertex.id));
        }
//        if (oneHopIndex != null && LabelConst.ENTITY_LABEL.equals(startVertex.label)) {
//            oneHopIndex.addNeighbor(startVertex, endVertex);
//        }
    }

    @Override
    public int getDegree(Vertex vertex) {
        return adjList.get(vertex).size();
    }

    @Override
    public List<Vertex> getNeighbors(Vertex vertex) {
        return this.adjList.get(vertex);
    }

    @Override
    public boolean hasEdge(Vertex vertex1, Vertex vertex2) {
        return adjList.get(vertex1).contains(vertex2);
    }

    @Override
    public boolean hasVertex(Vertex vertex) {
        return adjList.containsKey(vertex);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Vertex u : adjList.keySet()) {
            stringBuilder.append(u.label).append(":").append(getNeighbors(u)).append('\n');
        }
        return stringBuilder.toString();
    }

    public Graph clone() {
        Graph graph = new Graph();
        for (Vertex u : this.adjList.keySet()) {
            graph.adjList.put(u, new ArrayList<>(this.adjList.get(u)));
        }
        graph.entityVertexes = this.entityVertexes;
        return graph;
    }
}
