/* $Id$ */


import java.io.IOException;
import java.io.Serializable;

public final class DList implements Serializable {

    public static final int PAYLOAD = 4 * 4;

    DList next, prev;

    int i;

    int i1;

    int i2;

    int i3;

    private DList(int size, DList prev) {
        if (size > 0) {
            this.prev = prev;
            this.next = new DList(size - 1);
        }
    }

    public DList(int size) {
        this.prev = null;
        this.next = new DList(size - 1, this);
    }
}

