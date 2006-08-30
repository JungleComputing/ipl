/* $Id$ */

import ibis.util.TypedProperties;

public final class List implements TestObject {

    static final int OBJECT_SIZE = 4 * 4 + 1 * 4;

    static final int KARMI_SIZE = 4 * 4;

    static final int LEN = TypedProperties.intProperty("len", 1023);

    List next;

    int size;

    int i1;

    int i2;

    int i3;

    private List(int size) {
        if (size > 0) {
            this.next = new List(size - 1);
        }
        this.size = size;
    }

    public List() {
        this.next = new List(LEN - 1);
    }

    public int object_size() {
        return size * OBJECT_SIZE;
    }

    public int payload() {
        return size * KARMI_SIZE;
    }

    public String id() {
        return "List of " + size + " List objects";
    }

    public int num_objs() {
        return size;
    }
}
