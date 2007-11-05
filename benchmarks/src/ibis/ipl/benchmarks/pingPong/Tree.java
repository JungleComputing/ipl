package ibis.ipl.benchmarks.pingPong;

/* $Id: Tree.java 6546 2007-10-05 13:21:40Z ceriel $ */

import java.io.Serializable;

public final class Tree implements Serializable {

    private static final long serialVersionUID = -3021696985605964883L;

    public static final int PAYLOAD = 4*4;

    Tree left;
    Tree right;

    int i;
    int i1;
    int i2;
    int i3;

    public Tree(int size) {
	int leftSize = size / 2;
	if (leftSize > 0) {
	    this.left = new Tree(leftSize);
	}
	if (size - leftSize - 1 > 0) {
	    this.right = new Tree(size - leftSize - 1);
	}
    }
}





