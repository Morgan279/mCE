package edu.ecnu.aidadblab.data.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MatchGroup {

    public MatchGroup(List<Vertex> matchVertex, double diameter) {
        this.matchVertex = matchVertex;
        this.diameter = diameter;
    }

    public List<Vertex> matchVertex;

    public Map<Integer, Vertex> entityMap;

    public double diameter;

}
