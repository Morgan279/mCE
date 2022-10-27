package edu.ecnu.aidadblab.data.model;

import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class ReduceMap {

    public Graph reducedQueryGraph;

    public Vertex queryEntityVertex;

    public List<Vertex> dataEntityVertexes;
}
