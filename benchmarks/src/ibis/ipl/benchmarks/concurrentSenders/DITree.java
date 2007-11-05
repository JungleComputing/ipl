package ibis.ipl.benchmarks.concurrentSenders;

/* $Id: DITree.java 6546 2007-10-05 13:21:40Z ceriel $ */


import java.io.Serializable;

public final class DITree implements Serializable {

    private static final long serialVersionUID = -958642085796432906L;

    public static final int OBJECT_SIZE = 4 * 4 + 2 * 4;

    DITree left;

    DITree right;

    int i;

    int i1;

    int i2;

    int i3;

    public DITree(int size) {
        int leftSize = size / 2;
        if (leftSize > 0) {
            this.left = new DITree(leftSize);
        }
        if (size - leftSize - 1 > 0) {
            this.right = new DITree(size - leftSize - 1);
        }
    }
}

