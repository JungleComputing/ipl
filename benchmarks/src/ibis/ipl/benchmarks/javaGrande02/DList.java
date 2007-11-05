package ibis.ipl.benchmarks.javaGrande02;

/* $Id: DList.java 6546 2007-10-05 13:21:40Z ceriel $ */

import java.io.Serializable;

public final class DList implements Serializable {

    private static final long serialVersionUID = -1747815590000713983L;

    public static final int PAYLOAD = 4*4;

    DList next, prev;

    int i;
    int i1;
    int i2;
    int i3;

    private DList(int size, DList prev) { 
	if (size > 0) {
	    this.prev = prev;
	    this.next = new DList(size-1);
	}
    } 

    public DList(int size) {
	this.prev = null;
	this.next = new DList(size-1, this);
    }
}





