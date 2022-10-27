package edu.ecnu.aidadblab.index.bplustree;

import edu.ecnu.aidadblab.data.model.Vertex;

import java.util.ArrayList;
import java.util.List;

public class BPlusTreeNonLeafNode extends BPlusTreeNode {

    private List<BPlusTreeNode> children;

    public BPlusTreeNonLeafNode(List<BPlusTreeEntry> entries, List<BPlusTreeNode> children) {
        this.entries = entries;
        this.children = children;
    }

    @Override
    public List<Vertex> search(BPlusTreeEntry entry) {
        return children.get(findChildIndex(findEntryIndex(entry), entry)).search(entry);
    }

    @Override
    public BPlusTreeNode insert(Vertex v, BPlusTreeEntry entry) {
        BPlusTreeNode newChildNode = children.get(findChildIndex(findEntryIndex(entry), entry)).insert(v, entry);

        if (newChildNode != null) {
            BPlusTreeEntry newEntry = findLeafEntry(newChildNode);
            int newEntryIndex = findEntryIndex(newEntry);
            if (isFull()) {
                BPlusTreeNonLeafNode newNonLeafNode = split();
                int medianIndex = getMedianIndex();
                if (newEntryIndex < medianIndex) {
                    entries.add(newEntryIndex, newEntry);
                    children.add(newEntryIndex + 1, newChildNode);
                } else {
                    int rightIndex = newNonLeafNode.findEntryIndex(newEntry);
                    newNonLeafNode.entries.add(rightIndex, newEntry);
                    newNonLeafNode.children.add(rightIndex, newChildNode);
                }
                newNonLeafNode.entries.remove(0);
                return newNonLeafNode;
            }

            entries.add(newEntryIndex, newEntry);
            children.add(newEntryIndex + 1, newChildNode);
        }

        return null;
    }

    public BPlusTreeEntry findLeafEntry(BPlusTreeNode cur) {
        if (cur.getClass().equals(BPlusTreeLeafNode.class)) {
            return cur.entries.get(0);
        }
        return findLeafEntry(((BPlusTreeNonLeafNode) cur).children.get(0));
    }

    private int findChildIndex(int entryIndex, BPlusTreeEntry entry) {
        return (entryIndex == entries.size() || entry.compareTo(entries.get(entryIndex)) < 0) ? entryIndex : entryIndex + 1;
    }

    private BPlusTreeNonLeafNode split() {
        int medianIndex = getMedianIndex();
        List<BPlusTreeEntry> allEntries = entries;
        List<BPlusTreeNode> allChildren = children;

        List<BPlusTreeEntry> leftEntries = new ArrayList<>(allEntries.subList(0, medianIndex));
        List<BPlusTreeNode> leftChildren = new ArrayList<>(allChildren.subList(0, medianIndex + 1));
        this.entries = leftEntries;
        this.children = leftChildren;

        List<BPlusTreeEntry> rightEntries = new ArrayList<>(allEntries.subList(medianIndex, allEntries.size()));
        List<BPlusTreeNode> rightChildren = new ArrayList<>(allChildren.subList(medianIndex + 1, allChildren.size()));
        return new BPlusTreeNonLeafNode(rightEntries, rightChildren);
    }

}
