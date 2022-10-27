package edu.ecnu.aidadblab.index.bplustree;

import edu.ecnu.aidadblab.data.model.Vertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BPlusTreeNode {

    protected static final int KEY_UPPER_BOUND = 256;

    protected List<BPlusTreeEntry> entries;

    protected boolean isFull() {
        return entries.size() == KEY_UPPER_BOUND;
    }

    protected int getMedianIndex() {
        return KEY_UPPER_BOUND / 2;
    }

    protected int findEntryIndex(BPlusTreeEntry bPlusTreeEntry) {
        int l = 0;
        int r = entries.size() - 1;
        int index = entries.size();
        while (l <= r) {
            int mid = l + ((r - l) >> 1);
            if (entries.get(mid).compareTo(bPlusTreeEntry) >= 0) {
                index = mid;
                r = mid - 1;
            } else {
                l = mid + 1;
            }
        }
        return index;
    }

    @SafeVarargs
    public static <T> List<T> asList(T... e) {
        List<T> res = new ArrayList<>();
        Collections.addAll(res, e);
        return res;
    }

    public abstract BPlusTreeNode insert(Vertex v, BPlusTreeEntry entry);

    public abstract List<Vertex> search(BPlusTreeEntry entry);
}
