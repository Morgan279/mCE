package edu.ecnu.aidadblab.index.bplustree;

import edu.ecnu.aidadblab.data.model.Vertex;

import java.util.ArrayList;
import java.util.List;

public class BPlusTreeLeafNode extends BPlusTreeNode {

    public List<List<Vertex>> data;

    public BPlusTreeLeafNode next;

    public BPlusTreeLeafNode(List<BPlusTreeEntry> entries, List<List<Vertex>> data) {
        this.entries = entries;
        this.data = data;
    }

    @Override
    public List<Vertex> search(BPlusTreeEntry entry) {
        List<Vertex> res = new ArrayList<>();
        for (int i = findEntryIndex(entry); i < data.size(); ++i) {
            res.addAll(data.get(i));
        }
        BPlusTreeLeafNode nextLeafNode = next;
        while (nextLeafNode != null) {
            for (int i = 0; i < nextLeafNode.entries.size(); ++i) {
                if (nextLeafNode.entries.get(i).compareTo(entry) >= 0) {
                    res.addAll(nextLeafNode.data.get(i));
                } else {
                    return res;
                }
            }
            nextLeafNode = nextLeafNode.next;
        }
        return res;
    }

    @Override
    public BPlusTreeNode insert(Vertex v, BPlusTreeEntry entry) {
        int equalEntryIndex = getEqualEntryIndex(entry);
        if (equalEntryIndex != -1) {
            data.get(equalEntryIndex).add(v);
            return null;
        }

        int index = findEntryIndex(entry);

        if (isFull()) {
            BPlusTreeLeafNode newLeafNode = split();
            int medianIndex = getMedianIndex();
            if (index < medianIndex) {
                entries.add(index, entry);
                data.add(index, asList(v));
            } else {
                int rightIndex = index - medianIndex;
                newLeafNode.entries.add(rightIndex, entry);
                newLeafNode.data.add(rightIndex, asList(v));
            }
            return newLeafNode;
        }

        entries.add(index, entry);
        data.add(index, asList(v));
        return null;
    }

    private BPlusTreeLeafNode split() {
        int medianIndex = getMedianIndex();
        List<BPlusTreeEntry> allEntries = entries;
        List<List<Vertex>> allData = data;

        List<BPlusTreeEntry> leftEntries = new ArrayList<>(allEntries.subList(0, medianIndex));
        List<List<Vertex>> leftData = new ArrayList<>(allData.subList(0, medianIndex));
        this.entries = leftEntries;
        this.data = leftData;

        List<BPlusTreeEntry> rightEntries = new ArrayList<>(allEntries.subList(medianIndex, allEntries.size()));
        List<List<Vertex>> rightData = new ArrayList<>(allData.subList(medianIndex, allData.size()));
        BPlusTreeLeafNode newLeafNode = new BPlusTreeLeafNode(rightEntries, rightData);

        newLeafNode.next = this.next;
        this.next = newLeafNode;
        return newLeafNode;
    }

    private int getEqualEntryIndex(BPlusTreeEntry bPlusTreeEntry) {
        int l = 0;
        int r = entries.size() - 1;
        while (l <= r) {
            int mid = l + ((r - l) >> 1);
            int compare = entries.get(mid).compareTo(bPlusTreeEntry);
            if (compare == 0) {
                return mid;
            } else if (compare > 0) {
                r = mid - 1;
            } else {
                l = mid + 1;
            }
        }
        return -1;
    }
}
