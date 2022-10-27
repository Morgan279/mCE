package edu.ecnu.aidadblab.index.arctree;

import edu.ecnu.aidadblab.data.model.Vertex;

import java.util.ArrayList;
import java.util.List;

public class ArcTree {

    public Vertex centerVertex;

    private ArcTreeNode root;

    List<ArcTreeNode> leafNodes = new ArrayList<>();

    public ArcTree(Vertex centerVertex) {
        this.centerVertex = centerVertex;
    }

    public ArcTreeLeafNode getBestGroupArcTreeLeafNode() {
        if (root.getLeftChild() == null && root.getRightChild() == null) {
            return (ArcTreeLeafNode) root;
        }

        return findBestGroupLeafNode(root);
    }

    public void addLeafNode(ArcTreeLeafNode arcTreeLeafNode) {
        leafNodes.add(arcTreeLeafNode);
    }

    public void constructArcTree() {
        //System.out.println("leaf nodes: " + leafNodes.size());
        if (leafNodes.isEmpty()) {
            root = null;
            return;
        }

        if (leafNodes.size() == 1) {
            root = leafNodes.get(0);
            return;
        }

        this.constructArcTree(leafNodes);
    }

    public double getGroupDiameter() {
        return isRemainCandidates() ? this.root.getGroupDiameter() : Double.MAX_VALUE;
    }

    public boolean isRemainCandidates() {
        //System.out.println(root.getGroupDiameter());
        //return root != null && !root.isDeleted();
        return root != null && root.getGroupDiameter() != Double.MAX_VALUE;
    }

    private ArcTreeLeafNode findBestGroupLeafNode(ArcTreeNode curNode) {
        if (curNode.isLeafNode()) return (ArcTreeLeafNode) curNode;
        
        if (curNode.getLeftChild() == null) {
            return findBestGroupLeafNode(curNode.getRightChild());
        }

        if (curNode.getRightChild() == null) {
            return findBestGroupLeafNode(curNode.getLeftChild());
        }

        ArcTreeNode left = curNode.getLeftChild();
        ArcTreeNode right = curNode.getRightChild();

        return left.getGroupDiameter() < right.getGroupDiameter()
                ? findBestGroupLeafNode(left)
                : findBestGroupLeafNode(right);
    }

    private void constructArcTree(List<ArcTreeNode> arcTreeNodes) {
        if (arcTreeNodes.size() == 2) {
            this.root = new ArcTreeNonLeafNode(arcTreeNodes.get(0), arcTreeNodes.get(1));
            arcTreeNodes.get(0).setParent((ArcTreeNonLeafNode) root);
            arcTreeNodes.get(1).setParent((ArcTreeNonLeafNode) root);
            return;
        }

        int N;
        boolean isEven;
        if ((arcTreeNodes.size() & 1) == 0) {
            N = arcTreeNodes.size();
            isEven = true;
        } else {
            N = arcTreeNodes.size() - 1;
            isEven = false;
        }

        List<ArcTreeNode> arcTreeParentNodes = new ArrayList<>((int) Math.ceil(arcTreeNodes.size() / 2d));
        for (int i = 0; i < N; i += 2) {
            ArcTreeNode left = arcTreeNodes.get(i);
            ArcTreeNode right = arcTreeNodes.get(i + 1);
            ArcTreeNonLeafNode parent = new ArcTreeNonLeafNode(left, right);
            left.setParent(parent);
            right.setParent(parent);
            arcTreeParentNodes.add(parent);
        }
        if (!isEven) {
            ArcTreeNode left = arcTreeNodes.get(N);
            ArcTreeNonLeafNode parent = new ArcTreeNonLeafNode(left, null);
            left.setParent(parent);
            arcTreeParentNodes.add(parent);
        }

        constructArcTree(arcTreeParentNodes);
    }
}
