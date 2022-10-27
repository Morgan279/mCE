package edu.ecnu.aidadblab.data.model;

import lombok.AllArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
public class Vertex {
    public String id;

    public String label;

    public Vertex(String label) {
        this.id = UUID.randomUUID().toString();
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vertex)) return false;
        Vertex vertex = (Vertex) o;
        return id.equals(vertex.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + "-" + label;
    }
}
