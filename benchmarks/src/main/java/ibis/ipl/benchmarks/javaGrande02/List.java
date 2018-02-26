package ibis.ipl.benchmarks.javaGrande02;

/* $Id$ */

import java.io.Serializable;

public final class List implements Serializable {

    private static final long serialVersionUID = 3707293386751871390L;

    public static final int PAYLOAD = 4*4;

    List next;

    int i;
    int i1;
    int i2;
    int i3;

    public List(int size) {
	if (size > 0) {
	    this.next = new List(size-1);
	}
    }
}





