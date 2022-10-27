package edu.ecnu.aidadblab.data.model;

import cn.hutool.core.text.StrBuilder;

import java.util.*;


public class Match {

    private Map<Vertex, Vertex> matchMap = new HashMap<>();

    public void add(Vertex u, Vertex v) {
        matchMap.put(u, v);
    }

    public void remove(Vertex u) {
        matchMap.remove(u);
    }

    public Vertex get(Vertex u) {
        return matchMap.get(u);
    }

    public int size() {
        return this.matchMap.keySet().size();
    }

    public Set<Vertex> getQueryVertexSet() {
        return this.matchMap.keySet();
    }

    public Collection<Vertex> getDataVertexes() {
        return this.matchMap.values();
    }

    @Override
    public Match clone() {
        Match match = new Match();
        for (Vertex u : matchMap.keySet()) {
            match.add(u, matchMap.get(u));
        }
        return match;
    }

    @Override
    public String toString() {
        StrBuilder strBuilder = new StrBuilder();
        for (Vertex u : matchMap.keySet()) {
            strBuilder.append("(").append(u.label).append(", ").append(matchMap.get(u)).append(") ");
        }

        return strBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Match)) return false;
        Match match = (Match) o;
        Set<Vertex> hashSet = new HashSet<>(getDataVertexes());
        hashSet.addAll(match.getDataVertexes());
        return hashSet.size() == getDataVertexes().size();
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        for (Vertex v : getDataVertexes()) {
            hashCode += v.id.hashCode();
        }
        return hashCode;
    }
}
