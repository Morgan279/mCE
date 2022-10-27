package edu.ecnu.aidadblab.index.arctree;

public interface ArcTreeNode {

    ArcTreeNode getLeftChild();

    ArcTreeNode getRightChild();

    ArcTreeNonLeafNode getParent();

    void setParent(ArcTreeNonLeafNode parent);

    boolean isLeafNode();

    double getGroupDiameter();

    void delete();

    boolean isDeleted();

}
