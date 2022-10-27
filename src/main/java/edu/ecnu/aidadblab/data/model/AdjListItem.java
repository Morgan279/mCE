package edu.ecnu.aidadblab.data.model;

import lombok.AllArgsConstructor;

import java.util.Objects;

@AllArgsConstructor
public class AdjListItem {
    public String eLabel;

    public String nLabel;

    public String nId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdjListItem)) return false;
        AdjListItem that = (AdjListItem) o;
        return eLabel.equals(that.eLabel) && nLabel.equals(that.nLabel) && nId.equals(that.nId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eLabel, nLabel, nId);
    }
}
