package edu.ecnu.aidadblab.data.model;

import lombok.AllArgsConstructor;

import java.util.Objects;

@AllArgsConstructor
public class PivotPair {
    public Vertex uPrime;

    public Vertex u;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PivotPair)) return false;
        PivotPair pivotPair = (PivotPair) o;
        return (uPrime.equals(pivotPair.uPrime) && u.equals(pivotPair.u))
                || (uPrime.equals(pivotPair.u) && u.equals(pivotPair.uPrime));
    }

    @Override
    public int hashCode() {
        return Objects.hash(uPrime) + Objects.hash(u);
    }
}
