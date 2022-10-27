package edu.ecnu.aidadblab.index.bplustree;

import edu.ecnu.aidadblab.data.model.Vertex;

import java.util.Collections;
import java.util.List;

public class BPlusTree {

    private BPlusTreeNode root;

    public void insert(Vertex v, BPlusTreeEntry entry) {
        if (root == null) {
            root = new BPlusTreeLeafNode(BPlusTreeNode.asList(entry), BPlusTreeNode.asList(BPlusTreeNode.asList(v)));
            return;
        }

        BPlusTreeNode newChildNode = root.insert(v, entry);
        if (newChildNode != null) {
            BPlusTreeEntry newRootEntry = newChildNode.getClass().equals(BPlusTreeLeafNode.class) ? newChildNode.entries.get(0) : ((BPlusTreeNonLeafNode) newChildNode).findLeafEntry(newChildNode);
            root = new BPlusTreeNonLeafNode(BPlusTreeNode.asList(newRootEntry), BPlusTreeNode.asList(root, newChildNode));
        }
    }

    public List<Vertex> search(BPlusTreeEntry entry) {
        if (root == null) {
            return Collections.emptyList();
        }
        return root.search(entry);
    }
}
