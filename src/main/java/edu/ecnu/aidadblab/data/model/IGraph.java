package edu.ecnu.aidadblab.data.model;

import java.util.List;

public interface IGraph {

    void addVertex(Vertex vertex);

    void addEdge(Vertex startVertex, Vertex endVertex);

    int getDegree(Vertex vertex);

    List<Vertex> getNeighbors(Vertex vertex);

    boolean hasEdge(Vertex vertex1, Vertex vertex2);

    boolean hasVertex(Vertex vertex);
}
