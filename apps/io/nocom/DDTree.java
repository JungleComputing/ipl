/* $Id$ */

import ibis.util.TypedProperties;

public final class DDTree implements TestObject {

    static final int OBJECT_SIZE = 2 * 4 + 8 + 2 * 4;

    static final int KARMI_SIZE = 2 * 4 + 8;

    static final int LEN = TypedProperties.intProperty("len", 1023);

    DDTree left;

    DDTree right;

    int size;

    int i1;

    double d1;

    public DDTree() {
        this(LEN);
    }

    private DDTree(int size) {
        int leftSize = size / 2;
        if (leftSize > 0) {
            this.left = new DDTree(leftSize);
        }
        if (size - leftSize - 1 > 0) {
            this.right = new DDTree(size - leftSize - 1);
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
        return "Tree of " + size + " DDTree objects";
    }

    public int num_objs() {
        return size;
    }
}
