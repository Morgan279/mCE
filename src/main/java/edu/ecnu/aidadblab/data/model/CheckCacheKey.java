package edu.ecnu.aidadblab.data.model;

import lombok.AllArgsConstructor;

import java.util.Objects;

@AllArgsConstructor
public class CheckCacheKey {

    private final Vertex v;

    private final int tag;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CheckCacheKey)) return false;
        CheckCacheKey that = (CheckCacheKey) o;
        return tag == that.tag && v.equals(that.v);
    }

    @Override
    public int hashCode() {
        return Objects.hash(v.id, tag);
    }
}
