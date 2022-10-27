package edu.ecnu.aidadblab.index.arctree;

public class ArcTreeNonLeafNode implements ArcTreeNode {


    private ArcTreeNode leftChild;

    private ArcTreeNode rightChild;

    private double groupDiameter;

    private ArcTreeNonLeafNode parent;

    private boolean deletedTag;

    public ArcTreeNonLeafNode(ArcTreeNode leftChild, ArcTreeNode rightChild) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.groupDiameter = rightChild == null ? leftChild.getGroupDiameter() : Math.min(leftChild.getGroupDiameter(), rightChild.getGroupDiameter());
        this.deletedTag = false;
    }

    public void deleteChild(ArcTreeNode child) {
        if (child == leftChild) {
            leftChild = null;
            if (rightChild != null) {
                groupDiameter = rightChild.getGroupDiameter();
            }
        } else if (child == rightChild) {
            rightChild = null;
            if (leftChild != null) {
                groupDiameter = leftChild.getGroupDiameter();
            }
        }

        if (leftChild == null && rightChild == null) {
            this.delete();
        }
    }

    @Override
    public ArcTreeNode getLeftChild() {
        return leftChild;
    }

    @Override
    public ArcTreeNode getRightChild() {
        return rightChild;
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
        return false;
    }

    @Override
    public double getGroupDiameter() {
        return groupDiameter;
    }

    public void updateGroupDiameter(ArcTreeNode child) {
        //更新后组半径只会增大
        if (child == leftChild) {
            if (rightChild != null && this.groupDiameter == rightChild.getGroupDiameter()) return;

            if (rightChild == null || child.getGroupDiameter() < rightChild.getGroupDiameter()) {
                this.groupDiameter = child.getGroupDiameter();
            } else {
                this.groupDiameter = rightChild.getGroupDiameter();
            }
        } else if (child == rightChild) {
            if (leftChild != null && this.groupDiameter == leftChild.getGroupDiameter()) return;

            if (leftChild == null || child.getGroupDiameter() < leftChild.getGroupDiameter()) {
                this.groupDiameter = child.getGroupDiameter();
            } else {
                this.groupDiameter = leftChild.getGroupDiameter();
            }
        }

        if (this.getParent() != null) {
            this.getParent().updateGroupDiameter(this);
        }
    }

    @Override
    public void delete() {
        if (deletedTag) return;
        //Console.log("{} deleted", this);
        this.deletedTag = true;
        if (this.getParent() != null) {
            this.getParent().deleteChild(this);
        }
    }

    @Override
    public boolean isDeleted() {
        return deletedTag;
    }
}
