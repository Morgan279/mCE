package edu.ecnu.aidadblab.processor;

import edu.ecnu.aidadblab.data.model.Graph;
import edu.ecnu.aidadblab.data.model.Vertex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CoreProcessor {

    private Vertex[] vertices;

    public Map<Vertex, Integer> getCoreValueMap(Graph graph) {
        Map<Vertex, Integer> coreValueMap = new HashMap<>();
        vertices = graph.adjList.keySet().toArray(new Vertex[0]);
        int[] deg = new int[vertices.length];

        int maxDegree = Integer.MIN_VALUE;
        for (int i = 0; i < vertices.length; ++i) {
            int curDegree = graph.getDegree(vertices[i]);
            deg[i] = curDegree;
            maxDegree = Math.max(maxDegree, curDegree);
        }

        int[] bin = getBinArray(deg, maxDegree);
        Map<Vertex, Integer> posMap = getPosMap(graph, deg, bin);

        for (int i = maxDegree; i > 0; --i) {
            bin[i] = bin[i - 1];
        }
        bin[0] = 0;

        for (int i = 0; i < vertices.length; ++i) {
            Vertex v = vertices[i];
            coreValueMap.put(v, deg[i]);
            for (Vertex u : graph.getNeighbors(v)) {
                int posU = posMap.get(u);
                int uDegree = deg[posU];
                if (uDegree == deg[i]) continue;

                int posW = bin[uDegree];
                Vertex uDegreeBinFirstVertex = vertices[posW];
                if (!uDegreeBinFirstVertex.equals(u)) {
                    vertices[posW] = u;
                    vertices[posU] = uDegreeBinFirstVertex;
                    posMap.put(u, posW);
                    posMap.put(uDegreeBinFirstVertex, posU);
                }
                ++bin[uDegree];
                --deg[posW];
            }
        }

        return coreValueMap;
    }


    private int[] getBinArray(int[] deg, int maxDegree) {
        int[] bin = new int[maxDegree + 1];

        for (int i = 0; i < vertices.length; ++i) {
            ++bin[deg[i]];
        }

        int start = 0;
        for (int i = 0; i <= maxDegree; ++i) {
            int num = bin[i];
            bin[i] = start;
            start += num;
        }

        return bin;
    }

    private Map<Vertex, Integer> getPosMap(Graph graph, int[] deg, int[] bin) {
        Map<Vertex, Integer> posMap = new HashMap<>();
        Vertex[] originVertices = Arrays.copyOf(vertices, vertices.length);

        for (int i = 0; i < vertices.length; ++i) {
            int curDegree = graph.getDegree(originVertices[i]);
            int pos = bin[curDegree];
            vertices[pos] = originVertices[i];
            deg[pos] = graph.getDegree(vertices[pos]);
            posMap.put(vertices[pos], pos);
            ++bin[curDegree];
        }

        return posMap;
    }

}
