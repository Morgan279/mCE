package edu.ecnu.aidadblab.index.bplustree;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BPlusTreeEntry implements Comparable<BPlusTreeEntry> {

    public int degree;

    public int neighborConnection;

    @Override
    public int compareTo(BPlusTreeEntry o) {
        if (degree == o.degree) {
            return neighborConnection - o.neighborConnection;
        }
        return degree - o.degree;
    }
}
