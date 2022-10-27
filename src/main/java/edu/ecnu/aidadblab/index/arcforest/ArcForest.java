package edu.ecnu.aidadblab.index.arcforest;

import cn.hutool.core.lang.Console;
import edu.ecnu.aidadblab.index.arctree.ArcTree;

public class ArcForest {

    private final ArcTree[] arcTrees;

    private int counter;

    public ArcForest(final int N) {
        this.counter = 0;
        this.arcTrees = new ArcTree[N];
    }

    public void print() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < counter; ++i) {
            stringBuilder.append(arcTrees[i].getGroupDiameter());
            if ((i << 1) + 1 < counter) {
                stringBuilder.append(" L: ").append(arcTrees[(i << 1) + 1].getGroupDiameter());
            }
            if ((i << 1) + 2 < counter) {
                stringBuilder.append(" R: ").append(arcTrees[(i << 1) + 2].getGroupDiameter());
            }
            stringBuilder.append('\n');
        }
        System.out.println(stringBuilder.toString());
    }

    public void add(ArcTree arcTree) {
        arcTrees[counter++] = arcTree;
        swim(counter - 1);
    }

    public ArcTree peek() {
        return arcTrees[0];
    }

    public void update(ArcTree arcTree) {
        // Console.log("update diameter: {}", arcTree.getGroupDiameter());
        sink(0);
    }

    public void pop() {
        arcTrees[0] = arcTrees[--counter];
        sink(0);
    }

    public boolean isRemainCandidate() {
        return counter > 0;
    }

    public int size() {
        return counter;
    }

    private void sink(int i) {
        ArcTree arcTree = arcTrees[i];
        int child;
        while ((child = (i << 1) + 1) < counter) {
            if (child < counter - 1 && arcTrees[child].getGroupDiameter() > arcTrees[child + 1].getGroupDiameter()) {
                ++child;
            }
            if (arcTrees[child].getGroupDiameter() < arcTree.getGroupDiameter()) {
                arcTrees[i] = arcTrees[child];
                i = child;
            } else {
                break;
            }
        }
        arcTrees[i] = arcTree;
    }

    private void swim(int i) {
        ArcTree arcTree = arcTrees[i];
        int parent;
        while (i > 0 && (parent = (i - 1) >> 1) != i) {
            if (arcTrees[parent].getGroupDiameter() > arcTree.getGroupDiameter()) {
                arcTrees[i] = arcTrees[parent];
                i = parent;
            } else {
                break;
            }
        }
        arcTrees[i] = arcTree;
    }

}
