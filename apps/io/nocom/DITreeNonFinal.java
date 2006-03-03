/* $Id$ */

import ibis.util.TypedProperties;

public class DITreeNonFinal implements TestObject {

    static final int OBJECT_SIZE = 4 * 4 + 2 * 4;

    static final int KARMI_SIZE = 4 * 4;

    static final int LEN = TypedProperties.intProperty("len", 1023);

    DITreeNonFinal left;

    DITreeNonFinal right;

    int size;

    int i1;

    int i2;

    int i3;

    public DITreeNonFinal() {
        this(LEN);
    }

    private DITreeNonFinal(int size) {
        int leftSize = size / 2;
        if (leftSize > 0) {
            this.left = new DITreeNonFinal(leftSize);
        }
        if (size - leftSize - 1 > 0) {
            this.right = new DITreeNonFinal(size - leftSize - 1);
        }
        this.size = size;
    }

    public int object_size() {
        return size * OBJECT_SIZE;
    }

    public int payload() {
        return size * KARMI_SIZE;
    }

    public String id() {
        return "Tree of " + size + " DITreeNonFinal objects";
    }

    public int num_objs() {
        return size;
    }
}
