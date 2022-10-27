package edu.ecnu.aidadblab.index.arctree;

import edu.ecnu.aidadblab.data.model.CircleScanItem;
import edu.ecnu.aidadblab.data.model.MatchGroup;
import edu.ecnu.aidadblab.data.model.Vertex;
import lombok.Getter;

import java.util.Set;

public class ArcTreeLeafNode implements ArcTreeNode {


    private double angle;

    @Getter
    private int[] tab;

    @Getter
    private Set<Vertex> coverVertex;

    @Getter
    private MatchGroup matchGroup;

    private ArcTreeNonLeafNode parent;

    boolean deletedTag;

    public ArcTreeLeafNode(CircleScanItem circleScanItem, MatchGroup matchGroup, Set<Vertex> coverVertex, int[] tab) {
        this.angle = circleScanItem.angle.angleDegree;
        this.matchGroup = matchGroup;
        this.coverVertex = coverVertex;
        this.tab = tab;
        this.deletedTag = false;
    }

    public ArcTreeLeafNode(MatchGroup matchGroup) {
        this.matchGroup = matchGroup;
        this.deletedTag = false;
    }

    public void updateMatchGroup(MatchGroup matchGroup) {
        this.matchGroup = matchGroup;
        if (this.getParent() != null) {
            this.getParent().updateGroupDiameter(this);
        }
    }

    @Override
    public ArcTreeNode getLeftChild() {
        return null;
    }

    @Override
    public ArcTreeNode getRightChild() {
        return null;
    }

    @Override
    public ArcTreeNonLeafNode getParent() {
        return parent;
    }

    @Override
    public void setParent(ArcTreeNonLeafNode parent) {
        this.parent = parent;
    }

    @Override
    public boolean isLeafNode() {
        return true;
    }

    @Override
    public double getGroupDiameter() {
        return matchGroup.diameter;
    }


    @Override
    public void delete() {
        if (deletedTag) return;

        this.deletedTag = true;
        this.matchGroup.diameter = Double.MAX_VALUE;
        if (this.getParent() != null) {
            this.getParent().updateGroupDiameter(this);
            //this.getParent().deleteChild(this);
        }
    }

    @Override
    public boolean isDeleted() {
        return deletedTag;
    }
}
